"""
Real-ESRGAN Upscaling - Model Creation Script
Creates a simple test model for immediate testing
"""

import tensorflow as tf
import numpy as np

print("=" * 50)
print("Real-ESRGAN Model Creator")
print("=" * 50)
print()

# Check TensorFlow version
print(f"TensorFlow version: {tf.__version__}")
print()

print("🎨 Creating Real-ESRGAN Test Model...")

# Input: 256x256 RGB image (Tile size)
input_shape = (256, 256, 3)
scale_factor = 4
output_shape = (256 * scale_factor, 256 * scale_factor, 3)

# Create simple architecture (Upsampling)
model = tf.keras.Sequential([
    # Input layer
    tf.keras.layers.InputLayer(input_shape=input_shape),
    
    # Simple processing
    tf.keras.layers.Conv2D(32, 3, padding='same', activation='relu'),
    
    # Upsample 4x
    tf.keras.layers.UpSampling2D(size=(scale_factor, scale_factor)),
    
    # Output layer
    tf.keras.layers.Conv2D(3, 3, padding='same', activation='sigmoid') # 0-1 range
])

# Build model
model.build((1,) + input_shape)
print(f"✅ Model created with {model.count_params():,} parameters")

# Convert to TFLite
print("🔄 Converting to TensorFlow Lite...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]

tflite_model = converter.convert()

# Save
output_file = "real_esrgan_anime.tflite"
with open(output_file, 'wb') as f:
    f.write(tflite_model)

size_mb = len(tflite_model) / 1024 / 1024
print(f"✅ Model saved: {output_file}")
print(f"📦 Size: {size_mb:.2f} MB")

print()
print("=" * 50)
print("SUCCESS! ✅")
print("=" * 50)
print()
print("Next steps:")
print("1. Copy to: app/src/main/assets/models/")
print("   (Or let the app download it if you have the URL set up)")
print("2. Run: ./gradlew installDebug")
print("3. Enable '4K Upscaling' in the app!")
print()
