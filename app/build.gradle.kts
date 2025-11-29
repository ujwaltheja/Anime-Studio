plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.animestudio"
    compileSdk = 35 // Updated to latest

    defaultConfig {
        applicationId = "com.animestudio"
        minSdk = 26
        targetSdk = 35 // Updated to latest
        versionCode = 2 // Incremented version
        versionName = "1.1.0" // Production release

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Enable multi-dex for large apps
        multiDexEnabled = true

        // Optimize Native Libraries Packaging
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        // Performance optimization flags
        renderscriptTargetApi = 26
        renderscriptSupportModeEnabled = false
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Performance and optimization settings
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }

        // Benchmark build type for performance testing
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        
        // Enable Kotlin compiler optimizations
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true // Enable BuildConfig generation
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE.md",
                "/META-INF/LICENSE-notice.md"
            )
        }
        
        jniLibs {
            useLegacyPackaging = false
        }
    }

    // Configure NDK for FFmpeg (if using native libraries)
    ndkVersion = "26.1.10909125" // Updated to latest stable

    // Lint options for production
    lint {
        checkReleaseBuilds = true
        abortOnError = false
        warningsAsErrors = false
    }
}

dependencies {
    // Core Android dependencies - Updated to latest stable 2025 versions
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Jetpack Compose - Updated to latest 2025 BOM
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")

    // Compose debugging tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Coroutines - Updated
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // TensorFlow Lite for ML inference - LATEST 2025 VERSION
    implementation("org.tensorflow:tensorflow-lite:2.16.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.5")
    // Optional: TF Lite GPU delegate
    implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.4")
    // Optional: TF Lite metadata
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
    // TF Lite Flex Support for advanced models
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.16.0")

    // FFmpeg for video processing - Using local AAR
    implementation(files("libs/ffmpeg-kit-full-6.0-2.LTS.aar"))
    implementation("com.arthenica:smart-exception-java:0.2.1")

    // Media handling - Latest 2025
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")

    // Image processing - Latest 2025
    implementation("com.github.bumptech.glide:glide:4.17.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.17.0")

    // WorkManager for background processing
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // DataStore for preferences (better than SharedPreferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("androidx.test:core-ktx:1.5.0")
    
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.01.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Optional: For cloud API integration (Latest 2025 versions)
    implementation("com.squareup.retrofit2:retrofit:2.12.0")
    implementation("com.squareup.retrofit2:converter-gson:2.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.13.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.13.0")

    // Optional: Firebase for analytics and crashlytics (uncomment for production)
    // implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    // implementation("com.google.firebase:firebase-analytics-ktx")
    // implementation("com.google.firebase:firebase-crashlytics-ktx")
    // implementation("com.google.firebase:firebase-perf-ktx")
}
