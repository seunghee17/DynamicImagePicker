package io.github.seunghee17.imagepicker.domain.usecase

import io.github.seunghee17.imagepicker.domain.repository.ImageEditRepository

internal class ClearEditCacheUseCase(
    private val repository: ImageEditRepository
) {
    suspend operator fun invoke() = repository.clearEditCache()
}
