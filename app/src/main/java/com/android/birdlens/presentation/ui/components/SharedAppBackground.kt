// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/components/SharedAppBackground.kt
package com.android.birdlens.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.android.birdlens.ui.theme.GreenWave1
import com.android.birdlens.ui.theme.GreenWave2
import com.android.birdlens.ui.theme.GreenWave3
import com.android.birdlens.ui.theme.GreenWave4
import com.android.birdlens.ui.theme.VeryDarkGreenBase

@Composable
fun SharedAppBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = VeryDarkGreenBase) // Use the new darker base color
        drawWavePath1()
        drawWavePath2()
    }
}

private fun DrawScope.drawWavePath1() {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val path1 = Path().apply {
        moveTo(0f, canvasHeight * 0.1f)
        cubicTo(
            canvasWidth * 0.2f, canvasHeight * 0.05f,
            canvasWidth * 0.3f, canvasHeight * 0.4f,
            canvasWidth * 0.6f, canvasHeight * 0.35f
        )
        cubicTo(
            canvasWidth * 0.9f, canvasHeight * 0.3f,
            canvasWidth * 1.1f, canvasHeight * 0.6f, // Can go slightly off-screen for smoother edge waves
            canvasWidth * 0.7f, canvasHeight * 0.7f
        )
        // Added a more dynamic curve downwards and back to the left
        cubicTo(
            canvasWidth * 0.3f, canvasHeight * 0.85f,
            canvasWidth * 0.1f, canvasHeight * 0.5f,
            0f, canvasHeight * 0.6f
        )
        close()
    }
    drawPath(
        path = path1,
        brush = Brush.radialGradient(
            colors = listOf(
                GreenWave2.copy(alpha = 0.6f), // Prominent Lime
                GreenWave1.copy(alpha = 0.5f),
                GreenWave3.copy(alpha = 0.35f),
                VeryDarkGreenBase.copy(alpha = 0.2f)
            ),
            center = Offset(canvasWidth * 0.3f, canvasHeight * 0.3f), // Adjust center for effect
            radius = canvasWidth * 0.9f // Adjust radius
        )
    )
}

private fun DrawScope.drawWavePath2() {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val path2 = Path().apply {
        moveTo(canvasWidth, canvasHeight * 0.5f)
        cubicTo(
            canvasWidth * 0.8f, canvasHeight * 0.6f,
            canvasWidth * 0.7f, canvasHeight * 0.2f, // Higher peak
            canvasWidth * 0.4f, canvasHeight * 0.35f // Adjusted control point
        )
        cubicTo(
            canvasWidth * 0.05f, canvasHeight * 0.5f, // More to the left
            canvasWidth * -0.1f, canvasHeight * 0.95f, // Off-screen for smoother edge
            canvasWidth * 0.35f, canvasHeight // Ends at bottom
        )
        lineTo(canvasWidth, canvasHeight) // Line to bottom right
        close()
    }
    drawPath(
        path = path2,
        brush = Brush.linearGradient(
            colors = listOf(
                GreenWave4.copy(alpha = 0.55f),
                GreenWave2.copy(alpha = 0.4f), // Lime again
                GreenWave1.copy(alpha = 0.3f),
                VeryDarkGreenBase.copy(alpha = 0.1f)
            ),
            start = Offset(canvasWidth * 0.9f, canvasHeight * 0.4f), // Adjust gradient direction
            end = Offset(canvasWidth * 0.2f, canvasHeight)
        )
    )
}