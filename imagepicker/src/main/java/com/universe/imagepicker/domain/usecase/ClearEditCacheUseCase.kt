package com.universe.imagepicker.domain.usecase

import com.universe.imagepicker.domain.repository.ImageEditRepository

class ClearEditCacheUseCase(
    private val repository: ImageEditRepository
) {
    suspend operator fun invoke() = repository.clearEditCache()
}
