package com.universe.imagepicker.presentation.editor

import com.universe.imagepicker.domain.model.PickedImage

/**
 * 일회성 사이드 이펙트. Channel을 통해 정확히 1회만 소비된다.
 */
sealed class EditorEffect {
    data class ReturnEditedImage(val pickedImage: PickedImage) : EditorEffect()
    object Cancelled : EditorEffect()
    data class ShowError(val message: String) : EditorEffect()
}
