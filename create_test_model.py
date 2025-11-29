"""
White-box Cartoonization - Model Creation Script
Creates a simple test model for immediate testing
"""

import tensorflow as tf
import numpy as np

print("=" * 50)
print("White-box Cartoonization Model Creator")
print("=" * 50)
print()

# Check TensorFlow version
print(f"TensorFlow version: {tf.__version__}")
print()

print("🎨 Creating White-box Cartoonization Test Model...")

# Input: 512x512 RGB image, normalized to [-1, 1]
input_shape = (512, 512, 3)

# Create simple architecture
model = tf.keras.Sequential([
    # Input layer
    tf.keras.layers.InputLayer(input_shape=input_shape),
    
    # Encoder (downsample)
    tf.keras.layers.Conv2D(64, 3, padding='same', activation='relu'),
    tf.keras.layers.Conv2D(64, 3, strides=2, padding='same', activation='relu'),  # 256x256
    
    # Processing
    tf.keras.layers.Conv2D(128, 3, padding='same', activation='relu'),
    tf.keras.layers.Conv2D(128, 3, padding='same', activation='relu'),
    
    # Decoder (upsample)
    tf.keras.layers.Conv2DTranspose(64, 3, strides=2, padding='same', activation='relu'),  # 512x512
    tf.keras.layers.Conv2D(64, 3, padding='same', activation='relu'),
    
    # Output layer (RGB, tanh activation for [-1, 1])
    tf.keras.layers.Conv2D(3, 3, padding='same', activation='tanh')
])

# Build model
model.build((1,) + input_shape)
print(f"✅ Model created with {model.count_params():,} parameters")

# Convert to TFLite
print("🔄 Converting to TensorFlow Lite...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# Optimization
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]

tflite_model = converter.convert()

# Save
output_file = "whitebox_cartoon.tflite"
with open(output_file, 'wb') as f:
    f.write(tflite_model)

size_mb = len(tflite_model) / 1024 / 1024
print(f"✅ Model saved: {output_file}")
print(f"📦 Size: {size_mb:.2f} MB")

# Verify
print("🔍 Verifying model...")
interpreter = tf.lite.Interpreter(model_path=output_file)
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print(f"✅ Input shape: {input_details[0]['shape']}")
print(f"✅ Output shape: {output_details[0]['shape']}")

# Test inference
test_input = np.random.randn(1, 512, 512, 3).astype(np.float32)
interpreter.set_tensor(input_details[0]['index'], test_input)
interpreter.invoke()
output = interpreter.get_tensor(output_details[0]['index'])

print(f"✅ Test inference successful!")
print(f"✅ Output range: [{output.min():.2f}, {output.max():.2f}]")

print()
print("=" * 50)
print("SUCCESS! ✅")
print("=" * 50)
print()
print("Next steps:")
print("1. Copy to: app/src/main/assets/models/")
print("2. Run: ./gradlew installDebug")
print("3. Test the Cel-Shaded style!")
print()
print("⚠️  NOTE: This is a TEST model for development")
print("   For production, download the real model from:")
print("   https://huggingface.co/sayakpaul/whitebox-cartoonizer")
