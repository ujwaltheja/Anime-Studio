# Architecture Documentation

## Overview

Anime Studio follows **Clean Architecture** principles with clear separation of concerns across layers.

## Architecture Layers

```
┌─────────────────────────────────────────────┐
│           UI Layer (Compose)                │
│  - VideoProcessingScreen                    │
│  - VideoProcessingViewModel                 │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│         Domain Layer (Interfaces)           │
│  - VideoProcessor                           │
│  - VideoInputManager                        │
│  - FrameExtractor                           │
│  - StyleTransferEngine                      │
│  - VideoReconstructor                       │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│       Data/Implementation Layer             │
│  - VideoProcessorImpl                       │
│  - VideoInputManagerImpl                    │
│  - FrameExtractorImpl                       │
│  - StyleTransferEngineImpl                  │
│  - VideoReconstructorImpl                   │
└─────────────────────────────────────────────┘
```

## Module Details

### 1. Domain Layer

**Purpose**: Defines business logic contracts

**Files**:
- `VideoData.kt` - Data models
- `VideoProcessor.kt` - Core interfaces

**Key Interfaces**:

```kotlin
interface VideoProcessor {
    fun processVideo(
        videoData: VideoData,
        styleConfig: StyleConfig,
        outputFile: File
    ): Flow<ProcessingState>
}

interface FrameExtractor {
    suspend fun extractFrames(
        videoData: VideoData,
        outputDir: File,
        onProgress: (Int, Int) -> Unit
    ): Result<List<FrameData>>
}

interface StyleTransferEngine {
    suspend fun initialize(styleConfig: StyleConfig): Result<Unit>
    suspend fun transferStyle(frame: FrameData): Result<FrameData>
}

interface VideoReconstructor {
    suspend fun reconstructVideo(
        frames: List<FrameData>,
        audioFile: File?,
        outputFile: File,
        frameRate: Float
    ): Result<File>
}
```

### 2. Data Layer

**Purpose**: Implementation of domain interfaces

**VideoProcessorImpl Flow**:

```
1. Initialize ML Model
        ↓
2. Extract Frames from Video
        ↓
3. Extract Audio (if present)
        ↓
4. Apply Style Transfer to Each Frame
        ↓
5. Reconstruct Video from Styled Frames
        ↓
6. Merge Audio with Video
        ↓
7. Return Final Output
```

### 3. UI Layer

**Purpose**: User interface using Jetpack Compose

**State Management**:

```kotlin
sealed class VideoProcessingUiState {
    object Idle
    data class Loading(val message: String)
    data class VideoLoaded(val videoData: VideoData)
    data class Processing(val stage: String, val progress: Int)
    data class Complete(val outputFile: File)
    data class Error(val message: String)
}
```

**ViewModel Flow**:

```
User Action → ViewModel → VideoProcessor → Flow<ProcessingState> → UI Update
```

## Processing Pipeline

### Detailed Flow

```
┌───────────────┐
│ Video Input   │
│ - Gallery     │
│ - Camera      │
└───────┬───────┘
        │
        ▼
┌───────────────────┐
│ Metadata Extract  │
│ - Duration        │
│ - Resolution      │
│ - Frame Rate      │
│ - Audio Present   │
└───────┬───────────┘
        │
        ▼
┌───────────────────┐
│ Frame Extraction  │
│ - MediaRetriever  │
│ - or FFmpeg       │
└───────┬───────────┘
        │
        ├─────────────────┐
        │                 │
        ▼                 ▼
┌───────────────┐  ┌──────────────┐
│ Video Frames  │  │ Audio Track  │
└───────┬───────┘  └──────┬───────┘
        │                 │
        ▼                 │
┌───────────────────┐     │
│ Style Transfer    │     │
│ - Load TF Model   │     │
│ - Preprocess      │     │
│ - Inference       │     │
│ - Postprocess     │     │
└───────┬───────────┘     │
        │                 │
        ▼                 │
┌───────────────┐         │
│ Styled Frames │         │
└───────┬───────┘         │
        │                 │
        ▼                 │
┌───────────────────┐     │
│ Video Encode      │     │
│ - MediaCodec      │     │
│ - or FFmpeg       │     │
└───────┬───────────┘     │
        │                 │
        │   ┌─────────────┘
        │   │
        ▼   ▼
┌───────────────────┐
│ Merge Audio/Video │
│ - FFmpeg          │
└───────┬───────────┘
        │
        ▼
┌───────────────────┐
│ Final Output      │
│ - Styled Video    │
└───────────────────┘
```

## Threading Model

### Coroutines Usage

All heavy operations run on background threads:

```kotlin
// UI Thread
viewModelScope.launch {
    // Switch to IO thread
    withContext(Dispatchers.IO) {
        // Heavy operation
        frameExtractor.extractFrames(...)
    }
}
```

### Thread Safety

