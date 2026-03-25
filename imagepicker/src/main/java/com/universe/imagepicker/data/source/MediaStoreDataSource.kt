package com.universe.imagepicker.data.source

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.universe.imagepicker.domain.model.GalleryAlbum
import com.universe.imagepicker.domain.model.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MediaStore ContentResolver를 통해 기기 갤러리 이미지를 조회한다.
 */
internal class MediaStoreDataSource(
    private val contentResolver: ContentResolver
) {
    private val collection: Uri
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

            // 특정 앨범만 필터링
            val selection = albumId?.let { "${MediaStore.Images.Media.BUCKET_ID} = ?" }
            val selectionArgs = albumId?.let { arrayOf(it) }
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

            val images = mutableListOf<GalleryImage>()
            // 어떤 테이블 조회 / 어떤 컬럼 / 어떤 조건 / 조건 값 / 정렬 방식
            contentResolver.query(
                collection, projection, selection, selectionArgs, sortOrder
            )?.use { cursor -> // 끝나면 자동으로 cursor을 닫아 리소스 누수 방지
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID) // 이미지 고유 id
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME) // 파일 이름
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN) // 촬영 시각
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID) // 앨범 id
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME) // 앨범 이름
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH) // 이미지 너비
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT) // 이미지 높이
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE) // 이미지 타입

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
                        mimeType = cursor.getString(mimeCol) ?: ""
                    )
                }
            }
            images
        }

    suspend fun queryAlbums(): List<GalleryAlbum> =
        withContext(Dispatchers.IO) {
            // 앨범 목록만 조회하기 위한 경량 쿼리 (전체 이미지 로드 없이 버킷 정보만 조회)
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

            // 버킷별로 첫 번째 이미지(대표 이미지)와 개수를 수집
            val coverUriMap = mutableMapOf<String, Uri>()      // albumId → coverUri
            val albumNameMap = mutableMapOf<String, String>()  // albumId → albumName
            val albumCountMap = mutableMapOf<String, Int>()    // albumId → count

            contentResolver.query(
                collection, projection, null, null, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val bucketId = cursor.getString(bucketIdCol) ?: ""
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
}
