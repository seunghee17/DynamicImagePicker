package io.github.seunghee17.imagepicker.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import io.github.seunghee17.imagepicker.PickedImage
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import io.github.seunghee17.imagepicker.domain.model.MediaType
import io.github.seunghee17.imagepicker.domain.usecase.ClearEditCacheUseCase
import io.github.seunghee17.imagepicker.domain.usecase.GetGalleryAlbumsUseCase
import io.github.seunghee17.imagepicker.domain.usecase.GetPagedImagesUseCase
import io.github.seunghee17.imagepicker.PickerResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
internal class GalleryScreenViewModel(
    private val getAlbums: GetGalleryAlbumsUseCase,
    private val getPagedImages: GetPagedImagesUseCase,
    private val clearEditCache: ClearEditCacheUseCase,
    maxSelectionCount: Int,
    showAlbumSelector: Boolean = true,
) : ViewModel() {

    private val _state = MutableStateFlow(
        GalleryContract.State(
            maxSelectionCount = maxSelectionCount,
            showAlbumSelector = showAlbumSelector,
        )
    )
    val state: StateFlow<GalleryContract.State> = _state.asStateFlow()

    private val _effect = Channel<GalleryContract.Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()


    val pagingFlow: Flow<PagingData<GalleryImage>> = _state
        .filter { !it.isAlbumsLoading }
        .map { it.selectedAlbum?.id }
        .distinctUntilChanged()
        .flatMapLatest { albumId -> getPagedImages(albumId) }
        .cachedIn(viewModelScope)

    private var albumsObserved = false
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
            is GalleryContract.Intent.SelectAlbum ->
                _state.update { it.copy(selectedAlbum = intent.album) }
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
                    current.copy(
                        albums = albums,
                        selectedAlbum = current.selectedAlbum ?: albums.firstOrNull(),
                        isAlbumsLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun toggleSelection(image: GalleryImage) {
        val current = _state.value

        if (current.selectedImages.any { it.id == image.id }) {
            _state.update { it.copy(selectedImages = it.selectedImages.filter { img -> img.id != image.id }) }
            return
        }

        if (current.isSelectionLimitReached) {
            viewModelScope.launch {
                _effect.send(
                    GalleryContract.Effect.ShowSelectionLimitSnackbar(current.maxSelectionCount)
                )
            }
            return
        }

        _state.update { it.copy(selectedImages = it.selectedImages + image) }
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
        pendingCacheClean = true
        viewModelScope.launch {
            _effect.send(GalleryContract.Effect.SelectionConfirmed(result))
        }
    }

    private fun cancel() {
        resetSelection()
        viewModelScope.launch {
            _effect.send(GalleryContract.Effect.Cancelled)
            runCatching { clearEditCache() }
        }
    }

    private fun buildPickerResult(): PickerResult {
        val current = _state.value
        val items = current.selectedImages.map { image ->
            val base = current.editResults[image.id] ?: PickedImage(originalUri = image.uri)
            base.copy(
                isVideo = image.mediaType == MediaType.VIDEO,
                videoDurationMs = image.videoDuration,
            )
        }
        return PickerResult(items)
    }

    private fun resetSelection() {
        _state.update {
            it.copy(
                selectedImages = emptyList(),
                editResults = emptyMap(),
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        _effect.close()
    }
}
