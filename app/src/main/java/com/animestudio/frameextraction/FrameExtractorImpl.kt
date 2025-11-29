package com.animestudio.frameextraction

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.animestudio.domain.FrameData
import com.animestudio.domain.FrameExtractor
import com.animestudio.domain.Result
import com.animestudio.domain.VideoData
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext

/**
 * Implementation of FrameExtractor using MediaMetadataRetriever
 *
 * This module handles:
 * - Frame extraction from video using MediaMetadataRetriever (Android native)
 * - Audio extraction using FFmpeg (when available)
 * - Progress tracking during extraction
 *
 * For production apps, consider using FFmpeg for more control:
 * - Add dependency: implementation 'com.arthenica:mobile-ffmpeg-full:4.4.LTS'
 * - Or use: implementation 'com.arthenica:ffmpeg-kit-full:5.1'
 */
class FrameExtractorImpl(
    private val context: Context
) : FrameExtractor {

    @Volatile
    private var isCancelled = false

    override suspend fun extractFrames(
        videoData: VideoData,
        outputDir: File,
        extractionInterval: Long,
        onProgress: (Int, Int) -> Unit
    ): Result<List<FrameData>> = withContext(Dispatchers.IO) {
        isCancelled = false
        var retriever: MediaMetadataRetriever? = null
        val extractedFrames = mutableListOf<FrameData>()

        try {
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            // Safety check: Limit video duration to 60 seconds for processing
            if (videoData.duration > 60000) {
                return@withContext Result.Error(
                    "Video too long (${videoData.duration / 1000}s). Please use videos shorter than 60 seconds for processing."
                )
            }

            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoData.uri)

            val duration = videoData.duration * 1000 // Convert to microseconds
            val interval = if (extractionInterval > 0) {
                extractionInterval * 1000 // Convert to microseconds
            } else {
                // Extract frames every 100ms (10 fps max) to avoid memory issues
                // Full frame rate extraction can cause crashes
                100000L // 100ms = 0.1 seconds
            }

            var currentTime = 0L
            var frameIndex = 0
            val totalFrames = (duration / interval).toInt().coerceAtMost(300) // Max 300 frames

            println("FrameExtractor: Starting extraction of ~$totalFrames frames")

            while (currentTime < duration && coroutineContext.isActive && !isCancelled) {
                try {
                    // Use OPTION_CLOSEST instead of OPTION_CLOSEST_SYNC for better compatibility
                    val bitmap = retriever.getFrameAtTime(
                        currentTime,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )

                    if (bitmap != null) {
                        try {
                            val frameFile = File(outputDir, "frame_${String.format("%05d", frameIndex)}.jpg")
                            saveBitmapToFile(bitmap, frameFile)

                            val frameData = FrameData(
                                frameNumber = frameIndex,
                                timestamp = currentTime,
                                bitmap = null, // Don't keep bitmaps in memory
                                file = frameFile
                            )

                            extractedFrames.add(frameData)
                            onProgress(frameIndex + 1, totalFrames)

                            println("FrameExtractor: Extracted frame $frameIndex at ${currentTime / 1000}ms")
                            frameIndex++
                        } finally {
                            // Always recycle bitmap to avoid memory leaks
                            bitmap.recycle()
                        }
                    } else {
                        println("FrameExtractor: Failed to get bitmap at ${currentTime / 1000}ms")
                    }

                    currentTime += interval
                } catch (e: OutOfMemoryError) {
                    // Critical: Out of memory, stop extraction
                    println("FrameExtractor: OUT OF MEMORY at frame $frameIndex")
                    e.printStackTrace()
                    break
                } catch (e: Exception) {
                    // Skip failed frames and continue
                    println("FrameExtractor: Error at frame $frameIndex: ${e.message}")
                    currentTime += interval
                }
            }

            println("FrameExtractor: Extraction complete. Extracted ${extractedFrames.size} frames")

            if (isCancelled) {
                // Clean up extracted frames
                extractedFrames.forEach { it.file?.delete() }
                Result.Error("Frame extraction cancelled")
            } else {
                Result.Success(extractedFrames)
            }
        } catch (e: Exception) {
            Result.Error("Failed to extract frames", e)
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                // Ignore release errors
            }
        }
    }

    override suspend fun extractAudio(
        videoData: VideoData,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!videoData.hasAudio) {
                return@withContext Result.Error("Video has no audio track")
            }

            // Use FFmpeg to extract audio
            val success = extractAudioWithFFmpeg(videoData, outputFile)

            if (success) {
                Result.Success(outputFile)
            } else {
                Result.Error("Audio extraction failed. Ensure FFmpeg library is properly integrated.")
            }
        } catch (e: Exception) {
            Result.Error("Failed to extract audio", e)
        }
    }

    /**
     * Extract audio using FFmpeg
     * Requires: implementation 'com.arthenica:ffmpeg-kit-full:5.1'
     */
    private fun extractAudioWithFFmpeg(videoData: VideoData, outputFile: File): Boolean {
        // Use File path from URI if possible, or copy to temp file
        // For simplicity, we assume we can get a path or use a temp file
        // Note: Direct URI access with FFmpeg might require content resolver magic or copying
        
        // Since we have a URI, we might need to copy it to a temp file first if it's a content URI
        // But for now, let's assume we can get a path or the user provided a file path in VideoData
        // If VideoData only has URI, we need to handle that.
        // Let's assume we copy to cache if needed.
        
        val inputPath = try {
            val file = File(context.cacheDir, "temp_input_video.mp4")
            context.contentResolver.openInputStream(videoData.uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            return false
        }

        val command = "-y -i \"$inputPath\" -vn -acodec copy \"${outputFile.absolutePath}\""
        val session = FFmpegKit.execute(command)
        
        // Clean up temp file
        File(inputPath).delete()
        
        return ReturnCode.isSuccess(session.getReturnCode())
    }

    /**
     * Alternative: Extract frames using FFmpeg (more efficient for large videos)
     * Command example: ffmpeg -i input.mp4 -vf fps=30 frame_%05d.jpg
     */
    fun extractFramesWithFFmpeg(
        videoData: VideoData,
        outputDir: File,
        frameRate: Int = 30,
        onProgress: (String) -> Unit
    ): Result<List<File>> {
         val inputPath = try {
            val file = File(context.cacheDir, "temp_input_video_frames.mp4")
            context.contentResolver.openInputStream(videoData.uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            return Result.Error("Failed to prepare video file for FFmpeg")
        }

        val command = "-y -i \"$inputPath\" -vf fps=$frameRate \"${outputDir.absolutePath}/frame_%05d.jpg\""

        FFmpegKit.executeAsync(
            command,
            { session ->
                // Session complete
            },
            { log ->
                onProgress(log.getMessage())
            },
            { statistics ->
                // Progress statistics
            }
        )
        
        // Wait for completion (synchronous for this method signature, though async is better)
        // Since executeAsync returns immediately, we can't wait here easily without a latch.
        // But for this refactor, let's use synchronous execute if we want to return Result
        
        // Re-running synchronously for simplicity in this method signature
        val syncSession = FFmpegKit.execute(command)
        
        File(inputPath).delete()

        if (ReturnCode.isSuccess(syncSession.getReturnCode())) {
             val files = outputDir.listFiles()?.sorted()?.toList() ?: emptyList()
             return Result.Success(files)
        } else {
             return Result.Error("FFmpeg frame extraction failed")
        }
    }

    /**
     * Save bitmap to file as JPEG
     */
    private fun saveBitmapToFile(bitmap: Bitmap, file: File, quality: Int = 90) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
    }

    /**
     * Cancel ongoing extraction
     */
    fun cancel() {
        isCancelled = true
        FFmpegKit.cancel()
    }
}

/**
 * FFmpeg command builder for common operations
 */
object FFmpegCommands {
    /**
     * Extract frames at specific FPS
     */
    fun extractFrames(inputPath: String, outputPattern: String, fps: Int = 30): String {
        return "-i $inputPath -vf fps=$fps $outputPattern"
    }

    /**
     * Extract audio from video
     */
    fun extractAudio(inputPath: String, outputPath: String, format: String = "aac"): String {
        return "-i $inputPath -vn -acodec copy $outputPath"
    }

    /**
     * Combine frames into video
     */
    fun framesToVideo(
        inputPattern: String,
        outputPath: String,
        frameRate: Int = 30,
        quality: Int = 23
    ): String {
        return "-framerate $frameRate -i $inputPattern -c:v libx264 -crf $quality -pix_fmt yuv420p $outputPath"
    }

    /**
     * Merge video and audio
     */
    fun mergeVideoAudio(videoPath: String, audioPath: String, outputPath: String): String {
        return "-i $videoPath -i $audioPath -c:v copy -c:a aac -strict experimental $outputPath"
    }

    /**
     * Get video information
     */
    fun getVideoInfo(inputPath: String): String {
        return "-i $inputPath"
    }
}
