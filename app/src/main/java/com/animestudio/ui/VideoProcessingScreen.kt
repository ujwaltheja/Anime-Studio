@file:OptIn(ExperimentalMaterial3Api::class)

package com.animestudio.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.animestudio.R
import com.animestudio.domain.StyleType
import com.animestudio.domain.VideoData
import com.animestudio.ui.theme.*
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
    var showAboutScreen by remember { mutableStateOf(false) }
    var launchVideoPicker by remember { mutableStateOf(false) }

    // Video picker launcher
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onVideoSelected(it) }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            videoPicker.launch("video/*")
        } else {
            Toast.makeText(
                context,
                "Permissions required to select videos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Function to request permissions and launch video picker
    fun requestPermissionsAndPickVideo() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                )
            )
        } else {
            // Android 12 and below
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }
    }

    if (showAboutScreen) {
        AboutScreen(onBackClick = { showAboutScreen = false })
        return
    }

    // Gradient Background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
    ) {
        // Content
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (uiState !is VideoProcessingUiState.Processing) {
                    CenterAlignedTopAppBar(
                        title = { },
                        actions = {
                            IconButton(onClick = { showAboutScreen = true }) {
                                Icon(Icons.Default.Info, contentDescription = "About", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f) togetherWith
                                fadeOut(animationSpec = tween(300))
                    },
                    label = "ScreenTransition"
                ) { state ->
                    when (state) {
                        is VideoProcessingUiState.Idle -> {
                            IdleScreen(
                                onUploadClick = { requestPermissionsAndPickVideo() }
                            )
                        }
                        is VideoProcessingUiState.Loading -> {
                            LoadingScreen(message = state.message)
                        }
                        is VideoProcessingUiState.VideoLoaded -> {
                            StyleSelectionScreen(
                                videoData = state.videoData,
                                onStyleSelected = { styleType, enableUpscaling ->
                                    viewModel.processVideo(styleType, enableUpscaling = enableUpscaling)
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
                                action = state.action,
                                onRetry = { viewModel.reset() },
                                onDownloadModels = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TachibanaYoshino/AnimeGANv2/releases"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
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
        // App Logo or Icon (Placeholder)
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Primary)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Anime Studio",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Transform your videos into anime-style animations with AI.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        GlassButton(
            text = "Upload Video",
            icon = Icons.Default.Upload,
            onClick = onUploadClick
        )
    }
}

@Composable
fun StyleSelectionScreen(
    videoData: VideoData,
    onStyleSelected: (StyleType, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var enableUpscaling by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Choose Style",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )

        // Video info card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Secondary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Video Selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${videoData.duration / 1000}s • ${videoData.width}x${videoData.height}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Upscaling Option
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable 4K Upscaling",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Uses Real-ESRGAN (Slower)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Switch(
                    checked = enableUpscaling,
                    onCheckedChange = { enableUpscaling = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Secondary,
                        checkedTrackColor = Secondary.copy(alpha = 0.5f)
                    )
                )
            }
        }

        // Style options
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(StyleType.values()) { styleType ->
                StyleOptionCard(
                    styleType = styleType,
                    onClick = { onStyleSelected(styleType, enableUpscaling) }
                )
            }
        }

        // Cancel button
        TextButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun StyleOptionCard(
    styleType: StyleType,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Style Icon/Preview (Placeholder)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                getStyleColor(styleType),
                                getStyleColor(styleType).copy(alpha = 0.5f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getStyleInitials(styleType),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = getStyleDisplayName(styleType),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = getStyleDescription(styleType),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
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
    val animatedProgress by animateFloatAsState(
        targetValue = if (total > 0) progress.toFloat() / total else 0f,
        label = "ProgressAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Processing Graphic
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier.fillMaxSize(),
                color = Secondary,
                strokeWidth = 8.dp,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stage,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$progress / $total frames",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
        ) {
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
        CircularProgressIndicator(color = Secondary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
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
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = Secondary,
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Processing Complete!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your video is ready to share.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        GlassButton(
            text = "Share Video",
            icon = Icons.Default.Share,
            onClick = { onShare(outputFile) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onProcessAnother) {
            Text("Process Another Video", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    action: ErrorAction? = null,
    onRetry: () -> Unit,
    onDownloadModels: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = Tertiary,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Oops!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (action == ErrorAction.DOWNLOAD_MODELS) {
            GlassButton(
                text = "Download Models",
                icon = Icons.Default.Download,
                onClick = onDownloadModels
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onRetry) {
                Text("I've Downloaded Them", color = Color.White.copy(alpha = 0.7f))
            }
        } else {
            GlassButton(
                text = "Try Again",
                icon = Icons.Default.Refresh,
                onClick = onRetry
            )
        }
    }
}

// --- Components ---

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        content()
    }
}

@Composable
fun GlassButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// --- Helpers ---

private fun getStyleDisplayName(styleType: StyleType): String {
    return when (styleType) {
        StyleType.CARTOON_GAN -> "CartoonGAN"
        StyleType.ANIME_GAN -> "AnimeGAN"
        StyleType.HAYAO -> "Hayao Style"
        StyleType.SHINKAI -> "Shinkai Style"
        StyleType.PAPRIKA -> "Paprika Style"
        StyleType.CEL_SHADED -> "Cel-Shaded Cartoon"
        StyleType.STYLE_TRANSFER -> "Style Transfer"
        StyleType.CUSTOM -> "Custom Style"
    }
}

private fun getStyleDescription(styleType: StyleType): String {
    return when (styleType) {
        StyleType.CARTOON_GAN -> "Classic cartoon look"
        StyleType.ANIME_GAN -> "Modern vibrant anime"
        StyleType.HAYAO -> "Whimsical & soft"
        StyleType.SHINKAI -> "Realistic & detailed"
        StyleType.PAPRIKA -> "Surreal & dreamlike"
        StyleType.CEL_SHADED -> "Flat colors & sharp edges"
        StyleType.STYLE_TRANSFER -> "Artistic style transfer"
        StyleType.CUSTOM -> "Your custom model"
    }
}

private fun getStyleInitials(styleType: StyleType): String {
    return when (styleType) {
        StyleType.CARTOON_GAN -> "C"
        StyleType.ANIME_GAN -> "A"
        StyleType.HAYAO -> "H"
        StyleType.SHINKAI -> "S"
        StyleType.PAPRIKA -> "P"
        StyleType.CEL_SHADED -> "CS"
        StyleType.STYLE_TRANSFER -> "ST"
        StyleType.CUSTOM -> "?"
    }
}

private fun getStyleColor(styleType: StyleType): Color {
    return when (styleType) {
        StyleType.CARTOON_GAN -> Color(0xFFFF9800)
        StyleType.ANIME_GAN -> Color(0xFF2196F3)
        StyleType.HAYAO -> Color(0xFF4CAF50)
        StyleType.SHINKAI -> Color(0xFF9C27B0)
        StyleType.PAPRIKA -> Color(0xFFE91E63)
        StyleType.CEL_SHADED -> Color(0xFF00BCD4)  // Cyan for cel-shaded
        StyleType.STYLE_TRANSFER -> Color(0xFFFF5722)
        StyleType.CUSTOM -> Color(0xFF607D8B)
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
