package com.kherio.khome.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    /** Apps lanzables en TV (LEANBACK) y, si no tienen, las que tienen icono normal de launcher. */
    @Suppress("DEPRECATION")
    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.Default) {
        val resolved = LinkedHashMap<String, ResolveInfo>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(intent, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg !in resolved) resolved[pkg] = ri
            }
        }
        resolved.values
            .filter { it.activityInfo.packageName != context.packageName }
            .mapNotNull { runCatching { toEntry(it) }.getOrNull() }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun toEntry(ri: ResolveInfo): AppEntry {
        val info = ri.activityInfo
        val iconBitmap = renderIcon(loadBestIcon(ri))
        return AppEntry(
            packageName = info.packageName,
            label = ri.loadLabel(pm).toString(),
            activityName = info.name,
            icon = iconBitmap.asImageBitmap(),
            tint = dominantColor(iconBitmap),
            banner = runCatching {
                (info.loadBanner(pm) ?: info.applicationInfo.loadBanner(pm))
                    ?.toBitmap(BANNER_W, BANNER_H)?.asImageBitmap()
            }.getOrNull(),
        )
    }

    /** Color dominante: media de los píxeles con color (se ignoran transparentes, blancos y negros). */
    private fun dominantColor(bitmap: Bitmap): Color {
        val small = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
        var r = 0f
        var g = 0f
        var b = 0f
        var weight = 0f
        val hsv = FloatArray(3)
        for (y in 0 until small.height) {
            for (x in 0 until small.width) {
                val px = small.getPixel(x, y)
                if (android.graphics.Color.alpha(px) < 200) continue
                android.graphics.Color.colorToHSV(px, hsv)
                if (hsv[2] < 0.2f || (hsv[1] < 0.15f && hsv[2] > 0.85f)) continue
                val w = hsv[1] * hsv[2] + 0.02f
                r += android.graphics.Color.red(px) * w
                g += android.graphics.Color.green(px) * w
                b += android.graphics.Color.blue(px) * w
                weight += w
            }
        }
        if (small !== bitmap) small.recycle()
        if (weight <= 0.05f) return Color.Unspecified
        return Color(r / weight / 255f, g / weight / 255f, b / weight / 255f)
    }

    /**
     * Dibuja el icono en alta resolución. Los iconos adaptativos se renderizan a tamaño completo,
     * se recorta la zona visible y se les da esquinas redondeadas para que no queden pequeños.
     */
    /** Pide al paquete el icono de mayor densidad (xxxhdpi); si no lo da, usa el que elige el sistema. */
    private fun loadBestIcon(ri: ResolveInfo): Drawable {
        val info = ri.activityInfo
        val iconRes = info.icon.takeIf { it != 0 } ?: info.applicationInfo.icon
        if (iconRes != 0) {
            val hi = runCatching {
                pm.getResourcesForApplication(info.applicationInfo)
                    .getDrawableForDensity(iconRes, DENSITY_XXXHIGH, null)
            }.getOrNull()
            if (hi != null) return hi
        }
        return ri.loadIcon(pm)
    }

    private fun renderIcon(d: Drawable): Bitmap {
        if (Build.VERSION.SDK_INT >= 26 && d is AdaptiveIconDrawable) {
            val big = ICON_SIZE * 3 / 2
            val full = Bitmap.createBitmap(big, big, Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, big, big)
            d.draw(Canvas(full))
            val off = (big - ICON_SIZE) / 2
            val out = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val r = ICON_SIZE * 0.22f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawRoundRect(RectF(0f, 0f, ICON_SIZE.toFloat(), ICON_SIZE.toFloat()), r, r, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(full, -off.toFloat(), -off.toFloat(), paint)
            full.recycle()
            return out
        }
        // Iconos clásicos: se dibujan al doble y se reducen con filtro para que los bordes queden suaves.
        val twice = d.toBitmap(ICON_SIZE * 2, ICON_SIZE * 2)
        val scaled = Bitmap.createScaledBitmap(twice, ICON_SIZE, ICON_SIZE, true)
        if (scaled !== twice) twice.recycle()
        return scaled
    }

    private companion object {
        const val ICON_SIZE = 384
        const val DENSITY_XXXHIGH = 640
        const val BANNER_W = 640
        const val BANNER_H = 360
    }
}
