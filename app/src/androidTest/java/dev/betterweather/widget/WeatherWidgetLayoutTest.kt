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
import org.junit.Assert.assertTrue
import org.junit.Test
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
                        setTextViewText(R.id.widget_wind_label, "Ветер · СВ")
                        setTextViewText(R.id.widget_wind, "19,4 м/с")
                        setTextViewText(R.id.widget_uv, "11,7")
                        setTextViewText(R.id.widget_updated, "Обновлено 09:00")
                    }
                    val widget = remote.apply(context, FrameLayout(context)) as ViewGroup
                    val w = (width * density).roundToInt()
                    val h = (150 * density).roundToInt()
                    widget.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                    widget.layout(0, 0, w, h)
                    for (id in listOf(R.id.widget_temperature, R.id.widget_wind, R.id.widget_uv, R.id.widget_updated)) {
                        val reading = widget.findViewById<TextView>(id)
                        val bounds = Rect(0, 0, reading.width, reading.height)
                        widget.offsetDescendantRectToMyCoords(reading, bounds)
                        val name = context.resources.getResourceEntryName(id)
                        assertTrue("$name clipped at ${width}×150dp, font $scale: $bounds",
                            bounds.top >= widget.paddingTop && bounds.bottom <= h - widget.paddingBottom &&
                                bounds.left >= widget.paddingLeft && bounds.right <= w - widget.paddingRight)
                        assertTrue("$name text clipped at ${width}×150dp, font $scale: ${reading.layout.height} > ${reading.height}",
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
