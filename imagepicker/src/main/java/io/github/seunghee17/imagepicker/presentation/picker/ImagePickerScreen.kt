package io.github.seunghee17.imagepicker.presentation.picker

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.seunghee17.imagepicker.ImagePickerConfig
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import io.github.seunghee17.imagepicker.domain.model.MediaType
import io.github.seunghee17.imagepicker.domain.model.PermissionStatus
import io.github.seunghee17.imagepicker.presentation.gallery.GalleryContract
import io.github.seunghee17.imagepicker.presentation.gallery.GalleryScreen
import io.github.seunghee17.imagepicker.presentation.gallery.GalleryScreenViewModel
import io.github.seunghee17.imagepicker.presentation.gallery.GalleryScreenViewModelFactory
import io.github.seunghee17.imagepicker.PickerResult
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun ImagePickerScreen(
    config: ImagePickerConfig,
    onResult: (PickerResult) -> Unit,
    onCancel: () -> Unit,
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

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

    LaunchedEffect(Unit) {
        galleryViewModel.effect.collectLatest { galleryEffect ->
            when (galleryEffect) {
                is GalleryContract.Effect.ShowSelectionLimitSnackbar ->
                    snackbarHostState.showSnackbar(galleryEffect.message)
                is GalleryContract.Effect.SelectionConfirmed ->
                    viewModel.handleIntent(ImagePickerContract.Intent.ConfirmSelection(galleryEffect.result))
                GalleryContract.Effect.Cancelled ->
                    viewModel.handleIntent(ImagePickerContract.Intent.Cancel)
            }
        }
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
    LaunchedEffect(hasRequestedPermission) {
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
                    permissionLauncher.launch(requestedPermissionsForPicker(config.allowVideo))
                }
                is ImagePickerContract.Effect.NavigateToSettings -> openAppSettings(context)
                is ImagePickerContract.Effect.ReturnResult -> onResult(effect.result)
                is ImagePickerContract.Effect.Cancelled -> onCancel()
                is ImagePickerContract.Effect.ShowToast ->
                    Toast.makeText(context, effect.message, android.widget.Toast.LENGTH_SHORT).show()
                is ImagePickerContract.Effect.NavigateToEditor -> {
                    val tappedImage = effect.image
                    val selected = galleryState.selectedImages
                    val index = selected.indexOfFirst { it.id == tappedImage.id }.coerceAtLeast(0)
                    editorDestination = EditorDestination(
                        entryId = effect.entryId,
                        imageId = tappedImage.id,
                        originalUri = tappedImage.uri,
                        initialIndex = index,
                        tappedImage = tappedImage,
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
                    pagingFlow = galleryViewModel.pagingFlow,
                    snackbarHostState = snackbarHostState,
                    onIntent = galleryViewModel::handleIntent,
                    onOpenEditor = { image ->
                        if (image.mediaType == MediaType.VIDEO) {
                            // Videos don't support editing; tap toggles selection instead
                            galleryViewModel.handleIntent(GalleryContract.Intent.ToggleImageSelection(image))
                        } else {
                            viewModel.handleIntent(ImagePickerContract.Intent.OpenEditor(image = image))
                        }
                    },
                )
            }
        } else {
            saveableStateHolder.SaveableStateProvider(
                key = "$EDITOR_SCREEN_KEY-${editorDestination!!.entryId}"
            ) {
                val destination = editorDestination!!
                val selected = galleryState.selectedImages
                // 탭한 이미지가 선택 목록에 없으면 맨 앞에 추가하여 단독 표시
                val allImages: List<GalleryImage> = when {
                    selected.any { it.id == destination.imageId } -> selected
                    destination.tappedImage != null -> listOf(destination.tappedImage) + selected
                    selected.isNotEmpty() -> selected
                    else -> listOf(
                        GalleryImage(
                            id = destination.imageId,
                            uri = destination.originalUri,
                            displayName = "", dateTaken = 0L,
                            albumId = "", albumName = "", width = 0, height = 0, mimeType = "",
                        )
                    )
                }
                EditorRoute(
                    destination = destination,
                    allImages = allImages,
                    selectedImages = selected,
                    snackbarHostState = snackbarHostState,
                    onEditApplied = { pickedImage ->
                        galleryViewModel.handleIntent(GalleryContract.Intent.OnEditResult(pickedImage))
                        editorDestination = null
                    },
                    onDismiss = { editorDestination = null },
                    onToggleSelection = { image ->
                        galleryViewModel.handleIntent(GalleryContract.Intent.ToggleImageSelection(image))
                    },
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
