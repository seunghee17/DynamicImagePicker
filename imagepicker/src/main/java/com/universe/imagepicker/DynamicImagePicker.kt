package com.universe.imagepicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.universe.imagepicker.domain.model.PickerResult
import com.universe.imagepicker.presentation.picker.ImagePickerScreen

/**
 * DynamicImagePicker 라이브러리의 공개 진입점.
 *
 * ## 사용 예시
 * ```kotlin
 * DynamicImagePicker.Content(
 *     config = ImagePickerConfig(maxSelectionCount = 5),
 *     onResult = { result -> handleResult(result) },
 *     onCancel = { finish() }
 * )
 * ```
 */
object DynamicImagePicker {

    /**
     * 피커를 전체 화면 Composable로 삽입한다.
     *
     * @param config    피커 동작 설정
     * @param onResult  선택/편집 완료 시 결과 콜백
     * @param onCancel  취소 시 콜백
     */
    @Composable
    fun Content(
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
}
