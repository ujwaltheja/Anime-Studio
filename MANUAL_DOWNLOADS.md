# Manual Downloads Required

This document lists components that need to be downloaded manually to complete the setup.

## Status Overview

✅ **Completed**:
- Gradle wrapper
- Android dependencies
- Basic launcher icons
- CartoonGAN TFLite model
- Arbitrary Style Transfer TFLite model

⚠️ **Requires Manual Setup**:
- FFmpeg Kit library
- Additional AnimeGAN models (Hayao, Shinkai, Paprika)

---

## 1. FFmpeg Kit (For Video Processing)

### Current Status
The app includes a **stub implementation** that allows compilation but video processing features will not work.

### Download Instructions

1. **Visit GitHub Releases**:
   - Go to: https://github.com/arthenica/ffmpeg-kit/releases
   - Find latest release (e.g., v6.0-2)

2. **Download the AAR file**:
   - Look for: `ffmpeg-kit-full-6.0-2.aar` (or latest version)
   - Size: ~70-100 MB

3. **Installation**:
   ```bash
   # Create libs directory
   mkdir -p app/libs

   # Copy downloaded AAR
   cp ~/Downloads/ffmpeg-kit-full-6.0-2.aar app/libs/
   ```

4. **Update build.gradle.kts**:
   ```kotlin
   dependencies {
       // Remove or comment out the stub comment
       // Add this line:
       implementation(files("libs/ffmpeg-kit-full-6.0-2.aar"))
   }
   ```

5. **Remove stub file**:
   ```bash
   rm app/src/main/java/com/arthenica/ffmpegkit/FFmpegKit.kt
   ```

6. **Rebuild**:
   ```bash
   ./gradlew clean assembleDebug
   ```

### Alternative: Use Mobile-FFmpeg

If FFmpeg Kit is unavailable, you can try the older Mobile-FFmpeg library (no longer maintained):

```kotlin
dependencies {
    implementation("com.arthenica:mobile-ffmpeg-full:4.4.LTS")
}
```

Note: This may require code changes as the API differs from FFmpeg Kit.

---

## 2. Additional AnimeGAN Models (Optional)

### Currently Installed Models
- ✅ CartoonGAN (cartoongan.tflite) - 1.8 MB
- ✅ Arbitrary Style Transfer (style_transfer.tflite) - 2.7 MB

### Additional Models to Download

#### Option A: Pre-converted TFLite Models

Search these sources:
1. **TensorFlow Hub**: https://tfhub.dev/
   - Search for "anime style transfer tflite"
   - Search for "cartoon gan tflite"

2. **Hugging Face**: https://huggingface.co/models
   - Filter by: Task: Image-to-Image, Library: TFLite
   - Search for "anime" or "cartoon"

3. **GitHub**:
   - Search: "animegan tflite android"
   - Look for converted models in Issues/Releases

#### Option B: Convert Models Yourself

**Prerequisites**:
```bash
pip install tensorflow==2.14.0
pip install onnx2tf  # For ONNX models
```

**Download Source Models**:

1. **AnimeGANv2** (PyTorch/TensorFlow):
   ```bash
   git clone https://github.com/TachibanaYoshino/AnimeGANv2
   cd AnimeGANv2

   # Download pre-trained models
   wget https://github.com/TachibanaYoshino/AnimeGANv2/releases/download/1.0/Hayao.zip
   wget https://github.com/TachibanaYoshino/AnimeGANv2/releases/download/1.0/Shinkai.zip
   wget https://github.com/TachibanaYoshino/AnimeGANv2/releases/download/1.0/Paprika.zip

   unzip Hayao.zip
   unzip Shinkai.zip
   unzip Paprika.zip
   ```

2. **Convert to TFLite**:
   ```python
   import tensorflow as tf

   # Load saved model
   converter = tf.lite.TFLiteConverter.from_saved_model('path/to/Hayao')

   # Optimize for mobile
   converter.optimizations = [tf.lite.Optimize.DEFAULT]
   converter.target_spec.supported_types = [tf.float16]

   # Convert
   tflite_model = converter.convert()

   # Save
   with open('hayao.tflite', 'wb') as f:
       f.write(tflite_model)
   ```

3. **Install in App**:
   ```bash
   cp hayao.tflite app/src/main/assets/models/
   cp shinkai.tflite app/src/main/assets/models/
   cp paprika.tflite app/src/main/assets/models/
   ```

**Expected File Sizes**:
- Hayao: 2-8 MB
- Shinkai: 2-8 MB
- Paprika: 2-8 MB

---

## 3. Verification

After adding all components, verify the installation:

### Check Models
```bash
ls -lh app/src/main/assets/models/
```

Expected output:
```
cartoongan.tflite      (1.8 MB) ✓
style_transfer.tflite  (2.7 MB) ✓
hayao.tflite          (2-8 MB) [optional]
shinkai.tflite        (2-8 MB) [optional]
paprika.tflite        (2-8 MB) [optional]
```

### Check FFmpeg Kit
```bash
ls -lh app/libs/
```

Expected output:
```
ffmpeg-kit-full-6.0-2.aar  (70-100 MB) [for video processing]
```

### Build APK
```bash
./gradlew assembleDebug
```

---

## Notes

### Why Manual Downloads?

1. **FFmpeg Kit**: Not available in Maven Central or other standard repositories. GitHub releases only.

2. **AnimeGAN Models**: Most models are in PyTorch or ONNX format and need conversion. Pre-converted TFLite models are rare.

### Can I Use the App Without These?

**Yes, with limitations**:
- ✅ Basic image style transfer works with included models
- ❌ Video processing will not work (requires FFmpeg Kit)
- ⚠️ Limited style options (only 2 styles instead of 5+)

### Alternative Approach

Instead of video processing, you can:
1. Extract frames using Android MediaMetadataRetriever (already implemented)
2. Process each frame with TFLite models (already implemented)
3. Use external app to combine frames (user responsibility)

This approach works but is less convenient than integrated video reconstruction.

---

## Support

If you encounter issues:

1. Check [GitHub Issues](https://github.com/arthenica/ffmpeg-kit/issues) for FFmpeg Kit
2. Check [AnimeGANv2 Issues](https://github.com/TachibanaYoshino/AnimeGANv2/issues) for models
3. Review [MODEL_SETUP.md](MODEL_SETUP.md) for detailed conversion instructions
4. Open an issue in this repository with:
   - Downloaded file details
   - Error messages
   - Build logs

---

## Quick Commands Summary

```bash
# Download models (if available as TFLite)
cd app/src/main/assets/models/
wget [model-url] -O hayao.tflite

# Setup FFmpeg Kit
mkdir -p app/libs
cp ~/Downloads/ffmpeg-kit-full-6.0-2.aar app/libs/

# Remove stub
rm app/src/main/java/com/arthenica/ffmpegkit/FFmpegKit.kt

# Update build.gradle.kts (add to dependencies)
# implementation(files("libs/ffmpeg-kit-full-6.0-2.aar"))

# Rebuild
./gradlew clean assembleDebug

# Verify APK
ls -lh app/build/outputs/apk/debug/app-debug.apk
```
