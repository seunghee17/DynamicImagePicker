package com.universe.imagepicker.presentation.picker

import com.universe.imagepicker.domain.model.GalleryAlbum
import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.domain.model.PickedImage
import com.universe.imagepicker.domain.model.PermissionStatus

sealed class ImagePickerIntent {
    object Initialize : ImagePickerIntent()
    object OnHostResumed : ImagePickerIntent()
    object RequestPermissionClick : ImagePickerIntent()
    object OpenSettingsClick : ImagePickerIntent()
    data class OnPermissionEvaluated(
        val status: PermissionStatus,
        val source: PermissionCheckSource
    ) : ImagePickerIntent()

    // 앨범
    data class SelectAlbum(val album: GalleryAlbum) : ImagePickerIntent()

    // 이미지 선택
    data class ToggleImageSelection(val image: GalleryImage) : ImagePickerIntent()

    // 화면 이동
    data class OpenEditor(val selectedImages: List<GalleryImage>, val selectImageId: Long) : ImagePickerIntent()

    // 편집 결과 수신 (Editor → Picker 복귀 시)
    data class OnEditResult(val pickedImage: PickedImage) : ImagePickerIntent()

    // 완료/취소
    object ConfirmSelection : ImagePickerIntent()
    object Cancel : ImagePickerIntent()

    // 에러 처리
    object DismissError : ImagePickerIntent()
    object DismissSelectionLimitMessage : ImagePickerIntent()
}

enum class PermissionCheckSource {
    INITIAL,
    RESUME,
    PERMISSION_RESULT,
    RETRY_BUTTON
}
