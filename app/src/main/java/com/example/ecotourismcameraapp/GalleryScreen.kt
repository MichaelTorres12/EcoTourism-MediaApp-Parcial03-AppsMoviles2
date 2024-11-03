package com.example.ecotourismcameraapp

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import java.io.File
import android.media.ThumbnailUtils
import android.provider.MediaStore
import androidx.compose.foundation.background
import com.example.ecotourismcameraapp.utils.getOutputDirectory

@Composable
fun GalleryScreen(navController: NavController) {
    val context = LocalContext.current
    val mediaFiles = getMediaFiles(context)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp)
    ) {
        items(mediaFiles) { file ->
            if (file.extension == "jpg") {
                // Mostrar imagen
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(4.dp)
                            .clickable {
                                // Acción al hacer clic en la imagen
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            } else if (file.extension == "mp4") {
                // Generar miniatura del video
                val thumbnail = ThumbnailUtils.createVideoThumbnail(
                    file.absolutePath,
                    MediaStore.Video.Thumbnails.MINI_KIND
                )?.asImageBitmap()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(4.dp)
                        .clickable {
                            // Navegar a la pantalla de reproducción de video
                            val encodedUri = Uri.encode(file.absolutePath)
                            navController.navigate("video_player/$encodedUri")
                        }
                ) {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Superposición para indicar que es un video
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .align(Alignment.BottomCenter)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "Video",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

fun getMediaFiles(context: Context): List<File> {
    val directory = getOutputDirectory(context)
    return directory.listFiles()?.sorted()?.reversed()?.toList() ?: emptyList()
}
