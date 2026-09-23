package com.abrarshakhi.bricks.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.abrarshakhi.bricks.core.designsystem.R
import com.abrarshakhi.paperui.PuiColorScheme
import com.abrarshakhi.paperui.PuiTheme
import com.abrarshakhi.paperui.PuiTypography
import com.abrarshakhi.paperui.puiDarkColors
import com.abrarshakhi.paperui.puiLightColors
import com.abrarshakhi.paperui.puiTypography

enum class BricksPalette { Newsprint, Blueprint, Mint, Sunset, Grape }

val BricksLettering: FontFamily = FontFamily(Font(R.font.bangers_regular, FontWeight.Normal))

val BricksTypography: PuiTypography = puiTypography().let { base ->
    fun lettering(size: Int, line: Int, tracking: Float) = TextStyle(
        fontFamily = BricksLettering,
        fontWeight = FontWeight.Normal,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp,
    )
    base.copy(
        displayLarge = lettering(64, 66, 1.5f),
        displayMedium = lettering(50, 54, 1.2f),
        displaySmall = lettering(40, 44, 1f),
        headlineLarge = lettering(34, 38, 1f),
        headlineMedium = lettering(30, 34, 0.9f),
        headlineSmall = lettering(26, 30, 0.8f),
        titleLarge = lettering(25, 30, 1f),
    )
}

@Composable
fun BricksTheme(
    palette: BricksPalette = BricksPalette.Newsprint,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = remember(palette, darkTheme) { bricksColors(palette, darkTheme) }
    PuiTheme(
        darkTheme = darkTheme,
        colors = colors,
        typography = BricksTypography,
        content = content,
    )
}

