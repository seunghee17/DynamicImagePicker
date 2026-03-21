package com.universe.imagepicker.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.domain.model.PickedImage
import com.universe.imagepicker.domain.model.PickerResult
import com.universe.imagepicker.domain.usecase.ClearEditCacheUseCase
import com.universe.imagepicker.domain.usecase.GetGalleryAlbumsUseCase
import com.universe.imagepicker.domain.usecase.GetImagesInAlbumUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GalleryScreenViewModel(
    private val getAlbums: GetGalleryAlbumsUseCase,
    private val getImagesInAlbum: GetImagesInAlbumUseCase,
    private val clearEditCache: ClearEditCacheUseCase,
    maxSelectionCount: Int
) : ViewModel() {

    private val _state = MutableStateFlow(GalleryContract.State(maxSelectionCount = maxSelectionCount))
    val state: StateFlow<GalleryContract.State> = _state.asStateFlow()

    private val _effect = Channel<GalleryContract.Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var albumsObserved = false

    // confirmSelection() 후 다음 세션 시작 시 캐시를 정리하기 위한 플래그
    private var pendingCacheClean = false

    fun handleIntent(intent: GalleryContract.Intent) {
        when (intent) {
            GalleryContract.Intent.Initialize -> {
                if (pendingCacheClean) {
                    pendingCacheClean = false
                    viewModelScope.launch { runCatching { clearEditCache() } }
                }
                observeAlbums()
            }
            is GalleryContract.Intent.SelectAlbum -> {
                _state.update { it.copy(selectedAlbum = intent.album) }
                loadImages(intent.album.id)
            }
            is GalleryContract.Intent.ToggleImageSelection -> toggleSelection(intent.image)
            is GalleryContract.Intent.OnEditResult -> applyEditResult(intent.pickedImage)
            GalleryContract.Intent.Confirm -> confirmSelection()
            GalleryContract.Intent.Cancel -> cancel()
        }
    }

    private fun observeAlbums() {
        if (albumsObserved) return
        albumsObserved = true
        getAlbums()
            .onEach { albums ->
                _state.update { current ->
                    val selectedAlbum = current.selectedAlbum ?: albums.firstOrNull()
                    current.copy(albums = albums, selectedAlbum = selectedAlbum)
                }
                loadImages(_state.value.selectedAlbum?.id)
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

    private fun toggleSelection(image: GalleryImage) {
        val current = _state.value
        val selected = current.selectedImages.toMutableList()

        if (selected.any { it.id == image.id }) {
            selected.removeAll { it.id == image.id }
            _state.update { it.copy(selectedImages = selected) }
            return
        }

        if (current.isSelectionLimitReached) {
            viewModelScope.launch {
                _effect.send(
                    GalleryContract.Effect.ShowSelectionLimitSnackbar(
                        "이미지는 최대 ${current.maxSelectionCount}장까지 선택할 수 있습니다."
                    )
                )
            }
            return
        }

        selected.add(image)
        _state.update { it.copy(selectedImages = selected) }
    }

    private fun applyEditResult(pickedImage: PickedImage) {
        _state.update { current ->
            val targetId = current.selectedImages
                .firstOrNull { it.uri == pickedImage.originalUri }
                ?.id
                ?: return@update current
            current.copy(editResults = current.editResults + (targetId to pickedImage))
        }
    }

    private fun confirmSelection() {
        val result = buildPickerResult()
        resetSelection()
        // 호스트 앱이 editedUri 파일을 사용할 수 있도록 다음 세션 시작 시 정리
        pendingCacheClean = true
        viewModelScope.launch {
            _effect.send(GalleryContract.Effect.SelectionConfirmed(result))
        }
    }

    private fun cancel() {
        resetSelection()
        // 결과를 반환하지 않으므로 캐시 파일 즉시 삭제
        viewModelScope.launch {
            _effect.send(GalleryContract.Effect.Cancelled)
            runCatching { clearEditCache() }
        }
    }

    private fun buildPickerResult(): PickerResult {
        val current = _state.value
        val items = current.selectedImages.map { image ->
            current.editResults[image.id] ?: PickedImage(originalUri = image.uri)
        }
        return PickerResult(items)
    }

    private fun resetSelection() {
        _state.update {
            it.copy(
                selectedImages = emptyList(),
                editResults = emptyMap()
            )
        }
    }
}
