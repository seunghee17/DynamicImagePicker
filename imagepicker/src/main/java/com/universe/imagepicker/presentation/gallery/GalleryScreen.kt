package com.universe.imagepicker.presentation.gallery

import android.widget.Toast
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.presentation.component.TopBarWithCount
import com.universe.imagepicker.presentation.utils.photoGridDragHandler
import com.universe.imagepicker.PickerResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/// 갤러리 이미지 그리드 화면 (권한이 허용된 상태에서 표시).

@Composable
internal fun GalleryScreen(
    state: GalleryContract.State,
    effect: Flow<GalleryContract.Effect>,
    onIntent: (GalleryContract.Intent) -> Unit,
    onOpenEditor: (GalleryImage) -> Unit,
    onConfirm: (PickerResult) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val gridState = rememberLazyGridState()
    val autoScrollSpeed = remember { mutableFloatStateOf(0f) }
    var dropDownExpanded by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(effect) {
        effect.collect { galleryEffect ->
            when (galleryEffect) {
                is GalleryContract.Effect.ShowSelectionLimitSnackbar ->
                    snackbarHostState.showSnackbar(galleryEffect.message)
                is GalleryContract.Effect.SelectionConfirmed -> onConfirm(galleryEffect.result)
                GalleryContract.Effect.Cancelled -> onCancel()
            }
        }
    }

    // 이미지 로드 실패 시 토스트 표시
    LaunchedEffect(state.error) {
        state.error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    // 드래그 중 autoScrollSpeed 값에 따라 그리드를 자동 스크롤
    LaunchedEffect(gridState) {
        while (true) {
            val speed = autoScrollSpeed.floatValue
            if (speed != 0f) gridState.scrollBy(speed)
            delay(16L) // ~60fps
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopBarWithCount(
                selectedCount = state.selectedImages.size,
                maxCount = state.maxSelectionCount,
                onConfirm = { onIntent(GalleryContract.Intent.Confirm) },
                onCancel = { onIntent(GalleryContract.Intent.Cancel) },
                albums = state.albums,
                dropDownExpanded = dropDownExpanded,
                openDropDown = { dropDownExpanded = true },
                closeDropDown = { dropDownExpanded = false },
                selectedAlbum = state.selectedAlbum,
                onAlbumSelected = { album ->
                    onIntent(GalleryContract.Intent.SelectAlbum(album))
                },
                showAlbumSelector = state.showAlbumSelector,
            )
        }
    ) { innerPadding ->
        if (state.isLoadingImages) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .photoGridDragHandler(
                        lazyGridState = gridState,
                        haptics = LocalHapticFeedback.current,
                        selectedImages = state.selectedImages,
                        onSelect = { id ->
                            state.images.firstOrNull { it.id == id }?.let { image ->
                                onIntent(GalleryContract.Intent.ToggleImageSelection(image))
                            }
                        },
                        autoScrollSpeed = autoScrollSpeed,
                        autoScrollThreshold = with(LocalDensity.current) { 40.dp.toPx() }
                    )
            ) {
                items(state.images, key = { it.id }) { image ->
                    val selectedIndex = state.selectedImages.indexOfFirst { it.id == image.id }
                    val order = selectedIndex
                        .takeIf { it >= 0 }?.let { it + 1 }

                    GalleryGridItem(
                        image = image,
                        selectionOrder = order,
                        onSelectionBadgeTap = {
                            onIntent(GalleryContract.Intent.ToggleImageSelection(image))
                        },
                        onOpenEditor = { onOpenEditor(image) }
                    )
                }
            }
        }
    }
}
