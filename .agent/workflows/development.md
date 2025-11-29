# Development Workflows for Anime Studio

This document contains common development workflows and commands for the Anime Studio project.

## 🚀 Quick Start Workflows

### Build and Test Workflow

```bash
# Clean and rebuild everything
./gradlew clean
./gradlew build

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Generate test coverage report
./gradlew jacocoTestReport
```

### Release Workflow

```bash
# 1. Update version in build.gradle.kts
# versionCode = X
# versionName = "X.Y.Z"

# 2. Clean build
./gradlew clean

# 3. Build release APK
./gradlew assembleRelease

# 4. Build release AAB (for Play Store)
./gradlew bundleRelease

# Output locations:
# APK: app/build/outputs/apk/release/app-release.apk
# AAB: app/build/outputs/bundle/release/app-release.aab
```

### Model Management Workflow

```powershell
# Download all models (Windows PowerShell)
cd app/src/main/assets/models
.\download_models.ps1

# Verify models downloaded
Get-ChildItem -Filter *.tflite | ForEach-Object {
    Write-Host "$($_.Name) - $([math]::Round($_.Length / 1MB, 2)) MB"
}

# After model changes, sync with Gradle
cd ../../../../.. # Back to project root
./gradlew clean
./gradlew build
```

### Performance Profiling Workflow

```bash
# Build benchmark variant
./gradlew assembleBenchmark

# Install and run
adb install app/build/outputs/apk/benchmark/app-benchmark.apk

# Capture CPU profile
adb shell am start-activity -n com.animestudio/com.animestudio.MainActivity
adb shell am profile start com.animestudio /data/local/tmp/profile.trace
# ... use the app ...
adb shell am profile stop com.animestudio
adb pull /data/local/tmp/profile.trace .

# Analyze in Android Studio: File → Profile or Debug APK
```

### Memory Analysis Workflow

```bash
# Capture heap dump during processing
adb shell am dumpheap com.animestudio /data/local/tmp/heap.dump
adb pull /data/local/tmp/heap.dump .

# Analyze with Android Studio Memory Profiler
# File → Profile or Debug APK → Select heap.dump
```

## 🧪 Testing Workflows

### Unit Test Workflow

```bash
# Run all unit tests
./gradlew test

# Run tests for specific module
./gradlew :app:testDebugUnitTest

# Run specific test class
./gradlew test --tests "com.animestudio.ml.StyleTransferEngineImplTest"

# Run with coverage
./gradlew testDebugUnitTest -Pcoverage
```

### UI Test Workflow

```bash
# Start emulator (or connect physical device)
emulator -avd Pixel_6_API_34

# Run all UI tests
./gradlew connectedAndroidTest

# Run specific UI test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.animestudio.ui.VideoProcessingScreenTest
```

### Manual Testing Checklist

**Smoke Test (5 min - Before every release)**
- [ ] App launches without crash
- [ ] Can select video from gallery
- [ ] Each style loads correctly
- [ ] Processing starts and shows progress
- [ ] Complete video is playable
- [ ] Can share processed video

**Full Test (30 min - Before major releases)**
- [ ] Test all 3 styles (Hayao, Shinkai, Portrait)
- [ ] Test different video lengths (10s, 30s, 1min, 3min)
- [ ] Test different resolutions (480p, 720p, 1080p)
- [ ] Test with/without audio
- [ ] Test cancellation mid-process
- [ ] Test app backgrounding/foregrounding
- [ ] Test low battery scenarios
- [ ] Test low storage scenarios
- [ ] Test permissions denied/granted
- [ ] Test on low-end device
- [ ] Test on high-end device

## 🔧 Development Workflows

### Add New Feature Workflow

```bash
# 1. Create feature branch
git checkout -b feature/awesome-feature

# 2. Develop locally
# ... make changes ...

# 3. Test locally
./gradlew test
./gradlew connectedAndroidTest

# 4. Lint check
./gradlew lint

# 5. Commit and push
git add .
git commit -m "feat: add awesome feature"
git push origin feature/awesome-feature

# 6. Create pull request on GitHub
```

### Fix Bug Workflow

```bash
# 1. Create bugfix branch
git checkout -b fix/issue-123

# 2. Reproduce bug
# ... write failing test ...
./gradlew test # Should fail

# 3. Fix bug
# ... implement fix ...

# 4. Verify fix
./gradlew test # Should pass
./gradlew connectedAndroidTest

# 5. Commit and push
git add .
git commit -m "fix: resolve issue #123"
git push origin fix/issue-123
```

### Update Dependencies Workflow

