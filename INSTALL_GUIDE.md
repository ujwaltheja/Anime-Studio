# Installation Guide - Anime Studio App

**APK Location**: `d:\Github\Anime-Studio\app\build\outputs\apk\debug\app-debug.apk`
**Size**: 188 MB
**Status**: ✅ Ready to Install

---

## Method 1: USB Installation (Recommended)

### Step 1: Enable USB Debugging on Your Phone

1. Go to **Settings** → **About Phone**
2. Tap **Build Number** 7 times (enables Developer Options)
3. Go back to **Settings** → **Developer Options**
4. Enable **USB Debugging**
5. Connect your phone to computer via USB

### Step 2: Authorize Your Computer

1. When you connect, a popup appears: **"Allow USB debugging?"**
2. **Check** "Always allow from this computer"
3. Tap **"Allow"** or **"OK"**

### Step 3: Install via ADB

Open Command Prompt or PowerShell:

```bash
cd "d:\Github\Anime-Studio"
adb devices
```

You should see:
```
List of devices attached
RZCR5008BZK    device
```

Then install:
```bash
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## Method 2: Direct APK Installation

### Step 1: Copy APK to Phone

**Option A: Via USB**
1. Connect phone to computer
2. Copy `app\build\outputs\apk\debug\app-debug.apk` to your phone's Download folder
3. Disconnect phone

**Option B: Via Cloud**
1. Upload APK to Google Drive / Dropbox
2. Download on your phone

### Step 2: Enable Unknown Sources

1. Go to **Settings** → **Security**
2. Enable **"Install unknown apps"** or **"Unknown sources"**
3. Allow for your file manager or browser

### Step 3: Install APK

1. Open **File Manager** on your phone
2. Navigate to **Downloads** folder
3. Tap on **app-debug.apk**
4. Tap **"Install"**
5. Wait for installation to complete
6. Tap **"Open"** or find app in app drawer

---

## Method 3: Wireless ADB (No USB Cable)

### Step 1: Enable Wireless Debugging (Android 11+)

1. Go to **Settings** → **Developer Options**
2. Enable **Wireless debugging**
3. Tap **"Pair device with pairing code"**
4. Note the **IP address and port** (e.g., 192.168.1.100:12345)

### Step 2: Pair from Computer

```bash
adb pair 192.168.1.100:12345
# Enter the 6-digit pairing code shown on phone
```

### Step 3: Connect and Install

```bash
adb connect 192.168.1.100:12345
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## Troubleshooting

### Device Shows "unauthorized"
**Fix**:
- Check phone for USB debugging authorization popup
- Tap "Allow"
- Or go to Settings → Developer Options → Revoke USB debugging authorizations → Reconnect

### Device Shows "offline"
**Fix**:
```bash
adb kill-server
adb start-server
adb devices
```

### "adb: command not found"
**Fix**: Install Android SDK Platform Tools
- Download: https://developer.android.com/studio/releases/platform-tools
- Extract and add to PATH

### Installation Blocked
**Fix**:
- Settings → Security → Enable "Unknown sources"
- Or Settings → Apps → Special access → Install unknown apps → Enable for File Manager

### Not Enough Space
**Fix**:
- Free up at least 300 MB on your phone
- Delete unused apps or files

---

## After Installation

### First Launch:

1. **Find the app**: Look for "Anime Studio" icon (gallery icon temporarily)
2. **Tap to open**
3. **Grant permissions** when prompted:
   - ✅ Media permissions (to access videos)
   - ✅ Camera (for video recording)

### Quick Test:

1. Tap **"Select Video"**
2. Choose a short video (< 10 seconds recommended)
3. Select a style (try **"Hayao"** first)
4. Tap **"Process Video"**
5. Wait ~1-2 minutes
6. View your anime-styled video!

---

## Manual Installation Commands

### Check if device is connected:
```bash
adb devices
```

### Install APK:
```bash
adb install -r "d:\Github\Anime-Studio\app\build\outputs\apk\debug\app-debug.apk"
```

### Launch app after install:
```bash
adb shell am start -n com.animestudio/.MainActivity
```

### View app logs:
```bash
adb logcat | grep -E "AnimStudio|VideoProcessor"
```

### Uninstall app:
```bash
adb uninstall com.animestudio
```

### Grant permissions manually:
```bash
adb shell pm grant com.animestudio android.permission.CAMERA
adb shell pm grant com.animestudio android.permission.READ_MEDIA_VIDEO
adb shell pm grant com.animestudio android.permission.READ_MEDIA_IMAGES
```

---

## Installation Success Indicators

### ✅ Success:
```
Performing Streamed Install
Success
```

### ✅ App appears in app drawer
### ✅ Icon shows (temporary gallery icon)
### ✅ App launches without crashing

---

## Common Installation Errors

### Error: "INSTALL_FAILED_INSUFFICIENT_STORAGE"
**Solution**: Free up space on your phone

### Error: "INSTALL_FAILED_UPDATE_INCOMPATIBLE"
**Solution**: Uninstall old version first
```bash
adb uninstall com.animestudio
```

### Error: "INSTALL_FAILED_INVALID_APK"
**Solution**: APK might be corrupted, rebuild:
```bash
cd "d:\Github\Anime-Studio"
./gradlew clean assembleDebug
```

### Error: "device offline"
**Solution**: Reconnect USB cable or restart ADB

---

## Quick Copy-Paste Commands

### Full Installation Sequence:
```bash
cd "d:\Github\Anime-Studio"
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.animestudio/.MainActivity
```

### With Permissions:
```bash
cd "d:\Github\Anime-Studio"
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell pm grant com.animestudio android.permission.CAMERA
adb shell pm grant com.animestudio android.permission.READ_MEDIA_VIDEO
adb shell pm grant com.animestudio android.permission.READ_MEDIA_IMAGES
adb shell am start -n com.animestudio/.MainActivity
```

---

## APK Information

- **Package Name**: com.animestudio
- **Version**: 1.0
- **Size**: 188 MB
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

---

## What's Included

✅ Complete video processing pipeline
✅ 7 AI models for anime/cartoon styles
✅ FFmpeg Kit for video encoding
✅ TensorFlow Lite for ML inference
✅ Modern Material 3 UI
✅ Runtime permission handling
✅ Crash-free (GPU disabled by default)

---

## Next Steps After Install

1. ✅ Launch app
2. ✅ Grant permissions
3. ✅ Select a test video
4. ✅ Try different styles
5. ✅ Share your results!

---

**Generated**: November 29, 2025
**APK**: app-debug.apk (188 MB)
**Status**: ✅ Ready to Install
