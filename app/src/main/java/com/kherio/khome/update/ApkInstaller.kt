package com.kherio.khome.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object ApkInstaller {

    const val ACTION_INSTALL_RESULT = "com.kherio.khome.INSTALL_RESULT"

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    /** Abre la pantalla donde se permite a KHome instalar apps. */
    fun openInstallPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun isTrustedHost(host: String): Boolean =
        host == "github.com" ||
            host.endsWith(".github.com") ||
            host.endsWith(".githubusercontent.com")

    /**
     * Descarga el APK de la release a la caché de la app (solo por HTTPS y solo desde GitHub)
     * y, si GitHub facilita el SHA-256 del archivo, lo verifica.
     */
    suspend fun download(
        context: Context,
        release: ReleaseInfo,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val url = URL(release.apkUrl)
        if (url.protocol != "https" || !isTrustedHost(url.host)) {
            throw IOException("URL de descarga no permitida")
        }

        val dir = File(context.cacheDir, "updates").apply {
            mkdirs()
            listFiles()?.forEach { it.delete() }
        }
        val target = File(dir, "KHome-${release.version}.apk")

        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "KHome-updater")
        }
        try {
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            // Tras las redirecciones, el host final también debe ser de GitHub.
            if (conn.url.protocol != "https" || !isTrustedHost(conn.url.host)) {
                throw IOException("Redirección no permitida")
            }

            val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            var downloaded = 0L
            var lastPercent = -1

            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    var read = input.read(buffer)
                    while (read != -1) {
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val percent = (downloaded * 100 / total).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                        read = input.read(buffer)
                    }
                }
            }

            release.sha256?.let { expected ->
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actual.equals(expected, ignoreCase = true)) {
                    target.delete()
                    throw IOException("El SHA-256 del APK no coincide")
                }
            }
            target
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Instala el APK con el instalador de paquetes del sistema. El sistema pedirá confirmación
     * al usuario y, al ser la misma firma, actualizará KHome conservando sus datos.
     */
    fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply { setAppPackageName(context.packageName) }
        val sessionId = installer.createSession(params)

        installer.openSession(sessionId).use { session ->
            session.openWrite("khome.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val intent = Intent(context, InstallReceiver::class.java)
                .setAction(ACTION_INSTALL_RESULT)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
            session.commit(pending.intentSender)
        }
    }
}