```bash
# 1. Check for updates
./gradlew dependencyUpdates

# 2. Update build.gradle.kts
# ... modify version numbers ...

# 3. Sync and build
./gradlew clean
./gradlew build

# 4. Run all tests
./gradlew test
./gradlew connectedAndroidTest

# 5. Test manually
# ... verify nothing broke ...

# 6. Commit
git add app/build.gradle.kts
git commit -m "chore: update dependencies"
```

## 📱 Device Testing Workflows

### Test on Physical Device

```bash
# Enable USB debugging on device
# Settings → Developer Options → USB Debugging

# Connect device
adb devices

# Install debug build
./gradlew installDebug

# View logs
adb logcat | grep "AnimeStudio"

# Clear app data
adb shell pm clear com.animestudio
```

### Test on Multiple Devices

```bash
# List all connected devices
adb devices

# Install on specific device
adb -s <device-id> install app/build/outputs/apk/debug/app-debug.apk

# Run tests on all connected devices
./gradlew connectedAndroidTest

# Or on specific device
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.animestudio.ui.tests
```

## 🐛 Debugging Workflows

### Crash Analysis Workflow

```bash
# Capture crash log
adb logcat -d > crash.log

# Filter Anime Studio logs
cat crash.log | grep "com.animestudio"

# Get stack trace
adb logcat *:E | grep "FATAL EXCEPTION"
```

### Performance Issue Workflow

```bash
# 1. Profile app
# Open Android Studio → Run → Profile 'app'

# 2. Perform slow operation

# 3. Analyze:
#    - CPU: Check for hot methods
#    - Memory: Check for leaks
#    - Network: Check unnecessary calls

# 4. Fix identified issues

# 5. Re-profile to verify improvement
```

## 📦 Distribution Workflows

### Internal Testing (Team)

```bash
# Build debug APK
./gradlew assembleDebug

# Share via Firebase App Distribution (if set up)
./gradlew appDistributionUploadDebug

# Or manually share
# app/build/outputs/apk/debug/app-debug.apk
```

### Beta Testing (Play Store)

```bash
# 1. Build release bundle
./gradlew bundleRelease

# 2. Upload to Play Console
# → Release → Testing → Open Testing
# → Upload app/build/outputs/bundle/release/app-release.aab

# 3. Fill release notes

# 4. Roll out to beta testers
```

### Production Release

```bash
# 1. Update version
# build.gradle.kts:
#   versionCode = <increment>
#   versionName = "X.Y.Z"

# 2. Update changelogs
# Update CHANGELOG.md

# 3. Build signed release
./gradlew bundleRelease

# 4. Test release build thoroughly
./gradlew installRelease

# 5. Upload to Play Console
# → Release → Production → Upload app-release.aab

# 6. Create GitHub release
git tag -a v1.1.0 -m "Release version 1.1.0"
git push origin v1.1.0

# 7. Monitor crash reports
```

## 🔄 CI/CD Workflows (GitHub Actions)

**Note**: Set up `.github/workflows/` for automation

### Continuous Integration

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Build
        run: ./gradlew build
      - name: Test
        run: ./gradlew test
```

### Continuous Deployment

```yaml
# .github/workflows/cd.yml
name: CD
on:
  push:
    tags:
      - 'v*'
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build Release
        run: ./gradlew bundleRelease
      - name: Upload to Play Store
        # Use appropriate action
```

## 📊 Monitoring Workflows

### Collect User Feedback

```bash
# Review Play Store ratings
# Google Play Console → Quality → Reviews

# Monitor crash rates
# Play Console → Quality → Android vitals

# Analyze user behavior
# If Firebase Analytics enabled:
# Firebase Console → Analytics
```

### Performance Monitoring

```bash
# Check ANR rate
# Play Console → Quality → Android vitals → ANRs

# Memory usage
# Play Console → Quality → Android vitals → Memory

# Battery impact
# Play Console → Quality → Android vitals → Battery
```

---

## 💡 Tips & Best Practices

1. **Always test before committing**: `./gradlew test`
2. **Clean build when in doubt**: `./gradlew clean build`
3. **Use feature branches**: `git checkout -b feature/name`
4. **Commit often**: Small, focused commits
5. **Test on real devices**: Emulators don't catch everything
6. **Profile before optimizing**: Measure, don't guess
7. **Review ProGuard warnings**: Check R8 optimization logs
8. **Keep models updated**: Run `download_models.ps1` regularly
9. **Monitor app size**: Check APK Analyzer after every build
10. **Automate repetitive tasks**: Create shell scripts

---

**Last Updated**: 2025-01-29  
**Maintained By**: Anime Studio Development Team
