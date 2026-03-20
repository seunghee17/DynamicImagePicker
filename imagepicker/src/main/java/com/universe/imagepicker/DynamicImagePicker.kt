package com.universe.imagepicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.universe.imagepicker.domain.model.PickerResult
import com.universe.imagepicker.presentation.picker.ImagePickerScreen

/**
 * DynamicImagePicker 라이브러리의 공개 진입점.
 */
@Composable
fun DynamicImagePicker(
    config: ImagePickerConfig = ImagePickerConfig(),
    onResult: (PickerResult) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    ImagePickerScreen(
        config = config,
        onResult = onResult,
        onCancel = onCancel,
        modifier = modifier
    )
}