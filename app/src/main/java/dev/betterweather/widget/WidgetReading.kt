package dev.betterweather.widget

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan

internal fun widgetReading(label: String, value: String): CharSequence =
    SpannableStringBuilder("$label ").apply {
        val start = length
        append(value)
        setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(RelativeSizeSpan(14f / 12f), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
