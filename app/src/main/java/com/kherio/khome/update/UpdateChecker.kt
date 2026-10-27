package com.kherio.khome.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val LATEST_URL = "https://api.github.com/repos/kherio/KHome/releases/latest"

    @Suppress("DEPRECATION")
    fun installedVersion(context: Context): String =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0"

    /**
     * Devuelve la última Release publicada (no borradores ni pre-releases),
     * o null si el repo todavía no tiene ninguna.
     * Lanza [NoApkException] si la release no trae APK, o [IOException] si falla la red.
     */
    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val conn = (URL(LATEST_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "KHome-updater")
        }
        try {
            when (conn.responseCode) {
                200 -> parse(conn.inputStream.bufferedReader().use { it.readText() })
                404 -> null
                else -> throw IOException("HTTP ${conn.responseCode}")
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(text: String): ReleaseInfo {
        val obj = JSONObject(text)
        val assets = obj.optJSONArray("assets") ?: throw NoApkException()
        val apk = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            ?: throw NoApkException()

        return ReleaseInfo(
            version = obj.optString("tag_name").removePrefix("v"),
            apkUrl = apk.getString("browser_download_url"),
            apkSize = apk.optLong("size"),
            sha256 = apk.optString("digest")
                .takeIf { it.startsWith("sha256:") }
                ?.removePrefix("sha256:"),
        )
    }

    /** Compara versiones tipo 1.2.3 (ignora sufijos como -beta). */
    fun isNewer(remote: String, local: String): Boolean {
        val r = numbers(remote)
        val l = numbers(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun numbers(version: String): List<Int> =
        version.trim()
            .removePrefix("v")
            .substringBefore('-')
            .substringBefore('+')
            .split('.')
            .map { it.toIntOrNull() ?: 0 }
}
