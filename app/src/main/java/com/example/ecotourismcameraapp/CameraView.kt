package com.example.ecotourismcameraapp

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.navigation.NavController
import com.example.ecotourismcameraapp.utils.getOutputDirectory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraView(navController: NavController) {
    val context = LocalContext.current

    val permissionsList = remember {
        mutableStateListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    val permissionsState = rememberMultiplePermissionsState(permissionsList)

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    if (permissionsState.allPermissionsGranted) {
        CameraPreview(navController)
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Se requieren permisos de cámara y audio para usar esta aplicación.")
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Solicitar Permisos")
            }
        }
    }
}

@Composable
fun CameraPreview(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as androidx.lifecycle.LifecycleOwner

    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }

    val cameraSelector = remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    val flashMode = remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    val isRecording = remember { mutableStateOf(false) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val outputDirectory = getOutputDirectory(context)

    val previewView = remember { PreviewView(context) }

    val zoomRatio = remember { mutableStateOf(1f) }
    var camera: Camera? by remember { mutableStateOf(null) }

    LaunchedEffect(cameraSelector.value, flashMode.value) {
        val cameraProvider = cameraProviderFuture.get()

        preview = Preview.Builder()
            .build()

        imageCapture = ImageCapture.Builder()
            .setFlashMode(flashMode.value)
            .build()

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector.value,
                preview,
                imageCapture,
                videoCapture
            )
            preview?.setSurfaceProvider(previewView.surfaceProvider)
        } catch (e: Exception) {
            Log.e("CameraPreview", "Error al configurar la cámara", e)
        }
    }

    val minZoom = camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f
    val maxZoom = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 10f

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())

        // Slider de zoom
        if (camera != null) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Slider(
                    value = zoomRatio.value,
                    onValueChange = {
                        zoomRatio.value = it
                        camera?.cameraControl?.setZoomRatio(it)
                    },
                    valueRange = minZoom..maxZoom,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 32.dp)
                        .rotate(-90f)
                )
            }
        }

        // Controles de cámara
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Botón de flash
                IconButton(onClick = {
                    if (camera?.cameraInfo?.hasFlashUnit() == true) {
                        flashMode.value = if (flashMode.value == ImageCapture.FLASH_MODE_OFF) {
                            ImageCapture.FLASH_MODE_ON
                        } else {
                            ImageCapture.FLASH_MODE_OFF
                        }
                        // Reconfigurar la cámara para aplicar el cambio de flash
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()
                    } else {
                        Toast.makeText(context, "El flash no está disponible en esta cámara", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        imageVector = if (flashMode.value == ImageCapture.FLASH_MODE_OFF) Icons.Default.FlashOff else Icons.Default.FlashOn,
                        contentDescription = "Flash"
                    )
                }

                // Botón de captura de foto
                IconButton(onClick = {
                    takePhoto(
                        imageCapture = imageCapture,
                        context = context,
                        outputDirectory = outputDirectory
                    )
                }) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Tomar Foto"
                    )
                }

                // Botón de grabación de video
                IconButton(onClick = {
                    if (isRecording.value) {
                        stopRecording(context, recording)
                        isRecording.value = false
                    } else {
                        startRecording(
                            context = context,
                            videoCapture = videoCapture,
                            outputDirectory = outputDirectory,
                            onRecordingStarted = { rec ->
                                recording = rec
                                isRecording.value = true
                            }
                        )
                    }
                }) {
                    Icon(
                        imageVector = if (isRecording.value) Icons.Default.Stop else Icons.Default.Videocam,
                        contentDescription = "Grabar Video"
                    )
                }
            }

            // Segunda fila de controles
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Botón para cambiar cámara
                IconButton(onClick = {
                    cameraSelector.value = if (cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Cached,
                        contentDescription = "Cambiar Cámara"
                    )
                }

                // Botón para abrir la galería
                IconButton(onClick = {
                    navController.navigate("gallery")
                }) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Abrir Galería"
                    )
                }
            }
        }
    }
}

private fun takePhoto(
    imageCapture: ImageCapture?,
    context: Context,
    outputDirectory: File
) {
    val photoFile = File(
        outputDirectory,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture?.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                // Aplicar filtro a la imagen capturada
                val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                val filteredBitmap = applyFilter(originalBitmap)

                // Guardar la imagen filtrada
                val out = FileOutputStream(photoFile)
                filteredBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
                out.close()

                Toast.makeText(context, "Foto guardada: ${photoFile.absolutePath}", Toast.LENGTH_SHORT).show()
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(context, "Error al guardar foto: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

private fun applyFilter(original: Bitmap): Bitmap {
    val width = original.width
    val height = original.height
    val bitmap = Bitmap.createBitmap(width, height, original.config)

    val canvas = Canvas(bitmap)
    val paint = Paint()
    val colorMatrix = ColorMatrix()

    // Ajustar saturación (incrementar en 1.2)
    colorMatrix.setSaturation(1.2f)

    // Aplicar la matriz de color
    val filter = ColorMatrixColorFilter(colorMatrix)
    paint.colorFilter = filter
    canvas.drawBitmap(original, 0f, 0f, paint)

    return bitmap
}

private fun startRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>?,
    outputDirectory: File,
    onRecordingStarted: (Recording) -> Unit
) {
    val videoFile = File(
        outputDirectory,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".mp4"
    )
    val outputOptions = FileOutputOptions.Builder(videoFile).build()

    val recording = videoCapture?.output
        ?.prepareRecording(context, outputOptions)
        ?.withAudioEnabled()
        ?.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    // Grabación iniciada
                    Toast.makeText(context, "Grabación iniciada", Toast.LENGTH_SHORT).show()
                }
                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        Toast.makeText(context, "Video guardado: ${videoFile.absolutePath}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error al guardar video: ${recordEvent.error}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    if (recording != null) {
        onRecordingStarted(recording)
    }
}

private fun stopRecording(context: Context, recording: Recording?) {
    recording?.stop()
    Toast.makeText(context, "Grabación detenida", Toast.LENGTH_SHORT).show()
}
