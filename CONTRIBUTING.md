# Contributing to Anime Studio

Thank you for your interest in contributing to Anime Studio! This document provides guidelines and instructions for contributing.

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Focus on what is best for the community
- Show empathy towards other contributors

## How to Contribute

### Reporting Bugs

Before creating bug reports, please check existing issues. When creating a bug report, include:

- **Clear title and description**
- **Steps to reproduce**
- **Expected behavior**
- **Actual behavior**
- **Screenshots** (if applicable)
- **Device information** (Android version, device model)
- **App version**

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, include:

- **Clear title and description**
- **Use case** - why is this enhancement needed?
- **Proposed solution**
- **Alternative solutions** you've considered
- **Additional context** (mockups, examples)

### Pull Requests

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/my-new-feature
   ```

3. **Make your changes**
   - Follow the code style guidelines
   - Add comments for complex logic
   - Update documentation if needed

4. **Test your changes**
   - Run existing tests
   - Add new tests for new functionality
   - Test on multiple devices if possible

5. **Commit your changes**
   ```bash
   git commit -m "Add feature: description"
   ```

6. **Push to your fork**
   ```bash
   git push origin feature/my-new-feature
   ```

7. **Create a Pull Request**
   - Provide a clear description
   - Reference related issues
   - Include screenshots for UI changes

## Development Guidelines

### Code Style

Follow Kotlin official coding conventions:

```kotlin
// Good
class VideoProcessor(
    private val context: Context,
    private val frameExtractor: FrameExtractor
) {
    fun processVideo() {
        // Implementation
    }
}

// Use meaningful variable names
val videoFrameCount = 30
val processingDuration = System.currentTimeMillis()

// Add KDoc comments for public APIs
/**
 * Processes a video with the specified style
 * @param videoData Input video data
 * @param styleConfig Style configuration
 * @return Flow of processing states
 */
fun processVideo(
    videoData: VideoData,
    styleConfig: StyleConfig
): Flow<ProcessingState>
```

### Architecture

- Follow Clean Architecture principles
- Maintain separation of concerns
- Use dependency injection
- Prefer composition over inheritance
- Write testable code

### Testing

Write tests for:
- Business logic (unit tests)
- UI components (UI tests)
- Integration between modules

```kotlin
@Test
fun `test frame extraction returns correct count`() = runTest {
    val result = frameExtractor.extractFrames(mockVideo, tempDir)

    assertTrue(result is Result.Success)
    assertEquals(30, (result as Result.Success).data.size)
}
```

### Documentation

- Add KDoc comments for public APIs
- Update README.md for new features
- Create documentation in /docs for complex features
- Include code examples where helpful

### Commit Messages

Use conventional commits:

```
feat: add support for custom ML models
fix: resolve memory leak in frame extraction
docs: update model setup guide
refactor: simplify video reconstruction logic
test: add unit tests for style transfer engine
```

## Project Structure

```
app/src/main/java/com/animestudio/
├── domain/              # Interfaces and models
├── data/                # Implementations
├── video/               # Video input module
├── frameextraction/     # Frame extraction module
├── ml/                  # ML style transfer module
├── videoreconstruction/ # Video reconstruction module
├── ui/                  # UI components
└── utils/               # Utility classes
```

## Adding a New Feature

Example: Adding a new style model

1. **Add to StyleType enum** (domain/VideoData.kt):
```kotlin
enum class StyleType {
    // ...existing styles...
    NEW_STYLE
}
```

2. **Add model file** to `app/src/main/assets/models/newstyle.tflite`

3. **Update model path mapping** (ui/VideoProcessingViewModel.kt):
```kotlin
private fun getModelPathForStyle(styleType: StyleType): String {
    return when (styleType) {
        // ...existing cases...
        StyleType.NEW_STYLE -> "models/newstyle.tflite"
    }
}
```

4. **Add UI display** (ui/VideoProcessingScreen.kt):
```kotlin
private fun getStyleDisplayName(styleType: StyleType): String {
    return when (styleType) {
        // ...existing cases...
        StyleType.NEW_STYLE -> "New Style Name"
    }
}
```

5. **Test the feature**

6. **Update documentation**

## Performance Considerations

- Profile code before optimizing
- Use appropriate coroutine dispatchers
- Recycle bitmaps after use
- Clean up temporary files
- Monitor memory usage

## Questions?

- Open an issue for discussion
- Check existing documentation
- Review code comments

Thank you for contributing!
