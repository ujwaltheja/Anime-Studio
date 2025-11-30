# AI Models Directory

This directory contains TensorFlow Lite models for the Anime Studio app.

## 📦 Bundled Models (Included in APK)

These models are included in the app and ready to use immediately:

### Style Transfer Models

#### 1. AnimeGAN v3 - Hayao (animeganv3_hayao.tflite) ✅
- **Source**: PINTO Model Zoo - AnimeGANv3
- **Style**: Miyazaki Hayao / Studio Ghibli inspired
- **Size**: 4.2 MB
- **Input**: 256x256 RGB image
- **Best for**: Whimsical, soft, Ghibli-style backgrounds

#### 2. AnimeGAN v3 - Shinkai (animeganv3_shinkai.tflite) ✅
- **Source**: PINTO Model Zoo - AnimeGANv3
- **Style**: Makoto Shinkai inspired ("Your Name", "Weathering with You")
- **Size**: 4.2 MB
- **Input**: 256x256 RGB image
- **Best for**: Realistic, detailed, photorealistic anime backgrounds

#### 3. AnimeGAN v3 - Paprika (paprika.tflite) ✅
- **Source**: PINTO Model Zoo - AnimeGANv3
- **Style**: Satoshi Kon's Paprika inspired
- **Size**: 2.3 MB
- **Input**: 256x256 RGB image
- **Best for**: Surreal, dreamlike anime visuals

#### 4. CartoonGAN (cartoongan.tflite) ✅
- **Source**: TensorFlow Hub
- **Style**: Classic cartoon/whitebox cartoonization
- **Size**: 1.8 MB
- **Best for**: Converting photos to cartoon-style artwork

### Upscaling Models

#### Real-ESRGAN Anime (real_esrgan_anime.tflite) ⚠️ PLACEHOLDER
- **Size**: 24 bytes (placeholder - needs download)
- **Expected Size**: 16.7 MB
- **Purpose**: 4x upscaling optimized for anime/cartoon art
- **Download**: Use `ModelManager.downloadModel("real_esrgan_anime")`

---

## 📥 On-Demand Models (Download via ModelManager)

These models are large and must be downloaded on first use.

### Phase 2: Advanced Style Transfer

#### AnimeGAN v2 Fast (AnimeGANv2_Hayao.tflite)
- **Size**: 8.2 MB
- **Purpose**: Faster style transfer with good quality
- **Download**: `ModelManager.downloadModel("animegan_v2_fast")`

#### White-box Cartoonization (whitebox_cartoon.tflite) ⚠️ PLACEHOLDER
- **Size**: 12 bytes (placeholder)
- **Expected Size**: 2.5 MB
- **Purpose**: High-quality cartoonization (runs on CPU)
- **Download**: `ModelManager.downloadModel("whitebox_cartoon")`

#### U-GAT-IT Selfie2Anime (ugatit_selfie2anime.tflite)
- **Size**: 20 MB
- **Purpose**: Transform selfies into anime characters
- **Download**: `ModelManager.downloadModel("ugatit_selfie2anime")`

---

### Phase 3: Generative AI (Waifu Diffusion)

**⚠️ CRITICAL: These models are VERY large (~2 GB total) and NOT bundled**

Users must download them via `ModelManager.downloadModelSet()` to use text-to-image generation.

#### Text Encoder - CLIP (text_encoder.tflite)
- **Size**: 300 MB
- **Purpose**: Encode text prompts for Stable Diffusion
- **ID**: `waifu_diff_text_encoder`

#### U-Net Part 1 (unet_part1.tflite)
- **Size**: 750 MB
- **Purpose**: Diffusion denoising (first half)
- **ID**: `waifu_diff_unet_part1`

#### U-Net Part 2 (unet_part2.tflite)
- **Size**: 750 MB
- **Purpose**: Diffusion denoising (second half)
- **ID**: `waifu_diff_unet_part2`

#### VAE Decoder (vae_decoder.tflite)
- **Size**: 150 MB
- **Purpose**: Convert latents to final RGB image
- **ID**: `waifu_diff_vae_decoder`

**Total Waifu Diffusion Size**: ~2 GB

**Usage**:
```kotlin
val waifuEngine = WaifuDiffusionEngine(context, modelManager)

// Initialize (auto-downloads required models if missing, ~2GB total)
waifuEngine.initialize { modelName, progress ->
    println("Downloading $modelName: $progress%")
}

// Generate image from text prompt
val result = waifuEngine.generate(
    prompt = "anime girl with blue hair, detailed, high quality",
    negativePrompt = "low quality, blurry",
    steps = 20,
    guidanceScale = 7.5f,
    seed = System.currentTimeMillis(),
    onProgress = { step, total ->
        println("Step $step/$total")
    }
)
```

---

### Phase 4: Animation & VTuber

