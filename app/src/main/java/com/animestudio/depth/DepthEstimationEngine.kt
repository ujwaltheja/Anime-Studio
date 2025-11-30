package com.animestudio.depth

import android.content.Context
import android.graphics.Bitmap
import com.animestudio.domain.Result
import com.animestudio.models.ModelManager
import com.animestudio.models.ModelRegistry
import com.animestudio.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Depth Estimation Engine - MiDaS depth estimation
 *
 * Uses MiDaS v2.1 Small model to estimate depth from a single RGB image.
 *
 * Use cases:
 * - 3D effects (parallax, depth-of-field blur)
 * - Background replacement with proper depth boundaries
 * - AR overlay positioning
 * - Portrait mode effects
 */
class DepthEstimationEngine(
    private val context: Context,
    private val modelManager: ModelManager
) {
    companion object {
        private const val TAG = "DepthEstimationEngine"

        // MiDaS Small model input/output configuration
        private const val INPUT_WIDTH = 256
        private const val INPUT_HEIGHT = 256
        private const val OUTPUT_WIDTH = 256
        private const val OUTPUT_HEIGHT = 256

        // Normalization constants for MiDaS
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var isInitialized = false

    private var inputBuffer: ByteBuffer? = null
    private var outputBuffer: ByteBuffer? = null

    /**
     * Initialize the depth estimation model
     */
    suspend fun initialize(useGpu: Boolean = true): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelId = ModelRegistry.MIDAS_DEPTH.id

            if (!modelManager.isModelAvailable(modelId)) {
                Logger.w(TAG, "MiDaS depth model not found.")
                Logger.w(TAG, "Download via: ModelManager.downloadModel(\"$modelId\")")
                return@withContext Result.Error("Model not available. Download required (~8MB)")
            }

            val modelPath = modelManager.getModelPath(modelId)!!

            // Configure interpreter options
            val options = Interpreter.Options().apply {
                setNumThreads(4)

                if (useGpu) {
                    try {
                        gpuDelegate = GpuDelegate()
                        addDelegate(gpuDelegate)
                        Logger.i(TAG, "GPU acceleration enabled")
                    } catch (e: Exception) {
                        Logger.w(TAG, "GPU delegate failed, using CPU: ${e.message}")
                    }
                }
            }

            interpreter = Interpreter(modelPath, options)

            // Pre-allocate buffers
            allocateBuffers()

            isInitialized = true
            Logger.i(TAG, "Depth Estimation Engine initialized")
            Result.Success(Unit)

        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize: ${e.message}")
            cleanup()
            Result.Error("Initialization failed: ${e.message}")
        }
    }

    /**
     * Estimate depth map from an RGB image
     *
     * @param bitmap Input image (will be resized to 256x256 internally)
     * @return Depth map as grayscale bitmap (0 = far, 255 = near)
     */
    suspend fun estimateDepth(bitmap: Bitmap): Result<Bitmap> = withContext(Dispatchers.Default) {
        if (!isInitialized) {
            return@withContext Result.Error("Engine not initialized. Call initialize() first.")
        }

        try {
            // Preprocess: resize and normalize
            val resized = Bitmap.createScaledBitmap(bitmap, INPUT_WIDTH, INPUT_HEIGHT, true)
            preprocessImage(resized)

            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)
                ?: return@withContext Result.Error("Interpreter not available")

            // Postprocess: convert depth values to bitmap
            val depthMap = postprocessDepth()

            Result.Success(depthMap)

        } catch (e: Exception) {
            Logger.e(TAG, "Depth estimation failed: ${e.message}")
            Result.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Estimate depth and return as float array for advanced processing
     *
     * @return FloatArray of size (OUTPUT_HEIGHT * OUTPUT_WIDTH)
     */
    suspend fun estimateDepthRaw(bitmap: Bitmap): Result<FloatArray> = withContext(Dispatchers.Default) {
        if (!isInitialized) {
            return@withContext Result.Error("Engine not initialized")
        }

        try {
            val resized = Bitmap.createScaledBitmap(bitmap, INPUT_WIDTH, INPUT_HEIGHT, true)
            preprocessImage(resized)

            interpreter?.run(inputBuffer, outputBuffer)
                ?: return@withContext Result.Error("Interpreter not available")

            // Extract raw depth values
            outputBuffer?.rewind()
            val depthValues = FloatArray(OUTPUT_WIDTH * OUTPUT_HEIGHT)
            outputBuffer?.asFloatBuffer()?.get(depthValues)

            Result.Success(depthValues)

        } catch (e: Exception) {
            Logger.e(TAG, "Raw depth estimation failed: ${e.message}")
            Result.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Allocate input and output buffers
     */
    private fun allocateBuffers() {
        // Input: [1, 256, 256, 3] float32
        val inputSize = 1 * INPUT_HEIGHT * INPUT_WIDTH * 3 * 4 // 4 bytes per float
        inputBuffer = ByteBuffer.allocateDirect(inputSize).apply {
            order(ByteOrder.nativeOrder())
        }

        // Output: [1, 256, 256, 1] float32
        val outputSize = 1 * OUTPUT_HEIGHT * OUTPUT_WIDTH * 4
        outputBuffer = ByteBuffer.allocateDirect(outputSize).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    /**
     * Preprocess image: normalize with ImageNet mean/std
     */
    private fun preprocessImage(bitmap: Bitmap) {
        inputBuffer?.rewind()

        val pixels = IntArray(INPUT_WIDTH * INPUT_HEIGHT)
        bitmap.getPixels(pixels, 0, INPUT_WIDTH, 0, 0, INPUT_WIDTH, INPUT_HEIGHT)

        for (pixel in pixels) {
            // Extract RGB channels
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            // Normalize using ImageNet statistics
            val rNorm = (r - MEAN[0]) / STD[0]
            val gNorm = (g - MEAN[1]) / STD[1]
            val bNorm = (b - MEAN[2]) / STD[2]

            inputBuffer?.putFloat(rNorm)
            inputBuffer?.putFloat(gNorm)
            inputBuffer?.putFloat(bNorm)
        }

        inputBuffer?.rewind()
    }

    /**
     * Postprocess depth output to grayscale bitmap
     */
    private fun postprocessDepth(): Bitmap {
        outputBuffer?.rewind()

        val depthValues = FloatArray(OUTPUT_WIDTH * OUTPUT_HEIGHT)
        outputBuffer?.asFloatBuffer()?.get(depthValues)

        // Find min/max for normalization
        var minDepth = Float.MAX_VALUE
        var maxDepth = Float.MIN_VALUE
        for (value in depthValues) {
            if (value < minDepth) minDepth = value
            if (value > maxDepth) maxDepth = value
        }

        val range = maxDepth - minDepth

        // Create grayscale bitmap
        val bitmap = Bitmap.createBitmap(OUTPUT_WIDTH, OUTPUT_HEIGHT, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(OUTPUT_WIDTH * OUTPUT_HEIGHT)

        for (i in depthValues.indices) {
            // Normalize to 0-255 (invert so near = bright, far = dark)
            val normalized = ((depthValues[i] - minDepth) / range)
            val gray = (normalized * 255).toInt().coerceIn(0, 255)

            // Create grayscale pixel (ARGB)
            pixels[i] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
        }

        bitmap.setPixels(pixels, 0, OUTPUT_WIDTH, 0, 0, OUTPUT_WIDTH, OUTPUT_HEIGHT)
        return bitmap
    }

    /**
     * Check if model is available
     */
    fun isModelAvailable(): Boolean {
        return modelManager.isModelAvailable(ModelRegistry.MIDAS_DEPTH.id)
    }

    /**
     * Get model info
     */
    fun getModelInfo() = ModelRegistry.MIDAS_DEPTH

    private fun cleanup() {
        interpreter?.close()
        gpuDelegate?.close()

        interpreter = null
        gpuDelegate = null
        inputBuffer = null
        outputBuffer = null
        isInitialized = false
    }

    fun release() {
        cleanup()
    }
}
