package com.universe.imagepicker.presentation.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universe.imagepicker.CropRect
import com.universe.imagepicker.PickedImage
import com.universe.imagepicker.domain.usecase.CropImageUseCase
import com.universe.imagepicker.domain.usecase.RotateImageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [Committed State 패턴]
 *
 * 크롭과 회전의 순서가 섞일 때 좌표 공간 불일치 문제를 방지하기 위해
 * 크롭 확정 시마다 결과를 새 기준점(committedUri)으로 삼는다.
 *
 * - 회전: EXIF 방식으로 committedUri 위에 pendingRotation 누적 → 비트맵 없음
 * - 크롭: previewUri(committedUri + pendingRotation 반영)에 적용 → 결과가 새 committedUri
 *        pendingRotation 리셋 (회전이 크롭 결과에 이미 반영됐으므로)
 *
 * 크롭 후 회전:
 *   crop(originalUri, rect) → committedUri = 크롭결과
 *   rotate → rotate(committedUri, 90) → 크롭된 이미지를 회전 ✓
 *
 * 회전 후 크롭:
 *   rotate → pendingRotation=90, previewUri = rotate(originalUri, 90)
 *   crop(previewUri, rect) → committedUri = 크롭결과 ✓
 */
internal class EditorViewModel(
    originalUri: Uri,
    private val rotateImage: RotateImageUseCase,
    private val cropImage: CropImageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        EditorContract.State(
            originalUri = originalUri,
            committedUri = originalUri,
            previewUri = originalUri,
        )
    )
    val state: StateFlow<EditorContract.State> = _state.asStateFlow()

    private val _effect = Channel<EditorContract.Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    // 진행 중인 편집 작업. 새 요청 시 이전 작업을 취소하고 최신 상태로 재시작한다.
    private var editJob: Job? = null

    fun handleIntent(intent: EditorContract.Intent) {
        when (intent) {
            EditorContract.Intent.RotateClockwise -> rotate()
            EditorContract.Intent.EnterCropMode ->
                _state.update { it.copy(
                    mode = EditorContract.Mode.CROPPING,
                    cropRectOnEnter = it.cropRect,
                ) }
            is EditorContract.Intent.UpdateCropRect ->
                _state.update { it.copy(cropRect = intent.rect) }
            EditorContract.Intent.ApplyCrop -> applyCrop()
            EditorContract.Intent.ExitCropMode ->
                _state.update { it.copy(
                    mode = EditorContract.Mode.NORMAL,
                    cropRect = it.cropRectOnEnter,
                ) }
            EditorContract.Intent.SaveAndReturn -> save()
            EditorContract.Intent.Cancel -> sendEffect(EditorContract.Effect.Cancelled)
        }
    }

    /**
     * pendingRotation을 90도 증가시킨 뒤, committedUri를 회전하여 previewUri를 갱신한다.
     * EXIF 방식이므로 비트맵 디코딩 없음.
     */
    private fun rotate() {
        val newPendingRotation = (_state.value.pendingRotation + 90) % 360
        val newTotalRotation = (_state.value.totalRotation + 90) % 360
        _state.update { it.copy(
            pendingRotation = newPendingRotation,
            totalRotation = newTotalRotation,
        ) }
        refreshPreview()
    }

    /**
     * 현재 previewUri(committedUri + pendingRotation 반영)에 cropRect를 적용한다.
     * 크롭 결과가 새 committedUri가 되고, pendingRotation은 0으로 리셋된다.
     *
     */
    private fun applyCrop() {
        _state.update { it.copy(mode = EditorContract.Mode.NORMAL) }
        editJob?.cancel()
        editJob = viewModelScope.launch {
            val current = _state.value
            _state.update { it.copy(isSaving = true) }
            runCatching {
                cropImage(current.previewUri, current.cropRect)
            }.onSuccess { uri ->
                _state.update { it.copy(
                    committedUri = uri,     // 크롭 결과가 새 기준점
                    pendingRotation = 0,    // 회전이 크롭에 반영됐으므로 리셋
                    previewUri = uri,
                    cropRect = CropRect.FULL,  // 다음 크롭 모드 진입 시 전체 영역부터 시작
                    isSaving = false,
                    hasUnsavedChanges = true,
                    hasCropApplied = true,
                ) }
            }.onFailure { e ->
                _state.update { it.copy(isSaving = false) }
                sendEffect(EditorContract.Effect.ShowError(e.message ?: "크롭 처리에 실패했습니다."))
            }
        }
    }

    /**
     * committedUri에 pendingRotation을 EXIF로 적용하여 previewUri를 갱신한다.
     * pendingRotation == 0이면 committedUri를 그대로 사용한다.
     *
     * [Job 취소 전략]
     * 회전 버튼 연타 시 이전 작업을 취소하고 최신 상태로 재시작하여
     * 불필요한 중간 연산을 생략한다.
     */
    private fun refreshPreview() {
        editJob?.cancel()
        editJob = viewModelScope.launch {
            val current = _state.value
            _state.update { it.copy(isSaving = true) }
            runCatching {
                if (current.pendingRotation % 360 != 0) {
                    rotateImage(current.committedUri, current.pendingRotation)
                } else {
                    current.committedUri
                }
            }.onSuccess { uri ->
                _state.update { it.copy(
                    previewUri = uri,
                    isSaving = false,
                    hasUnsavedChanges = true,
                ) }
            }.onFailure { e ->
                _state.update { it.copy(isSaving = false) }
                sendEffect(EditorContract.Effect.ShowError(e.message ?: "회전 처리에 실패했습니다."))
            }
        }
    }

    private fun save() {
        val current = _state.value
        _state.update { it.copy(isSaving = true) }
        val pickedImage = PickedImage(
            originalUri = current.originalUri,
            editedUri = if (current.hasUnsavedChanges) current.previewUri else null,
            rotationDegrees = current.totalRotation,
            // 크롭과 회전이 조합된 경우 단일 CropRect로 표현이 불가하므로 null.
            // 편집 결과는 editedUri에 픽셀로 반영되어 있다.
            cropRect = null,
            isCropped = current.hasCropApplied,
        )
        _state.update { it.copy(isSaving = false) }
        sendEffect(EditorContract.Effect.ReturnEditedImage(pickedImage))
    }

    private fun sendEffect(effect: EditorContract.Effect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    override fun onCleared() {
        super.onCleared()
        _effect.close()
    }
}
