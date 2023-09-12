package com.ermolaevio.waveformeditor.ui.utils

import android.content.Context
import android.graphics.Canvas
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.core.graphics.ColorUtils

internal fun View.dp(value: Int): Int = context.dp(value)
internal fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
internal fun Float.clamp(min: Float, max: Float) = max.coerceAtMost(this.coerceAtLeast(min))
internal inline fun Canvas.transform(crossinline init: Canvas.() -> Unit) {
    save()
    init()
    restore()
}
@ColorInt
internal fun Int.withAlpha(@IntRange(0, 255) alpha: Int) =
    ColorUtils.setAlphaComponent(this, alpha)