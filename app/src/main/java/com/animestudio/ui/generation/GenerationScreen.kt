package com.animestudio.ui.generation

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animestudio.ui.theme.Primary
import com.animestudio.ui.theme.Secondary
import com.animestudio.ui.theme.Background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerationScreen(
    viewModel: GenerationViewModel = viewModel(
        factory = GenerationViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val config by viewModel.config.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Text(
                text = "AI Art Generator",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
            )

            // Result Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                when (val state = uiState) {
                    is GenerationUiState.Success -> {
                        Image(
                            bitmap = state.image.asImageBitmap(),
                            contentDescription = "Generated Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    is GenerationUiState.Loading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Secondary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.message,
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (state.progress > 0) {
                                Text(
                                    text = "${state.progress}%",
                                    color = Secondary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    is GenerationUiState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Enter a prompt to start",
                                color = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Prompt",
                        style = MaterialTheme.typography.labelLarge,
                        color = Secondary
                    )
                    OutlinedTextField(
                        value = config.prompt,
                        onValueChange = { viewModel.updateConfig(config.copy(prompt = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 1girl, anime style, blue hair") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Secondary,
                            focusedBorderColor = Secondary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Negative Prompt",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = config.negativePrompt,
                        onValueChange = { viewModel.updateConfig(config.copy(negativePrompt = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Secondary,
                            focusedBorderColor = Secondary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Advanced Settings
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Settings", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Steps: ${config.steps}", color = Color.White)
                    Slider(
                        value = config.steps.toFloat(),
                        onValueChange = { viewModel.updateConfig(config.copy(steps = it.toInt())) },
                        valueRange = 10f..50f,
                        steps = 39,
                        colors = SliderDefaults.colors(
                            thumbColor = Secondary,
                            activeTrackColor = Secondary
                        )
                    )

                    Text("Guidance Scale: ${config.guidanceScale}", color = Color.White)
                    Slider(
                        value = config.guidanceScale,
                        onValueChange = { viewModel.updateConfig(config.copy(guidanceScale = it)) },
                        valueRange = 1f..20f,
                        colors = SliderDefaults.colors(
                            thumbColor = Secondary,
                            activeTrackColor = Secondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Generate Button
            Button(
                onClick = { viewModel.generateImage() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState !is GenerationUiState.Loading
            ) {
                if (uiState is GenerationUiState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Art", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        color = Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    ) {
        content()
    }
}
