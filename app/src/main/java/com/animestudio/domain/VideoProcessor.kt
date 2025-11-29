package com.animestudio.domain

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Main interface for the video processing pipeline
 */
interface VideoProcessor {
    /**
     * Process a video with the specified style
     * @return Flow emitting processing states
     */
    fun processVideo(
        videoData: VideoData,
        styleConfig: StyleConfig,
        outputFile: File
    ): Flow<ProcessingState>

    /**
     * Cancel ongoing processing
     */
    fun cancelProcessing()
}

/**
 * Interface for video input operations
 */
interface VideoInputManager {
    /**
     * Select video from gallery
     */
    suspend fun selectFromGallery(): Result<VideoData>

    /**
     * Record video from camera
     */
    suspend fun recordVideo(): Result<VideoData>

    /**
     * Get video metadata
     */
    suspend fun getVideoMetadata(uri: android.net.Uri): Result<VideoData>

    /**
     * Set current video URI
     */
    fun setCurrentVideoUri(uri: android.net.Uri)

    /**
     * Get current video URI
     */
    fun getCurrentVideoUri(): android.net.Uri?

    /**
     * Validate video
     */
    suspend fun validateVideo(uri: android.net.Uri): Result<Boolean>
}

/**
 * Interface for frame extraction operations
 */
interface FrameExtractor {
    /**
     * Extract all frames from video
     * @param extractionInterval Extract frame every N milliseconds (default: every frame)
     */
    suspend fun extractFrames(
        videoData: VideoData,
        outputDir: File,
        extractionInterval: Long = 0L,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<List<FrameData>>

    /**
     * Extract audio from video
     */
    suspend fun extractAudio(
        videoData: VideoData,
        outputFile: File
    ): Result<File>
}

/**
 * Interface for ML style transfer operations
 */
interface StyleTransferEngine {
    /**
     * Initialize the ML model
     */
    suspend fun initialize(styleConfig: StyleConfig): Result<Unit>

    /**
     * Apply style transfer to a single frame
     */
    suspend fun transferStyle(frame: FrameData): Result<FrameData>

    /**
     * Process a single frame with progress callback
     */
    suspend fun processFrame(
        frame: FrameData,
        onProgress: (Float) -> Unit = {}
    ): Result<FrameData>

    /**
     * Batch process frames
     */
    suspend fun transferStyleBatch(
        frames: List<FrameData>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<List<FrameData>>

    /**
     * Process multiple frames
     */
    suspend fun processFrames(
        frames: List<FrameData>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<List<FrameData>>

    /**
     * Apply style to bitmap
     */
    suspend fun applyStyle(bitmap: android.graphics.Bitmap): Result<android.graphics.Bitmap>

    /**
     * Release model resources
     */
    fun release()
}

/**
 * Interface for video reconstruction operations
 */
interface VideoReconstructor {
    /**
     * Rebuild video from processed frames
     */
    suspend fun reconstructVideo(
        frames: List<FrameData>,
        audioFile: File?,
        outputFile: File,
        frameRate: Float = 30f,
        onProgress: (Int) -> Unit = {}
    ): Result<File>

    /**
     * Merge video and audio
     */
    suspend fun mergeAudioVideo(
        videoFile: File,
        audioFile: File,
        outputFile: File
    ): Result<File>
}
