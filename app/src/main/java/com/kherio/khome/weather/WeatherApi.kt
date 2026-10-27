package com.kherio.khome.weather

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.kherio.khome.R
import com.kherio.khome.ui.WeatherKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@Immutable
data class DayForecast(val dayName: String, val max: Int, val code: Int)

@Immutable
data class WeatherData(
    val temp: Int,
    val code: Int,
    val humidity: Int,
    val windKmh: Int,
    val max: Int,
    val min: Int,
    val rainPct: Int,
    val forecast: List<DayForecast>,
)

@Immutable
data class CityResult(val name: String, val label: String, val lat: Double, val lon: Double)

fun weatherKind(code: Int): WeatherKind = when (code) {
    0 -> WeatherKind.CLEAR
    1, 2 -> WeatherKind.PARTLY
    3 -> WeatherKind.CLOUDY
    45, 48 -> WeatherKind.FOG
    in 51..67, in 80..82 -> WeatherKind.RAIN
    in 71..77, 85, 86 -> WeatherKind.SNOW
    in 95..99 -> WeatherKind.STORM
    else -> WeatherKind.CLOUDY
}

@StringRes
fun weatherDescription(code: Int): Int = when (code) {
    0 -> R.string.w_clear
    1 -> R.string.w_mostly_clear
    2 -> R.string.w_partly_cloudy
    3 -> R.string.w_cloudy
    45, 48 -> R.string.w_fog
    in 51..57 -> R.string.w_drizzle
    in 61..65 -> R.string.w_rain
    66, 67 -> R.string.w_freezing_rain
    in 71..75 -> R.string.w_snow
    77 -> R.string.w_snow_grains
    in 80..82 -> R.string.w_showers
    85, 86 -> R.string.w_snow_showers
    95 -> R.string.w_thunder
    in 96..99 -> R.string.w_thunder_hail
    else -> R.string.w_cloudy
}

/** Cliente de Open-Meteo (gratuito y sin clave): tiempo actual, previsión y búsqueda de ciudades. */
object WeatherApi {

    suspend fun fetch(lat: Double, lon: Double): WeatherData = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&forecast_days=4&timezone=auto&wind_speed_unit=kmh"

        val json = JSONObject(httpGet(url))
        val current = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")

        val times = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val maxs = daily.getJSONArray("temperature_2m_max")
        val mins = daily.getJSONArray("temperature_2m_min")
        val rain = daily.getJSONArray("precipitation_probability_max")

        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val locale = Locale.getDefault()

        val forecast = (1 until times.length()).map { i ->
            val date = parser.parse(times.getString(i))
            val dayName = date?.let { dayFormat.format(it) }.orEmpty()
                .replaceFirstChar { it.titlecase(locale) }
            DayForecast(dayName, maxs.rounded(i), codes.optInt(i, 3))
        }

        WeatherData(
            temp = current.optDouble("temperature_2m").roundOrZero(),
            code = current.optInt("weather_code", 3),
            humidity = current.optDouble("relative_humidity_2m").roundOrZero(),
            windKmh = current.optDouble("wind_speed_10m").roundOrZero(),
            max = maxs.rounded(0),
            min = mins.rounded(0),
            rainPct = rain.optInt(0, 0),
            forecast = forecast,
        )
    }

    suspend fun searchCities(query: String): List<CityResult> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList()

        val url = "https://geocoding-api.open-meteo.com/v1/search?name=" +
            URLEncoder.encode(q, "UTF-8") + "&count=5&language=es&format=json"
        val results = JSONObject(httpGet(url)).optJSONArray("results")
            ?: return@withContext emptyList()

        (0 until results.length()).map { results.getJSONObject(it) }.map { r ->
            val name = r.optString("name")
            val label = listOf(name, r.optString("admin1"), r.optString("country"))
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(", ")
            CityResult(name, label, r.getDouble("latitude"), r.getDouble("longitude"))
        }
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "KHome")
        }
        try {
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun Double.roundOrZero(): Int = if (isNaN()) 0 else roundToInt()

    private fun JSONArray.rounded(index: Int): Int = optDouble(index).roundOrZero()
}
