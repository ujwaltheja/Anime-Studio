# 🛠️ Setup Requirements

## 📦 Dependencies

To build and run Anime Studio, you need the following:

1.  **Android Studio**: Hedgehog or newer.
2.  **JDK**: Version 17.
3.  **Android SDK**: API 34 (UpsideDownCake).

## 🧠 ML Models

The app uses several TensorFlow Lite models.

### **1. Included (Bundled) Models**
These are included in `app/src/main/assets/models/` for immediate testing:
- **AnimeGAN** (Hayao, Shinkai, Paprika)
- **Real-ESRGAN** (Dummy version included for testing. Replace with real model for actual upscaling.)

### **2. Downloadable Models**
These are too large to bundle and are downloaded on demand (or simulated in Test Mode):
- **Waifu Diffusion** (Text-to-Image)
- **MediaPipe Face Landmarker** (VTuber)

### **⚠️ Important: Real-ESRGAN Setup**
The included `real_esrgan_anime.tflite` is a **dummy model** to allow the app to build and run. It does **NOT** perform actual upscaling.

**To enable real upscaling:**
1.  Download the real `Real-ESRGAN` TFLite model (approx 16MB).
2.  Rename it to `real_esrgan_anime.tflite`.
3.  Replace the file in `app/src/main/assets/models/`.
4.  Rebuild the app.

### **⚠️ Important: Waifu Diffusion Setup**
To enable real image generation:
1.  Download the 4 model parts (Text Encoder, UNet Part 1 & 2, VAE Decoder).
2.  Place them in `app/src/main/assets/models/waifu_diffusion/` (create directory if needed).
3.  Update `ModelRegistry.kt` to set `bundled = true` for these models OR host them on a server and update the `downloadUrl`.

## 📹 FFmpeg
The app uses `ffmpeg-kit-full`. Ensure the AAR is present in `app/libs/` or the dependency is correctly resolved from Maven Central.
