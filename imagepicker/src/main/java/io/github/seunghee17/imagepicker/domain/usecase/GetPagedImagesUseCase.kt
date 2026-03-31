package io.github.seunghee17.imagepicker.domain.usecase

import androidx.paging.PagingData
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import io.github.seunghee17.imagepicker.domain.repository.GalleryRepository
import kotlinx.coroutines.flow.Flow

internal class GetPagedImagesUseCase(
    private val repository: GalleryRepository,
) {
    /** @param albumId null이면 전체 이미지(앨범 필터 없음) */
    operator fun invoke(albumId: String?): Flow<PagingData<GalleryImage>> =
        repository.getPagedImages(albumId)
}
