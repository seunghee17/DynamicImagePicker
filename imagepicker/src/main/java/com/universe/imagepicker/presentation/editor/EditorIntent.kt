package com.universe.imagepicker.presentation.editor

import com.universe.imagepicker.domain.model.CropRect

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
