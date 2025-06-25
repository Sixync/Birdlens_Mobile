// app/src/main/java/com/android/birdlens/presentation/ui/screens/map/TutorialOverlay.kt
package com.android.birdlens.presentation.ui.screens.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.birdlens.ui.theme.ButtonGreen
import com.android.birdlens.ui.theme.CardBackground
import com.android.birdlens.ui.theme.GreenWave2
import com.android.birdlens.ui.theme.TextWhite
import com.android.birdlens.ui.theme.VeryDarkGreenBase

data class TutorialStepInfo(
    val key: String,
    val text: String,
    val targetRect: Rect?,
    val isCircleSpotlight: Boolean = true
)

@Composable
fun TutorialOverlay(
    isVisible: Boolean,
    currentStep: TutorialStepInfo?,
    totalSteps: Int,
    currentStepIndex: Int,
    contentPadding: PaddingValues, // Accept the scaffold's content padding
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    // Logic: Get the status bar height in pixels.
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()

    // Calculate content padding in pixels.
    val topPaddingPx = with(density) { contentPadding.calculateTopPadding().toPx() }
    val leftPaddingPx = with(density) { contentPadding.calculateLeftPadding(layoutDirection).toPx() }

    // Logic: The total offset from the top of the window includes both the status bar and the top bar.
    val totalTopOffset = topPaddingPx + statusBarHeightPx

    AnimatedVisibility(
        visible = isVisible && currentStep != null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { /* Consume clicks */ }
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = Color.Black.copy(alpha = 0.8f))

                currentStep?.targetRect?.let { rect ->
                    // Logic: Use the new totalTopOffset for translation.
                    val adjustedRect = rect.translate(-leftPaddingPx, -totalTopOffset)

                    // Define Dp to Px conversions here, within the DrawScope, which is a Density scope
                    val inflatedAmountOval = 12.dp.toPx()
                    val inflatedAmountRoundRect = 8.dp.toPx()
                    val cornerRadius = 16.dp.toPx()

                    val path = Path().apply {
                        if (currentStep.isCircleSpotlight) {
                            addOval(adjustedRect.inflate(inflatedAmountOval)) // Inflate for a bit of padding
                        } else {
                            addRoundRect(
                                RoundRect(
                                    rect = adjustedRect.inflate(inflatedAmountRoundRect),
                                    radiusX = cornerRadius,
                                    radiusY = cornerRadius
                                )
                            )
                        }
                    }
                    clipPath(path = path) {
                        drawRect(color = Color.Transparent, blendMode = BlendMode.Clear)
                    }
                }
            }

            currentStep?.let { step ->
                val targetRect = step.targetRect
                val screenHeightPx = LocalConfiguration.current.screenHeightDp * density.density
                val isTooltipAbove = targetRect != null && targetRect.center.y > screenHeightPx / 1.9

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (targetRect != null) {
                        if (isTooltipAbove) {
                            // This box takes all the space above the spotlight
                            // Logic: Use the new totalTopOffset for positioning.
                            val boxHeight = with(density) { (targetRect.top - totalTopOffset).toDp() }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(boxHeight.coerceAtLeast(0.dp)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                // Place tooltip at the bottom of this box
                                Box(modifier = Modifier.padding(bottom = 24.dp)) {
                                    TooltipBox(step.text)
                                }
                            }
                        } else { // Tooltip below
                            // Spacer to occupy space down to the bottom of the spotlight
                            // Logic: Use the new totalTopOffset for positioning.
                            val spacerHeight = with(density) { (targetRect.bottom - totalTopOffset).toDp() }
                            Spacer(modifier = Modifier.height(spacerHeight.coerceAtLeast(0.dp)))
                            // Tooltip appears right after the spacer
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Box(modifier = Modifier.padding(top = 24.dp)) {
                                    TooltipBox(step.text)
                                }
                            }
                        }
                    } else { // No target rect, so tooltip is in the middle of the available space
                        Spacer(modifier = Modifier.weight(1f))
                        TooltipBox(step.text)
                    }


                    Spacer(modifier = Modifier.weight(1f)) // Pushes navigation to the bottom

                    TutorialNavigation(
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSkip = onSkip,
                        isFirstStep = currentStepIndex == 0,
                        isLastStep = currentStepIndex == totalSteps - 1,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Rect.inflate(amount: Float): Rect {
    return Rect(
        left = this.left - amount,
        top = this.top - amount,
        right = this.right + amount,
        bottom = this.bottom + amount
    )
}

@Composable
fun TooltipBox(text: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.95f)),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Text(
            text = text,
            color = TextWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            lineHeight = 22.sp,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TutorialNavigation(
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    isFirstStep: Boolean,
    isLastStep: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onSkip) {
            Text("Skip", color = TextWhite.copy(alpha = 0.8f))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onPrevious,
                enabled = !isFirstStep,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isFirstStep) Color.Gray.copy(alpha = 0.3f) else ButtonGreen)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous", tint = TextWhite)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = GreenWave2)
            ) {
                Text(if (isLastStep) "Finish" else "Next", color = VeryDarkGreenBase, fontWeight = FontWeight.Bold)
                if (!isLastStep) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = VeryDarkGreenBase)
                }
            }
        }
    }
}