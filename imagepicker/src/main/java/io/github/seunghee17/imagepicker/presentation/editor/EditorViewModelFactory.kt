package io.github.seunghee17.imagepicker.presentation.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.seunghee17.imagepicker.data.repository.ImageEditRepositoryImpl
import io.github.seunghee17.imagepicker.data.source.ImageFileDataSource
import io.github.seunghee17.imagepicker.domain.usecase.CropImageUseCase
import io.github.seunghee17.imagepicker.domain.usecase.RotateImageUseCase

internal class EditorViewModelFactory(
    private val originalUri: Uri,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        val dataSource = ImageFileDataSource(context.applicationContext)
        val repository = ImageEditRepositoryImpl(dataSource)
        return EditorViewModel(
            originalUri,
            RotateImageUseCase(repository),
            CropImageUseCase(repository),
        ) as T
    }
}