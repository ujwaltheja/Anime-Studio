package com.animestudio.domain

import android.net.Uri
import java.io.File

/**
 * Represents video data throughout the processing pipeline
 */
data class VideoData(
    val uri: Uri,
    val file: File? = null,
    val duration: Long = 0L, // in milliseconds
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = 30f,
    val hasAudio: Boolean = false
)

/**
 * Represents a single extracted frame
 */
data class FrameData(
    val bitmap: android.graphics.Bitmap?,
    val frameNumber: Int,
    val timestamp: Long = 0L, // in microseconds
    val file: File? = null
) {
    // Alias for backward compatibility
    val index: Int get() = frameNumber
}

/**
 * Represents style transfer options
 */
enum class StyleType {
    CARTOON_GAN,
    ANIME_GAN,
    HAYAO,
    SHINKAI,
    PAPRIKA,
    STYLE_TRANSFER,
    CUSTOM
}

/**
 * Style transfer configuration
 */
data class StyleConfig(
    val styleType: StyleType,
    val modelPath: String,
    val useGPU: Boolean = false,
    val inputSize: Int = 512,
    val outputQuality: Int = 90
)

/**
 * Processing state for UI updates
 */
sealed class ProcessingState {
    object Idle : ProcessingState()
    data class Loading(val message: String) : ProcessingState()
    data class Extracting(val progress: Int, val totalFrames: Int) : ProcessingState()
    data class Transferring(val progress: Int, val totalFrames: Int) : ProcessingState()
    data class Reconstructing(val progress: Int) : ProcessingState()
    data class Complete(val outputFile: File) : ProcessingState()
    data class Error(val message: String, val exception: Throwable? = null) : ProcessingState()
}

/**
 * Result wrapper for operations
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
