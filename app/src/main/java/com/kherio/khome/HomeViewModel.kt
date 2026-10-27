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
import com.kherio.khome.news.NewsApi
import com.kherio.khome.news.NewsRow
import com.kherio.khome.news.NewsTitle
import com.kherio.khome.news.PLATFORMS
import com.kherio.khome.update.ApkInstaller
import com.kherio.khome.update.NoApkException
import com.kherio.khome.update.UpdateChecker
import com.kherio.khome.update.UpdateError
import com.kherio.khome.update.UpdateState
import com.kherio.khome.weather.CityResult
import com.kherio.khome.weather.WeatherApi
import com.kherio.khome.weather.WeatherData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)
    private val prefs = PrefsRepository(app)

    private val allApps = MutableStateFlow<List<AppEntry>>(emptyList())
    private var refreshJob: Job? = null

    /** Se emite cuando el usuario pulsa Home estando ya en el launcher. */
    val homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val settings: StateFlow<Settings> =
        prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** Apps visibles en el orden elegido por el usuario (las nuevas, al final por nombre). */
    val visibleApps: StateFlow<List<AppEntry>> =
        combine(allApps, settings) { apps, s ->
            val rank = s.order.withIndex().associate { it.value to it.index }
            apps.filter { it.packageName !in s.hidden }
                .sortedWith(
                    compareBy<AppEntry> { rank[it.packageName] ?: Int.MAX_VALUE }
                        .thenBy { it.label.lowercase(Locale.getDefault()) },
                )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hiddenApps: StateFlow<List<AppEntry>> =
        combine(allApps, settings) { apps, s -> apps.filter { it.packageName in s.hidden } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Todas las apps instaladas (visibles u ocultas), para elegir cuáles salen en el escritorio. */
    val installedApps: StateFlow<List<AppEntry>> = allApps

    /** Apps visibles lanzadas hace poco, la más reciente primero. */
    val recentApps: StateFlow<List<AppEntry>> =
        combine(visibleApps, settings) { apps, s ->
            val byPkg = apps.associateBy { it.packageName }
            s.recent.mapNotNull { byPkg[it] }.take(10)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun recordLaunch(pkg: String) {
        viewModelScope.launch {
            prefs.setRecent((listOf(pkg) + settings.value.recent.filter { it != pkg }).take(12))
        }
    }

    fun setShowNews(value: Boolean) {
        viewModelScope.launch { prefs.setShowNews(value) }
    }

    fun setAccent(value: Int) {
        viewModelScope.launch { prefs.setAccent(value) }
    }

    fun setCardStyle(value: Int) {
        viewModelScope.launch { prefs.setCardStyle(value) }
    }

    fun setShowRecent(value: Boolean) {
        viewModelScope.launch { prefs.setShowRecent(value) }
    }

    fun allPackages(): List<String> = allApps.value.map { it.packageName }

    /** Muestra u oculta una app del escritorio. */
    fun setAppVisible(pkg: String, visible: Boolean) {
        viewModelScope.launch {
            val hidden = settings.value.hidden
            prefs.setHidden(if (visible) hidden - pkg else hidden + pkg)
        }
    }

    // --- Novedades por plataforma -------------------------------------------

    private val newsByPlatform = MutableStateFlow<Map<Int, List<NewsTitle>>>(emptyMap())

    /** Una fila por cada plataforma que tengas instalada y de la que haya novedades. */
    val newsRows: StateFlow<List<NewsRow>> =
        combine(allApps, settings, newsByPlatform) { apps, s, news ->
            if (!s.showNews || !NewsApi.hasKey) {
                emptyList()
            } else {
                PLATFORMS.mapNotNull { platform ->
                    val app = apps.firstOrNull { platform.matches(it) } ?: return@mapNotNull null
                    val titles = news[platform.id].orEmpty()
                    if (titles.isEmpty()) null else NewsRow(platform, app, titles)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun startNewsLoop() {
        if (!NewsApi.hasKey) return
        viewModelScope.launch {
            combine(allApps, settings.map { it.showNews }.distinctUntilChanged()) { apps, show ->
                if (!show) emptyList() else PLATFORMS.filter { p -> apps.any { p.matches(it) } }.map { it.id }
            }
                .distinctUntilChanged()
                .collectLatest { ids ->
                    while (ids.isNotEmpty()) {
                        for (id in ids) {
                            val titles = runCatching { NewsApi.latest(id) }.getOrNull()
                            if (!titles.isNullOrEmpty()) newsByPlatform.value = newsByPlatform.value + (id to titles)
                        }
                        delay(6 * 60 * 60_000L)
                    }
                }
        }
    }

    // --- Tiempo -------------------------------------------------------------

    private val _weather = MutableStateFlow<WeatherData?>(null)
    val weather: StateFlow<WeatherData?> = _weather

    private val _cityResults = MutableStateFlow<List<CityResult>>(emptyList())
    val cityResults: StateFlow<List<CityResult>> = _cityResults

    // --- Actualizaciones ----------------------------------------------------

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
        startWeatherLoop()
        startNewsLoop()
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

    private fun startWeatherLoop() {
        viewModelScope.launch {
            settings
                .map { Triple(it.weatherEnabled, it.lat, it.lon) }
                .distinctUntilChanged()
                .collectLatest { (enabled, lat, lon) ->
                    if (!enabled || (lat == 0.0 && lon == 0.0)) {
                        _weather.value = null
                        return@collectLatest
                    }
                    while (true) {
                        val data = runCatching { WeatherApi.fetch(lat, lon) }.getOrNull()
                        if (data != null) _weather.value = data
                        delay(if (data != null) 30 * 60_000L else 5 * 60_000L)
                    }
                }
        }
    }

    // --- Apps ---------------------------------------------------------------

    /** Mueve una app [delta] posiciones dentro de la lista visible y guarda el nuevo orden. */
    fun moveApp(pkg: String, delta: Int) {
        viewModelScope.launch {
            val list = visibleApps.value.map { it.packageName }.toMutableList()
            val from = list.indexOf(pkg)
            if (from < 0) return@launch
            val to = (from + delta).coerceIn(0, list.lastIndex)
            if (from == to) return@launch
            list.add(to, list.removeAt(from))
            prefs.setOrder(list)
        }
    }

    fun hide(pkg: String) {
        viewModelScope.launch { prefs.setHidden(settings.value.hidden + pkg) }
    }

    fun unhide(pkg: String) {
        viewModelScope.launch { prefs.setHidden(settings.value.hidden - pkg) }
    }

    // --- Ajustes ------------------------------------------------------------

    fun setTheme(value: Int) {
        viewModelScope.launch { prefs.setTheme(value) }
    }

    fun setBackground(value: Int) {
        viewModelScope.launch { prefs.setBackground(value) }
    }

    fun setRaisedButtons(value: Boolean) {
        viewModelScope.launch { prefs.setRaisedButtons(value) }
    }

    fun setUse24h(value: Boolean) {
        viewModelScope.launch { prefs.setUse24h(value) }
    }

    fun setPerPage(value: Int) {
        viewModelScope.launch { prefs.setPerPage(value) }
    }

    fun setWeatherEnabled(value: Boolean) {
        viewModelScope.launch { prefs.setWeatherEnabled(value) }
    }

    fun searchCity(query: String) {
        viewModelScope.launch {
            _cityResults.value = runCatching { WeatherApi.searchCities(query) }.getOrDefault(emptyList())
        }
    }

    fun selectCity(city: CityResult) {
        viewModelScope.launch {
            prefs.setCity(city.name, city.lat, city.lon)
            _cityResults.value = emptyList()
        }
    }

    // --- Actualizaciones ----------------------------------------------------

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
