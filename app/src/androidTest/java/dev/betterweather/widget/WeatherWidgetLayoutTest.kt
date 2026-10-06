package dev.betterweather.widget

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
    @Test fun readingsRemainVisibleInCompactWidgetWithLargeFont() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (scale in listOf(1f, 1.3f)) {
                val context = instrumentation.targetContext.createConfigurationContext(
                    Configuration(instrumentation.targetContext.resources.configuration).apply { fontScale = scale },
                )
                val density = context.resources.displayMetrics.density
                for (width in listOf(240, 360)) {
                    val remote = RemoteViews(context.packageName, R.layout.weather_widget).apply {
                        setTextViewText(R.id.widget_city, "Тбилиси")
                        setTextViewText(R.id.widget_temperature, "−23°")
                        setTextViewText(R.id.widget_condition, "Переменная облачность")
                        setTextViewText(R.id.widget_wind, widgetReading("Ветер · СВ", "19,4\u00A0м/с"))
                        setTextViewText(R.id.widget_uv, widgetReading("UV-индекс", "11,7"))
                        setTextViewText(R.id.widget_updated, "Обновлено 09:00")
                    }
                    val widget = remote.apply(context, FrameLayout(context)) as ViewGroup
                    val w = (width * density).roundToInt()
                    val h = (140 * density).roundToInt()
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
                    } else if (scale == 1.3f) {
                        assertTrue("Narrow readings should wrap instead of clipping", wind.layout.lineCount > 1)
                    }
                    for (id in listOf(R.id.widget_temperature, R.id.widget_wind, R.id.widget_uv, R.id.widget_updated)) {
                        val reading = widget.findViewById<TextView>(id)
                        val bounds = Rect(0, 0, reading.width, reading.height)
                        widget.offsetDescendantRectToMyCoords(reading, bounds)
                        val name = context.resources.getResourceEntryName(id)
                        assertTrue("$name clipped at ${width}×140dp, font $scale: $bounds",
                            bounds.top >= widget.paddingTop && bounds.bottom <= h - widget.paddingBottom &&
                                bounds.left >= widget.paddingLeft && bounds.right <= w - widget.paddingRight)
                        assertTrue("$name text clipped at ${width}×140dp, font $scale: ${reading.layout.height} > ${reading.height}",
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
