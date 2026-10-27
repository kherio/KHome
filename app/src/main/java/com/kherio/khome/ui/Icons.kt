package com.kherio.khome.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin

// Todos los iconos se dibujan en una cuadrícula de 24x24 y se escalan al tamaño pedido.

enum class LineIconKind { WIFI, ETHERNET, VPN, MONITOR, BROOM, GEAR }

enum class WeatherKind { CLEAR, PARTLY, CLOUDY, FOG, RAIN, SNOW, STORM }

private val SunColor = Color(0xFFF5A623)
private val RainColor = Color(0xFF4A90E2)

@Composable
fun LineIcon(kind: LineIconKind, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 24f
        scale(s, s, Offset.Zero) {
            val st = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (kind) {
                LineIconKind.WIFI -> drawWifi(tint, st)
                LineIconKind.ETHERNET -> drawEthernet(tint, st)
                LineIconKind.VPN -> drawShield(tint, st)
                LineIconKind.MONITOR -> drawMonitor(tint, st)
                LineIconKind.BROOM -> drawBroom(tint, st)
                LineIconKind.GEAR -> drawGear(tint, st)
            }
        }
    }
}

@Composable
fun WeatherIcon(
    kind: WeatherKind,
    cloudColor: Color,
    backdrop: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val s = size.minDimension / 24f
        scale(s, s, Offset.Zero) {
            val st = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (kind) {
                WeatherKind.CLEAR -> drawSun(SunColor, st, 12f, 12f, 4.2f, 7f, 9.5f)

                WeatherKind.PARTLY -> {
                    drawSun(SunColor, st, 8.5f, 8.5f, 3f, 5f, 7f)
                    drawCloud(cloudColor, st, backdrop, 4.5f, 5.5f, 0.78f)
                }

                WeatherKind.CLOUDY -> drawCloud(cloudColor, st, null, 0f, 1.5f, 1f)

                WeatherKind.FOG -> {
                    drawCloud(cloudColor, st, null, 0f, -3f, 1f)
                    drawLine(cloudColor, Offset(6f, 18.5f), Offset(18f, 18.5f), st.width, StrokeCap.Round)
                    drawLine(cloudColor, Offset(8f, 21.5f), Offset(16f, 21.5f), st.width, StrokeCap.Round)
                }

                WeatherKind.RAIN -> {
                    drawCloud(cloudColor, st, null, 0f, -3f, 1f)
                    for (x in listOf(8f, 12f, 16f)) {
                        drawLine(RainColor, Offset(x, 18f), Offset(x - 1f, 21.5f), st.width, StrokeCap.Round)
                    }
                }

                WeatherKind.SNOW -> {
                    drawCloud(cloudColor, st, null, 0f, -3f, 1f)
                    drawCircle(cloudColor, 1.1f, Offset(8f, 19f))
                    drawCircle(cloudColor, 1.1f, Offset(12f, 21f))
                    drawCircle(cloudColor, 1.1f, Offset(16f, 19f))
                }

                WeatherKind.STORM -> {
                    drawCloud(cloudColor, st, null, 0f, -3f, 1f)
                    val bolt = Path().apply {
                        moveTo(13f, 15.5f)
                        lineTo(10f, 19.5f)
                        lineTo(13f, 19.5f)
                        lineTo(11f, 23f)
                    }
                    drawPath(bolt, SunColor, style = st)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Iconos de interfaz
// ---------------------------------------------------------------------------

private fun DrawScope.drawWifi(c: Color, st: Stroke) {
    val cx = 12f
    val cy = 19f
    for (r in listOf(4f, 8f, 12f)) {
        drawArc(
            color = c,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = Size(2f * r, 2f * r),
            style = st,
        )
    }
    drawCircle(c, 1.4f, Offset(cx, cy))
}

private fun DrawScope.drawEthernet(c: Color, st: Stroke) {
    drawRoundRect(c, Offset(6f, 4f), Size(12f, 11f), CornerRadius(2f, 2f), style = st)
    for (x in listOf(9.5f, 12f, 14.5f)) {
        drawLine(c, Offset(x, 7.5f), Offset(x, 10.5f), st.width, StrokeCap.Round)
    }
    drawLine(c, Offset(12f, 15f), Offset(12f, 19f), st.width, StrokeCap.Round)
    drawLine(c, Offset(7f, 19f), Offset(17f, 19f), st.width, StrokeCap.Round)
}

private fun DrawScope.drawShield(c: Color, st: Stroke) {
    val p = Path().apply {
        moveTo(12f, 3f)
        lineTo(19f, 6f)
        lineTo(19f, 12f)
        cubicTo(19f, 16.5f, 16f, 19.5f, 12f, 21f)
        cubicTo(8f, 19.5f, 5f, 16.5f, 5f, 12f)
        lineTo(5f, 6f)
        close()
    }
    drawPath(p, c, style = st)
    val check = Path().apply {
        moveTo(9f, 12f)
        lineTo(11f, 14f)
        lineTo(15f, 9.5f)
    }
    drawPath(check, c, style = st)
}

private fun DrawScope.drawMonitor(c: Color, st: Stroke) {
    drawRoundRect(c, Offset(3f, 4f), Size(18f, 12f), CornerRadius(2f, 2f), style = st)
    drawLine(c, Offset(12f, 16f), Offset(12f, 20f), st.width, StrokeCap.Round)
    drawLine(c, Offset(8f, 20f), Offset(16f, 20f), st.width, StrokeCap.Round)
}

private fun DrawScope.drawBroom(c: Color, st: Stroke) {
    // mango
    drawLine(c, Offset(12f, 2.5f), Offset(12f, 11f), st.width, StrokeCap.Round)
    // cabeza con cerdas
    val head = Path().apply {
        moveTo(9f, 11f)
        lineTo(15f, 11f)
        lineTo(18f, 21f)
        lineTo(6f, 21f)
        close()
    }
    drawPath(head, c, style = st)
    drawLine(c, Offset(10.5f, 15f), Offset(9.8f, 21f), st.width, StrokeCap.Round)
    drawLine(c, Offset(13.5f, 15f), Offset(14.2f, 21f), st.width, StrokeCap.Round)
}

private fun DrawScope.drawGear(c: Color, st: Stroke) {
    drawCircle(c, 3f, Offset(12f, 12f), style = st)
    drawCircle(c, 7f, Offset(12f, 12f), style = st)
    for (i in 0 until 8) {
        val a = Math.toRadians(i * 45.0)
        val ca = cos(a).toFloat()
        val sa = sin(a).toFloat()
        drawLine(
            c,
            Offset(12f + ca * 7f, 12f + sa * 7f),
            Offset(12f + ca * 10f, 12f + sa * 10f),
            3f,
            StrokeCap.Butt,
        )
    }
}

// ---------------------------------------------------------------------------
// Piezas del tiempo
// ---------------------------------------------------------------------------

private fun DrawScope.drawSun(
    c: Color,
    st: Stroke,
    cx: Float,
    cy: Float,
    r: Float,
    rayFrom: Float,
    rayTo: Float,
) {
    drawCircle(c, r, Offset(cx, cy), style = st)
    for (i in 0 until 8) {
        val a = Math.toRadians(i * 45.0)
        val ca = cos(a).toFloat()
        val sa = sin(a).toFloat()
        drawLine(
            c,
            Offset(cx + ca * rayFrom, cy + sa * rayFrom),
            Offset(cx + ca * rayTo, cy + sa * rayTo),
            st.width,
            StrokeCap.Round,
        )
    }
}

/** Contorno de nube dentro de la cuadrícula 24x24 (ocupa x 2..22, y 4..19). */
private fun cloudPath(): Path = Path().apply {
    moveTo(6.5f, 19f)
    lineTo(17.5f, 19f)
    arcTo(Rect(Offset(17.5f, 14.5f), 4.5f), 90f, -180f, false)
    arcTo(Rect(Offset(12f, 9.5f), 5.5f), 5.2f, -185.2f, false)
    arcTo(Rect(Offset(6.5f, 14.5f), 4.5f), 270f, -180f, false)
    close()
}

private fun DrawScope.drawCloud(
    c: Color,
    st: Stroke,
    backdrop: Color?,
    dx: Float,
    dy: Float,
    s: Float,
) {
    withTransform({
        translate(dx, dy)
        scale(s, s, Offset.Zero)
    }) {
        val p = cloudPath()
        if (backdrop != null) drawPath(p, backdrop)
        drawPath(p, c, style = st)
    }
}
