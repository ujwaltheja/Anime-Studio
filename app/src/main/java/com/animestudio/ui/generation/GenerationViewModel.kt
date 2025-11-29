package com.animestudio.ui.generation

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.animestudio.domain.Result
import com.animestudio.generation.WaifuDiffusionEngine
import com.animestudio.models.ModelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GenerationViewModel(
    private val context: Context
) : ViewModel() {

    private val modelManager = ModelManager(context)
    private val waifuDiffusionEngine = WaifuDiffusionEngine(context, modelManager)

    private val _uiState = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)
    val uiState: StateFlow<GenerationUiState> = _uiState.asStateFlow()

    private val _config = MutableStateFlow(GenerationConfig())
    val config: StateFlow<GenerationConfig> = _config.asStateFlow()

    fun updateConfig(newConfig: GenerationConfig) {
        _config.value = newConfig
    }

    fun generateImage() {
        val currentConfig = _config.value
        if (currentConfig.prompt.isBlank()) {
            _uiState.value = GenerationUiState.Error("Please enter a prompt")
            return
        }

        viewModelScope.launch {
            _uiState.value = GenerationUiState.Loading("Initializing engine...")

            // Initialize engine
            when (val initResult = waifuDiffusionEngine.initialize { stage, progress ->
                _uiState.value = GenerationUiState.Loading("Initializing: $stage", progress)
            }) {
                is Result.Error -> {
                    _uiState.value = GenerationUiState.Error("Initialization failed: ${initResult.message}")
                    return@launch
                }
                else -> {}
            }

            _uiState.value = GenerationUiState.Loading("Generating image...", 0)

            // Generate
            val result = waifuDiffusionEngine.generate(
                prompt = currentConfig.prompt,
                negativePrompt = currentConfig.negativePrompt,
                steps = currentConfig.steps,
                guidanceScale = currentConfig.guidanceScale,
                seed = currentConfig.seed ?: System.currentTimeMillis(),
                onProgress = { step, total ->
                    val percent = (step.toFloat() / total * 100).toInt()
                    _uiState.value = GenerationUiState.Loading("Generating: Step $step/$total", percent)
                }
            )

            when (result) {
                is Result.Success -> {
                    // Save image
                    try {
                        val dir = java.io.File(context.filesDir, "generated")
                        if (!dir.exists()) dir.mkdirs()
                        val file = java.io.File(dir, "gen_${System.currentTimeMillis()}.png")
                        val stream = java.io.FileOutputStream(file)
                        result.data.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        stream.close()
                        
                        _uiState.value = GenerationUiState.Success(result.data)
                    } catch (e: Exception) {
                        _uiState.value = GenerationUiState.Error("Failed to save image: ${e.message}")
                    }
                }
                is Result.Error -> {
                    _uiState.value = GenerationUiState.Error(result.message)
                }
                else -> {} // Should not happen
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        waifuDiffusionEngine.release()
    }
}

class GenerationViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GenerationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GenerationViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
