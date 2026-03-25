package com.universe.imagepicker.data.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.provider.MediaStore
import com.universe.imagepicker.data.source.MediaStoreDataSource
import com.universe.imagepicker.domain.model.GalleryAlbum
import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.domain.repository.GalleryRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

internal class GalleryRepositoryImpl(
    private val dataSource: MediaStoreDataSource,
    private val contentResolver: ContentResolver,
) : GalleryRepository {

    /**
     * 앨범 목록을 Flow로 반환한다.
     * ContentObserver로 MediaStore 변경을 감지하여 갤러리에 사진이 추가/삭제되면
     * 최신 앨범 목록을 재emit한다.
     */
    override fun getAlbums(): Flow<List<GalleryAlbum>> = callbackFlow {
        // 최초 emit
        send(dataSource.queryAlbums())

        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                launch {
                    runCatching { dataSource.queryAlbums() }
                        .onSuccess { trySend(it) }
                }
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }

    override suspend fun getImagesInAlbum(albumId: String?): List<GalleryImage> {
        return dataSource.queryImages(albumId)
    }
}