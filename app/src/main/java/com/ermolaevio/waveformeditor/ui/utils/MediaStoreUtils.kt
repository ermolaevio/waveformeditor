package com.ermolaevio.waveformeditor.ui.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi

object MediaStoreUtils {
    @RequiresApi(Build.VERSION_CODES.Q)
    fun createDownloadUri(context: Context, filename: String): Uri? {
        val downloadsCollection =
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val newFile = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
        }
        return context.contentResolver.insert(downloadsCollection, newFile)
    }
}