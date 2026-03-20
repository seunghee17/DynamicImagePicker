package com.universe.imagepicker.presentation.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.universe.imagepicker.domain.model.GalleryAlbum

/**
 * 앨범 선택 드롭다운.
 */
@Composable
fun AlbumDropdown(
    modifier: Modifier = Modifier,
    albums: List<GalleryAlbum>,
    dropDownExpanded: Boolean,
    openDropDown: () -> Unit,
    closeDropDown: () -> Unit,
    selectedAlbum: GalleryAlbum?,
    onAlbumSelected: (GalleryAlbum) -> Unit,
) {
    Box(modifier = modifier) {
        TextButton(onClick = openDropDown) {
            Text(text = selectedAlbum?.name ?: "전체")
        }
        DropdownMenu(
            expanded = dropDownExpanded,
            onDismissRequest = closeDropDown
        ) {
            albums.forEach { album ->
                DropdownMenuItem(
                    text = { Text("${album.name} (${album.imageCount})") },
                    onClick = {
                        onAlbumSelected(album)
                        closeDropDown()
                    }
                )
            }
        }
    }
}
