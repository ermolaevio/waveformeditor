package com.ermolaevio.waveformeditor

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.ermolaevio.waveformeditor.utils.dp

interface WaveFormCallback {
    fun openFileClicked()
    fun saveFileClicked()
}

class MainView : LinearLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    val waveFormEditorView: WaveFormEditorView
    private val callback = context as WaveFormCallback

    init {
        orientation = VERTICAL
        layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(Color.BLACK)
        setPadding(size = dp(16))
        addView(
            WaveFormEditorView(context).also { editorView ->
                editorView.layoutParams = MarginLayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    editorView.dp(400)
                )
                editorView.isVisible = false
                waveFormEditorView = editorView
            }
        )

        addView(
            Button(context).also { button ->
                button.layoutParams = MarginLayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                button.text = "Open File Picker"
                button.setOnClickListener { callback.openFileClicked() }
            }
        )

        addView(
            Button(context).also { button ->
                button.layoutParams = MarginLayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                button.text = "Save new file"
                button.setOnClickListener { callback.saveFileClicked() }
            }
        )
    }
}