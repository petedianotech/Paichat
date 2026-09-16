package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object ImageCompressorHelper {
    private const val TAG = "ImageCompressorHelper"

    fun compressImageForMms(
        context: Context,
        inputUriString: String,
        qualityMode: String
    ): String {
        try {
            val uri = Uri.parse(inputUriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return inputUriString
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                return inputUriString
            }

            // Determine target quality and max dimension based on qualityMode setting
            val (quality, maxDim) = when (qualityMode.uppercase()) {
                "ORIGINAL" -> return inputUriString // No compression requested
                "HIGH" -> Pair(85, 1600)
                "LOW" -> Pair(40, 800)
                else -> Pair(65, 1200) // "NORMAL" or default
            }

            // Check if resizing is needed
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scaledBitmap = if (width > maxDim || height > maxDim) {
                val ratio = width.toFloat() / height.toFloat()
                val (newWidth, newHeight) = if (ratio > 1) {
                    Pair(maxDim, (maxDim / ratio).toInt())
                } else {
                    Pair((maxDim * ratio).toInt(), maxDim)
                }
                Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            } else {
                originalBitmap
            }

            // Save to temp cache file
            val outputDir = context.cacheDir
            val compressedFile = File.createTempFile("mms_compressed_", ".jpg", outputDir)
            val outStream = FileOutputStream(compressedFile)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
            outStream.flush()
            outStream.close()

            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()

            return Uri.fromFile(compressedFile).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress image", e)
            return inputUriString
        }
    }
}
