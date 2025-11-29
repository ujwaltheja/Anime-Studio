package com.animestudio.vtuber

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.animestudio.domain.Result
import com.animestudio.models.ModelManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VTuberViewModel(
    private val context: Context
) : ViewModel() {

    private val modelManager = ModelManager(context)
    private val vtuberEngine = VTuberEngine(context, modelManager)

    private val _faceData = MutableStateFlow(
        VTuberEngine.FaceData(1f, 1f, 0f, 0f, 0f, 0f)
    )
    val faceData: StateFlow<VTuberEngine.FaceData> = _faceData.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    init {
        initializeEngine()
    }

    private fun initializeEngine() {
        viewModelScope.launch {
            vtuberEngine.initialize()
            startTracking()
        }
    }

    fun toggleTracking() {
        if (_isTracking.value) {
            stopTracking()
        } else {
            startTracking()
        }
    }

    private var calibrationOffset = VTuberEngine.FaceData(0f, 0f, 0f, 0f, 0f, 0f)

    fun calibrate() {
        val current = _faceData.value
        // Calibrate head rotation to zero
        calibrationOffset = VTuberEngine.FaceData(
            leftEyeOpen = 0f, 
            rightEyeOpen = 0f, 
            mouthOpen = 0f,
            headYaw = current.headYaw,
            headPitch = current.headPitch,
            headRoll = current.headRoll
        )
    }

    private fun startTracking() {
        _isTracking.value = true
        viewModelScope.launch {
            while (isActive && _isTracking.value) {
                // In a real app, we would get frames from CameraX here
                // For now, we pass null/dummy to trigger the test mode simulation
                val result = vtuberEngine.processFrame(
                    android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                )

                if (result is Result.Success) {
                    val raw = result.data
                    // Apply calibration
                    val calibrated = raw.copy(
                        headYaw = raw.headYaw - calibrationOffset.headYaw,
                        headPitch = raw.headPitch - calibrationOffset.headPitch,
                        headRoll = raw.headRoll - calibrationOffset.headRoll
                    )
                    _faceData.value = calibrated
                }

                // Target 60 FPS
                delay(16)
            }
        }
    }

    private fun stopTracking() {
        _isTracking.value = false
    }

    override fun onCleared() {
        super.onCleared()
        vtuberEngine.release()
    }
}

class VTuberViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VTuberViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VTuberViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
