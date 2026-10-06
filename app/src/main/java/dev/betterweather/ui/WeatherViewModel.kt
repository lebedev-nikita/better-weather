package dev.betterweather.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.betterweather.R
import dev.betterweather.WeatherApplication
import dev.betterweather.data.WeatherForecast
import dev.betterweather.data.WeatherPlace
import dev.betterweather.widget.WeatherWidgetProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Instant

data class WeatherUiState(
    val forecast: WeatherForecast? = null,
    val loading: Boolean = false,
    val loadingPlace: String? = null,
    val error: String? = null,
    val offline: Boolean = false,
    val locating: Boolean = false,
    val locationFailed: Boolean = false,
    val now: Instant = Instant.now(),
)

data class SearchUiState(
    val query: String = "",
    val results: List<WeatherPlace> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as WeatherApplication).repository
    private val _state = MutableStateFlow(WeatherUiState(forecast = repository.cached()))
    val state = _state.asStateFlow()
    private val _search = MutableStateFlow(SearchUiState())
    val search = _search.asStateFlow()
    private var refreshJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                _state.update { it.copy(now = Instant.now()) }
            }
        }
    }

    fun onOpen() {
        if (refreshJob?.isActive == true || _state.value.locating) return
        val cached = repository.cached()
        _state.update { it.copy(forecast = cached ?: it.forecast, now = Instant.now()) }
        WeatherWidgetProvider.updateAll(getApplication())
        refresh()
    }

    fun refresh(place: WeatherPlace? = null) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, loadingPlace = place?.name, error = null, locationFailed = false) }
            try {
                val forecast = if (place == null) repository.refreshSelected() else repository.refresh(place)
                _state.update { it.copy(forecast = forecast, loading = false, loadingPlace = null, error = null, offline = false, now = Instant.now()) }
                WeatherWidgetProvider.updateAll(getApplication())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, loadingPlace = null, offline = place == null && it.forecast != null,
                        error = if (place != null) "Не удалось загрузить погоду для «${place.name}». Выбор города не изменён."
                        else if (error is IOException) "Не удалось обновить погоду. Проверьте интернет и попробуйте снова."
                        else "Не удалось прочитать прогноз. Попробуйте обновить ещё раз.")
                }
                if (place == null) WeatherWidgetProvider.updateAll(getApplication(),
                    getApplication<Application>().getString(R.string.widget_refresh_failed))
            }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        _search.value = SearchUiState(query = query, loading = query.trim().length >= 2)
        if (query.trim().length < 2) return
        searchJob = viewModelScope.launch {
            delay(350)
            try {
                val results = repository.search(query)
                _search.value = SearchUiState(query, results)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _search.value = SearchUiState(query = query, error = "Не удалось найти города. Проверьте интернет и повторите поиск.")
            }
        }
    }

    fun setLocating(value: Boolean) { _state.update { it.copy(locating = value, error = null, locationFailed = false) } }
    fun locationError(message: String) { _state.update { it.copy(locating = false, error = message, locationFailed = true) } }
}
