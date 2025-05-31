// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/welcome/WelcomeScreen.kt
package com.android.birdlens.presentation.ui.screens.welcome

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource // Make sure this is imported
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.birdlens.R // Make sure this is imported for R.string access
import com.android.birdlens.presentation.ui.components.AuthScreenLayout
import com.android.birdlens.ui.theme.AuthCardBackground
import com.android.birdlens.ui.theme.ButtonGreen
import com.android.birdlens.ui.theme.BirdlensTheme
import com.android.birdlens.ui.theme.TextWhite

@Composable
fun WelcomeScreen(
    onLoginClicked: () -> Unit,
    onNewUserClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthScreenLayout(modifier = modifier) {
        Spacer(modifier = Modifier.weight(0.25f))

        Text(
            // CRITICAL: Use stringResource for the title
            text = stringResource(id = R.string.welcome_title),
            style =
            TextStyle(
                fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 72.sp,
                color = TextWhite,
                letterSpacing = 1.5.sp
            ),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Spacer(modifier = Modifier.weight(0.75f))

        Surface(
            color = AuthCardBackground,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 32.dp, bottomEnd = 32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier =
                Modifier.padding(
                    vertical = 32.dp,
                    horizontal = 24.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onLoginClicked,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    // CRITICAL: Use stringResource for the button text
                    Text(
                        stringResource(id = R.string.login),
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = onNewUserClicked,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    // CRITICAL: Use stringResource for the button text
                    Text(
                        stringResource(id = R.string.i_am_new_user),
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 48.dp)) // Ensure space above nav bar
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun WelcomeScreenPreview() {
    BirdlensTheme {
        WelcomeScreen(onLoginClicked = {}, onNewUserClicked = {})
    }
}