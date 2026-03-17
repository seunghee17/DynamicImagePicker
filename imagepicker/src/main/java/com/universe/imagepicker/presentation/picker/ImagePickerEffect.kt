package com.universe.imagepicker.presentation.picker

import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.domain.model.PickerResult

/**
 * 일회성 사이드 이펙트. Channel을 통해 정확히 1회만 소비된다.
 */
sealed class ImagePickerEffect {
    object NavigateToSettings : ImagePickerEffect()
    data class NavigateToEditor(val image: GalleryImage) : ImagePickerEffect()
    data class ReturnResult(val result: PickerResult) : ImagePickerEffect()
    object Cancelled : ImagePickerEffect()
    data class ShowToast(val message: String) : ImagePickerEffect()
}
