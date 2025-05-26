// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/loginsuccess/LoginSuccessScreen.kt
package com.android.birdlens.presentation.ui.screens.loginsuccess

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.birdlens.presentation.ui.components.AuthScreenLayout
import com.android.birdlens.ui.theme.*

@Composable
fun LoginSuccessScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthScreenLayout(modifier = modifier) { // This is the ColumnScope from AuthScreenLayout's mainContent
        // This Spacer will take up all available vertical space,
        // pushing the Surface to the bottom of the mainContent area.
        Spacer(modifier = Modifier.weight(1f))

        Surface(
            color = AuthCardBackground,
            shape = RoundedCornerShape(32.dp), // Consistent rounding with other auth cards
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp) // Horizontal padding for the card itself
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 48.dp), // Inner padding for card content
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
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Welcome to BirdLens",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextWhite.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Normal
                    )
                )
                Spacer(modifier = Modifier.height(40.dp))
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
        // Add some space at the bottom, below the card, but above any system navigation bar
        // AuthScreenLayout already includes navigationBarsPadding for its root Column.
        // This spacer is for visual separation if the card is very close to the bottom edge.
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun LoginSuccessScreenPreview() {
    BirdlensTheme {
        LoginSuccessScreen(onContinue = {})
    }
}