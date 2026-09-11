package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageCompressorHelper {

    suspend fun compressImageForMms(
        context: Context,
        inputUriString: String,
        qualityMode: String = "HIGH"
    ): String = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(inputUriString)
            val inputStream: InputStream? = if (inputUriString.startsWith("content://") || inputUriString.startsWith("file://")) {
                context.contentResolver.openInputStream(uri)
            } else {
                File(inputUriString).inputStream()
            }

            if (inputStream == null) return@withContext inputUriString

            // Determine max dimensions and compression quality
            val (maxDimension, jpegQuality) = when (qualityMode.uppercase()) {
                "LOW" -> Pair(800, 50)
                "MEDIUM" -> Pair(1280, 75)
                else -> Pair(1920, 90) // HIGH
            }

            // Decode dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext inputUriString

            // Calculate inSampleSize
            var inSampleSize = 1
            val maxOriginal = max(origWidth, origHeight)
            if (maxOriginal > maxDimension) {
                var halfMax = maxOriginal / 2
                while ((halfMax / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            // Decode sampled bitmap
            val secondStream = if (inputUriString.startsWith("content://") || inputUriString.startsWith("file://")) {
                context.contentResolver.openInputStream(uri)
            } else {
                File(inputUriString).inputStream()
            } ?: return@withContext inputUriString

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = BitmapFactory.decodeStream(secondStream, null, decodeOptions)
            secondStream.close()

            if (decodedBitmap == null) return@withContext inputUriString

            // Fix orientation if EXIF is available
            val rotatedBitmap = try {
                val exifStream = if (inputUriString.startsWith("content://") || inputUriString.startsWith("file://")) {
                    context.contentResolver.openInputStream(uri)
                } else {
                    File(inputUriString).inputStream()
                }
                if (exifStream != null) {
                    val exif = ExifInterface(exifStream)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    exifStream.close()
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                        Bitmap.createBitmap(decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true)
                    } else {
                        decodedBitmap
                    }
                } else {
                    decodedBitmap
                }
            } catch (_: Exception) {
                decodedBitmap
            }

            // Scale to exact max dimension if needed
            val currentMax = max(rotatedBitmap.width, rotatedBitmap.height)
            val finalBitmap = if (currentMax > maxDimension) {
                val scale = maxDimension.toFloat() / currentMax.toFloat()
                val targetW = (rotatedBitmap.width * scale).toInt()
                val targetH = (rotatedBitmap.height * scale).toInt()
                Bitmap.createScaledBitmap(rotatedBitmap, targetW, targetH, true)
            } else {
                rotatedBitmap
            }

            // Save compressed JPEG to cache dir
            val outputFile = File(context.cacheDir, "mms_opt_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outputFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
                out.flush()
            }

            if (finalBitmap != decodedBitmap && !finalBitmap.isRecycled) {
                finalBitmap.recycle()
            }
            if (rotatedBitmap != decodedBitmap && !rotatedBitmap.isRecycled) {
                rotatedBitmap.recycle()
            }
            if (!decodedBitmap.isRecycled) {
                decodedBitmap.recycle()
            }

            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            inputUriString
        }
    }
}
