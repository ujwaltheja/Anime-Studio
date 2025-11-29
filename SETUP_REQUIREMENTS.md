# Anime Studio - Setup Requirements

This document outlines the required setup steps to get the Anime Studio app fully functional.

## Critical Missing Dependencies

### 1. FFmpeg Library

The app requires the FFmpeg library for video processing functionality.

**Required File:** `app/libs/ffmpeg-kit-full-6.0-2.LTS.aar`

**Download Instructions:**
1. Visit: https://github.com/arthenica/ffmpeg-kit/releases
2. Download `ffmpeg-kit-full-6.0-2.LTS.aar` from the releases page
3. Place the file in `app/libs/` directory
4. The `app/libs/` directory has been created for you

**Alternative:**
```bash
cd app/libs
wget https://github.com/arthenica/ffmpeg-kit/releases/download/v6.0-2.LTS/ffmpeg-kit-full-6.0-2.LTS.aar
```

### 2. Machine Learning Models

Some ML models are currently placeholders and need to be replaced with actual models.

#### Missing Models (Currently Dummy Files):

**a) White-box Cartoonization Model**
- **Location:** `app/src/main/assets/models/whitebox_cartoon.tflite`
- **Current Status:** Dummy placeholder (12 bytes)
- **Required Size:** ~2-5 MB
- **Download:** You need to convert the white-box cartoonization model to TFLite format
- **Reference:** https://github.com/SystemErrorWang/White-box-Cartoonization

**b) Real-ESRGAN Anime Upscaler**
- **Location:** `app/src/main/assets/models/real_esrgan_anime.tflite`
- **Current Status:** Dummy placeholder (24 bytes)
- **Required Size:** ~16.7 MB
- **Download:** Convert Real-ESRGAN x4 Anime model to TFLite
- **Reference:** https://github.com/xinntao/Real-ESRGAN

#### Available Models (Fully Functional):

✅ **AnimeGAN v3 Models** (Already included):
- `animeganv3_hayao.tflite` (4.2 MB) - Ghibli style
- `animeganv3_shinkai.tflite` (4.2 MB) - Makoto Shinkai style
- `paprika.tflite` (2.2 MB) - Paprika style
- `animegan.tflite` (2.2 MB) - Generic anime style
- `cartoongan.tflite` (1.8 MB) - Cartoon style
- `style_transfer.tflite` (2.8 MB) - General style transfer

## Setup Steps

### Step 1: Install FFmpeg Library

```bash
# Create libs directory (already created)
mkdir -p app/libs

# Download FFmpeg Kit AAR
cd app/libs
wget https://github.com/arthenica/ffmpeg-kit/releases/download/v6.0-2.LTS/ffmpeg-kit-full-6.0-2.LTS.aar
```

### Step 2: (Optional) Replace Placeholder Models

If you want to use White-box Cartoonization or Real-ESRGAN features:

1. **For White-box Cartoonization:**
   - Convert the model to TFLite format
   - Replace `app/src/main/assets/models/whitebox_cartoon.tflite`
   - Model should be ~2-5 MB in size

2. **For Real-ESRGAN:**
   - Convert the Real-ESRGAN x4 Anime model to TFLite
   - Replace `app/src/main/assets/models/real_esrgan_anime.tflite`
   - Model should be ~16.7 MB in size

### Step 3: Build the Project

```bash
# On Linux/Mac
./gradlew assembleDebug

# On Windows
gradlew.bat assembleDebug
```

## What Works Without Additional Setup

The following features work out of the box with the current setup:

✅ **Video Processing Pipeline**
- Frame extraction from videos
- Basic video reconstruction
- Audio extraction and merging

✅ **Style Transfer Models**
- AnimeGAN v3 (Hayao, Shinkai, Paprika styles)
- CartoonGAN
- Basic style transfer

✅ **User Interface**
- Video processing screen
- Generation screen (UI only)
- VTuber screen (UI only)
- Gallery screen

## What Requires Additional Setup

The following features require the dependencies mentioned above:

⚠️ **FFmpeg-Dependent Features** (Requires FFmpeg library):
- Video encoding/decoding
- Frame extraction (FFmpeg-based)
- Video reconstruction with audio
- Format conversion

⚠️ **Advanced ML Features** (Requires actual model files):
- White-box cartoonization (cel-shaded style)
- Real-ESRGAN upscaling (4x super-resolution)
- Waifu Diffusion (requires cloud models - not bundled)
- MediaPipe features (requires downloading MediaPipe models)

## Model Conversion Guide

### Converting Models to TFLite

If you have PyTorch or TensorFlow models and need to convert them:

**For PyTorch models:**
```python
import torch
from ai_edge_torch import convert

# Load your model
model = YourModel()
model.eval()

# Convert to TFLite
sample_input = torch.randn(1, 3, 512, 512)
edge_model = convert(model, (sample_input,))
edge_model.export("model.tflite")
```

**For TensorFlow models:**
```python
import tensorflow as tf

# Load saved model
converter = tf.lite.TFLiteConverter.from_saved_model('saved_model_dir')
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# Save
with open('model.tflite', 'wb') as f:
    f.write(tflite_model)
```

## Troubleshooting

### Build Fails with "FFmpeg library not found"
- Ensure `ffmpeg-kit-full-6.0-2.LTS.aar` is in `app/libs/` directory
- Clean and rebuild: `./gradlew clean assembleDebug`

### App Crashes on Video Processing
- Check that FFmpeg library is properly installed
- Verify video file is in a supported format (MP4, AVI, MOV)
- Ensure sufficient storage space for frame extraction

### Model Loading Fails
- Verify model files are not corrupted
- Check model file sizes match expected sizes
- Ensure models are in TFLite format (.tflite extension)

## Production Deployment

Before deploying to production:

1. ✅ Install FFmpeg library
2. ✅ Replace all placeholder models with real models
3. ✅ Test all features thoroughly
4. ✅ Optimize models for size/performance if needed
5. ✅ Configure ProGuard rules (already done)
6. ✅ Enable R8 minification (already configured)

## Support

For issues or questions:
- Check existing documentation in the repository
- Review build logs for specific errors
- Ensure all dependencies are properly installed

---

**Version:** 1.1.0
**Last Updated:** 2025-11-29
**Status:** Development Build - Requires Setup
