package com.universe.imagepicker.presentation.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.universe.imagepicker.domain.model.GalleryAlbum
import com.universe.imagepicker.presentation.gallery.AlbumDropdown

/**
 * 선택 개수와 완료 버튼을 포함한 상단 앱바.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithCount(
    selectedCount: Int,
    albums: List<GalleryAlbum>,
    dropDownExpanded: Boolean,
    openDropDown: () -> Unit,
    closeDropDown: () -> Unit,
    selectedAlbum: GalleryAlbum?,
    onAlbumSelected: (GalleryAlbum) -> Unit,
    maxCount: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(text = if (selectedCount > 0) "$selectedCount / $maxCount" else "사진 선택")
        },
        navigationIcon = {
            TextButton(onClick = onCancel) {
                Text("취소")
            }
        },
        actions = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedCount > 0
            ) {
                Text("완료")
            }
            AlbumDropdown(
                albums = albums,
                dropDownExpanded = dropDownExpanded,
                openDropDown = openDropDown,
                closeDropDown = closeDropDown,
                selectedAlbum = selectedAlbum,
                onAlbumSelected = onAlbumSelected
            )
        }
    )
}
