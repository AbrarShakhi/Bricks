package com.abrarshakhi.bricks.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abrarshakhi.bricks.core.designsystem.component.ScreenBodyInsets
import com.abrarshakhi.bricks.core.designsystem.component.ScreenTopBar
import com.abrarshakhi.bricks.core.designsystem.component.halftone
import com.abrarshakhi.bricks.core.designsystem.component.paper
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.bricks.core.designsystem.theme.bricksColors
import com.abrarshakhi.bricks.core.domain.model.AppSort
import com.abrarshakhi.bricks.core.domain.model.Backdrop
import com.abrarshakhi.bricks.core.domain.model.DrawerLayout
import com.abrarshakhi.bricks.core.domain.model.GestureAction
import com.abrarshakhi.bricks.core.domain.model.IconStyle
import com.abrarshakhi.bricks.core.domain.model.InkPalette
import com.abrarshakhi.bricks.core.domain.model.PaperTexture
import com.abrarshakhi.bricks.core.domain.model.ThemeMode
import com.abrarshakhi.bricks.core.ui.component.AppIconShape
import com.abrarshakhi.bricks.core.ui.component.InkTile
import com.abrarshakhi.bricks.core.ui.theme.isDark
import com.abrarshakhi.bricks.core.ui.theme.toBricksPalette
import com.abrarshakhi.bricks.core.ui.theme.toTextureStyle
import com.abrarshakhi.bricks.feature.settings.R
import com.abrarshakhi.paperui.PuiCard
import com.abrarshakhi.paperui.PuiIcon
import com.abrarshakhi.paperui.PuiScaffold
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTheme

@Composable
internal fun SettingsScaffold(
    title: String,
    onBack: (() -> Unit)?,
    content: LazyListScope.() -> Unit,
) {
    PuiScaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = ScreenBodyInsets,
        topBar = {
            ScreenTopBar(
                title = title,
                onBack = onBack,
                backContentDescription = stringResource(R.string.settings_back),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PuiTheme.colors.paper),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + PuiTheme.spacing.lg,
                    bottom = padding.calculateBottomPadding() + PuiTheme.spacing.xxl,
                    start = PuiTheme.spacing.lg,
                    end = PuiTheme.spacing.lg,
                ),
                verticalArrangement = Arrangement.spacedBy(PuiTheme.spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}

internal val SettingsItemModifier: Modifier = Modifier
    .fillMaxWidth()
    .widthIn(max = 720.dp)

@Composable
internal fun themeModeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.System -> R.string.settings_theme_system
        ThemeMode.Light -> R.string.settings_theme_light
        ThemeMode.Dark -> R.string.settings_theme_dark
    },
)

@Composable
internal fun paletteLabel(palette: InkPalette): String = stringResource(
    when (palette) {
        InkPalette.Newsprint -> R.string.settings_palette_newsprint
        InkPalette.Blueprint -> R.string.settings_palette_blueprint
        InkPalette.Mint -> R.string.settings_palette_mint
        InkPalette.Sunset -> R.string.settings_palette_sunset
        InkPalette.Grape -> R.string.settings_palette_grape
    },
)

@Composable
internal fun iconStyleLabel(style: IconStyle): String = stringResource(
    when (style) {
        IconStyle.Sticker -> R.string.settings_icons_sticker
        IconStyle.Classic -> R.string.settings_icons_classic
        IconStyle.Ink -> R.string.settings_icons_ink
    },
)

@Composable
internal fun backdropLabel(backdrop: Backdrop): String = stringResource(
    when (backdrop) {
        Backdrop.Wallpaper -> R.string.settings_backdrop_wallpaper
        Backdrop.Paper -> R.string.settings_backdrop_paper
    },
)

@Composable
internal fun textureLabel(texture: PaperTexture): String = stringResource(
    when (texture) {
        PaperTexture.None -> R.string.settings_texture_none
        PaperTexture.Halftone -> R.string.settings_texture_halftone
        PaperTexture.Hatching -> R.string.settings_texture_hatching
    },
)

