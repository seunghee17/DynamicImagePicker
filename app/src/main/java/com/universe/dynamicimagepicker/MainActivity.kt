package com.universe.dynamicimagepicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import com.universe.imagepicker.DynamicImagePicker
import com.universe.imagepicker.ImagePickerConfig
import com.universe.imagepicker.presentation.picker.ImagePickerScreen

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DynamicImagePicker(
                config = ImagePickerConfig(),
                onResult = {},
                onCancel = {},
                modifier = Modifier
            )
        }
    }
}