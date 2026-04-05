package io.github.seunghee17.imagepicker.presentation.gallery

import androidx.compose.runtime.Stable
import io.github.seunghee17.imagepicker.domain.model.GalleryAlbum
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import io.github.seunghee17.imagepicker.PickedImage
import io.github.seunghee17.imagepicker.PickerResult

internal interface GalleryContract {

    @Stable
    data class State(
        val albums: List<GalleryAlbum> = emptyList(),
        val selectedAlbum: GalleryAlbum? = null,
        val selectedImages: List<GalleryImage> = emptyList(),
        val maxSelectionCount: Int = 10,
        val showAlbumSelector: Boolean = true,
        val editResults: Map<Long, PickedImage> = emptyMap(),
        val isAlbumsLoading: Boolean = true,
    ) {
        val isSelectionLimitReached: Boolean
            get() = selectedImages.size >= maxSelectionCount

        val selectionOrderMap: Map<Long, Int>
            get() = selectedImages.mapIndexed { idx, img -> img.id to (idx + 1) }.toMap()
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
        data class ShowSelectionLimitSnackbar(val maxSelectionCount: Int) : Effect
        data class SelectionConfirmed(val result: PickerResult) : Effect
        data object Cancelled : Effect
    }
}
