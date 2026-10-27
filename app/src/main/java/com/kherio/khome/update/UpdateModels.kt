package com.kherio.khome.update

import androidx.compose.runtime.Immutable

@Immutable
data class ReleaseInfo(
    /** Versión sin la "v" inicial, por ejemplo "0.2.0". */
    val version: String,
    val apkUrl: String,
    val apkSize: Long,
    /** SHA-256 en hexadecimal si GitHub lo facilita en el asset; null si no. */
    val sha256: String?,
)

enum class UpdateError { NETWORK, NO_APK, DOWNLOAD, INSTALL, PERMISSION }

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val release: ReleaseInfo, val percent: Int) : UpdateState
    data class Failed(val error: UpdateError, val release: ReleaseInfo? = null) : UpdateState
}

/** La Release existe pero no trae ningún archivo .apk. */
class NoApkException : Exception("La release no incluye APK")
