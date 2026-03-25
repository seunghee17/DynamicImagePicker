package com.universe.imagepicker.presentation.picker

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universe.imagepicker.ImagePickerConfig
import com.universe.imagepicker.domain.model.PermissionStatus
import com.universe.imagepicker.presentation.gallery.GalleryContract
import com.universe.imagepicker.presentation.gallery.GalleryScreen
import com.universe.imagepicker.presentation.gallery.GalleryScreenViewModel
import com.universe.imagepicker.presentation.gallery.GalleryScreenViewModelFactory
import com.universe.imagepicker.PickerResult
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ImagePickerScreen(
    config: ImagePickerConfig,
    onResult: (PickerResult) -> Unit,
    onCancel: () -> Unit,
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel: ImagePickerViewModel = viewModel(
        factory = ImagePickerViewModelFactory()
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val galleryViewModel: GalleryScreenViewModel = viewModel(
        factory = GalleryScreenViewModelFactory(context = context, config = config)
    )
    val galleryState by galleryViewModel.state.collectAsStateWithLifecycle()

    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }
    var editorDestination by rememberSaveable(stateSaver = editorDestinationSaver()) {
        mutableStateOf<EditorDestination?>(null)
    }
    val saveableStateHolder = rememberSaveableStateHolder()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasRequestedPermission = true
        viewModel.handleIntent(
            ImagePickerContract.Intent.OnPermissionEvaluated(
                status = resolvePermissionStatus(
                    context = context,
                    hasRequestedPermission = hasRequestedPermission
                ),
                source = ImagePickerContract.PermissionCheckSource.PERMISSION_RESULT
            )
        )
    }

    // 최초 진입 시 권한 체크
    LaunchedEffect(Unit) {
        viewModel.handleIntent(ImagePickerContract.Intent.Initialize)
    }

    // 화면 복귀 시 권한 재확인 (설정에서 돌아온 경우 대응)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.handleIntent(ImagePickerContract.Intent.OnHostResumed)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 권한 허용(전체 또는 부분) 시 갤러리 초기화
    LaunchedEffect(state.permissionStatus) {
        if (state.permissionStatus == PermissionStatus.GRANTED ||
            state.permissionStatus == PermissionStatus.PARTIALLY_GRANTED
        ) {
            galleryViewModel.handleIntent(GalleryContract.Intent.Initialize)
        }
    }

    // Effect 처리
    LaunchedEffect(viewModel, context, hasRequestedPermission) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ImagePickerContract.Effect.CheckPermission -> {
                    viewModel.handleIntent(
                        ImagePickerContract.Intent.OnPermissionEvaluated(
                            status = resolvePermissionStatus(
                                context = context,
                                hasRequestedPermission = hasRequestedPermission
                            ),
                            source = effect.source
                        )
                    )
                }
                is ImagePickerContract.Effect.RequestPermission -> {
                    hasRequestedPermission = true
                    permissionLauncher.launch(requestedPermissionsForPicker())
                }
                is ImagePickerContract.Effect.NavigateToSettings -> openAppSettings(context)
                is ImagePickerContract.Effect.ReturnResult -> onResult(effect.result)
                is ImagePickerContract.Effect.Cancelled -> onCancel()
                is ImagePickerContract.Effect.ShowToast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is ImagePickerContract.Effect.NavigateToEditor -> {
                    editorDestination = EditorDestination(
                        entryId = effect.entryId,
                        imageId = effect.image.id,
                        originalUri = effect.image.uri
                    )
                }
            }
        }
    }

    // 화면 라우팅: 전체/부분 권한 → 갤러리, 미허용 → 권한 안내
    val hasGalleryAccess = state.permissionStatus == PermissionStatus.GRANTED ||
            state.permissionStatus == PermissionStatus.PARTIALLY_GRANTED
    if (hasGalleryAccess) {
        if (editorDestination == null) {
            saveableStateHolder.SaveableStateProvider(key = GALLERY_SCREEN_KEY) {
                GalleryScreen(
                    modifier = modifier,
                    state = galleryState,
                    effect = galleryViewModel.effect,
                    onIntent = galleryViewModel::handleIntent,
                    onOpenEditor = { image ->
                        viewModel.handleIntent(ImagePickerContract.Intent.OpenEditor(image = image))
                    },
                    onConfirm = { result ->
                        viewModel.handleIntent(ImagePickerContract.Intent.ConfirmSelection(result))
                    },
                    onCancel = {
                        viewModel.handleIntent(ImagePickerContract.Intent.Cancel)
                    }
                )
            }
        } else {
            saveableStateHolder.SaveableStateProvider(
                key = "$EDITOR_SCREEN_KEY-${editorDestination!!.entryId}"
            ) {
                EditorRoute(
                    destination = editorDestination!!,
                    onEditApplied = { pickedImage ->
                        galleryViewModel.handleIntent(GalleryContract.Intent.OnEditResult(pickedImage))
                        editorDestination = null
                    },
                    onDismiss = { editorDestination = null },
                    onError = onError,
                    modifier = modifier,
                    allowEditing = config.allowEditing,
                )
            }
        }
    } else {
        PermissionFallbackContent(
            modifier = modifier,
            state = state,
            onIntent = viewModel::handleIntent
        )
    }
}

private const val GALLERY_SCREEN_KEY = "gallery-screen"
private const val EDITOR_SCREEN_KEY = "editor-screen"
