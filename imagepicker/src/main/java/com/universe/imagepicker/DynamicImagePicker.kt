package com.universe.imagepicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.universe.imagepicker.presentation.picker.ImagePickerScreen
import com.universe.imagepicker.PickerResult

/**
 * DynamicImagePicker 라이브러리의 공개 진입점.
 *
 * @param config       피커 동작 설정 (최대 선택 수, 앨범 표시, 편집 허용 여부)
 * @param onResult     선택/편집 완료 시 [PickerResult]를 전달
 * @param onCancel     사용자가 취소 버튼을 눌렀을 때 호출
 * @param onError      편집 처리 중 복구 불가 에러 발생 시 에러 메시지를 전달 (기본값: no-op)
 * @param modifier     Composable 레이아웃 수정자
 */
@Composable
fun DynamicImagePicker(
    config: ImagePickerConfig = ImagePickerConfig(),
    onResult: (PickerResult) -> Unit,
    onCancel: () -> Unit,
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    ImagePickerScreen(
        config = config,
        onResult = onResult,
        onCancel = onCancel,
        onError = onError,
        modifier = modifier
    )
}