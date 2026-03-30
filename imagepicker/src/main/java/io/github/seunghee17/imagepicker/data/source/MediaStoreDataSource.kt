package io.github.seunghee17.imagepicker.data.source

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import io.github.seunghee17.imagepicker.domain.model.GalleryAlbum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Queries device gallery items (images and/or videos) via MediaStore ContentResolver.
 * Image/video queries are delegated to GalleryImagePagingSource for pagination support.
 * This class handles album queries which return all media types matching the allowVideo filter.
 */
internal class MediaStoreDataSource(
    private val contentResolver: ContentResolver
) {

    suspend fun queryAlbums(allowVideo: Boolean = false): List<GalleryAlbum> =
        withContext(Dispatchers.IO) {
            // Use MediaStore.Files for a unified image+video album query.
            val collection = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                BUCKET_ID,
                BUCKET_DISPLAY_NAME,
            )
            val mediaTypeSelection = if (allowVideo) {
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN " +
                    "(${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}," +
                    "${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
            } else {
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}"
            }
            val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC"

            val coverUriMap = mutableMapOf<String, Uri>()
            val albumNameMap = mutableMapOf<String, String>()
            val albumCountMap = mutableMapOf<String, Int>()

            contentResolver.query(
                collection, projection, mediaTypeSelection, null, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val bucketIdCol = cursor.getColumnIndexOrThrow(BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val bucketId = cursor.getString(bucketIdCol) ?: continue
                    albumCountMap[bucketId] = (albumCountMap[bucketId] ?: 0) + 1
                    if (!coverUriMap.containsKey(bucketId)) {
                        val id = cursor.getLong(idCol)
                        coverUriMap[bucketId] = ContentUris.withAppendedId(collection, id)
                        albumNameMap[bucketId] = cursor.getString(bucketNameCol) ?: ""
                    }
                }
            }

            albumCountMap.entries
                .sortedByDescending { it.value }
                .mapNotNull { (albumId, count) ->
                    val coverUri = coverUriMap[albumId] ?: return@mapNotNull null
                    GalleryAlbum(
                        id = albumId,
                        name = albumNameMap[albumId] ?: "",
                        coverUri = coverUri,
                        count = count
                    )
                }
        }

    companion object {
        private const val BUCKET_ID = "bucket_id"
        private const val BUCKET_DISPLAY_NAME = "bucket_display_name"
    }
}