- **Main/UI Thread**: Compose UI updates only
- **IO Dispatcher**: File operations, network calls
- **Default Dispatcher**: CPU-intensive ML operations
- **GPU Delegate**: Offloads to GPU when available

## State Management

### Flow-based Updates

```kotlin
fun processVideo(...): Flow<ProcessingState> = flow {
    emit(ProcessingState.Loading("Initializing..."))

    // Initialize
    emit(ProcessingState.Loading("Extracting frames..."))

    // Extract frames
    emit(ProcessingState.Extracting(current, total))

    // Style transfer
    emit(ProcessingState.Transferring(current, total))

    // Reconstruct
    emit(ProcessingState.Reconstructing(progress))

    // Complete
    emit(ProcessingState.Complete(outputFile))
}
```

### ViewModel Pattern

```kotlin
class VideoProcessingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun processVideo() {
        viewModelScope.launch {
            videoProcessor.processVideo(...)
                .collect { state ->
                    _uiState.value = state.toUiState()
                }
        }
    }
}
```

## Dependency Injection

### Manual DI (Current Implementation)

```kotlin
object VideoProcessorFactory {
    fun create(context: Context): VideoProcessor {
        val frameExtractor = FrameExtractorImpl(context)
        val styleEngine = StyleTransferEngineImpl(context)
        val videoReconstructor = VideoReconstructorImpl(context)

        return VideoProcessorImpl(
            context,
            frameExtractor,
            styleEngine,
            videoReconstructor
        )
    }
}
```

### Future: Hilt Integration

For production apps, consider using Hilt:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFrameExtractor(
        @ApplicationContext context: Context
    ): FrameExtractor = FrameExtractorImpl(context)

    @Provides
    @Singleton
    fun provideStyleTransferEngine(
        @ApplicationContext context: Context
    ): StyleTransferEngine = StyleTransferEngineImpl(context)
}
```

## Error Handling

### Result Pattern

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Throwable?) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
```

### Error Propagation

```kotlin
try {
    when (val result = frameExtractor.extractFrames(...)) {
        is Result.Success -> processFrames(result.data)
        is Result.Error -> emit(ProcessingState.Error(result.message))
    }
} catch (e: Exception) {
    emit(ProcessingState.Error("Unexpected error", e))
}
```

## Memory Management

### Frame Processing

```kotlin
// Don't keep bitmaps in memory
val frameData = FrameData(
    index = index,
    bitmap = null,  // Save to file instead
    file = frameFile
)

// Recycle bitmaps immediately after use
bitmap.compress(...)
bitmap.recycle()
```

### Cache Management

```kotlin
// Clean up temporary files
fun cleanup(workDir: File) {
    try {
        workDir.deleteRecursively()
    } catch (e: Exception) {
        // Log error
    }
}
```

### Large Heap

Enable in AndroidManifest for video processing:
```xml
<application
    android:largeHeap="true"
    ...>
```

## Testing Strategy

### Unit Tests

```kotlin
@Test
fun testFrameExtraction() = runTest {
    val mockVideo = createMockVideoData()
    val result = frameExtractor.extractFrames(mockVideo, tempDir)

    assertTrue(result is Result.Success)
    assertEquals(30, (result as Result.Success).data.size)
}
```

### Integration Tests

```kotlin
@Test
fun testEndToEndProcessing() = runTest {
    val states = mutableListOf<ProcessingState>()

    videoProcessor.processVideo(videoData, styleConfig, outputFile)
        .collect { states.add(it) }

    assertTrue(states.last() is ProcessingState.Complete)
}
```

## Performance Optimization

### Model Optimization

1. **Quantization**: Reduce model size
2. **GPU Delegation**: Hardware acceleration
3. **NNAPI**: Android Neural Networks API
4. **Batch Processing**: Process multiple frames together

### Video Optimization

1. **Resolution Scaling**: Process at lower resolution
2. **Frame Sampling**: Skip frames for faster processing
3. **Parallel Processing**: Multi-threaded frame processing
4. **Caching**: Cache intermediate results

## Security Considerations

### File Access

- Use scoped storage (Android 10+)
- FileProvider for sharing
- Validate file URIs
- Clean up temporary files

### ML Model Security

- Verify model checksums
- Sandbox model execution
- Limit model file size
- Validate model inputs

### Permissions

- Request only necessary permissions
- Explain permission usage
- Handle denied permissions gracefully
- Use runtime permissions

## Scalability

### Modular Design

Each module can be:
- Developed independently
- Tested independently
- Replaced/upgraded independently
- Reused in other projects

### Extension Points

1. **New Style Models**: Add to StyleType enum
2. **Cloud Processing**: Implement CloudStyleTransferClient
3. **Custom Extractors**: Implement FrameExtractor interface
4. **Alternative Encoders**: Implement VideoReconstructor interface

---

This architecture provides a solid foundation for a production-ready video processing app with clean separation of concerns and extensibility.
