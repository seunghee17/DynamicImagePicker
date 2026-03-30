package io.github.seunghee17.imagepicker.presentation.gallery

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.seunghee17.imagepicker.ImagePickerConfig
import io.github.seunghee17.imagepicker.data.repository.GalleryRepositoryImpl
import io.github.seunghee17.imagepicker.data.repository.ImageEditRepositoryImpl
import io.github.seunghee17.imagepicker.data.source.ImageFileDataSource
import io.github.seunghee17.imagepicker.data.source.MediaStoreDataSource
import io.github.seunghee17.imagepicker.domain.usecase.ClearEditCacheUseCase
import io.github.seunghee17.imagepicker.domain.usecase.GetGalleryAlbumsUseCase
import io.github.seunghee17.imagepicker.domain.usecase.GetPagedImagesUseCase

internal class GalleryScreenViewModelFactory(
    private val context: Context,
    private val config: ImagePickerConfig
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        val appContext = context.applicationContext
        val galleryRepository = GalleryRepositoryImpl(
            dataSource = MediaStoreDataSource(appContext.contentResolver),
            contentResolver = appContext.contentResolver,
            allowVideo = config.allowVideo,
        )
        val imageEditRepository = ImageEditRepositoryImpl(ImageFileDataSource(appContext))
        return GalleryScreenViewModel(
            getAlbums = GetGalleryAlbumsUseCase(galleryRepository),
            getPagedImages = GetPagedImagesUseCase(galleryRepository),
            clearEditCache = ClearEditCacheUseCase(imageEditRepository),
            maxSelectionCount = config.maxSelectionCount,
            showAlbumSelector = config.showAlbumSelector,
        ) as T
    }
}
