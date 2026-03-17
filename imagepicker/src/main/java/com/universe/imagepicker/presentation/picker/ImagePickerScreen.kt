package com.universe.imagepicker.presentation.picker

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universe.imagepicker.ImagePickerConfig
import com.universe.imagepicker.domain.model.PermissionStatus
import com.universe.imagepicker.domain.model.PickerResult
import com.universe.imagepicker.presentation.gallery.GalleryScreen
import com.universe.imagepicker.presentation.permission.PermissionScreen
import com.universe.imagepicker.presentation.picker.viewmodel.ImagePickerViewModel
import com.universe.imagepicker.presentation.picker.viewmodel.ImagePickerViewModelFactory

/**
 * 라이브러리 최상위 Composable.
 * 권한 상태에 따라 PermissionScreen 또는 GalleryScreen으로 분기한다.
 */
@Composable
fun ImagePickerScreen(
    config: ImagePickerConfig,
    onResult: (PickerResult) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // viewModelFactory를 통해 viewmodel 생성하면 viewmodel이 안드로이드 시스템에 의해 안전하게 관리되고 기존 viewmodel 인스턴스 유지하려 노력한다
    //
    val viewModel: ImagePickerViewModel = viewModel(factory = ImagePickerViewModelFactory(context = LocalContext.current))

    // Effect 수집 (일회성 이벤트 처리)
    // LaunchedEffect(Unit) {
    //     viewModel.effect.collect { effect ->
    //         when (effect) {
    //             is ImagePickerEffect.ReturnResult -> onResult(effect.result)
    //             is ImagePickerEffect.Cancelled -> onCancel()
    //             is ImagePickerEffect.NavigateToSettings -> { /* 설정 화면 이동 */ }
    //             is ImagePickerEffect.NavigateToEditor -> { /* 에디터 화면 이동 */ }
    //             is ImagePickerEffect.ShowToast -> { /* 토스트 표시 */ }
    //         }
    //     }
    // }

    // TODO: state 및 viewModel 연동 후 아래 임시 구현 교체
    PermissionScreen(
        permissionStatus = PermissionStatus.DENIED,
        onRequestPermission = {},
        onOpenSettings = {},
        modifier = modifier
    )
}
