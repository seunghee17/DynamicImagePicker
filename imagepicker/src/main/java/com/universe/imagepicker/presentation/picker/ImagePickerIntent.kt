package com.universe.imagepicker.presentation.picker

import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.domain.model.PermissionStatus
import com.universe.imagepicker.domain.model.PickerResult

sealed class ImagePickerIntent {
    object Initialize : ImagePickerIntent()
    object OnHostResumed : ImagePickerIntent()
    object RequestPermissionClick : ImagePickerIntent()
    object OpenSettingsClick : ImagePickerIntent()
    data class OnPermissionEvaluated(
        val status: PermissionStatus,
        val source: PermissionCheckSource
    ) : ImagePickerIntent()

    // 화면 이동
    data class OpenEditor(val image: GalleryImage) : ImagePickerIntent()

    // 완료/취소
    data class ConfirmSelection(val result: PickerResult) : ImagePickerIntent()
    object Cancel : ImagePickerIntent()
}

enum class PermissionCheckSource {
    INITIAL,
    RESUME,
    PERMISSION_RESULT,
    RETRY_BUTTON
}
