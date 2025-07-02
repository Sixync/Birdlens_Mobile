// app/src/main/java/com/android/birdlens/presentation/ui/screens/map/components/TutorialOverlay.kt
package com.android.birdlens.presentation.ui.screens.map.components

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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.birdlens.presentation.ui.screens.map.TutorialStepInfo
import com.android.birdlens.ui.theme.ButtonGreen
import com.android.birdlens.ui.theme.CardBackground
import com.android.birdlens.ui.theme.GreenWave2
import com.android.birdlens.ui.theme.TextWhite
import com.android.birdlens.ui.theme.VeryDarkGreenBase

@Composable
fun TutorialOverlay(
    isVisible: Boolean,
    currentStep: TutorialStepInfo?,
    totalSteps: Int,
    currentStepIndex: Int,
    contentPadding: PaddingValues,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()
    val topPaddingPx = with(density) { contentPadding.calculateTopPadding().toPx() }
    val leftPaddingPx = with(density) { contentPadding.calculateLeftPadding(layoutDirection).toPx() }
    val totalTopOffset = topPaddingPx + statusBarHeightPx

    AnimatedVisibility(
        visible = isVisible && currentStep != null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { /* Consume clicks to prevent interaction with underlying UI */ }
                )
        ) {
            val fullHeightDp = this.maxHeight

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            ) {
                drawRect(color = Color.Black.copy(alpha = 0.8f))

                currentStep?.targetRect?.let { rect ->
                    val adjustedRect = rect.translate(-leftPaddingPx, -totalTopOffset)

                    val inflatedAmountOval = 6.dp.toPx() // Tighter spotlight
                    val inflatedAmountRoundRect = 8.dp.toPx()
                    val cornerRadius = 16.dp.toPx()

                    val path = Path().apply {
                        if (currentStep.isCircleSpotlight) {
                            addOval(adjustedRect.inflate(inflatedAmountOval))
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

            Box(modifier = Modifier.fillMaxSize()) {
                currentStep?.let { step ->
                    val targetRect = step.targetRect
                    val isTooltipAbove = targetRect != null && with(density) { (targetRect.center.y - totalTopOffset).toDp() } > (fullHeightDp / 2)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (targetRect != null) {
                            if (isTooltipAbove) {
                                val spaceAboveDp = with(density) { (targetRect.top - totalTopOffset).toDp() }
                                val tooltipHeightApproximation = 120.dp
                                val bottomPadding = 24.dp
                                Spacer(modifier = Modifier.height((spaceAboveDp - tooltipHeightApproximation - bottomPadding).coerceAtLeast(0.dp)))
                                TooltipBox(step.text)
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                                val spaceBelowDp = fullHeightDp - with(density) { (targetRect.bottom - totalTopOffset).toDp() }
                                val navControlsHeightApproximation = 80.dp
                                val topPadding = 24.dp
                                TooltipBox(step.text)
                                Spacer(modifier = Modifier.height((spaceBelowDp - navControlsHeightApproximation - topPadding).coerceAtLeast(0.dp)))
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                            TooltipBox(step.text)
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    TutorialNavigation(
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSkip = onSkip,
                        isFirstStep = currentStepIndex == 0,
                        isLastStep = currentStepIndex == totalSteps - 1,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .navigationBarsPadding()
                    )
                }
            }
        }
    }
}

private fun Rect.inflate(amount: Float): Rect {
    return Rect(
        left = this.left - amount,
        top = this.top - amount,
        right = this.right + amount,
        bottom = this.bottom + amount
    )
}

@Composable
fun TooltipBox(text: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.95f)),
        modifier = modifier
            .fillMaxWidth(0.9f),
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