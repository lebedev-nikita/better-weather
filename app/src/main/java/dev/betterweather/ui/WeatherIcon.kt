package dev.betterweather.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.betterweather.data.WeatherKind
import dev.betterweather.data.weatherDescription
import dev.betterweather.data.weatherKind

@Composable
fun WeatherIcon(code: Int?, modifier: Modifier = Modifier, isDay: Boolean = true) {
    val kind = weatherKind(code)
    val icon = when (kind) {
        WeatherKind.Clear -> if (isDay) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay
        WeatherKind.PartlyCloudy -> Icons.Outlined.WbCloudy
        WeatherKind.Cloudy, WeatherKind.Fog, WeatherKind.Unknown -> Icons.Outlined.CloudQueue
        WeatherKind.Rain -> Icons.Outlined.WaterDrop
        WeatherKind.Snow -> Icons.Outlined.Grain
        WeatherKind.Thunder -> Icons.Outlined.Thunderstorm
    }
    Icon(icon, weatherDescription(code, isDay), modifier,
        tint = if (kind == WeatherKind.Clear && isDay) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
}
