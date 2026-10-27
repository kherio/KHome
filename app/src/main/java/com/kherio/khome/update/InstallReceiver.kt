package com.kherio.khome.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast
import androidx.core.content.IntentCompat

/** Recibe el resultado de la sesión de instalación del sistema. */
class InstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // El sistema necesita que el usuario confirme la instalación.
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                if (confirm != null) {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(confirm) }
                }
            }

            PackageInstaller.STATUS_SUCCESS,
            PackageInstaller.STATUS_FAILURE_ABORTED -> Unit // éxito (la app se reinicia) o cancelado por el usuario

            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                if (!message.isNullOrBlank()) {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
