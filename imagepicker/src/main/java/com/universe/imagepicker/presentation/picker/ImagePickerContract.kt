package com.universe.imagepicker.presentation.picker

import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.domain.model.PermissionStatus
import com.universe.imagepicker.domain.model.PickerResult

interface ImagePickerContract {

    data class State(
        val permissionStatus: PermissionStatus = PermissionStatus.DENIED,
        val hasRequestedPermission: Boolean = false
    )

    sealed class Intent {
        object Initialize : Intent()
        object OnHostResumed : Intent()
        object RequestPermissionClick : Intent()
        object OpenSettingsClick : Intent()
        data class OnPermissionEvaluated(
            val status: PermissionStatus,
            val source: PermissionCheckSource
        ) : Intent()

        // 화면 이동
        data class OpenEditor(val image: GalleryImage) : Intent()

        // 완료/취소
        data class ConfirmSelection(val result: PickerResult) : Intent()
        object Cancel : Intent()
    }

    sealed class Effect {
        object NavigateToSettings : Effect()
        object RequestPermission : Effect()
        data class CheckPermission(val source: PermissionCheckSource) : Effect()
        data class NavigateToEditor(val image: GalleryImage, val entryId: Long) : Effect()
        data class ReturnResult(val result: PickerResult) : Effect()
        object Cancelled : Effect()
        data class ShowToast(val message: String) : Effect()
    }

    enum class PermissionCheckSource {
        INITIAL,
        RESUME,
        PERMISSION_RESULT,
        RETRY_BUTTON
    }
}
