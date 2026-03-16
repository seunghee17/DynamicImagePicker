package com.universe.imagepicker.domain.usecase

import android.net.Uri
import com.universe.imagepicker.domain.model.CropRect
import com.universe.imagepicker.domain.repository.ImageEditRepository

class CropImageUseCase(
    private val repository: ImageEditRepository
) {
    suspend operator fun invoke(sourceUri: Uri, cropRect: CropRect): Uri =
        repository.cropImage(sourceUri, cropRect)
}
