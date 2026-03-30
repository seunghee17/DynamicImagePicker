package io.github.seunghee17.imagepicker.presentation.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.seunghee17.imagepicker.R
import io.github.seunghee17.imagepicker.domain.model.GalleryAlbum
import io.github.seunghee17.imagepicker.presentation.gallery.AlbumDropdown

/**
 * 선택 개수와 완료 버튼을 포함한 상단 앱바.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopBarWithCount(
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
    showAlbumSelector: Boolean = true,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(text = if (selectedCount > 0) stringResource(R.string.selection_count, selectedCount, maxCount) else stringResource(R.string.select_photo))
        },
        navigationIcon = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        },
        actions = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedCount > 0
            ) {
                Text(stringResource(R.string.done))
            }
            if (showAlbumSelector) {
                AlbumDropdown(
                    albums = albums,
                    dropDownExpanded = dropDownExpanded,
                    openDropDown = openDropDown,
                    closeDropDown = closeDropDown,
                    selectedAlbum = selectedAlbum,
                    onAlbumSelected = onAlbumSelected
                )
            }
        }
    )
}
