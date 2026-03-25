package io.github.seunghee17.imagepicker.presentation.editor

import android.net.Uri
import io.github.seunghee17.imagepicker.CropRect
import io.github.seunghee17.imagepicker.PickedImage

internal interface EditorContract {

    enum class Mode { NORMAL, CROPPING }

    data class State(
        val originalUri: Uri,

        /**
         * 마지막 크롭 확정 이후의 기준 이미지.
         * - 크롭 미적용 시: originalUri
         * - 크롭 확정 시: 크롭 결과 파일로 교체됨
         * 회전은 이 위에 EXIF로 쌓이므로 메모리 부담이 없다.
         */
        val committedUri: Uri,

        /**
         * committedUri 위에 아직 픽셀로 굽지 않은 회전각 (0/90/180/270).
         * 크롭 확정 시 0으로 리셋된다 (회전이 크롭 결과에 반영됐으므로).
         */
        val pendingRotation: Int = 0,

        /** PickedImage 메타데이터용 총 누적 회전각 */
        val totalRotation: Int = 0,

        /** 현재 화면에 표시되는 이미지 = rotate(committedUri, pendingRotation) */
        val previewUri: Uri,

        val cropRect: CropRect = CropRect.FULL,
        val cropRectOnEnter: CropRect = CropRect.FULL,
        val mode: Mode = Mode.NORMAL,
        val isSaving: Boolean = false,
        val hasUnsavedChanges: Boolean = false,
        /** 크롭이 한 번 이상 확정(ApplyCrop) 된 경우 true */
        val hasCropApplied: Boolean = false,
    )

    sealed interface Intent {
        data object RotateClockwise : Intent
        data object EnterCropMode : Intent
        data class UpdateCropRect(val rect: CropRect) : Intent
        data object ApplyCrop : Intent
        data object ExitCropMode : Intent
        data object SaveAndReturn : Intent
        data object Cancel : Intent
    }

    sealed interface Effect {
        data class ReturnEditedImage(val pickedImage: PickedImage) : Effect
        data object Cancelled : Effect
        data class ShowError(val message: String) : Effect
    }
}