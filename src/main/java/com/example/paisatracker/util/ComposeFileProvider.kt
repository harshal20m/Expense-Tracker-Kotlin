package com.example.paisatracker.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ComposeFileProvider {
    fun getImageUri(context: Context): Uri {
        val imagesDir = File(context.filesDir, "images")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        val imageFile = File(
            imagesDir,
            "CAM_${System.currentTimeMillis()}.jpg"
        )
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }
}
