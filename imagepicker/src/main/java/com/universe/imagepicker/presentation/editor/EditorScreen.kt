package com.universe.imagepicker.presentation.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

/**
 * 이미지 편집 화면 (회전 / 크롭).
 *
 * 크롭 모드:
 *   - TopAppBar: 취소(ExitCropMode) / 완료(ApplyCrop)
 *   - 이미지 위에 CropOverlay 표시 (보라색 경계선 + 드래그 핸들)
 *   - 하단 버튼 숨김
 * 일반 모드:
 *   - TopAppBar: 취소(Cancel) / 완료(SaveAndReturn)
 *   - 하단 버튼: 회전, 크롭
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: EditorContract.State,
    onIntent: (EditorContract.Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCropping = state.mode == EditorContract.Mode.CROPPING

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (isCropping) "크롭 영역 선택" else "편집") },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            onIntent(
                                if (isCropping) EditorContract.Intent.ExitCropMode
                                else EditorContract.Intent.Cancel
                            )
                        }
                    ) { Text("취소") }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onIntent(
                                if (isCropping) EditorContract.Intent.ApplyCrop
                                else EditorContract.Intent.SaveAndReturn
                            )
                        },
                        enabled = !state.isSaving
                    ) { Text("완료") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 이미지 + 오버레이
            ImageWithCropOverlay(
                state = state,
                onIntent = onIntent,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            // 하단 도구 버튼 (크롭 모드에서는 숨김)
            if (!isCropping) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = { onIntent(EditorContract.Intent.RotateClockwise) },
                        enabled = !state.isSaving
                    ) { Text("회전") }
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedButton(
                        onClick = { onIntent(EditorContract.Intent.EnterCropMode) },
                        enabled = !state.isSaving
                    ) { Text("크롭") }
                }
            }
        }
    }
}

/**
 * 이미지를 렌더링하고, 크롭 모드일 때 CropOverlay를 덮어씌운다.
 */
@Composable
private fun ImageWithCropOverlay(
    state: EditorContract.State,
    onIntent: (EditorContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val painter = rememberAsyncImagePainter(
        model = state.previewUri,
        contentScale = ContentScale.Fit,
    )

    // 컨테이너 크기와 이미지 intrinsic 크기를 추적하여 imageRect 계산
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    //
    val intrinsicSize: IntSize? = remember(painter.state) {
        (painter.state as? AsyncImagePainter.State.Success)
            ?.painter
            ?.intrinsicSize
            ?.takeIf { it.width > 0f && it.height > 0f }
            ?.let { IntSize(it.width.toInt(), it.height.toInt()) }
    }
    val imageRect: Rect? = remember(containerSize, intrinsicSize) {
        if (containerSize == IntSize.Zero || intrinsicSize == null) null
        else calculateImageRect(containerSize, intrinsicSize)
    }

    val isCropping = state.mode == EditorContract.Mode.CROPPING

    Box(
        modifier = modifier
            .padding(horizontal = if (isCropping) 20.dp else 0.dp)
            .onSizeChanged { containerSize = it },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painter,
            contentDescription = "미리보기",
            contentScale = ContentScale.Fit,
        )

        if (state.isSaving) {
            CircularProgressIndicator()
        }

        // 이미지 로드 완료 + 크롭 모드일 때만 오버레이 표시
        if (isCropping && imageRect != null) {
            CropOverlay(
                cropRect = state.cropRect,
                imageRect = imageRect,
                onCropRectChange = { onIntent(EditorContract.Intent.UpdateCropRect(it)) },
            )
        }
    }
}
