// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/language/LanguageSelectionScreen.kt
package com.android.birdlens.presentation.ui.screens.language

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.birdlens.MainActivity
import com.android.birdlens.R
import com.android.birdlens.data.LanguageManager
import com.android.birdlens.presentation.ui.components.AuthScreenLayout
import com.android.birdlens.ui.theme.ButtonGreen
import com.android.birdlens.ui.theme.TextWhite

@Composable
fun LanguageSelectionScreen() {
    val context = LocalContext.current

    AuthScreenLayout {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Welcome to Birdlens",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Please select your language\nVui lòng chọn ngôn ngữ của bạn",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            LanguageButton(
                text = stringResource(R.string.language_english),
                languageCode = LanguageManager.LANGUAGE_ENGLISH,
                onClick = {
                    // Logic: When a language is chosen, save the preference and recreate the activity
                    // to apply the new locale throughout the entire application.
                    LanguageManager.changeLanguage(context, it)
                    (context as? MainActivity)?.recreateActivity()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            LanguageButton(
                text = stringResource(R.string.language_vietnamese),
                languageCode = LanguageManager.LANGUAGE_VIETNAMESE,
                onClick = {
                    LanguageManager.changeLanguage(context, it)
                    (context as? MainActivity)?.recreateActivity()
                }
            )
        }
    }
}

@Composable
private fun LanguageButton(text: String, languageCode: String, onClick: (String) -> Unit) {
    Button(
        onClick = { onClick(languageCode) },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(56.dp)
    ) {
        Text(text, fontSize = 18.sp, color = TextWhite)
    }
}