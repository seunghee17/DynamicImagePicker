package com.universe.imagepicker.domain.model

import android.net.Uri

internal data class GalleryAlbum(
    val id: String,
    val name: String,
    val coverUri: Uri,
    val imageCount: Int
)
