# 🛠️ Setup Requirements

## 📦 Dependencies

To build and run Anime Studio, you need the following:

1.  **Android Studio**: Hedgehog or newer.
2.  **JDK**: Version 17.
3.  **Android SDK**: API 34 (UpsideDownCake).

## 🧠 ML Models (2025 Edition)

The app uses state-of-the-art on-device AI models.

### **1. Bundled / Auto-Download Models**
These are handled by the `download_models.ps1` script or included:
- **AnimeGANv3** (Hayao, Shinkai, Portrait) - *Included*
- **Real-ESRGAN x4** (Upscaling) - *Bundled* (Ensure `real_esrgan_anime.tflite` is ~16MB+)
- **White-box Cartoonization** - *Downloadable via script*

### **2. Generative Models (AnimeDiffusion XL)**
These are large models for Text-to-Image generation. Due to size (~2GB total), they must be downloaded manually:

**Required Files:**
Place these in `app/src/main/assets/models/waifu_diffusion/`:
1.  `text_encoder.tflite` (~250MB)
2.  `unet_part1.tflite` (~650MB)
3.  `unet_part2.tflite` (~650MB)
4.  `vae_decoder.tflite` (~100MB)

**Download Source:**
Use the links provided in `ModelRegistry.kt` or search for "Stable Diffusion 1.5 Mobile TFLite" on HuggingFace.

### **⚠️ Setup Instructions**
1.  **Run the Download Script**:
    ```powershell
    ./app/src/main/assets/models/download_models.ps1
    ```
    This will fetch the White-box model and verify others.

2.  **Verify Real-ESRGAN**:
    Ensure `app/src/main/assets/models/real_esrgan_anime.tflite` is >10MB.

3.  **Optional: Enable Generation**:
    Create `app/src/main/assets/models/waifu_diffusion/` and place the 4 model parts there.

## 📹 FFmpeg
The app uses `ffmpeg-kit-full`. Ensure the AAR is present in `app/libs/`.
