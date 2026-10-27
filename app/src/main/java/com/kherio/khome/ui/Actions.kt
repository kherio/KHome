package com.kherio.khome.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.net.Uri
import android.provider.Settings
import com.kherio.khome.data.AppEntry
import kotlinx.coroutines.delay

fun Context.launchApp(app: AppEntry) {
    val intent = Intent(Intent.ACTION_MAIN)
        .setClassName(app.packageName, app.activityName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    runCatching { startActivity(intent) }
}

fun Context.openAppInfo(app: AppEntry) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", app.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

fun Context.uninstallApp(app: AppEntry) {
    val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

private fun Context.openSettingsAction(action: String) {
    runCatching { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun Context.openSystemSettings() = openSettingsAction(Settings.ACTION_SETTINGS)

fun Context.openHomeSettings() = openSettingsAction(Settings.ACTION_HOME_SETTINGS)

fun Context.openWifiSettings() = openSettingsAction(Settings.ACTION_WIFI_SETTINGS)

fun Context.openVpnSettings() = openSettingsAction(Settings.ACTION_VPN_SETTINGS)

/** Cierra los procesos en segundo plano de una app. */
fun Context.closeApp(app: AppEntry) {
    runCatching {
        getSystemService(ActivityManager::class.java).killBackgroundProcesses(app.packageName)
    }
}

/** Cierra los procesos en segundo plano de las apps indicadas y devuelve los MB liberados. */
suspend fun Context.cleanMemory(packages: List<String>): Long {
    val am = getSystemService(ActivityManager::class.java)

    fun available(): Long {
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.availMem
    }

    val before = available()
    packages.filter { it != packageName }.forEach {
        runCatching { am.killBackgroundProcesses(it) }
    }
    delay(900)
    return ((available() - before) / (1024L * 1024L)).coerceAtLeast(0L)
}

data class TvInput(val id: String, val label: String)

/** Entradas externas de la tele (HDMI, etc.). Vacío si el dispositivo no las expone. */
fun Context.passthroughInputs(): List<TvInput> =
    runCatching {
        val manager = getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
        manager.tvInputList
            .filter { it.isPassthroughInput }
            .map { TvInput(it.id, it.loadLabel(this)?.toString() ?: it.id) }
    }.getOrDefault(emptyList())

fun Context.openInput(input: TvInput) {
    runCatching {
        val uri = TvContract.buildChannelUriForPassthroughInput(input.id)
        startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
