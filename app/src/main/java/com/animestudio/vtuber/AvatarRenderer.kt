package com.animestudio.vtuber

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AvatarRenderer(
    faceData: VTuberEngine.FaceData,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val faceSize = minOf(size.width, size.height) * 0.4f

        // Apply head rotation
        rotate(degrees = faceData.headRoll * 180 / PI.toFloat(), pivot = Offset(centerX, centerY)) {
            translate(
                left = faceData.headYaw * 50f,
                top = faceData.headPitch * 50f
            ) {
                // 1. Face Contour & Skin
                val skinBrush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFE0BD), Color(0xFFFFD0A0)),
                    center = Offset(centerX, centerY),
                    radius = faceSize
                )
                drawCircle(
                    brush = skinBrush,
                    radius = faceSize,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = Color(0xFFE0B090),
                    radius = faceSize,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 4f)
                )

                // Blush
                drawOval(
                    color = Color(0xFFFF8080).copy(alpha = 0.3f),
                    topLeft = Offset(centerX - faceSize * 0.7f, centerY + faceSize * 0.1f),
                    size = Size(faceSize * 0.4f, faceSize * 0.2f)
                )
                drawOval(
                    color = Color(0xFFFF8080).copy(alpha = 0.3f),
                    topLeft = Offset(centerX + faceSize * 0.3f, centerY + faceSize * 0.1f),
                    size = Size(faceSize * 0.4f, faceSize * 0.2f)
                )

                // 2. Eyes
                val eyeOffsetX = faceSize * 0.35f
                val eyeOffsetY = faceSize * 0.1f
                val eyeSize = faceSize * 0.28f

                // Left Eye
                drawEye(
                    center = Offset(centerX - eyeOffsetX, centerY - eyeOffsetY),
                    size = eyeSize,
                    openness = faceData.leftEyeOpen,
                    isLeft = true
                )

                // Right Eye
                drawEye(
                    center = Offset(centerX + eyeOffsetX, centerY - eyeOffsetY),
                    size = eyeSize,
                    openness = faceData.rightEyeOpen,
                    isLeft = false
                )

                // 3. Mouth
                val mouthY = centerY + faceSize * 0.45f
                drawMouth(
                    center = Offset(centerX, mouthY),
                    width = faceSize * 0.25f,
                    openness = faceData.mouthOpen
                )
                
                // 4. Hair (Premium Bangs)
                val hairColor = Color(0xFF3F51B5)
                val hairHighlight = Color(0xFF7986CB)
                val hairShadow = Color(0xFF303F9F)
                
                val hairPath = Path().apply {
                    moveTo(centerX - faceSize * 1.1f, centerY - faceSize * 0.3f)
                    // Left bang
                    quadraticBezierTo(
                        centerX - faceSize * 0.8f, centerY - faceSize * 1.2f,
                        centerX, centerY - faceSize * 0.8f
                    )
                    // Right bang
                    quadraticBezierTo(
                        centerX + faceSize * 0.8f, centerY - faceSize * 1.2f,
                        centerX + faceSize * 1.1f, centerY - faceSize * 0.3f
                    )
                    // Bottom curve
                    quadraticBezierTo(
                        centerX, centerY - faceSize * 0.6f,
                        centerX - faceSize * 1.1f, centerY - faceSize * 0.3f
                    )
                }
                
                // Hair Shadow
                drawPath(
                    path = hairPath,
                    color = hairShadow,
                    style = Stroke(width = 8f)
                )
                // Hair Fill
                drawPath(
                    path = hairPath,
                    brush = Brush.linearGradient(
                        colors = listOf(hairColor, hairHighlight),
                        start = Offset(centerX, centerY - faceSize),
                        end = Offset(centerX, centerY)
                    )
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEye(
    center: Offset,
    size: Float,
    openness: Float,
    isLeft: Boolean
) {
    // Eye white
    drawCircle(
        color = Color.White,
        radius = size,
        center = center
    )
    
    // Eye shadow (top part)
    drawArc(
        color = Color.Black.copy(alpha = 0.1f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - size, center.y - size),
        size = Size(size * 2, size * 2)
    )

    // Iris
    val irisSize = size * 0.7f
    val irisBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF42A5F5), Color(0xFF1565C0)),
        center = center,
        radius = irisSize
    )
    
    drawCircle(
        brush = irisBrush,
        radius = irisSize,
        center = center
    )

    // Pupil
    drawCircle(
        color = Color(0xFF0D47A1),
        radius = irisSize * 0.4f,
        center = center
    )
    
    // Highlights (Sparkle)
    drawCircle(
        color = Color.White,
        radius = irisSize * 0.25f,
        center = Offset(center.x - irisSize * 0.3f, center.y - irisSize * 0.3f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.7f),
        radius = irisSize * 0.15f,
        center = Offset(center.x + irisSize * 0.3f, center.y + irisSize * 0.3f)
    )

    // Eyelid (based on openness)
    val lidHeight = size * 2.2f * (1f - openness)
    if (lidHeight > 0) {
        drawRect(
            color = Color(0xFFFFE0BD),
            topLeft = Offset(center.x - size * 1.2f, center.y - size * 1.2f),
            size = Size(size * 2.4f, lidHeight)
        )
    }
    
    // Eyelash (Thick and styled)
    val lashY = center.y - size * openness
    val lashPath = Path().apply {
        moveTo(center.x - size * 1.2f, lashY + size * 0.2f)
        quadraticBezierTo(
            center.x, lashY - size * 0.3f,
            center.x + size * 1.2f, lashY + size * 0.2f
        )
    }
    drawPath(
        path = lashPath,
        color = Color(0xFF212121),
        style = Stroke(width = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMouth(
    center: Offset,
    width: Float,
    openness: Float
) {
    val height = width * 0.6f * openness
    
    if (openness < 0.1f) {
        // Closed mouth (cute smile)
        val path = Path().apply {
            moveTo(center.x - width/2, center.y)
            quadraticBezierTo(
                center.x, center.y + width * 0.2f,
                center.x + width/2, center.y
            )
        }
        drawPath(
            path = path,
            color = Color(0xFFD81B60),
            style = Stroke(width = 5f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    } else {
        // Open mouth (D-shape)
        val path = Path().apply {
            moveTo(center.x - width/2, center.y)
            // Top lip
            quadraticBezierTo(
                center.x, center.y + height * 0.1f,
                center.x + width/2, center.y
            )
            // Bottom lip
            quadraticBezierTo(
                center.x, center.y + height,
                center.x - width/2, center.y
            )
        }
        
        // Inside mouth
        drawPath(path = path, color = Color(0xFF880E4F))
        
        // Tongue
        drawOval(
            color = Color(0xFFF48FB1),
            topLeft = Offset(center.x - width * 0.3f, center.y + height * 0.5f),
            size = Size(width * 0.6f, height * 0.4f)
        )
    }
}
