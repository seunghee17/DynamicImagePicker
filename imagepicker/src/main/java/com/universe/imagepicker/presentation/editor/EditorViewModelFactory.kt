package com.universe.imagepicker.presentation.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.universe.imagepicker.data.repository.ImageEditRepositoryImpl
import com.universe.imagepicker.data.source.ImageFileDataSource
import com.universe.imagepicker.domain.usecase.CropImageUseCase
import com.universe.imagepicker.domain.usecase.RotateImageUseCase

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