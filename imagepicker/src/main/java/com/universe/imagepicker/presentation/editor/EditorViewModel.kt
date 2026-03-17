package com.universe.imagepicker.presentation.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universe.imagepicker.domain.model.CropRect
import com.universe.imagepicker.domain.model.PickedImage
import com.universe.imagepicker.domain.usecase.CropImageUseCase
import com.universe.imagepicker.domain.usecase.RotateImageUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(
    originalUri: Uri,
    private val rotateImage: RotateImageUseCase,
    private val cropImage: CropImageUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(
        EditorState(originalUri = originalUri, previewUri = originalUri)
    )
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val _effect = Channel<EditorEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun handleIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.RotateClockwise -> rotate()
            is EditorIntent.EnterCropMode -> _state.update { it.copy(mode = EditorMode.CROPPING) }
            is EditorIntent.UpdateCropRect -> _state.update { it.copy(cropRect = intent.rect) }
            is EditorIntent.ApplyCrop -> applyCrop()
            is EditorIntent.ExitCropMode -> _state.update { it.copy(mode = EditorMode.NORMAL, cropRect = CropRect.FULL) }
            is EditorIntent.SaveAndReturn -> save()
            is EditorIntent.Cancel -> sendEffect(EditorEffect.Cancelled)
            is EditorIntent.DismissSaveError -> _state.update { it.copy(saveError = null) }
            is EditorIntent.RetrySave -> save()
        }
    }

    private fun rotate() {
        viewModelScope.launch {
            val current = _state.value
            runCatching {
                // 항상 originalUri 기준으로 회전 → JPEG 재압축 품질 손실 방지
                rotateImage(current.originalUri, current.rotationDegrees)
            }.onSuccess { (uri, degrees) ->
                _state.update { it.copy(previewUri = uri, rotationDegrees = degrees, hasUnsavedChanges = true) }
            }.onFailure { e ->
                sendEffect(EditorEffect.ShowError(e.message ?: "회전 처리에 실패했습니다."))
            }
        }
    }

    private fun applyCrop() {
        viewModelScope.launch {
            val current = _state.value
            runCatching {
                cropImage(current.previewUri, current.cropRect)
            }.onSuccess { uri ->
                _state.update { it.copy(previewUri = uri, mode = EditorMode.NORMAL, hasUnsavedChanges = true) }
            }.onFailure { e ->
                sendEffect(EditorEffect.ShowError(e.message ?: "크롭 처리에 실패했습니다."))
            }
        }
    }

    private fun save() {
        val current = _state.value
        _state.update { it.copy(isSaving = true, saveError = null) }
        val pickedImage = PickedImage(
            originalUri = current.originalUri,
            editedUri = if (current.hasUnsavedChanges) current.previewUri else null,
            rotationDegrees = current.rotationDegrees,
            cropRect = if (current.cropRect != CropRect.FULL) current.cropRect else null
        )
        _state.update { it.copy(isSaving = false) }
        sendEffect(EditorEffect.ReturnEditedImage(pickedImage))
    }

    private fun sendEffect(effect: EditorEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    override fun onCleared() {
        super.onCleared()
        _effect.close()
    }
}
