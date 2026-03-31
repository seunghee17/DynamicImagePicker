# Project Purpose
This project implements an image picker library for Android using Jetpack Compose.
v1 focuses on the core image selection flow.

### Development Goals
- Implement a reusable Image Picker component for use across apps
- Design a Compose-based UI and state management architecture
- Provide minimal but stable image selection and editing features
- Establish an extensible structure for future support (e.g., video)

## v1 Feature Scope
### Included Features

1. Image access permission request
2. Device gallery image list retrieval
3. Maximum 10 image selection limit
4. Selected image editing
    - 90-degree rotation
    - Crop
5. Drag multi-select
6. Return selection result

## 3. Core Requirements

## 3.1 Permission Handling

### Description
Request device storage or photo access permission so users can select gallery images.

### Requirements

- If required permissions are missing on first entry, prompt the user.
- If permission is granted, load the image list.
- If permission is permanently denied, guide the user to the Settings screen.

### Success Criteria
- After granting permission, the user must be able to access the image list screen.

---

## 3.2 Image List

### Description
Retrieve images stored in the device gallery and display them in a grid layout.

### Requirements
- Allow access by gallery album name.
- Display images sorted by most recent first.
- Show each image as a thumbnail.
- Provide a scrollable grid UI.
- UI must not freeze during image loading.

### Success Criteria
- Users must be able to view all gallery images in a list format.

---

## 3.3 Image Selection

### Description
Users can select images from the gallery list.

### Requirements
- Images can be selected/deselected by tapping.
- Up to 10 images can be selected.
- Attempting to select more than 10 images must show a limit warning.
- Selected images must be visually distinguished in the UI.
- Selection order is shown as a number on each grid item.
- Drag multi-select is supported.
- The current selection count must be visible on screen.

### Success Criteria
- Users must be able to select up to 10 images.
- The system must block selection beyond the limit.

### Policy

- Maximum selection count: 10
- The 11th selection attempt must be rejected
- Limit message example:

    `You can select up to 10 images.`


---

## 3.4 Image Editing

### Description
Users can perform basic editing on selected images.

### Supported Features
- Rotation
- Crop

---

### 3.4.1 Rotation

### Requirements
- Users can rotate images in 90-degree increments.
- Rotation is triggered by a button tap.
- The rotation result must be reflected immediately in the preview.

### Success Criteria
- Each tap of the rotate button must rotate the image by 90 degrees.

### Constraints
- Free-angle rotation is not supported in v1.
- Rotation direction is limited to clockwise 90 degrees.

---

### 3.4.2 Crop

### Requirements

- Users must be able to specify a crop region.
- The crop region must be visually displayed on screen.
- Upon crop confirmation, only the selected region should be retained.

### Success Criteria
- The image must be cropped to the user-specified region and reflected in the result.

### Constraints
- Crop ratio options are limited in v1.
- Advanced editing (zoom-based precision editing, multiple ratio presets, etc.) is excluded.

---

## 3.5 Result Return

### Description

After completing selection and editing, the result must be returned to the caller.

### Requirements

- On completion, return the list of selected images.
- If editing was applied, return the edited result instead of the original.
- Original URI and edited URI must be distinguishable.

### Success Criteria

- The caller must be able to receive the selection/edit result and perform follow-up processing.

### Return Data Example

```kotlin
data class PickerResult(
    val items: List<PickedImage>
)

data class PickedImage(
    val originalUri: Uri,
    val editedUri: Uri? = null,
    val rotationDegrees: Int = 0,
    val cropRect: CropRect? = null    // isCropped is determined by cropRect != null
)
```

---

## 4. User Flows

## 4.1 Basic Selection Flow

1. User launches the Image Picker.
2. If permissions are missing, display the permission request UI.
3. After permission is granted, load the gallery image list.
4. User selects images by tapping or dragging.
5. Up to 10 images can be selected.
6. User taps the Done button to return the selection result.

