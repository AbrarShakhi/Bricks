package com.abrarshakhi.bricks.core.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.UserManager
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.math.roundToInt

class WidgetPreviewLoader(
    context: Context,
    private val userManager: UserManager,
) {
    private val appContext = context.applicationContext
    private val manager = AppWidgetManager.getInstance(appContext)
    private val densityDpi = appContext.resources.displayMetrics.densityDpi
    private val permits = Semaphore(PARALLEL_LOADS)
    private val cache = object : LruCache<String, ImageBitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    fun peek(key: WidgetProviderKey, maxPx: Int, icon: Boolean): ImageBitmap? = cache.get(cacheKey(key, maxPx, icon))

    suspend fun load(key: WidgetProviderKey, maxPx: Int, icon: Boolean): ImageBitmap? {
        val cacheKey = cacheKey(key, maxPx, icon)
        cache.get(cacheKey)?.let { return it }
        return permits.withPermit {
            withContext(Dispatchers.IO) {
                cache.get(cacheKey) ?: render(key, maxPx, icon)?.also { cache.put(cacheKey, it) }
            }
        }
    }

    private fun render(key: WidgetProviderKey, maxPx: Int, icon: Boolean): ImageBitmap? {
        val info = providerInfo(key) ?: return null
        val drawable = if (icon) {
            runCatching { info.loadIcon(appContext, densityDpi) }.getOrNull()
        } else {
            runCatching { info.loadPreviewImage(appContext, densityDpi) }.getOrNull()
                ?: runCatching { info.loadIcon(appContext, densityDpi) }.getOrNull()
        } ?: return null
        return draw(drawable, maxPx)?.asImageBitmap()
    }

    private fun providerInfo(key: WidgetProviderKey): AppWidgetProviderInfo? {
        val user = runCatching { userManager.getUserForSerialNumber(key.userSerial) }.getOrNull() ?: return null
        return runCatching { manager.getInstalledProvidersForProfile(user) }.getOrNull()
            ?.firstOrNull { it.provider.packageName == key.packageName && it.provider.className == key.className }
    }

    private fun draw(drawable: Drawable, maxPx: Int): Bitmap? {
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: maxPx
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: maxPx
        val scale = minOf(1f, maxPx.toFloat() / maxOf(width, height))
        val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return runCatching {
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, targetWidth, targetHeight)
            drawable.draw(Canvas(bitmap))
            bitmap
        }.getOrNull()
    }

    private fun cacheKey(key: WidgetProviderKey, maxPx: Int, icon: Boolean) =
        "${key.packageName}/${key.className}#${key.userSerial}|$maxPx|$icon"

    private companion object {
        const val PARALLEL_LOADS = 3
        const val CACHE_BYTES = 12 * 1024 * 1024
    }
}

@Composable
fun rememberWidgetPreview(key: WidgetProviderKey, maxSize: Dp, icon: Boolean = false): ImageBitmap? {
    val loader = koinInject<WidgetPreviewLoader>()
    val maxPx = with(LocalDensity.current) { maxSize.toPx().roundToInt() }
    val bitmap by produceState(loader.peek(key, maxPx, icon), key, maxPx, icon) {
        value = loader.load(key, maxPx, icon)
    }
    return bitmap
}
