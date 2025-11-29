# AI Models Directory

This directory contains TensorFlow Lite models for anime/cartoon style transfer.

## High-Quality Models (AnimeGANv2)

To get the best results, run the `download_models.ps1` script in this directory. It will download the official AnimeGANv2 models from the PINTO Model Zoo.

### 1. Hayao Style (animeganv2_hayao.tflite)
- **Source**: PINTO Model Zoo - AnimeGANv2
- **Style**: Miyazaki Hayao / Studio Ghibli inspired
- **Input**: 256x256 RGB image
- **Best for**: Whimsical, soft, Ghibli-style backgrounds. High quality.

### 2. Shinkai Style (animeganv2_shinkai.tflite)
- **Source**: PINTO Model Zoo - AnimeGANv2
- **Style**: Makoto Shinkai inspired ("Your Name", "Weathering with You")
- **Input**: 256x256 RGB image
- **Best for**: Realistic, detailed, photorealistic anime backgrounds.

### 3. Paprika Style (animeganv2_paprika.tflite)
- **Source**: PINTO Model Zoo - AnimeGANv2
- **Style**: Satoshi Kon's Paprika inspired
- **Input**: 256x256 RGB image
- **Best for**: Surreal, dreamlike anime visuals.

### 4. CartoonGAN (cartoongan.tflite)
- **Source**: TensorFlow Hub
- **Style**: Classic cartoon/whitebox cartoonization
- **Input**: 512x512 RGB image
- **Best for**: Converting photos to cartoon-style artwork.

## Legacy Models

- **animegan.tflite**: Older AnimeGAN version.
- **style_transfer.tflite**: Generic artistic style transfer.

## How to Download Models

1. Open PowerShell in this directory: `app/src/main/assets/models/`
2. Run: `.\download_models.ps1`
3. Rebuild the app.

## Model Sources

- **PINTO Model Zoo**: https://github.com/PINTO0309/PINTO_model_zoo/tree/main/050_AnimeGANv2
- **AnimeGANv2 Official**: https://github.com/TachibanaYoshino/AnimeGANv2
- **TensorFlow Hub**: https://tfhub.dev

See MODEL_SETUP.md in project root for model training and conversion instructions.
