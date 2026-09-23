package com.abrarshakhi.bricks.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.abrarshakhi.bricks.core.designsystem.component.PaperTextureStyle
import com.abrarshakhi.bricks.core.designsystem.theme.BricksPalette
import com.abrarshakhi.bricks.core.designsystem.theme.BricksTheme
import com.abrarshakhi.bricks.core.domain.model.AppearanceSettings
import com.abrarshakhi.bricks.core.domain.model.InkPalette
import com.abrarshakhi.bricks.core.domain.model.PaperTexture
import com.abrarshakhi.bricks.core.domain.model.ThemeMode

@Composable
fun LauncherTheme(
    appearance: AppearanceSettings,
    content: @Composable () -> Unit,
) {
    BricksTheme(
        palette = appearance.palette.toBricksPalette(),
        darkTheme = appearance.themeMode.isDark(),
        content = content,
    )
}

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

fun InkPalette.toBricksPalette(): BricksPalette = when (this) {
    InkPalette.Newsprint -> BricksPalette.Newsprint
    InkPalette.Blueprint -> BricksPalette.Blueprint
    InkPalette.Mint -> BricksPalette.Mint
    InkPalette.Sunset -> BricksPalette.Sunset
    InkPalette.Grape -> BricksPalette.Grape
}

fun PaperTexture.toTextureStyle(): PaperTextureStyle = when (this) {
    PaperTexture.None -> PaperTextureStyle.None
    PaperTexture.Halftone -> PaperTextureStyle.Halftone
    PaperTexture.Hatching -> PaperTextureStyle.Hatching
}
