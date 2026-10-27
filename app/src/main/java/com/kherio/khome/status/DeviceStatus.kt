package com.kherio.khome.status

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.Inet4Address
import java.net.HttpURLConnection
import java.net.URL

enum class Connection { ETHERNET, WIFI, NONE }

data class DeviceInfo(
    val connection: Connection,
    val ip: String,
    val storage: String,
    val memory: String,
    val android: String,
    val model: String,
)

/** Datos básicos de la tele: red, almacenamiento, memoria y versión del sistema. */
fun readDeviceInfo(context: Context): DeviceInfo {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork
    val caps = network?.let { cm.getNetworkCapabilities(it) }
    val connection = when {
        caps == null -> Connection.NONE
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Connection.ETHERNET
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Connection.WIFI
        else -> Connection.NONE
    }
    val ip = runCatching {
        cm.getLinkProperties(network)?.linkAddresses
            ?.map { it.address }
            ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    }.getOrNull().orEmpty()

    val storage = runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        Formatter.formatFileSize(context, stat.availableBytes) + " / " +
            Formatter.formatFileSize(context, stat.totalBytes)
    }.getOrDefault("—")

    val memory = runCatching {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        Formatter.formatFileSize(context, info.availMem) + " / " +
            Formatter.formatFileSize(context, info.totalMem)
    }.getOrDefault("—")

    return DeviceInfo(
        connection = connection,
        ip = ip.ifBlank { "—" },
        storage = storage,
        memory = memory,
        android = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        model = listOf(Build.MANUFACTURER, Build.MODEL).filter { it.isNotBlank() }.joinToString(" "),
    )
}

sealed interface SpeedState {
    data object Idle : SpeedState
    data class Running(val mbps: Double, val latencyMs: Long?) : SpeedState
    data class Done(val mbps: Double, val latencyMs: Long?) : SpeedState
    data object Failed : SpeedState
}

/** Test de descarga: baja datos durante unos segundos y mide la velocidad media en Mbps. */
object SpeedTest {
    private val SOURCES = listOf(
        "https://speed.cloudflare.com/__down?bytes=100000000",
        "https://proof.ovh.net/files/100Mb.dat",
    )
    private const val DURATION_MS = 8_000L

    fun run(): Flow<SpeedState> = flow {
        for (source in SOURCES) {
            val ok = runCatching { measure(source) { emit(it) } }.getOrDefault(false)
            if (ok) return@flow
        }
        emit(SpeedState.Failed)
    }.flowOn(Dispatchers.IO)

    private suspend fun measure(url: String, emit: suspend (SpeedState) -> Unit): Boolean {
        val started = System.nanoTime()
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 8_000
        try {
            if (conn.responseCode != 200) return false
            val latency = (System.nanoTime() - started) / 1_000_000
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            val t0 = System.nanoTime()
            var lastEmit = t0
            conn.inputStream.use { input ->
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    val now = System.nanoTime()
                    if ((now - lastEmit) / 1_000_000 >= 250) {
                        lastEmit = now
                        emit(SpeedState.Running(mbps(total, now - t0), latency))
                    }
                    if ((now - t0) / 1_000_000 >= DURATION_MS) break
                }
            }
            val elapsed = System.nanoTime() - t0
            if (total <= 0L || elapsed <= 0L) return false
            emit(SpeedState.Done(mbps(total, elapsed), latency))
            return true
        } finally {
            conn.disconnect()
        }
    }

    private fun mbps(bytes: Long, nanos: Long): Double = bytes * 8.0 / (nanos / 1e9) / 1e6
}
