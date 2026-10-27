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
        return AppEntry(
            packageName = info.packageName,
            label = ri.loadLabel(pm).toString(),
            activityName = info.name,
            icon = renderIcon(ri.loadIcon(pm)).asImageBitmap(),
            banner = runCatching {
                (info.loadBanner(pm) ?: info.applicationInfo.loadBanner(pm))
                    ?.toBitmap(BANNER_W, BANNER_H)?.asImageBitmap()
            }.getOrNull(),
        )
    }

    /**
     * Dibuja el icono en alta resolución. Los iconos adaptativos se renderizan a tamaño completo,
     * se recorta la zona visible y se les da esquinas redondeadas para que no queden pequeños.
     */
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
        return d.toBitmap(ICON_SIZE, ICON_SIZE)
    }

    private companion object {
        const val ICON_SIZE = 256
        const val BANNER_W = 480
        const val BANNER_H = 270
    }
}
