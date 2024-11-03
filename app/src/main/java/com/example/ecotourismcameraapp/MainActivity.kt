package com.example.ecotourismcameraapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.ecotourismcameraapp.ui.theme.EcoTourismCameraAppTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EcoTourismCameraAppTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "main_menu",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("main_menu") { MainMenuScreen(navController) }
                        composable("camera_view") { CameraView(navController) }
                        composable("gallery") { GalleryScreen(navController) }
                        composable("video_player/{videoUri}") { backStackEntry ->
                            val videoUri = backStackEntry.arguments?.getString("videoUri")
                            videoUri?.let { VideoPlayerScreen(it) }
                        }
                    }
                }
            }
        }
    }
}
