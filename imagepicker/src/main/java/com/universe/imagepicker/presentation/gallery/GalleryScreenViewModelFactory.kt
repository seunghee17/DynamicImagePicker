package com.universe.imagepicker.presentation.gallery

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.universe.imagepicker.ImagePickerConfig
import com.universe.imagepicker.data.repository.GalleryRepositoryImpl
import com.universe.imagepicker.data.repository.ImageEditRepositoryImpl
import com.universe.imagepicker.data.source.ImageFileDataSource
import com.universe.imagepicker.data.source.MediaStoreDataSource
import com.universe.imagepicker.domain.usecase.ClearEditCacheUseCase
import com.universe.imagepicker.domain.usecase.GetGalleryAlbumsUseCase
import com.universe.imagepicker.domain.usecase.GetImagesInAlbumUseCase

internal class GalleryScreenViewModelFactory(
    private val context: Context,
    private val config: ImagePickerConfig
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        val appContext = context.applicationContext
        val galleryRepository = GalleryRepositoryImpl(MediaStoreDataSource(appContext.contentResolver))
        val imageEditRepository = ImageEditRepositoryImpl(ImageFileDataSource(appContext))
        return GalleryScreenViewModel(
            getAlbums = GetGalleryAlbumsUseCase(galleryRepository),
            getImagesInAlbum = GetImagesInAlbumUseCase(galleryRepository),
            clearEditCache = ClearEditCacheUseCase(imageEditRepository),
            maxSelectionCount = config.maxSelectionCount
        ) as T
    }
}
