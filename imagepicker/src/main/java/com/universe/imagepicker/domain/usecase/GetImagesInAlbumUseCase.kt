package com.universe.imagepicker.domain.usecase

import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.domain.repository.GalleryRepository

class GetImagesInAlbumUseCase(
    private val repository: GalleryRepository
) {
    /**
     * @param albumId null이면 전체 이미지(앨범 필터 없음)
     */
    suspend operator fun invoke(albumId: String?): List<GalleryImage> =
        repository.getImagesInAlbum(albumId)
}
