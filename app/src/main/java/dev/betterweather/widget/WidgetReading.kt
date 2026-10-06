package dev.betterweather.widget

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan

internal fun widgetReading(label: String, value: String, valueSizeRatio: Float = 16f / 12f): CharSequence =
    SpannableStringBuilder(if (label.isEmpty()) "" else "$label\u2002 ").apply {
        val start = length
        append(value)
        setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(RelativeSizeSpan(valueSizeRatio), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
