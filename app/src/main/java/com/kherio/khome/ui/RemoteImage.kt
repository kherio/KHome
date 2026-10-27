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
    val cache = LruCache<String, ImageBitmap>(80)

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
