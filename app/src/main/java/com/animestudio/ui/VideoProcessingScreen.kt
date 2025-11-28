package com.animestudio.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.animestudio.domain.StyleType
import com.animestudio.domain.VideoData
import java.io.File

/**
 * Main Jetpack Compose screen for video processing
 */
@Composable
fun VideoProcessingScreen(
    viewModel: VideoProcessingViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Video picker launcher
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onVideoSelected(it) }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = uiState) {
            is VideoProcessingUiState.Idle -> {
                IdleScreen(
                    onUploadClick = { videoPicker.launch("video/*") }
                )
            }
            is VideoProcessingUiState.Loading -> {
                LoadingScreen(message = state.message)
            }
            is VideoProcessingUiState.VideoLoaded -> {
                StyleSelectionScreen(
                    videoData = state.videoData,
                    onStyleSelected = { styleType ->
                        viewModel.processVideo(styleType)
                    },
                    onCancel = { viewModel.reset() }
                )
            }
            is VideoProcessingUiState.Processing -> {
                ProcessingScreen(
                    stage = state.stage,
                    progress = state.progress,
                    total = state.total,
                    onCancel = { viewModel.cancelProcessing() }
                )
            }
            is VideoProcessingUiState.Complete -> {
                CompletionScreen(
                    outputFile = state.outputFile,
                    onShare = { file ->
                        shareVideo(context, file)
                    },
                    onProcessAnother = { viewModel.reset() }
                )
            }
            is VideoProcessingUiState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.reset() }
                )
            }
        }
    }
}

@Composable
fun IdleScreen(
    onUploadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Anime Studio",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Transform your videos into anime-style animations",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onUploadClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Upload Video")
        }
    }
}

@Composable
fun StyleSelectionScreen(
    videoData: VideoData,
    onStyleSelected: (StyleType) -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Choose Animation Style",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Video info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Video Info", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Duration: ${videoData.duration / 1000}s")
                Text("Resolution: ${videoData.width}x${videoData.height}")
                Text("Frame Rate: ${videoData.frameRate} fps")
                Text("Has Audio: ${if (videoData.hasAudio) "Yes" else "No"}")
            }
        }

        // Style options
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(StyleType.values()) { styleType ->
                StyleOptionCard(
                    styleType = styleType,
                    onClick = { onStyleSelected(styleType) }
                )
            }
        }

        // Cancel button
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Cancel")
        }
    }
}

@Composable
fun StyleOptionCard(
    styleType: StyleType,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = getStyleDisplayName(styleType),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = getStyleDescription(styleType),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProcessingScreen(
    stage: String,
    progress: Int,
    total: Int,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stage,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LinearProgressIndicator(
            progress = if (total > 0) progress.toFloat() / total else 0f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "$progress / $total",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}

@Composable
fun LoadingScreen(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message)
    }
}

@Composable
fun CompletionScreen(
    outputFile: File,
    onShare: (File) -> Unit,
    onProcessAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Processing Complete!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your video has been successfully converted",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { onShare(outputFile) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Share Video")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onProcessAnother,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Process Another Video")
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

// Helper functions
private fun getStyleDisplayName(styleType: StyleType): String {
    return when (styleType) {
        StyleType.CARTOON_GAN -> "CartoonGAN"
        StyleType.ANIME_GAN -> "AnimeGAN"
        StyleType.HAYAO -> "Hayao Miyazaki Style"
        StyleType.SHINKAI -> "Makoto Shinkai Style"
        StyleType.PAPRIKA -> "Paprika Style"
        StyleType.CUSTOM -> "Custom Style"
    }
}

private fun getStyleDescription(styleType: StyleType): String {
    return when (styleType) {
        StyleType.CARTOON_GAN -> "Classic cartoon animation style"
        StyleType.ANIME_GAN -> "Modern anime style with vibrant colors"
        StyleType.HAYAO -> "Studio Ghibli inspired whimsical style"
        StyleType.SHINKAI -> "Realistic anime with detailed backgrounds"
        StyleType.PAPRIKA -> "Surreal and dreamlike animation style"
        StyleType.CUSTOM -> "Your custom trained model"
    }
}

private fun shareVideo(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share video"))
}
