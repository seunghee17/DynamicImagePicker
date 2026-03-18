package com.universe.dynamicimagepicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.universe.imagepicker.ImagePickerConfig
import com.universe.imagepicker.presentation.gallery.GalleryScreen
import com.universe.imagepicker.presentation.picker.ImagePickerScreen

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImagePickerScreen(
                config = ImagePickerConfig(

                ),
                onResult = TODO(),
                onCancel = TODO(),
                modifier = Modifier
            )
        }
    }
}

@Composable
fun MainScreen() {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Button(
            onClick = {
                //GalleryScreen()
            },
            content = {
                Text(text = "사진 선택")
            }
        )
    }
}