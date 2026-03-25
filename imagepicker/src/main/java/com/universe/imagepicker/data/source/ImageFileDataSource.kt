package com.universe.imagepicker.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import com.universe.imagepicker.domain.model.CropRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 이미지 회전/크롭 처리 및 캐시 파일 관리.
 *
 * 메모리 관리 전략:
 * - rotate(): 원본 바이트를 스트림으로 복사 후 EXIF TAG_ORIENTATION만 수정.
 *             비트맵 디코딩이 전혀 없으므로 OOM 위험 없음.
 *             Coil과 BitmapRegionDecoder 모두 EXIF를 읽으므로 올바른 방향으로 표시/크롭됨.
 * - crop(): BitmapRegionDecoder로 크롭 영역만 디코딩 → 전체 로드 대비 peak 메모리 대폭 절감.
 *           EXIF 방향 보정 후 좌표 변환, 디코딩된 비트맵에 회전을 픽셀로 적용하여 올바른 방향으로 저장.
 */
class ImageFileDataSource(
    private val context: Context
) {
    private val cacheDir: File
        get() = File(context.cacheDir, "imagepicker_edits").also { it.mkdirs() }

    /**
     * 비트맵 디코딩 없이 EXIF TAG_ORIENTATION만 수정하여 회전을 표현한다.
     *
     * [동작 원리]
     * 픽셀을 실제로 회전시키는 대신, 원본 파일 바이트를 캐시로 스트림 복사한 뒤
     * EXIF orientation 태그만 새로 씀. Coil과 BitmapRegionDecoder(crop에서 사용)는
     * 모두 EXIF를 읽으므로 올바른 방향으로 표시/처리된다.
     *
     * [EXIF 합성]
     * sourceUri(originalUri)가 이미 카메라 EXIF를 갖고 있을 수 있다(예: 90°).
     * 사용자 회전(degrees)은 그 위에 추가되므로 total = originalExif + degrees.
     * 캐시 파일에 total을 기록하면 이후 crop()의 transformCropRectForExif()가
     * 올바른 좌표 변환을 수행한다.
     *
     * [메모리]
     * 비트맵 할당 전혀 없음. 40MB JPEG에서도 peak 메모리 ≈ 0.
     */
    suspend fun rotate(sourceUri: Uri, degrees: Int): Uri =
        withContext(Dispatchers.IO) {
            val cacheFile = File(cacheDir, "rotate_${degrees}_${System.currentTimeMillis()}.jpg")

            // 1. 원본 바이트를 캐시 파일로 스트림 복사 (비트맵 디코딩 없음)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            } ?: error("InputStream을 열 수 없음: $sourceUri")

            // 2. 원본 EXIF 회전각 + 사용자 누적 회전각 합산
            val originalExifDegrees = readExifDegrees(sourceUri)
            val totalDegrees = (originalExifDegrees + degrees) % 360

            // 3. 합산 회전각을 EXIF orientation 상수로 변환
            val newOrientation = when (totalDegrees) {
                90  -> ExifInterface.ORIENTATION_ROTATE_90
                180 -> ExifInterface.ORIENTATION_ROTATE_180
                270 -> ExifInterface.ORIENTATION_ROTATE_270
                else -> ExifInterface.ORIENTATION_NORMAL
            }

            // 4. 캐시 파일의 EXIF orientation 덮어쓰기
            ExifInterface(cacheFile.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, newOrientation.toString())
                saveAttributes()
            }

            Uri.fromFile(cacheFile)
        }

    /**
     * BitmapRegionDecoder를 사용해 크롭 영역만 디코딩한 뒤, EXIF 회전을 픽셀에 적용하여 저장한다.
     *
     * [메모리]
     * 크롭 영역 비트맵만 처리하므로, 원본 전체 대비 peak 메모리가 크롭 비율만큼 줄어든다.
     * (예: 50% 크롭 → peak ≈ 원본의 50%)
     */
    suspend fun crop(sourceUri: Uri, cropRect: CropRect): Uri =
        withContext(Dispatchers.IO) {
            val exifDegrees = readExifDegrees(sourceUri)
            // 사용자 선택 좌표(표시 공간) → raw 픽셀 좌표로 역변환
            val adjustedRect = transformCropRectForExif(cropRect, exifDegrees)

            val decoded = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                val decoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // bitmapRegionDecoder은 EXIF를 무시하고 raw 픽셀
                    BitmapRegionDecoder.newInstance(stream)
                } else {
                    @Suppress("DEPRECATION")
                    BitmapRegionDecoder.newInstance(stream, false)
                } ?: error("BitmapRegionDecoder 초기화 실패: $sourceUri")

                try {
                    val rawW = decoder.width
                    val rawH = decoder.height

                    val x = (adjustedRect.left * rawW).toInt().coerceIn(0, rawW - 1)
                    val y = (adjustedRect.top * rawH).toInt().coerceIn(0, rawH - 1)
                    val r = (adjustedRect.right * rawW).toInt().coerceIn(x + 1, rawW)
                    val b = (adjustedRect.bottom * rawH).toInt().coerceIn(y + 1, rawH)

                    decoder.decodeRegion(android.graphics.Rect(x, y, r, b), BitmapFactory.Options())
                        ?: error("영역 디코딩 실패")
                } finally {
                    decoder.recycle()
                }
            } ?: error("InputStream을 열 수 없음: $sourceUri")

            // raw 방향 비트맵에 EXIF 회전을 픽셀로 적용 → 올바른 방향으로 저장
            // (저장 파일에는 EXIF가 없으므로, Coil이 회전 없이 그대로 표시하도록 픽셀을 맞춰둔다)
            val oriented = applyRotationToBitmap(decoded, exifDegrees)
            saveToCacheFile(oriented, "crop_${System.currentTimeMillis()}.jpg")
        }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    // ── EXIF 처리 ──────────────────────────────────────────────────────────────

    /**
     * URI에서 EXIF 회전 각도를 읽는다.
     * file:// URI는 ExifInterface(path), content:// URI는 ExifInterface(InputStream)를 사용.
     * rotate()로 생성된 캐시 파일(file://)은 합산 회전각이 EXIF에 기록되어 있으므로 올바른 값이 반환됨.
     */
    private fun readExifDegrees(uri: Uri): Int {
        val orientation = runCatching {
            when (uri.scheme) {
                "file" -> {
                    val path = uri.path ?: return 0
                    ExifInterface(path).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }
                else -> {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        ExifInterface(stream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    } ?: ExifInterface.ORIENTATION_NORMAL
                }
            }
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            ExifInterface.ORIENTATION_TRANSPOSE -> 90   // 수평 반전 + 90도
            ExifInterface.ORIENTATION_TRANSVERSE -> 270 // 수평 반전 + 270도
            else -> 0
        }
    }

    /**
     * Coil이 표시하기 위해 보정한 방향(exifDegrees만큼 CCW 회전)을 역변환하여
     * cropRect를 raw-file 좌표계로 변환한다.
     *
     * Coil은 EXIF 90°(CW) 이미지를 CCW 90° 회전하여 표시한다.
     * UI의 cropRect는 Coil 표시 기준이므로, raw 좌표로 돌리려면
     * 그 역변환(=exifDegrees만큼 CW 회전)을 적용한다.
     *
     * 변환 공식 (L=left, T=top, R=right, B=bottom, 모두 [0,1]):
     *   0°   → no change
     *   90°  → (T, 1-R, B, 1-L)  [raw dims: (originalH, originalW)]
     *   180° → (1-R, 1-B, 1-L, 1-T)
     *   270° → (1-B, L, 1-T, R)  [raw dims: (originalH, originalW)]
     */
    private fun transformCropRectForExif(crop: CropRect, exifDegrees: Int): CropRect =
        when (exifDegrees) {
            90 -> safeCropRect(crop.top, 1f - crop.right, crop.bottom, 1f - crop.left)
            180 -> safeCropRect(1f - crop.right, 1f - crop.bottom, 1f - crop.left, 1f - crop.top)
            270 -> safeCropRect(1f - crop.bottom, crop.left, 1f - crop.top, crop.right)
            else -> crop
        }

    private fun safeCropRect(l: Float, t: Float, r: Float, b: Float): CropRect {
        val left = minOf(l, r)
        val top = minOf(t, b)
        val right = maxOf(l, r).coerceAtLeast(left + 0.001f)
        val bottom = maxOf(t, b).coerceAtLeast(top + 0.001f)
        return CropRect(
            left.coerceIn(0f, 1f),
            top.coerceIn(0f, 1f),
            right.coerceIn(0f, 1f),
            bottom.coerceIn(0f, 1f)
        )
    }

    // ── 공통 유틸 ──────────────────────────────────────────────────────────────

    /**
     * 비트맵에 회전을 픽셀로 적용한다.
     * crop() 결과 보정 전용: 크롭된 영역 비트맵(원본 대비 훨씬 작음)에만 사용하므로 메모리 부담이 적다.
     * degrees == 0이면 입력 비트맵을 그대로 반환한다.
     */
    private fun applyRotationToBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun saveToCacheFile(bitmap: Bitmap, fileName: String): Uri {
        val file = File(cacheDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            return Uri.fromFile(file)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }
}