@Composable
internal fun drawerLayoutLabel(layout: DrawerLayout): String = stringResource(
    when (layout) {
        DrawerLayout.Grid -> R.string.settings_drawer_grid
        DrawerLayout.List -> R.string.settings_drawer_list
    },
)

@Composable
internal fun sortLabel(sort: AppSort): String = stringResource(
    when (sort) {
        AppSort.Alphabetical -> R.string.settings_sort_alphabetical
        AppSort.MostUsed -> R.string.settings_sort_most_used
        AppSort.RecentlyInstalled -> R.string.settings_sort_recent
    },
)

@Composable
internal fun gestureLabel(action: GestureAction): String = stringResource(
    when (action) {
        GestureAction.None -> R.string.settings_gesture_none
        GestureAction.OpenDrawer -> R.string.settings_gesture_drawer
        GestureAction.OpenSearch -> R.string.settings_gesture_search
        GestureAction.ExpandNotifications -> R.string.settings_gesture_notifications
        GestureAction.ExpandQuickSettings -> R.string.settings_gesture_quick_settings
        GestureAction.LockScreen -> R.string.settings_gesture_lock
        GestureAction.OpenSettings -> R.string.settings_gesture_settings
    },
)

@Composable
internal fun PaletteSwatch(palette: InkPalette) {
    val colors = bricksColors(palette.toBricksPalette(), dark = false)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(colors.primary, colors.secondary, colors.accent).forEach { color ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, PuiTheme.shapes.pill)
                    .border(1.dp, PuiTheme.colors.ink, PuiTheme.shapes.pill),
            )
        }
    }
}

@Composable
internal fun ThemePreview(
    palette: InkPalette,
    themeMode: ThemeMode,
    iconStyle: IconStyle,
    texture: PaperTexture,
    modifier: Modifier = Modifier,
) {
    val colors = bricksColors(palette.toBricksPalette(), themeMode.isDark())
    PuiCard(
        modifier = modifier,
        color = colors.paper,
        borderColor = colors.ink,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .paper(colors.paper, colors.ink, texture.toTextureStyle())
                .padding(PuiTheme.spacing.lg),
        ) {
            PuiText(
                text = stringResource(R.string.settings_preview_title),
                style = PuiTheme.typography.headlineSmall,
                color = colors.onPaper,
            )
            Spacer(Modifier.height(PuiTheme.spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val samples = listOf(
                    BricksIcons.Globe to colors.primary,
                    BricksIcons.Clock to colors.secondary,
                    BricksIcons.Star to colors.accent,
                    BricksIcons.Bell to colors.success,
                )
                samples.forEach { (icon, color) ->
                    PreviewTile(icon, color, colors.ink, colors.surface, iconStyle)
                }
            }
        }
    }
}

@Composable
private fun PreviewTile(icon: ImageVector, color: Color, ink: Color, surface: Color, style: IconStyle) {
    when (style) {
        IconStyle.Classic -> Box(
            modifier = Modifier
                .size(48.dp)
                .background(color, PuiTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) { PuiIcon(icon, contentDescription = null, tint = Color.White) }
        IconStyle.Sticker -> InkTile(
            modifier = Modifier.size(48.dp),
            shape = AppIconShape,
            color = color,
            borderColor = ink,
        ) { PuiIcon(icon, contentDescription = null, tint = Color.White) }
        IconStyle.Ink -> InkTile(
            modifier = Modifier.size(48.dp),
            shape = AppIconShape,
            color = surface,
            borderColor = ink,
        ) {
            Box(modifier = Modifier.size(48.dp).halftone(color = ink, dotRadius = 1.dp, spacing = 5.dp, alpha = 0.12f))
            PuiIcon(icon, contentDescription = null, tint = ink)
        }
    }
}
