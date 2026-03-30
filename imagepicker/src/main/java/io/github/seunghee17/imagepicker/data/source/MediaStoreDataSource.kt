package io.github.seunghee17.imagepicker.data.source

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import io.github.seunghee17.imagepicker.domain.model.GalleryAlbum
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Queries device gallery images (and optionally videos) via MediaStore ContentResolver.
 */
internal class MediaStoreDataSource(
    private val contentResolver: ContentResolver
) {
    private val imageCollection: Uri
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

    suspend fun queryImages(albumId: String? = null): List<GalleryImage> =
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.MIME_TYPE
            )

            val selection = albumId?.let { "${MediaStore.Images.Media.BUCKET_ID} = ?" }
            val selectionArgs = albumId?.let { arrayOf(it) }
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

            val images = mutableListOf<GalleryImage>()
            contentResolver.query(
                imageCollection, projection, selection, selectionArgs, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(imageCollection, id)
                    images += GalleryImage(
                        id = id,
                        uri = uri,
                        displayName = cursor.getString(nameCol) ?: "",
                        dateTaken = cursor.getLong(dateCol),
                        albumId = cursor.getString(bucketIdCol) ?: "",
                        albumName = cursor.getString(bucketNameCol) ?: "",
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        mimeType = cursor.getString(mimeCol) ?: ""
                    )
                }
            }
            images
        }

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
                        imageCount = count
                    )
                }
        }

    companion object {
        private const val BUCKET_ID = "bucket_id"
        private const val BUCKET_DISPLAY_NAME = "bucket_display_name"
    }
}
