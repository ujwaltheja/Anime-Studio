package com.animestudio.models

/**
 * Model Registry - Central management for all TFLite models
 * Based on: "Edge Intelligence in Anime Production" research
 * 
 * Manages model lifecycle, downloads, and versioning
 */
data class ModelInfo(
    val id: String,
    val name: String,
    val category: ModelCategory,
    val filePath: String,
    val sizeBytes: Long,
    val version: String,
    val requiredDelegate: DelegateType = DelegateType.ANY,
    val bundled: Boolean = false,  // If true, included in APK
    val downloadUrl: String? = null
)

enum class ModelCategory {
    STYLE_TRANSFER,      // AnimeGAN, White-box, U-GAT-IT
    GENERATION,          // Stable Diffusion, Waifu Diffusion
    ANIMATION,           // MediaPipe Face/Pose
    UPSCALING,           // Real-ESRGAN
    SUPPORT              // Segmentation, Depth estimation
}

enum class DelegateType {
    CPU,
    GPU,
    NNAPI,
    ANY
}

/**
 * Complete model catalog from research report
 */
object ModelRegistry {
    
    // ========== PHASE 1: CURRENT MODELS ==========
    
    val ANIMEGAN_HAYAO = ModelInfo(
        id = "animegan_hayao",
        name = "AnimeGAN Hayao (Ghibli)",
        category = ModelCategory.STYLE_TRANSFER,
        filePath = "models/animeganv3_hayao.tflite",
        sizeBytes = 4_237_396,  // Actual file size
        version = "3.0",
        requiredDelegate = DelegateType.GPU,
        bundled = true
    )

    val ANIMEGAN_SHINKAI = ModelInfo(
        id = "animegan_shinkai",
        name = "AnimeGAN Shinkai",
        category = ModelCategory.STYLE_TRANSFER,
        filePath = "models/animeganv3_shinkai.tflite",
        sizeBytes = 4_237_396,  // Actual file size
        version = "3.0",
        requiredDelegate = DelegateType.GPU,
        bundled = true
    )

    val ANIMEGAN_PAPRIKA = ModelInfo(
        id = "animegan_paprika",
        name = "AnimeGAN Paprika",
        category = ModelCategory.STYLE_TRANSFER,
        filePath = "models/paprika.tflite",
        sizeBytes = 2_250_816,  // Actual file size
        version = "3.0",
        requiredDelegate = DelegateType.GPU,
        bundled = true
    )
    
    // ========== PHASE 2: ADVANCED STYLE TRANSFER ==========
    
    val ANIMEGAN_V2_FAST = ModelInfo(
        id = "animegan_v2_fast",
        name = "AnimeGAN v2 (Fast Mode)",
        category = ModelCategory.STYLE_TRANSFER,
        filePath = "models/AnimeGANv2_Hayao.tflite",
        sizeBytes = 8_170_000,  // 8.17 MB from report
        version = "2.0",
        requiredDelegate = DelegateType.GPU,
        bundled = false,
        downloadUrl = "https://your-cdn.com/models/animegan_v2_fast.tflite"
    )
    
    val WHITEBOX_CARTOON = ModelInfo(
        id = "whitebox_cartoon",
        name = "White-box Cartoonization (2025 Optimized)",
        category = ModelCategory.STYLE_TRANSFER,
        filePath = "models/whitebox_cartoon.tflite",
        sizeBytes = 2_500_000,
        version = "2.1",
        requiredDelegate = DelegateType.CPU,
        bundled = true,
        downloadUrl = "https://storage.googleapis.com/cartoon_gan/fixed_shaped_models/with_metadata/whitebox_cartoon_gan_fp16.tflite"
    )
    
    val UGATIT_SELFIE = ModelInfo(
        id = "ugatit_selfie2anime",
        name = "U-GAT-IT v2 (Selfie to Anime)",
        category = ModelCategory.STYLE_TRANSFER,
        filePath = "models/ugatit_selfie2anime.tflite",
        sizeBytes = 20_000_000,
        version = "2.0",
        requiredDelegate = DelegateType.GPU,
        bundled = false,
        downloadUrl = "https://your-cdn.com/models/ugatit_selfie_v2.tflite"
    )
    