---

## 4.2 Editing Flow

1. Tapping an image (outside its checkbox area) opens the editor screen.
2. User performs rotation or crop.
3. Editing result is saved.
4. The result is reflected in the selection on completion.
5. On final Done, the result is returned to the caller.

---

## 5. Screen Requirements

## 5.1 Permission Request Screen

### Elements
- Settings navigation button (when needed)

### States
- Before first request
- Denied
- Permanently denied

---

## 5.2 Image List Screen

### Elements
- Top bar
- Selection count display
- Done button
- Scrollable grid of image thumbnails

### States
- Loading
- Has data
- No data
- No permission

---

## 5.3 Editor Screen

### Elements
- Original/edited image preview
- Rotate button
- Crop button
- Cancel button
- Done button

### States
- Before editing
- Rotation applied
- Crop applied
- Saving

---

## 6. Error Handling

### 6.1 Permission Denied
- Do not show the image list if permission is denied.
- Display a message explaining that permission is required.

### 6.2 No Images
- Display an empty state UI if the gallery has no images.

### 6.3 Selection Limit Exceeded
- Block additional selection and show a warning message when the limit is exceeded.

### 6.4 Edit Failure
- Display an error message if crop or rotation processing fails.
- The original image must not be damaged.

### 6.5 Save Failure
- If saving the edited result fails, provide options to retry or cancel.

---

# Project Structure & Work Guide

## Module Layout

The project consists of two modules:

- **`:app`** — Sample app demonstrating library usage (`com.universe.dynamicimagepicker`)
- **`:imagepicker`** — The actual library implementation (`com.universe.imagepicker`)

> All feature implementation must be done in `:imagepicker`. `:app` only demonstrates integration and should not be modified.

---

## Architecture Pattern: MVI

The overall structure follows the **MVI (Model-View-Intent)** pattern.

```
User Action → Intent → ViewModel → State update → Composable render
                                  ↓
                             Effect (Channel) → one-time side effects
```

- **State**: Immutable data for the UI. Managed via `StateFlow` in ViewModel.
- **Intent**: Events triggered by the user or system. Defined as sealed classes.
- **Effect**: One-time events such as navigation, toasts, or permission requests. Managed via `Channel`.

---

## Layer Structure

```
presentation/
  picker/          ← Gallery selection screen (Screen, State, Intent, Effect, ViewModel, Factory)
  editor/          ← Image editor screen (Screen, State, Intent, Effect, ViewModel, Factory)
  gallery/         ← Gallery grid components (GalleryScreen, GalleryGridItem, AlbumDropdown)
  component/       ← Reusable UI components (TopBarWithCount, SelectionBadge)
  utils/           ← Drag multi-select helper (photoGridDragHandler modifier)
domain/
  model/           ← Domain models (GalleryImage, GalleryAlbum, PickedImage, PickerResult, CropRect, PermissionStatus)
  repository/      ← Repository interfaces (GalleryRepository, ImageEditRepository)
  usecase/         ← Use cases (GetGalleryAlbums, GetImagesInAlbum, RotateImage, CropImage, CheckPermission)
data/
  source/          ← Data sources (MediaStoreDataSource, ImageFileDataSource)
  repository/      ← Repository implementations (GalleryRepositoryImpl, ImageEditRepositoryImpl)
```

---

## Key File Roles

