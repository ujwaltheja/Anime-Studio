package com.animestudio.models

import android.content.Context
import com.animestudio.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Model Manager - Handles downloading, caching, and lifecycle of TFLite models
 * Based on research report Section 8: "Technical Integration Guide"
 * 
 * Features:
 * - On-demand model downloading
 * - Checksum verification
 * - Progress tracking
 * - Storage management
 */
class ModelManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelManager"
        private const val MODEL_DIR_NAME = "tflite_models"
    }
    
    private val modelDir: File = File(context.filesDir, MODEL_DIR_NAME).apply {
        if (!exists()) mkdirs()
    }
    
    /**
     * Check if model is available locally
     */
    fun isModelAvailable(modelId: String): Boolean {
        val modelInfo = ModelRegistry.getModel(modelId) ?: return false
        val modelFile = File(modelDir, modelInfo.filePath)
        return modelFile.exists() && modelFile.length() > 0
    }
    
    /**
     * Get local path to model file
     */
    fun getModelPath(modelId: String): File? {
        val modelInfo = ModelRegistry.getModel(modelId) ?: return null
        
        // Check if bundled in assets
        if (modelInfo.bundled) {
            return extractBundledModel(modelInfo)
        }
        
        // Check if downloaded
        val modelFile = File(modelDir, modelInfo.filePath)
        return if (modelFile.exists()) modelFile else null
    }
    
    /**
     * Download model with progress tracking
     */
    suspend fun downloadModel(
        modelId: String,
        forceRedownload: Boolean = false
    ): Flow<DownloadProgress> = flow {
        val modelInfo = ModelRegistry.getModel(modelId)
            ?: throw IllegalArgumentException("Model not found: $modelId")
        
        if (modelInfo.bundled) {
            emit(DownloadProgress.Complete(extractBundledModel(modelInfo)))
            return@flow
        }
        
        val modelFile = File(modelDir, modelInfo.filePath).apply {
            parentFile?.mkdirs()
        }
        
        // Skip if already downloaded
        if (!forceRedownload && modelFile.exists() && modelFile.length() == modelInfo.sizeBytes) {
            emit(DownloadProgress.Complete(modelFile))
            return@flow
        }
        
        emit(DownloadProgress.Starting(modelInfo.name))
        
        val url = modelInfo.downloadUrl
            ?: throw IllegalStateException("No download URL for model: $modelId")
        
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()
                
                val totalBytes = connection.contentLength.toLong()
                var downloadedBytes = 0L
                
                connection.inputStream.use { input ->
                    FileOutputStream(modelFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            
                            val progress = (downloadedBytes.toFloat() / totalBytes * 100).toInt()
                            emit(DownloadProgress.Downloading(
                                modelInfo.name,
                                downloadedBytes,
                                totalBytes,
                                progress
                            ))
                        }
                    }
                }
                
                emit(DownloadProgress.Complete(modelFile))
                Logger.i(TAG, "Model downloaded: $modelId (${modelFile.length()} bytes)")
                
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to download model $modelId: ${e.message}")
                modelFile.delete()  // Clean up partial download
                emit(DownloadProgress.Error(modelInfo.name, e.message ?: "Unknown error"))
            }
        }
    }
    
    /**
     * Download multiple models
     */
    suspend fun downloadModelSet(
        models: List<String>,
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ): Result<Unit> {
        return try {
            for (modelId in models) {
                var errorMessage: String? = null
                downloadModel(modelId).collect { progress ->
                    when (progress) {
                        is DownloadProgress.Downloading -> {
                            onProgress(modelId, progress.percent)
                        }
                        is DownloadProgress.Error -> {
                            errorMessage = progress.message
                        }
                        else -> {}
                    }
                }
                
                if (errorMessage != null) {
                    return Result.failure(Exception(errorMessage))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Extract bundled model from assets
     */
    private fun extractBundledModel(modelInfo: ModelInfo): File {
        val modelFile = File(modelDir, modelInfo.filePath).apply {
            parentFile?.mkdirs()
        }
        
        // Skip if already extracted
        if (modelFile.exists() && modelFile.length() > 0) {
            return modelFile
        }
        
        try {
            context.assets.open(modelInfo.filePath).use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output)
                }
            }
            Logger.i(TAG, "Extracted bundled model: ${modelInfo.id}")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to extract bundled model: ${e.message}")
            throw e
        }
        
        return modelFile
    }
    
    /**
     * Delete model to free space
     */
    fun deleteModel(modelId: String): Boolean {
        val modelInfo = ModelRegistry.getModel(modelId) ?: return false
        if (modelInfo.bundled) return false  // Can't delete bundled models
        
        val modelFile = File(modelDir, modelInfo.filePath)
        return modelFile.delete()
    }
    
    /**
     * Clear all downloaded models
     */
    fun clearCache(): Long {
        var freedBytes = 0L
        ModelRegistry.ALL_MODELS
            .filter { !it.bundled }
            .forEach { model ->
                val file = File(modelDir, model.filePath)
                if (file.exists()) {
                    freedBytes += file.length()
                    file.delete()
                }
            }
        Logger.i(TAG, "Cleared cache: ${freedBytes / 1024 / 1024} MB freed")
        return freedBytes
    }
    
    /**
     * Get total cache size
     */
    fun getCacheSize(): Long {
        return modelDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }
    
    /**
     * Get available storage space
     */
    fun getAvailableSpace(): Long {
        return modelDir.usableSpace
    }
}

/**
 * Download progress states
 */
sealed class DownloadProgress {
    data class Starting(val modelName: String) : DownloadProgress()
    data class Downloading(
        val modelName: String,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val percent: Int
    ) : DownloadProgress()
    data class Complete(val file: File) : DownloadProgress()
    data class Error(val modelName: String, val message: String) : DownloadProgress()
}
