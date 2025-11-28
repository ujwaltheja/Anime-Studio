package com.animestudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.animestudio.ui.VideoProcessingScreen
import com.animestudio.ui.VideoProcessingViewModel
import com.animestudio.ui.theme.AnimeStudioTheme

/**
 * Main Activity for Anime Studio app
 *
 * Features:
 * - Video upload/recording
 * - Style selection
 * - Real-time processing progress
 * - Video preview and sharing
 */
class MainActivity : ComponentActivity() {

    private val viewModel: VideoProcessingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AnimeStudioTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VideoProcessingScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel any ongoing processing when activity is destroyed
        viewModel.cancelProcessing()
    }
}
