# ML Model Setup Guide

Complete guide for setting up TensorFlow Lite models for anime/cartoon style transfer.

## Table of Contents

1. [Model Overview](#model-overview)
2. [Obtaining Models](#obtaining-models)
3. [Converting to TensorFlow Lite](#converting-to-tensorflow-lite)
4. [Model Optimization](#model-optimization)
5. [Integration](#integration)
6. [Testing Models](#testing-models)

## Model Overview

### Supported Model Types

| Model | Style | Input Size | Performance | Quality |
|-------|-------|------------|-------------|---------|
| CartoonGAN | Classic cartoon | 512x512 | Fast | Good |
| AnimeGANv2 | Modern anime | 512x512 | Medium | Excellent |
| White-box (Hayao) | Ghibli-style | 450x450 | Fast | Excellent |
| White-box (Shinkai) | Realistic anime | 450x450 | Fast | Excellent |
| White-box (Paprika) | Surreal anime | 450x450 | Fast | Excellent |

### Model Requirements

- **Format**: TensorFlow Lite (.tflite)
- **Input**: RGB images (typically 512x512)
- **Output**: Stylized RGB images
- **Size**: 1-10MB (after quantization)

## Obtaining Models

### Option 1: Download Pre-converted Models

#### CartoonGAN

```bash
# Download from official repository
git clone https://github.com/SystemErrorWang/White-box-Cartoonization
cd White-box-Cartoonization

# Download pre-trained weights
# Follow repository instructions
```

#### AnimeGANv2

```bash
# Clone repository
git clone https://github.com/TachibanaYoshino/AnimeGANv2
cd AnimeGANv2

# Download pre-trained models
# Available styles: Hayao, Shinkai, Paprika
wget https://github.com/TachibanaYoshino/AnimeGANv2/releases/download/1.0/Hayao.zip
unzip Hayao.zip
```

### Option 2: Use Pre-converted TFLite Models

Search for community-converted models:
- [TensorFlow Hub](https://tfhub.dev/)
- [Hugging Face Models](https://huggingface.co/models)
- GitHub repositories with "tflite" tag

## Converting to TensorFlow Lite

### Prerequisites

```bash
# Install TensorFlow
pip install tensorflow==2.14.0

# Optional: TensorFlow Model Optimization Toolkit
pip install tensorflow-model-optimization
```

### Method 1: From SavedModel

```python
import tensorflow as tf

# Path to your saved model
saved_model_dir = 'path/to/saved_model'

# Create converter
converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)

# Convert
tflite_model = converter.convert()

# Save
with open('model.tflite', 'wb') as f:
    f.write(tflite_model)
```

### Method 2: From Keras Model

```python
import tensorflow as tf

# Load Keras model
model = tf.keras.models.load_model('model.h5')

# Create converter
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# Convert
tflite_model = converter.convert()

# Save
with open('model.tflite', 'wb') as f:
    f.write(tflite_model)
```

### Method 3: From Frozen Graph

```python
import tensorflow as tf

# Create converter
converter = tf.lite.TFLiteConverter.from_frozen_graph(
    graph_def_file='frozen_graph.pb',
    input_arrays=['input'],
    output_arrays=['output'],
    input_shapes={'input': [1, 512, 512, 3]}
)

# Convert
tflite_model = converter.convert()

# Save
with open('model.tflite', 'wb') as f:
    f.write(tflite_model)
```

## Model Optimization

### 1. Float16 Quantization (Recommended)

**Benefits**: 50% size reduction, minimal quality loss

```python
import tensorflow as tf

converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)

# Enable float16 quantization
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float16]

tflite_model = converter.convert()

with open('model_fp16.tflite', 'wb') as f:
    f.write(tflite_model)
```

### 2. Dynamic Range Quantization

**Benefits**: Up to 4x size reduction, good quality

```python
converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)

# Enable dynamic range quantization
converter.optimizations = [tf.lite.Optimize.DEFAULT]

tflite_model = converter.convert()

with open('model_quantized.tflite', 'wb') as f:
    f.write(tflite_model)
```

### 3. Full Integer Quantization

**Benefits**: Maximum size reduction, fastest inference

```python
import numpy as np

def representative_dataset():
    """Generate representative dataset for calibration"""
    for _ in range(100):
        # Generate random images or use real samples
        data = np.random.rand(1, 512, 512, 3).astype(np.float32)
        yield [data]

converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)

# Enable full integer quantization
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.representative_dataset = representative_dataset

# Force integer-only quantization
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter.inference_input_type = tf.uint8
converter.inference_output_type = tf.uint8

tflite_model = converter.convert()

with open('model_int8.tflite', 'wb') as f:
    f.write(tflite_model)
```

### Comparison

| Method | Size Reduction | Quality | Speed | GPU Support |
|--------|---------------|---------|-------|-------------|
| None | 0% | Perfect | Baseline | Yes |
| Float16 | ~50% | Excellent | 1.2x | Yes |
| Dynamic Range | ~75% | Good | 2x | Limited |
| Full Integer | ~75% | Good | 3x | Limited |

## Integration

### 1. Place Models in Assets

```
app/src/main/assets/
└── models/
    ├── cartoongan.tflite
    ├── animegan.tflite
    ├── hayao.tflite
    ├── shinkai.tflite
    └── paprika.tflite
```

### 2. Configure Model Paths

In `VideoProcessingViewModel.kt`:

```kotlin
private fun getModelPathForStyle(styleType: StyleType): String {
    return when (styleType) {
        StyleType.CARTOON_GAN -> "models/cartoongan.tflite"
        StyleType.ANIME_GAN -> "models/animegan.tflite"
        StyleType.HAYAO -> "models/hayao.tflite"
        StyleType.SHINKAI -> "models/shinkai.tflite"
        StyleType.PAPRIKA -> "models/paprika.tflite"
        StyleType.CUSTOM -> "models/custom.tflite"
    }
}
```

### 3. Verify Model Loading

Add to `StyleTransferEngineImpl.kt`:

```kotlin
private fun loadModelFile(modelPath: String): MappedByteBuffer? {
    return try {
        // Load from assets
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    } catch (e: Exception) {
        Log.e("StyleTransfer", "Failed to load model: $modelPath", e)
        null
    }
}
```

## Testing Models

### 1. Command-line Testing (Python)

```python
import tensorflow as tf
import numpy as np
from PIL import Image

# Load TFLite model
interpreter = tf.lite.Interpreter(model_path="model.tflite")
interpreter.allocate_tensors()

# Get input/output details
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print(f"Input shape: {input_details[0]['shape']}")
print(f"Input type: {input_details[0]['dtype']}")
print(f"Output shape: {output_details[0]['shape']}")
print(f"Output type: {output_details[0]['dtype']}")

# Load and preprocess image
image = Image.open("test_image.jpg")
image = image.resize((512, 512))
input_data = np.array(image, dtype=np.float32)
input_data = np.expand_dims(input_data, axis=0)

# Normalize (adjust based on model requirements)
input_data = (input_data - 127.5) / 127.5

# Run inference
interpreter.set_tensor(input_details[0]['index'], input_data)
interpreter.invoke()

# Get output
output_data = interpreter.get_tensor(output_details[0]['index'])

# Denormalize
output_data = (output_data * 127.5 + 127.5).astype(np.uint8)

# Save result
output_image = Image.fromarray(output_data[0])
output_image.save("output.jpg")
```

### 2. Android Testing

Create a test in your app:

```kotlin
@Test
fun testModelLoading() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val engine = StyleTransferEngineImpl(context)

    val config = StyleConfig(
        styleType = StyleType.ANIME_GAN,
        modelPath = "models/animegan.tflite",
        useGPU = false
    )

    runBlocking {
        val result = engine.initialize(config)
        assertTrue(result is Result.Success)
    }
}
```

## Troubleshooting

### Issue: Model file not found

**Solution**:
```kotlin
// Verify assets
context.assets.list("models")?.forEach { fileName ->
    Log.d("Models", "Found: $fileName")
}
```

### Issue: Input/output shape mismatch

**Solution**: Check model input shape:
```python
interpreter.get_input_details()[0]['shape']
# Expected: [1, 512, 512, 3]
```

### Issue: Poor output quality

**Solutions**:
1. Check normalization range ([-1, 1] or [0, 1])
2. Verify model quantization settings
3. Test with original (non-quantized) model
4. Check input image preprocessing

### Issue: Slow inference

**Solutions**:
1. Enable GPU acceleration
2. Use quantized models
3. Reduce input size
4. Enable NNAPI

## Model Metadata (Optional)

Add metadata for better organization:

```python
from tflite_support import metadata as _metadata
from tflite_support import metadata_schema_py_generated as _metadata_fb

# Create metadata
model_meta = _metadata_fb.ModelMetadataT()
model_meta.name = "AnimeGAN Style Transfer"
model_meta.description = "Transforms images to anime style"
model_meta.version = "1.0.0"
model_meta.author = "Your Name"

# Add to model
b = flatbuffers.Builder(0)
b.Finish(model_meta.Pack(b))
metadata_buf = b.Output()

# Populate metadata
populator = _metadata.MetadataPopulator.with_model_file(model_path)
populator.load_metadata_buffer(metadata_buf)
populator.populate()
```

## Custom Model Training

For advanced users wanting to train custom models:

### Resources

1. **CartoonGAN Training**:
   - Paper: [CartoonGAN](https://openaccess.thecvf.com/content_cvpr_2018/papers/Chen_CartoonGAN_Generative_Adversarial_CVPR_2018_paper.pdf)
   - Code: [GitHub](https://github.com/FilipAndersson245/cartoon-gan)

2. **AnimeGAN Training**:
   - Paper: [AnimeGAN](https://github.com/TachibanaYoshino/AnimeGAN)
   - Code: [GitHub](https://github.com/TachibanaYoshino/AnimeGANv2)

3. **Datasets**:
   - [Danbooru](https://www.gwern.net/Danbooru2019)
   - [Anime Face Dataset](https://github.com/bchao1/Anime-Face-Dataset)

---

For questions or issues with models, please open a GitHub issue with:
- Model source
- Conversion command used
- Error messages
- Sample input/output