    // ========== PHASE 3: GENERATIVE SYNTHESIS (2025) ==========
    
    val WAIFU_DIFFUSION_TEXT_ENCODER = ModelInfo(
        id = "waifu_diff_text_encoder",
        name = "AnimeDiffusion XL - Text Encoder",
        category = ModelCategory.GENERATION,
        filePath = "models/waifu_diffusion/text_encoder.tflite",
        sizeBytes = 250_000_000,
        version = "2025.1",
        requiredDelegate = DelegateType.ANY,
        bundled = false,
        downloadUrl = "https://huggingface.co/qualcomm/Stable-Diffusion-1-5-Mobile/resolve/main/text_encoder.tflite"
    )
    
    val WAIFU_DIFFUSION_UNET_PART1 = ModelInfo(
        id = "waifu_diff_unet_part1",
        name = "AnimeDiffusion XL - UNet Part 1",
        category = ModelCategory.GENERATION,
        filePath = "models/waifu_diffusion/unet_part1.tflite",
        sizeBytes = 650_000_000,
        version = "2025.1",
        requiredDelegate = DelegateType.NNAPI,
        bundled = false,
        downloadUrl = "https://huggingface.co/qualcomm/Stable-Diffusion-1-5-Mobile/resolve/main/unet_part1.tflite"
    )
    
    val WAIFU_DIFFUSION_UNET_PART2 = ModelInfo(
        id = "waifu_diff_unet_part2",
        name = "AnimeDiffusion XL - UNet Part 2",
        category = ModelCategory.GENERATION,
        filePath = "models/waifu_diffusion/unet_part2.tflite",
        sizeBytes = 650_000_000,
        version = "2025.1",
        requiredDelegate = DelegateType.NNAPI,
        bundled = false,
        downloadUrl = "https://huggingface.co/qualcomm/Stable-Diffusion-1-5-Mobile/resolve/main/unet_part2.tflite"
    )
    
    val WAIFU_DIFFUSION_VAE_DECODER = ModelInfo(
        id = "waifu_diff_vae_decoder",
        name = "AnimeDiffusion XL - VAE Decoder",
        category = ModelCategory.GENERATION,
        filePath = "models/waifu_diffusion/vae_decoder.tflite",
        sizeBytes = 100_000_000,
        version = "2025.1",
        requiredDelegate = DelegateType.GPU,
        bundled = false,
        downloadUrl = "https://huggingface.co/qualcomm/Stable-Diffusion-1-5-Mobile/resolve/main/vae_decoder.tflite"
    )
    
    // ========== PHASE 4: ANIMATION & VTUBER ==========
    
    val MEDIAPIPE_FACE_LANDMARKER = ModelInfo(
        id = "mediapipe_face_landmarker",
        name = "MediaPipe Face Landmarker (2025)",
        category = ModelCategory.ANIMATION,
        filePath = "models/mediapipe/face_landmarker.task",
        sizeBytes = 5_000_000,
        version = "0.10.14",
        requiredDelegate = DelegateType.ANY,
        bundled = true,
        downloadUrl = "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/latest/face_landmarker.task"
    )
    
    val MEDIAPIPE_POSE_LANDMARKER = ModelInfo(
        id = "mediapipe_pose_landmarker",
        name = "MediaPipe Pose Landmarker (BlazePose 2025)",
        category = ModelCategory.ANIMATION,
        filePath = "models/mediapipe/pose_landmarker.task",
        sizeBytes = 12_000_000,
        version = "0.10.14",
        requiredDelegate = DelegateType.GPU,
        bundled = true,
        downloadUrl = "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_heavy/float16/latest/pose_landmarker_heavy.task"
    )
    
