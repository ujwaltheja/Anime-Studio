package com.animestudio.config

import com.animestudio.domain.StyleType

/**
 * Central configuration for Anime Studio
 * Defines:
 * - Available ML models (2025 versions)
 * - Model metadata and paths
 * - Processing parameters
 * - Feature flags
 */
object AppConfig {
    // App version
    const val APP_VERSION = "2.0.0"
    const val BUILD_DATE = "2025-01-15"

    // Processing configuration
    object Processing {
        // GPU acceleration configuration
        const val ENABLE_GPU_BY_DEFAULT = false // Set to true after stability testing
        const val ENABLE_NNAPI = true
        const val NUM_THREADS = 4

        // Memory configuration
        const val MAX_MEMORY_MB = 512
        const val CRITICAL_MEMORY_THRESHOLD = 50
        const val WARNING_MEMORY_THRESHOLD = 100

        // Frame extraction
        const val DEFAULT_FRAME_RATE = 30
        const val FRAME_QUALITY = 90
        const val MAX_CONCURRENT_FRAMES = 8
    }

    // ML Models Configuration (2025 Updated)
    object Models {
        // Base path for models in assets
        const val MODEL_BASE_PATH = "models"

        // Latest AnimeGAN v3 models (2025)
        object AnimeGANv3 {
            const val NAME = "AnimeGAN v3"
            const val VERSION = "3.1"
            const val RELEASE_DATE = "2024-Q4"
            const val HAYAO_PATH = "models/animeganv3_hayao.tflite"
            const val SHINKAI_PATH = "models/animeganv3_shinkai.tflite"
            const val PAPRIKA_PATH = "models/animeganv3_paprika.tflite"
            const val HOSODA_PATH = "models/animeganv3_hosoda.tflite"

            // Inference configuration
            const val INPUT_SIZE = 512
            const val OUTPUT_SIZE = 512
            const val USE_FLOAT32 = true // Better quality

            // Performance
            const val AVG_INFERENCE_TIME_MS = 500  // Per frame
            const val MODEL_SIZE_MB = 4.2f
        }

        // CartoonGAN - Latest version
        object CartoonGAN {
            const val NAME = "CartoonGAN"
            const val VERSION = "2.1"
            const val RELEASE_DATE = "2024-Q3"
            const val PATH = "models/cartoongan.tflite"

            const val INPUT_SIZE = 512
            const val OUTPUT_SIZE = 512
            const val AVG_INFERENCE_TIME_MS = 400
            const val MODEL_SIZE_MB = 1.8f
        }

        // Real-ESRGAN for upscaling (New in 2025)
        object RealESRGAN {
            const val NAME = "Real-ESRGAN"
            const val VERSION = "0.3"
            const val RELEASE_DATE = "2024-Q4"
            const val PATH = "models/realesrgan_x2.tflite"

            const val UPSCALE_FACTOR = 2
            const val INPUT_SIZE = 256
            const val AVG_INFERENCE_TIME_MS = 800
            const val MODEL_SIZE_MB = 2.5f
        }

        // Style transfer models (Legacy support)
        object StyleTransfer {
            const val NAME = "Generic Style Transfer"
            const val VERSION = "1.0"
            const val PATH = "models/style_transfer.tflite"
        }

