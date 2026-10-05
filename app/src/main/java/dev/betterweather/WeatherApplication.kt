package dev.betterweather

import android.app.Application
import dev.betterweather.data.WeatherRepository

class WeatherApplication : Application() {
    val repository by lazy { WeatherRepository(this) }
}