    val MOVENET_LIGHTNING = ModelInfo(
        id = "movenet_lightning",
        name = "MoveNet SinglePose Lightning v4",
        category = ModelCategory.ANIMATION,
        filePath = "models/movenet_lightning.tflite",
        sizeBytes = 4_500_000,
        version = "4.0",
        requiredDelegate = DelegateType.GPU,
        bundled = true,
        downloadUrl = "https://tfhub.dev/google/lite-model/movenet/singlepose/lightning/tflite/int8/4?lite-format=tflite"
    )
    
    // ========== PHASE 5: UPSCALING & SUPPORT ==========
    
    val REAL_ESRGAN_ANIME = ModelInfo(
        id = "real_esrgan_anime",
        name = "Real-ESRGAN x4 Anime (2025)",
        category = ModelCategory.UPSCALING,
        filePath = "models/real_esrgan_anime.tflite",
        sizeBytes = 16_700_000,
        version = "2.0",
        requiredDelegate = DelegateType.GPU,
        bundled = true,
        downloadUrl = "https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/realesr-animevideov3.tflite"
    )
    
    val SELFIE_SEGMENTATION = ModelInfo(
        id = "selfie_segmentation",
        name = "MediaPipe Selfie Segmentation",
        category = ModelCategory.SUPPORT,
        filePath = "models/mediapipe/selfie_segmentation.tflite",
        sizeBytes = 1_000_000,  // ~1 MB
        version = "0.10.9",
        requiredDelegate = DelegateType.GPU,
        bundled = true,
        downloadUrl = "https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite"
    )
    
    val MIDAS_DEPTH = ModelInfo(
        id = "midas_depth_small",
        name = "MiDaS Depth Estimation (Small)",
        category = ModelCategory.SUPPORT,
        filePath = "models/midas_v2_1_small.tflite",
        sizeBytes = 8_000_000,  // ~8 MB
        version = "2.1",
        requiredDelegate = DelegateType.GPU,
        bundled = false,
        downloadUrl = "https://your-cdn.com/models/midas_small.tflite"
    )
    
    // ========== MODEL COLLECTIONS ==========
    
    val CORE_MODELS = listOf(
        ANIMEGAN_HAYAO,
        ANIMEGAN_SHINKAI,
        ANIMEGAN_PAPRIKA
    )
    
    val PHASE_2_MODELS = listOf(
        ANIMEGAN_V2_FAST,
        WHITEBOX_CARTOON,
        UGATIT_SELFIE
    )
    
    val PHASE_3_MODELS = listOf(
        WAIFU_DIFFUSION_TEXT_ENCODER,
        WAIFU_DIFFUSION_UNET_PART1,
        WAIFU_DIFFUSION_UNET_PART2,
        WAIFU_DIFFUSION_VAE_DECODER
    )
    
    val PHASE_4_MODELS = listOf(
        MEDIAPIPE_FACE_LANDMARKER,
        MEDIAPIPE_POSE_LANDMARKER,
        MOVENET_LIGHTNING
    )
    
    val PHASE_5_MODELS = listOf(
        REAL_ESRGAN_ANIME,
        SELFIE_SEGMENTATION,
        MIDAS_DEPTH
    )
    
    val ALL_MODELS = CORE_MODELS + PHASE_2_MODELS + PHASE_3_MODELS + 
                     PHASE_4_MODELS + PHASE_5_MODELS
    
    /**
     * Get model by ID
     */
    fun getModel(id: String): ModelInfo? {
        return ALL_MODELS.find { it.id == id }
    }
    
    /**
     * Get models by category
     */
    fun getModelsByCategory(category: ModelCategory): List<ModelInfo> {
        return ALL_MODELS.filter { it.category == category }
    }
    
    /**
     * Get total size for a phase
     */
    fun getPhaseSize(phase: Int): Long {
        return when (phase) {
            2 -> PHASE_2_MODELS.sumOf { it.sizeBytes }
            3 -> PHASE_3_MODELS.sumOf { it.sizeBytes }
            4 -> PHASE_4_MODELS.sumOf { it.sizeBytes }
            5 -> PHASE_5_MODELS.sumOf { it.sizeBytes }
            else -> 0L
        }
    }
}
