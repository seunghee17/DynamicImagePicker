package io.github.seunghee17.imagepicker.data.source

import android.content.ContentResolver
import android.database.ContentObserver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MediaStore 를 오프셋 기반으로 페이징하는 PagingSource.
 *
 * ContentObserver 를 내부에 등록하여 갤러리 변경(신규 사진 추가 등)이 감지되면
 * 자동으로 [invalidate] 를 호출한다. Pager 는 이 신호를 받아 새 PagingSource 를 생성한다.
 */
internal class GalleryImagePagingSource(
    private val contentResolver: ContentResolver,
    private val albumId: String?,
) : PagingSource<Int, GalleryImage>() {

    private val collection: Uri
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

    private val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) = invalidate()
    }

    init {
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        registerInvalidatedCallback {
            contentResolver.unregisterContentObserver(observer)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, GalleryImage>): Int? =
        state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(state.config.pageSize)
                ?: page?.nextKey?.minus(state.config.pageSize)
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GalleryImage> {
        val offset = params.key ?: 0
        val limit = params.loadSize
        return withContext(Dispatchers.IO) {
            try {
                val images = query(offset, limit)
                LoadResult.Page(
                    data = images,
                    prevKey = if (offset == 0) null else maxOf(0, offset - limit),
                    nextKey = if (images.size < limit) null else offset + images.size,
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }

    private fun query(offset: Int, limit: Int): List<GalleryImage> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
        )

        val queryArgs = Bundle().apply {
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Images.Media.DATE_TAKEN),
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
            if (albumId != null) {
                putString(
                    ContentResolver.QUERY_ARG_SQL_SELECTION,
                    "${MediaStore.Images.Media.BUCKET_ID} = ?",
                )
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(albumId))
            }
        }

        val images = mutableListOf<GalleryImage>()
        contentResolver.query(collection, projection, queryArgs, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                images += GalleryImage(
                    id = id,
                    uri = uri,
                    displayName = cursor.getString(nameCol) ?: "",
                    dateTaken = cursor.getLong(dateCol),
                    albumId = cursor.getString(bucketIdCol) ?: "",
                    albumName = cursor.getString(bucketNameCol) ?: "",
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    mimeType = cursor.getString(mimeCol) ?: "",
                )
            }
        }
        return images
    }
}
