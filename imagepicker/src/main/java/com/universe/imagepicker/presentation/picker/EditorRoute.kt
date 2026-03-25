package com.universe.imagepicker.presentation.picker

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universe.imagepicker.domain.model.PickedImage
import com.universe.imagepicker.presentation.editor.EditorContract
import com.universe.imagepicker.presentation.editor.EditorScreen
import com.universe.imagepicker.presentation.editor.EditorViewModel
import com.universe.imagepicker.presentation.editor.EditorViewModelFactory
import kotlinx.coroutines.flow.collectLatest

internal data class EditorDestination(
    val entryId: Long,
    val imageId: Long,
    val originalUri: Uri
)

internal fun editorDestinationSaver(): Saver<EditorDestination?, Any> = Saver(
    save = { destination ->
        destination?.let {
            listOf(
                it.entryId.toString(),
                it.imageId.toString(),
                it.originalUri.toString()
            )
        }
    },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        val values = saved as? List<String> ?: return@Saver null
        EditorDestination(
            entryId = values[0].toLong(),
            imageId = values[1].toLong(),
            originalUri = Uri.parse(values[2])
        )
    }
)

@Composable
internal fun EditorRoute(
    destination: EditorDestination,
    onEditApplied: (PickedImage) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    allowEditing: Boolean = true,
) {
    val context = LocalContext.current
    val viewModel: EditorViewModel = viewModel(
        key = "editor-${destination.entryId}",
        factory = EditorViewModelFactory(
            originalUri = destination.originalUri,
            context = context
        )
    )
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is EditorContract.Effect.ReturnEditedImage -> onEditApplied(effect.pickedImage)
                EditorContract.Effect.Cancelled -> onDismiss()
                is EditorContract.Effect.ShowError ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler { onDismiss() }

    EditorScreen(
        state = state,
        onIntent = viewModel::handleIntent,
        modifier = modifier,
        allowEditing = allowEditing,
    )
}
