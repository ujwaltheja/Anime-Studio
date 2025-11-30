import os
import urllib.request
import ssl

# Ignore SSL errors
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

models_dir = os.path.join("app", "src", "main", "assets", "models")
os.makedirs(models_dir, exist_ok=True)

models = {
    "animeganv3_hayao.tflite": "https://github.com/TachibanaYoshino/AnimeGANv3/releases/download/v1.1.0/AnimeGANv3_Hayao_36.tflite",
    "animeganv3_shinkai.tflite": "https://github.com/TachibanaYoshino/AnimeGANv3/releases/download/v1.1.0/AnimeGANv3_Shinkai_37.tflite",
    "animeganv3_portrait.tflite": "https://github.com/TachibanaYoshino/AnimeGANv3/releases/download/v1.1.0/AnimeGANv3_PortraitSketch_25.tflite",
    "whitebox_cartoon.tflite": "https://storage.googleapis.com/cartoon_gan/fixed_shaped_models/with_metadata/whitebox_cartoon_gan_fp16.tflite",
    "movenet_lightning.tflite": "https://tfhub.dev/google/lite-model/movenet/singlepose/lightning/tflite/int8/4?lite-format=tflite",
    "mediapipe/face_landmarker.task": "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/latest/face_landmarker.task",
    "mediapipe/pose_landmarker.task": "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_heavy/float16/latest/pose_landmarker_heavy.task",
    "mediapipe/selfie_segmentation.tflite": "https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite"
}

print(f"Downloading models to {models_dir}...")

for filename, url in models.items():
    filepath = os.path.join(models_dir, filename)
    if os.path.exists(filepath) and os.path.getsize(filepath) > 1000:
        print(f"Skipping {filename} (already exists)")
        continue
        
    print(f"Downloading {filename}...")
    try:
        urllib.request.urlretrieve(url, filepath, context=ctx)
        print(f"Downloaded {filename}")
    except Exception as e:
        print(f"Failed to download {filename}: {e}")

print("Done!")
