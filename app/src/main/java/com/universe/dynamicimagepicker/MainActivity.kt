package com.universe.dynamicimagepicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.seunghee17.imagepicker.DynamicImagePicker
import io.github.seunghee17.imagepicker.ImagePickerConfig
import io.github.seunghee17.imagepicker.PickedImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PickerHost()
                }
            }
        }
    }
}

@Composable
private fun PickerHost(modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }
    var selectedImages by remember { mutableStateOf(emptyList<PickedImage>()) }

    if (showPicker) {
        DynamicImagePicker(
            config = ImagePickerConfig(
                allowVideo = true
            ),
            onResult = { result ->
                selectedImages = result.items
                showPicker = false
            },
            onCancel = {
                showPicker = false
            },
            modifier = modifier
        )
    } else {
        PickerLauncherScreen(
            selectedImages = selectedImages,
            onOpenPicker = { showPicker = true },
            modifier = modifier
        )
    }
}

@Composable
private fun PickerLauncherScreen(
    selectedImages: List<PickedImage>,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (selectedImages.isNotEmpty()) {
                "선택된 이미지: ${selectedImages.size}장"
            } else {
                "아직 선택된 이미지가 없습니다."
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Button(
            onClick = onOpenPicker
        ) {
            Text("이미지 선택 열기")
        }
        if (selectedImages.isNotEmpty()) {
            SelectedImageGrid(
                images = selectedImages,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            EmptySelectionCard(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SelectedImageGrid(
    images: List<PickedImage>,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = true
    ) {
        items(images, key = { image ->
            val displayUri = image.editedUri ?: image.originalUri
            "${image.originalUri}-$displayUri-${image.rotationDegrees}-${image.isCropped}"
        }) { image ->
            SelectedImageTile(image = image)
        }
    }
}

@Composable
private fun SelectedImageTile(
    image: PickedImage,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = image.editedUri ?: image.originalUri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (image.isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
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
                    text = formatDuration(image.videoDurationMs),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSecs = (durationMs / 1000).coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%d:%02d".format(mins, secs)
}

@Composable
private fun EmptySelectionCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "선택을 완료하면 여기에서 결과 이미지를 바로 확인할 수 있습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
