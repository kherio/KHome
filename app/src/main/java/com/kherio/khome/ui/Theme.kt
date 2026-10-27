@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kherio.khome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import com.kherio.khome.data.Settings

/** Colores de KHome. Se leen con [LocalK] desde cualquier composable. */
@Immutable
data class KColors(
    val isDark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val card: Color,
    val cardFocused: Color,
    val cardBorder: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val button: Color,
    val buttonBorder: Color,
    val panel: Color,
    val panelBorder: Color,
    val chip: Color,
    val chipSelected: Color,
)

private val Gold = Color(0xFFF2C277)

/** Número de fondos disponibles para el tema oscuro (ver strings bg_*). */
const val BACKGROUND_COUNT = 5

fun darkKColors(background: Int): KColors {
    val (top, bottom) = when (background) {
        0 -> Color(0xFF1B212B) to Color(0xFF262D39) // Grafito
        1 -> Color(0xFF0E1A30) to Color(0xFF18294A) // Azul noche
        2 -> Color(0xFF101F1A) to Color(0xFF1C3328) // Bosque
        3 -> Color(0xFF2A1520) to Color(0xFF40202F) // Vino
        else -> Color(0xFF08090C) to Color(0xFF14161C) // Negro
    }
    return KColors(
        isDark = true,
        bgTop = top,
        bgBottom = bottom,
        card = lerp(bottom, Color.White, 0.10f),
        cardFocused = lerp(bottom, Color.White, 0.18f),
        cardBorder = Color.White.copy(alpha = 0.16f),
        accent = Gold,
        textPrimary = Color.White.copy(alpha = 0.96f),
        textSecondary = Color.White.copy(alpha = 0.62f),
        textTertiary = Color.White.copy(alpha = 0.42f),
        button = lerp(bottom, Color.White, 0.12f),
        buttonBorder = Color.White.copy(alpha = 0.20f),
        panel = lerp(bottom, Color.White, 0.07f),
        panelBorder = Color.White.copy(alpha = 0.16f),
        chip = Color.White.copy(alpha = 0.08f),
        chipSelected = Color.White.copy(alpha = 0.22f),
    )
}

fun lightKColors(): KColors = KColors(
    isDark = false,
    bgTop = Color(0xFFE6EAF1),
    bgBottom = Color(0xFFF4F6FA),
    card = Color.White,
    cardFocused = Color.White,
    cardBorder = Color.Black.copy(alpha = 0.10f),
    accent = Color(0xFFD9962B),
    textPrimary = Color(0xFF1B212B),
    textSecondary = Color.Black.copy(alpha = 0.60f),
    textTertiary = Color.Black.copy(alpha = 0.42f),
    button = Color.White,
    buttonBorder = Color.Black.copy(alpha = 0.12f),
    panel = Color.White,
    panelBorder = Color.Black.copy(alpha = 0.10f),
    chip = Color.Black.copy(alpha = 0.06f),
    chipSelected = Color.Black.copy(alpha = 0.14f),
)

val LocalK = staticCompositionLocalOf { darkKColors(0) }

@Composable
fun KHomeTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val colors = if (dark) darkKColors(settings.background) else lightKColors()

    CompositionLocalProvider(LocalK provides colors) {
        MaterialTheme(
            colorScheme = if (dark) {
                darkColorScheme(primary = colors.accent, background = colors.bgTop, surface = colors.panel)
            } else {
                lightColorScheme(primary = colors.accent, background = colors.bgTop, surface = colors.panel)
            },
            content = content,
        )
    }
}

/** Fondo liso con degradado vertical y una ligera viñeta en el tema oscuro. */
@Composable
fun KBackground(content: @Composable BoxScope.() -> Unit) {
    val k = LocalK.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(k.bgTop, k.bgBottom)))
            .drawBehind {
                if (k.isDark) {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                            center = Offset(size.width / 2f, size.height * 0.55f),
                            radius = size.maxDimension * 0.75f,
                        ),
                    )
                }
            },
        content = content,
    )
}
