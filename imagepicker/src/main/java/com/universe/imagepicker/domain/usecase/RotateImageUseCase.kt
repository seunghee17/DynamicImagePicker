package com.universe.imagepicker.domain.usecase

import android.net.Uri
import com.universe.imagepicker.domain.repository.ImageEditRepository

internal class RotateImageUseCase(
    private val repository: ImageEditRepository
) {
    /**
     * [sourceUri]를 [degrees]도 회전한 이미지를 생성한다.
     * degrees는 회전각(0/90/180/270)을 받는다.
     */
    suspend operator fun invoke(sourceUri: Uri, degrees: Int): Uri =
        repository.rotateImage(sourceUri, degrees)
}
