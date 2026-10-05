package dev.betterweather.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant

/** Stores one atomic snapshot: the selected city and its forecast always agree. */
class WeatherRepository(context: Context, private val api: WeatherApi = WeatherApi()) {
    private val preferences = context.applicationContext.getSharedPreferences("weather", Context.MODE_PRIVATE)

    fun cached(): WeatherForecast? = runCatching {
        val snapshot = JSONObject(preferences.getString("snapshot", null) ?: return null)
        parseForecast(snapshot.getJSONObject("forecast"), snapshot.getJSONObject("place").toPlace(), Instant.ofEpochMilli(snapshot.getLong("fetched_at")))
    }.getOrNull()

    fun selectedPlace(): WeatherPlace = cached()?.place ?: WeatherPlace.Default

    suspend fun refresh(place: WeatherPlace): WeatherForecast = lock.withLock { fetchAndSave(place) }

    /** A queued widget refresh must not change a city selected while the request was waiting. */
    suspend fun refreshSelected(): WeatherForecast = lock.withLock { fetchAndSave(selectedPlace()) }

    private suspend fun fetchAndSave(place: WeatherPlace): WeatherForecast {
        val (forecast, raw) = api.forecast(place)
        withContext(Dispatchers.IO) {
            val snapshot = JSONObject().put("place", place.toJson())
                .put("forecast", JSONObject(raw)).put("fetched_at", forecast.fetchedAt.toEpochMilli())
            check(preferences.edit().putString("snapshot", snapshot.toString()).commit()) { "Не удалось сохранить прогноз." }
        }
        return forecast
    }

    suspend fun search(query: String): List<WeatherPlace> = api.search(query)

    companion object { private val lock = Mutex() }
}

private fun WeatherPlace.toJson() = JSONObject().put("name", name).put("latitude", latitude)
    .put("longitude", longitude).put("country", country).put("region", region)
private fun JSONObject.toPlace() = WeatherPlace(getString("name"), getDouble("latitude"), getDouble("longitude"), optString("country"), optString("region"))
