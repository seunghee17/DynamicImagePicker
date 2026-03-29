package io.github.seunghee17.imagepicker.presentation.editor

import android.widget.Toast
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import io.github.seunghee17.imagepicker.PickedImage
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import io.github.seunghee17.imagepicker.presentation.component.SelectionBadge
import kotlinx.coroutines.flow.collectLatest

/**
 * 이미지 편집 화면.
 *
 * - 상단 바 / 하단 버튼 / SelectionBadge 는 스와이프 영역 밖에 고정된다.
 * - HorizontalPager 는 이미지 영역(weight=1)만 감싸므로, 스와이프 시 사진만 전환된다.
 * - 각 페이지는 독립된 EditorViewModel 을 가지며, 현재 페이지의 state/intent 가
 *   SideEffect 를 통해 상단 바 / 버튼에 노출된다.
 *
 * 크롭 모드:
 *   - 상단 바: 취소(ExitCropMode) / 완료(ApplyCrop)
 *   - 스와이프 비활성화
 *   - SelectionBadge 숨김
 * 일반 모드:
 *   - 상단 바: 취소(→갤러리 복귀) / 완료(SaveAndReturn)
 *   - 하단 버튼: 회전, 크롭
 *   - 우측 상단: SelectionBadge (선택 순서 표시, 탭으로 토글)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorScreen(
    pagerState: PagerState,
    allImages: List<GalleryImage>,
    selectedImages: List<GalleryImage>,
    entryId: Long,
    snackbarHostState: SnackbarHostState,
    onEditApplied: (PickedImage) -> Unit,
    onDismiss: () -> Unit,
    onToggleSelection: (GalleryImage) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowEditing: Boolean = true,
) {
    // 화면이 컴포지션을 떠날 때(갤러리 복귀 등) 잔여 스낵바 제거
    DisposableEffect(Unit) {
        onDispose { snackbarHostState.currentSnackbarData?.dismiss() }
    }

    // 현재 페이지의 ViewModel state / intent 를 페이저 밖에서 참조하기 위한 홀더
    val activeState = remember { mutableStateOf<EditorContract.State?>(null) }
    val activeOnIntent = remember { mutableStateOf<((EditorContract.Intent) -> Unit)?>(null) }

    val currentState = activeState.value
    val isCropping = currentState?.mode == EditorContract.Mode.CROPPING

    val currentImage = allImages.getOrNull(pagerState.settledPage)
    val selectionOrder = currentImage?.let { img ->
        selectedImages.indexOfFirst { it.id == img.id }.takeIf { it >= 0 }?.let { it + 1 }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isCropping) "크롭 영역 선택" else "편집") },
                navigationIcon = {
                    TextButton(onClick = {
                        if (isCropping) activeOnIntent.value?.invoke(EditorContract.Intent.ExitCropMode)
                        else onDismiss()
                    }) { Text("취소") }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (isCropping) activeOnIntent.value?.invoke(EditorContract.Intent.ApplyCrop)
                            else activeOnIntent.value?.invoke(EditorContract.Intent.SaveAndReturn)
                        },
                        enabled = currentState?.isSaving != true,
                    ) { Text("완료") }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 이미지 영역만 페이저 — 상단 바/하단 버튼은 고정
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    key = { allImages[it].id },
                    userScrollEnabled = !isCropping, // 크롭 중 스와이프 비활성화
                ) { pageIndex ->
                    EditorImagePage(
                        image = allImages[pageIndex],
                        entryId = entryId,
                        isCurrentPage = pageIndex == pagerState.settledPage,
                        onActivate = { state, onIntent ->
                            activeState.value = state
                            activeOnIntent.value = onIntent
                        },
                        onEditApplied = onEditApplied,
                        onError = onError,
                    )
                }

                // 하단 편집 버튼 (크롭 모드·편집 비허용 시 숨김)
                if (!isCropping && allowEditing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        OutlinedButton(
                            onClick = { activeOnIntent.value?.invoke(EditorContract.Intent.RotateClockwise) },
                            enabled = currentState?.isSaving != true,
                        ) { Text("회전") }
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedButton(
                            onClick = { activeOnIntent.value?.invoke(EditorContract.Intent.EnterCropMode) },
                            enabled = currentState?.isSaving != true,
                        ) { Text("크롭") }
                    }
                }
            }

            // 우측 상단 SelectionBadge — 크롭 모드 진입 시 숨김
            if (!isCropping) {
                SelectionBadge(
                    order = selectionOrder,
                    onTap = { currentImage?.let { onToggleSelection(it) } },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )
            }
        }
    }
}

/**
 * 페이저 내 단일 페이지.
 * - 독립된 EditorViewModel 을 생성·보유한다.
 * - 현재 페이지일 때 SideEffect 로 상위에 state / intent 핸들러를 노출한다.
 * - 이미지 + CropOverlay 만 렌더링한다.
 */
@Composable
private fun EditorImagePage(
    image: GalleryImage,
    entryId: Long,
    isCurrentPage: Boolean,
    onActivate: (EditorContract.State, (EditorContract.Intent) -> Unit) -> Unit,
    onEditApplied: (PickedImage) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: EditorViewModel = viewModel(
        key = "editor-$entryId-${image.id}",
        factory = EditorViewModelFactory(originalUri = image.uri, context = context),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val latestOnActivate by rememberUpdatedState(onActivate)
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) return@LaunchedEffect
        snapshotFlow { state }.collect { latestState ->
            latestOnActivate(latestState, viewModel::handleIntent)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is EditorContract.Effect.ReturnEditedImage -> onEditApplied(effect.pickedImage)
                is EditorContract.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    onError(effect.message)
                }
            }
        }
    }

    ImageWithCropOverlay(
        state = state,
        onIntent = viewModel::handleIntent,
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * 이미지를 렌더링하고, 크롭 모드일 때 CropOverlay 를 덮어씌운다.
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

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

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

        if (isCropping && imageRect != null) {
            CropOverlay(
                cropRect = state.cropRect,
                imageRect = imageRect,
                onCropRectChange = { onIntent(EditorContract.Intent.UpdateCropRect(it)) },
            )
        }
    }
}
