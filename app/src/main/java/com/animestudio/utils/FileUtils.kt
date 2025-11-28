package com.animestudio.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility functions for file operations
 */
object FileUtils {

    /**
     * Create a temporary directory for video processing
     */
    fun createTempDir(context: Context, prefix: String = "video_work"): File {
        val timestamp = System.currentTimeMillis()
        val dir = File(context.cacheDir, "${prefix}_$timestamp")
        dir.mkdirs()
        return dir
    }

    /**
     * Create output file with timestamp
     */
    fun createOutputFile(context: Context, prefix: String = "styled_video"): File {
        val outputDir = File(context.getExternalFilesDir(null), "outputs")
        outputDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(outputDir, "${prefix}_$timestamp.mp4")
    }

    /**
     * Get human-readable file size
     */
    fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$bytes bytes"
        }
    }

    /**
     * Clean up old cache files
     */
    fun cleanupOldCache(context: Context, olderThanHours: Int = 24) {
        val cutoffTime = System.currentTimeMillis() - (olderThanHours * 60 * 60 * 1000)

        context.cacheDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                file.deleteRecursively()
            }
        }
    }

    /**
     * Get available storage space in bytes
     */
    fun getAvailableSpace(context: Context): Long {
        return context.cacheDir.usableSpace
    }

    /**
     * Format duration in milliseconds to readable string
     */
    fun formatDuration(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        val hours = (milliseconds / (1000 * 60 * 60))

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}
