package io.github.seunghee17.imagepicker.presentation.picker

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import io.github.seunghee17.imagepicker.PickedImage
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import io.github.seunghee17.imagepicker.presentation.editor.EditorScreen

internal data class EditorDestination(
    val entryId: Long,
    val imageId: Long,
    val originalUri: Uri,
    val initialIndex: Int = 0,
    val tappedImage: GalleryImage? = null,
)

internal fun editorDestinationSaver(): Saver<EditorDestination?, Any> = Saver(
    save = { destination ->
        destination?.let {
            listOf(
                it.entryId.toString(),
                it.imageId.toString(),
                it.originalUri.toString(),
                it.initialIndex.toString(),
                it.tappedImage?.uri?.toString() ?: "",
            )
        }
    },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        val values = saved as? List<String> ?: return@Saver null
        val tappedUri = values.getOrNull(4)?.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
        val tappedId = values.getOrNull(1)?.toLongOrNull() ?: 0L
        val tappedImage = tappedUri?.let {
            GalleryImage(id = tappedId, uri = it, displayName = "", dateTaken = 0L,
                albumId = "", albumName = "", width = 0, height = 0, mimeType = "")
        }
        EditorDestination(
            entryId = values[0].toLong(),
            imageId = values[1].toLong(),
            originalUri = Uri.parse(values[2]),
            initialIndex = values.getOrNull(3)?.toIntOrNull() ?: 0,
            tappedImage = tappedImage,
        )
    }
)

@Composable
internal fun EditorRoute(
    destination: EditorDestination,
    allImages: List<GalleryImage>,
    selectedImages: List<GalleryImage>,
    snackbarHostState: SnackbarHostState,
    onEditApplied: (PickedImage) -> Unit,
    onDismiss: () -> Unit,
    onToggleSelection: (GalleryImage) -> Unit,
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    allowEditing: Boolean = true,
) {
    BackHandler { onDismiss() }

    val initialPage = destination.initialIndex
        .coerceIn(0, (allImages.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { allImages.size },
    )

    EditorScreen(
        pagerState = pagerState,
        allImages = allImages,
        selectedImages = selectedImages,
        entryId = destination.entryId,
        snackbarHostState = snackbarHostState,
        onEditApplied = onEditApplied,
        onDismiss = onDismiss,
        onToggleSelection = onToggleSelection,
        onError = onError,
        modifier = modifier,
        allowEditing = allowEditing,
    )
}
