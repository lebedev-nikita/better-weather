package dev.betterweather.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

data class WeatherPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String = "",
    val region: String = "",
) {
    val subtitle: String get() = listOf(region, country).filter { it.isNotBlank() && it != name }.distinct().joinToString(", ")

    companion object {
        val Default = WeatherPlace("Тбилиси", 41.7151, 44.8271, "Грузия")
    }
}

data class CurrentWeather(
    val time: LocalDateTime,
    val temperature: Double?,
    val feelsLike: Double?,
    val windSpeed: Double?,
    val windDirection: Double?,
    val humidity: Double?,
    val code: Int?,
    val isDay: Boolean,
)

data class HourWeather(
    val time: LocalDateTime,
    val temperature: Double?,
    val code: Int?,
    val precipitationProbability: Double?,
    val windSpeed: Double?,
    val uv: Double?,
    val isDay: Boolean,
)

data class DayWeather(
    val date: LocalDate,
    val low: Double?,
    val high: Double?,
    val code: Int?,
    val precipitationProbability: Double?,
    val uvMax: Double?,
    val sunrise: LocalDateTime?,
    val sunset: LocalDateTime?,
)

data class WeatherForecast(
    val place: WeatherPlace,
    val timezone: String,
    val current: CurrentWeather,
    val hours: List<HourWeather>,
    val days: List<DayWeather>,
    val fetchedAt: Instant,
) {
    fun localNow(now: Instant = Instant.now()): LocalDateTime = LocalDateTime.ofInstant(now, ZoneId.of(timezone))
    fun todayHours(now: Instant = Instant.now()): List<HourWeather> = hours.filter { it.time.toLocalDate() == localNow(now).toLocalDate() }
    fun upcomingDays(now: Instant = Instant.now()): List<DayWeather> = days.filter { !it.date.isBefore(localNow(now).toLocalDate()) }.take(7)
    fun currentUv(now: Instant = Instant.now()): Double? {
        val hour = localNow(now).withMinute(0).withSecond(0).withNano(0)
        return hours.firstOrNull { it.time == hour }?.uv
    }
}

fun Double?.degrees(): String = this?.let { "${it.roundToInt()}°" } ?: "—"
fun Double?.decimal(): String = this?.let { String.format(Locale.forLanguageTag("ru"), "%.1f", it) } ?: "—"
fun Double?.percent(): String = this?.let { "${it.roundToInt()}%" } ?: "—"
fun windDirection(degrees: Double?): String = degrees?.let {
    listOf("С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ")[((it / 45).roundToInt() % 8 + 8) % 8]
} ?: "—"

enum class WeatherKind { Clear, PartlyCloudy, Cloudy, Fog, Rain, Snow, Thunder, Unknown }

fun weatherKind(code: Int?): WeatherKind = when (code) {
    0 -> WeatherKind.Clear
    1, 2 -> WeatherKind.PartlyCloudy
    3 -> WeatherKind.Cloudy
    45, 48 -> WeatherKind.Fog
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherKind.Rain
    71, 73, 75, 77, 85, 86 -> WeatherKind.Snow
    95, 96, 99 -> WeatherKind.Thunder
    else -> WeatherKind.Unknown
}

fun weatherDescription(code: Int?, isDay: Boolean = true): String = when (weatherKind(code)) {
    WeatherKind.Clear -> if (isDay) "Ясно" else "Ясная ночь"
    WeatherKind.PartlyCloudy -> "Переменная облачность"
    WeatherKind.Cloudy -> "Облачно"
    WeatherKind.Fog -> "Туман"
    WeatherKind.Rain -> "Дождь"
    WeatherKind.Snow -> "Снег"
    WeatherKind.Thunder -> "Гроза"
    WeatherKind.Unknown -> "Нет данных"
}

fun uvDescription(uv: Double?): String = when {
    uv == null -> "Нет данных"
    uv < 3 -> "Низкий"
    uv < 6 -> "Умеренный"
    uv < 8 -> "Высокий"
    uv < 11 -> "Очень высокий"
    else -> "Экстремальный"
}
