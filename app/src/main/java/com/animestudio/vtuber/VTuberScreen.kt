package com.animestudio.vtuber

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animestudio.ui.generation.GlassCard
import com.animestudio.ui.theme.Background
import com.animestudio.ui.theme.Primary
import com.animestudio.ui.theme.Secondary

@Composable
fun VTuberScreen(
    viewModel: VTuberViewModel = viewModel(
        factory = VTuberViewModelFactory(LocalContext.current)
    )
) {
    val faceData by viewModel.faceData.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Live Avatar",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
            )

            // Avatar View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                // Background grid or effect
                
                // The Avatar
                AvatarRenderer(
                    faceData = faceData,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                )
                
                // Status indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isTracking) Color.Green else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTracking) "Tracking Active" else "Tracking Paused",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Controls",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ControlButton(
                            icon = if (isTracking) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            label = if (isTracking) "Stop Camera" else "Start Camera",
                            isActive = isTracking,
                            onClick = { viewModel.toggleTracking() }
                        )
                        
                        ControlButton(
                            icon = Icons.Default.Face,
                            label = "Calibrate",
                            isActive = false,
                            onClick = { viewModel.calibrate() }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isActive) Secondary else Color.White.copy(alpha = 0.1f),
                contentColor = if (isActive) Color.Black else Color.White
            ),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(icon, contentDescription = null)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
