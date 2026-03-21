package com.universe.imagepicker.presentation.editor

import android.net.Uri
import com.universe.imagepicker.domain.model.CropRect
import com.universe.imagepicker.domain.model.PickedImage

interface EditorContract {

    enum class Mode { NORMAL, CROPPING }

    data class State(
        val originalUri: Uri,
        val previewUri: Uri,                        // 편집 전은 originalUri와 동일
        val rotationDegrees: Int = 0,
        val cropRect: CropRect = CropRect.FULL,
        val mode: Mode = Mode.NORMAL,
        val isSaving: Boolean = false,
        val saveError: String? = null,
        val hasUnsavedChanges: Boolean = false
    )

    sealed class Intent {
        /** 시계 방향 90도 회전 */
        object RotateClockwise : Intent()

        object EnterCropMode : Intent()
        data class UpdateCropRect(val rect: CropRect) : Intent()
        object ApplyCrop : Intent()
        object ExitCropMode : Intent()

        object SaveAndReturn : Intent()
        object Cancel : Intent()

        object DismissSaveError : Intent()
        object RetrySave : Intent()
    }

    sealed class Effect {
        data class ReturnEditedImage(val pickedImage: PickedImage) : Effect()
        object Cancelled : Effect()
        data class ShowError(val message: String) : Effect()
    }
}
