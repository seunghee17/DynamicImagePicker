package com.universe.imagepicker.presentation.gallery

import com.universe.imagepicker.domain.model.GalleryAlbum
import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.PickedImage
import com.universe.imagepicker.PickerResult

internal interface GalleryContract {

    data class State(
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

    sealed interface Intent {
        data object Initialize : Intent
        data class SelectAlbum(val album: GalleryAlbum) : Intent
        data class ToggleImageSelection(val image: GalleryImage) : Intent
        data class OnEditResult(val pickedImage: PickedImage) : Intent
        data object Confirm : Intent
        data object Cancel : Intent
    }

    sealed interface Effect {
        data class ShowSelectionLimitSnackbar(val message: String) : Effect
        data class SelectionConfirmed(val result: PickerResult) : Effect
        data object Cancelled : Effect
    }
}