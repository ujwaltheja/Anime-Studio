# ⚠️ Missing Models (Action Required)

The automated download scripts are not working in your environment. Please manually download the following files to complete the setup.



## 2. Generative Models (Stable Diffusion)
These are required for the "Text-to-Image" feature.
**Location**: `app/src/main/assets/models/waifu_diffusion/`

We will use the verified `milad-m` Stable Diffusion models. Please download them and **rename** them as specified below.

| Download Link | Save As (Rename to this) |
|---------------|--------------------------|
| [text_encoder.tflite](https://huggingface.co/milad-m/stable-diffusion-v1-4-tflite/resolve/main/text_encoder.tflite) | `text_encoder.tflite` |
| [unet_1.tflite](https://huggingface.co/milad-m/stable-diffusion-v1-4-tflite/resolve/main/unet_1.tflite) | `unet_part1.tflite` |
| [unet_2.tflite](https://huggingface.co/milad-m/stable-diffusion-v1-4-tflite/resolve/main/unet_2.tflite) | `unet_part2.tflite` |
| [decoder.tflite](https://huggingface.co/milad-m/stable-diffusion-v1-4-tflite/resolve/main/decoder.tflite) | `vae_decoder.tflite` |

**Note**: These are large files (~2GB total). Ensure you have enough space.

## ✅ Completed
- **Real-ESRGAN**: Installed.
- **White-box Cartoon**: Installed.
- **MediaPipe Models** (Face, Pose, Selfie): Installed.
- **MoveNet**: Installed.

