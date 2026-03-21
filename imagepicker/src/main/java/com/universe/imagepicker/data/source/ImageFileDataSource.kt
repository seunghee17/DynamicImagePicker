package com.universe.imagepicker.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import com.universe.imagepicker.domain.model.CropRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Bitmap 기반 이미지 회전/크롭 처리 및 캐시 파일 관리.
 */
class ImageFileDataSource(
    private val context: Context
) {
    private val cacheDir: File
        get() = File(context.cacheDir, "imagepicker_edits").also { it.mkdirs() }

    suspend fun rotate(sourceUri: Uri, degrees: Int): Uri =
        withContext(Dispatchers.Default) {
            val bitmap = decodeBitmap(sourceUri)
            val normalizedDegrees = degrees % 360

            if (normalizedDegrees == 0) {
                return@withContext saveToCacheFile(
                    bitmap = bitmap,
                    fileName = "rotate_${degrees}_${System.currentTimeMillis()}.jpg"
                )
            }

            val matrix = Matrix().apply { postRotate(normalizedDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            if (rotated !== bitmap) {
                bitmap.recycle()
            }

            saveToCacheFile(
                bitmap = rotated,
                fileName = "rotate_${degrees}_${System.currentTimeMillis()}.jpg"
            )
        }

    suspend fun crop(sourceUri: Uri, cropRect: CropRect): Uri =
        withContext(Dispatchers.Default) {
            val bitmap = decodeBitmap(sourceUri)
            val x = (cropRect.left * bitmap.width).toInt()
            val y = (cropRect.top * bitmap.height).toInt()
            val w = ((cropRect.right - cropRect.left) * bitmap.width).toInt()
            val h = ((cropRect.bottom - cropRect.top) * bitmap.height).toInt()
            val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)

            if (cropped !== bitmap) {
                bitmap.recycle()
            }

            saveToCacheFile(cropped, "crop_${System.currentTimeMillis()}.jpg")
        }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    private fun decodeBitmap(uri: Uri): Bitmap {
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it) ?: error("Bitmap 디코딩 실패: $uri")
        } ?: error("InputStream을 열 수 없음: $uri")
    }

    private fun saveToCacheFile(bitmap: Bitmap, fileName: String): Uri {
        val file = File(cacheDir, fileName)
        try {
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
            }
            return Uri.fromFile(file)
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}
