# Anime Studio - ProGuard Rules for Production Release
# Keep optimization aggressive while preserving functionality

# ============================================================================
# General Android Rules
# ============================================================================

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Preserve enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Serializable
-keep class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================================
# Kotlin specific
# ============================================================================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ============================================================================
# Jetpack Compose
# ============================================================================
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Compose runtime
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.animation.** { *; }

# ============================================================================
# TensorFlow Lite - Critical for ML Models
# ============================================================================
-keep class org.tensorflow.lite.** { *; }
-keep interface org.tensorflow.lite.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }

# TensorFlow Lite Support Library
-keep class org.tensorflow.lite.support.** { *; }
-keep interface org.tensorflow.lite.support.** { *; }

# TensorFlow Lite GPU Delegate
-keep class org.tensorflow.lite.gpu.** { *; }
-keepclassmembers class org.tensorflow.lite.gpu.** { *; }

# NNAPI Delegate
-keep class org.tensorflow.lite.nnapi.** { *; }

# Don't warn about TensorFlow
-dontwarn org.tensorflow.**

# ============================================================================
# FFmpeg Kit - Critical for Video Processing
# ============================================================================
-keep class com.arthenica.ffmpegkit.** { *; }
-keep interface com.arthenica.ffmpegkit.** { *; }
-keepclassmembers class com.arthenica.ffmpegkit.** { *; }

# FFmpeg native methods
-keepclasseswithmembernames class com.arthenica.** {
    native <methods>;
}

-keep class com.arthenica.smartexception.** { *; }
-dontwarn com.arthenica.**

# ============================================================================
# Media3 (ExoPlayer)
# ============================================================================
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-keepclassmembers class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ============================================================================
# AndroidX Core Libraries
# ============================================================================
-keep class androidx.core.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.activity.** { *; }
-keep class androidx.fragment.** { *; }

# Lifecycle
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# ViewBinding and DataBinding
-keep class androidx.databinding.** { *; }
-keep class androidx.viewbinding.** { *; }

# ============================================================================
# WorkManager
# ============================================================================
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keep class androidx.work.impl.** { *; }
-dontwarn androidx.work.impl.**

# ============================================================================
# Retrofit & OkHttp (for optional cloud API)
# ============================================================================
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ============================================================================
# Glide
# ============================================================================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}

# ============================================================================
# App-Specific Classes - Keep our models and data classes
# ============================================================================

# Domain models - Keep all data classes
-keep class com.animestudio.domain.** { *; }
-keepclassmembers class com.animestudio.domain.** { *; }

# Keep all ViewModels
-keep class com.animestudio.ui.** extends androidx.lifecycle.ViewModel { *; }
-keep class com.animestudio.presentation.** extends androidx.lifecycle.ViewModel { *; }

# Keep interfaces - important for dependency injection
-keep interface com.animestudio.** { *; }

# Keep data classes with their properties
-keepclassmembers class com.animestudio.**.VideoData { *; }
-keepclassmembers class com.animestudio.**.FrameData { *; }
-keepclassmembers class com.animestudio.**.StyleConfig { *; }

# Keep Result sealed class hierarchy
-keep class com.animestudio.domain.Result { *; }
-keep class com.animestudio.domain.Result$* { *; }

# Keep ProcessingState sealed class hierarchy
-keep class com.animestudio.domain.ProcessingState { *; }
-keep class com.animestudio.domain.ProcessingState$* { *; }

# Keep enums
-keep enum com.animestudio.**.StyleType { *; }
-keep enum com.animestudio.**.VideoDuration { *; }
-keep enum com.animestudio.**.ErrorAction { *; }

# Keep implementations of core interfaces
-keep class * implements com.animestudio.domain.VideoProcessor { *; }
-keep class * implements com.animestudio.domain.FrameExtractor { *; }
-keep class * implements com.animestudio.domain.StyleTransferEngine { *; }
-keep class * implements com.animestudio.domain.VideoReconstructor { *; }

# ============================================================================
# Debugging
# ============================================================================

# Keep line numbers for better crash stack traces
-keepattributes SourceFile,LineNumberTable

# Rename source file attribute for better obfuscation
-renamesourcefileattribute SourceFile

# Keep custom exceptions
-keep public class * extends java.lang.Exception

# ============================================================================
# Optimization Flags
# ============================================================================

# Enable aggressive optimization
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Optimization filters
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Allow optimization
-allowaccessmodification

# Remove logging in release builds (optional - comment out to keep logs)
# -assumenosideeffects class android.util.Log {
#     public static *** d(...);
#     public static *** v(...);
#     public static *** i(...);
# }

# Keep printStackTrace for crash reports
-keep class java.lang.Throwable {
    public void printStackTrace();
}

# ============================================================================
# Warnings to Ignore
# ============================================================================
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ============================================================================
# Additional Safety Rules
# ============================================================================

# Keep all native methods
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep all annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep generic signatures for reflection
-keepattributes Signature

# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================================================
# Firebase (if enabled)
# ============================================================================
# Uncomment if using Firebase
# -keep class com.google.firebase.** { *; }
# -keep class com.google.android.gms.** { *; }
# -dontwarn com.google.firebase.**
# -dontwarn com.google.android.gms.**

# ============================================================================
# End of ProGuard Rules
# ============================================================================
