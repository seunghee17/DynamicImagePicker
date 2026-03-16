package com.universe.imagepicker.data.repository

import com.universe.imagepicker.data.source.MediaStoreDataSource
import com.universe.imagepicker.domain.model.GalleryAlbum
import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.domain.repository.GalleryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GalleryRepositoryImpl(
    private val dataSource: MediaStoreDataSource
) : GalleryRepository {

    override fun getAlbums(): Flow<List<GalleryAlbum>> = flow {
        emit(dataSource.queryAlbums())
    }

    override suspend fun getImagesInAlbum(albumId: String?): List<GalleryImage> {
        return dataSource.queryImages(albumId)
    }
}
