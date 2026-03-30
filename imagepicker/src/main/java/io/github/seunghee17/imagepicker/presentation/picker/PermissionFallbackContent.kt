package io.github.seunghee17.imagepicker.presentation.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.seunghee17.imagepicker.R
import io.github.seunghee17.imagepicker.domain.model.PermissionStatus

@Composable
internal fun PermissionFallbackContent(
    modifier: Modifier = Modifier,
    state: ImagePickerContract.State,
    onIntent: (ImagePickerContract.Intent) -> Unit,
) {
    val messageRes = when (state.permissionStatus) {
        PermissionStatus.PARTIALLY_GRANTED -> R.string.permission_partially_granted
        PermissionStatus.PERMANENTLY_DENIED -> R.string.permission_permanently_denied
        PermissionStatus.DENIED -> R.string.permission_denied
        PermissionStatus.GRANTED -> error("PermissionFallbackContent should not render in granted state")
    }

    val buttonLabelRes = when (state.permissionStatus) {
        PermissionStatus.PERMANENTLY_DENIED -> R.string.open_settings
        PermissionStatus.PARTIALLY_GRANTED -> R.string.request_full_permission
        PermissionStatus.DENIED -> R.string.request_permission
        PermissionStatus.GRANTED -> error("PermissionFallbackContent should not render in granted state")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge
        )

        Button(
            onClick = {
                if (state.permissionStatus == PermissionStatus.PERMANENTLY_DENIED) {
                    onIntent(ImagePickerContract.Intent.OpenSettingsClick)
                } else {
                    onIntent(ImagePickerContract.Intent.RequestPermissionClick)
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(stringResource(buttonLabelRes))
        }
    }
}
