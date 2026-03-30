package io.github.seunghee17.imagepicker.domain.model

import android.net.Uri

internal data class GalleryImage(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateTaken: Long,
    val albumId: String,
    val albumName: String,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val mediaType: MediaType = MediaType.IMAGE,
    val videoDuration: Long = 0L,   // milliseconds, only meaningful when mediaType == VIDEO
)
