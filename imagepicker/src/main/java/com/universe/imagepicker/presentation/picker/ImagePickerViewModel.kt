package com.universe.imagepicker.presentation.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universe.imagepicker.domain.model.PermissionStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImagePickerViewModel : ViewModel() {

    private val _state = MutableStateFlow(ImagePickerContract.State())
    val state: StateFlow<ImagePickerContract.State> = _state.asStateFlow()

    /** 각 Effect는 Channel을 통해 정확히 1회만 소비된다. */
    private val _effect = Channel<ImagePickerContract.Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun handleIntent(intent: ImagePickerContract.Intent) {
        when (intent) {
            ImagePickerContract.Intent.Initialize -> sendEffect(
                ImagePickerContract.Effect.CheckPermission(ImagePickerContract.PermissionCheckSource.INITIAL)
            )
            ImagePickerContract.Intent.OnHostResumed -> sendEffect(
                ImagePickerContract.Effect.CheckPermission(ImagePickerContract.PermissionCheckSource.RESUME)
            )
            ImagePickerContract.Intent.RequestPermissionClick ->
                sendEffect(ImagePickerContract.Effect.RequestPermission)
            ImagePickerContract.Intent.OpenSettingsClick ->
                sendEffect(ImagePickerContract.Effect.NavigateToSettings)
            is ImagePickerContract.Intent.OnPermissionEvaluated -> handlePermissionEvaluated(
                status = intent.status,
                source = intent.source
            )
            is ImagePickerContract.Intent.OpenEditor ->
                sendEffect(ImagePickerContract.Effect.NavigateToEditor(intent.image))
            is ImagePickerContract.Intent.ConfirmSelection ->
                sendEffect(ImagePickerContract.Effect.ReturnResult(intent.result))
            ImagePickerContract.Intent.Cancel ->
                sendEffect(ImagePickerContract.Effect.Cancelled)
        }
    }

    private fun handlePermissionEvaluated(
        status: PermissionStatus,
        source: ImagePickerContract.PermissionCheckSource
    ) {
        _state.update { current ->
            current.copy(
                permissionStatus = status,
                hasRequestedPermission = current.hasRequestedPermission ||
                        source == ImagePickerContract.PermissionCheckSource.PERMISSION_RESULT
            )
        }

        when (status) {
            PermissionStatus.GRANTED -> Unit
            PermissionStatus.PARTIALLY_GRANTED -> {
                if (source != ImagePickerContract.PermissionCheckSource.RESUME) {
                    sendEffect(ImagePickerContract.Effect.RequestPermission)
                }
            }
            PermissionStatus.DENIED -> {
                if (source == ImagePickerContract.PermissionCheckSource.INITIAL ||
                    source == ImagePickerContract.PermissionCheckSource.RETRY_BUTTON
                ) {
                    sendEffect(ImagePickerContract.Effect.RequestPermission)
                }
            }
            PermissionStatus.PERMANENTLY_DENIED -> {
                if (source == ImagePickerContract.PermissionCheckSource.PERMISSION_RESULT ||
                    source == ImagePickerContract.PermissionCheckSource.RETRY_BUTTON
                ) {
                    sendEffect(ImagePickerContract.Effect.NavigateToSettings)
                }
            }
        }
    }

    private fun sendEffect(effect: ImagePickerContract.Effect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    override fun onCleared() {
        super.onCleared()
        _effect.close()
    }
}
