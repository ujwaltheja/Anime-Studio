# AI Models Directory

This directory contains TensorFlow Lite models for anime/cartoon style transfer.

## Currently Installed Models

### 1. CartoonGAN (cartoongan.tflite)
- **Size**: 1.8 MB
- **Source**: TensorFlow Hub - CartoonGAN
- **Style**: Classic cartoon/whitebox cartoonization
- **Input**: 512x512 RGB image
- **Format**: INT8 quantized
- **Best for**: Converting photos to cartoon-style artwork

### 2. Arbitrary Style Transfer (style_transfer.tflite)
- **Size**: 2.7 MB
- **Source**: TensorFlow Hub - Magenta Style Transfer
- **Style**: Arbitrary artistic style transfer
- **Input**: 256x256 RGB image
- **Format**: INT8 quantized
- **Best for**: General-purpose style transfer with custom style images

### 3. AnimeGAN (animegan.tflite)
- **Size**: 2.2 MB
- **Source**: PINTO Model Zoo - AnimeGANv2
- **Style**: General anime style conversion
- **Input**: 256x256 RGB image
- **Format**: Weight quantized
- **Best for**: Modern vibrant anime style

### 4. Hayao Style (hayao.tflite)
- **Size**: 2.2 MB
- **Source**: PINTO Model Zoo - AnimeGANv2 Hayao
- **Style**: Miyazaki Hayao / Studio Ghibli inspired
- **Input**: 256x256 RGB image
- **Format**: Weight quantized
- **Best for**: Whimsical, soft, Ghibli-style backgrounds

### 5. Shinkai Style (shinkai.tflite)
- **Size**: 2.2 MB
- **Source**: AnimeGANv2 Hayao (placeholder)
- **Style**: Makoto Shinkai inspired ("Your Name", "Weathering with You")
- **Input**: 256x256 RGB image
- **Format**: Weight quantized
- **Best for**: Realistic, detailed, photorealistic anime backgrounds
- **Note**: Currently using Hayao model as placeholder. For authentic Shinkai style, convert from checkpoint at https://github.com/TachibanaYoshino/AnimeGANv2

### 6. Paprika Style (paprika.tflite)
- **Size**: 2.2 MB
- **Source**: PINTO Model Zoo - AnimeGANv2 Paprika
- **Style**: Satoshi Kon's Paprika inspired
- **Input**: 256x256 RGB image
- **Format**: Weight quantized
- **Best for**: Surreal, dreamlike anime visuals

### 7. Custom Style (custom.tflite)
- **Size**: 1.8 MB (currently CartoonGAN copy)
- **Source**: User-provided
- **Style**: Your custom trained model
- **Best for**: Replace with your own trained style transfer model

## Model Sources

- **PINTO Model Zoo**: https://github.com/PINTO0309/PINTO_model_zoo/tree/main/050_AnimeGANv2
- **AnimeGANv2 Official**: https://github.com/TachibanaYoshino/AnimeGANv2
- **TensorFlow Hub**: https://tfhub.dev

See MODEL_SETUP.md in project root for model training and conversion instructions.
