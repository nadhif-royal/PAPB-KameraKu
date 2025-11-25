package com.example.kameraku

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.kameraku.ui.CameraScreen
import com.example.kameraku.ui.theme.KameraKuTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KameraKuTheme {

                var permissionGranted by remember { mutableStateOf(false) }

                val permissionLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        permissionGranted = granted
                    }

                // MINTA IZIN SEKALI SAAT APP DIBUKA
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (permissionGranted) {
                        CameraScreen()
                    } else {
                        Text("Izin kamera diperlukan untuk melanjutkan.")
                    }
                }
            }
        }
    }
}
