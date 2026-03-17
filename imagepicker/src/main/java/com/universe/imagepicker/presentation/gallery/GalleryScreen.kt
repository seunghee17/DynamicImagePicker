package com.universe.imagepicker.presentation.gallery

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.universe.imagepicker.presentation.component.TopBarWithCount
import com.universe.imagepicker.presentation.picker.ImagePickerIntent
import com.universe.imagepicker.presentation.picker.ImagePickerState

/**
 * 갤러리 이미지 그리드 화면 (권한이 허용된 상태에서 표시).
 */
@Composable
fun GalleryScreen(
    state: ImagePickerState,
    onIntent: (ImagePickerIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.selectionLimitMessage) {
        state.selectionLimitMessage?.let {
            snackbarHostState.showSnackbar(it)
            onIntent(ImagePickerIntent.DismissSelectionLimitMessage)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopBarWithCount(
                selectedCount = state.selectedImages.size,
                maxCount = state.maxSelectionCount,
                onConfirm = { onIntent(ImagePickerIntent.ConfirmSelection) },
                onCancel = { onIntent(ImagePickerIntent.Cancel) }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(state.images, key = { it.id }) { image ->
                val order = state.selectedImages.indexOfFirst { it.id == image.id }
                    .takeIf { it >= 0 }?.let { it + 1 }

                GalleryGridItem(
                    image = image,
                    selectionOrder = order,
                    onTap = { onIntent(ImagePickerIntent.ToggleImageSelection(image)) },
                    onLongPress = { onIntent(ImagePickerIntent.StartDragSelection(image)) }
                )
            }
        }
    }
}
