package com.universe.imagepicker

/**
 * 이미지 피커 동작을 설정하는 구성 클래스.
 *
 * @param maxSelectionCount 최대 선택 가능 이미지 수 (기본값: 10)
 * @param showAlbumSelector 앨범 선택 드롭다운 표시 여부
 * @param allowEditing      편집(회전/크롭) 기능 제공 여부
 */
data class ImagePickerConfig(
    val maxSelectionCount: Int = 10,
    val showAlbumSelector: Boolean = true,
    val allowEditing: Boolean = true
)
