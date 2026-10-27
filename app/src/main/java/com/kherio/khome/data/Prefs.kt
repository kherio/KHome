package com.kherio.khome.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "khome_prefs")

data class Settings(
    /** Orden personalizado de las apps (nombres de paquete). Las que no estén van al final por nombre. */
    val order: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    /** Tarjetas visibles por pantalla (3 a 6). */
    val perPage: Int = 4,
    val use24h: Boolean = true,
    /** 0 = sistema, 1 = claro, 2 = oscuro. */
    val theme: Int = 2,
    /** Índice del fondo del tema oscuro. */
    val background: Int = 0,
    /** Botones inferiores con relieve (true) o planos (false). */
    val raisedButtons: Boolean = false,
    val weatherEnabled: Boolean = true,
    val cityName: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    /** Apps lanzadas desde KHome, la más reciente primero. */
    val recent: List<String> = emptyList(),
    val showRecent: Boolean = true,
    /** Color de acento (índice de paleta). */
    val accent: Int = 0,
    /** 0 = icono, 1 = banner de TV cuando la app lo tiene. */
    val cardStyle: Int = 0,
    val showNews: Boolean = true,
    /** 0 manchas animadas, 1 fondos de películas (TMDB), 2 liso. */
    val bgMode: Int = 0,
    /** Teñir el halo y el fondo con el color de la app enfocada. */
    val tintByApp: Boolean = true,
    val showExtras: Boolean = true,
    /** Paquete de la app que abre los títulos de las filas generales; vacío = preguntar. */
    val titleApp: String = "",
)

class PrefsRepository(private val context: Context) {

    private object Keys {
        val ORDER = stringPreferencesKey("order")
        val HIDDEN = stringSetPreferencesKey("hidden")
        val PER_PAGE = intPreferencesKey("per_page")
        val USE_24H = booleanPreferencesKey("use_24h")
        val THEME = intPreferencesKey("theme")
        val BACKGROUND = intPreferencesKey("background")
        val RAISED = booleanPreferencesKey("raised_buttons")
        val WEATHER = booleanPreferencesKey("weather_enabled")
        val CITY = stringPreferencesKey("city_name")
        val LAT = doublePreferencesKey("lat")
        val LON = doublePreferencesKey("lon")
        val RECENT = stringPreferencesKey("recent")
        val SHOW_RECENT = booleanPreferencesKey("show_recent")
        val ACCENT = intPreferencesKey("accent")
        val CARD_STYLE = intPreferencesKey("card_style")
        val SHOW_NEWS = booleanPreferencesKey("show_news")
        val BG_MODE = intPreferencesKey("bg_mode")
        val TINT = booleanPreferencesKey("tint_by_app")
        val EXTRAS = booleanPreferencesKey("show_extras")
        val TITLE_APP = stringPreferencesKey("title_app")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            order = p[Keys.ORDER]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            hidden = p[Keys.HIDDEN] ?: emptySet(),
            perPage = (p[Keys.PER_PAGE] ?: 4).coerceIn(3, 6),
            use24h = p[Keys.USE_24H] ?: true,
            theme = p[Keys.THEME] ?: 2,
            background = p[Keys.BACKGROUND] ?: 0,
            raisedButtons = p[Keys.RAISED] ?: false,
            weatherEnabled = p[Keys.WEATHER] ?: true,
            cityName = p[Keys.CITY] ?: "",
            lat = p[Keys.LAT] ?: 0.0,
            lon = p[Keys.LON] ?: 0.0,
            recent = p[Keys.RECENT]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            showRecent = p[Keys.SHOW_RECENT] ?: true,
            accent = p[Keys.ACCENT] ?: 0,
            cardStyle = p[Keys.CARD_STYLE] ?: 0,
            showNews = p[Keys.SHOW_NEWS] ?: true,
            bgMode = p[Keys.BG_MODE] ?: 0,
            tintByApp = p[Keys.TINT] ?: true,
            showExtras = p[Keys.EXTRAS] ?: true,
            titleApp = p[Keys.TITLE_APP] ?: "",
        )
    }

    suspend fun setOrder(list: List<String>) {
        context.dataStore.edit { it[Keys.ORDER] = list.joinToString(",") }
    }

    suspend fun setHidden(set: Set<String>) {
        context.dataStore.edit { it[Keys.HIDDEN] = set }
    }

    suspend fun setPerPage(value: Int) {
        context.dataStore.edit { it[Keys.PER_PAGE] = value }
    }

    suspend fun setUse24h(value: Boolean) {
        context.dataStore.edit { it[Keys.USE_24H] = value }
    }

    suspend fun setTheme(value: Int) {
        context.dataStore.edit { it[Keys.THEME] = value }
    }

    suspend fun setBackground(value: Int) {
        context.dataStore.edit { it[Keys.BACKGROUND] = value }
    }

    suspend fun setRaisedButtons(value: Boolean) {
        context.dataStore.edit { it[Keys.RAISED] = value }
    }

    suspend fun setWeatherEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.WEATHER] = value }
    }

    suspend fun setRecent(list: List<String>) {
        context.dataStore.edit { it[Keys.RECENT] = list.joinToString(",") }
    }

    suspend fun setShowExtras(value: Boolean) {
        context.dataStore.edit { it[Keys.EXTRAS] = value }
    }

    suspend fun setTitleApp(value: String) {
        context.dataStore.edit { it[Keys.TITLE_APP] = value }
    }

    suspend fun setBgMode(value: Int) {
        context.dataStore.edit { it[Keys.BG_MODE] = value }
    }

    suspend fun setTintByApp(value: Boolean) {
        context.dataStore.edit { it[Keys.TINT] = value }
    }

    suspend fun setShowNews(value: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_NEWS] = value }
    }

    suspend fun setAccent(value: Int) {
        context.dataStore.edit { it[Keys.ACCENT] = value }
    }

    suspend fun setCardStyle(value: Int) {
        context.dataStore.edit { it[Keys.CARD_STYLE] = value }
    }

    suspend fun setShowRecent(value: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_RECENT] = value }
    }

    suspend fun setCity(name: String, lat: Double, lon: Double) {
        context.dataStore.edit {
            it[Keys.CITY] = name
            it[Keys.LAT] = lat
            it[Keys.LON] = lon
        }
    }
}
