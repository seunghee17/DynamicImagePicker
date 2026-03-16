package com.universe.imagepicker.domain.usecase

import com.universe.imagepicker.domain.model.GalleryAlbum
import com.universe.imagepicker.domain.repository.GalleryRepository
import kotlinx.coroutines.flow.Flow

class GetGalleryAlbumsUseCase(
    private val repository: GalleryRepository
) {
    operator fun invoke(): Flow<List<GalleryAlbum>> = repository.getAlbums()
}
