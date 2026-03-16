package com.universe.imagepicker.domain.usecase

import android.net.Uri
import com.universe.imagepicker.domain.repository.ImageEditRepository

class RotateImageUseCase(
    private val repository: ImageEditRepository
) {
    /**
     * 원본 URI와 현재 누적 회전각을 기반으로 +90도 회전한 이미지를 생성한다.
     * 항상 originalUri 기준으로 회전하여 JPEG 재압축 품질 저하를 방지한다.
     *
     * @param originalUri 편집 전 원본 이미지 URI
     * @param currentDegrees EditorState에서 추적 중인 현재 누적 회전각
     * @return Pair(새 이미지 URI, 새 누적 회전각)
     */
    suspend operator fun invoke(originalUri: Uri, currentDegrees: Int): Pair<Uri, Int> {
        val newDegrees = (currentDegrees + 90) % 360
        val resultUri = repository.rotateImage(originalUri, newDegrees)
        return resultUri to newDegrees
    }
}
