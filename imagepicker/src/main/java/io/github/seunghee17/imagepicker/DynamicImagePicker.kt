package io.github.seunghee17.imagepicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.VideoFrameDecoder
import io.github.seunghee17.imagepicker.presentation.picker.ImagePickerScreen

/**
 * Public entry point of the DynamicImagePicker library.
 *
 * Provides a video-aware Coil [ImageLoader] scoped to this composable tree so
 * that video thumbnails are decoded without affecting the host app's image loader.
 *
 * @param config    Picker behaviour config (max selection, album selector, editing, video)
 * @param onResult  Called with [PickerResult] when the user confirms selection
 * @param onCancel  Called when the user cancels
 * @param onError   Called with an error message on unrecoverable edit errors (default: no-op)
 * @param modifier  Layout modifier
 */
@Composable
fun DynamicImagePicker(
    config: ImagePickerConfig = ImagePickerConfig(),
    onResult: (PickerResult) -> Unit,
    onCancel: () -> Unit,
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Build a scoped ImageLoader that adds VideoFrameDecoder for video thumbnail support.
    // Memory and disk caching use Coil defaults (25% of available memory + disk cache),
    // keeping memory usage efficient — frames are only decoded on demand and cached lazily.
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }
    CompositionLocalProvider(LocalImageLoader provides imageLoader) {
        ImagePickerScreen(
            config = config,
            onResult = onResult,
            onCancel = onCancel,
            onError = onError,
            modifier = modifier
        )
    }
}
