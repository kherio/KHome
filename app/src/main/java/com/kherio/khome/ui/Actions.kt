package com.kherio.khome.ui

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.net.Uri
import android.provider.Settings
import com.kherio.khome.data.AppEntry

/** Con [fade] la app entra con un fundido (la tarjeta de KHome ya ha ocupado la pantalla). */
fun Context.launchApp(app: AppEntry, fade: Boolean = false) {
    val intent = Intent(Intent.ACTION_MAIN)
        .setClassName(app.packageName, app.activityName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    runCatching {
        if (fade) {
            val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(intent, options.toBundle())
        } else {
            startActivity(intent)
        }
    }
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

/**
 * Abre los ajustes de Bluetooth del propio Android. En Android TV / Google TV esa pantalla la
 * ofrece la app de Ajustes de la tele y su nombre varía según el fabricante, así que se prueban
 * varias rutas, de la más estándar a las específicas; si ninguna existe, abre los Ajustes generales.
 */
fun Context.openBluetoothSettings() {
    val candidates = listOf(
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS).setPackage("com.android.tv.settings"),
        Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.accessories.AddAccessoryActivity"),
        Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.accessories.BluetoothAccessoryActivity"),
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS).setPackage("com.android.settings"),
        Intent().setClassName("com.android.settings", "com.android.settings.bluetooth.BluetoothSettings"),
        Intent(Settings.ACTION_SETTINGS),
    )
    for (intent in candidates) {
        val opened = runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        if (opened) return
    }
}

/** Cierra los procesos en segundo plano de una app. */
fun Context.closeApp(app: AppEntry) {
    runCatching {
        getSystemService(ActivityManager::class.java).killBackgroundProcesses(app.packageName)
    }
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
