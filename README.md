# DynamicImagePicker

`DynamicImagePicker` is an Android image picker library built with Jetpack Compose.

It focuses on the v1 core flow:

- runtime permission handling
- gallery image browsing
- configurable max image selection count with selection order support
- drag multi-select
- basic editing with rotate and crop
- returning original and edited results

## Installation

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.seunghee17:imagepicker:1.0.0")
}
```

## Usage

```kotlin
DynamicImagePicker(
    config = ImagePickerConfig(maxSelectionCount = 10),
    onResult = { result -> /* handle PickerResult */ },
    onCancel = { /* handle cancel */ }
)
```
## Feature

https://github.com/user-attachments/assets/093d0710-0bf3-4637-ae96-960e8473fd15

Select multiple images by dragging.

<table>
  <tr>
    <td width="50%">
      <video src="https://github.com/user-attachments/assets/b2d78b46-64c0-4d9c-822b-5d7756e475b4" width="100%">
    </td>
    <td width="50%">
      <video src="https://github.com/user-attachments/assets/77942faf-2ff9-4ee2-b5f3-49bc7024ddf7" width="100%">
    </td>
  </tr>
</table>

Provides image rotation and editing functions.

## License

This project is licensed under the Apache License 2.0.

See the [LICENSE](LICENSE) file for details.
