package io.github.seunghee17.imagepicker.fake

import android.net.Uri
import io.github.seunghee17.imagepicker.CropRect
import io.github.seunghee17.imagepicker.domain.repository.ImageEditRepository

internal class FakeImageEditRepository : ImageEditRepository {

    var rotateResult: Uri? = null
    var cropResult: Uri? = null
    var rotateThrows: Boolean = false
    var cropThrows: Boolean = false
    var clearCacheCalled: Boolean = false

    var lastRotateSourceUri: Uri? = null
    var lastCropSourceUri: Uri? = null

    override suspend fun rotateImage(sourceUri: Uri, degrees: Int): Uri {
        lastRotateSourceUri = sourceUri
        if (rotateThrows) throw RuntimeException("Rotate failed")
        return rotateResult ?: sourceUri
    }

    override suspend fun cropImage(sourceUri: Uri, cropRect: CropRect): Uri {
        lastCropSourceUri = sourceUri
        if (cropThrows) throw RuntimeException("Crop failed")
        return cropResult ?: sourceUri
    }

    override suspend fun clearEditCache() {
        clearCacheCalled = true
    }
}
