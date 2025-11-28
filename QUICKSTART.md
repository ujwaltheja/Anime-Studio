# Quick Start Guide

Get Anime Studio running in 5 minutes!

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android device or emulator (API 26+)

## Step 1: Clone and Open

```bash
git clone <repository-url>
cd Anime-Studio
```

Open the project in Android Studio:
`File → Open → Select Anime-Studio folder`

## Step 2: Sync Gradle

Wait for Gradle sync to complete. If prompted, update Gradle wrapper.

## Step 3: Add ML Models

**Option A: Quick Test (Skip Models)**

For initial testing without models, the app will build but style transfer won't work. Add placeholder error handling.

**Option B: Add Real Models**

1. Download sample model (small size for testing):
   ```bash
   # Download a lightweight test model
   # See MODEL_SETUP.md for detailed instructions
   ```

2. Place in: `app/src/main/assets/models/`
   - At minimum, add one model: `animegan.tflite`

## Step 4: Build and Run

### On Emulator

1. Create an emulator: `Tools → Device Manager → Create Device`
   - Choose: Pixel 5 or newer
   - API Level: 26 or higher
   - Enable: Hardware acceleration

2. Run: Click green play button or `Shift+F10`

### On Physical Device

1. Enable Developer Options on your Android device
2. Enable USB Debugging
3. Connect device via USB
4. Select device and run

## Step 5: Test the App

1. **Grant Permissions**: Allow storage and camera access

2. **Upload Test Video**:
   - Use a short video (5-10 seconds)
   - Lower resolution (480p) for faster testing

3. **Choose Style**: Select any available style

4. **Process**: Watch the progress indicators

5. **Share**: Export your styled video

## Common Issues

### Issue: Gradle sync failed

**Solution**:
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

### Issue: Model not found

**Solution**: Check `app/src/main/assets/models/` directory exists and contains .tflite files

### Issue: Out of memory

**Solution**: Use a shorter video or lower resolution for testing

### Issue: App crashes on startup

**Solution**:
1. Check Android version (must be API 26+)
2. Verify all permissions granted
3. Check logcat for errors: `View → Tool Windows → Logcat`

## Quick Development Workflow

### Add a New Style

1. Add .tflite model to `app/src/main/assets/models/`
2. Update `StyleType` enum in `domain/VideoData.kt`
3. Update `getModelPathForStyle()` in `VideoProcessingViewModel.kt`
4. Add display name in `VideoProcessingScreen.kt`

### Test a Module

```kotlin
@Test
fun testFrameExtraction() = runTest {
    val extractor = FrameExtractorImpl(context)
    val result = extractor.extractFrames(testVideo, tempDir)
    assertTrue(result is Result.Success)
}
```

## Next Steps

- Read [README.md](README.md) for full documentation
- See [ARCHITECTURE.md](ARCHITECTURE.md) for design details
- Check [MODEL_SETUP.md](MODEL_SETUP.md) for ML models
- Review [CONTRIBUTING.md](CONTRIBUTING.md) to contribute

## Performance Tips

For faster development testing:

1. **Use Smaller Videos**:
   - 5-10 seconds
   - 480p resolution
   - Fewer frames

2. **Enable GPU**:
   ```kotlin
   StyleConfig(useGPU = true)
   ```

3. **Sample Frames**:
   ```kotlin
   extractFrames(extractionInterval = 100L) // Every 100ms
   ```

## Development Tools

### Useful Gradle Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test

# Check dependencies
./gradlew dependencies
```

### Android Studio Shortcuts

- `Ctrl+Shift+A` - Find action
- `Ctrl+N` - Find class
- `Ctrl+Shift+N` - Find file
- `Alt+Enter` - Quick fix
- `Shift+F10` - Run app
- `Shift+F9` - Debug app

## Getting Help

- Check [README.md](README.md) FAQ section
- Review code comments
- Open issue on GitHub
- Check Android Studio Logcat

---

**Ready to build!** 🚀

For detailed setup and advanced features, see the full [README.md](README.md)
