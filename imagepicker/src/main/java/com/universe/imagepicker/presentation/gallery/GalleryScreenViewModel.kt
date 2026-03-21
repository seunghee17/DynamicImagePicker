package com.universe.imagepicker.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universe.imagepicker.domain.model.PickedImage
import com.universe.imagepicker.domain.model.PickerResult
import com.universe.imagepicker.domain.usecase.GetGalleryAlbumsUseCase
import com.universe.imagepicker.domain.usecase.GetImagesInAlbumUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GalleryScreenViewModel(
    private val getAlbums: GetGalleryAlbumsUseCase,
    private val getImagesInAlbum: GetImagesInAlbumUseCase,
    maxSelectionCount: Int
) : ViewModel() {

    private val _state = MutableStateFlow(GalleryScreenState(maxSelectionCount = maxSelectionCount))
    val state: StateFlow<GalleryScreenState> = _state.asStateFlow()

    private var albumsObserved = false

    fun handleIntent(intent: GalleryScreenIntent) {
        when (intent) {
            GalleryScreenIntent.Initialize -> observeAlbums()
            is GalleryScreenIntent.SelectAlbum -> {
                _state.update { it.copy(selectedAlbum = intent.album) }
                loadImages(intent.album.id)
            }
            is GalleryScreenIntent.ToggleImageSelection -> toggleSelection(intent.image)
            is GalleryScreenIntent.OnEditResult -> applyEditResult(intent.pickedImage)
            GalleryScreenIntent.ResetSelection -> resetSelection()
            GalleryScreenIntent.DismissSelectionLimitMessage -> {
                _state.update { it.copy(selectionLimitMessage = null) }
            }
        }
    }

    fun buildPickerResult(): PickerResult {
        val current = _state.value
        val items = current.selectedImages.map { image ->
            current.editResults[image.id] ?: PickedImage(originalUri = image.uri)
        }
        return PickerResult(items)
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

    private fun toggleSelection(image: com.universe.imagepicker.domain.model.GalleryImage) {
        val current = _state.value
        val selected = current.selectedImages.toMutableList()

        if (selected.any { it.id == image.id }) {
            selected.removeAll { it.id == image.id }
            _state.update { it.copy(selectedImages = selected) }
            return
        }

        if (current.isSelectionLimitReached) {
            _state.update {
                it.copy(
                    selectionLimitMessage = "이미지는 최대 ${current.maxSelectionCount}장까지 선택할 수 있습니다."
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

    private fun resetSelection() {
        _state.update {
            it.copy(
                selectedImages = emptyList(),
                selectionLimitMessage = null,
                editResults = emptyMap()
            )
        }
    }
}
