package com.kherio.khome.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
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
            icon = ri.loadIcon(pm).toBitmap(ICON_SIZE, ICON_SIZE).asImageBitmap(),
        )
    }

    private companion object {
        const val ICON_SIZE = 192
    }
}
