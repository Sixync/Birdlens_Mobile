package com.android.birdlens.presentation.ui.screens.birdidentifier

import android.Manifest
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.viewmodel.BirdIdentifierUiState
import com.android.birdlens.presentation.viewmodel.BirdIdentifierViewModel
import com.android.birdlens.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BirdIdentifierScreen(
    navController: NavController,
    viewModel: BirdIdentifierViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            // Ensure bitmap is mutable and in ARGB_8888 for Gemini
            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            selectedImageBitmap = mutableBitmap
            viewModel.identifyBirdFromImage(mutableBitmap)
        }
    }

    // Permission Handler
    val readImagesPermissionState = rememberPermissionState(permission = Manifest.permission.READ_MEDIA_IMAGES)

    AppScaffold(
        navController = navController,
        topBar = { SimpleTopAppBar("AI Bird Identifier", onNavigateBack = { navController.popBackStack() }) },
        showBottomBar = false
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                    .clickable {
                        if (readImagesPermissionState.status.isGranted) {
                            imagePickerLauncher.launch("image/*")
                        } else {
                            readImagesPermissionState.launchPermissionRequest()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageBitmap != null) {
                    Image(
                        bitmap = selectedImageBitmap!!.asImageBitmap(),
                        contentDescription = "Selected bird image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Add a photo",
                            tint = TextWhite.copy(alpha = 0.8f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to select a bird photo",
                            color = TextWhite.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (!readImagesPermissionState.status.isGranted && readImagesPermissionState.status.shouldShowRationale) {
                Text(
                    "Permission to read images is needed to identify birds from your photos.",
                    color = TextWhite.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // State Handling Section
            when (val state = uiState) {
                is BirdIdentifierUiState.Idle -> {
                    Text("Select an image to start.", color = TextWhite.copy(alpha = 0.7f))
                }
                is BirdIdentifierUiState.Loading -> {
                    CircularProgressIndicator(color = GreenWave2)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.message, color = TextWhite.copy(alpha = 0.9f))
                }
                is BirdIdentifierUiState.Error -> {
                    Text(state.errorMessage, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
                is BirdIdentifierUiState.Success -> {
                    ConversationArea(
                        identifiedBird = state.identifiedBird,
                        chatResponse = state.chatResponse,
                        onQuestionAsked = { question -> viewModel.askQuestion(question) }
                    )
                }
            }
        }
    }
}

@Composable
fun ColumnScope.ConversationArea(
    identifiedBird: String,
    chatResponse: String,
    onQuestionAsked: (String) -> Unit
) {
    var questionText by remember { mutableStateOf("") }

    // Identified Bird Title
    Text(
        text = identifiedBird,
        style = MaterialTheme.typography.headlineSmall,
        color = GreenWave2,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Chat Response Area
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = chatResponse,
            color = TextWhite,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Input Area
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = questionText,
            onValueChange = { questionText = it },
            placeholder = { Text("Ask about the ${identifiedBird}...") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                disabledContainerColor = CardBackground,
                cursorColor = TextWhite,
                focusedIndicatorColor = GreenWave2,
                unfocusedIndicatorColor = TextWhite.copy(alpha = 0.5f),
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                if (questionText.isNotBlank()) {
                    onQuestionAsked(questionText)
                    questionText = ""
                }
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(GreenWave2),
            enabled = questionText.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send question",
                tint = VeryDarkGreenBase
            )
        }
    }
}