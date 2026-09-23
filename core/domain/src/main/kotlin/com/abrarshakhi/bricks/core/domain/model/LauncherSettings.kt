package com.abrarshakhi.bricks.core.domain.model

enum class ThemeMode { System, Light, Dark }

enum class InkPalette { Newsprint, Blueprint, Mint, Sunset, Grape }

enum class IconStyle { Sticker, Classic, Ink }

enum class Backdrop { Wallpaper, Paper }

enum class PaperTexture { None, Halftone, Hatching }

enum class DrawerLayout { Grid, List }

enum class AppSort { Alphabetical, MostUsed, RecentlyInstalled }

enum class GestureAction {
    None,
    OpenDrawer,
    OpenSearch,
    ExpandNotifications,
    ExpandQuickSettings,
    LockScreen,
    OpenSettings,
}

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val palette: InkPalette = InkPalette.Newsprint,
    val iconStyle: IconStyle = IconStyle.Sticker,
    val iconScale: Float = 1f,
    val backdrop: Backdrop = Backdrop.Wallpaper,
    val wallpaperDim: Float = 0.1f,
    val texture: PaperTexture = PaperTexture.Halftone,
) {
    companion object {
        const val MIN_ICON_SCALE = 0.8f
        const val MAX_ICON_SCALE = 1.25f
        const val MAX_WALLPAPER_DIM = 0.7f
    }
}

data class HomeSettings(
    val grid: GridSize? = null,
    val showLabels: Boolean = true,
    val showGlance: Boolean = true,
    val showSearchBar: Boolean = true,
    val dockEnabled: Boolean = true,
    val dockSlots: Int = 5,
    val autoAddNewApps: Boolean = true,
    val layoutLocked: Boolean = false,
    val wallpaperScrolling: Boolean = true,
    val notificationDots: Boolean = true,
) {
    companion object {
        const val MIN_DOCK_SLOTS = 3
        const val MAX_DOCK_SLOTS = 7
    }
}

data class DrawerSettings(
    val layout: DrawerLayout = DrawerLayout.Grid,
    val sort: AppSort = AppSort.Alphabetical,
    val showSuggestions: Boolean = true,
    val showLabels: Boolean = true,
    val autoFocusSearch: Boolean = false,
    val showIndex: Boolean = true,
)

data class GestureSettings(
    val swipeUp: GestureAction = GestureAction.OpenDrawer,
    val swipeDown: GestureAction = GestureAction.ExpandNotifications,
    val doubleTap: GestureAction = GestureAction.None,
)

data class LauncherSettings(
    val appearance: AppearanceSettings = AppearanceSettings(),
    val home: HomeSettings = HomeSettings(),
    val drawer: DrawerSettings = DrawerSettings(),
    val gestures: GestureSettings = GestureSettings(),
)
