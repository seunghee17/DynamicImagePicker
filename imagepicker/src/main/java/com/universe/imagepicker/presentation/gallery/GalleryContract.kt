package com.universe.imagepicker.presentation.gallery

import com.universe.imagepicker.domain.model.GalleryAlbum
import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.domain.model.PickedImage
import com.universe.imagepicker.domain.model.PickerResult

data class GalleryScreenState(
    val albums: List<GalleryAlbum> = emptyList(),
    val selectedAlbum: GalleryAlbum? = null,
    val images: List<GalleryImage> = emptyList(),
    val selectedImages: List<GalleryImage> = emptyList(),
    val isLoadingImages: Boolean = false,
    val maxSelectionCount: Int = 10,
    val error: String? = null,
    val editResults: Map<Long, PickedImage> = emptyMap()
) {
    val isSelectionLimitReached: Boolean
        get() = selectedImages.size >= maxSelectionCount
}

sealed class GalleryScreenIntent {
    object Initialize : GalleryScreenIntent()
    data class SelectAlbum(val album: GalleryAlbum) : GalleryScreenIntent()
    data class ToggleImageSelection(val image: GalleryImage) : GalleryScreenIntent()
    data class OnEditResult(val pickedImage: PickedImage) : GalleryScreenIntent()
    object Confirm : GalleryScreenIntent()
    object Cancel : GalleryScreenIntent()
}

sealed class GalleryScreenEffect {
    data class ShowSelectionLimitSnackbar(val message: String) : GalleryScreenEffect()
    data class SelectionConfirmed(val result: PickerResult) : GalleryScreenEffect()
    object Cancelled : GalleryScreenEffect()
}