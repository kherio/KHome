@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kherio.khome.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.tv.material3.ProvideTextStyle
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
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

/** Número de fondos disponibles para el tema oscuro (ver strings bg_*). */
const val BACKGROUND_COUNT = 11

/** Número de colores de acento (ver strings accent_*). */
const val ACCENT_COUNT = 6

private val DarkAccents = listOf(
    Color(0xFFF2C277), Color(0xFF6EB6FF), Color(0xFF7FD99A),
    Color(0xFFFF8FB1), Color(0xFFFFA25C), Color(0xFFF2F4F8),
)
private val LightAccents = listOf(
    Color(0xFFD9962B), Color(0xFF2F7DD1), Color(0xFF2E9E57),
    Color(0xFFD6457A), Color(0xFFE0702A), Color(0xFF3A4250),
)

fun darkKColors(background: Int, accentIndex: Int = 0, hour: Int = 12): KColors {
    val (top, bottom) = when (background) {
        0 -> Color(0xFF1B212B) to Color(0xFF262D39) // Grafito
        1 -> Color(0xFF0E1A30) to Color(0xFF18294A) // Azul noche
        2 -> Color(0xFF101F1A) to Color(0xFF1C3328) // Bosque
        3 -> Color(0xFF2A1520) to Color(0xFF40202F) // Vino
        4 -> Color(0xFF08090C) to Color(0xFF14161C) // Negro
        5 -> Color(0xFF0B1F24) to Color(0xFF123B42) // Océano
        6 -> Color(0xFF2A1B3D) to Color(0xFF5A2A45) // Atardecer
        7 -> Color(0xFF1D1530) to Color(0xFF33214F) // Púrpura
        8 -> Color(0xFF1F1713) to Color(0xFF362820) // Café
        9 -> Color(0xFF0A0F2C) to Color(0xFF1A2260) // Medianoche
        else -> when (hour) { // Automático según la hora
            in 5..7 -> Color(0xFF241B33) to Color(0xFF5B3A4A) // amanecer
            in 8..16 -> Color(0xFF13243D) to Color(0xFF25476F) // día
            in 17..19 -> Color(0xFF2A1B3D) to Color(0xFF6A3047) // atardecer
            else -> Color(0xFF090C1C) to Color(0xFF161C3A) // noche
        }
    }
    return KColors(
        isDark = true,
        bgTop = top,
        bgBottom = bottom,
        card = lerp(bottom, Color.White, 0.10f),
        cardFocused = lerp(bottom, Color.White, 0.18f),
        cardBorder = Color.White.copy(alpha = 0.16f),
        accent = DarkAccents[accentIndex.coerceIn(0, DarkAccents.lastIndex)],
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

fun lightKColors(accentIndex: Int = 0): KColors = KColors(
    isDark = false,
    bgTop = Color(0xFFE6EAF1),
    bgBottom = Color(0xFFF4F6FA),
    card = Color.White,
    cardFocused = Color.White,
    cardBorder = Color.Black.copy(alpha = 0.10f),
    accent = LightAccents[accentIndex.coerceIn(0, LightAccents.lastIndex)],
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
    // La hora solo importa para el fondo automático; se revisa cada pocos minutos.
    val hour by produceState(initialValue = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        while (true) {
            delay(5 * 60_000L)
            value = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
    }
    val colors = if (dark) {
        darkKColors(settings.background, settings.accent, hour)
    } else {
        lightKColors(settings.accent)
    }

    CompositionLocalProvider(LocalK provides colors) {
        MaterialTheme(
            colorScheme = if (dark) {
                darkColorScheme(primary = colors.accent, background = colors.bgTop, surface = colors.panel)
            } else {
                lightColorScheme(primary = colors.accent, background = colors.bgTop, surface = colors.panel)
            },
            content = {
                ProvideTextStyle(TextStyle(fontFamily = InterFamily)) { content() }
            },
        )
    }
}

/**
 * Fondo vivo: degradado, manchas de color que se mueven muy despacio (teñidas por la app enfocada)
 * o, si se elige, fondos de películas en tendencia que cambian solos con un fundido.
 *
 * [mode]: 0 manchas, 1 películas (TMDB), 2 liso.
 */
@Composable
fun KBackground(
    tint: Color,
    mode: Int,
    backdrops: List<String>,
    content: @Composable BoxScope.() -> Unit,
) {
    val k = LocalK.current
    val top by animateColorAsState(k.bgTop, tween(1500), label = "bgTop")
    val bottom by animateColorAsState(k.bgBottom, tween(1500), label = "bgBottom")

    val phase by rememberInfiniteTransition(label = "bg").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(90_000, easing = LinearEasing)),
        label = "phase",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .drawBehind {
                if (mode == 0 && k.isDark) {
                    val w = size.width
                    val h = size.height

                    fun blob(cx: Float, cy: Float, r: Float, color: Color) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(color, Color.Transparent),
                                center = Offset(cx, cy),
                                radius = r,
                            ),
                            radius = r,
                            center = Offset(cx, cy),
                        )
                    }
                    blob(w * (0.28f + 0.14f * sin(phase)), h * (0.32f + 0.12f * cos(phase)), w * 0.50f, tint.copy(alpha = 0.26f))
                    blob(w * (0.80f + 0.10f * cos(phase + 1f)), h * (0.70f + 0.14f * sin(phase + 2f)), w * 0.52f, k.accent.copy(alpha = 0.13f))
                    blob(w * (0.55f + 0.18f * sin(phase * 2f + 3f)), h * (0.10f + 0.08f * cos(phase)), w * 0.40f, tint.copy(alpha = 0.10f))
                }
            },
    ) {
        if (mode == 1 && backdrops.isNotEmpty()) {
            var index by remember { mutableIntStateOf(0) }
            LaunchedEffect(backdrops) {
                while (true) {
                    delay(30_000L)
                    index = (index + 1) % backdrops.size
                }
            }
            Crossfade(
                targetState = backdrops[index % backdrops.size],
                animationSpec = tween(2500),
                label = "backdrop",
            ) { url ->
                val image = rememberRemoteImage(url)
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().alpha(if (k.isDark) 0.55f else 0.35f),
                    )
                }
            }
        }
        // Viñeta y velo para que los iconos y los textos siempre se lean bien.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (k.isDark) 0.32f else 0f)),
                    ),
                ),
        )
        content()
    }
}
