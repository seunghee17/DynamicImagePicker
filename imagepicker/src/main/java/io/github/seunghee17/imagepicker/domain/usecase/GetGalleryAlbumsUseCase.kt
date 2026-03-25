package io.github.seunghee17.imagepicker.domain.usecase

import io.github.seunghee17.imagepicker.domain.model.GalleryAlbum
import io.github.seunghee17.imagepicker.domain.repository.GalleryRepository
import kotlinx.coroutines.flow.Flow

internal class GetGalleryAlbumsUseCase(
    private val repository: GalleryRepository
) {
    operator fun invoke(): Flow<List<GalleryAlbum>> = repository.getAlbums()
}
