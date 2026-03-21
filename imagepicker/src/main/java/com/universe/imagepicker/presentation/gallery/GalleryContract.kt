package com.universe.imagepicker.presentation.gallery

import com.universe.imagepicker.domain.model.GalleryAlbum
import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.domain.model.PickedImage
import com.universe.imagepicker.domain.model.PickerResult

interface GalleryContract {

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

    sealed class Intent {
        object Initialize : Intent()
        data class SelectAlbum(val album: GalleryAlbum) : Intent()
        data class ToggleImageSelection(val image: GalleryImage) : Intent()
        data class OnEditResult(val pickedImage: PickedImage) : Intent()
        object Confirm : Intent()
        object Cancel : Intent()
    }

    sealed class Effect {
        data class ShowSelectionLimitSnackbar(val message: String) : Effect()
        data class SelectionConfirmed(val result: PickerResult) : Effect()
        object Cancelled : Effect()
    }
}
