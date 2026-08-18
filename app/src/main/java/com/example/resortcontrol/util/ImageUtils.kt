package com.example.resortcontrol.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageUtils {

    private const val MAX_DIMENSION = 1024
    private const val COMPRESSION_QUALITY = 80
    private const val FOLDER_NAME = "cabin_photos"

    fun processAndSaveImage(context: Context, uri: Uri): String? {
        return try {
            val bitmap = decodeSampledBitmapFromUri(context, uri, MAX_DIMENSION) ?: return null
            val resizedBitmap = resizeBitmap(bitmap, MAX_DIMENSION)
            
            val photosDir = File(context.filesDir, FOLDER_NAME)
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }
            
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val file = File(photosDir, fileName)
            
            val out = FileOutputStream(file)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, out)
            out.flush()
            out.close()
            
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        var inSampleSize = 1
        if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
            val halfHeight: Int = options.outHeight / 2
            val halfWidth: Int = options.outWidth / 2
            while (halfHeight / inSampleSize >= maxDimension && halfWidth / inSampleSize >= maxDimension) {
                inSampleSize *= 2
            }
        }

        val finalInputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val finalOptions = BitmapFactory.Options().apply {
            inSampleSize = inSampleSize
        }
        val bitmap = BitmapFactory.decodeStream(finalInputStream, null, finalOptions)
        finalInputStream?.close()
        return bitmap
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxDimension && height <= maxDimension) return bitmap
        
        val ratio = width.toFloat() / height.toFloat()
        var newWidth = maxDimension
        var newHeight = maxDimension
        
        if (width > height) {
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newWidth = (maxDimension * ratio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
