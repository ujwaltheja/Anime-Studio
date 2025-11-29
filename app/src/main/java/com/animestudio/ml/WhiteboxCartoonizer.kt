package com.animestudio.ml

import android.content.Context
import android.graphics.Bitmap
import com.animestudio.domain.FrameData
import com.animestudio.domain.Result
import com.animestudio.models.ModelManager
import com.animestudio.models.ModelRegistry
import com.animestudio.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

/**
 * White-box Cartoonization Engine
 * Based on research report Section 3.2: "White-box Cartoonization"
 * 
 * Implements guided filter-based cartoonization with three-representation decomposition:
 * - Surface: Smooth textures and colors (limited palette)
 * - Structure: Segmentation and sparse structures (edges)
 * - Texture: High-frequency details (line art)
 * 
 * Features:
 * - Extremely lightweight (~2.5 MB model)
 * - Runs on CPU (no GPU required)
 * - Distinct "cel-shaded" anime look
 * - Fast inference (~100-150ms on mid-range devices)
 * 
 * Reference: https://blog.tensorflow.org/2020/09/how-to-create-cartoonizer-with-tf-lite.html
 */
class WhiteboxCartoonizer(
    private val context: Context,
    private val modelManager: ModelManager
) {
    
    companion object {
        private const val TAG = "WhiteboxCartoonizer"
        
        // Model specifications from research
        private const val INPUT_SIZE = 512  // Can be 512 or 720
        private const val OUTPUT_SIZE = 512
        private const val PIXEL_SIZE = 3  // RGB
        
        // Normalization range: [-1, 1] as per report Section 8.1
        private const val MEAN = 127.5f
        private const val STD = 127.5f
    }
    
    private var interpreter: Interpreter? = null
    private var isInitialized = false
    
    // Image processor for preprocessing
    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeWithCropOrPadOp(INPUT_SIZE, INPUT_SIZE))  // Center crop/pad
        .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))  // CRITICAL: Use BILINEAR
        .add(NormalizeOp(MEAN, STD))  // Normalize to [-1, 1]
        .build()
    
    /**
     * Initialize the model
     * Downloads if necessary
     */
    suspend fun initialize(onProgress: (Int) -> Unit = {}): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (isInitialized) {
                    return@withContext Result.Success(Unit)
                }
                
                Logger.i(TAG, "Initializing White-box Cartoonizer...")
                
                // Check if model is available
                if (!modelManager.isModelAvailable(ModelRegistry.WHITEBOX_CARTOON.id)) {
                    Logger.i(TAG, "Model not found, downloading...")
                    
                    // Download model
                    var downloadError: String? = null
                    modelManager.downloadModel(ModelRegistry.WHITEBOX_CARTOON.id)
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
                
                // Load model
                val modelPath = modelManager.getModelPath(ModelRegistry.WHITEBOX_CARTOON.id)
                    ?: return@withContext Result.Error("Model file not found")
                
                val options = Interpreter.Options().apply {
                    setNumThreads(4)  // CPU execution - from report
                    // No GPU delegate needed - runs efficiently on CPU
                }
                
                interpreter = Interpreter(loadModelFile(modelPath), options)
                isInitialized = true
                
                Logger.i(TAG, "White-box Cartoonizer initialized successfully")
                Logger.i(TAG, "Model size: ${modelPath.length() / 1024 / 1024} MB")
                
                Result.Success(Unit)
                
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to initialize: ${e.message}")
                e.printStackTrace()
                Result.Error("Initialization failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Apply cartoonization to a frame
     * 
     * @param frame Input frame
     * @return Cartoonized frame with cel-shaded aesthetic
     */
    suspend fun cartoonize(frame: FrameData): Result<FrameData> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isInitialized) {
                    return@withContext Result.Error("Model not initialized")
                }
                
                val startTime = System.currentTimeMillis()
                
                // Load bitmap
                val bitmap = android.graphics.BitmapFactory.decodeFile(frame.file?.absolutePath)
                    ?: return@withContext Result.Error("Failed to load frame bitmap")
                
                // Apply cartoonization
                val cartoonized = cartoonize(bitmap)
                
                // Save output
                val outputFile = File(
                    frame.file?.parent,
                    "cartoon_${frame.file?.name}"
                ).apply {
                    outputStream().use { out ->
                        cartoonized.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                }
                
                val elapsedTime = System.currentTimeMillis() - startTime
                Logger.i(TAG, "Cartoonized frame ${frame.index} in ${elapsedTime}ms")
                
                val resultFrame = frame.copy(file = outputFile)
                cartoonized.recycle()
                bitmap.recycle()
                
                Result.Success(resultFrame)
                
            } catch (e: Exception) {
                Logger.e(TAG, "Cartoonization failed: ${e.message}")
                Result.Error("Cartoonization failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Apply cartoonization to a bitmap
     * 
     * @param input Input bitmap (any size)
     * @return Cartoonized bitmap
     */
    suspend fun cartoonize(input: Bitmap): Bitmap = withContext(Dispatchers.IO) {
        
        val originalWidth = input.width
        val originalHeight = input.height
        
        // Preprocess image
        val tensorImage = TensorImage.fromBitmap(input)
        val processedImage = imageProcessor.process(tensorImage)
        
        // Prepare input buffer
        val inputBuffer = processedImage.buffer
        
        // Prepare output buffer
        val outputBuffer = ByteBuffer.allocateDirect(
            4 * OUTPUT_SIZE * OUTPUT_SIZE * PIXEL_SIZE
        ).apply {
            order(ByteOrder.nativeOrder())
        }
        
        // Run inference
        interpreter!!.run(inputBuffer, outputBuffer)
        
        // Convert output to bitmap
        val cartoonBitmap = byteBufferToBitmap(outputBuffer, OUTPUT_SIZE, OUTPUT_SIZE)
        
        // Resize back to original dimensions if needed
        if (originalWidth != OUTPUT_SIZE || originalHeight != OUTPUT_SIZE) {
            val scaled = Bitmap.createScaledBitmap(
                cartoonBitmap,
                originalWidth,
                originalHeight,
                true  // Use bilinear filtering
            )
            cartoonBitmap.recycle()
            scaled
        } else {
            cartoonBitmap
        }
    }
    
    /**
     * Batch process multiple frames
     */
    suspend fun cartoonizeBatch(
        frames: List<FrameData>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<List<FrameData>> = withContext(Dispatchers.IO) {
        
        try {
            if (!isInitialized) {
                return@withContext Result.Error("Model not initialized")
            }
            
            val results = mutableListOf<FrameData>()
            
            frames.forEachIndexed { index, frame ->
                when (val result = cartoonize(frame)) {
                    is Result.Success -> {
                        results.add(result.data)
                        onProgress(index + 1, frames.size)
                    }
                    is Result.Error -> {
                        Logger.e(TAG, "Failed to process frame ${frame.index}: ${result.message}")
                        results.add(frame)  // Keep original on error
                    }
                    Result.Loading -> {
                        // Should not happen, skip
                    }
                }
            }
            
            Result.Success(results)
            
        } catch (e: Exception) {
            Logger.e(TAG, "Batch processing failed: ${e.message}")
            Result.Error("Batch processing failed: ${e.message}", e)
        }
    }
    
    /**
     * Convert ByteBuffer to Bitmap
     * Denormalize from [-1, 1] to [0, 255]
     */
    private fun byteBufferToBitmap(buffer: ByteBuffer, width: Int, height: Int): Bitmap {
        buffer.rewind()
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        
        for (i in pixels.indices) {
            val r = ((buffer.float * STD + MEAN)).toInt().coerceIn(0, 255)
            val g = ((buffer.float * STD + MEAN)).toInt().coerceIn(0, 255)
            val b = ((buffer.float * STD + MEAN)).toInt().coerceIn(0, 255)
            
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
    
    /**
     * Load model file with memory mapping
     */
    private fun loadModelFile(file: File): java.nio.MappedByteBuffer {
        val inputStream = FileInputStream(file)
        val fileChannel = inputStream.channel
        val buffer = fileChannel.map(
            java.nio.channels.FileChannel.MapMode.READ_ONLY,
            0,
            file.length()
        )
        inputStream.close()
        return buffer
    }
    
    /**
     * Get model info
     */
    fun getModelInfo(): String {
        return """
            White-box Cartoonization
            - Category: Cel-shaded anime style
            - Input: ${INPUT_SIZE}x${INPUT_SIZE} RGB
            - Output: ${OUTPUT_SIZE}x${OUTPUT_SIZE} RGB
            - Execution: CPU (4 threads)
            - Inference time: ~100-150ms
            - Features: Guided filter, limited color palette
        """.trimIndent()
    }
    
    /**
     * Release resources
     */
    fun release() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
        Logger.i(TAG, "Resources released")
    }
    
    /**
     * Check if initialized
     */
    fun isReady(): Boolean = isInitialized
}
