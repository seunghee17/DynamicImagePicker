package io.github.seunghee17.imagepicker.domain.repository

import androidx.paging.PagingData
import io.github.seunghee17.imagepicker.domain.model.GalleryAlbum
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import kotlinx.coroutines.flow.Flow

internal interface GalleryRepository {
    /**
     * 기기의 모든 앨범 목록을 Flow로 반환.
     * MediaStore 변경 시 자동으로 갱신된다.
     */
    fun getAlbums(): Flow<List<GalleryAlbum>>

    /**
     * 특정 앨범의 이미지를 페이지 단위로 반환
     * @param albumId null이면 전체 이미지(앨범 필터 없음)
     */
    fun getPagedImages(albumId: String?): Flow<PagingData<GalleryImage>>
}
