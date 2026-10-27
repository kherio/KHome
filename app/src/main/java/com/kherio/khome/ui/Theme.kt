@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kherio.khome.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/** Paleta y utilidades del estilo "glassmorphism". */
object Glass {
    val Base = Color(0xFF080B14)

    val Fill = Color.White.copy(alpha = 0.10f)
    val FillStrong = Color.White.copy(alpha = 0.14f)
    val FillFocused = Color.White.copy(alpha = 0.24f)

    val Stroke = Color.White.copy(alpha = 0.20f)
    val StrokeFocused = Color.White.copy(alpha = 0.92f)

    val TextPrimary = Color.White.copy(alpha = 0.95f)
    val TextSecondary = Color.White.copy(alpha = 0.65f)

    val Accent = Color(0xFF4F8CFF)
}

/**
 * Panel de cristal: relleno translúcido con degradado suave y borde de luz
 * (más brillante arriba que abajo). Sin blur en tiempo real para ir fluido en TVs modestas.
 */
fun Modifier.glass(shape: Shape, fill: Color = Glass.Fill): Modifier =
    this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(fill.copy(alpha = (fill.alpha + 0.05f).coerceAtMost(1f)), fill),
            ),
        )
        .border(
            BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.08f)),
                ),
            ),
            shape,
        )

/** Fondo oscuro con halos de color difuminados que dan "profundidad" al cristal. */
@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Glass.Base)
            .drawBehind {
                val w = size.width
                val h = size.height
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF3B6BFF).copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(w * 0.12f, h * 0.10f),
                        radius = w * 0.55f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF9B4DFF).copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(w * 0.88f, h * 0.30f),
                        radius = w * 0.50f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF00C2B8).copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(w * 0.55f, h * 1.00f),
                        radius = w * 0.60f,
                    ),
                )
            },
        content = content,
    )
}

@Composable
fun KHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Glass.Accent,
            background = Glass.Base,
            surface = Color(0xFF111628),
        ),
        content = content,
    )
}
