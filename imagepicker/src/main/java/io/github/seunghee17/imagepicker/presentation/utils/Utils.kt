package io.github.seunghee17.imagepicker.presentation.utils

import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import io.github.seunghee17.imagepicker.domain.model.GalleryImage

internal data class DragSelectionState(
    val offset: Offset,
    val anchorIndex: Int,
    val lastProcessedIndex: Int?,
)

// 현재 터치한 좌표를 매개변수로 받는다
// 이 아이템 영역 안에 현재 터치좌표가 들어가 있는가? 만약 찾은 아이템이 있다면 그 아이템 정보(키/그리드 인덱스 포함) 반환, 없으면 null
internal fun LazyGridState.gridItemInfoAtPosition(hitPoint: Offset): LazyGridItemInfo? =
    // 현재 화면에 보이는 아이템 정보 리스트
    layoutInfo.visibleItemsInfo.find { itemInfo ->
        itemInfo.size.toIntRect()
            .contains(hitPoint.round() - itemInfo.offset) // global 좌표를 item local 좌표로 변환
    }

// anchor(드래그 시작 지점)와 current(현재 손가락 위치) 사이의 grid 선형 인덱스 범위를
// 방향과 무관하게 오름차순(좌→우, 위→아래)으로 계산한다. 아직 로드되지 않은 인덱스는 건너뛴다.
internal fun computeDragRange(
    anchorIndex: Int,
    currentIndex: Int,
    images: List<GalleryImage>,
): List<GalleryImage> {
    val from = minOf(anchorIndex, currentIndex)
    val to = maxOf(anchorIndex, currentIndex)
    return (from..to).mapNotNull { images.getOrNull(it) }
}

internal fun Modifier.photoGridDragHandler(
    lazyGridState: LazyGridState,
    haptics: HapticFeedback,
    imagesSnapshot: List<GalleryImage>, // grid 인덱스 순으로 정렬된, 현재 로드된 이미지 스냅샷
    onBeginDrag: (GalleryImage) -> Unit, // anchor로 선택된 이미지를 viewmodel에 알림 (선택/해제 모드 결정용)
    onUpdateRange: (List<GalleryImage>) -> Unit, // anchor~현재 위치 range를 viewmodel에 알림
    onEndDrag: () -> Unit, // 드래그 종료(취소 포함)를 viewmodel에 알림
    autoScrollSpeed: MutableState<Float>,
    autoScrollThreshold: Float,
    currentDragState: MutableState<DragSelectionState?> // 자동 스크롤 중 range 재계산을 위해 anchor/좌표/마지막 처리 인덱스를 함께 노출
): Modifier = composed {
    val currentImagesSnapshot by rememberUpdatedState(imagesSnapshot)
    val currentOnBeginDrag by rememberUpdatedState(onBeginDrag)
    val currentOnUpdateRange by rememberUpdatedState(onUpdateRange)
    val currentOnEndDrag by rememberUpdatedState(onEndDrag)

    pointerInput(Unit) {
        var anchorIndex: Int? = null
        var lastProcessedIndex: Int? = null

        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                lazyGridState.gridItemInfoAtPosition(offset)?.let { info ->
                    currentImagesSnapshot.getOrNull(info.index)?.let { image ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        anchorIndex = info.index
                        lastProcessedIndex = info.index
                        currentOnBeginDrag(image)
                        currentOnUpdateRange(listOf(image))
                        currentDragState.value = DragSelectionState(
                            offset = offset,
                            anchorIndex = info.index,
                            lastProcessedIndex = info.index,
                        )
                    }
                }
            },
            onDragCancel = {
                anchorIndex = null
                lastProcessedIndex = null
                autoScrollSpeed.value = 0f
                currentDragState.value = null
                currentOnEndDrag()
            },
            onDragEnd = {
                anchorIndex = null
                lastProcessedIndex = null
                autoScrollSpeed.value = 0f
                currentDragState.value = null
                currentOnEndDrag()
            },
            onDrag = { change, _ ->
                val anchor = anchorIndex
                if (anchor != null) {
                    val distFromBottom =
                        lazyGridState.layoutInfo.viewportSize.height - change.position.y
                    val distFromTop = change.position.y
                    //손가락이 아래쪽 경계선으로 들어오면 아래로 자동 스크롤
                    autoScrollSpeed.value = when {
                        distFromBottom < autoScrollThreshold -> autoScrollThreshold - distFromBottom
                        distFromTop < autoScrollThreshold -> -(autoScrollThreshold - distFromTop)
                        else -> 0f
                    }
                    val hitIndex = lazyGridState.gridItemInfoAtPosition(change.position)?.index
                    if (hitIndex != null && hitIndex != lastProcessedIndex) {
                        lastProcessedIndex = hitIndex
                        currentOnUpdateRange(computeDragRange(anchor, hitIndex, currentImagesSnapshot))
                    }
                    // autoscroll 폴링 루프가 이 좌표를 기준으로 계속 range를 재계산할 수 있도록 항상 최신 offset을 반영
                    currentDragState.value = DragSelectionState(
                        offset = change.position,
                        anchorIndex = anchor,
                        lastProcessedIndex = lastProcessedIndex,
                    )
                }
            }
        )
    }
}