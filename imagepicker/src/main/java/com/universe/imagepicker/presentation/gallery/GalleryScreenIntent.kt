package com.universe.imagepicker.presentation.gallery

import com.universe.imagepicker.domain.model.GalleryAlbum
import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.domain.model.PickedImage

sealed class GalleryScreenIntent {
    object Initialize : GalleryScreenIntent()
    data class SelectAlbum(val album: GalleryAlbum) : GalleryScreenIntent()
    data class ToggleImageSelection(val image: GalleryImage) : GalleryScreenIntent()
    data class OnEditResult(val pickedImage: PickedImage) : GalleryScreenIntent()
    object ResetSelection : GalleryScreenIntent()
    object DismissSelectionLimitMessage : GalleryScreenIntent()
}
