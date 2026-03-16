package com.universe.imagepicker.domain.model

import android.net.Uri

data class GalleryAlbum(
    val id: String,
    val name: String,
    val coverUri: Uri,
    val imageCount: Int
)
