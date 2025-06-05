// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/admin/subscriptions/CreateSubscriptionScreen.kt
package com.android.birdlens.presentation.ui.screens.admin.subscriptions

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.ui.screens.login.CustomTextField
import com.android.birdlens.presentation.viewmodel.AdminSubscriptionViewModel
import com.android.birdlens.presentation.viewmodel.SubscriptionUIState
import com.android.birdlens.ui.theme.*

@Composable
fun CreateSubscriptionScreen(
    navController: NavController,
    adminSubscriptionViewModel: AdminSubscriptionViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var durationDays by remember { mutableStateOf("") }
    val context = LocalContext.current

    val createState by adminSubscriptionViewModel.createSubscriptionState.collectAsState()

    LaunchedEffect(createState) {
        when (val state = createState) {
            is SubscriptionUIState.Success -> {
                Toast.makeText(context, "Subscription '${state.data.name}' created!", Toast.LENGTH_LONG).show()
                adminSubscriptionViewModel.resetCreateSubscriptionState()
                navController.popBackStack() // Go back to list after successful creation
            }
            is SubscriptionUIState.Error -> {
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                adminSubscriptionViewModel.resetCreateSubscriptionState()
            }
            else -> { /* Idle or Loading */ }
        }
    }

    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar(
                title = "Create Subscription",
                onNavigateBack = { navController.popBackStack() }
            )
        },
        showBottomBar = false
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CustomTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Subscription Name",
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = AuthInputBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            CustomTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Description",
                modifier = Modifier.fillMaxWidth().height(100.dp), // For multi-line
                backgroundColor = AuthInputBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            CustomTextField(
                value = price,
                onValueChange = { price = it },
                placeholder = "Price (e.g., 9.99)",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                backgroundColor = AuthInputBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            CustomTextField(
                value = durationDays,
                onValueChange = { durationDays = it },
                placeholder = "Duration (days, e.g., 30)",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                backgroundColor = AuthInputBackground
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    adminSubscriptionViewModel.createSubscription(name, description, price, durationDays)
                },
                enabled = createState !is SubscriptionUIState.Loading,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp)
            ) {
                if (createState is SubscriptionUIState.Loading) {
                    CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(24.dp))
                } else {
                    Text("Create Subscription", color = TextWhite)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateSubscriptionScreenPreview() {
    BirdlensTheme {
        CreateSubscriptionScreen(navController = rememberNavController())
    }
}