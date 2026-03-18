package com.universe.imagepicker.presentation.picker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universe.imagepicker.ImagePickerConfig
import com.universe.imagepicker.domain.model.PermissionStatus
import com.universe.imagepicker.domain.model.PickerResult
import com.universe.imagepicker.presentation.gallery.GalleryScreen
import com.universe.imagepicker.presentation.picker.viewmodel.ImagePickerViewModel
import com.universe.imagepicker.presentation.picker.viewmodel.ImagePickerViewModelFactory
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ImagePickerScreen(
    config: ImagePickerConfig,
    onResult: (PickerResult) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: ImagePickerViewModel = viewModel(
        factory = ImagePickerViewModelFactory(
            context = context,
            config = config
        )
    )
    val state by viewModel.state.collectAsState()
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasRequestedPermission = true
        viewModel.handleIntent(
            ImagePickerIntent.OnPermissionEvaluated(
                status = resolvePermissionStatus(
                    context = context,
                    hasRequestedPermission = hasRequestedPermission
                ),
                source = PermissionCheckSource.PERMISSION_RESULT
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.handleIntent(ImagePickerIntent.Initialize)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.handleIntent(ImagePickerIntent.OnHostResumed)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel, context, hasRequestedPermission) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ImagePickerEffect.CheckPermission -> {
                    viewModel.handleIntent(
                        ImagePickerIntent.OnPermissionEvaluated(
                            status = resolvePermissionStatus(
                                context = context,
                                hasRequestedPermission = hasRequestedPermission
                            ),
                            source = effect.source
                        )
                    )
                }
                ImagePickerEffect.RequestPermission -> {
                    hasRequestedPermission = true
                    permissionLauncher.launch(requestedPermissionsForPicker())
                }
                ImagePickerEffect.NavigateToSettings -> openAppSettings(context)
                is ImagePickerEffect.ReturnResult -> onResult(effect.result)
                ImagePickerEffect.Cancelled -> onCancel()
                is ImagePickerEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ImagePickerEffect.NavigateToEditor -> Unit
            }
        }
    }

    if (state.permissionStatus == PermissionStatus.GRANTED) {
        GalleryScreen(
            modifier = modifier,
            state = state,
            onIntent = viewModel::handleIntent
        )
    } else {
        PermissionFallbackContent(
            modifier = modifier,
            state = state,
            onIntent = viewModel::handleIntent
        )
    }
}

private fun requestedPermissionsForPicker(): Array<String> {
    return when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 -> {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        }
        else -> {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        }
    }
}

private fun fullAccessPermission(): String {
    return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        Manifest.permission.READ_EXTERNAL_STORAGE
    } else {
        Manifest.permission.READ_MEDIA_IMAGES
    }
}

private fun resolvePermissionStatus(
    context: Context,
    hasRequestedPermission: Boolean
): PermissionStatus {
    val activity = context.findActivity()
    val fullAccessPermission = fullAccessPermission()
    val hasFullAccess = ContextCompat.checkSelfPermission(
        context,
        fullAccessPermission
    ) == PackageManager.PERMISSION_GRANTED

    if (hasFullAccess) {
        return PermissionStatus.GRANTED
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val hasSelectedPhotoAccess = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        ) == PackageManager.PERMISSION_GRANTED

        if (hasSelectedPhotoAccess) {
            return PermissionStatus.PARTIALLY_GRANTED
        }
    }

    val shouldShowRationale = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, fullAccessPermission)
    } ?: false

    return if (hasRequestedPermission && !shouldShowRationale) {
        PermissionStatus.PERMANENTLY_DENIED
    } else {
        PermissionStatus.DENIED
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
