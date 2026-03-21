package com.universe.imagepicker.presentation.gallery

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.universe.imagepicker.presentation.component.TopBarWithCount
import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.presentation.utils.photoGridDragHandler

/// 갤러리 이미지 그리드 화면 (권한이 허용된 상태에서 표시).

@Composable
fun GalleryScreen(
    state: GalleryScreenState,
    onIntent: (GalleryScreenIntent) -> Unit,
    onOpenEditor: (GalleryImage) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val gridState = rememberLazyGridState()
    val autoScrollSpeed = remember { mutableFloatStateOf(0f) }
    var dropDownExpanded by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(state.selectionLimitMessage) {
        state.selectionLimitMessage?.let {
            snackbarHostState.showSnackbar(it)
            onIntent(GalleryScreenIntent.DismissSelectionLimitMessage)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopBarWithCount(
                selectedCount = state.selectedImages.size,
                maxCount = state.maxSelectionCount,
                onConfirm = onConfirm,
                onCancel = onCancel,
                albums = state.albums,
                dropDownExpanded = dropDownExpanded,
                openDropDown = { dropDownExpanded = true },
                closeDropDown = { dropDownExpanded = false },
                selectedAlbum = state.selectedAlbum,
                onAlbumSelected = { album ->
                    onIntent(GalleryScreenIntent.SelectAlbum(album))
                },
            )
        }
    ) { innerPadding ->
        if (state.isLoadingImages) {
            CircularProgressIndicator()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .photoGridDragHandler(
                        lazyGridState = gridState,
                        haptics = LocalHapticFeedback.current, //현재 화면에서 햅틱 피드백으로 진동기능을 제어할 수 있는 인스턴스 가져오는 compositionLocal
                        selectedImages = state.selectedImages,
                        onSelect = { id ->
                            state.images.firstOrNull { it.id == id }?.let { image ->
                                onIntent(GalleryScreenIntent.ToggleImageSelection(image))
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
                    val isSelected = selectedIndex >= 0

                    GalleryGridItem(
                        image = image,
                        selectionOrder = order,
                        onSelectionBadgeTap = {
                            onIntent(GalleryScreenIntent.ToggleImageSelection(image))
                        },
                        onOpenEditor = { onOpenEditor(image) }
                    )
                }
            }
        }
    }
}
