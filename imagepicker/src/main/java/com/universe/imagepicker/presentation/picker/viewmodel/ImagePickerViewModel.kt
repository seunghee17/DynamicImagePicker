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
    //private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(ImagePickerState())
    val state: StateFlow<ImagePickerState> = _state.asStateFlow()

    /** 각 Effect는 Channel을 통해 정확히 1회만 소비된다. */
    private val _effect = Channel<ImagePickerEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    // observeAlbums 중복 호출 방지
    private var albumsObserved = false

    init {
        observeAlbums()
    }

    fun handleIntent(intent: ImagePickerIntent) {
        when (intent) {
            is ImagePickerIntent.OnPermissionResult -> onPermissionResult(intent.status)
            is ImagePickerIntent.RequestPermission -> { /* UI 레이어에서 처리 */ }
            is ImagePickerIntent.OpenAppSettings -> sendEffect(ImagePickerEffect.NavigateToSettings)
            is ImagePickerIntent.SelectAlbum -> loadImages(intent.album.id)
            is ImagePickerIntent.ToggleImageSelection -> toggleSelection(intent.image)
            is ImagePickerIntent.StartDragSelection -> startDrag(intent.image)
            is ImagePickerIntent.UpdateDragSelection -> updateDrag(intent.image)
            is ImagePickerIntent.EndDragSelection -> _state.update { it.copy(isDragSelecting = false, dragSelectAnchorId = null) }
            is ImagePickerIntent.OpenEditor -> sendEffect(ImagePickerEffect.NavigateToEditor(intent.image))
            is ImagePickerIntent.OnEditResult -> applyEditResult(intent.pickedImage)  // 편집 결과 map에 저장
            is ImagePickerIntent.ConfirmSelection -> confirmSelection()
            is ImagePickerIntent.Cancel -> sendEffect(ImagePickerEffect.Cancelled)
            is ImagePickerIntent.DismissError -> _state.update { it.copy(error = null) }
            is ImagePickerIntent.DismissSelectionLimitMessage -> _state.update { it.copy(selectionLimitMessage = null) }
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

    private fun onPermissionResult(status: PermissionStatus) {
        _state.update { it.copy(permissionStatus = status) }
        if (status == PermissionStatus.GRANTED) {
            observeAlbums()
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

    private fun startDrag(image: GalleryImage) {
        _state.update { it.copy(isDragSelecting = true, dragSelectAnchorId = image.id) }
        toggleSelection(image)
    }

    private fun updateDrag(image: GalleryImage) {
        if (!_state.value.isDragSelecting) return
        val current = _state.value
        if (current.selectedImages.none { it.id == image.id }) {
            toggleSelection(image)
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
