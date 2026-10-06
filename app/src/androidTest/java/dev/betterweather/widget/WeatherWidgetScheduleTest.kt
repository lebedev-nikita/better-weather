package dev.betterweather.widget

import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class WeatherWidgetScheduleTest {
    @Test fun hourlyWorkIsUpdatedWithoutCreatingDuplicates() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = WorkManager.getInstance(context)
        val name = "weather-widget-periodic"
        val hourly = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(1, TimeUnit.HOURS)
            // Keep the old request pending while exercising the migration.
            .setConstraints(Constraints.Builder().setRequiresCharging(true).build())
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        manager.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, hourly)
            .result.get(10, TimeUnit.SECONDS)
        try {
            repeat(2) {
                WeatherWidgetProvider.schedule(context).result.get(10, TimeUnit.SECONDS)
                val work = manager.getWorkInfosForUniqueWork(name).get(10, TimeUnit.SECONDS).single()
                assertEquals(hourly.id, work.id)
                assertEquals(TimeUnit.MINUTES.toMillis(15), work.periodicityInfo?.repeatIntervalMillis)
                assertEquals(NetworkType.CONNECTED, work.constraints.requiredNetworkType)
                assertEquals(false, work.constraints.requiresCharging())
            }
        } finally {
            // Leave the emulator with the production schedule, without the fixture's delay.
            manager.cancelUniqueWork(name).result.get(10, TimeUnit.SECONDS)
            WeatherWidgetProvider.schedule(context).result.get(10, TimeUnit.SECONDS)
        }
    }
}
