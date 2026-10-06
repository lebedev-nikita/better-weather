package dev.betterweather.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

    override fun onEnabled(context: Context) { schedule(context) }

    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            updateAll(context, context.getString(R.string.widget_updating))
            refresh(context)
        }
    }

    companion object {
        private const val PERIODIC = "weather-widget-periodic"
        private const val IMMEDIATE = "weather-widget-refresh"
        private const val ACTION_REFRESH = "dev.betterweather.REFRESH_WIDGET"
        private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WeatherRefreshWorker>(1, TimeUnit.HOURS).setConstraints(network).build())
        }

        fun refresh(context: Context) {
            // Manual attempts run even offline so that the widget leaves its loading state.
            WorkManager.getInstance(context).enqueueUniqueWork(IMMEDIATE, ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<WeatherRefreshWorker>().build())
        }

        fun updateAll(context: Context, status: String? = null) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val forecast = (context.applicationContext as WeatherApplication).repository.cached()
            val views = RemoteViews(context.packageName, R.layout.weather_widget)
            val openApp = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val refreshIntent = PendingIntent.getBroadcast(context, 1,
                Intent(context, WeatherWidgetProvider::class.java).setAction(ACTION_REFRESH), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, openApp)
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshIntent)
            views.setTextViewText(R.id.widget_city, forecast?.place?.name ?: context.getString(R.string.app_name))
            views.setTextViewText(R.id.widget_temperature, forecast?.current?.temperature.degrees())
            views.setTextViewText(R.id.widget_condition, forecast?.let { weatherDescription(it.current.code, it.current.isDay) } ?: context.getString(R.string.widget_choose_city))
            val artwork = forecast?.let { weatherIconResource(it.current.code, it.current.isDay) }
            views.setViewVisibility(R.id.widget_weather_icon, if (artwork != null) View.VISIBLE else View.GONE)
            if (artwork != null) views.setImageViewResource(R.id.widget_weather_icon, artwork)
            val direction = forecast?.current?.windDirection?.let(::windDirection)
            views.setTextViewText(R.id.widget_wind_label,
                context.getString(R.string.widget_wind_label) + (direction?.let { " · $it" } ?: ""))
            views.setTextViewText(R.id.widget_wind, forecast?.current?.windSpeed?.let { "${it.decimal()} м/с" } ?: "—")
            views.setTextViewText(R.id.widget_uv, forecast?.currentUv(forecast.fetchedAt).decimal())
            val updated = forecast?.let {
                val age = Duration.between(it.fetchedAt, Instant.now()).toHours()
                if (age >= 1) "Данные ${age} ч назад · нажмите ↻"
                else "Обновлено ${it.localNow(it.fetchedAt).format(DateTimeFormatter.ofPattern("HH:mm"))}"
            } ?: context.getString(R.string.widget_tap_to_open)
            views.setTextViewText(R.id.widget_updated, status?.let { "$it · $updated" } ?: updated)
            ids.forEach { manager.updateAppWidget(it, views) }
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
