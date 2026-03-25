package io.github.seunghee17.imagepicker.data.repository

import android.net.Uri
import io.github.seunghee17.imagepicker.data.source.ImageFileDataSource
import io.github.seunghee17.imagepicker.CropRect
import io.github.seunghee17.imagepicker.domain.repository.ImageEditRepository

internal class ImageEditRepositoryImpl(
    private val dataSource: ImageFileDataSource
) : ImageEditRepository {

    override suspend fun rotateImage(sourceUri: Uri, degrees: Int): Uri {
        return dataSource.rotate(sourceUri, degrees)
    }

    override suspend fun cropImage(sourceUri: Uri, cropRect: CropRect): Uri {
        return dataSource.crop(sourceUri, cropRect)
    }

    override suspend fun clearEditCache() {
        dataSource.clearCache()
    }
}
