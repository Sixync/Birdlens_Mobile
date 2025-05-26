// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/components/AuthScreenLayout.kt
package com.android.birdlens.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AuthScreenLayout(
    modifier: Modifier = Modifier,
    topContent: @Composable ColumnScope.() -> Unit = {}, // For back buttons etc.
    mainContent: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        SharedAppBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            topContent()
            mainContent()
        }
    }
}