package io.github.seunghee17.imagepicker.presentation.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.seunghee17.imagepicker.domain.model.GalleryAlbum

/**
 * 앨범 선택 드롭다운.
 */
@Composable
internal fun AlbumDropdown(
    modifier: Modifier = Modifier,
    albums: List<GalleryAlbum>,
    dropDownExpanded: Boolean,
    openDropDown: () -> Unit,
    closeDropDown: () -> Unit,
    selectedAlbum: GalleryAlbum?,
    onAlbumSelected: (GalleryAlbum) -> Unit,
) {
    Box(modifier = modifier) {
        Button(
            onClick = openDropDown,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.Transparent,
            ),
        ) {
            Row {
                Text(
                    text = selectedAlbum?.name ?: "All",
                    color = Color.Black
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Dropdown",
                    tint = Color.Black
                )
            }
        }
        DropdownMenu(
            expanded = dropDownExpanded,
            onDismissRequest = closeDropDown
        ) {
            albums.forEach { album ->
                DropdownMenuItem(
                    text = { Text("${album.name} (${album.count})") },
                    onClick = {
                        onAlbumSelected(album)
                        closeDropDown()
                    }
                )
            }
        }
    }
}
