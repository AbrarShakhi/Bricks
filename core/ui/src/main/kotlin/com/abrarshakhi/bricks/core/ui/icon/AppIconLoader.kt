package com.abrarshakhi.bricks.core.ui.icon

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.util.DisplayMetrics
import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.IconStyle
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.ceil

enum class IconKind { System, FullBleed, Monochrome, Legacy }

@Immutable
class LoadedIcon(
    val bitmap: ImageBitmap,
    val kind: IconKind,
    internal val byteCount: Int,
)

class AppIconLoader(
    context: Context,
    private val launcherApps: LauncherApps,
    private val userManager: UserManager,
) {
    private val appContext = context.applicationContext
    private val densityDpi = appContext.resources.displayMetrics.densityDpi
    private val density = appContext.resources.displayMetrics.density
    private val permits = Semaphore(MAX_PARALLEL_LOADS)
    private val generation = MutableStateFlow(0)

    val version: StateFlow<Int> = generation.asStateFlow()

    private val cache = object : LruCache<String, LoadedIcon>(cacheBudget(appContext)) {
        override fun sizeOf(key: String, value: LoadedIcon): Int = value.byteCount
    }

    init {
        launcherApps.registerCallback(
            object : LauncherApps.Callback() {
                override fun onPackageRemoved(packageName: String, user: UserHandle) = invalidate(packageName)

                override fun onPackageAdded(packageName: String, user: UserHandle) = invalidate(packageName)

                override fun onPackageChanged(packageName: String, user: UserHandle) = invalidate(packageName)

                override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
                    packageNames.forEach(::invalidate)

                override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
                    packageNames.forEach(::invalidate)

                override fun onShortcutsChanged(
                    packageName: String,
                    shortcuts: MutableList<android.content.pm.ShortcutInfo>,
                    user: UserHandle,
                ) = invalidate(packageName)
            },
            Handler(Looper.getMainLooper()),
        )
    }

    fun peek(app: AppKey, sizePx: Int, style: IconStyle): LoadedIcon? = cache.get(cacheKey(app, sizePx, style))

    suspend fun load(app: AppKey, sizePx: Int, style: IconStyle): LoadedIcon? {
        val key = cacheKey(app, sizePx, style)
        cache.get(key)?.let { return it }
        return permits.withPermit {
            withContext(Dispatchers.IO) {
                cache.get(key) ?: renderApp(app, sizePx, style)?.also { cache.put(key, it) }
            }
        }
    }

    fun peekShortcut(shortcut: ShortcutKey, sizePx: Int, style: IconStyle): LoadedIcon? =
        cache.get(shortcutCacheKey(shortcut, sizePx, style))

    suspend fun loadShortcut(shortcut: ShortcutKey, sizePx: Int, style: IconStyle): LoadedIcon? {
        val key = shortcutCacheKey(shortcut, sizePx, style)
        cache.get(key)?.let { return it }
        return permits.withPermit {
            withContext(Dispatchers.IO) {
                cache.get(key) ?: renderShortcut(shortcut, sizePx, style)?.also { cache.put(key, it) }
            }
        }
    }

    suspend fun loadExternal(sizePx: Int, style: IconStyle, source: (densityDpi: Int) -> Drawable?): LoadedIcon? =
        permits.withPermit {
            withContext(Dispatchers.IO) {
                runCatching { source(iconDensityFor(sizePx)) }.getOrNull()?.let { render(it, sizePx, style) }
            }
        }

    fun clear() {
        cache.evictAll()
        generation.update { it + 1 }
    }

    private fun invalidate(packageName: String) {
        val prefix = "$packageName/"
        val shortcutPrefix = "shortcut:$packageName/"
        cache.snapshot().keys
            .filter { it.startsWith(prefix) || it.startsWith(shortcutPrefix) }
            .forEach { cache.remove(it) }
        generation.update { it + 1 }
    }

    private fun renderApp(app: AppKey, sizePx: Int, style: IconStyle): LoadedIcon? {
        val user = runCatching { userManager.getUserForSerialNumber(app.userSerial) }.getOrNull() ?: return null
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.packageName, app.className))
        val info = runCatching { launcherApps.resolveActivity(intent, user) }.getOrNull() ?: return null
        val drawable = runCatching { info.getIcon(iconDensityFor(sizePx)) }.getOrNull() ?: return null
        return render(drawable, sizePx, style)
    }

    private fun renderShortcut(shortcut: ShortcutKey, sizePx: Int, style: IconStyle): LoadedIcon? {
        val user = runCatching { userManager.getUserForSerialNumber(shortcut.userSerial) }.getOrNull() ?: return null
        if (!runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)) return null
        val query = LauncherApps.ShortcutQuery()
            .setPackage(shortcut.packageName)
            .setShortcutIds(listOf(shortcut.shortcutId))
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST,
            )
        val info = runCatching { launcherApps.getShortcuts(query, user) }.getOrNull()?.firstOrNull() ?: return null
        val drawable = runCatching { launcherApps.getShortcutIconDrawable(info, iconDensityFor(sizePx)) }.getOrNull()
            ?: return null
        return render(drawable, sizePx, style)
    }

    private fun render(drawable: Drawable, sizePx: Int, style: IconStyle): LoadedIcon {
        val size = sizePx.coerceIn(MIN_SIZE_PX, MAX_SIZE_PX)
        val adaptive = drawable as? AdaptiveIconDrawable
        return when {
            style == IconStyle.Classic -> icon(drawSystem(drawable, size), IconKind.System)
            adaptive != null && style == IconStyle.Ink && monochromeOf(adaptive) != null ->
                icon(drawLayers(listOfNotNull(monochromeOf(adaptive)), size), IconKind.Monochrome)
            adaptive != null -> icon(drawLayers(listOfNotNull(adaptive.background, adaptive.foreground), size), IconKind.FullBleed)
            else -> icon(drawLegacy(drawable, size), IconKind.Legacy)
        }
    }

    private fun monochromeOf(adaptive: AdaptiveIconDrawable): Drawable? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) adaptive.monochrome else null

    private fun icon(bitmap: Bitmap, kind: IconKind) = LoadedIcon(
        bitmap = bitmap.asImageBitmap(),
        kind = kind,
        byteCount = bitmap.allocationByteCount,
    )

    private fun drawSystem(drawable: Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    private fun drawLayers(layers: List<Drawable>, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val inset = size / 4
        layers.forEach { layer ->
            layer.setBounds(-inset, -inset, size + inset, size + inset)
            layer.draw(canvas)
        }
        return bitmap
    }

    private fun drawLegacy(drawable: Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: size
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: size
        val scale = size.toFloat() / maxOf(width, height)
        val drawWidth = (width * scale).toInt()
        val drawHeight = (height * scale).toInt()
        val left = (size - drawWidth) / 2
        val top = (size - drawHeight) / 2
        drawable.setBounds(left, top, left + drawWidth, top + drawHeight)
        drawable.draw(canvas)
        return bitmap
    }

    private fun iconDensityFor(sizePx: Int): Int {
        val required = ceil(sizePx / (NOMINAL_ICON_DP * density) * densityDpi).toInt()
        return DENSITY_BUCKETS.firstOrNull { it >= maxOf(required, densityDpi) } ?: DisplayMetrics.DENSITY_XXXHIGH
    }

    private fun cacheKey(app: AppKey, sizePx: Int, style: IconStyle) = "${app.flattened}|$sizePx|${style.name}"

    private fun shortcutCacheKey(shortcut: ShortcutKey, sizePx: Int, style: IconStyle) =
        "shortcut:${shortcut.packageName}/${shortcut.shortcutId}#${shortcut.userSerial}|$sizePx|${style.name}"

    private companion object {
        const val MAX_PARALLEL_LOADS = 4
        const val MIN_SIZE_PX = 24
        const val MAX_SIZE_PX = 512
        const val NOMINAL_ICON_DP = 48f
        val DENSITY_BUCKETS = listOf(
            DisplayMetrics.DENSITY_MEDIUM,
            DisplayMetrics.DENSITY_HIGH,
            DisplayMetrics.DENSITY_XHIGH,
            DisplayMetrics.DENSITY_XXHIGH,
            DisplayMetrics.DENSITY_XXXHIGH,
        )

        fun cacheBudget(context: Context): Int {
            val memoryClass = context.getSystemService(ActivityManager::class.java)?.memoryClass ?: 128
            return (memoryClass * 1024 * 1024 / 8).coerceIn(8 * 1024 * 1024, 48 * 1024 * 1024)
        }
    }
}
