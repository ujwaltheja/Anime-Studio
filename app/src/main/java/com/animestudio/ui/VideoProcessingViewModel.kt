package com.animestudio.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animestudio.data.VideoProcessorFactory
import com.animestudio.domain.*
import com.animestudio.video.VideoInputManagerImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for managing video processing state and operations
 */
class VideoProcessingViewModel(application: Application) : AndroidViewModel(application) {

    private val videoInputManager = VideoInputManagerImpl(application)
    private val videoProcessor = VideoProcessorFactory.create(application)

    private val _uiState = MutableStateFlow<VideoProcessingUiState>(VideoProcessingUiState.Idle)
    val uiState: StateFlow<VideoProcessingUiState> = _uiState.asStateFlow()

    private var currentVideoData: VideoData? = null

    /**
     * Handle video selection from gallery
     */
    fun onVideoSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = VideoProcessingUiState.Loading("Loading video...")

            videoInputManager.setCurrentVideoUri(uri)

            when (val result = videoInputManager.getVideoMetadata(uri)) {
                is Result.Success -> {
                    currentVideoData = result.data
                    _uiState.value = VideoProcessingUiState.VideoLoaded(result.data)
                }
                is Result.Error -> {
                    _uiState.value = VideoProcessingUiState.Error(result.message)
                }
                else -> {}
            }
        }
    }

    /**
     * Start video processing with selected style
     */
    fun processVideo(styleType: StyleType) {
        val videoData = currentVideoData ?: run {
            _uiState.value = VideoProcessingUiState.Error("No video loaded")
            return
        }

        viewModelScope.launch {
            // Configure style
            val styleConfig = StyleConfig(
                styleType = styleType,
                modelPath = getModelPathForStyle(styleType),
                useGPU = true,
                inputSize = 512,
                outputQuality = 90
            )

            // Prepare output file
            val outputDir = File(getApplication<Application>().getExternalFilesDir(null), "outputs")
            outputDir.mkdirs()
            val outputFile = File(outputDir, "styled_video_${System.currentTimeMillis()}.mp4")

            // Process video
            videoProcessor.processVideo(videoData, styleConfig, outputFile)
                .collect { state ->
                    when (state) {
                        is ProcessingState.Idle -> {
                            _uiState.value = VideoProcessingUiState.Idle
                        }
                        is ProcessingState.Loading -> {
                            _uiState.value = VideoProcessingUiState.Loading(state.message)
                        }
                        is ProcessingState.Extracting -> {
                            _uiState.value = VideoProcessingUiState.Processing(
                                stage = "Extracting frames",
                                progress = state.progress,
                                total = state.totalFrames
                            )
                        }
                        is ProcessingState.Transferring -> {
                            _uiState.value = VideoProcessingUiState.Processing(
                                stage = "Applying style",
                                progress = state.progress,
                                total = state.totalFrames
                            )
                        }
                        is ProcessingState.Reconstructing -> {
                            _uiState.value = VideoProcessingUiState.Processing(
                                stage = "Rebuilding video",
                                progress = state.progress,
                                total = 100
                            )
                        }
                        is ProcessingState.Complete -> {
                            _uiState.value = VideoProcessingUiState.Complete(state.outputFile)
                        }
                        is ProcessingState.Error -> {
                            _uiState.value = VideoProcessingUiState.Error(state.message)
                        }
                    }
                }
        }
    }

    /**
     * Cancel ongoing processing
     */
    fun cancelProcessing() {
        videoProcessor.cancelProcessing()
        _uiState.value = VideoProcessingUiState.Idle
    }

    /**
     * Reset to idle state
     */
    fun reset() {
        currentVideoData = null
        _uiState.value = VideoProcessingUiState.Idle
    }

    /**
     * Get model path for style type
     * Models should be placed in assets/models/
     */
    private fun getModelPathForStyle(styleType: StyleType): String {
        return when (styleType) {
            StyleType.CARTOON_GAN -> "models/cartoongan.tflite"
            StyleType.ANIME_GAN -> "models/animegan.tflite"
            StyleType.HAYAO -> "models/hayao.tflite"
            StyleType.SHINKAI -> "models/shinkai.tflite"
            StyleType.PAPRIKA -> "models/paprika.tflite"
            StyleType.CUSTOM -> "models/custom.tflite"
        }
    }
}

/**
 * UI State for video processing screen
 */
sealed class VideoProcessingUiState {
    object Idle : VideoProcessingUiState()
    data class Loading(val message: String) : VideoProcessingUiState()
    data class VideoLoaded(val videoData: VideoData) : VideoProcessingUiState()
    data class Processing(
        val stage: String,
        val progress: Int,
        val total: Int
    ) : VideoProcessingUiState()
    data class Complete(val outputFile: File) : VideoProcessingUiState()
    data class Error(val message: String) : VideoProcessingUiState()
}
