package com.universe.imagepicker.presentation.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.universe.imagepicker.domain.model.GalleryImage
import com.universe.imagepicker.presentation.component.SelectionBadge

/**
 * 그리드 내 개별 이미지 아이템.
 * - 탭: 선택/해제 토글
 * - 드래그: 드래그 멀티 선택 시작
 * - 선택 시: 배지 번호 및 오버레이 표시
 */
@Composable
internal fun GalleryGridItem(
    image: GalleryImage,
    selectionOrder: Int?,       // null = 미선택, 1 이상 = 선택 순서
    onOpenEditor: () -> Unit,
    onSelectionBadgeTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = selectionOrder != null

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                else Modifier
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onOpenEditor() },
                )
            }
    ) {
        AsyncImage(
            model = image.uri,
            contentDescription = image.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }
        SelectionBadge(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
            order = selectionOrder,
            onTap = onSelectionBadgeTap,
        )
    }
}
