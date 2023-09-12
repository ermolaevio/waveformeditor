package com.ermolaevio.waveformeditor.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.ermolaevio.waveformeditor.R
import com.ermolaevio.waveformeditor.ui.utils.clamp
import com.ermolaevio.waveformeditor.ui.utils.dp
import com.ermolaevio.waveformeditor.ui.utils.transform
import com.ermolaevio.waveformeditor.ui.utils.withAlpha
import kotlin.math.ceil

internal typealias Points = Pair<Float, Float>

private const val MIN_POINTS_LENGTH = 3
private const val SELECTOR_COLOR_ALPHA = 170
private const val TAG = "WaveFormEditorViewTag"

class WaveFormEditorView : View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    var onNewWaveSelected: (List<Points>) -> Unit = {}
    var selectedPoints: List<Points> = emptyList()
        private set

    private var points = listOf<Points>()

    private var selectorWidthF = dp(2).toFloat()
    private val selectorBtnWidth = dp(8).toFloat()
    private var touchSelectorArea = dp(16)
    private var minSelectionWidth = dp(50)

    private var path = Path().apply {
        fillType = Path.FillType.WINDING
    }

    private val selectedWavesPaint = Paint().apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.wave_color)
    }

    private val unselectedWavesPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.GRAY
    }

    private val leftSelectorPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL_AND_STROKE
        color = Color.WHITE.withAlpha(SELECTOR_COLOR_ALPHA)
        strokeWidth = selectorWidthF
    }

    private val rightSelectorPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL_AND_STROKE
        color = Color.WHITE.withAlpha(SELECTOR_COLOR_ALPHA)
        strokeWidth = selectorWidthF
    }

    private var leftSelectorDragging = false
        set(value) {
            field = value
            leftSelectorPaint.color = if (value) {
                Color.WHITE
            } else {
                Color.WHITE.withAlpha(SELECTOR_COLOR_ALPHA)
            }
        }
    private var rightSelectorDragging = false
        set(value) {
            field = value
            rightSelectorPaint.color = if (value) {
                Color.WHITE
            } else {
                Color.WHITE.withAlpha(SELECTOR_COLOR_ALPHA)
            }
        }

    private var w = 0
    private var h = 0

    private val widthF: Float
        get() = w.toFloat()
    private val heightF: Float
        get() = h.toFloat()

    private val halfOfHeight: Float
        get() = heightF / 2

    private var leftSelectorX: Float = 0f
    private var rightSelectorX: Float = 0f

    private val leftSelectorWithWidthX: Float
        get() = leftSelectorX + selectorWidthF

    private val rightSelectorWithWidthX: Float
        get() = rightSelectorX + selectorWidthF

    private var lastTouchedX: Float = Float.NaN

    private val MotionEvent.xInsideBoundary
        get() = x.clamp(0F, widthF)

    /// region Public
    fun setData(list: List<Points>) {
        if (list.count() < MIN_POINTS_LENGTH) return
        if (list.any { it.first !in -1f..0f || it.second !in 0f..1f }) return

        points = list
        selectedPoints = emptyList()
        leftSelectorX = 0f
        rightSelectorX = getDefaultRightSelectorX()
        createPath()
        invalidate()
    }
    /// endregion

    /// region Overrides
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top

        if (width == w && height == h) return

        w = width
        h = height
        leftSelectorX = 0f
        rightSelectorX = getDefaultRightSelectorX()
        createPath()
    }

    override fun onDraw(canvas: Canvas) {
        if (points.isEmpty()) return

        canvas.drawColor(Color.DKGRAY)

        // draw left unselected slice
        if (leftSelectorX > 0f) {
            canvas.transform {
                clipRect(0f, 0f, leftSelectorX, heightF)
                canvas.drawPath(path, unselectedWavesPaint)
            }
        }
        // draw selected slice
        canvas.transform {
            clipRect(leftSelectorWithWidthX, 0f, rightSelectorX, heightF)
            canvas.drawPath(path, selectedWavesPaint)
        }
        // draw right unselected selected slice
        if (rightSelectorWithWidthX < widthF) {
            canvas.transform {
                clipRect(rightSelectorWithWidthX, 0f, widthF, heightF)
                canvas.drawPath(path, unselectedWavesPaint)
            }
        }

        // draw left selector
        canvas.drawLine(
            /* startX = */ leftSelectorX + selectorWidthF / 2,
            /* startY = */ 0f,
            /* stopX = */ leftSelectorX + selectorWidthF / 2,
            /* stopY = */ heightF,
            /* paint = */ leftSelectorPaint
        )

        canvas.drawRect(
            /* left = */ leftSelectorWithWidthX,
            /* top = */ 0f,
            /* right = */ leftSelectorWithWidthX + selectorBtnWidth,
            /* bottom = */ selectorBtnWidth,
            /* paint = */ leftSelectorPaint
        )

        // draw right selector
        canvas.drawLine(
            /* startX = */ rightSelectorX + selectorWidthF / 2,
            /* startY = */ 0f,
            /* stopX = */ rightSelectorX + selectorWidthF / 2,
            /* stopY = */ heightF,
            /* paint = */ rightSelectorPaint
        )
        canvas.drawRect(
            /* left = */ rightSelectorX - selectorBtnWidth,
            /* top = */ heightF - selectorBtnWidth,
            /* right = */ rightSelectorX,
            /* bottom = */ heightF,
            /* paint = */  rightSelectorPaint
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }

        val xInsideBoundary = event.xInsideBoundary
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedPoints = emptyList()

                val leftSideOfLeftSelector = leftSelectorX - touchSelectorArea
                val rightSideOfLeftSelector = leftSelectorWithWidthX + touchSelectorArea

                val leftSideOfRightSelector = rightSelectorX - touchSelectorArea
                val rightSideOfRightSelector = rightSelectorWithWidthX + touchSelectorArea

                when (xInsideBoundary) {
                    in leftSideOfLeftSelector..rightSideOfLeftSelector -> {
                        lastTouchedX = xInsideBoundary
                        leftSelectorDragging = true
                        invalidate()
                    }

                    in leftSideOfRightSelector..rightSideOfRightSelector -> {
                        lastTouchedX = xInsideBoundary
                        rightSelectorDragging = true
                        invalidate()
                    }
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                when {
                    leftSelectorDragging -> {
                        val movedSelectorX = leftSelectorX + (xInsideBoundary - lastTouchedX)
                        leftSelectorX = movedSelectorX.clamp(
                            min = 0f,
                            max = rightSelectorX - minSelectionWidth - selectorWidthF
                        )
                        lastTouchedX = xInsideBoundary.clamp(
                            min = 0f,
                            max = rightSelectorX - minSelectionWidth - selectorWidthF
                        )
                        invalidate()
                    }

                    rightSelectorDragging -> {
                        val movedSelectorX = rightSelectorX + (xInsideBoundary - lastTouchedX)
                        rightSelectorX = movedSelectorX.clamp(
                            min = leftSelectorWithWidthX + minSelectionWidth,
                            max = widthF - selectorWidthF
                        )
                        lastTouchedX = xInsideBoundary.clamp(
                            min = leftSelectorWithWidthX + minSelectionWidth,
                            max = widthF - selectorWidthF
                        )
                        invalidate()
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                leftSelectorDragging = false
                rightSelectorDragging = false
                lastTouchedX = Float.NaN
                invalidate()
                notifyNewSliceSelected()
                return false
            }

            else -> {
                return super.onTouchEvent(event)
            }
        }
    }
    /// endregion

    /// region Private
    private fun createPath() {
        if (widthF == 0f || points.count() < MIN_POINTS_LENGTH) return
        val step = widthF / points.count().dec()

        path.reset()

        path.moveTo(0f, halfOfHeight)
        points.forEachIndexed { index, point ->
            path.lineTo(index * step, halfOfHeight - point.second * halfOfHeight)
        }
        path.lineTo(widthF, halfOfHeight)
        path.close()

        path.moveTo(0f, halfOfHeight)
        points.forEachIndexed { index, point ->
            path.lineTo(index * step, halfOfHeight + -point.first * halfOfHeight)
        }
        path.lineTo(widthF, halfOfHeight)
        path.close()
    }

    private fun notifyNewSliceSelected() {
        selectedPoints = emptyList()

        if (widthF == 0f) return
        if (points.count() < MIN_POINTS_LENGTH) return
        if (leftSelectorX == 0f && rightSelectorWithWidthX == widthF) return

        val step = widthF / points.count().dec()
        val leftIndex = ceil(leftSelectorX / step).toInt()
        val rightIndex = (rightSelectorWithWidthX / step).toInt()

        Log.d(TAG, "new points:$leftIndex, $rightIndex from: ${points.size}")

        val subList = if (leftIndex >= rightIndex) {
            emptyList()
        } else {
            points.subList(leftIndex, rightIndex + 1)
        }
        selectedPoints = subList
        onNewWaveSelected(subList)
    }

    private fun getDefaultRightSelectorX() = if (widthF > 0f) widthF - selectorWidthF else 0f
    /// endregion
}