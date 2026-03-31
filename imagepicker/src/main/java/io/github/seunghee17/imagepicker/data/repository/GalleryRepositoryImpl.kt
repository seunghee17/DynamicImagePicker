package io.github.seunghee17.imagepicker.data.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.provider.MediaStore
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.github.seunghee17.imagepicker.data.source.GalleryImagePagingSource
import io.github.seunghee17.imagepicker.data.source.MediaStoreDataSource
import io.github.seunghee17.imagepicker.domain.model.GalleryAlbum
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import io.github.seunghee17.imagepicker.domain.repository.GalleryRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

internal class GalleryRepositoryImpl(
    private val dataSource: MediaStoreDataSource,
    private val contentResolver: ContentResolver,
    private val allowVideo: Boolean = false,
) : GalleryRepository {

    override fun getAlbums(): Flow<List<GalleryAlbum>> = callbackFlow {
        send(dataSource.queryAlbums(allowVideo))

        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                launch {
                    runCatching { dataSource.queryAlbums(allowVideo) }
                        .onSuccess { trySend(it) }
                }
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        if (allowVideo) {
            contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                observer,
            )
        }

        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }

    override fun getPagedImages(albumId: String?): Flow<PagingData<GalleryImage>> =
        Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = {
                GalleryImagePagingSource(contentResolver, albumId, allowVideo)
            },
        ).flow

    companion object {
        private const val PAGE_SIZE = 30
    }
}
