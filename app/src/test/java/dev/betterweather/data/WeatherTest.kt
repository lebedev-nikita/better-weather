package dev.betterweather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class WeatherTest {
    private val now = Instant.parse("2026-10-05T22:30:00Z")
    private val forecast = WeatherForecast(
        place = WeatherPlace.Default,
        timezone = "Asia/Tbilisi",
        current = CurrentWeather(LocalDateTime.parse("2026-10-06T02:30"), 12.0, 11.0, 2.0, 0.0, 80.0, 0, false),
        hours = listOf(
            HourWeather(LocalDateTime.parse("2026-10-05T23:00"), 13.0, 0, 0.0, 2.0, 0.0, false),
            HourWeather(LocalDateTime.parse("2026-10-06T02:00"), 12.0, 0, 0.0, 2.0, 0.0, false),
            HourWeather(LocalDateTime.parse("2026-10-06T12:00"), 23.0, 0, 0.0, 2.0, 6.7, true),
        ),
        days = (0..8).map { DayWeather(LocalDate.of(2026, 10, 5).plusDays(it.toLong()), 10.0, 23.0, 0, 0.0, 6.7, null, null) },
        fetchedAt = now,
    )

    @Test fun `today is determined in the forecast timezone rather than UTC`() {
        assertEquals(LocalDate.of(2026, 10, 6), forecast.localNow(now).toLocalDate())
        assertEquals(2, forecast.todayHours(now).size)
    }

    @Test fun `UV is the current hour rather than the daily maximum`() {
        assertEquals(0.0, forecast.currentUv(now)!!, 0.001)
        assertEquals(6.7, forecast.currentUv(Instant.parse("2026-10-06T08:20:00Z"))!!, 0.001)
        assertNull(forecast.currentUv(Instant.parse("2026-10-07T08:00:00Z")))
    }

    @Test fun `the week excludes past days and is limited to seven`() {
        val week = forecast.upcomingDays(now)
        assertEquals(7, week.size)
        assertEquals(LocalDate.of(2026, 10, 6), week.first().date)
        assertEquals(LocalDate.of(2026, 10, 12), week.last().date)
    }

    @Test fun `missing readings never appear as zero`() {
        assertEquals("—", (null as Double?).degrees())
        assertEquals("—", (null as Double?).percent())
        assertEquals("—", (null as Double?).decimal())
        assertEquals("0°", 0.0.degrees())
        assertEquals("Нет данных", uvDescription(null))
    }

    @Test fun `weather codes include freezing rain snow thunderstorms and unknown values`() {
        assertEquals(WeatherKind.Rain, weatherKind(67))
        assertEquals(WeatherKind.Snow, weatherKind(86))
        assertEquals(WeatherKind.Thunder, weatherKind(99))
        assertEquals(WeatherKind.Unknown, weatherKind(null))
        assertEquals("С", windDirection(359.0))
        assertEquals("Ю", windDirection(180.0))
    }
}
