package io.github.seunghee17.imagepicker.presentation.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import io.github.seunghee17.imagepicker.domain.model.MediaType
import io.github.seunghee17.imagepicker.presentation.component.SelectionBadge

/**
 * Single grid item rendering an image or video thumbnail.
 * - Image tap: opens editor
 * - Video tap: toggles selection
 * - Badge tap: toggles selection
 * - Video items show a play icon + duration overlay at the bottom-start
 */
@Composable
internal fun GalleryGridItem(
    image: GalleryImage,
    selectionOrder: Int?,       // null = unselected, 1+ = selection order
    onOpenEditor: () -> Unit,
    onSelectionBadgeTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = selectionOrder != null
    val currentOnOpenEditor by rememberUpdatedState(onOpenEditor)
    val currentOnSelectionBadgeTap by rememberUpdatedState(onSelectionBadgeTap)

    val handleTap: () -> Unit = {
        if (image.mediaType == MediaType.VIDEO) {
            currentOnSelectionBadgeTap()
        } else {
            currentOnOpenEditor()
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                else Modifier
            )
            .pointerInput(image.mediaType) {
                detectTapGestures(onTap = { handleTap() })
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

        // Video indicator: gradient scrim + play icon + duration
        if (image.mediaType == MediaType.VIDEO) {
            // Bottom gradient scrim for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 5.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatDuration(image.videoDuration),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        SelectionBadge(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
            order = selectionOrder,
            onTap = { handleTap() },
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSecs = (durationMs / 1000).coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%d:%02d".format(mins, secs)
}
