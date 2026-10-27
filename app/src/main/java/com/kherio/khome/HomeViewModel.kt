package com.kherio.khome

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kherio.khome.data.AppEntry
import com.kherio.khome.data.AppRepository
import com.kherio.khome.data.PrefsRepository
import com.kherio.khome.data.Settings
import com.kherio.khome.update.ApkInstaller
import com.kherio.khome.update.NoApkException
import com.kherio.khome.update.UpdateChecker
import com.kherio.khome.update.UpdateError
import com.kherio.khome.update.UpdateState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)
    private val prefs = PrefsRepository(app)

    private val allApps = MutableStateFlow<List<AppEntry>>(emptyList())
    private var refreshJob: Job? = null

    /** Se emite cuando el usuario pulsa Home estando ya en el launcher. */
    val homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val settings: StateFlow<Settings> =
        prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    val visibleApps: StateFlow<List<AppEntry>> =
        combine(allApps, settings) { apps, s -> apps.filter { it.packageName !in s.hidden } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites: StateFlow<List<AppEntry>> =
        combine(allApps, settings) { apps, s ->
            s.favorites.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hiddenApps: StateFlow<List<AppEntry>> =
        combine(allApps, settings) { apps, s -> apps.filter { it.packageName in s.hidden } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update: StateFlow<UpdateState> = _update

    val installedVersion: String = UpdateChecker.installedVersion(app)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh(debounceMs = 400)
        }
    }

    init {
        refresh()
        checkForUpdate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(app, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    fun refresh(debounceMs: Long = 0) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            allApps.value = repo.loadApps()
        }
    }

    fun toggleFavorite(pkg: String) {
        viewModelScope.launch {
            val list = settings.value.favorites.toMutableList()
            if (!list.remove(pkg)) list.add(pkg)
            prefs.setFavorites(list)
        }
    }

    fun moveFavorite(pkg: String, delta: Int) {
        viewModelScope.launch {
            val list = settings.value.favorites.toMutableList()
            val from = list.indexOf(pkg)
            if (from < 0) return@launch
            val to = (from + delta).coerceIn(0, list.lastIndex)
            if (from == to) return@launch
            list.removeAt(from)
            list.add(to, pkg)
            prefs.setFavorites(list)
        }
    }

    fun hide(pkg: String) {
        viewModelScope.launch {
            prefs.setHidden(settings.value.hidden + pkg)
            prefs.setFavorites(settings.value.favorites - pkg)
        }
    }

    fun unhide(pkg: String) {
        viewModelScope.launch { prefs.setHidden(settings.value.hidden - pkg) }
    }

    fun setColumns(value: Int) {
        viewModelScope.launch { prefs.setColumns(value) }
    }

    fun setUse24h(value: Boolean) {
        viewModelScope.launch { prefs.setUse24h(value) }
    }

    /**
     * Consulta la última Release de GitHub. Al arrancar se hace en silencio (si falla, no se
     * muestra nada); con [manual] = true se informa del resultado en Ajustes.
     */
    fun checkForUpdate(manual: Boolean = false) {
        val current = _update.value
        if (current is UpdateState.Checking || current is UpdateState.Downloading) return

        viewModelScope.launch {
            if (manual) _update.value = UpdateState.Checking
            _update.value = try {
                val release = UpdateChecker.fetchLatest()
                when {
                    release == null -> UpdateState.UpToDate
                    UpdateChecker.isNewer(release.version, installedVersion) -> UpdateState.Available(release)
                    else -> UpdateState.UpToDate
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: NoApkException) {
                if (manual) UpdateState.Failed(UpdateError.NO_APK) else UpdateState.Idle
            } catch (e: Exception) {
                if (manual) UpdateState.Failed(UpdateError.NETWORK) else UpdateState.Idle
            }
        }
    }

    /** Descarga el APK de la última versión y lanza la instalación del sistema. */
    fun installUpdate() {
        val release = when (val s = _update.value) {
            is UpdateState.Available -> s.release
            is UpdateState.Failed -> s.release
            else -> null
        } ?: return

        val app = getApplication<Application>()
        if (!ApkInstaller.canInstall(app)) {
            ApkInstaller.openInstallPermission(app)
            _update.value = UpdateState.Failed(UpdateError.PERMISSION, release)
            return
        }

        viewModelScope.launch {
            _update.value = UpdateState.Downloading(release, 0)
            val apk = try {
                ApkInstaller.download(app, release) { percent ->
                    _update.value = UpdateState.Downloading(release, percent)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _update.value = UpdateState.Failed(UpdateError.DOWNLOAD, release)
                return@launch
            }

            _update.value = try {
                ApkInstaller.install(app, apk)
                // Si el usuario cancela la confirmación del sistema, puede volver a intentarlo.
                UpdateState.Available(release)
            } catch (e: Exception) {
                UpdateState.Failed(UpdateError.INSTALL, release)
            }
        }
    }

    override fun onCleared() {
        runCatching { getApplication<Application>().unregisterReceiver(packageReceiver) }
        super.onCleared()
    }
}
