package com.universe.imagepicker.presentation.editor

import android.net.Uri
import com.universe.imagepicker.domain.model.CropRect
import com.universe.imagepicker.domain.model.PickedImage

enum class EditorMode { NORMAL, CROPPING }

data class EditorState(
    val originalUri: Uri,
    val previewUri: Uri,                        // 편집 전은 originalUri와 동일
    val rotationDegrees: Int = 0,
    val cropRect: CropRect = CropRect.FULL,
    val mode: EditorMode = EditorMode.NORMAL,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val hasUnsavedChanges: Boolean = false
)

sealed class EditorIntent {
    /** 시계 방향 90도 회전 */
    object RotateClockwise : EditorIntent()

    object EnterCropMode : EditorIntent()
    data class UpdateCropRect(val rect: CropRect) : EditorIntent()
    object ApplyCrop : EditorIntent()
    object ExitCropMode : EditorIntent()

    object SaveAndReturn : EditorIntent()
    object Cancel : EditorIntent()

    object DismissSaveError : EditorIntent()
    object RetrySave : EditorIntent()
}

sealed class EditorEffect {
    data class ReturnEditedImage(val pickedImage: PickedImage) : EditorEffect()
    object Cancelled : EditorEffect()
    data class ShowError(val message: String) : EditorEffect()
}
