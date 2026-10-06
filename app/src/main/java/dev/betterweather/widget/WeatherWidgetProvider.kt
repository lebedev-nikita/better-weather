package dev.betterweather.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.betterweather.MainActivity
import dev.betterweather.R
import dev.betterweather.WeatherApplication
import dev.betterweather.data.WeatherForecast
import dev.betterweather.data.decimal
import dev.betterweather.data.degrees
import dev.betterweather.data.weatherDescription
import dev.betterweather.data.windDirection
import dev.betterweather.ui.weatherIconResource
import kotlinx.coroutines.CancellationException
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class WeatherWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateAll(context)
        schedule(context)
        refresh(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        updateAll(context)
    }

    override fun onEnabled(context: Context) { schedule(context) }

    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val manager = AppWidgetManager.getInstance(context)
            if (manager.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java)).isNotEmpty()) {
                schedule(context)
                updateAll(context)
            }
        }
    }

    companion object {
        private const val PERIODIC = "weather-widget-periodic"
        private const val IMMEDIATE = "weather-widget-refresh"
        private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedule(context: Context) =
            // UPDATE also migrates the hourly work retained from older versions.
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<WeatherRefreshWorker>(15, TimeUnit.MINUTES).setConstraints(network).build())

        fun refresh(context: Context) {
            // The initial widget refresh also attempts to load data when offline.
            WorkManager.getInstance(context).enqueueUniqueWork(IMMEDIATE, ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<WeatherRefreshWorker>().build())
        }

        fun updateAll(context: Context, status: String? = null) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val forecast = (context.applicationContext as WeatherApplication).repository.cached()
            val full = render(context, forecast, status, R.layout.weather_widget)
            val compact = render(context, forecast, status, R.layout.weather_widget_compact)
            val short = render(context, forecast, status, R.layout.weather_widget_short)
            fun forHeight(height: Int) = when {
                height < 96 -> short
                height < 132 -> compact
                else -> full
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // The host chooses independently for each widget size and orientation.
                val responsive = RemoteViews(mapOf(
                    SizeF(240f, 48f) to short,
                    SizeF(240f, 96f) to compact,
                    SizeF(240f, 132f) to full,
                ))
                ids.forEach { manager.updateAppWidget(it, responsive) }
            } else {
                ids.forEach { id ->
                    val options = manager.getAppWidgetOptions(id)
                    val landscape = forHeight(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 132))
                    val portrait = forHeight(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 132))
                    val views = if (landscape === portrait) portrait else RemoteViews(landscape, portrait)
                    manager.updateAppWidget(id, views)
                }
            }
        }

        private fun render(context: Context, forecast: WeatherForecast?, status: String?, layout: Int): RemoteViews {
            val short = layout == R.layout.weather_widget_short
            val compact = layout != R.layout.weather_widget
            val views = RemoteViews(context.packageName, layout)
            val valueSizeRatio = when {
                short -> 12f / 11f
                compact -> 14f / 12f
                else -> 16f / 12f
            }
            val openApp = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, openApp)
            views.setTextViewText(R.id.widget_city, forecast?.place?.name ?: context.getString(R.string.app_name))
            views.setTextViewText(R.id.widget_temperature, forecast?.current?.temperature.degrees())
            if (!short) views.setTextViewText(R.id.widget_condition, forecast?.let { weatherDescription(it.current.code, it.current.isDay) } ?: context.getString(R.string.widget_choose_city))
            val artwork = forecast?.let { weatherIconResource(it.current.code, it.current.isDay) }
            if (!short) {
                views.setViewVisibility(R.id.widget_weather_icon, if (artwork != null) View.VISIBLE else View.GONE)
                if (artwork != null) views.setImageViewResource(R.id.widget_weather_icon, artwork)
            } else {
                views.setContentDescription(R.id.widget_temperature,
                    "${forecast?.current?.temperature.degrees()} · ${forecast?.let { weatherDescription(it.current.code, it.current.isDay) } ?: "—"}")
            }
            val direction = forecast?.current?.windDirection?.let(::windDirection)
            val windLabel = context.getString(R.string.widget_wind_label) + (direction?.let { " · $it" } ?: "")
            val wind = forecast?.current?.windSpeed?.let { "${it.decimal()}\u00A0м/с" } ?: "—"
            views.setTextViewText(R.id.widget_wind, widgetReading(if (short) "" else windLabel, wind, valueSizeRatio))
            views.setContentDescription(R.id.widget_wind, "$windLabel $wind")
            views.setTextViewText(R.id.widget_uv,
                widgetReading(context.getString(if (short) R.string.widget_uv_label_short else R.string.widget_uv_label), forecast?.currentUv(forecast.fetchedAt).decimal(), valueSizeRatio))
            views.setContentDescription(R.id.widget_uv,
                "${context.getString(R.string.widget_uv_label)} ${forecast?.currentUv(forecast.fetchedAt).decimal()}")
            val age = forecast?.let { Duration.between(it.fetchedAt, Instant.now()).toHours() } ?: 0L
            val time = forecast?.localNow(forecast.fetchedAt)?.format(DateTimeFormatter.ofPattern("HH:mm"))
            val updated = when {
                forecast == null -> context.getString(R.string.widget_tap_to_open)
                age >= 1 -> "Данные ${age} ч назад · откройте приложение"
                else -> "Обновлено $time"
            }
            val message = status?.let { "$it · $updated" } ?: updated
            val shortAgeOrTime = if (age >= 1) context.getString(R.string.widget_age_compact, age) else time ?: "—"
            val compactUpdated = when {
                status != null -> context.getString(R.string.widget_update_error_compact, shortAgeOrTime)
                forecast == null -> context.getString(R.string.widget_choose_city_compact)
                age >= 1 -> shortAgeOrTime
                else -> context.getString(R.string.widget_updated_compact, time)
            }
            views.setTextViewText(R.id.widget_updated, if (compact) compactUpdated else message)
            views.setContentDescription(R.id.widget_updated, message)
            return views
        }
    }
}

class WeatherRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        return try {
            (applicationContext as WeatherApplication).repository.refreshSelected()
            WeatherWidgetProvider.updateAll(applicationContext)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            WeatherWidgetProvider.updateAll(applicationContext, applicationContext.getString(R.string.widget_refresh_failed))
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }
}
