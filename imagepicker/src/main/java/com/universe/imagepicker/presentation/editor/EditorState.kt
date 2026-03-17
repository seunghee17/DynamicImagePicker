package com.universe.imagepicker.presentation.editor

import android.net.Uri
import com.universe.imagepicker.domain.model.CropRect

enum class EditorMode { NORMAL, CROPPING }

data class EditorState(
    val originalUri: Uri,
    val previewUri: Uri,                        // 편집 전은 originalUri와 동일
    val rotationDegrees: Int = 0,
    val cropRect: CropRect = CropRect.FULL,
    val mode: EditorMode = EditorMode.NORMAL,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val hasUnsavedChanges: Boolean = false
)
