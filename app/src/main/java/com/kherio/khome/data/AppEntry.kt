package com.kherio.khome.data

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

@Immutable
data class AppEntry(
    val packageName: String,
    val label: String,
    val activityName: String,
    val image: ImageBitmap,
    /** true si la imagen es el banner 16:9 de la app; false si es el icono. */
    val isBanner: Boolean,
)
