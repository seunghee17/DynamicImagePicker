package com.universe.imagepicker.presentation.picker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.universe.imagepicker.ImagePickerConfig
import com.universe.imagepicker.data.repository.GalleryRepositoryImpl
import com.universe.imagepicker.data.source.MediaStoreDataSource
import com.universe.imagepicker.domain.usecase.GetGalleryAlbumsUseCase
import com.universe.imagepicker.domain.usecase.GetImagesInAlbumUseCase

internal class ImagePickerViewModelFactory(
    private val context: Context,
    private val config: ImagePickerConfig,
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        val dataSource = MediaStoreDataSource(context.contentResolver)
        val repository = GalleryRepositoryImpl(dataSource)
        return ImagePickerViewModel(
            GetGalleryAlbumsUseCase(repository),
            GetImagesInAlbumUseCase(repository),
            config.maxSelectionCount,
        ) as T
    }
}
