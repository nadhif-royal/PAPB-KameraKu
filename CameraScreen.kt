package com.example.kameraku.ui

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen() {


    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var lastImageUri by remember { mutableStateOf<Uri?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }



    Column(modifier = Modifier.fillMaxSize()) {

        // PREVIEW AREA
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build()
                    val capture = ImageCapture.Builder()
                        .setFlashMode(
                            if (flashEnabled)
                                ImageCapture.FLASH_MODE_ON
                            else
                                ImageCapture.FLASH_MODE_OFF
                        )
                        .build()

                    imageCapture = capture

                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        capture
                    )
                }, androidx.core.content.ContextCompat.getMainExecutor(ctx))


                previewView
            }
        )

        // CONTROLS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Flip camera
            IconButton(onClick = {
                cameraSelector =
                    if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    else
                        CameraSelector.DEFAULT_BACK_CAMERA
            }) {
                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera")
            }

            // Flash toggle
            IconButton(onClick = {
                flashEnabled = !flashEnabled
            }) {
                Icon(Icons.Default.FlashOn, contentDescription = "Flash")
            }

            // Take photo
            IconButton(onClick = {
                takePhoto(
                    context = context,
                    imageCapture = imageCapture,
                    onImageSaved = { uri -> lastImageUri = uri }
                )
            }) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo")
            }

            // Thumbnail preview
            if (lastImageUri != null) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                }
            }
        }

        lastImageUri?.let { uri ->
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Last Photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(8.dp)
            )
        }
    }
}

fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    onImageSaved: (Uri) -> Unit
) {
    val capture = imageCapture ?: return

    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        .format(System.currentTimeMillis())

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    }

    val output = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    capture.takePicture(
        output,
        androidx.core.content.ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {

            override fun onError(exc: ImageCaptureException) {
                Toast.makeText(context, "Gagal menyimpan foto!", Toast.LENGTH_SHORT).show()
            }

            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                result.savedUri?.let(onImageSaved)
                Toast.makeText(context, "Foto Tersimpan", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
