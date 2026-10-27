package com.kherio.khome.data

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

@Immutable
data class AppEntry(
    val packageName: String,
    val label: String,
    val activityName: String,
    val icon: ImageBitmap,
    /** Banner de TV (16:9) si la app lo trae. */
    val banner: ImageBitmap? = null,
)
