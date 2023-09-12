package com.ermolaevio.waveformeditor

import android.Manifest.permission
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ermolaevio.waveformeditor.utils.FileUtils
import com.ermolaevio.waveformeditor.utils.FileUtils.GetPointsResult
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), WaveFormCallback {

    private val registerOpenFileActivityResult = registerForActivityResult(
        /* contract = */ ActivityResultContracts.OpenDocument(),
        /* callback = */ ::openFile
    )
    private val registerForWritePermission = registerForActivityResult(
        /* contract = */ RequestMultiplePermissions(),
        /* callback = */ ::handlePermission
    )

    private lateinit var mainView: MainView
    private lateinit var waveFormEditorView: WaveFormEditorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainView = MainView(this)
        waveFormEditorView = mainView.waveFormEditorView
        setContentView(mainView)
    }

    override fun openFileClicked() {
        registerOpenFileActivityResult.launch(arrayOf("text/plain"))
    }

    override fun saveFileClicked() {
        saveFile()
    }

    private fun openFile(uri: Uri?) {
        if (uri == null) {
            return showMessage("Error occurred while opening file!")
        }

        lifecycleScope.launch {
            when (val result = FileUtils.getPointsFromFile(this@MainActivity, uri)) {
                is GetPointsResult.Success -> {
                    showPoints(result.points)
                }

                is GetPointsResult.FileIsTooBig -> {
                    showMessage("File is too big, imported only part")
                    showPoints(result.points)
                }

                is GetPointsResult.InvalidFile -> {
                    showMessage("File is invalid!")
                }

                is GetPointsResult.NotEnoughPoints -> {
                    showMessage("File must have at least ${result.minPoints} points!")
                }

                is GetPointsResult.UnknownError -> {
                    showMessage("Unknown error occurred while opening file!")
                }
            }
        }
    }

    private fun showPoints(points: List<Points>) {
        Log.d("DocumentPickerTag", "str: ${points.joinToString(separator = "\n")}")
        waveFormEditorView.isVisible = points.isNotEmpty()
        waveFormEditorView.setData(points)
    }

    private fun saveFile() {
        val points = waveFormEditorView.selectedPoints.orEmpty()

        if (points.isEmpty()) {
            return showMessage("Please choice a valid slice with at least 2 points!")
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasWritePermission()) {
            return registerForWritePermission.launch(arrayOf(permission.WRITE_EXTERNAL_STORAGE))
        }

        lifecycleScope.launch {
            val msg = when (FileUtils.savePoints(this@MainActivity, points)) {
                FileUtils.SaveFileResult.Success -> "Success!"
                FileUtils.SaveFileResult.Error -> "Error!"
            }
            showMessage(msg)
        }
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
    }

    private fun handlePermission(ignored: Map<String, Boolean>) {
        if (hasWritePermission()) {
            saveFile()
        } else {
            showMessage("Write permission not granted!")
        }
    }

    private fun hasWritePermission() = ContextCompat.checkSelfPermission(
        this,
        permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
}