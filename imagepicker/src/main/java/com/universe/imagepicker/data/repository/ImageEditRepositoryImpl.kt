package com.universe.imagepicker.data.repository

import android.net.Uri
import com.universe.imagepicker.data.source.ImageFileDataSource
import com.universe.imagepicker.CropRect
import com.universe.imagepicker.domain.repository.ImageEditRepository

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
