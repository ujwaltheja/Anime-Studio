package com.animestudio.frameextraction

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.animestudio.domain.FrameData
import com.animestudio.domain.FrameExtractor
import com.animestudio.domain.Result
import com.animestudio.domain.VideoData
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

            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoData.uri)

            val duration = videoData.duration * 1000 // Convert to microseconds
            val interval = if (extractionInterval > 0) {
                extractionInterval * 1000 // Convert to microseconds
            } else {
                // Extract all frames based on frame rate
                (1000000f / videoData.frameRate).toLong()
            }

            var currentTime = 0L
            var frameIndex = 0
            val totalFrames = (duration / interval).toInt()

            while (currentTime < duration && coroutineContext.isActive && !isCancelled) {
                try {
                    val bitmap = retriever.getFrameAtTime(
                        currentTime,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )

                    if (bitmap != null) {
                        val frameFile = File(outputDir, "frame_${String.format("%05d", frameIndex)}.jpg")
                        saveBitmapToFile(bitmap, frameFile)

                        val frameData = FrameData(
                            index = frameIndex,
                            timestamp = currentTime,
                            bitmap = null, // Don't keep bitmaps in memory
                            file = frameFile
                        )

                        extractedFrames.add(frameData)
                        onProgress(frameIndex + 1, totalFrames)

                        bitmap.recycle()
                    }

                    frameIndex++
                    currentTime += interval
                } catch (e: Exception) {
                    // Skip failed frames
                    currentTime += interval
                }
            }

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
            // Note: This requires FFmpeg library integration
            // For placeholder, we'll return an error with instructions

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
        // Placeholder for FFmpeg integration
        // Actual implementation would use:
        /*
        val command = "-i ${videoData.file?.absolutePath} -vn -acodec copy ${outputFile.absolutePath}"
        val session = FFmpegKit.execute(command)
        return ReturnCode.isSuccess(session.returnCode)
        */

        // For now, return false to indicate FFmpeg is not integrated
        return false
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
        // Placeholder for FFmpeg implementation
        /*
        val command = "-i ${videoData.file?.absolutePath} -vf fps=$frameRate ${outputDir.absolutePath}/frame_%05d.jpg"

        val session = FFmpegKit.executeAsync(command) { session ->
            if (ReturnCode.isSuccess(session.returnCode)) {
                // Success
            } else {
                // Failed
            }
        } { log ->
            onProgress(log.message)
        } { statistics ->
            // Progress statistics
        }
        */

        return Result.Error("FFmpeg integration required")
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
