package com.animestudio.data

import android.content.Context
import com.animestudio.domain.*
import com.animestudio.frameextraction.FrameExtractorImpl
import com.animestudio.ml.StyleTransferEngineImpl
import com.animestudio.videoreconstruction.VideoReconstructorImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.io.File

/**
 * Main implementation of VideoProcessor
 * Orchestrates the entire video processing pipeline
 */
class VideoProcessorImpl(
    private val context: Context,
    private val frameExtractor: FrameExtractor,
    private val styleTransferEngine: StyleTransferEngine,
    private val videoReconstructor: VideoReconstructor
) : VideoProcessor {

    @Volatile
    private var isCancelled = false

    override fun processVideo(
        videoData: VideoData,
        styleConfig: StyleConfig,
        outputFile: File
    ): Flow<ProcessingState> = channelFlow {
        isCancelled = false

        try {
            // Create temporary directories
            val workDir = File(context.cacheDir, "video_processing_${System.currentTimeMillis()}")
            val framesDir = File(workDir, "frames")
            val styledFramesDir = File(workDir, "styled_frames")
            framesDir.mkdirs()
            styledFramesDir.mkdirs()

            send(ProcessingState.Loading("Initializing ML model..."))

            // 1. Initialize ML model
            when (val initResult = styleTransferEngine.initialize(styleConfig)) {
                is Result.Error -> {
                    send(ProcessingState.Error(initResult.message, initResult.exception))
                    return@channelFlow
                }
                else -> {}
            }

            if (checkCancelled()) {
                cleanup(workDir)
                send(ProcessingState.Error("Processing cancelled"))
                return@channelFlow
            }

            send(ProcessingState.Loading("Extracting frames..."))

            // 2. Extract frames
            val framesResult = frameExtractor.extractFrames(
                videoData = videoData,
                outputDir = framesDir,
                extractionInterval = 0L, // Extract all frames
                onProgress = { current, total ->
                    if (!isCancelled) {
                        trySend(ProcessingState.Extracting(current, total))
                    }
                }
            )

            val frames = when (framesResult) {
                is Result.Success -> framesResult.data
                is Result.Error -> {
                    cleanup(workDir)
                    send(ProcessingState.Error(framesResult.message, framesResult.exception))
                    return@channelFlow
                }
                else -> {
                    cleanup(workDir)
                    send(ProcessingState.Error("Unknown error during frame extraction"))
                    return@channelFlow
                }
            }

            if (checkCancelled()) {
                cleanup(workDir)
                send(ProcessingState.Error("Processing cancelled"))
                return@channelFlow
            }

            send(ProcessingState.Loading("Applying style transfer..."))

            // 3. Apply style transfer to all frames
            val styledFramesResult = styleTransferEngine.transferStyleBatch(
                frames = frames,
                onProgress = { current, total ->
                    if (!isCancelled) {
                        trySend(ProcessingState.Transferring(current, total))
                    }
                }
            )

            val styledFrames = when (styledFramesResult) {
                is Result.Success -> styledFramesResult.data
                is Result.Error -> {
                    cleanup(workDir)
                    send(ProcessingState.Error(styledFramesResult.message, styledFramesResult.exception))
                    return@channelFlow
                }
                else -> {
                    cleanup(workDir)
                    send(ProcessingState.Error("Unknown error during style transfer"))
                    return@channelFlow
                }
            }

            if (checkCancelled()) {
                cleanup(workDir)
                send(ProcessingState.Error("Processing cancelled"))
                return@channelFlow
            }

            send(ProcessingState.Loading("Extracting audio..."))

            // 4. Extract audio if video has audio
            var audioFile: File? = null
            if (videoData.hasAudio) {
                val audioOutputFile = File(workDir, "audio.aac")
                when (val audioResult = frameExtractor.extractAudio(videoData, audioOutputFile)) {
                    is Result.Success -> audioFile = audioResult.data
                    is Result.Error -> {
                        // Continue without audio if extraction fails
                        println("Warning: Audio extraction failed: ${audioResult.message}")
                    }
                    else -> {}
                }
            }

            if (checkCancelled()) {
                cleanup(workDir)
                send(ProcessingState.Error("Processing cancelled"))
                return@channelFlow
            }

            send(ProcessingState.Loading("Reconstructing video..."))

            // 5. Reconstruct video from styled frames
            val reconstructResult = videoReconstructor.reconstructVideo(
                frames = styledFrames,
                audioFile = audioFile,
                outputFile = outputFile,
                frameRate = videoData.frameRate,
                onProgress = { progress ->
                    if (!isCancelled) {
                        trySend(ProcessingState.Reconstructing(progress))
                    }
                }
            )

            when (reconstructResult) {
                is Result.Success -> {
                    cleanup(workDir)
                    send(ProcessingState.Complete(reconstructResult.data))
                }
                is Result.Error -> {
                    cleanup(workDir)
                    send(ProcessingState.Error(reconstructResult.message, reconstructResult.exception))
                }
                else -> {
                    cleanup(workDir)
                    send(ProcessingState.Error("Unknown error during video reconstruction"))
                }
            }

        } catch (e: Throwable) {
            e.printStackTrace()
            send(ProcessingState.Error("Unexpected error during processing: ${e.message}", e))
        } finally {
            styleTransferEngine.release()
        }
    }

    override fun cancelProcessing() {
        isCancelled = true

        // Cancel individual processors if they support it
        (frameExtractor as? FrameExtractorImpl)?.cancel()
        (styleTransferEngine as? StyleTransferEngineImpl)?.cancel()
        (videoReconstructor as? VideoReconstructorImpl)?.cancel()
    }

    private fun checkCancelled(): Boolean = isCancelled

    private fun cleanup(workDir: File) {
        try {
            workDir.deleteRecursively()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}

/**
 * Factory for creating VideoProcessor instances
 */
object VideoProcessorFactory {
    fun create(context: Context): VideoProcessor {
        val frameExtractor = FrameExtractorImpl(context)
        val styleTransferEngine = StyleTransferEngineImpl(context)
        val videoReconstructor = VideoReconstructorImpl(context)

        return VideoProcessorImpl(
            context = context,
            frameExtractor = frameExtractor,
            styleTransferEngine = styleTransferEngine,
            videoReconstructor = videoReconstructor
        )
    }
}
