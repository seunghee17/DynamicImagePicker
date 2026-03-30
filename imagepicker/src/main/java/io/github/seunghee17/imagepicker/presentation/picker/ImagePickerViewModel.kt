package io.github.seunghee17.imagepicker.presentation.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.seunghee17.imagepicker.domain.model.PermissionStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ImagePickerViewModel : ViewModel() {

    private val _state = MutableStateFlow(ImagePickerContract.State())

    // Activity-scoped이므로 세션이 바뀌어도 절대 0으로 리셋되지 않음
    private var editorEntryCounter = 0L
    val state: StateFlow<ImagePickerContract.State> = _state.asStateFlow()

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
                sendEffect(ImagePickerContract.Effect.NavigateToEditor(intent.image, editorEntryCounter++))
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
        _state.update { current -> current.copy(permissionStatus = status) }

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
