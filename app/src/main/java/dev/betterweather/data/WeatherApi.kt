package dev.betterweather.data

import androidx.core.net.toUri
import dev.betterweather.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Open-Meteo's open-access endpoints require no account or API key. */
class WeatherApi {
    suspend fun forecast(place: WeatherPlace): Pair<WeatherForecast, String> {
        val url = "https://api.open-meteo.com/v1/forecast".toUri().buildUpon()
            .appendQueryParameter("latitude", place.latitude.toString())
            .appendQueryParameter("longitude", place.longitude.toString())
            .appendQueryParameter("current", "temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m,is_day")
            .appendQueryParameter("hourly", "temperature_2m,weather_code,precipitation_probability,wind_speed_10m,uv_index,is_day")
            .appendQueryParameter("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,uv_index_max,sunrise,sunset")
            .appendQueryParameter("wind_speed_unit", "ms")
            .appendQueryParameter("timezone", "auto")
            .appendQueryParameter("forecast_days", "7")
            .build().toString()
        val raw = get(url)
        return parseForecast(JSONObject(raw), place, Instant.now()) to raw
    }

    suspend fun search(query: String): List<WeatherPlace> {
        if (query.trim().length < 2) return emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search".toUri().buildUpon()
            .appendQueryParameter("name", query.trim())
            .appendQueryParameter("count", "10")
            .appendQueryParameter("language", "ru")
            .appendQueryParameter("format", "json")
            .build().toString()
        val results = JSONObject(get(url)).optJSONArray("results") ?: return emptyList()
        return List(results.length()) { index ->
            val item = results.getJSONObject(index)
            WeatherPlace(item.getString("name"), item.getDouble("latitude"), item.getDouble("longitude"), item.optString("country"), item.optString("admin1"))
        }
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        ensureActive()
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 12_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "BetterWeather/${BuildConfig.VERSION_NAME} Android")
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException(if (code == 429) "Слишком много запросов. Попробуйте чуть позже." else "Сервис погоды недоступен (HTTP $code).")
            }
            connection.inputStream.bufferedReader().use { it.readText() }.also { ensureActive() }
        } finally {
            connection.disconnect()
        }
    }
}

fun parseForecast(root: JSONObject, place: WeatherPlace, fetchedAt: Instant): WeatherForecast {
    val timezone = root.getString("timezone")
    ZoneId.of(timezone) // Validate before committing the response to the cache.
    val current = root.getJSONObject("current")
    val hourly = root.getJSONObject("hourly")
    val daily = root.getJSONObject("daily")
    val hourTimes = hourly.getJSONArray("time")
    val dayTimes = daily.getJSONArray("time")
    return WeatherForecast(
        place = place,
        timezone = timezone,
        current = CurrentWeather(
            LocalDateTime.parse(current.getString("time")), current.number("temperature_2m"),
            current.number("apparent_temperature"), current.number("wind_speed_10m"),
            current.number("wind_direction_10m"), current.number("relative_humidity_2m"),
            current.number("weather_code")?.toInt(), current.optInt("is_day", 1) == 1,
        ),
        hours = List(hourTimes.length()) { index ->
            HourWeather(
                LocalDateTime.parse(hourTimes.getString(index)), hourly.numberAt("temperature_2m", index),
                hourly.numberAt("weather_code", index)?.toInt(), hourly.numberAt("precipitation_probability", index),
                hourly.numberAt("wind_speed_10m", index), hourly.numberAt("uv_index", index),
                hourly.numberAt("is_day", index) != 0.0,
            )
        },
        days = List(dayTimes.length()) { index ->
            DayWeather(
                LocalDate.parse(dayTimes.getString(index)), daily.numberAt("temperature_2m_min", index),
                daily.numberAt("temperature_2m_max", index), daily.numberAt("weather_code", index)?.toInt(),
                daily.numberAt("precipitation_probability_max", index), daily.numberAt("uv_index_max", index),
                daily.timeAt("sunrise", index), daily.timeAt("sunset", index),
            )
        },
        fetchedAt = fetchedAt,
    )
}

private fun JSONObject.number(key: String): Double? = if (isNull(key)) null else optDouble(key).takeIf { it.isFinite() }
private fun JSONObject.numberAt(key: String, index: Int): Double? = optJSONArray(key)?.let { values ->
    if (values.isNull(index)) null else values.optDouble(index).takeIf { it.isFinite() }
}
private fun JSONObject.timeAt(key: String, index: Int): LocalDateTime? = optJSONArray(key)?.optString(index)
    ?.takeIf { it.isNotBlank() && it != "null" }?.let { LocalDateTime.parse(it) }
