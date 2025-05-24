// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/welcome/WelcomeScreen.kt
package com.android.birdlens.presentation.ui.screens.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.birdlens.ui.theme.*

@Composable
fun WelcomeScreen(
        onLoginClicked: () -> Unit,
        onNewUserClicked: () -> Unit,
        modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Background with gradient waves
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Base dark background
            drawRect(color = GreenDeep)

            // Wave 1 (Top-left to mid)
            val path1 =
                    Path().apply {
                        moveTo(0f, canvasHeight * 0.1f)
                        cubicTo(
                                canvasWidth * 0.2f,
                                canvasHeight * 0.05f,
                                canvasWidth * 0.3f,
                                canvasHeight * 0.4f,
                                canvasWidth * 0.6f,
                                canvasHeight * 0.35f
                        )
                        cubicTo(
                                canvasWidth * 0.9f,
                                canvasHeight * 0.3f,
                                canvasWidth * 1.1f,
                                canvasHeight * 0.6f, // extend beyond for smoothness
                                canvasWidth * 0.7f,
                                canvasHeight * 0.7f
                        )
                        lineTo(0f, canvasHeight * 0.8f)
                        close()
                    }
            drawPath(
                    path = path1,
                    brush =
                            Brush.radialGradient(
                                    colors =
                                            listOf(
                                                    GreenWave1.copy(alpha = 0.8f),
                                                    GreenWave3.copy(alpha = 0.6f),
                                                    GreenDeep.copy(alpha = 0.3f)
                                            ),
                                    center = Offset(canvasWidth * 0.2f, canvasHeight * 0.2f),
                                    radius = canvasWidth * 0.8f
                            )
            )

            // Wave 2 (Bottom-right to mid) - more subtle
            val path2 =
                    Path().apply {
                        moveTo(canvasWidth, canvasHeight * 0.5f)
                        cubicTo(
                                canvasWidth * 0.8f,
                                canvasHeight * 0.6f,
                                canvasWidth * 0.7f,
                                canvasHeight * 0.3f,
                                canvasWidth * 0.4f,
                                canvasHeight * 0.4f
                        )
                        cubicTo(
                                canvasWidth * 0.1f,
                                canvasHeight * 0.5f,
                                canvasWidth * 0.0f,
                                canvasHeight * 0.9f,
                                canvasWidth * 0.3f,
                                canvasHeight // extend beyond
                        )
                        lineTo(canvasWidth, canvasHeight)
                        close()
                    }
            drawPath(
                    path = path2,
                    brush =
                            Brush.linearGradient(
                                    colors =
                                            listOf(
                                                    GreenWave4.copy(alpha = 0.4f),
                                                    GreenWave1.copy(alpha = 0.3f),
                                                    GreenDeep.copy(alpha = 0.1f)
                                            ),
                                    start = Offset(canvasWidth * 0.8f, canvasHeight * 0.5f),
                                    end = Offset(canvasWidth * 0.3f, canvasHeight)
                            )
            )
        }

        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(16.dp) // General padding for screen content from edges
                                .statusBarsPadding() // Add padding for the status bar
                                .navigationBarsPadding(), // Add padding for the navigation bar
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.2f)) // Pushes "Welcome" down a bit

            Text(
                    text = "Welcome",
                    style =
                            TextStyle(
                                    // Consider adding a custom font here if you have one
                                    // fontFamily = ...,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 64.sp,
                                    color = TextWhite,
                                    letterSpacing = 1.5.sp
                            ),
                    modifier = Modifier.padding(bottom = 32.dp)
            )

            Spacer(modifier = Modifier.weight(0.8f)) // Pushes button card to bottom

            // Semi-transparent card for buttons
            Surface(
                    color = CardBackground,
                    shape = RoundedCornerShape(24.dp), // Rounded on all corners
                    modifier = Modifier.fillMaxWidth() // This will make the Surface take the full
                    // width of its parent Column
                    // The horizontal padding was removed from here.
                    ) {
                Column(
                        modifier =
                                Modifier.padding(
                                        vertical = 32.dp,
                                        horizontal = 24.dp
                                ), // Inner padding for buttons
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                            onClick = onLoginClicked,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("Login", color = TextWhite, fontSize = 16.sp) }

                    Button(
                            onClick = onNewUserClicked,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("I am a new user", color = TextWhite, fontSize = 16.sp) }
                }
            }
            Spacer(modifier = Modifier.height(32.dp)) // Space at the very bottom
        }
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun WelcomeScreenPreview() {
    BirdlensTheme { WelcomeScreen(onLoginClicked = {}, onNewUserClicked = {}) }
}
