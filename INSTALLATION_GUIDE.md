# Installation Guide - Anime Studio App

## Quick Fix Applied ✅

**Issue**: App installed but not visible in launcher
**Solution**: Updated to use Android's built-in gallery icon (temporary)
**Status**: APK rebuilt and ready to install

---

## Installation Steps

### Method 1: Direct Installation (Recommended)

1. **Uninstall Previous Version** (if installed):
   ```bash
   adb uninstall com.animestudio
   ```

2. **Install New APK**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Look for "Anime Studio"** in your app drawer
   - Icon: Gallery/Image icon (temporary)
   - Name: "Anime Studio"

### Method 2: Manual Installation

1. **Copy APK to device**:
   ```bash
   adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/
   ```

2. **On your device**:
   - Open Files or Downloads app
   - Tap on `app-debug.apk`
   - Allow installation from unknown sources if prompted
   - Tap "Install"

3. **Open the app**:
   - Find "Anime Studio" in app drawer
   - Or search for "Anime Studio" in device settings

---

## Troubleshooting

### App Not Showing in Launcher

**Try these steps**:

1. **Restart Device**:
   ```bash
   adb reboot
   ```

2. **Clear Launcher Cache**:
   - Go to Settings → Apps → Your Launcher
   - Clear Cache
   - Restart launcher

3. **Force Stop Launcher**:
   - Settings → Apps → Your Launcher
   - Force Stop
   - Reopen launcher

4. **Check if app is installed**:
   ```bash
   adb shell pm list packages | grep animestudio
   ```
   Should show: `package:com.animestudio`

5. **Launch app directly via ADB**:
   ```bash
   adb shell am start -n com.animestudio/.MainActivity
   ```

### App Crashes on Launch

**Check logs**:
```bash
adb logcat | grep animestudio
```

**Common issues**:
- Low memory: Close other apps
- Android version too old: Requires Android 8.0+ (API 26)
- Permissions: Grant storage/media permissions when prompted

### Installation Failed

1. **Check available storage**:
   - APK size: 178 MB
   - Recommended free space: 500+ MB

2. **Try clean install**:
   ```bash
   adb uninstall com.animestudio
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Check ADB connection**:
   ```bash
   adb devices
   ```

---

## Verifying Installation

### Check Package is Installed
```bash
adb shell pm list packages | grep animestudio
```
**Expected**: `package:com.animestudio`

### Check Main Activity Exists
```bash
adb shell dumpsys package com.animestudio | grep -A 5 "Activity"
```
**Expected**: Should show `com.animestudio.MainActivity`

### Launch App Directly
```bash
adb shell am start -n com.animestudio/.MainActivity
```
**Expected**: App should launch

---

## Known Issue: Temporary Icon

**Current Status**:
- App uses Android's built-in gallery/image icon (temporary)
- Icon will be small and generic
- This is intentional to fix launcher visibility

**To Get Custom Icon**:

We need to create proper PNG launcher icons. Two options:

### Option A: Quick Fix (Use Android Asset Studio)

1. Visit: https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html
2. Upload an image or use text/clipart
3. Download icon set
4. Extract to `app/src/main/res/`
5. Update AndroidManifest.xml:
   ```xml
   android:icon="@mipmap/ic_launcher"
   android:roundIcon="@mipmap/ic_launcher_round"
   ```
6. Rebuild: `./gradlew assembleDebug`

### Option B: Create Icons Manually

**Required sizes**:
- mdpi: 48x48 px
- hdpi: 72x72 px
- xhdpi: 96x96 px
- xxhdpi: 144x144 px
- xxxhdpi: 192x192 px

**Save as**:
```
app/src/main/res/mipmap-mdpi/ic_launcher.png
app/src/main/res/mipmap-hdpi/ic_launcher.png
app/src/main/res/mipmap-xhdpi/ic_launcher.png
app/src/main/res/mipmap-xxhdpi/ic_launcher.png
app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
```

Then rebuild.

---

## App Information

### Package Details
- **Package**: com.animestudio
- **Name**: Anime Studio
- **Version**: 1.0
- **Min Android**: 8.0 (API 26)
- **Target Android**: 14 (API 34)

### Features
- Video processing with AI
- Image style transfer
- FFmpeg integration
- 2 AI models included

### Permissions Required
- Read Media Images (Android 13+)
- Read Media Video (Android 13+)
- Read External Storage (Android 12 and below)
- Camera (optional)
- Internet (for future features)

---

## Testing After Installation

### Quick Test
1. Open "Anime Studio" app
2. Should see main screen
3. Tap "Select Video" or "Select Image"
4. Grant permissions when prompted
5. Select a test image/video
6. Choose a style
7. Wait for processing
8. View result

### If App Opens Successfully
✅ Installation successful!
- All features should work
- Video processing enabled
- AI models loaded

### If App Doesn't Open
1. Check logs: `adb logcat | grep -E "animestudio|AndroidRuntime"`
2. Check permissions in Settings → Apps → Anime Studio
3. Try launching directly: `adb shell am start -n com.animestudio/.MainActivity`

---

## Reinstallation

If you need to reinstall:

```bash
# Full clean reinstall
adb uninstall com.animestudio
adb install app/build/outputs/apk/debug/app-debug.apk

# Or force reinstall (keeps data)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Next Steps

After successful installation:

1. **Grant Permissions**: Allow storage/media access
2. **Test Features**: Try image and video processing
3. **Custom Icon** (optional): Follow steps above to add custom launcher icon
4. **Report Issues**: If problems persist, check logcat output

---

## Support

### Get Help

1. **Check Logs**:
   ```bash
   adb logcat -s animestudio:* AndroidRuntime:E
   ```

2. **System Info**:
   ```bash
   adb shell getprop ro.build.version.release  # Android version
   adb shell getprop ro.build.version.sdk      # API level
   adb shell df /data                          # Storage space
   ```

3. **App Info**:
   ```bash
   adb shell dumpsys package com.animestudio | grep -E "versionCode|versionName|targetSdk"
   ```

### Common Error Messages

| Error | Solution |
|-------|----------|
| "App not installed" | Clear space, check Android version |
| "Installation failed" | Uninstall old version first |
| "App keeps stopping" | Check logs, verify Android 8.0+ |
| "Can't find app" | Restart device, use ADB to launch |

---

## Summary

✅ **New APK built** with visible launcher icon
- Location: `app/build/outputs/apk/debug/app-debug.apk`
- Size: 178 MB
- Icon: Temporary (Android gallery icon)
- Status: Ready to install

📱 **Installation**: Use `adb install -r app/build/outputs/apk/debug/app-debug.apk`

🔍 **Find App**: Look for "Anime Studio" in app drawer (gallery icon)

🚀 **Launch**: Tap icon or use `adb shell am start -n com.animestudio/.MainActivity`
