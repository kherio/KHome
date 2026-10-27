package com.kherio.khome.ui

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Cargador de imágenes mínimo con caché en memoria (suficiente para los carteles). */
private object ImageCache {
    // Límite por memoria (40 MB), no por número de imágenes: los fondos pesan bastante más que un cartel.
    val cache = object : LruCache<String, ImageBitmap>(40 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    suspend fun load(url: String): ImageBitmap? {
        cache.get(url)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                try {
                    if (conn.responseCode != 200) return@runCatching null
                    BitmapFactory.decodeStream(conn.inputStream)?.asImageBitmap()
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()?.also { cache.put(url, it) }
        }
    }
}

@Composable
fun rememberRemoteImage(url: String): ImageBitmap? {
    val image by produceState<ImageBitmap?>(initialValue = ImageCache.cache.get(url), url) {
        if (value == null) value = ImageCache.load(url)
    }
    return image
}
