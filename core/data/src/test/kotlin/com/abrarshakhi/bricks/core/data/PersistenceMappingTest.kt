package com.abrarshakhi.bricks.core.data

import androidx.datastore.core.CorruptionException
import com.abrarshakhi.bricks.core.data.home.HomeLayoutDto
import com.abrarshakhi.bricks.core.data.home.toDomain
import com.abrarshakhi.bricks.core.data.home.toDto
import com.abrarshakhi.bricks.core.data.settings.SettingsDto
import com.abrarshakhi.bricks.core.data.settings.toDomain
import com.abrarshakhi.bricks.core.data.settings.toDto
import com.abrarshakhi.bricks.core.data.store.JsonDataStoreSerializer
import com.abrarshakhi.bricks.core.data.store.LauncherJson
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.AppSort
import com.abrarshakhi.bricks.core.domain.model.AppearanceSettings
import com.abrarshakhi.bricks.core.domain.model.DrawerSettings
import com.abrarshakhi.bricks.core.domain.model.GestureAction
import com.abrarshakhi.bricks.core.domain.model.GestureSettings
import com.abrarshakhi.bricks.core.domain.model.GridCell
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import com.abrarshakhi.bricks.core.domain.model.HomeItem
import com.abrarshakhi.bricks.core.domain.model.HomeLayout
import com.abrarshakhi.bricks.core.domain.model.HomeSettings
import com.abrarshakhi.bricks.core.domain.model.IconStyle
import com.abrarshakhi.bricks.core.domain.model.InkPalette
import com.abrarshakhi.bricks.core.domain.model.LauncherSettings
import com.abrarshakhi.bricks.core.domain.model.Placement
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey
import com.abrarshakhi.bricks.core.domain.model.ThemeMode
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PersistenceMappingTest {

    @Test
    fun `settings survive a round trip`() {
        val settings = LauncherSettings(
            appearance = AppearanceSettings(themeMode = ThemeMode.Dark, palette = InkPalette.Mint, iconStyle = IconStyle.Ink, iconScale = 1.1f),
            home = HomeSettings(grid = GridSize(5, 6), dockSlots = 6, showLabels = false),
            drawer = DrawerSettings(sort = AppSort.MostUsed, autoFocusSearch = true),
            gestures = GestureSettings(doubleTap = GestureAction.LockScreen),
        )
        assertEquals(settings, settings.toDto().toDomain())
    }

    @Test
    fun `an empty settings file yields defaults`() {
        assertEquals(LauncherSettings(), SettingsDto().toDomain())
    }

    @Test
    fun `unknown enum names and out of range numbers fall back safely`() {
        val dto = SettingsDto(themeMode = "Sepia", iconScale = 9f, dockSlots = 99, wallpaperDim = -1f)
        val settings = dto.toDomain()
        assertEquals(ThemeMode.System, settings.appearance.themeMode)
        assertEquals(AppearanceSettings.MAX_ICON_SCALE, settings.appearance.iconScale)
        assertEquals(HomeSettings.MAX_DOCK_SLOTS, settings.home.dockSlots)
        assertEquals(0f, settings.appearance.wallpaperDim)
    }

    @Test
    fun `home layout survives a round trip`() {
        val layout = HomeLayout(
            pageCount = 2,
            items = listOf(
                HomeItem.Application(1, Placement.Desktop(1, GridCell(2, 3)), AppKey("a", "a.Main", 0)),
                HomeItem.Folder(2, Placement.Dock(0), "Work"),
                HomeItem.Application(3, Placement.InFolder(2, 0), AppKey("b", "b.Main", 10)),
                HomeItem.Widget(4, Placement.Desktop(0, GridCell(0, 0), GridSpan(4, 2)), 99, WidgetProviderKey("w", "w.P", 0)),
                HomeItem.Shortcut(5, Placement.InFolder(2, 1), ShortcutKey("c", "compose", 0), "Compose"),
            ),
        )
        assertEquals(layout, layout.toDto().toDomain())
    }

    @Test
    fun `a missing layout file is uninitialized`() {
        val layout = HomeLayoutDto().toDomain()
        assertFalse(layout.isInitialized)
        assertEquals(1, layout.pageCount)
    }

    @Test
    fun `serializer reads what it writes and ignores unknown keys`() = runTest {
        val serializer = JsonDataStoreSerializer(SettingsDto.serializer(), SettingsDto(), LauncherJson)
        val output = ByteArrayOutputStream()
        serializer.writeTo(SettingsDto(palette = "Grape"), output)
        val read = serializer.readFrom(ByteArrayInputStream(output.toByteArray()))
        assertEquals("Grape", read.palette)
        val future = """{"palette":"Mint","addedLater":{"x":1}}"""
        assertEquals("Mint", serializer.readFrom(ByteArrayInputStream(future.toByteArray())).palette)
        assertEquals(SettingsDto(), serializer.readFrom(ByteArrayInputStream(ByteArray(0))))
    }

    @Test
    fun `corrupt files surface as corruption`() {
        val serializer = JsonDataStoreSerializer(SettingsDto.serializer(), SettingsDto(), LauncherJson)
        assertThrows(CorruptionException::class.java) {
            runBlocking { serializer.readFrom(ByteArrayInputStream("{not json".toByteArray())) }
        }
    }
}
