package io.github.seunghee17.imagepicker.presentation.gallery

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import io.github.seunghee17.imagepicker.presentation.component.TopBarWithCount
import io.github.seunghee17.imagepicker.presentation.utils.gridItemKeyAtPosition
import io.github.seunghee17.imagepicker.presentation.utils.photoGridDragHandler
import io.github.seunghee17.imagepicker.PickerResult
import kotlinx.coroutines.delay

/// 갤러리 이미지 그리드 화면 (권한이 허용된 상태에서 표시).

@Composable
internal fun GalleryScreen(
    state: GalleryContract.State,
    snackbarHostState: SnackbarHostState,
    onIntent: (GalleryContract.Intent) -> Unit,
    onOpenEditor: (GalleryImage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val autoScrollSpeed = remember { mutableFloatStateOf(0f) }
    val currentDragOffset = remember { mutableStateOf<Offset?>(null) }
    val currentState by rememberUpdatedState(state)
    var dropDownExpanded by rememberSaveable { mutableStateOf(false) }

    // 화면이 컴포지션을 떠날 때(에디터 진입 등) 잔여 스낵바 제거
    DisposableEffect(Unit) {
        onDispose { snackbarHostState.currentSnackbarData?.dismiss() }
    }

    // 이미지 로드 실패 시 토스트 표시
    LaunchedEffect(state.error) {
        state.error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    // 드래그 중 autoScrollSpeed 값에 따라 그리드를 자동 스크롤
    // 스크롤 후 현재 손가락 위치의 아이템을 체크하여 선택
    LaunchedEffect(gridState) {
        while (true) {
            val speed = autoScrollSpeed.floatValue
            if (speed != 0f) {
                gridState.scrollBy(speed)
                currentDragOffset.value?.let { offset ->
                    gridState.gridItemKeyAtPosition(offset)?.let { key ->
                        if (currentState.selectedImages.none { it.id == key }) {
                            currentState.images.firstOrNull { it.id == key }?.let { image ->
                                onIntent(GalleryContract.Intent.ToggleImageSelection(image))
                            }
                        }
                    }
                }
            }
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
                        autoScrollThreshold = with(LocalDensity.current) { 40.dp.toPx() },
                        currentDragOffset = currentDragOffset
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
