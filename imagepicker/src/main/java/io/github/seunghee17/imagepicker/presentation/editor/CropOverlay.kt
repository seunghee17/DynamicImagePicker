package io.github.seunghee17.imagepicker.presentation.editor

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.seunghee17.imagepicker.CropRect
import kotlin.math.min

private val HANDLE_TOUCH_SIZE = 48.dp   // 터치 영역
private val HANDLE_VISUAL_SIZE = 30.dp  // 실제 보이는 핸들 크기
private val BORDER_WIDTH = 2.dp
private val OVERLAY_COLOR = Color.Black.copy(alpha = 0.5f)
private val CROP_COLOR = Color(0xFF7B2FBE)   // 보라색
private const val MIN_CROP_FRACTION = 0.05f  // 최소 크롭 영역 (이미지의 5%)

/**
 * 크롭 오버레이.
 * - 크롭 영역 바깥: 반투명 검정 마스크
 * - 크롭 영역 경계: 보라색 테두리
 * - 네 모서리: 드래그 가능한 핸들
 *
 * @param cropRect      정규화된 크롭 영역 [0f, 1f]
 * @param imageRect     컨테이너 내에서 이미지가 실제로 렌더링된 픽셀 영역
 * @param onCropRectChange cropRect가 바뀔 때 호출 (드래그 중 실시간)
 */
@Composable
internal fun BoxScope.CropOverlay(
    cropRect: CropRect,
    imageRect: Rect,
    onCropRectChange: (CropRect) -> Unit,
) {
    val density = LocalDensity.current
    // dp → px 변환: Canvas/Layout API는 px 단위를 사용하므로 미리 변환
    val handleTouchPx = with(density) { HANDLE_TOUCH_SIZE.toPx() }
    val handleVisualPx = with(density) { HANDLE_VISUAL_SIZE.toPx() }
    val borderPx = with(density) { BORDER_WIDTH.toPx() }

    // remember(cropRect, imageRect): 두 값 중 하나라도 바뀔 때만 재계산 (성능 최적화)
    val cropPx = remember(cropRect, imageRect) {
        Rect(
            left   = imageRect.left + cropRect.left   * imageRect.width,
            top    = imageRect.top  + cropRect.top    * imageRect.height,
            right  = imageRect.left + cropRect.right  * imageRect.width,
            bottom = imageRect.top  + cropRect.bottom * imageRect.height,
        )
    }

    // 마스크 + 테두리
    Box(
        modifier = Modifier
            .fillMaxSize()  // 컨테이너 전체를 덮어야 마스크가 제대로 그려진다
            .drawWithContent {

                // 마스크1 상단: imageRect 상단 ~ cropPx 상단
                drawRect(
                    color = OVERLAY_COLOR,
                    topLeft = Offset(imageRect.left, imageRect.top),
                    size = Size(imageRect.width, cropPx.top - imageRect.top)
                )
                // 마스크2 하단: cropPx 하단 ~ imageRect 하단
                drawRect(
                    color = OVERLAY_COLOR,
                    topLeft = Offset(imageRect.left, cropPx.bottom),
                    size = Size(imageRect.width, imageRect.bottom - cropPx.bottom)
                )
                // 마스크3 좌측: cropPx 세로 범위, imageRect 좌측 ~ cropPx 좌측
                drawRect(
                    color = OVERLAY_COLOR,
                    topLeft = Offset(imageRect.left, cropPx.top),
                    size = Size(cropPx.left - imageRect.left, cropPx.height)
                )
                // 마스크4 우측: cropPx 세로 범위, cropPx 우측 ~ imageRect 우측
                drawRect(
                    color = OVERLAY_COLOR,
                    topLeft = Offset(cropPx.right, cropPx.top),
                    size = Size(imageRect.right - cropPx.right, cropPx.height)
                )

                // 크롭 영역 보라색 테두리
                drawRect(
                    color = CROP_COLOR,
                    topLeft = Offset(cropPx.left, cropPx.top),
                    size = Size(cropPx.width, cropPx.height),
                    style = Stroke(width = borderPx)
                )

                // 네 모서리 핸들 시각적 표시 (보라색 원)
                listOf(
                    Offset(cropPx.left,  cropPx.top),    // 좌상
                    Offset(cropPx.right, cropPx.top),    // 우상
                    Offset(cropPx.left,  cropPx.bottom), // 좌하
                    Offset(cropPx.right, cropPx.bottom), // 우하
                ).forEach { center ->
                    drawCircle(
                        color = CROP_COLOR,
                        radius = handleVisualPx / 2f,
                        center = center
                    )
                }
            }
    )

    // TOP-LEFT: left, top 이동 / right, bottom 고정
    CropHandle(
        centerX = cropPx.left,
        centerY = cropPx.top,
        touchSizePx = handleTouchPx,
    ) { dx, dy ->
        onCropRectChange(
            buildCropRect(
                newLeft   = cropRect.left + dx / imageRect.width,
                newTop    = cropRect.top  + dy / imageRect.height,
                newRight  = cropRect.right,   // 고정
                newBottom = cropRect.bottom,  // 고정
            )
        )
    }

    // TOP-RIGHT: right, top 이동 / left, bottom 고정
    CropHandle(
        centerX = cropPx.right,
        centerY = cropPx.top,
        touchSizePx = handleTouchPx,
    ) { dx, dy ->
        onCropRectChange(
            buildCropRect(
                newLeft   = cropRect.left,    // 고정
                newTop    = cropRect.top  + dy / imageRect.height,
                newRight  = cropRect.right + dx / imageRect.width,
                newBottom = cropRect.bottom,  // 고정
            )
        )
    }

    // BOTTOM-LEFT: left, bottom 이동 / right, top 고정
    CropHandle(
        centerX = cropPx.left,
        centerY = cropPx.bottom,
        touchSizePx = handleTouchPx,
    ) { dx, dy ->
        onCropRectChange(
            buildCropRect(
                newLeft   = cropRect.left + dx / imageRect.width,
                newTop    = cropRect.top,     // 고정
                newRight  = cropRect.right,   // 고정
                newBottom = cropRect.bottom + dy / imageRect.height,
            )
        )
    }

    // BOTTOM-RIGHT: right, bottom 이동 / left, top 고정
    CropHandle(
        centerX = cropPx.right,
        centerY = cropPx.bottom,
        touchSizePx = handleTouchPx,
    ) { dx, dy ->
        onCropRectChange(
            buildCropRect(
                newLeft   = cropRect.left,    // 고정
                newTop    = cropRect.top,     // 고정
                newRight  = cropRect.right + dx / imageRect.width,
                newBottom = cropRect.bottom + dy / imageRect.height,
            )
        )
    }
}

