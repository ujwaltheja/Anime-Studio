package com.animestudio.ui.gallery

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animestudio.ui.theme.Background
import com.animestudio.ui.theme.Secondary
import java.io.File

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = viewModel(
        factory = GalleryViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

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
            Text(
                text = "Gallery",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier.padding(bottom = 16.dp, top = 16.dp)
            )

            when (val state = uiState) {
                is GalleryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Secondary)
                    }
                }
                is GalleryUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No images generated yet", color = Color.White.copy(alpha = 0.5f))
                    }
                }
                is GalleryUiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.items) { item ->
                            GalleryItemCard(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryItemCard(item: GalleryItem) {
    // Load bitmap efficiently (simplified for demo)
    val bitmap = remember(item.file) {
        if (item.type == MediaType.IMAGE) {
            BitmapFactory.decodeFile(item.file.absolutePath)
        } else {
            // Placeholder for video thumbnail
            null
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable { /* Open full screen */ }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fallback / Video icon
            Icon(
                imageVector = if (item.type == MediaType.VIDEO) Icons.Default.Videocam else Icons.Default.Image,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        if (item.type == MediaType.VIDEO) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(4.dp)
                    .size(16.dp)
            )
        }
    }
}
