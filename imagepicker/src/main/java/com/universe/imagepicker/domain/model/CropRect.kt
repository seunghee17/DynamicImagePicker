package com.universe.imagepicker.domain.model

/**
 * 정규화된 크롭 영역. 모든 값은 이미지 크기 대비 [0f, 1f] 범위.
 */
data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    init {
        require(left >= 0f && top >= 0f && right <= 1f && bottom <= 1f) {
            "CropRect 값은 [0f, 1f] 범위 내에 있어야 합니다."
        }
        require(left < right && top < bottom) {
            "left < right, top < bottom 이어야 합니다."
        }
    }

    companion object {
        /** 크롭 없음 (전체 이미지) */
        val FULL = CropRect(0f, 0f, 1f, 1f)
    }
}
