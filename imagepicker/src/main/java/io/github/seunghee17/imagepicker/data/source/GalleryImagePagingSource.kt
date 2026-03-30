package io.github.seunghee17.imagepicker.data.source

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.os.Bundle
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import io.github.seunghee17.imagepicker.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PagingSource that queries both images and videos from MediaStore.Files.
 *
 * Uses offset-based paging via the Bundle query API (API 26+).
 * A ContentObserver is registered to invalidate the source when the gallery changes.
 */
internal class GalleryImagePagingSource(
    private val contentResolver: ContentResolver,
    private val albumId: String?,
    private val allowVideo: Boolean,
) : PagingSource<Int, GalleryImage>() {

    private val collection = MediaStore.Files.getContentUri("external")

    private val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) = invalidate()
    }

    init {
        contentResolver.registerContentObserver(collection, true, observer)
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
                val items = query(offset, limit)
                LoadResult.Page(
                    data = items,
                    prevKey = if (offset == 0) null else maxOf(0, offset - limit),
                    nextKey = if (items.size < limit) null else offset + items.size,
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }

    private fun query(offset: Int, limit: Int): List<GalleryImage> {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            BUCKET_ID,
            BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Video.VideoColumns.DURATION,
        )

        val mediaTypeSelection = if (allowVideo) {
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN " +
                "(${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}," +
                "${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
        } else {
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}"
        }

        val queryArgs = Bundle().apply {
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.MediaColumns.DATE_TAKEN),
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
            if (albumId != null) {
                putString(
                    ContentResolver.QUERY_ARG_SQL_SELECTION,
                    "$mediaTypeSelection AND $BUCKET_ID = ?",
                )
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(albumId))
            } else {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, mediaTypeSelection)
            }
        }

        val items = mutableListOf<GalleryImage>()
        contentResolver.query(collection, projection, queryArgs, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val bucketIdCol = cursor.getColumnIndexOrThrow(BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(BUCKET_DISPLAY_NAME)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val mediaTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val mediaTypeInt = cursor.getInt(mediaTypeCol)
                val mediaType = if (mediaTypeInt == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                    MediaType.VIDEO else MediaType.IMAGE
                items += GalleryImage(
                    id = id,
                    uri = uri,
                    displayName = cursor.getString(nameCol) ?: "",
                    dateTaken = cursor.getLong(dateCol),
                    albumId = cursor.getString(bucketIdCol) ?: "",
                    albumName = cursor.getString(bucketNameCol) ?: "",
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    mimeType = cursor.getString(mimeCol) ?: "",
                    mediaType = mediaType,
                    videoDuration = if (mediaType == MediaType.VIDEO) cursor.getLong(durationCol) else 0L,
                )
            }
        }
        return items
    }

    companion object {
        // "bucket_id" and "bucket_display_name" are stable column names available for
        // MediaStore.Files queries across all supported API levels (26+).
        private const val BUCKET_ID = "bucket_id"
        private const val BUCKET_DISPLAY_NAME = "bucket_display_name"
    }
}
