package com.universe.imagepicker.presentation.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.universe.imagepicker.domain.model.PermissionStatus

/**
 * 권한 요청 / 거부 / 영구 거부 상태에 따른 화면.
 */
@Composable
fun PermissionScreen(
    permissionStatus: PermissionStatus,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (permissionStatus) {
            PermissionStatus.PERMANENTLY_DENIED -> {
                Text(
                    text = "사진 접근 권한이 필요합니다.\n설정에서 권한을 허용해 주세요.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onOpenSettings) {
                    Text("설정으로 이동")
                }
            }
            else -> {
                Text(
                    text = "갤러리 이미지를 불러오려면\n사진 접근 권한이 필요합니다.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onRequestPermission) {
                    Text("권한 요청")
                }
            }
        }
    }
}