/**
 * 드래그 핸들의 터치 영역 컴포저블.
 *
 */
@Composable
private fun BoxScope.CropHandle(
    centerX: Float,
    centerY: Float,
    touchSizePx: Float,
    onDrag: (dx: Float, dy: Float) -> Unit,
) {
    val density = LocalDensity.current
    val halfPx = touchSizePx / 2f

    // px → dp 변환
    val touchSizeDp = with(density) { touchSizePx.toDp() }
    // 터치 영역의 좌상단 = 중심 - 절반 크기
    val offsetX = with(density) { (centerX - halfPx).toDp() }
    val offsetY = with(density) { (centerY - halfPx).toDp() }

    val latestOnDrag by rememberUpdatedState(onDrag)

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = offsetX, y = offsetY)
            .size(touchSizeDp)
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    latestOnDrag(dragAmount.x, dragAmount.y)
                }
            }
    )
}

/**
 * 드래그 델타가 적용된 새 CropRect를 생성하되, 항상 유효한 범위를 보장한다.
 *
 * [두 단계 유효성 보정]
 *
 * 1단계 - 범위 클램프:
 *   left/top은 최대 (1f - MIN_CROP_FRACTION)까지 → right/bottom이 들어올 최소 공간 확보
 *   right/bottom은 최소 MIN_CROP_FRACTION부터  → left/top보다 항상 크도록
 *
 * 2단계 - left < right, top < bottom 보장:
 *   만약 1단계 후에도 l >= r 상황이 생기면 (사용자가 핸들을 반대편으로 넘긴 경우):
 *     left  = min(l, r - MIN_CROP_FRACTION)  → left가 right를 넘지 못하게
 *     right = max(r, l + MIN_CROP_FRACTION)  → right가 left보다 항상 크도록
 *
 * 결과적으로 크롭 영역의 너비/높이는 항상 MIN_CROP_FRACTION(5%) 이상이 유지된다.
 */
private fun buildCropRect(
    newLeft: Float,
    newTop: Float,
    newRight: Float,
    newBottom: Float,
): CropRect {
    // 1단계: [0f, 1f] 범위 클램프
    val l = newLeft.coerceIn(0f, 1f - MIN_CROP_FRACTION)
    val t = newTop.coerceIn(0f, 1f - MIN_CROP_FRACTION)
    val r = newRight.coerceIn(MIN_CROP_FRACTION, 1f)
    val b = newBottom.coerceIn(MIN_CROP_FRACTION, 1f)

    // 2단계: left < right, top < bottom 보장 (핸들이 교차하는 경우 방어)
    return CropRect(
        left   = minOf(l, r - MIN_CROP_FRACTION),
        top    = minOf(t, b - MIN_CROP_FRACTION),
        right  = maxOf(r, l + MIN_CROP_FRACTION),
        bottom = maxOf(b, t + MIN_CROP_FRACTION),
    )
}

/**
 * ContentScale.Fit 기준으로 이미지가 컨테이너 내에 실제로 렌더링되는 픽셀 영역을 계산한다.
 * [scale 계산]
 * scaleX = 컨테이너 너비 / 이미지 원본 너비
 * scaleY = 컨테이너 높이 / 이미지 원본 높이
 * scale  = min(scaleX, scaleY)  ← 더 작은 값을 쓰면 이미지가 컨테이너를 벗어나지 않는다
 *
 * [중앙 정렬 오프셋]
 * Fit으로 그려진 이미지는 컨테이너 중앙에 위치한다.
 * offsetX = (컨테이너 너비 - 렌더 너비) / 2
 * offsetY = (컨테이너 높이 - 렌더 높이) / 2
 *
 * [반환값]
 * 컨테이너 좌표계 기준의 이미지 렌더 영역 Rect (픽셀 절대좌표)
 */
internal fun calculateImageRect(containerSize: IntSize, imageSize: IntSize): Rect {
    if (imageSize.width <= 0 || imageSize.height <= 0) {
        // 이미지 크기 정보가 없으면 컨테이너 전체를 이미지 영역으로 간주 (방어 처리)
        return Rect(0f, 0f, containerSize.width.toFloat(), containerSize.height.toFloat())
    }
    // ContentScale.Fit과 동일한 스케일 계산
    val scale = min(
        containerSize.width.toFloat()  / imageSize.width,
        containerSize.height.toFloat() / imageSize.height,
    )
    val displayW = imageSize.width  * scale
    val displayH = imageSize.height * scale
    // 이미지가 컨테이너 중앙에 정렬되므로 남은 공간의 절반이 오프셋이 된다
    val offsetX = (containerSize.width  - displayW) / 2f
    val offsetY = (containerSize.height - displayH) / 2f
    return Rect(offsetX, offsetY, offsetX + displayW, offsetY + displayH)
}
