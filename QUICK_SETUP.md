# 🚀 Quick Setup Guide - Anime Studio

Get your Anime Studio app running in **5 minutes**!

## Prerequisites Check ✓

Before you begin, ensure you have:
- [ ] **Windows PC** (for model download script)
- [ ] **Android Studio** Hedgehog (2023.1.1) or later
- [ ] **JDK 17** installed
- [ ] **Git** installed
- [ ] **PowerShell** (Windows) - pre-installed on Windows

## Step-by-Step Setup

### 1️⃣ **Download Models** (2 minutes)

Open PowerShell and navigate to the models directory:

```powershell
# Navigate to project
cd d:\Github\Anime-Studio\app\src\main\assets\models

# Run the download script
.\download_models.ps1

# Wait for downloads to complete (3 models, ~12.6 MB total)
```

**Expected Output:**
```
==================================
 Anime Studio Model Downloader
==================================

Downloading: animeganv3_hayao.tflite
  Description: Hayao Miyazaki / Studio Ghibli style
  Size: 4.2MB | Input: 512x512
  ✓ Successfully downloaded (4.03 MB)

Downloading: animeganv3_shinkai.tflite
  Description: Makoto Shinkai style (Your Name, Weathering with You)
  Size: 4.2MB | Input: 512x512
  ✓ Successfully downloaded (4.04 MB)

Downloading: animeganv3_portrait.tflite
  Description: Portrait sketch style
  Size: 4.2MB | Input: 512x512
  ✓ Successfully downloaded (4.03 MB)

==================================
 Download Complete
==================================
Successfully downloaded: 3/3 models
✓ All models ready to use!
```

### 2️⃣ **Open in Android Studio** (1 minute)

1. Launch **Android Studio**
2. Click **File** → **Open**
3. Navigate to `d:\Github\Anime-Studio`
4. Click **OK**
5. Wait for Gradle sync to complete

### 3️⃣ **Build the Project** (2 minutes)

**Option A: Using Android Studio UI**
1. Click **Build** → **Make Project** (Ctrl+F9)
2. Wait for build to complete

**Option B: Using Terminal in Android Studio**
```bash
# Clean previous builds
./gradlew clean

# Build debug version
./gradlew assembleDebug
```

### 4️⃣ **Run on Device/Emulator** (1 minute)

**For Physical Device:**
1. Enable **Developer Options** on your Android phone:
   - Go to **Settings** → **About Phone**
   - Tap **Build Number** 7 times
   - Go back to **Settings** → **Developer Options**
   - Enable **USB Debugging**
2. Connect phone via USB
3. Click **Run** ▶️ in Android Studio (Shift+F10)

**For Emulator:**
1. Click **Device Manager** in Android Studio
2. Create or start an emulator (Pixel 6, API 34 recommended)
3. Click **Run** ▶️

---

## ✅ Verification Steps

### Test #1: App Launches
- [ ] App icon appears
- [ ] App opens without crash
- [ ] Main screen shows "Upload Video" button

### Test #2: Video Selection
- [ ] Tap "Upload Video"
- [ ] Gallery opens
- [ ] Select a short video (10-30 seconds recommended for testing)
- [ ] Video information displays correctly

### Test #3: Style Selection
- [ ] "Choose Style" screen appears
- [ ] All 3 styles are listed:
  - Hayao (Ghibli)
  - Shinkai (Your Name)
  - Portrait Sketch
- [ ] Can select a style

### Test #4: Processing
- [ ] Tap "Start Processing"
- [ ] Progress bar shows
- [ ] Stage updates: "Extracting frames" → "Applying style" → "Rebuilding video"
- [ ] Processing completes (may take 2-5 minutes for 10-second video)

### Test #5: Output
- [ ] Success screen shows
- [ ] Can play processed video
- [ ] Video has anime style applied
- [ ] Audio is in sync (if original had audio)
- [ ] Can share video

---

## 🐛 Troubleshooting Quick Fixes

### **Problem: Models not found**
```powershell
# Re-run download script
cd d:\Github\Anime-Studio\app\src\main\assets\models
.\download_models.ps1

# Then rebuild in Android Studio
# Build → Clean Project
# Build → Rebuild Project
```

### **Problem: Gradle sync failed**
```bash
# In Android Studio terminal:
./gradlew clean
./gradlew --refresh-dependencies

# If still fails, check:
# - JDK 17 is set (File → Project Structure → SDK Location)
# - Internet connection is active
# - Gradle daemon: ./gradlew --stop then restart Android Studio
```

### **Problem: Build errors**
1. **Check Gradle JDK**: File → Settings → Build, Execution, Deployment → Build Tools → Gradle
2. **Set JDK**: Choose "JDK 17" from dropdown
3. **Invalidate Caches**: File → Invalidate Caches → Invalidate and Restart

### **Problem: App crashes on launch**
```bash
# Check logs
adb logcat | grep "AnimeStudio"

# Common fixes:
# 1. Ensure models are in assets/models/
# 2. Clean install:
./gradlew clean
./gradlew installDebug
```

### **Problem: Out of memory during processing**
**Quick Fix**: Use a shorter/lower resolution video for testing
- Use 10-second, 480p video
- Or enable "Low Memory Mode" in app settings (if implemented)

---

## 🎯 Quick Test Video

Want a quick test? Use this:
1. Find any 10-second video on your device
2. Or record a 10-second clip with your camera
3. Process with "Hayao" style
4. Should complete in 2-5 minutes on mid-range device

---

## 📦 Project Structure (for reference)

```
Anime-Studio/
├── app/
│   ├── src/main/
│   │   ├── java/com/animestudio/     ← Source code
│   │   ├── res/                       ← UI resources
│   │   └── assets/models/             ← ML models (MUST DOWNLOAD)
│   ├── build.gradle.kts               ← App dependencies
│   └── proguard-rules.pro             ← Optimization rules
├── build.gradle.kts                   ← Project config
├── gradle/ gradlew, gradlew.bat      ← Build tools
└── README.md                          ← Full documentation
```

---

## 🔥 Pro Tips

1. **First build is slow** (~3-5 minutes) - Subsequent builds are faster
2. **Use debug builds** for testing - Release builds take longer but are smaller
3. **Test with short videos** first - 10-30 seconds is perfect for testing
4. **GPU acceleration** works best on devices with good GPU (Adreno 600+, Mali G71+)
5. **Check logcat** if something goes wrong - very helpful for debugging

---

## 🆘 Need More Help?

- **Full Documentation**: README.md (600+ lines, very detailed)
- **Developer Guide**: `.agent/workflows/development.md`
- **Issue Tracker**: Create GitHub issue with details
- **Fixes Document**: `FIXES_AND_IMPROVEMENTS.md`

---

## ✨ What's Next?

Once setup is complete:
1. ✅ Test with different videos
2. ✅ Try all 3 styles
3. ✅ Check beforeafter comparison
4. ✅ Share your results!
5. ✅ Report any bugs

---

**Setup Time**: ~5 minutes  
**First Build Time**: ~3-5 minutes  
**Test Processing Time**: ~2-5 minutes (for 10s video)  
**Total Time to First Success**: **~15 minutes**

🎉 **You're all set! Happy anime styling!** 🎨

---

*Last Updated: November 29, 2025*  
*Version: 1.1.0*
