package com.universe.imagepicker.domain.repository

import android.net.Uri
import com.universe.imagepicker.domain.model.CropRect

interface ImageEditRepository {
    /**
     * [sourceUri]의 이미지를 [degrees]도 시계 방향 회전한 결과를 캐시에 저장하고 URI 반환.
     * 원본 파일은 변경되지 않는다.
     */
    suspend fun rotateImage(sourceUri: Uri, degrees: Int): Uri

    /**
     * [sourceUri]의 이미지를 정규화된 [cropRect] 기준으로 크롭하여 캐시에 저장하고 URI 반환.
     * 원본 파일은 변경되지 않는다.
     */
    suspend fun cropImage(sourceUri: Uri, cropRect: CropRect): Uri

    /**
     * 세션 중 생성된 임시 편집 파일을 모두 삭제한다.
     */
    suspend fun clearEditCache()
}
