package io.github.seunghee17.imagepicker.presentation.picker

import androidx.compose.runtime.Stable
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import io.github.seunghee17.imagepicker.domain.model.PermissionStatus
import io.github.seunghee17.imagepicker.PickerResult

internal interface ImagePickerContract {

    @Stable
    data class State(
        val permissionStatus: PermissionStatus = PermissionStatus.DENIED
    )

    sealed interface Intent {
        data object Initialize : Intent
        data object OnHostResumed : Intent
        data object RequestPermissionClick : Intent
        data object OpenSettingsClick : Intent
        data class OnPermissionEvaluated(
            val status: PermissionStatus,
            val source: PermissionCheckSource
        ) : Intent

        // 화면 이동
        data class OpenEditor(val image: GalleryImage) : Intent

        // 완료/취소
        data class ConfirmSelection(val result: PickerResult) : Intent
        data object Cancel : Intent
    }

    sealed interface Effect {
        data object NavigateToSettings : Effect
        data object RequestPermission : Effect
        data class CheckPermission(val source: PermissionCheckSource) : Effect
        data class NavigateToEditor(val image: GalleryImage, val entryId: Long) : Effect
        data class ReturnResult(val result: PickerResult) : Effect
        data object Cancelled : Effect
    }

    enum class PermissionCheckSource {
        INITIAL,
        RESUME,
        PERMISSION_RESULT,
        RETRY_BUTTON
    }
}
