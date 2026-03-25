package io.github.seunghee17.imagepicker.domain.usecase

import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import io.github.seunghee17.imagepicker.domain.repository.GalleryRepository

internal class GetImagesInAlbumUseCase(
    private val repository: GalleryRepository
) {
    /**
     * @param albumId null이면 전체 이미지(앨범 필터 없음)
     */
    suspend operator fun invoke(albumId: String?): List<GalleryImage> =
        repository.getImagesInAlbum(albumId)
}
