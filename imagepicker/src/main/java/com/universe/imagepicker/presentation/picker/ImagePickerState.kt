package com.universe.imagepicker.presentation.picker

import com.universe.imagepicker.domain.model.GalleryAlbum
import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.domain.model.PickedImage
import com.universe.imagepicker.domain.model.PermissionStatus

data class ImagePickerState(
    val permissionStatus: PermissionStatus = PermissionStatus.DENIED,
    val hasRequestedPermission: Boolean = false,
    val albums: List<GalleryAlbum> = emptyList(),
    val selectedAlbum: GalleryAlbum? = null,
    val images: List<GalleryImage> = emptyList(),
    /** 선택된 이미지 순서 보존 리스트. index + 1 = 배지 번호 */
    val selectedImages: List<GalleryImage> = emptyList(),
    val isLoadingImages: Boolean = false,
    val maxSelectionCount: Int = 10,
    val selectionLimitMessage: String? = null,
    val error: String? = null,
    /** 편집된 이미지 결과. Key = GalleryImage.id */
    val editResults: Map<Long, PickedImage> = emptyMap()
) {
    val isSelectionLimitReached: Boolean
        get() = selectedImages.size >= maxSelectionCount
}
