import os
import urllib.request
import ssl
import sys

# Ignore SSL errors
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

models_dir = os.path.join("app", "src", "main", "assets", "models")

models = {
    "movenet_lightning.tflite": "https://tfhub.dev/google/lite-model/movenet/singlepose/lightning/tflite/int8/4?lite-format=tflite",
    "mediapipe/face_landmarker.task": "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/latest/face_landmarker.task",
    "mediapipe/pose_landmarker.task": "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_heavy/float16/latest/pose_landmarker_heavy.task",
    "mediapipe/selfie_segmentation.tflite": "https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite"
}

print(f"Checking models in {models_dir}...")

for filename, url in models.items():
    filepath = os.path.join(models_dir, filename)
    
    # Ensure dir exists
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    
    if os.path.exists(filepath) and os.path.getsize(filepath) > 1000:
        print(f"Skipping {filename} (already exists)")
        continue
        
    print(f"Downloading {filename} from {url}...")
    try:
        urllib.request.urlretrieve(url, filepath, context=ctx)
        print(f"Downloaded {filename}")
    except Exception as e:
        print(f"Failed to download {filename}: {e}")

print("Done!")
