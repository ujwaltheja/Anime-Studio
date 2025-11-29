package com.animestudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.animestudio.ui.VideoProcessingScreen
import com.animestudio.ui.VideoProcessingViewModel
import com.animestudio.ui.gallery.GalleryScreen
import com.animestudio.ui.generation.GenerationScreen
import com.animestudio.ui.theme.AnimeStudioTheme
import com.animestudio.ui.theme.Background
import com.animestudio.ui.theme.Primary
import com.animestudio.ui.theme.Secondary
import com.animestudio.vtuber.VTuberScreen

/**
 * Main Activity for Anime Studio app
 */
class MainActivity : ComponentActivity() {

    private val videoViewModel: VideoProcessingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AnimeStudioTheme {
                MainScreen(videoViewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        videoViewModel.cancelProcessing()
    }
}

@Composable
fun MainScreen(videoViewModel: VideoProcessingViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.VIDEO) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Background,
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.VIDEO,
                    onClick = { currentScreen = Screen.VIDEO },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                    label = { Text("Video") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Secondary,
                        selectedTextColor = Secondary,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = Primary.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.GENERATE,
                    onClick = { currentScreen = Screen.GENERATE },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text("Generate") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Secondary,
                        selectedTextColor = Secondary,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = Primary.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.VTUBER,
                    onClick = { currentScreen = Screen.VTUBER },
                    icon = { Icon(Icons.Default.Face, contentDescription = null) },
                    label = { Text("VTuber") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Secondary,
                        selectedTextColor = Secondary,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = Primary.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.GALLERY,
                    onClick = { currentScreen = Screen.GALLERY },
                    icon = { Icon(Icons.Default.Collections, contentDescription = null) },
                    label = { Text("Gallery") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Secondary,
                        selectedTextColor = Secondary,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = Primary.copy(alpha = 0.2f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.VIDEO -> VideoProcessingScreen(
                    viewModel = videoViewModel
                )
                Screen.GENERATE -> GenerationScreen()
                Screen.VTUBER -> VTuberScreen()
                Screen.GALLERY -> GalleryScreen()
            }
        }
    }
}

enum class Screen {
    VIDEO, GENERATE, VTUBER, GALLERY
}
