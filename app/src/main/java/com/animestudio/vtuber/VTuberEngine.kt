package com.animestudio.vtuber

import android.content.Context
import android.graphics.Bitmap
import com.animestudio.domain.Result
import com.animestudio.models.ModelManager
import com.animestudio.models.ModelRegistry
import com.animestudio.utils.Logger
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * VTuber Engine - Face Tracking & Avatar Animation
 *
 * Uses MediaPipe Face Landmarker with 478 landmarks and 52 blendshapes:
 * - Eye blinking (precise coefficients via blendshapes)
 * - Mouth movement (jawOpen, mouthSmile, etc.)
 * - Head rotation (6DOF transformation matrix)
 * - Facial expressions (eyebrow, cheek, etc.)
 */
class VTuberEngine(
    private val context: Context,
    private val modelManager: ModelManager
) {
    companion object {
        private const val TAG = "VTuberEngine"

        // Eye landmark indices for fallback calculation
        private const val LEFT_EYE_TOP = 159
        private const val LEFT_EYE_BOTTOM = 145
        private const val RIGHT_EYE_TOP = 386
        private const val RIGHT_EYE_BOTTOM = 374

        // Mouth landmark indices
        private const val MOUTH_TOP = 13
        private const val MOUTH_BOTTOM = 14
    }

    private var faceLandmarker: FaceLandmarker? = null
    private var isInitialized = false
    private var isTestMode = false

    data class FaceData(
        val leftEyeOpen: Float,      // 0.0 (closed) to 1.0 (open)
        val rightEyeOpen: Float,
        val mouthOpen: Float,
        val headYaw: Float,          // Left/Right rotation (-1.0 to 1.0)
        val headPitch: Float,        // Up/Down rotation (-1.0 to 1.0)
        val headRoll: Float,         // Tilt (-1.0 to 1.0)
        val blendshapes: Map<String, Float> = emptyMap()  // All 52 blendshapes
    )

    suspend fun initialize(onProgress: (Int) -> Unit = {}): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelId = ModelRegistry.MEDIAPIPE_FACE_LANDMARKER.id

            if (!modelManager.isModelAvailable(modelId)) {
                Logger.w(TAG, "Face Landmarker model not found. Entering TEST MODE.")
                Logger.w(TAG, "Download model via ModelManager.downloadModel(\"$modelId\")")
                isTestMode = true
                isInitialized = true
                return@withContext Result.Success(Unit)
            }

            val modelPath = modelManager.getModelPath(modelId)!!

            // Build MediaPipe Face Landmarker options
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelPath.absolutePath)
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setOutputFaceBlendshapes(true)
                .setOutputFacialTransformationMatrixes(true)
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            isInitialized = true

            Logger.i(TAG, "VTuber Engine initialized with MediaPipe Face Landmarker")
            Result.Success(Unit)

        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize: ${e.message}")
            // Fallback to test mode
            isTestMode = true
            isInitialized = true
            Result.Success(Unit)
        }
    }

    suspend fun processFrame(bitmap: Bitmap): Result<FaceData> = withContext(Dispatchers.Default) {
        if (!isInitialized) return@withContext Result.Error("Not initialized")

        if (isTestMode) {
            // Simulate face movement based on time
            val time = System.currentTimeMillis() / 1000.0
            return@withContext Result.Success(
                FaceData(
                    leftEyeOpen = 0.8f + 0.2f * Math.sin(time * 2).toFloat(),
                    rightEyeOpen = 0.8f + 0.2f * Math.sin(time * 2).toFloat(),
                    mouthOpen = 0.3f + 0.3f * Math.sin(time * 5).toFloat(),
                    headYaw = 0.2f * Math.sin(time).toFloat(),
                    headPitch = 0.1f * Math.cos(time).toFloat(),
                    headRoll = 0.05f * Math.sin(time * 0.5).toFloat()
                )
            )
        }

        try {
            // Convert bitmap to MediaPipe image
            val mpImage = BitmapImageBuilder(bitmap).build()

            // Run face detection
            val result = faceLandmarker?.detect(mpImage)
                ?: return@withContext Result.Error("Face landmarker not available")

            if (result.faceLandmarks().isEmpty()) {
                return@withContext Result.Error("No face detected")
            }

            // Extract data from the first detected face
            val faceData = extractFaceData(result)
            Result.Success(faceData)

        } catch (e: Exception) {
            Logger.e(TAG, "Error processing frame: ${e.message}")
            Result.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Extract facial data from MediaPipe results
     */
    private fun extractFaceData(result: FaceLandmarkerResult): FaceData {
        val landmarks = result.faceLandmarks()[0]
        val blendshapes = result.faceBlendshapes().getOrNull(0)
        val transformMatrix = result.facialTransformationMatrixes().getOrNull(0)

        // Extract eye openness from blendshapes (preferred) or landmarks (fallback)
        val leftEyeOpen = blendshapes?.let { blendshapeList ->
            val eyeBlinkLeft = blendshapeList.find { it.categoryName() == "eyeBlinkLeft" }?.score() ?: 0f
            1.0f - eyeBlinkLeft  // Invert: blendshape is "blink", we want "open"
        } ?: calculateEyeOpenness(landmarks, LEFT_EYE_TOP, LEFT_EYE_BOTTOM)

        val rightEyeOpen = blendshapes?.let { blendshapeList ->
            val eyeBlinkRight = blendshapeList.find { it.categoryName() == "eyeBlinkRight" }?.score() ?: 0f
            1.0f - eyeBlinkRight
        } ?: calculateEyeOpenness(landmarks, RIGHT_EYE_TOP, RIGHT_EYE_BOTTOM)

        // Extract mouth openness
        val mouthOpen = blendshapes?.let { blendshapeList ->
            blendshapeList.find { it.categoryName() == "jawOpen" }?.score() ?: 0f
        } ?: calculateMouthOpenness(landmarks)

        // Extract head rotation from transformation matrix
        val (yaw, pitch, roll) = transformMatrix?.let {
            extractRotationFromMatrix(it.data())
        } ?: Triple(0f, 0f, 0f)

        // Store all blendshapes
        val allBlendshapes = blendshapes?.associate {
            it.categoryName() to it.score()
        } ?: emptyMap()

        return FaceData(
            leftEyeOpen = leftEyeOpen.coerceIn(0f, 1f),
            rightEyeOpen = rightEyeOpen.coerceIn(0f, 1f),
            mouthOpen = mouthOpen.coerceIn(0f, 1f),
            headYaw = yaw,
            headPitch = pitch,
            headRoll = roll,
            blendshapes = allBlendshapes
        )
    }

    /**
     * Fallback: Calculate eye openness from landmark distance
     */
    private fun calculateEyeOpenness(
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        topIdx: Int,
        bottomIdx: Int
    ): Float {
        if (topIdx >= landmarks.size || bottomIdx >= landmarks.size) return 0.8f
        val top = landmarks[topIdx]
        val bottom = landmarks[bottomIdx]
        val distance = abs(top.y() - bottom.y())
        return (distance * 20f).coerceIn(0f, 1f)  // Scale to 0-1 range
    }

    /**
     * Fallback: Calculate mouth openness from landmark distance
     */
    private fun calculateMouthOpenness(
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>
    ): Float {
        if (MOUTH_TOP >= landmarks.size || MOUTH_BOTTOM >= landmarks.size) return 0f
        val top = landmarks[MOUTH_TOP]
        val bottom = landmarks[MOUTH_BOTTOM]
        val distance = abs(top.y() - bottom.y())
        return (distance * 10f).coerceIn(0f, 1f)
    }

    /**
     * Extract Euler angles (yaw, pitch, roll) from 4x4 transformation matrix
     */
    private fun extractRotationFromMatrix(matrix: FloatArray): Triple<Float, Float, Float> {
        if (matrix.size < 16) return Triple(0f, 0f, 0f)

        // Extract rotation matrix (3x3 upper-left of 4x4 matrix)
        val m00 = matrix[0]
        val m01 = matrix[1]
        val m02 = matrix[2]
        val m10 = matrix[4]
        val m11 = matrix[5]
        val m12 = matrix[6]
        val m20 = matrix[8]
        val m21 = matrix[9]
        val m22 = matrix[10]

        // Calculate Euler angles (in radians, then normalized to -1 to 1)
        val pitch = Math.asin(-m20.toDouble()).toFloat()
        val yaw = Math.atan2(m10.toDouble(), m00.toDouble()).toFloat()
        val roll = Math.atan2(m21.toDouble(), m22.toDouble()).toFloat()

        return Triple(
            (yaw / Math.PI).toFloat(),      // Normalize to -1 to 1
            (pitch / Math.PI * 2).toFloat(),
            (roll / Math.PI).toFloat()
        )
    }

    fun release() {
        faceLandmarker?.close()
        faceLandmarker = null
        isInitialized = false
    }
}
