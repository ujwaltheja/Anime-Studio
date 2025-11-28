package com.animestudio.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.animestudio.domain.Result
import com.animestudio.domain.VideoData
import com.animestudio.domain.VideoInputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Implementation of VideoInputManager for handling video input operations
 *
 * This module handles:
 * - Video selection from gallery
 * - Video recording from camera
 * - Metadata extraction
 */
class VideoInputManagerImpl(
    private val context: Context
) : VideoInputManager {

    private var currentVideoUri: Uri? = null

    override suspend fun selectFromGallery(): Result<VideoData> = withContext(Dispatchers.IO) {
        try {
            // Note: Actual gallery selection would be triggered via Activity Result API
            // This is a placeholder for the processing logic after URI is received
            currentVideoUri?.let { uri ->
                getVideoMetadata(uri)
            } ?: Result.Error("No video selected")
        } catch (e: Exception) {
            Result.Error("Failed to select video from gallery", e)
        }
    }

    override suspend fun recordVideo(): Result<VideoData> = withContext(Dispatchers.IO) {
        try {
            // Note: Actual video recording would be triggered via Activity Result API
            // This is a placeholder for the processing logic after recording is complete
            currentVideoUri?.let { uri ->
                getVideoMetadata(uri)
            } ?: Result.Error("No video recorded")
        } catch (e: Exception) {
            Result.Error("Failed to record video", e)
        }
    }

    override suspend fun getVideoMetadata(uri: Uri): Result<VideoData> = withContext(Dispatchers.IO) {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)?.toIntOrNull() == 1

            // Try to get frame rate
            val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull() ?: 30f

            // Get file path if available
            val file = getFileFromUri(uri)

            val videoData = VideoData(
                uri = uri,
                file = file,
                duration = duration,
                width = width,
                height = height,
                frameRate = frameRate,
                hasAudio = hasAudio
            )

            Result.Success(videoData)
        } catch (e: Exception) {
            Result.Error("Failed to extract video metadata", e)
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                // Ignore release errors
            }
        }
    }

    /**
     * Set the current video URI (called from Activity after selection/recording)
     */
    fun setCurrentVideoUri(uri: Uri) {
        currentVideoUri = uri
    }

    /**
     * Attempt to get a File object from URI
     */
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            when (uri.scheme) {
                "file" -> uri.path?.let { File(it) }
                "content" -> {
                    // Try to get real path from content URI
                    val projection = arrayOf(MediaStore.Video.Media.DATA)
                    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                        if (cursor.moveToFirst()) {
                            File(cursor.getString(columnIndex))
                        } else null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Copy URI content to a temporary file for processing
     */
    suspend fun copyToTempFile(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Result.Success(tempFile)
        } catch (e: Exception) {
            Result.Error("Failed to copy video to temp file", e)
        }
    }
}

/**
 * Helper class for managing Activity Result Contracts for video operations
 * Use this in your Activity/Fragment
 */
class VideoInputHelper {
    companion object {
        const val REQUEST_VIDEO_PICK = "video/*"
        const val REQUEST_VIDEO_CAPTURE = android.provider.MediaStore.ACTION_VIDEO_CAPTURE
    }
}
