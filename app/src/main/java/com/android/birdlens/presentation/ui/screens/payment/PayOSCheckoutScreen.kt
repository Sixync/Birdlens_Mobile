// path: EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/payment/PayOSCheckoutScreen.kt
package com.android.birdlens.presentation.ui.screens.payment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.ui.theme.GreenDeep
import com.android.birdlens.ui.theme.TextWhite
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayOSCheckoutScreen(
    navController: NavController,
    checkoutUrl: String
) {
    val state = rememberWebViewState(url = checkoutUrl)
    var isLoading by remember { mutableStateOf(true) }

    // This is the updated WebViewClient with interception logic.
    val webViewClient = remember(navController) {
        object : AccompanistWebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                isLoading = true
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                isLoading = false
            }

            // Logic: This function intercepts every URL navigation attempt within the WebView.
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: ""
                Log.d("PayOSCheckout", "WebView trying to load URL: $url")

                // Check if the URL is our custom deep link for success.
                if (url.startsWith("app://birdlens/payment-success")) {
                    Log.d("PayOSCheckout", "Success deep link intercepted. Navigating to success screen.")
                    // Navigate to the success screen and clear the back stack.
                    navController.navigate(Screen.PaymentResult.createRoute(true)) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                    return true // Return true to tell the WebView we've handled this URL.
                }

                // Check if the URL is our custom deep link for cancellation.
                if (url.startsWith("app://birdlens/payment-cancel")) {
                    Log.d("PayOSCheckout", "Cancel deep link intercepted. Navigating to cancel screen.")
                    // Navigate to the cancel screen and clear the back stack.
                    navController.navigate(Screen.PaymentResult.createRoute(false)) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                    return true // We've handled it.
                }

                // For any other URL (e.g., links within the PayOS page), let the WebView handle it.
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Complete Payment", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = GreenDeep)
            )
        },
        containerColor = GreenDeep
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {
            WebView(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onCreated = { webView ->
                    webView.settings.javaScriptEnabled = true
                },
                client = webViewClient
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = TextWhite
                )
            }
        }
    }
}