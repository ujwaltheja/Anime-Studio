# ML Models Directory

Place your TensorFlow Lite (.tflite) model files here.

## Required Models

To use this app, you need to add the following models:

1. **cartoongan.tflite** - CartoonGAN style transfer model
2. **animegan.tflite** - AnimeGAN style transfer model
3. **hayao.tflite** - Hayao Miyazaki style (Studio Ghibli)
4. **shinkai.tflite** - Makoto Shinkai style
5. **paprika.tflite** - Paprika style
6. **custom.tflite** - Your custom model (optional)

## Where to Get Models

See [MODEL_SETUP.md](../../../../MODEL_SETUP.md) in the root directory for:
- Download links for pre-trained models
- Instructions for converting models to TensorFlow Lite
- Model optimization techniques
- Testing procedures

## Model Requirements

- **Format**: TensorFlow Lite (.tflite)
- **Input**: RGB images (typically 512x512 or 450x450)
- **Output**: Stylized RGB images (same size as input)
- **Size**: Recommended < 10MB after optimization

## Quick Start

1. Download or convert models using MODEL_SETUP.md guide
2. Copy .tflite files to this directory
3. Verify files are in place:
   - cartoongan.tflite
   - animegan.tflite
   - etc.
4. Build and run the app

## Model Testing

Before deploying, test each model:

```python
import tensorflow as tf

# Load model
interpreter = tf.lite.Interpreter(model_path="cartoongan.tflite")
interpreter.allocate_tensors()

# Check input/output details
print(interpreter.get_input_details())
print(interpreter.get_output_details())
```

Expected input shape: [1, height, width, 3]
Expected output shape: [1, height, width, 3]

## Troubleshooting

**Model not loading?**
- Verify file is in this exact directory
- Check file extension is .tflite
- Ensure file is not corrupted

**Poor quality output?**
- Test model outside the app first
- Check input preprocessing matches model training
- Try unquantized model for comparison

For more help, see MODEL_SETUP.md
