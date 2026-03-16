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
class MediaStoreDataSource(
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
            // 앨범별 대표 이미지와 개수를 구하기 위해 전체 이미지를 조회 후 그룹핑, 재활용 가능
            val allImages = queryImages()
            allImages
                .groupBy { it.albumId }
                .map { (albumId, images) ->
                    GalleryAlbum(
                        id = albumId,
                        name = images.first().albumName,
                        coverUri = images.first().uri,
                        imageCount = images.size
                    )
                }
                .sortedByDescending { it.imageCount } // 사진수가 많은 앨범 먼저 나오도록
        }
}
