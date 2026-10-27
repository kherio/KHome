package com.kherio.khome.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.kherio.khome.data.AppEntry

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

fun Context.openSystemSettings() {
    runCatching {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

fun Context.openHomeSettings() {
    runCatching {
        startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
