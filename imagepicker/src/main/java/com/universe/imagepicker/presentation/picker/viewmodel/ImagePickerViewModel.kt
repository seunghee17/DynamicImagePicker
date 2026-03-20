package com.universe.imagepicker.presentation.picker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universe.imagepicker.domain.model.PermissionStatus
import com.universe.imagepicker.presentation.picker.ImagePickerEffect
import com.universe.imagepicker.presentation.picker.ImagePickerIntent
import com.universe.imagepicker.presentation.picker.ImagePickerState
import com.universe.imagepicker.presentation.picker.PermissionCheckSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImagePickerViewModel : ViewModel() {

    private val _state = MutableStateFlow(ImagePickerState())
    val state: StateFlow<ImagePickerState> = _state.asStateFlow()

    /** 각 Effect는 Channel을 통해 정확히 1회만 소비된다. */
    private val _effect = Channel<ImagePickerEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun handleIntent(intent: ImagePickerIntent) {
        when (intent) {
            ImagePickerIntent.Initialize -> sendEffect(
                ImagePickerEffect.CheckPermission(PermissionCheckSource.INITIAL)
            )
            ImagePickerIntent.OnHostResumed -> sendEffect(
                ImagePickerEffect.CheckPermission(PermissionCheckSource.RESUME)
            )
            ImagePickerIntent.RequestPermissionClick -> sendEffect(ImagePickerEffect.RequestPermission)
            ImagePickerIntent.OpenSettingsClick -> sendEffect(ImagePickerEffect.NavigateToSettings)
            is ImagePickerIntent.OnPermissionEvaluated -> handlePermissionEvaluated(
                status = intent.status,
                source = intent.source
            )
            is ImagePickerIntent.OpenEditor -> sendEffect(ImagePickerEffect.NavigateToEditor(intent.image))
            is ImagePickerIntent.ConfirmSelection -> {
                sendEffect(ImagePickerEffect.ReturnResult(intent.result))
            }
            is ImagePickerIntent.Cancel -> sendEffect(ImagePickerEffect.Cancelled)
        }
    }

    private fun handlePermissionEvaluated(
        status: PermissionStatus,
        source: PermissionCheckSource
    ) {
        _state.update { current ->
            current.copy(
                permissionStatus = status,
                hasRequestedPermission = current.hasRequestedPermission || source == PermissionCheckSource.PERMISSION_RESULT
            )
        }

        when (status) {
            PermissionStatus.GRANTED -> Unit
            PermissionStatus.PARTIALLY_GRANTED -> {
                if (source != PermissionCheckSource.RESUME) {
                    sendEffect(ImagePickerEffect.RequestPermission)
                }
            }
            PermissionStatus.DENIED -> {
                if (source == PermissionCheckSource.INITIAL || source == PermissionCheckSource.RETRY_BUTTON) {
                    sendEffect(ImagePickerEffect.RequestPermission)
                }
            }
            PermissionStatus.PERMANENTLY_DENIED -> {
                if (
                    source == PermissionCheckSource.PERMISSION_RESULT ||
                    source == PermissionCheckSource.RETRY_BUTTON
                ) {
                    sendEffect(ImagePickerEffect.NavigateToSettings)
                }
            }
        }
    }

    private fun sendEffect(effect: ImagePickerEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    override fun onCleared() {
        super.onCleared()
        _effect.close()
    }
}