| File | Role |
|------|------|
| `DynamicImagePicker.kt` | Public entry point of the library. Exposes the top-level `@Composable fun DynamicImagePicker(config, onResult, onCancel, onError, modifier)` |
| `ImagePickerConfig.kt` | Library configuration (`maxSelectionCount`, `showAlbumSelector`, `allowEditing`) |
| `ImagePickerScreen.kt` | Permission handling + screen routing (gallery vs. permission denied screen) |
| `GalleryScreen.kt` | 3-column grid, drag multi-select, top bar, selection limit snackbar |
| `GalleryGridItem.kt` | Thumbnail + selection overlay + SelectionBadge |
| `ImagePickerViewModel.kt` | Manages overall gallery selection state, merges edit results, returns final result |
| `EditorScreen.kt` | Image editing UI (rotation/crop) |
| `EditorViewModel.kt` | Manages editing state. Always rotates from `originalUri` to prevent JPEG quality degradation |
| `MediaStoreDataSource.kt` | Queries images/albums via MediaStore ContentResolver (API 26–36 compatible) |
| `ImageFileDataSource.kt` | Bitmap rotation and crop. Cache: `context.cacheDir/imagepicker_edits/` |
| `Utils.kt` | `photoGridDragHandler` Modifier — implements long-press + drag multi-select |

---

## Usage

The library exposes a single top-level Composable function. Call it directly — there is no wrapper object or `.Content()` method.

```kotlin
DynamicImagePicker(
    config = ImagePickerConfig(),
    onResult = { result: PickerResult -> /* handle result */ },
    onCancel = { /* handle cancel */ },
    onError = { message -> /* handle error */ }
)
```

---

## Domain Model Summary

```kotlin
// Final return result
data class PickerResult(val items: List<PickedImage>)

data class PickedImage(
    val originalUri: Uri,
    val editedUri: Uri? = null,       // null if no editing was applied
    val rotationDegrees: Int = 0,
    val cropRect: CropRect? = null    // isCropped is determined by cropRect != null
)

// Normalized crop coordinates [0f, 1f]
data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    companion object { val FULL = CropRect(0f, 0f, 1f, 1f) }
}

data class GalleryImage(val id: Long, val uri: Uri, val albumId: String, ...)
data class GalleryAlbum(val id: String, val name: String, val coverUri: Uri, val imageCount: Int)

enum class PermissionStatus { GRANTED, PARTIALLY_GRANTED, DENIED, PERMANENTLY_DENIED }
```

---

## How to Work

### 1. Adding a New Feature
1. Add or modify data models in `domain/model/`
2. If needed, add methods to `domain/repository/` interfaces
3. Add a UseCase in `domain/usecase/`
4. Reflect changes in `data/source/` and `data/repository/` implementations
5. Add entries to the relevant `*Intent`, `*State`, `*Effect` sealed classes
6. Add handling logic to `handleIntent()` in the ViewModel
7. Consume State/Effect in the Composable
8. All code changes must follow the officially recommended approach per the relevant documentation

### 2. Modifying UI
- Reusable components live in `presentation/component/`
- Screen-specific components are managed within their screen package (`picker/`, `gallery/`, `editor/`)
- Compose state is collected from ViewModel `StateFlow` using `collectAsStateWithLifecycle()`

### 3. Modifying Drag Selection
- Modify the `photoGridDragHandler` Modifier in `presentation/utils/Utils.kt`
- Flow: long-press detection → drag start → item selection based on hit-box

### 4. Modifying Editing Features
- `EditorViewModel` always applies rotation from `originalUri` (prevents accumulated JPEG re-compression)
- Crop is applied based on the current `previewUri`
- On edit completion, return `PickedImage(editedUri = ..., rotationDegrees = ..., cropRect = ...)`

### 5. Modifying Permission Handling
- Permission branching logic: `ImagePickerScreen.kt`
- State representation: `PermissionStatus` enum
- Android 14+ partial permission (`READ_MEDIA_VISUAL_USER_SELECTED`) is handled as `PARTIALLY_GRANTED`

---

## Build Configuration

- **compileSdk / targetSdk**: 36
- **minSdk**: 26
- **Kotlin**: 2.0.21
- **Compose BOM**: 2025.05.00
- **Image loading**: Coil 2.7.0 (`AsyncImage`)
- **Async**: Kotlin Coroutines 1.9.0
- **ViewModel**: AndroidX Lifecycle 2.9.0
