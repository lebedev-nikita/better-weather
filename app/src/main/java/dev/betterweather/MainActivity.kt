package dev.betterweather

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import dev.betterweather.data.WeatherPlace
import dev.betterweather.location.deviceLocation
import dev.betterweather.ui.WeatherScreen
import dev.betterweather.ui.WeatherTheme
import dev.betterweather.ui.WeatherViewModel
import dev.betterweather.widget.WeatherWidgetProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: WeatherViewModel by viewModels()
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true || grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) locate()
        else viewModel.locationError("Доступ к местоположению не разрешён. Выберите город через поиск.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherTheme {
                WeatherScreen(viewModel, onLocation = ::requestLocation, onAddWidget = ::addWidget)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun requestLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate()
        else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }

    private fun locate() {
        if (viewModel.state.value.locating || viewModel.state.value.loading) return
        viewModel.setLocating(true)
        lifecycleScope.launch {
            try {
                val location = deviceLocation(this@MainActivity)
                viewModel.setLocating(false)
                if (location == null) viewModel.locationError("Не удалось определить местоположение. Включите геолокацию или найдите город.")
                else viewModel.refresh(WeatherPlace("Моё местоположение", location.latitude, location.longitude,
                    region = String.format(Locale.forLanguageTag("ru"), "%.2f°, %.2f°", location.latitude, location.longitude)))
            } catch (cancelled: CancellationException) {
                viewModel.setLocating(false)
                throw cancelled
            } catch (error: Exception) {
                viewModel.locationError("Не удалось определить местоположение. Попробуйте поиск города.")
            }
        }
    }

    private fun addWidget(): Boolean {
        val manager = AppWidgetManager.getInstance(this)
        return manager.isRequestPinAppWidgetSupported && manager.requestPinAppWidget(ComponentName(this, WeatherWidgetProvider::class.java), null, null)
    }
}
