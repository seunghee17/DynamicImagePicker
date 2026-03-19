package com.universe.imagepicker.presentation.utils

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
import com.universe.imagepicker.domain.model.GalleryImage

// 현재 터치한 좌표를 매개변수로 받는다
// 이 아이템 영역 안에 현재 터치좌표가 들어가 있는가? 만약 찾은 아이템이 있다면 key 반환 없으면 null
fun LazyGridState.gridItemKeyAtPosition(hitPoint: Offset): Long? =
    // 현재 화면에 보이는 아이템 정보 리스트
    layoutInfo.visibleItemsInfo.find { itemInfo ->
        itemInfo.size.toIntRect()
            .contains(hitPoint.round() - itemInfo.offset) // global 좌표를 item local 좌표로 변환
    }?.key as? Long

fun Modifier.photoGridDragHandler(
    lazyGridState: LazyGridState,
    haptics: HapticFeedback,
    selectedImages: List<GalleryImage>,
    onSelect: (Long) -> Unit, // viewmodel에 정의한 사진 선택 콜백 주입하도록 수정
    autoScrollSpeed: MutableState<Float>,
    autoScrollThreshold: Float
): Modifier = composed {
    val currentSelectedImages by rememberUpdatedState(selectedImages)
    val currentOnSelect by rememberUpdatedState(onSelect)

    pointerInput(Unit) {
        var initialKey: Long? = null
        var currentKey: Long? = null

        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                lazyGridState.gridItemKeyAtPosition(offset)?.let { key ->
                    if (currentSelectedImages.none { it.id == key }) {
                        // 새롭게 선택 상태 업데이트가 필요한 아이템
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        initialKey = key
                        currentKey = key
                        currentOnSelect(key)
                    }
                }
            },
            onDragCancel = {
                initialKey = null
                autoScrollSpeed.value = 0f
            },
            onDragEnd = {
                initialKey = null
                autoScrollSpeed.value = 0f
            },
            onDrag = { change, _ ->
                if (initialKey != null) {
                    val distFromBottom =
                        lazyGridState.layoutInfo.viewportSize.height - change.position.y
                    val distFromTop = change.position.y
                    //손가락이 아래쪽 경계선으로 들어오면 아래로 자동 스크롤
                    autoScrollSpeed.value = when {
                        distFromBottom < autoScrollThreshold -> autoScrollThreshold - distFromBottom
                        distFromTop < autoScrollThreshold -> -(autoScrollThreshold - distFromTop)
                        else -> 0f
                    }
                    lazyGridState.gridItemKeyAtPosition(change.position)?.let { key ->
                        if (currentKey != key && currentSelectedImages.none { it.id == key }) {
                            currentOnSelect(key)
                            currentKey = key
                        }
                    }
                }
            }
        )
    }
}