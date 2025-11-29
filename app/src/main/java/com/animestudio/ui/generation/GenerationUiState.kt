package com.animestudio.ui.generation

import android.graphics.Bitmap

sealed class GenerationUiState {
    object Idle : GenerationUiState()
    data class Loading(val message: String, val progress: Int = 0) : GenerationUiState()
    data class Success(val image: Bitmap) : GenerationUiState()
    data class Error(val message: String) : GenerationUiState()
}

data class GenerationConfig(
    val prompt: String = "",
    val negativePrompt: String = "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry",
    val steps: Int = 20,
    val guidanceScale: Float = 7.5f,
    val seed: Long? = null
)
