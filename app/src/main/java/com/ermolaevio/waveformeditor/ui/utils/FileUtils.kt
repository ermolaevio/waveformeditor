package com.ermolaevio.waveformeditor.ui.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import com.ermolaevio.waveformeditor.ui.Points
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter

private const val MAX_POINTS_LENGTH = 500
private const val MIN_POINTS_LENGTH = 3

internal object FileUtils {

    sealed class SaveFileResult {
        object Success : SaveFileResult()
        object Error : SaveFileResult()
    }

    sealed class GetPointsResult {
        data class Success(val points: List<Points>) : GetPointsResult()
        data class FileIsTooBig(val points: List<Points>) : GetPointsResult()
        data class NotEnoughPoints(val minPoints: Int) : GetPointsResult()
        object InvalidFile : GetPointsResult()
        object UnknownError : GetPointsResult()
    }

    suspend fun getPointsFromFile(context: Context, uri: Uri): GetPointsResult {
        return withContext(Dispatchers.IO) {
            val points = mutableListOf<Points>()
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(
                        InputStreamReader(inputStream, Charsets.US_ASCII)
                    ).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            val pairPointsStr = line.split(' ')
                            if (pairPointsStr.count() != 2) {
                                return@withContext GetPointsResult.InvalidFile
                            }

                            val pointF1 = pairPointsStr[0].toFloat()
                            val pointF2 = pairPointsStr[1].toFloat()

                            if (pointF1 !in -1f..0f || pointF2 !in 0f..1f) {
                                return@withContext GetPointsResult.InvalidFile
                            }

                            points += pointF1 to pointF2

                            if (points.count() > MAX_POINTS_LENGTH) {
                                return@withContext GetPointsResult.FileIsTooBig(points)
                            }

                            line = reader.readLine()
                        }
                    }
                }
            } catch (e: Exception) {
                return@withContext GetPointsResult.UnknownError
            }
            if (points.count() < MIN_POINTS_LENGTH) {
                return@withContext GetPointsResult.NotEnoughPoints(MIN_POINTS_LENGTH)
            }
            return@withContext GetPointsResult.Success(points)
        }
    }

    suspend fun savePoints(context: Context, points: List<Points>): SaveFileResult =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                createNewFileQImpl(context, points)
            } else {
                createNewFilePreQImpl(points)
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createNewFileQImpl(context: Context, points: List<Points>): SaveFileResult {
        val filename = generateFilename()
        val uri = MediaStoreUtils.createDownloadUri(context, filename)
            ?: return SaveFileResult.Error
        try {
            context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                output.write(points)
            }
        } catch (e: Exception) {
            return SaveFileResult.Error
        }
        return SaveFileResult.Success
    }

    private fun createNewFilePreQImpl(points: List<Points>): SaveFileResult {
        val downloadsFolder = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        var file = File(downloadsFolder, generateFilename())
        while (file.exists()) {
            file = File(downloadsFolder, generateFilename())
        }

        try {
            file.outputStream().use { output -> output.write(points) }
        } catch (e: IOException) {
            return SaveFileResult.Error
        }

        return SaveFileResult.Success
    }

    private fun OutputStream.write(points: List<Points>) {
        BufferedWriter(OutputStreamWriter(this, Charsets.US_ASCII)).use { writer ->
            points.forEachIndexed { index, point ->
                writer.write("${point.first} ${point.second}")
                if (index != points.lastIndex) {
                    writer.appendLine()
                }
            }
        }
    }

    private fun generateFilename() = "audiowave-${System.currentTimeMillis()}.txt"
}