package com.animestudio.utils

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Debug
import kotlin.math.roundToInt

/**
 * Performance optimization and memory management utility
 * Provides methods for:
 * - Memory threshold detection
 * - Bitmap scaling optimization
 * - Cache management
 * - Performance metrics
 */
object PerformanceOptimizer {
    private const val TAG = "Performance"

    // Memory thresholds (in MB)
    private const val CRITICAL_MEMORY_THRESHOLD = 50
    private const val WARNING_MEMORY_THRESHOLD = 100
    private const val OPTIMAL_MEMORY_THRESHOLD = 200

    /**
     * Get current memory usage
     */
    fun getMemoryUsageMB(): Long {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024
    }

    /**
     * Get available memory
     */
    fun getAvailableMemoryMB(): Long {
        return Runtime.getRuntime().maxMemory() / 1024 / 1024 - getMemoryUsageMB()
    }

    /**
     * Get memory status
     */
    fun getMemoryStatus(context: Context?): MemoryStatus {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        val usedMemory = totalMemory - freeMemory

        return MemoryStatus(
            usedMemory = usedMemory,
            maxMemory = maxMemory,
            availableMemory = maxMemory - usedMemory,
            percentUsed = ((usedMemory.toDouble() / maxMemory) * 100).roundToInt()
        )
    }

    /**
     * Check if memory is critical
     */
    fun isMemoryCritical(context: Context): Boolean {
        val status = getMemoryStatus(context)
        return status.availableMemory < CRITICAL_MEMORY_THRESHOLD
    }

    /**
     * Check if memory is low
     */
    fun isMemoryLow(context: Context): Boolean {
        val status = getMemoryStatus(context)
        return status.availableMemory < WARNING_MEMORY_THRESHOLD
    }

    /**
     * Suggest optimal batch size for frame processing
     */
    fun suggestOptimalBatchSize(context: Context, frameSizeMB: Float): Int {
        val availableMemory = getAvailableMemoryMB()
        val reservedMemory = 100 // Reserve 100MB for system
        val usableMemory = (availableMemory - reservedMemory).coerceAtLeast(50).toFloat()

        return (usableMemory / frameSizeMB).toInt().coerceAtLeast(1)
    }

    /**
     * Calculate optimal resize dimensions while maintaining aspect ratio
     */
    fun calculateOptimalDimensions(
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Pair<Int, Int> {
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return Pair(originalWidth, originalHeight)
        }

        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        val maxAspectRatio = maxWidth.toFloat() / maxHeight.toFloat()

        return if (aspectRatio > maxAspectRatio) {
            // Width is the limiting factor
            val newWidth = maxWidth
            val newHeight = (newWidth / aspectRatio).toInt()
            Pair(newWidth, newHeight)
        } else {
            // Height is the limiting factor
            val newHeight = maxHeight
            val newWidth = (newHeight * aspectRatio).toInt()
            Pair(newWidth, newHeight)
        }
    }

    /**
     * Estimate bitmap memory size
     */
    fun estimateBitmapSize(width: Int, height: Int): Float {
        // ARGB_8888 = 4 bytes per pixel
        return (width * height * 4) / (1024f * 1024f)
    }

    /**
     * Get native heap size
     */
    fun getNativeHeapSizeMB(): Long {
        return Debug.getNativeHeapAllocatedSize() / 1024 / 1024
    }

    /**
     * Optimize bitmap for memory
     */
    fun optimizeBitmapForMemory(bitmap: Bitmap): Bitmap {
        val status = getMemoryStatus(null)

        // If memory is low, reduce quality
        return if (status.percentUsed > 80) {
            val (optimalWidth, optimalHeight) = calculateOptimalDimensions(
                bitmap.width,
                bitmap.height,
                bitmap.width / 2,
                bitmap.height / 2
            )
            Bitmap.createScaledBitmap(bitmap, optimalWidth, optimalHeight, true)
        } else {
            bitmap
        }
    }

    /**
     * Memory status data class
     */
    data class MemoryStatus(
        val usedMemory: Long,
        val maxMemory: Long,
        val availableMemory: Long,
        val percentUsed: Int
    ) {
        override fun toString(): String {
            return "Memory: ${usedMemory}MB / ${maxMemory}MB (${percentUsed}% used)"
        }
    }
}
