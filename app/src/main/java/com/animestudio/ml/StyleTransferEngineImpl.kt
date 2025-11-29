package com.animestudio.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.animestudio.domain.FrameData
import com.animestudio.domain.Result
import com.animestudio.domain.StyleConfig
import com.animestudio.domain.StyleTransferEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.coroutines.coroutineContext

/**
 * TensorFlow Lite implementation of StyleTransferEngine
 *
 * This module handles:
 * - TensorFlow Lite model loading and initialization
 * - GPU acceleration (optional)
 * - Frame preprocessing and postprocessing
 * - Batch processing with progress tracking
 *
 * Model Requirements:
 * - Place .tflite models in app/src/main/assets/models/
 * - Supported models: CartoonGAN, AnimeGAN, White-box CartoonGAN
 * - Input: RGB image (typically 512x512 or dynamic size)
 * - Output: Stylized RGB image
 *
 * Model Optimization:
 * - Use quantized models for faster inference
 * - Enable GPU delegate for better performance
 * - Consider model pruning for smaller size
 */
class StyleTransferEngineImpl(
    private val context: Context
) : StyleTransferEngine {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var currentConfig: StyleConfig? = null

    private var inputWidth = 512
    private var inputHeight = 512
    private val pixelSize = 3 // RGB
    private val imageStdDev = 127.5f
    private val imageMean = 127.5f

    @Volatile
    private var isCancelled = false

    override suspend fun initialize(styleConfig: StyleConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Release previous resources
            release()

            currentConfig = styleConfig

            // Load model file
            val modelBuffer = loadModelFile(styleConfig.modelPath)
                ?: return@withContext Result.Error("Failed to load model file: ${styleConfig.modelPath}")

            // Configure interpreter options
            val options = Interpreter.Options().apply {
                setNumThreads(4)

                // Enable GPU acceleration if requested (with fallback)
                if (styleConfig.useGPU) {
                    try {
                        gpuDelegate = GpuDelegate()
                        addDelegate(gpuDelegate)
                        Logger.i("StyleTransferEngine", "GPU acceleration enabled")
                    } catch (e: Exception) {
                        // GPU not available, continue without it
                        Logger.w("StyleTransferEngine", "GPU delegate not available: ${e.message}. Will use CPU instead.")
                    }
                }

                // Use NNAPI if available (Android 8.1+) - with fallback
                try {
                    setUseNNAPI(true)
                    Logger.i("StyleTransferEngine", "NNAPI enabled")
                } catch (e: Exception) {
                    // NNAPI not available, continue without it
                    Logger.w("StyleTransferEngine", "NNAPI not available: ${e.message}. Will use default inference.")
            }

            // Create interpreter
            interpreter = Interpreter(modelBuffer, options)

            // Get input/output tensor dimensions
            val inputShape = interpreter?.getInputTensor(0)?.shape()
            if (inputShape != null && inputShape.size >= 3) {
                inputHeight = inputShape[1]
                inputWidth = inputShape[2]
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            release()
            Result.Error("Failed to initialize style transfer engine", e)
        }
    }

    override suspend fun transferStyle(frame: FrameData): Result<FrameData> = withContext(Dispatchers.IO) {
        try {
            val interpreter = this@StyleTransferEngineImpl.interpreter
                ?: return@withContext Result.Error("Model not initialized. Please ensure the style transfer engine is properly initialized before processing frames.")

            // Load frame bitmap
            val inputBitmap = when {
                frame.file?.exists() == true -> {
                    try {
                        BitmapFactory.decodeFile(frame.file.absolutePath)
                            ?: return@withContext Result.Error("Failed to decode bitmap from file: ${frame.file.absolutePath}")
                    } catch (e: Exception) {
                        return@withContext Result.Error("Error loading frame bitmap: ${e.message}", e)
                    }
                }
                frame.bitmap != null -> frame.bitmap
                else -> return@withContext Result.Error("No bitmap data available for frame ${frame.frameNumber}")
            }

            val originalWidth = inputBitmap.width
            val originalHeight = inputBitmap.height

            // Preprocess: resize to model input size
            val resizedBitmap = try {
                Bitmap.createScaledBitmap(inputBitmap, inputWidth, inputHeight, true)
            } catch (e: OutOfMemoryError) {
                if (frame.bitmap == null) inputBitmap.recycle()
                return@withContext Result.Error("Out of memory while resizing frame ${frame.frameNumber}. Try reducing video quality or processing fewer frames.", e)
            }

            // Convert bitmap to ByteBuffer
            val inputBuffer = try {
                bitmapToByteBuffer(resizedBitmap)
            } catch (e: Exception) {
                if (resizedBitmap != inputBitmap) resizedBitmap.recycle()
                if (frame.bitmap == null) inputBitmap.recycle()
                return@withContext Result.Error("Error converting bitmap to buffer for frame ${frame.frameNumber}: ${e.message}", e)
            }

            // Prepare output buffer
            val outputBuffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * pixelSize).apply {
                order(ByteOrder.nativeOrder())
            }

            // Run inference
            try {
                interpreter.run(inputBuffer, outputBuffer)
            } catch (e: Exception) {
                if (resizedBitmap != inputBitmap) resizedBitmap.recycle()
                if (frame.bitmap == null) inputBitmap.recycle()
                return@withContext Result.Error("Model inference failed for frame ${frame.frameNumber}: ${e.message}. The model may be corrupted or incompatible.", e)
            }

            // Convert output buffer to bitmap
            val outputBitmap = try {
                byteBufferToBitmap(outputBuffer, inputWidth, inputHeight)
            } catch (e: Exception) {
                if (resizedBitmap != inputBitmap) resizedBitmap.recycle()
                if (frame.bitmap == null) inputBitmap.recycle()
                return@withContext Result.Error("Error converting output buffer to bitmap for frame ${frame.frameNumber}: ${e.message}", e)
            }

            // Resize back to original dimensions if needed
            val finalBitmap = try {
                if (originalWidth != inputWidth || originalHeight != inputHeight) {
                    Bitmap.createScaledBitmap(outputBitmap, originalWidth, originalHeight, true)
                } else {
                    outputBitmap
                }
            } catch (e: OutOfMemoryError) {
                outputBitmap.recycle()
                if (resizedBitmap != inputBitmap) resizedBitmap.recycle()
                if (frame.bitmap == null) inputBitmap.recycle()
                return@withContext Result.Error("Out of memory while resizing output for frame ${frame.frameNumber}", e)
            }

            // Determine output directory and file name
            val outputDir = frame.file?.parentFile ?: File(context.cacheDir, "styled_frames").apply { mkdirs() }
            val outputFileName = if (frame.file != null) {
                "styled_${frame.file.name}"
            } else {
                "styled_frame_${String.format("%05d", frame.frameNumber)}.jpg"
            }
            val outputFile = File(outputDir, outputFileName)

            // Save to file
            try {
                saveBitmapToFile(finalBitmap, outputFile, currentConfig?.outputQuality ?: 90)
            } catch (e: Exception) {
                finalBitmap.recycle()
                if (outputBitmap != finalBitmap) outputBitmap.recycle()
                if (resizedBitmap != inputBitmap) resizedBitmap.recycle()
                if (frame.bitmap == null) inputBitmap.recycle()
                return@withContext Result.Error("Error saving styled frame ${frame.frameNumber} to file: ${e.message}", e)
            }

            // Verify file was saved
            if (!outputFile.exists() || outputFile.length() == 0L) {
                finalBitmap.recycle()
                if (outputBitmap != finalBitmap) outputBitmap.recycle()
                if (resizedBitmap != inputBitmap) resizedBitmap.recycle()
                if (frame.bitmap == null) inputBitmap.recycle()
                return@withContext Result.Error("Failed to save frame ${frame.frameNumber} - output file is empty or doesn't exist")
            }

            // Clean up bitmaps
            finalBitmap.recycle()
            if (outputBitmap != finalBitmap) outputBitmap.recycle()
            if (resizedBitmap != inputBitmap) resizedBitmap.recycle()
            if (frame.bitmap == null) inputBitmap.recycle()

            // Return success with updated frame data
            Result.Success(
                frame.copy(
                    file = outputFile,
                    bitmap = null // Don't keep bitmap in memory after saving
                )
            )
        } catch (e: OutOfMemoryError) {
            System.gc() // Suggest garbage collection
            Result.Error("Out of memory while processing frame ${frame.frameNumber}. Try closing other apps or reducing video quality.", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error processing frame ${frame.frameNumber}: ${e.message}", e)
        }
    }

    override suspend fun transferStyleBatch(
        frames: List<FrameData>,
        onProgress: (Int, Int) -> Unit
    ): Result<List<FrameData>> = withContext(Dispatchers.IO) {
        isCancelled = false
        val styledFrames = mutableListOf<FrameData>()
        val failedFrames = mutableListOf<Pair<Int, String>>()

        try {
            frames.forEachIndexed { index, frame ->
                if (!coroutineContext.isActive || isCancelled) {
                    return@withContext Result.Error("Batch processing cancelled")
                }

                when (val result = transferStyle(frame)) {
                    is Result.Success -> {
                        styledFrames.add(result.data)
                        onProgress(index + 1, frames.size)
                    }
                    is Result.Error -> {
                        // Track failed frames
                        failedFrames.add(Pair(frame.frameNumber, result.message))
                        Logger.w("StyleTransferEngine", "Error processing frame ${frame.frameNumber}: ${result.message}")
                        // Continue with other frames to maximize success
                    }
                    else -> {}
                }
            }

            // Check if we have results
            if (styledFrames.isEmpty() && failedFrames.isNotEmpty()) {
                val failureReport = failedFrames.take(3).joinToString(", ") { (num, msg) -> "Frame $num: $msg" }
                return@withContext Result.Error("All frames failed processing: $failureReport")
            }

            if (failedFrames.isNotEmpty()) {
                Logger.w("StyleTransferEngine", "Batch processing completed with ${failedFrames.size}/${frames.size} frames failed")
            }

            Result.Success(styledFrames)
        } catch (e: Exception) {
            Result.Error("Batch processing failed: ${e.message}", e)
        }
    }

    override suspend fun processFrame(
        frame: FrameData,
        onProgress: (Float) -> Unit
    ): Result<FrameData> {
        // Delegate to transferStyle with progress reporting
        onProgress(0f)
        val result = transferStyle(frame)
        onProgress(1f)
        return result
    }

    override suspend fun processFrames(
        frames: List<FrameData>,
        onProgress: (Int, Int) -> Unit
    ): Result<List<FrameData>> {
        // Delegate to transferStyleBatch
        return transferStyleBatch(frames, onProgress)
    }

    override suspend fun applyStyle(bitmap: Bitmap): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val interpreter = this@StyleTransferEngineImpl.interpreter
                ?: return@withContext Result.Error("Model not initialized")

            // Preprocess: resize to model input size
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)

            // Convert bitmap to ByteBuffer
            val inputBuffer = bitmapToByteBuffer(resizedBitmap)

            // Prepare output buffer
            val outputBuffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * pixelSize).apply {
                order(ByteOrder.nativeOrder())
            }

            // Run inference
            interpreter.run(inputBuffer, outputBuffer)

            // Convert output buffer to bitmap
            val outputBitmap = byteBufferToBitmap(outputBuffer, inputWidth, inputHeight)

            // Resize back to original dimensions if needed
            val finalBitmap = if (bitmap.width != inputWidth || bitmap.height != inputHeight) {
                Bitmap.createScaledBitmap(outputBitmap, bitmap.width, bitmap.height, true)
            } else {
                outputBitmap
            }

            // Clean up
            if (resizedBitmap != bitmap) resizedBitmap.recycle()
            if (outputBitmap != finalBitmap) outputBitmap.recycle()

            Result.Success(finalBitmap)
        } catch (e: Exception) {
            Result.Error("Failed to apply style to bitmap", e)
        }
    }

    override fun release() {
        interpreter?.close()
        interpreter = null

        gpuDelegate?.close()
        gpuDelegate = null

        currentConfig = null
    }

    fun cancel() {
        isCancelled = true
    }

    /**
     * Load TensorFlow Lite model file
     */
    private fun loadModelFile(modelPath: String): MappedByteBuffer? {
        return try {
            // Try loading from assets first
            if (!modelPath.startsWith("/")) {
                try {
                    val assetFileDescriptor = context.assets.openFd(modelPath)
                    var inputStream: FileInputStream? = null
                    return try {
                        inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
                        val fileChannel = inputStream.channel
                        val startOffset = assetFileDescriptor.startOffset
                        val declaredLength = assetFileDescriptor.declaredLength
                        fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
                    } finally {
                        inputStream?.close()
                    }
                } catch (e: Exception) {
                    Logger.e("StyleTransferEngine", "Failed to load model from assets: ${e.message}", e)
                    throw e
                }
            }

            // Try loading from file system
            val file = File(modelPath)
            if (file.exists()) {
                var inputStream: FileInputStream? = null
                return try {
                    inputStream = FileInputStream(file)
                    val fileChannel = inputStream.channel
                    fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
                } finally {
                    inputStream?.close()
                }
            }

            Logger.w("StyleTransferEngine", "Model file not found: $modelPath")
            null
        } catch (e: Exception) {
            Logger.e("StyleTransferEngine", "Error loading model file: ${e.message}", e)
            null
        }
    }

    /**
     * Convert bitmap to ByteBuffer for model input
     * Fixed: Proper pixel ordering and channel extraction
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * pixelSize).apply {
            order(ByteOrder.nativeOrder())
        }

        val intValues = IntArray(inputWidth * inputHeight)
        bitmap.getPixels(intValues, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        // Process pixels in correct order
        for (pixel in intValues) {
            // Extract RGB channels from ARGB pixel
            // Format: 0xAARRGGBB
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            // Normalize to [-1, 1] range for AnimeGAN models
            // Formula: (pixel_value / 255.0 - 0.5) * 2.0 or (pixel - 127.5) / 127.5
            byteBuffer.putFloat((r - imageMean) / imageStdDev)
            byteBuffer.putFloat((g - imageMean) / imageStdDev)
            byteBuffer.putFloat((b - imageMean) / imageStdDev)
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    /**
     * Convert ByteBuffer to Bitmap for model output
     * Fixed: Proper denormalization and pixel packing
     */
    private fun byteBufferToBitmap(byteBuffer: ByteBuffer, width: Int, height: Int): Bitmap {
        byteBuffer.rewind()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        for (i in pixels.indices) {
            // Read normalized float values from buffer (range [-1, 1])
            val rFloat = byteBuffer.float
            val gFloat = byteBuffer.float
            val bFloat = byteBuffer.float

            // Denormalize from [-1, 1] to [0, 255]
            // Formula: (normalized_value * 127.5 + 127.5)
            val r = (rFloat * imageStdDev + imageMean).toInt().coerceIn(0, 255)
            val g = (gFloat * imageStdDev + imageMean).toInt().coerceIn(0, 255)
            val b = (bFloat * imageStdDev + imageMean).toInt().coerceIn(0, 255)

            // Pack into ARGB format
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Save bitmap to file
     */
    private fun saveBitmapToFile(bitmap: Bitmap, file: File, quality: Int) {
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
    }
}

/**
 * Cloud-based style transfer API client (for hybrid processing)
 * Use this for advanced styles that are too heavy for on-device processing
 */
class CloudStyleTransferClient(
    private val apiEndpoint: String,
    private val apiKey: String
) {
    /**
     * Upload frame to cloud and get styled version
     * Placeholder implementation - integrate with your cloud API
     */
    suspend fun transferStyleCloud(frame: FrameData, styleType: String): Result<FrameData> {
        // Implement API call to cloud service
        // Example: DeepAI, Replicate, or custom backend
        return Result.Error("Cloud API not implemented")
    }
}
