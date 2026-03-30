# DynamicImagePicker

`DynamicImagePicker` is an Android image picker library built with Jetpack Compose.

It focuses on the v1 core flow:

- runtime permission handling
- gallery image browsing
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

## License

This project is licensed under the Apache License 2.0.

See the [LICENSE](LICENSE) file for details.
