package com.kherio.khome.news

import androidx.compose.runtime.Immutable
import com.kherio.khome.BuildConfig
import com.kherio.khome.data.AppEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Plataforma de streaming: id de proveedor en TMDB y cómo reconocer su app instalada. */
@Immutable
data class Platform(
    val id: Int,
    val name: String,
    val keywords: List<String>,
    val packages: List<String>,
) {
    fun matches(app: AppEntry): Boolean =
        app.packageName in packages ||
            keywords.any { app.label.contains(it, ignoreCase = true) }
}

@Immutable
data class NewsTitle(val id: Int, val name: String, val posterPath: String, val date: String)

@Immutable
data class ExtraRow(val key: String, val titleRes: Int, val titles: List<NewsTitle>)

@Immutable
data class NewsRow(val platform: Platform, val app: AppEntry, val titles: List<NewsTitle>)

val PLATFORMS = listOf(
    Platform(8, "Netflix", listOf("netflix"), listOf("com.netflix.ninja")),
    Platform(119, "Prime Video", listOf("prime video"), listOf("com.amazon.amazonvideo.livingroom")),
    Platform(337, "Disney+", listOf("disney"), listOf("com.disney.disneyplus")),
    Platform(1899, "Max", listOf("hbo max", "max"), listOf("com.wbd.stream", "com.hbo.hbonow")),
    Platform(350, "Apple TV+", listOf("apple tv"), listOf("com.apple.atve.androidtv.appletv")),
    Platform(149, "Movistar Plus+", listOf("movistar"), emptyList()),
    Platform(1773, "SkyShowtime", listOf("skyshowtime"), emptyList()),
)

/** Novedades por plataforma con la API de TMDB (https://www.themoviedb.org). */
object NewsApi {

    val hasKey: Boolean get() = BuildConfig.TMDB_KEY.isNotBlank()

    /** Últimos estrenos (películas y series) disponibles en la plataforma, en tu país. */
    suspend fun latest(providerId: Int): List<NewsTitle> = withContext(Dispatchers.IO) {
        val region = Locale.getDefault().country.ifBlank { "ES" }
        val language = Locale.getDefault().toLanguageTag()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        fun query(kind: String, dateField: String): List<NewsTitle> {
            val url = "https://api.themoviedb.org/3/discover/$kind" +
                "?api_key=${BuildConfig.TMDB_KEY}" +
                "&with_watch_providers=$providerId&watch_region=$region" +
                "&with_watch_monetization_types=flatrate" +
                "&sort_by=$dateField.desc&$dateField.lte=$today" +
                "&vote_count.gte=5&include_adult=false&language=$language"
            val results = JSONObject(httpGet(url)).optJSONArray("results") ?: return emptyList()
            return (0 until results.length()).mapNotNull { i ->
                val r = results.getJSONObject(i)
                val poster = r.optString("poster_path")
                if (poster.isBlank() || poster == "null") return@mapNotNull null
                NewsTitle(
                    id = r.optInt("id"),
                    name = r.optString("title").ifBlank { r.optString("name") },
                    posterPath = poster,
                    date = r.optString("release_date").ifBlank { r.optString("first_air_date") },
                )
            }
        }

        val movies = runCatching { query("movie", "primary_release_date") }.getOrDefault(emptyList())
        val shows = runCatching { query("tv", "first_air_date") }.getOrDefault(emptyList())
        (movies + shows)
            .distinctBy { it.id to it.name }
            .sortedByDescending { it.date }
            .take(14)
    }

    /** Filas generales de TMDB: tendencias de la semana, próximos estrenos y mejor valoradas. */
    suspend fun trendingWeek(): List<NewsTitle> = general("trending/all/week", "")

    suspend fun upcoming(): List<NewsTitle> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return general("movie/upcoming", "&region=${region()}")
            .filter { it.date >= today }
            .sortedBy { it.date }
    }

    suspend fun topRated(): List<NewsTitle> = general("movie/top_rated", "&region=${region()}")

    private fun region(): String = Locale.getDefault().country.ifBlank { "ES" }

    private suspend fun general(path: String, extra: String): List<NewsTitle> = withContext(Dispatchers.IO) {
        val language = Locale.getDefault().toLanguageTag()
        val url = "https://api.themoviedb.org/3/$path?api_key=${BuildConfig.TMDB_KEY}&language=$language$extra"
        val results = JSONObject(httpGet(url)).optJSONArray("results") ?: return@withContext emptyList()
        (0 until results.length()).mapNotNull { i ->
            val r = results.getJSONObject(i)
            val poster = r.optString("poster_path")
            if (poster.isBlank() || poster == "null") return@mapNotNull null
            NewsTitle(
                id = r.optInt("id"),
                name = r.optString("title").ifBlank { r.optString("name") },
                posterPath = poster,
                date = r.optString("release_date").ifBlank { r.optString("first_air_date") },
            )
        }.distinctBy { it.id to it.name }.take(14)
    }

    /** Fondos (backdrops) de películas en tendencia esta semana, para el fondo del launcher. */
    suspend fun trendingBackdrops(): List<String> = withContext(Dispatchers.IO) {
        val language = Locale.getDefault().toLanguageTag()
        val url = "https://api.themoviedb.org/3/trending/movie/week" +
            "?api_key=${BuildConfig.TMDB_KEY}&language=$language"
        val results = JSONObject(httpGet(url)).optJSONArray("results") ?: return@withContext emptyList()
        (0 until results.length()).mapNotNull { i ->
            val path = results.getJSONObject(i).optString("backdrop_path")
            if (path.isBlank() || path == "null") null else "https://image.tmdb.org/t/p/w780$path"
        }.take(12)
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
        }
        try {
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
