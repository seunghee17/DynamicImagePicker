package com.universe.imagepicker.domain.repository

import com.universe.imagepicker.domain.model.GalleryAlbum
import com.universe.imagepicker.domain.model.GalleryImage
import kotlinx.coroutines.flow.Flow

interface GalleryRepository {
    /**
     * 기기의 모든 앨범 목록을 Flow로 반환.
     * MediaStore 변경 시 자동으로 갱신된다.
     */
    fun getAlbums(): Flow<List<GalleryAlbum>>

    /**
     * 특정 앨범의 이미지 목록을 dateTaken 내림차순으로 반환.
     * @param albumId null이면 전체 이미지(앨범 필터 없음)
     */
    suspend fun getImagesInAlbum(albumId: String?): List<GalleryImage>
}
