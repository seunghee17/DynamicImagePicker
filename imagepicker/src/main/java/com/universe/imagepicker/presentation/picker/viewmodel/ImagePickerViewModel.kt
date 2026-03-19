package com.universe.imagepicker.presentation.picker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.domain.model.PermissionStatus
import com.universe.imagepicker.domain.model.PickedImage
import com.universe.imagepicker.domain.model.PickerResult
import com.universe.imagepicker.domain.usecase.GetGalleryAlbumsUseCase
import com.universe.imagepicker.domain.usecase.GetImagesInAlbumUseCase
import com.universe.imagepicker.presentation.picker.ImagePickerEffect
import com.universe.imagepicker.presentation.picker.ImagePickerIntent
import com.universe.imagepicker.presentation.picker.ImagePickerState
import com.universe.imagepicker.presentation.picker.PermissionCheckSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImagePickerViewModel(
    private val getAlbums: GetGalleryAlbumsUseCase,
    private val getImagesInAlbum: GetImagesInAlbumUseCase,
    maxSelectionCount: Int,
    //private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(ImagePickerState(maxSelectionCount = maxSelectionCount))
    val state: StateFlow<ImagePickerState> = _state.asStateFlow()

    /** 각 Effect는 Channel을 통해 정확히 1회만 소비된다. */
    private val _effect = Channel<ImagePickerEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    // observeAlbums 중복 호출 방지
    private var albumsObserved = false

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
            is ImagePickerIntent.SelectAlbum -> {
                _state.update { it.copy(selectedAlbum = intent.album) }
                loadImages(intent.album.id)
            }
            is ImagePickerIntent.ToggleImageSelection -> toggleSelection(intent.image)
            is ImagePickerIntent.OpenEditor -> sendEffect(ImagePickerEffect.NavigateToEditor(galleryImages = _state.value.images))
            is ImagePickerIntent.OnEditResult -> applyEditResult(intent.pickedImage)
            is ImagePickerIntent.ConfirmSelection -> confirmSelection()
            is ImagePickerIntent.Cancel -> sendEffect(ImagePickerEffect.Cancelled)
            is ImagePickerIntent.DismissError -> _state.update { it.copy(error = null) }
            is ImagePickerIntent.DismissSelectionLimitMessage -> {
                _state.update { it.copy(selectionLimitMessage = null) }
            }
        }
    }

    /// 계속해서 MediaStore 상태 반영해서 실시간 갱신하기
    private fun observeAlbums() {
        // 중복 Flow 구독 방지
        if (albumsObserved) return
        albumsObserved = true
        getAlbums()
            .onEach { albums ->
                _state.update { current ->
                    val selectedAlbum = current.selectedAlbum ?: albums.firstOrNull()
                    current.copy(albums = albums, selectedAlbum = selectedAlbum)
                }
                val albumId = _state.value.selectedAlbum?.id
                loadImages(albumId)
            }
            .launchIn(viewModelScope)
    }

    private fun loadImages(albumId: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingImages = true, error = null) }
            runCatching { getImagesInAlbum(albumId) }
                .onSuccess { images ->
                    _state.update { it.copy(images = images, isLoadingImages = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingImages = false, error = e.message) }
                }
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
            PermissionStatus.GRANTED -> observeAlbums()
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

    private fun toggleSelection(image: GalleryImage) {
        val current = _state.value
        val selected = current.selectedImages.toMutableList()

        if (selected.any { it.id == image.id }) {
            selected.removeAll { it.id == image.id }
            _state.update { it.copy(selectedImages = selected) }
        } else {
            if (current.isSelectionLimitReached) {
                _state.update { it.copy(selectionLimitMessage = "이미지는 최대 ${current.maxSelectionCount}장까지 선택할 수 있습니다.") }
                return
            }
            selected.add(image)
            _state.update { it.copy(selectedImages = selected) }
        }
    }

    // PickedImage를 editResults 맵에 저장
    private fun applyEditResult(pickedImage: PickedImage) {
        _state.update { current ->
            val targetId = current.selectedImages
                .firstOrNull { it.uri == pickedImage.originalUri }?.id
                ?: return@update current
            current.copy(editResults = current.editResults + (targetId to pickedImage))
        }
    }

    // editResults 맵과 병합하여 편집 결과 반영
    private fun confirmSelection() {
        val current = _state.value
        val items = current.selectedImages.map { image ->
            current.editResults[image.id] ?: PickedImage(originalUri = image.uri)
        }
        sendEffect(ImagePickerEffect.ReturnResult(PickerResult(items)))
    }

    private fun sendEffect(effect: ImagePickerEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    override fun onCleared() {
        super.onCleared()
        _effect.close()
    }
}
