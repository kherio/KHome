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

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh(debounceMs = 400)
        }
    }

    init {
        refresh()
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

    override fun onCleared() {
        runCatching { getApplication<Application>().unregisterReceiver(packageReceiver) }
        super.onCleared()
    }
}
