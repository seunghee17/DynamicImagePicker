package io.github.seunghee17.imagepicker

import androidx.core.content.FileProvider

/**
 * 라이브러리 전용 FileProvider.
 *
 * 경로 설정은 AndroidManifest의 <meta-data android:name="android.support.FILE_PROVIDER_PATHS">
 * 엔트리를 통해 res/xml/imagepicker_file_paths.xml에서 읽힌다.
 */
internal class ImagePickerFileProvider : FileProvider()