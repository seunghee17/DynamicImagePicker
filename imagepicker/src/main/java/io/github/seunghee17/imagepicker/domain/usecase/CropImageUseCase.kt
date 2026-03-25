package io.github.seunghee17.imagepicker.domain.usecase

import android.net.Uri
import io.github.seunghee17.imagepicker.CropRect
import io.github.seunghee17.imagepicker.domain.repository.ImageEditRepository

internal class CropImageUseCase(
    private val repository: ImageEditRepository
) {
    suspend operator fun invoke(sourceUri: Uri, cropRect: CropRect): Uri =
        repository.cropImage(sourceUri, cropRect)
}
