package com.universe.imagepicker.presentation.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.universe.imagepicker.domain.model.PermissionStatus

@Composable
internal fun PermissionFallbackContent(
    modifier: Modifier = Modifier,
    state: ImagePickerContract.State,
    onIntent: (ImagePickerContract.Intent) -> Unit,
) {
    val message = when (state.permissionStatus) {
        PermissionStatus.PARTIALLY_GRANTED ->
            "전체 갤러리를 표시하려면 전체 사진 접근 권한이 필요합니다."
        PermissionStatus.PERMANENTLY_DENIED ->
            "권한이 영구적으로 거부되었습니다. 앱 설정에서 사진 권한을 허용해 주세요."
        PermissionStatus.DENIED ->
            "갤러리를 불러오려면 사진 접근 권한이 필요합니다."
        PermissionStatus.GRANTED -> ""
    }

    val buttonLabel = when (state.permissionStatus) {
        PermissionStatus.PERMANENTLY_DENIED -> "앱 설정 열기"
        PermissionStatus.PARTIALLY_GRANTED -> "전체 권한 요청"
        PermissionStatus.DENIED -> "권한 요청"
        PermissionStatus.GRANTED -> ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )

        Button(
            onClick = {
                if (state.permissionStatus == PermissionStatus.PERMANENTLY_DENIED) {
                    onIntent(ImagePickerContract.Intent.OpenSettingsClick)
                } else {
                    onIntent(ImagePickerContract.Intent.RequestPermissionClick)
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(buttonLabel)
        }
    }
}