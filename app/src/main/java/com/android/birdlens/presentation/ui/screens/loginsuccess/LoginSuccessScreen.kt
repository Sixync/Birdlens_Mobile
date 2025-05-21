// EXE201/app/src/main/java/com/example/birdlens/presentation/ui/screens/loginsuccess/LoginSuccessScreen.kt
package com.android.birdlens.presentation.ui.screens.loginsuccess

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.birdlens.ui.theme.*

@Composable
fun LoginSuccessScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Reusable Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            drawRect(color = GreenDeep)
            val path1 = Path().apply {
                moveTo(0f, canvasHeight * 0.1f)
                cubicTo(canvasWidth * 0.2f, canvasHeight * 0.05f, canvasWidth * 0.3f, canvasHeight * 0.4f, canvasWidth * 0.6f, canvasHeight * 0.35f)
                cubicTo(canvasWidth * 0.9f, canvasHeight * 0.3f, canvasWidth * 1.1f, canvasHeight * 0.6f, canvasWidth * 0.7f, canvasHeight * 0.7f)
                lineTo(0f, canvasHeight * 0.8f); close()
            }
            drawPath(path = path1, brush = Brush.radialGradient(listOf(GreenWave1.copy(alpha = 0.8f), GreenWave3.copy(alpha = 0.6f), GreenDeep.copy(alpha = 0.3f)), center = Offset(canvasWidth * 0.2f, canvasHeight * 0.2f), radius = canvasWidth * 0.8f))
            val path2 = Path().apply {
                moveTo(canvasWidth, canvasHeight * 0.5f)
                cubicTo(canvasWidth * 0.8f, canvasHeight * 0.6f, canvasWidth * 0.7f, canvasHeight * 0.3f, canvasWidth * 0.4f, canvasHeight * 0.4f)
                cubicTo(canvasWidth * 0.1f, canvasHeight * 0.5f, canvasWidth * 0.0f, canvasHeight * 0.9f, canvasWidth * 0.3f, canvasHeight); lineTo(canvasWidth, canvasHeight); close()
            }
            drawPath(path = path2, brush = Brush.linearGradient(listOf(GreenWave4.copy(alpha = 0.4f), GreenWave1.copy(alpha = 0.3f), GreenDeep.copy(alpha = 0.1f)), start = Offset(canvasWidth * 0.8f, canvasHeight * 0.5f), end = Offset(canvasWidth * 0.3f, canvasHeight)))
        }

        // Centered card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp), // Give some horizontal padding to the card container
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = CardBackground,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                // .height(IntrinsicSize.Min) // Let card wrap content height
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Login Successful",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Welcome to BirdLens",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextWhite.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onContinue,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                        modifier = Modifier.size(60.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Continue",
                            tint = TextWhite,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:shape=Normal,width=360,height=640,unit=dp,dpi=480")
@Composable
fun LoginSuccessScreenPreview() {
    BirdlensTheme {
        LoginSuccessScreen(onContinue = {})
    }
}