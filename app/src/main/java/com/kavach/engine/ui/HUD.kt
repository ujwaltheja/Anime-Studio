package com.kavach.engine.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Observable state for the debug HUD. Write from any thread. */
class HUDState {
    var fps: Float by mutableFloatStateOf(0f)
}

/** Minimal overlay showing FPS and an optional message. */
@Composable
fun EngineHUD(state: HUDState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Text(
            text  = "${state.fps.toInt()} FPS",
            color = Color(0xFF00FF88),
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        )
    }
}