fun bricksColors(palette: BricksPalette, dark: Boolean): PuiColorScheme = when (palette) {
    BricksPalette.Newsprint -> if (dark) puiDarkColors() else puiLightColors()
    BricksPalette.Blueprint -> if (dark) {
        puiDarkColors(
            paper = Color(0xFF0B1420),
            surface = Color(0xFF13202F),
            surfaceMuted = Color(0xFF1D2D40),
            onSurfaceMuted = Color(0xFFB6C6DA),
            onPaper = Color(0xFFE8F0FA),
            onSurface = Color(0xFFE8F0FA),
            ink = Color(0xFFE8F0FA),
            primary = Color(0xFF7FA8FF),
            onPrimary = Color(0xFF0B1420),
            secondary = Color(0xFF4FD1C5),
            onSecondary = Color(0xFF0B1420),
            accent = Color(0xFFFFD23F),
            onAccent = Color(0xFF0B1420),
            disabled = Color(0xFF34455A),
            onDisabled = Color(0xFF8193A8),
        )
    } else {
        puiLightColors(
            paper = Color(0xFFEEF4FB),
            surface = Color(0xFFF9FBFE),
            surfaceMuted = Color(0xFFDAE5F2),
            onSurfaceMuted = Color(0xFF3A4A5E),
            onPaper = Color(0xFF0D1B2A),
            onSurface = Color(0xFF0D1B2A),
            ink = Color(0xFF0D1B2A),
            primary = Color(0xFF1D4ED8),
            onPrimary = Color(0xFFF9FBFE),
            secondary = Color(0xFF0E7C86),
            onSecondary = Color(0xFFF9FBFE),
            accent = Color(0xFFFFD23F),
            onAccent = Color(0xFF0D1B2A),
            disabled = Color(0xFFB9C7D8),
            onDisabled = Color(0xFF66778C),
        )
    }
    BricksPalette.Mint -> if (dark) {
        puiDarkColors(
            paper = Color(0xFF0D1712),
            surface = Color(0xFF15231B),
            surfaceMuted = Color(0xFF1F3127),
            onSurfaceMuted = Color(0xFFB5CFC0),
            onPaper = Color(0xFFEAF7EF),
            onSurface = Color(0xFFEAF7EF),
            ink = Color(0xFFEAF7EF),
            primary = Color(0xFF4ADE80),
            onPrimary = Color(0xFF0D1712),
            secondary = Color(0xFFA78BFA),
            onSecondary = Color(0xFF0D1712),
            accent = Color(0xFFFFD23F),
            onAccent = Color(0xFF0D1712),
            disabled = Color(0xFF2F4538),
            onDisabled = Color(0xFF7E978A),
        )
    } else {
        puiLightColors(
            paper = Color(0xFFF1FAF3),
            surface = Color(0xFFFBFEFC),
            surfaceMuted = Color(0xFFD9EDE0),
            onSurfaceMuted = Color(0xFF34503F),
            onPaper = Color(0xFF10231A),
            onSurface = Color(0xFF10231A),
            ink = Color(0xFF10231A),
            primary = Color(0xFF0B7A45),
            onPrimary = Color(0xFFFBFEFC),
            secondary = Color(0xFF5B45C2),
            onSecondary = Color(0xFFFBFEFC),
            accent = Color(0xFFFFD23F),
            onAccent = Color(0xFF10231A),
            disabled = Color(0xFFBDD3C5),
            onDisabled = Color(0xFF647A6D),
        )
    }
    BricksPalette.Sunset -> if (dark) {
        puiDarkColors(
            paper = Color(0xFF1A110D),
            surface = Color(0xFF2A1B14),
            surfaceMuted = Color(0xFF3A261C),
            onSurfaceMuted = Color(0xFFE0C2B0),
            onPaper = Color(0xFFFFF1E6),
            onSurface = Color(0xFFFFF1E6),
            ink = Color(0xFFFFF1E6),
            primary = Color(0xFFFF8A5B),
            onPrimary = Color(0xFF1A110D),
            secondary = Color(0xFFE879F9),
            onSecondary = Color(0xFF1A110D),
            accent = Color(0xFFFFC53D),
            onAccent = Color(0xFF1A110D),
            disabled = Color(0xFF4A362B),
            onDisabled = Color(0xFF9C8272),
        )
    } else {
        puiLightColors(
            paper = Color(0xFFFFF3E8),
            surface = Color(0xFFFFFAF5),
            surfaceMuted = Color(0xFFF6DFCC),
            onSurfaceMuted = Color(0xFF5A3F2E),
            onPaper = Color(0xFF26160E),
            onSurface = Color(0xFF26160E),
            ink = Color(0xFF26160E),
            primary = Color(0xFFD1441C),
            onPrimary = Color(0xFFFFFAF5),
            secondary = Color(0xFF8E2C8F),
            onSecondary = Color(0xFFFFFAF5),
            accent = Color(0xFFFFC53D),
            onAccent = Color(0xFF26160E),
            disabled = Color(0xFFE2CBB8),
            onDisabled = Color(0xFF8A6F5E),
        )
    }
    BricksPalette.Grape -> if (dark) {
        puiDarkColors(
            paper = Color(0xFF130F1C),
            surface = Color(0xFF1E182B),
            surfaceMuted = Color(0xFF2B2240),
            onSurfaceMuted = Color(0xFFCFC2E6),
            onPaper = Color(0xFFF4EEFF),
            onSurface = Color(0xFFF4EEFF),
            ink = Color(0xFFF4EEFF),
            primary = Color(0xFFB794F6),
            onPrimary = Color(0xFF130F1C),
            secondary = Color(0xFFF472B6),
            onSecondary = Color(0xFF130F1C),
            accent = Color(0xFFFFD23F),
            onAccent = Color(0xFF130F1C),
            disabled = Color(0xFF3B3150),
            onDisabled = Color(0xFF8D82A3),
        )
    } else {
        puiLightColors(
            paper = Color(0xFFF7F2FF),
            surface = Color(0xFFFCFAFF),
            surfaceMuted = Color(0xFFE6DCF7),
            onSurfaceMuted = Color(0xFF47395E),
            onPaper = Color(0xFF1C1429),
            onSurface = Color(0xFF1C1429),
            ink = Color(0xFF1C1429),
            primary = Color(0xFF6D28D9),
            onPrimary = Color(0xFFFCFAFF),
            secondary = Color(0xFFBE185D),
            onSecondary = Color(0xFFFCFAFF),
            accent = Color(0xFFFFD23F),
            onAccent = Color(0xFF1C1429),
            disabled = Color(0xFFD5CAE8),
            onDisabled = Color(0xFF7B6E91),
        )
    }
}
