package com.animestudio.ui.gallery

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class GalleryItem(
    val file: File,
    val type: MediaType,
    val timestamp: Long
)

enum class MediaType {
    IMAGE, VIDEO
}

sealed class GalleryUiState {
    object Loading : GalleryUiState()
    data class Success(val items: List<GalleryItem>) : GalleryUiState()
    object Empty : GalleryUiState()
}

class GalleryViewModel(
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadGallery()
    }

    fun loadGallery() {
        viewModelScope.launch {
            _uiState.value = GalleryUiState.Loading
            
            val items = mutableListOf<GalleryItem>()
            
            // 1. Load Generated Images
            val genDir = File(context.filesDir, "generated")
            if (genDir.exists()) {
                genDir.listFiles()?.forEach { file ->
                    if (file.extension == "png" || file.extension == "jpg") {
                        items.add(GalleryItem(file, MediaType.IMAGE, file.lastModified()))
                    }
                }
            }
            
            // 2. Load Processed Videos (Assuming they are saved in Movies or similar, 
            // but for now let's check a local 'videos' dir if we had one, 
            // or just the cache if we saved them there temporarily)
            // In a real app, we'd query MediaStore.
            // For this demo, let's assume we might save videos to filesDir/videos
            val videoDir = File(context.filesDir, "videos")
            if (videoDir.exists()) {
                videoDir.listFiles()?.forEach { file ->
                    if (file.extension == "mp4") {
                        items.add(GalleryItem(file, MediaType.VIDEO, file.lastModified()))
                    }
                }
            }
            
            if (items.isEmpty()) {
                _uiState.value = GalleryUiState.Empty
            } else {
                // Sort by newest first
                items.sortByDescending { it.timestamp }
                _uiState.value = GalleryUiState.Success(items)
            }
        }
    }
}

class GalleryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
