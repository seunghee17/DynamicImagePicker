package com.universe.imagepicker

import androidx.core.content.FileProvider

/**
 * 라이브러리 전용 FileProvider.
 *
 * 호스트 앱이 자체 FileProvider를 등록하더라도 authority가 분리되어 충돌이 발생하지 않는다.
 * authority: "${applicationId}.imagepicker.fileprovider"
 */
internal class ImagePickerFileProvider : FileProvider(R.xml.imagepicker_file_paths)