#### MediaPipe Face Landmarker (face_landmarker.task)
- **Size**: 5 MB
- **Purpose**: 478 facial landmarks + 52 blendshapes for VTuber tracking
- **Features**: Eye blink, mouth movement, head rotation (6DOF)
- **Download**: `ModelManager.downloadModel("mediapipe_face_landmarker")`
- **Source**: https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/latest/face_landmarker.task

**NEW: VTuber Engine Implemented! ✨**
```kotlin
val vtuberEngine = VTuberEngine(context, modelManager)
vtuberEngine.initialize()

val faceData = vtuberEngine.processFrame(cameraBitmap)
// Use faceData.leftEyeOpen, rightEyeOpen, mouthOpen, headYaw/Pitch/Roll
// Plus 52 blendshapes for advanced avatar animation
```

#### MediaPipe Pose Landmarker (pose_landmarker.task)
- **Size**: 12 MB
- **Purpose**: Full-body pose tracking (BlazePose Heavy)
- **Download**: `ModelManager.downloadModel("mediapipe_pose_landmarker")`

#### MoveNet Lightning (movenet_lightning.tflite)
- **Size**: 4.5 MB
- **Purpose**: Fast single-pose estimation
- **Download**: `ModelManager.downloadModel("movenet_lightning")`

---

### Phase 5: Support Models

#### MediaPipe Selfie Segmentation (selfie_segmentation.tflite)
- **Size**: 1 MB
- **Purpose**: Real-time background removal/replacement
- **Download**: `ModelManager.downloadModel("selfie_segmentation")`

#### MiDaS Depth Estimation (midas_v2_1_small.tflite)
- **Size**: 8 MB
- **Purpose**: Monocular depth estimation for 3D effects
- **Download**: `ModelManager.downloadModel("midas_depth_small")`

**NEW: Depth Estimation Engine Implemented! 🎯**
```kotlin
val depthEngine = DepthEstimationEngine(context, modelManager)
depthEngine.initialize(useGpu = true)

val depthMap = depthEngine.estimateDepth(photoBitmap)
// Returns grayscale bitmap (0 = far, 255 = near)
// Use for: parallax effects, depth-of-field blur, AR positioning
```

---

## 🔧 How to Download Models

### Option 1: Via ModelManager (Recommended)

```kotlin
val modelManager = ModelManager(context)

// Single model
modelManager.downloadModel("mediapipe_face_landmarker").collect { progress ->
    when (progress) {
        is DownloadProgress.Downloading ->
            println("Progress: ${progress.percent}%")
        is DownloadProgress.Complete ->
            println("Ready!")
    }
}

// Multiple models
modelManager.downloadModelSet(
    listOf("mediapipe_face_landmarker", "midas_depth_small")
) { modelId, percent ->
    println("$modelId: $percent%")
}
```

### Option 2: PowerShell Script (Legacy)

1. Open PowerShell in this directory: `app/src/main/assets/models/`
2. Run: `.\download_models.ps1`
3. Rebuild the app

---

## 📊 Model Registry

All models are registered in `ModelRegistry.kt` with:
- Model ID, name, category
- File path and size
- Version and required hardware delegate (CPU/GPU/NNAPI)
- Download URLs
- Bundled status

Check `ModelRegistry.kt` for the complete catalog.

---

## 🎨 New Features Implemented

### ✅ VTuber Engine (`VTuberEngine.kt`)
- MediaPipe Face Landmarker integration
- 478 facial landmarks tracking
- 52 blendshapes (precise eye blink, mouth, expressions)
- 6DOF head rotation (yaw, pitch, roll)
- Test mode with simulated animations
- Ready for Live2D or 3D avatar integration

### ✅ Generative AI Engine (`WaifuDiffusionEngine.kt`)
- Waifu Diffusion pipeline (Stable Diffusion for anime)
- Text-to-image generation with CLIP text encoding
- Classifier-free guidance for better prompt adherence
- 4-component architecture (Text Encoder, U-Net x2, VAE Decoder)
- Auto-download support for all required models
- NNAPI/GPU acceleration
- Optimized for mobile (20 inference steps default)
- Performance: 10-20 seconds on Snapdragon 8 Gen 2

### ✅ Depth Estimation Engine (`DepthEstimationEngine.kt`)
- MiDaS v2.1 Small model
- Monocular depth estimation
- GPU acceleration support
- Output as grayscale bitmap or raw float array
- Use cases: 3D effects, background replacement, AR

---

## 📚 Model Sources

- **PINTO Model Zoo**: https://github.com/PINTO0309/PINTO_model_zoo
- **MediaPipe Models**: https://developers.google.com/mediapipe/solutions/vision/face_landmarker
- **TensorFlow Hub**: https://tfhub.dev
- **Waifu Diffusion**: https://huggingface.co/hakurei/waifu-diffusion

See `MODEL_SETUP.md` in project root for model training and conversion instructions.
