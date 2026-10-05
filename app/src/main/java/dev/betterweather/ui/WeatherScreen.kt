package dev.betterweather.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.AddToHomeScreen
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.betterweather.data.DayWeather
import dev.betterweather.data.HourWeather
import dev.betterweather.data.WeatherForecast
import dev.betterweather.data.WeatherPlace
import dev.betterweather.data.decimal
import dev.betterweather.data.degrees
import dev.betterweather.data.percent
import dev.betterweather.data.uvDescription
import dev.betterweather.data.weatherDescription
import dev.betterweather.data.windDirection
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val russian = Locale.forLanguageTag("ru")
private val hourFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", russian)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: WeatherViewModel, onLocation: () -> Unit, onAddWidget: () -> Boolean) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    var showSearch by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val forecast = state.forecast
    val busy = state.loading || state.locating

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            // Scaffold applies the system insets to content; top bar owns the status inset.
            androidx.compose.material3.TopAppBar(
                title = {
                    TextButton(onClick = { showSearch = true }, enabled = !busy, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Icon(Icons.Outlined.LocationOn, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(forecast?.place?.name ?: WeatherPlace.Default.name, style = MaterialTheme.typography.titleLarge,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Icon(Icons.Outlined.ExpandMore, "Выбрать город", Modifier.padding(start = 4.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }, enabled = !busy) {
                        Icon(Icons.Outlined.Refresh, "Обновить погоду")
                    }
                },
            )
        },
    ) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets)) {
            val wide = maxWidth >= 840.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(forecast?.let { it.localNow(state.now).format(dateFormat).replaceFirstChar { char -> char.titlecase(russian) } }
                            ?: "Погода сегодня", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (forecast?.place?.subtitle?.isNotBlank() == true) {
                            Text(forecast.place.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // Always reserve the loading line so refresh never displaces content.
                        Box(Modifier.fillMaxWidth().height(4.dp)) {
                            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        if (state.locating) Text("Определяем местоположение…", style = MaterialTheme.typography.bodyMedium)
                        if (state.loadingPlace != null) Text("Загружаем: ${state.loadingPlace}…", style = MaterialTheme.typography.bodyMedium)
                        state.error?.let { error ->
                            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                                    if (state.locationFailed) {
                                        TextButton(onClick = { showSearch = true }, enabled = !busy) { Text("Выбрать город") }
                                    } else {
                                        TextButton(onClick = { viewModel.refresh() }, enabled = !busy) { Text("Повторить обновление") }
                                    }
                                }
                            }
                        }
                        if (forecast != null) {
                            Text(updateLabel(forecast, state.now, state.offline), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (forecast == null) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Outlined.WbSunny, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Text(if (busy) "Загружаем прогноз" else "Пока нет прогноза", style = MaterialTheme.typography.headlineSmall)
                            Text("Выберите город или своё местоположение.", style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = { showSearch = true }, enabled = !busy) { Text("Выбрать город") }
                            TextButton(onClick = onLocation, enabled = !busy) {
                                Icon(Icons.Outlined.MyLocation, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Моё местоположение")
                            }
                        }
                    }
                } else if (wide) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(36.dp), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(0.9f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                CurrentConditions(forecast, state.now)
                                WidgetSection(onAddWidget = {
                                    if (!onAddWidget()) scope.launch { snackbar.showSnackbar("Удерживайте свободное место на главном экране → Виджеты → Better Weather.") }
                                })
                            }
                            Column(Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                                HourlySection(forecast, state.now)
                                WeekSection(forecast, state.now)
                            }
                        }
                    }
                } else {
                    item { CurrentConditions(forecast, state.now) }
                    item { HourlySection(forecast, state.now) }
                    item { WeekSection(forecast, state.now) }
                    item {
                        WidgetSection(onAddWidget = {
                            if (!onAddWidget()) scope.launch { snackbar.showSnackbar("Удерживайте свободное место на главном экране → Виджеты → Better Weather.") }
                        })
                    }
                }
                item { Attribution() }
            }
        }
    }

    if (showSearch) {
        ModalBottomSheet(onDismissRequest = { showSearch = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            CitySearch(search, onQuery = viewModel::search,
                onSelect = { place -> showSearch = false; viewModel.refresh(place) },
                onLocation = { showSearch = false; onLocation() }, busy = busy)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrentConditions(forecast: WeatherForecast, now: Instant) {
    val current = forecast.current
    val today = forecast.upcomingDays(now).firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(current.temperature.degrees(), style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.semantics { contentDescription = "Температура ${current.temperature.degrees()} Цельсия" })
                Text(weatherDescription(current.code, current.isDay), style = MaterialTheme.typography.titleMedium)
            }
            WeatherIcon(current.code, Modifier.padding(start = 12.dp, end = 8.dp).size(72.dp), current.isDay)
        }
        Text("Ощущается как ${current.feelsLike.degrees()}  ·  ${today?.high.degrees()} / ${today?.low.degrees()}",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
            FlowRow(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp), maxItemsInEachRow = 3) {
                Metric(Icons.Outlined.Air, "Ветер", "${current.windSpeed.decimal()} м/с", windDirection(current.windDirection), Modifier.weight(1f))
                Metric(Icons.Outlined.WbSunny, "UV-индекс", forecast.currentUv(now).decimal(), uvDescription(forecast.currentUv(now)), Modifier.weight(1f))
                Metric(Icons.Outlined.WaterDrop, "Влажность", current.humidity.percent(), "Относительная", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Metric(icon: ImageVector, label: String, value: String, detail: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HourlySection(forecast: WeatherForecast, now: Instant) {
    val hours = forecast.todayHours(now)
    val localNow = forecast.localNow(now)
    val currentHour = localNow.withMinute(0).withSecond(0).withNano(0)
    var selectedTime by rememberSaveable(forecast.place, localNow.toLocalDate()) { mutableStateOf<String?>(null) }
    val selected = hours.firstOrNull { it.time.toString() == selectedTime }
        ?: hours.firstOrNull { it.time == currentHour } ?: hours.firstOrNull()
    val listState = rememberLazyListState()
    LaunchedEffect(forecast.place, localNow.toLocalDate()) {
        val index = hours.indexOfFirst { it.time >= currentHour }
        if (index > 0) listState.scrollToItem((index - 1).coerceAtLeast(0))
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Сегодня по часам", style = MaterialTheme.typography.titleLarge)
        if (hours.isEmpty()) {
            Text("Сохранённый прогноз на сегодня отсутствует. Обновите погоду.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(hours, key = { it.time.toString() }) { hour ->
                    HourReading(hour, hour == selected, hour.time == currentHour, onSelect = { selectedTime = hour.time.toString() })
                }
            }
            selected?.let { hour ->
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${hour.time.format(hourFormat)} · ${weatherDescription(hour.code, hour.isDay)}", style = MaterialTheme.typography.labelLarge)
                        Text("Ветер ${hour.windSpeed.decimal()} м/с  ·  Осадки ${hour.precipitationProbability.percent()}  ·  UV ${hour.uv.decimal()}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun HourReading(hour: HourWeather, selected: Boolean, current: Boolean, onSelect: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Surface(shape = RoundedCornerShape(12.dp), color = background,
        modifier = Modifier.width(76.dp).clickable(role = Role.Button, onClick = onSelect)
            .semantics { contentDescription = "${hour.time.format(hourFormat)}, ${hour.temperature.degrees()}, осадки ${hour.precipitationProbability.percent()}${if (selected) ", выбран" else ""}" }) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (current) "Сейчас" else hour.time.format(hourFormat), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            WeatherIcon(hour.code, Modifier.size(28.dp), hour.isDay)
            Text(hour.temperature.degrees(), style = MaterialTheme.typography.titleMedium)
            Text(hour.precipitationProbability.percent(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun WeekSection(forecast: WeatherForecast, now: Instant) {
    val days = forecast.upcomingDays(now)
    val today = forecast.localNow(now).toLocalDate()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("На ближайшую неделю", style = MaterialTheme.typography.titleLarge)
        if (days.isEmpty()) Text("Сохранённый прогноз устарел. Обновите погоду.", style = MaterialTheme.typography.bodyMedium)
        days.forEachIndexed { index, day ->
            DayReading(day, today)
            if (index != days.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun DayReading(day: DayWeather, today: LocalDate) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(when (day.date) {
                today -> "Сегодня"
                today.plusDays(1) -> "Завтра"
                else -> day.date.format(DateTimeFormatter.ofPattern("EEEE", russian)).replaceFirstChar { it.titlecase(russian) }
            }, style = MaterialTheme.typography.titleSmall)
            Text(day.date.format(DateTimeFormatter.ofPattern("d MMM", russian)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WeatherIcon(day.code, modifier = Modifier.size(24.dp))
            Text(day.precipitationProbability.percent(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(day.high.degrees(), style = MaterialTheme.typography.titleMedium)
            Text(day.low.degrees(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WidgetSection(onAddWidget: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Погода на главном экране", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Температура, ветер и UV-индекс — в одном виджете.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            TextButton(onClick = onAddWidget, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Icon(Icons.AutoMirrored.Outlined.AddToHomeScreen, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Добавить виджет")
            }
        }
    }
}

@Composable
private fun Attribution() {
    val uri = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Время и прогноз — для выбранного места. °C · м/с", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { uri.openUri("https://open-meteo.com/") }, contentPadding = PaddingValues(horizontal = 0.dp)) {
            Text("Данные: Open-Meteo · CC BY 4.0", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CitySearch(state: SearchUiState, onQuery: (String) -> Unit, onSelect: (WeatherPlace) -> Unit, onLocation: () -> Unit, busy: Boolean) {
    Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Выбрать место", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = state.query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Город") }, placeholder = { Text("Например, Тбилиси") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Outlined.Close, "Очистить поиск") }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onQuery(state.query) }),
        )
        TextButton(onClick = onLocation, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Outlined.MyLocation, null)
            Spacer(Modifier.width(10.dp))
            Text("Моё местоположение")
        }
        Box(Modifier.fillMaxWidth().height(4.dp)) { if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth()) }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 340.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            when {
                state.error != null -> item {
                    Text(state.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { onQuery(state.query) }) { Text("Повторить поиск") }
                }
                state.query.trim().length < 2 -> item {
                    Text("Введите хотя бы две буквы названия города.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                !state.loading && state.results.isEmpty() -> item {
                    Text("Город не найден. Попробуйте другое название.", style = MaterialTheme.typography.bodyMedium)
                }
                else -> items(state.results) { place ->
                    Column(Modifier.fillMaxWidth().clickable(enabled = !busy, role = Role.Button, onClick = { onSelect(place) })
                        .padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(place.name, style = MaterialTheme.typography.titleMedium)
                        if (place.subtitle.isNotBlank()) Text(place.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

private fun updateLabel(forecast: WeatherForecast, now: Instant, offline: Boolean): String {
    val age = Duration.between(forecast.fetchedAt, now).toMinutes().coerceAtLeast(0)
    val localTime = forecast.localNow(forecast.fetchedAt)
    return when {
        offline -> "Без сети · сохранено ${localTime.format(DateTimeFormatter.ofPattern("d MMM, HH:mm", russian))}"
        age >= 60 -> "Данные ${age / 60} ч назад · обновите прогноз"
        else -> "Обновлено в ${localTime.format(hourFormat)}"
    }
}
