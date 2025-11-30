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
import java.util.Optional

/**
 * VTuber Engine - Face Tracking & Avatar Animation
 *
 * Uses MediaPipe Face Landmarker to track:
 * - Eye blinking (via Blendshapes)
 * - Mouth movement (via Blendshapes)
 * - Head rotation (via Transformation Matrix)
 */
class VTuberEngine(
    private val context: Context,
    private val modelManager: ModelManager
) {
    companion object {
        private const val TAG = "VTuberEngine"
    }

    private var faceLandmarker: FaceLandmarker? = null
    private var isInitialized = false
    private var isTestMode = false

    data class FaceData(
        val leftEyeOpen: Float, // 0.0 (closed) to 1.0 (open)
        val rightEyeOpen: Float,
        val mouthOpen: Float,
        val headYaw: Float,   // Left/Right rotation
        val headPitch: Float, // Up/Down rotation
        val headRoll: Float   // Tilt
    )

    suspend fun initialize(onProgress: (Int) -> Unit = {}): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelId = ModelRegistry.MEDIAPIPE_FACE_LANDMARKER.id
            
            if (!modelManager.isModelAvailable(modelId)) {
                Logger.w(TAG, "Face Landmarker model not found. Entering TEST MODE.")
                isTestMode = true
                isInitialized = true
                return@withContext Result.Success(Unit)
            }

            val modelPath = modelManager.getModelPath(modelId)!!
            
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelPath.absolutePath)
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE) // Use IMAGE for single frame, VIDEO/LIVE_STREAM for stream
                .setNumFaces(1)
                .setOutputFaceBlendshapes(true)
                .setOutputFacialTransformationMatrixes(true)
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            isInitialized = true
            
            Logger.i(TAG, "MediaPipe Face Landmarker initialized successfully")
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
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = faceLandmarker?.detect(mpImage)
            
            if (result != null && result.faceBlendshapes().isPresent && result.faceBlendshapes().get().isNotEmpty()) {
                val blendshapes = result.faceBlendshapes().get()[0]
                
                // Extract blendshapes
                // Note: MediaPipe blendshape names are specific. 
                // We need to map them. Common indices or names:
                // eyeBlinkLeft, eyeBlinkRight, jawOpen
                
                var leftEyeBlink = 0f
                var rightEyeBlink = 0f
                var jawOpen = 0f
                
                for (category in blendshapes) {
                    when (category.categoryName()) {
                        "eyeBlinkLeft" -> leftEyeBlink = category.score()
                        "eyeBlinkRight" -> rightEyeBlink = category.score()
                        "jawOpen" -> jawOpen = category.score()
                    }
                }

                // Calculate head rotation from transformation matrix if available
                var yaw = 0f
                var pitch = 0f
                var roll = 0f
                
                if (result.facialTransformationMatrixes().isPresent && result.facialTransformationMatrixes().get().isNotEmpty()) {
                    val matrix = result.facialTransformationMatrixes().get()[0]
                    // Extract Euler angles from 4x4 matrix
                    // This is a simplified extraction
                    // Matrix is row-major float array of size 16
                    
                    // Rotation matrix is top-left 3x3
                    // R = [ r00 r01 r02 ]
                    //     [ r10 r11 r12 ]
                    //     [ r20 r21 r22 ]
                    
                    // Pitch (x-axis) = atan2(r21, r22)
                    // Yaw (y-axis) = atan2(-r20, sqrt(r21^2 + r22^2))
                    // Roll (z-axis) = atan2(r10, r00)
                    
                    val r10 = matrix[4]
                    val r00 = matrix[0]
                    val r20 = matrix[8]
                    val r21 = matrix[9]
                    val r22 = matrix[10]
                    
                    pitch = Math.atan2(r21.toDouble(), r22.toDouble()).toFloat()
                    yaw = Math.atan2(-r20.toDouble(), Math.sqrt((r21 * r21 + r22 * r22).toDouble())).toFloat()
                    roll = Math.atan2(r10.toDouble(), r00.toDouble()).toFloat()
                }

                return@withContext Result.Success(
                    FaceData(
                        leftEyeOpen = 1.0f - leftEyeBlink,
                        rightEyeOpen = 1.0f - rightEyeBlink,
                        mouthOpen = jawOpen,
                        headYaw = yaw,
                        headPitch = pitch,
                        headRoll = roll
                    )
                )
            } else {
                 // No face detected, return neutral
                 return@withContext Result.Success(
                    FaceData(1f, 1f, 0f, 0f, 0f, 0f)
                 )
            }

        } catch (e: Exception) {
            Logger.e(TAG, "Inference error: ${e.message}")
            return@withContext Result.Error(e.message ?: "Inference error")
        }
    }

    fun release() {
        faceLandmarker?.close()
        faceLandmarker = null
    }
}
