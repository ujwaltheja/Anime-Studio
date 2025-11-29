package com.animestudio.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.animestudio.domain.Result
import com.animestudio.models.ModelManager
import com.animestudio.models.ModelRegistry
import com.animestudio.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Real-ESRGAN Upscaler
 * Implements 4x upscaling using Real-ESRGAN-x4plus-anime model
 * 
 * Features:
 * - 4x Super Resolution
 * - Tiled processing for large images to avoid OOM
 * - GPU acceleration support
 */
class RealESRGANUpscaler(
    private val context: Context,
    private val modelManager: ModelManager
) {

    companion object {
        private const val TAG = "RealESRGANUpscaler"
        private const val TILE_SIZE = 256  // Input tile size
        private const val SCALE_FACTOR = 4
        private const val OUTPUT_TILE_SIZE = TILE_SIZE * SCALE_FACTOR
        private const val PIXEL_SIZE = 3 // RGB
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var isInitialized = false

    /**
     * Initialize the model
     */
    suspend fun initialize(onProgress: (Int) -> Unit = {}): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (isInitialized) {
                    return@withContext Result.Success(Unit)
                }

                Logger.i(TAG, "Initializing Real-ESRGAN Upscaler...")

                // Check/Download model
                if (!modelManager.isModelAvailable(ModelRegistry.REAL_ESRGAN_ANIME.id)) {
                    Logger.i(TAG, "Model not found, downloading...")
                    
                    var downloadError: String? = null
                    modelManager.downloadModel(ModelRegistry.REAL_ESRGAN_ANIME.id)
                        .collect { progress ->
                            when (progress) {
                                is com.animestudio.models.DownloadProgress.Downloading -> {
                                    onProgress(progress.percent)
                                }
                                is com.animestudio.models.DownloadProgress.Error -> {
                                    downloadError = progress.message
                                }
                                else -> {}
                            }
                        }
                    
                    if (downloadError != null) {
                        return@withContext Result.Error(downloadError!!)
                    }
                }

                val modelPath = modelManager.getModelPath(ModelRegistry.REAL_ESRGAN_ANIME.id)
                    ?: return@withContext Result.Error("Model file not found")

                // Initialize Interpreter with GPU if available
                val options = Interpreter.Options()
                
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    try {
                        gpuDelegate = GpuDelegate()
                        options.addDelegate(gpuDelegate)
                        Logger.i(TAG, "GPU Delegate enabled")
                    } catch (e: Exception) {
                        Logger.w(TAG, "Failed to create GPU delegate: ${e.message}")
                    }
                } else {
                    options.setNumThreads(4)
                    Logger.i(TAG, "GPU not supported, using 4 CPU threads")
                }

                interpreter = Interpreter(loadModelFile(modelPath), options)
                isInitialized = true

                Logger.i(TAG, "Real-ESRGAN initialized successfully")
                Result.Success(Unit)

            } catch (e: Exception) {
                Logger.e(TAG, "Failed to initialize: ${e.message}")
                Result.Error("Initialization failed: ${e.message}", e)
            }
        }
    }

    /**
     * Upscale an image by 4x
     */
    suspend fun upscale(input: Bitmap): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) {
                return@withContext Result.Error("Model not initialized")
            }

            val inputWidth = input.width
            val inputHeight = input.height
            val outputWidth = inputWidth * SCALE_FACTOR
            val outputHeight = inputHeight * SCALE_FACTOR

            Logger.i(TAG, "Upscaling image: ${inputWidth}x${inputHeight} -> ${outputWidth}x${outputHeight}")

            val outputBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outputBitmap)

            // Process in tiles
            val tilesX = (inputWidth + TILE_SIZE - 1) / TILE_SIZE
            val tilesY = (inputHeight + TILE_SIZE - 1) / TILE_SIZE

            for (y in 0 until tilesY) {
                for (x in 0 until tilesX) {
                    val startX = x * TILE_SIZE
                    val startY = y * TILE_SIZE
                    val width = minOf(TILE_SIZE, inputWidth - startX)
                    val height = minOf(TILE_SIZE, inputHeight - startY)

                    // Extract tile
                    val tile = Bitmap.createBitmap(input, startX, startY, width, height)
                    
                    // Pad if necessary to match model input size (if model requires fixed size)
                    // For now assuming model can handle variable input or we pad to TILE_SIZE
                    // Real-ESRGAN often works on patches. Let's pad to TILE_SIZE if needed.
                    val processedTile = processTile(tile)
                    
                    // Draw to output
                    val destRect = Rect(
                        startX * SCALE_FACTOR,
                        startY * SCALE_FACTOR,
                        (startX + width) * SCALE_FACTOR,
                        (startY + height) * SCALE_FACTOR
                    )
                    
                    // We need to crop the processed tile if we padded it
                    val srcRect = Rect(0, 0, width * SCALE_FACTOR, height * SCALE_FACTOR)
                    
                    canvas.drawBitmap(processedTile, srcRect, destRect, null)
                    
                    tile.recycle()
                    processedTile.recycle()
                }
            }

            Result.Success(outputBitmap)

        } catch (e: Exception) {
            Logger.e(TAG, "Upscaling failed: ${e.message}")
            Result.Error("Upscaling failed: ${e.message}", e)
        }
    }

    private fun processTile(tile: Bitmap): Bitmap {
        // Prepare input
        // Real-ESRGAN typically expects RGB float input [0, 1]
        // But TFLite models might vary. Let's assume standard float input.
        
        // Resize/Pad to TILE_SIZE if needed for fixed-size model
        // For this implementation, let's assume we pad to TILE_SIZE
        
        val paddedTile = if (tile.width != TILE_SIZE || tile.height != TILE_SIZE) {
            val padded = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(padded)
            canvas.drawBitmap(tile, 0f, 0f, null)
            padded
        } else {
            tile
        }

        val inputBuffer = ByteBuffer.allocateDirect(1 * TILE_SIZE * TILE_SIZE * 3 * 4) // Float32
        inputBuffer.order(ByteOrder.nativeOrder())
        
        val intValues = IntArray(TILE_SIZE * TILE_SIZE)
        paddedTile.getPixels(intValues, 0, TILE_SIZE, 0, 0, TILE_SIZE, TILE_SIZE)
        
        for (pixel in intValues) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)
        }

        // Prepare output
        val outputBuffer = ByteBuffer.allocateDirect(1 * OUTPUT_TILE_SIZE * OUTPUT_TILE_SIZE * 3 * 4)
        outputBuffer.order(ByteOrder.nativeOrder())

        // Run inference
        interpreter!!.run(inputBuffer, outputBuffer)

        // Convert output to bitmap
        outputBuffer.rewind()
        val outputBitmap = Bitmap.createBitmap(OUTPUT_TILE_SIZE, OUTPUT_TILE_SIZE, Bitmap.Config.ARGB_8888)
        val outputPixels = IntArray(OUTPUT_TILE_SIZE * OUTPUT_TILE_SIZE)
        
        for (i in outputPixels.indices) {
            val r = (outputBuffer.float * 255.0f).toInt().coerceIn(0, 255)
            val g = (outputBuffer.float * 255.0f).toInt().coerceIn(0, 255)
            val b = (outputBuffer.float * 255.0f).toInt().coerceIn(0, 255)
            outputPixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        
        outputBitmap.setPixels(outputPixels, 0, OUTPUT_TILE_SIZE, 0, 0, OUTPUT_TILE_SIZE, OUTPUT_TILE_SIZE)
        
        if (paddedTile != tile) {
            paddedTile.recycle()
        }
        
        return outputBitmap
    }

    private fun loadModelFile(file: File): java.nio.MappedByteBuffer {
        val inputStream = FileInputStream(file)
        val fileChannel = inputStream.channel
        val buffer = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            0,
            file.length()
        )
        inputStream.close()
        return buffer
    }

    fun release() {
        interpreter?.close()
        interpreter = null
        gpuDelegate?.close()
        gpuDelegate = null
        isInitialized = false
    }
    
    fun isReady(): Boolean = isInitialized
}
