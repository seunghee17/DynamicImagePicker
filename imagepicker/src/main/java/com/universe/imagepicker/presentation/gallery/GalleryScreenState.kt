package com.universe.imagepicker.presentation.gallery

import com.universe.imagepicker.domain.model.GalleryAlbum
import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.domain.model.PickedImage

data class GalleryScreenState(
    val albums: List<GalleryAlbum> = emptyList(),
    val selectedAlbum: GalleryAlbum? = null,
    val images: List<GalleryImage> = emptyList(),
    val selectedImages: List<GalleryImage> = emptyList(),
    val isLoadingImages: Boolean = false,
    val maxSelectionCount: Int = 10,
    val selectionLimitMessage: String? = null,
    val error: String? = null,
    val editResults: Map<Long, PickedImage> = emptyMap()
) {
    val isSelectionLimitReached: Boolean
        get() = selectedImages.size >= maxSelectionCount
}