        // Get model info by style type
        fun getModelInfo(styleType: StyleType): ModelInfo {
            return when (styleType) {
                StyleType.HAYAO -> ModelInfo(
                    name = "Hayao (Ghibli Style)",
                    path = AnimeGANv3.HAYAO_PATH,
                    size = AnimeGANv3.MODEL_SIZE_MB,
                    inputSize = AnimeGANv3.INPUT_SIZE,
                    version = AnimeGANv3.VERSION,
                    estimatedTimeMs = AnimeGANv3.AVG_INFERENCE_TIME_MS
                )
                StyleType.SHINKAI -> ModelInfo(
                    name = "Shinkai (Your Name Style)",
                    path = AnimeGANv3.SHINKAI_PATH,
                    size = AnimeGANv3.MODEL_SIZE_MB,
                    inputSize = AnimeGANv3.INPUT_SIZE,
                    version = AnimeGANv3.VERSION,
                    estimatedTimeMs = AnimeGANv3.AVG_INFERENCE_TIME_MS
                )
                StyleType.PAPRIKA -> ModelInfo(
                    name = "Paprika (Surreal Style)",
                    path = AnimeGANv3.PAPRIKA_PATH,
                    size = AnimeGANv3.MODEL_SIZE_MB,
                    inputSize = AnimeGANv3.INPUT_SIZE,
                    version = AnimeGANv3.VERSION,
                    estimatedTimeMs = AnimeGANv3.AVG_INFERENCE_TIME_MS
                )
                StyleType.CARTOON_GAN -> ModelInfo(
                    name = "CartoonGAN",
                    path = CartoonGAN.PATH,
                    size = CartoonGAN.MODEL_SIZE_MB,
                    inputSize = CartoonGAN.INPUT_SIZE,
                    version = CartoonGAN.VERSION,
                    estimatedTimeMs = CartoonGAN.AVG_INFERENCE_TIME_MS
                )
                StyleType.ANIME_GAN -> ModelInfo(
                    name = "AnimeGAN v2 (Legacy)",
                    path = "models/animegan.tflite",
                    size = 2.2f,
                    inputSize = 256,
                    version = "2.0",
                    estimatedTimeMs = 300
                )
                StyleType.STYLE_TRANSFER -> ModelInfo(
                    name = StyleTransfer.NAME,
                    path = StyleTransfer.PATH,
                    size = 2.8f,
                    inputSize = 256,
                    version = StyleTransfer.VERSION,
                    estimatedTimeMs = 400
                )
                StyleType.CUSTOM -> ModelInfo(
                    name = "Custom Model",
                    path = "models/animeganv3_hayao.tflite",
                    size = AnimeGANv3.MODEL_SIZE_MB,
                    inputSize = AnimeGANv3.INPUT_SIZE,
                    version = AnimeGANv3.VERSION,
                    estimatedTimeMs = AnimeGANv3.AVG_INFERENCE_TIME_MS
                )
            }
        }

        /**
         * Get estimated processing time for a video
         */
        fun getEstimatedProcessingTime(
            videoDurationSeconds: Int,
            frameRate: Int,
            styleType: StyleType
        ): Long {
            val frameCount = (videoDurationSeconds * frameRate).toLong()
            val modelInfo = getModelInfo(styleType)
            // Model inference + frame extraction + video reconstruction
            val overheadMs = 5000 // 5 seconds total overhead
            return (frameCount * modelInfo.estimatedTimeMs) + overheadMs
        }
    }

    // Feature flags
    object Features {
        const val ENABLE_BATCH_PROCESSING = true
        const val ENABLE_CLOUD_API = false  // Disabled until backend is ready
        const val ENABLE_HISTORY = true
        const val ENABLE_ANALYTICS = false  // Set to true with proper consent
        const val ENABLE_ADVANCED_SETTINGS = true
    }

    // Video processing defaults
    object Video {
        const val DEFAULT_BITRATE = "5M"
        const val DEFAULT_CODEC = "libx264"
        const val DEFAULT_PRESET = "medium"  // fast, medium, slow
        const val DEFAULT_CRF = 23  // Quality (0-51, lower is better)
        const val DEFAULT_PIXELFORMAT = "yuv420p"
    }

    // Error messages and help
    object ErrorMessages {
        const val MODEL_NOT_FOUND =
            "Model file not found. Models should be in assets/models/ directory.\n" +
            "Run the download script or ensure models are properly bundled."

        const val GPU_NOT_AVAILABLE =
            "GPU acceleration not available on this device.\n" +
            "Processing will use CPU instead (slower but more compatible)."

        const val INSUFFICIENT_MEMORY =
            "Not enough memory available.\n" +
            "Try closing other apps or processing a shorter video."

        const val UNSUPPORTED_VIDEO_FORMAT =
            "Video format not supported.\n" +
            "Please use MP4, MOV, or MKV formats."

        const val PROCESSING_CANCELLED =
            "Video processing was cancelled by user."

        const val UNKNOWN_ERROR =
            "An unexpected error occurred.\n" +
            "Please check device memory and try again."
    }

    // Help and tips
    object Tips {
        const val RECOMMENDED_VIDEO_LENGTH = "1-5 minutes for best results"
        const val RECOMMENDED_RESOLUTION = "1080p or lower for faster processing"
        const val RECOMMENDED_BITRATE = "5-10 Mbps"
    }
}

/**
 * Model metadata information
 */
data class ModelInfo(
    val name: String,
    val path: String,
    val size: Float,  // in MB
    val inputSize: Int,  // 256 or 512
    val version: String,
    val estimatedTimeMs: Int  // per frame
) {
    fun getDisplayInfo(): String {
        return buildString {
            append("Name: $name\n")
            append("Version: $version\n")
            append("Size: ${String.format("%.1f", size)}MB\n")
            append("Input Size: ${inputSize}x${inputSize}\n")
            append("Est. Time: ${estimatedTimeMs}ms per frame")
        }
    }
}
