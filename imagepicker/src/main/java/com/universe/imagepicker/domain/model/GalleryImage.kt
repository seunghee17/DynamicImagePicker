package com.universe.imagepicker.domain.model

import android.net.Uri

internal data class GalleryImage(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateTaken: Long,        // epoch millis, 최신 순 정렬 기준
    val albumId: String,
    val albumName: String,
    val width: Int,
    val height: Int,
    val mimeType: String
)
