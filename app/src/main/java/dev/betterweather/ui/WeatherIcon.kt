package dev.betterweather.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.betterweather.R
import dev.betterweather.data.WeatherKind
import dev.betterweather.data.weatherDescription
import dev.betterweather.data.weatherKind

/** Bundled Weather Icons artwork; shared by Compose and the RemoteViews widget. */
@DrawableRes
fun weatherIconResource(code: Int?, isDay: Boolean = true): Int? = when (weatherKind(code)) {
    WeatherKind.Clear -> if (isDay) R.drawable.weather_clear_day else R.drawable.weather_clear_night
    WeatherKind.PartlyCloudy -> if (isDay) R.drawable.weather_cloudy_1_day else R.drawable.weather_cloudy_1_night
    WeatherKind.Cloudy -> R.drawable.weather_cloudy
    WeatherKind.Fog -> if (isDay) R.drawable.weather_fog_day else R.drawable.weather_fog_night
    WeatherKind.Rain -> if (code in 51..57) R.drawable.weather_rainy_1 else R.drawable.weather_rainy_3
    WeatherKind.Snow -> R.drawable.weather_snowy_3
    WeatherKind.Thunder -> R.drawable.weather_thunderstorms
    WeatherKind.Unknown -> null
}

@Composable
fun WeatherIcon(code: Int?, modifier: Modifier = Modifier, isDay: Boolean = true) {
    val resource = weatherIconResource(code, isDay)
    val description = weatherDescription(code, isDay)
    if (resource != null) {
        Image(painterResource(resource), description, modifier)
    } else {
        Box(modifier.semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
            Text("—", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
