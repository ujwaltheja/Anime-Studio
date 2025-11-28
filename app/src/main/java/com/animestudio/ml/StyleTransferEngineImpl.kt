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

                // Enable GPU acceleration if requested
                if (styleConfig.useGPU) {
                    gpuDelegate = GpuDelegate()
                    addDelegate(gpuDelegate)
                }

                // Use NNAPI if available (Android 8.1+)
                setUseNNAPI(true)
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
                ?: return@withContext Result.Error("Model not initialized")

            // Load frame bitmap
            val inputBitmap = frame.file?.let { BitmapFactory.decodeFile(it.absolutePath) }
                ?: frame.bitmap
                ?: return@withContext Result.Error("No bitmap data available")

            // Preprocess: resize to model input size
            val resizedBitmap = Bitmap.createScaledBitmap(inputBitmap, inputWidth, inputHeight, true)

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
            val finalBitmap = if (inputBitmap.width != inputWidth || inputBitmap.height != inputHeight) {
                Bitmap.createScaledBitmap(outputBitmap, inputBitmap.width, inputBitmap.height, true)
            } else {
                outputBitmap
            }

            // Save to file
            val outputFile = File(
                frame.file?.parentFile,
                "styled_${frame.file?.name ?: "frame_${frame.index}.jpg"}"
            )

            saveBitmapToFile(finalBitmap, outputFile, currentConfig?.outputQuality ?: 90)

            // Clean up
            if (resizedBitmap != inputBitmap) resizedBitmap.recycle()
            outputBitmap.recycle()
            if (frame.bitmap == null) inputBitmap.recycle()

            Result.Success(frame.copy(file = outputFile))
        } catch (e: Exception) {
            Result.Error("Failed to transfer style for frame ${frame.index}", e)
        }
    }

    override suspend fun transferStyleBatch(
        frames: List<FrameData>,
        onProgress: (Int, Int) -> Unit
    ): Result<List<FrameData>> = withContext(Dispatchers.IO) {
        isCancelled = false
        val styledFrames = mutableListOf<FrameData>()

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
                        // Log error but continue with other frames
                        println("Error processing frame ${frame.index}: ${result.message}")
                    }
                    else -> {}
                }
            }

            Result.Success(styledFrames)
        } catch (e: Exception) {
            Result.Error("Batch processing failed", e)
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
                val assetFileDescriptor = context.assets.openFd(modelPath)
                val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
                val fileChannel = inputStream.channel
                val startOffset = assetFileDescriptor.startOffset
                val declaredLength = assetFileDescriptor.declaredLength
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            }

            // Try loading from file system
            val file = File(modelPath)
            if (file.exists()) {
                val inputStream = FileInputStream(file)
                val fileChannel = inputStream.channel
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert bitmap to ByteBuffer for model input
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * pixelSize).apply {
            order(ByteOrder.nativeOrder())
        }

        val intValues = IntArray(inputWidth * inputHeight)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until inputHeight) {
            for (j in 0 until inputWidth) {
                val value = intValues[pixel++]

                // Normalize to [-1, 1] or [0, 1] depending on model
                byteBuffer.putFloat(((value shr 16 and 0xFF) - imageMean) / imageStdDev)
                byteBuffer.putFloat(((value shr 8 and 0xFF) - imageMean) / imageStdDev)
                byteBuffer.putFloat(((value and 0xFF) - imageMean) / imageStdDev)
            }
        }

        return byteBuffer
    }

    /**
     * Convert ByteBuffer to Bitmap for model output
     */
    private fun byteBufferToBitmap(byteBuffer: ByteBuffer, width: Int, height: Int): Bitmap {
        byteBuffer.rewind()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        for (i in pixels.indices) {
            val r = ((byteBuffer.float * imageStdDev + imageMean).toInt()).coerceIn(0, 255)
            val g = ((byteBuffer.float * imageStdDev + imageMean).toInt()).coerceIn(0, 255)
            val b = ((byteBuffer.float * imageStdDev + imageMean).toInt()).coerceIn(0, 255)

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
