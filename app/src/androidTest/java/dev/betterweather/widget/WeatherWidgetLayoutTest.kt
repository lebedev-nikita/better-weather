package dev.betterweather.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.res.Configuration
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import dev.betterweather.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

class WeatherWidgetLayoutTest {
    @Test fun panelStaysCompactWhenLauncherAllocatesExtraHeight() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val provider = AppWidgetManager.getInstance(context).installedProviders.first {
                it.provider == ComponentName(context, WeatherWidgetProvider::class.java)
            }
            val host = AppWidgetHostView(context).apply {
                setAppWidget(0, provider)
                updateAppWidget(RemoteViews(context.packageName, R.layout.weather_widget).apply {
                    setTextViewText(R.id.widget_city, "Тбилиси")
                    setTextViewText(R.id.widget_temperature, "17°")
                    setTextViewText(R.id.widget_condition, "Облачно")
                    setTextViewText(R.id.widget_wind, widgetReading("Ветер · ЮВ", "0,7\u00A0м/с"))
                    setTextViewText(R.id.widget_uv, widgetReading("UV-индекс", "0,1"))
                    setTextViewText(R.id.widget_updated, "Обновлено 19:34")
                })
            }
            val density = context.resources.displayMetrics.density
            val w = (360 * density).roundToInt()
            val h = (220 * density).roundToInt()
            host.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            host.layout(0, 0, w, h)
            val panel = host.findViewById<View>(R.id.widget_root)
            assertTrue("The panel must hug its readings even in a tall launcher cell", panel.height < 132 * density)
        }
    }

    @Test fun readingsRemainVisibleInFullWidgetWithLargeFont() =
        assertReadingLayout(R.layout.weather_widget, 132, 16f / 12f)

    @Test fun readingsRemainVisibleAtMinimumResizableHeight() =
        assertReadingLayout(R.layout.weather_widget_compact, 96, 14f / 12f)

    @Test fun readingsRemainVisibleInShortLandscapeWidget() =
        assertReadingLayout(R.layout.weather_widget_short, 48, 12f / 11f)

    private fun assertReadingLayout(layoutId: Int, heightDp: Int, valueSizeRatio: Float) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (scale in listOf(1f, 1.3f)) {
                val context = instrumentation.targetContext.createConfigurationContext(
                    Configuration(instrumentation.targetContext.resources.configuration).apply { fontScale = scale },
                )
                val density = context.resources.displayMetrics.density
                for (width in listOf(240, 360)) {
                    val remote = RemoteViews(context.packageName, layoutId).apply {
                        setTextViewText(R.id.widget_city, "Тбилиси")
                        setTextViewText(R.id.widget_temperature, "−23°")
                        if (layoutId != R.layout.weather_widget_short)
                            setTextViewText(R.id.widget_condition, "Переменная облачность")
                        setTextViewText(R.id.widget_wind, widgetReading(if (layoutId == R.layout.weather_widget_short) "" else "Ветер · СВ", "19,4\u00A0м/с", valueSizeRatio))
                        setTextViewText(R.id.widget_uv, widgetReading(if (layoutId == R.layout.weather_widget_short) "UV" else "UV-индекс", "11,7", valueSizeRatio))
                        setTextViewText(R.id.widget_updated,
                            if (layoutId != R.layout.weather_widget) "Обн. 09:00" else "Обновлено 09:00")
                    }
                    val widget = remote.apply(context, FrameLayout(context)) as ViewGroup
                    val w = (width * density).roundToInt()
                    val h = (heightDp * density).roundToInt()
                    widget.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                    widget.layout(0, 0, w, h)
                    val city = widget.findViewById<TextView>(R.id.widget_city)
                    val updated = widget.findViewById<TextView>(R.id.widget_updated)
                    assertTrue("City and update time must share the header row",
                        city.right < updated.left && city.top < updated.bottom && updated.top < city.bottom)
                    assertTrue("Update time must sit at the right edge",
                        abs(w - widget.paddingRight - updated.right - (updated.parent as View).left) <= 1)
                    val wind = widget.findViewById<TextView>(R.id.widget_wind)
                    val uv = widget.findViewById<TextView>(R.id.widget_uv)
                    if (width == 360) {
                        assertEquals("Wind should fit one line in the wide widget", 1, wind.layout.lineCount)
                        assertEquals("UV should fit one line in the wide widget", 1, uv.layout.lineCount)
                    } else if (scale == 1.3f && layoutId != R.layout.weather_widget_short) {
                        assertTrue("Narrow readings should wrap instead of clipping", wind.layout.lineCount > 1)
                    }
                    for (id in listOf(R.id.widget_temperature, R.id.widget_wind, R.id.widget_uv, R.id.widget_updated)) {
                        val reading = widget.findViewById<TextView>(id)
                        val bounds = Rect(0, 0, reading.width, reading.height)
                        widget.offsetDescendantRectToMyCoords(reading, bounds)
                        val name = context.resources.getResourceEntryName(id)
                        assertTrue("$name clipped at ${width}×${heightDp}dp, font $scale: $bounds",
                            bounds.top >= widget.paddingTop && bounds.bottom <= h - widget.paddingBottom &&
                                bounds.left >= widget.paddingLeft && bounds.right <= w - widget.paddingRight)
                        assertTrue("$name text clipped at ${width}×${heightDp}dp, font $scale: ${reading.layout.height} > ${reading.height}",
                            reading.layout.height <= reading.height - reading.compoundPaddingTop - reading.compoundPaddingBottom &&
                                (0 until reading.layout.lineCount).all {
                                    reading.layout.getEllipsisCount(it) == 0 &&
                                        reading.layout.getLineWidth(it) <= reading.width - reading.compoundPaddingLeft - reading.compoundPaddingRight
                                })
                    }
                }
            }
        }
    }
}
