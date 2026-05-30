package com.kavach.demo.citygame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Observable HUD state written from the GL thread via [android.os.Handler]. */
class CityHUDState {
    var fps:              Float         by mutableFloatStateOf(0f)
    var budget:           Int           by mutableIntStateOf(50_000)
    var population:       Int           by mutableIntStateOf(0)
    var selectedBuilding: BuildingType? by mutableStateOf(null)
    var message:          String        by mutableStateOf("")
}

@Composable
fun CityHUD(
    state: CityHUDState,
    onBuildingSelected: (BuildingType?) -> Unit
) {
    Box(Modifier.fillMaxSize()) {

        // ── Top bar ──────────────────────────────────────────────────────
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatChip("👥 ${state.population}")
            StatChip("💰 ₹${"%,d".format(state.budget)}")
            StatChip("${state.fps.toInt()} FPS", color = Color(0xFF88FF99))
        }

        // ── Message ──────────────────────────────────────────────────────
        if (state.message.isNotEmpty()) {
            Text(
                text     = state.message,
                color    = Color(0xFFFFDD44),
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xAA000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // ── Building toolbar ─────────────────────────────────────────────
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Deselect / clear selection button
            BuildingButton(
                label     = "✖",
                subLabel  = "Clear",
                selected  = state.selectedBuilding == null,
                onClick   = { onBuildingSelected(null) }
            )

            BuildingType.entries.forEach { type ->
                BuildingButton(
                    label    = type.emoji,
                    subLabel = "₹${type.cost / 1000}k",
                    selected = state.selectedBuilding == type,
                    onClick  = { onBuildingSelected(if (state.selectedBuilding == type) null else type) }
                )
            }
        }
    }
}

@Composable
private fun StatChip(text: String, color: Color = Color.White) {
    Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun BuildingButton(
    label: String,
    subLabel: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                if (selected) Color(0xFF2255AA) else Color(0x00000000),
                RoundedCornerShape(8.dp)
            )
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) Color(0xFF88BBFF) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label,    fontSize = 22.sp, color = Color.White)
        Text(subLabel, fontSize = 10.sp, color = Color(0xFFCCCCCC))
    }
}
