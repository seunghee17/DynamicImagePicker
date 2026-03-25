package com.universe.imagepicker.domain.model

import android.net.Uri

data class PickedImage(
    val originalUri: Uri,
    val editedUri: Uri? = null,
    val rotationDegrees: Int = 0,       // 0, 90, 180, 270 (시계 방향)
    val cropRect: CropRect? = null,     // null = 크롭 좌표 미제공 (크롭 결과는 editedUri에 반영)
    val isCropped: Boolean = cropRect != null,  // cropRect가 없어도 true로 명시 가능
)
