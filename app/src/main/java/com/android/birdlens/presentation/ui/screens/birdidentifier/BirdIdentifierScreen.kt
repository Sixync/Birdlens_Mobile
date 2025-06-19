// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/birdidentifier/BirdIdentifierScreen.kt
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.R
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
    viewModel: BirdIdentifierViewModel = viewModel(),
    // Logic: Accept the ad trigger lambda.
    triggerAd: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var userPrompt by remember { mutableStateOf("") }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            selectedImageBitmap = mutableBitmap
        }
    }

    val readImagesPermissionState = rememberPermissionState(permission = Manifest.permission.READ_MEDIA_IMAGES)

    // Logic: Trigger an ad when the identification is successful.
    LaunchedEffect(uiState) {
        if (uiState is BirdIdentifierUiState.ConversationReady) {
            triggerAd()
        }
    }

    AppScaffold(
        navController = navController,
        topBar = { SimpleTopAppBar(stringResource(id = R.string.bird_identifier_title), onNavigateBack = { navController.popBackStack() }) },
        showBottomBar = false
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image Section is always visible unless in conversation
            if (uiState !is BirdIdentifierUiState.ConversationReady) {
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
                                text = stringResource(id = R.string.bird_identifier_select_prompt),
                                color = TextWhite.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                if (!readImagesPermissionState.status.isGranted && readImagesPermissionState.status.shouldShowRationale) {
                    Text(
                        stringResource(id = R.string.bird_identifier_permission_rationale),
                        color = TextWhite.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // State Handling Section
            when (val state = uiState) {
                is BirdIdentifierUiState.Idle -> {
                    UserInputSection(
                        userPrompt = userPrompt,
                        onPromptChange = { userPrompt = it },
                        selectedImageBitmap = selectedImageBitmap,
                        onIdentifyClick = {
                            if (selectedImageBitmap != null) {
                                viewModel.startChatWithImage(selectedImageBitmap!!, userPrompt)
                            } else {
                                viewModel.startChatWithText(userPrompt)
                            }
                        },
                        isLoading = false
                    )
                }
                is BirdIdentifierUiState.Loading -> {
                    CircularProgressIndicator(color = GreenWave2)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.message, color = TextWhite.copy(alpha = 0.9f))
                }
                is BirdIdentifierUiState.Error -> {
                    Text(state.errorMessage, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Button(onClick = { viewModel.resetState() }) {
                        Text(stringResource(id = R.string.bird_identifier_try_again))
                    }
                }
                is BirdIdentifierUiState.IdentificationSuccess -> {
                    PossibilitiesList(
                        possibilities = state.possibilities,
                        onBirdSelected = { birdName ->
                            viewModel.selectBirdAndStartConversation(birdName, state.pendingPrompt)
                        }
                    )
                }
                is BirdIdentifierUiState.ConversationReady -> {
                    ConversationArea(
                        state = state,
                        onQuestionAsked = { question -> viewModel.askQuestion(question) }
                    )
                }
            }
        }
    }
}
// Logic: The rest of the file (UserInputSection, PossibilitiesList, ConversationArea) remains unchanged.
// The code is omitted for brevity but should be kept in your file.
@Composable
fun UserInputSection(
    userPrompt: String,
    onPromptChange: (String) -> Unit,
    selectedImageBitmap: Bitmap?,
    onIdentifyClick: () -> Unit,
    isLoading: Boolean
) {
    val placeholderText = if (selectedImageBitmap == null) {
        stringResource(id = R.string.bird_identifier_text_input_placeholder_no_image)
    } else {
        stringResource(id = R.string.bird_identifier_text_input_placeholder_with_image)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = userPrompt,
            onValueChange = onPromptChange,
            placeholder = { Text(placeholderText) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                cursorColor = TextWhite,
                focusedIndicatorColor = GreenWave2,
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onIdentifyClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(GreenWave2),
            enabled = (userPrompt.isNotBlank() || selectedImageBitmap != null) && !isLoading
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(id = R.string.bird_identifier_start_conversation),
                tint = VeryDarkGreenBase
            )
        }
    }
}

@Composable
fun PossibilitiesList(
    possibilities: List<String>,
    onBirdSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.bird_identifier_possibilities_title),
            style = MaterialTheme.typography.titleLarge,
            color = TextWhite,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.bird_identifier_possibilities_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextWhite.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(possibilities) { birdName ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBirdSelected(birdName) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.8f))
                ) {
                    Text(
                        text = birdName,
                        modifier = Modifier.padding(16.dp),
                        color = GreenWave2,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


@Composable
fun ColumnScope.ConversationArea(
    state: BirdIdentifierUiState.ConversationReady,
    onQuestionAsked: (String) -> Unit
) {
    var followUpQuestion by remember { mutableStateOf("") }

    Text(
        text = state.identifiedBird,
        style = MaterialTheme.typography.headlineSmall,
        color = GreenWave2,
        fontWeight = FontWeight.Bold
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        state.imageUrl?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = rememberAsyncImagePainter(model = it),
                contentDescription = "Image of ${state.identifiedBird}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = state.chatResponse,
                color = TextWhite,
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = followUpQuestion,
            onValueChange = { followUpQuestion = it },
            placeholder = { Text(stringResource(id = R.string.bird_identifier_follow_up_placeholder)) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                cursorColor = TextWhite,
                focusedIndicatorColor = GreenWave2,
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                if (followUpQuestion.isNotBlank()) {
                    onQuestionAsked(followUpQuestion)
                    followUpQuestion = ""
                }
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(GreenWave2),
            enabled = followUpQuestion.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(id = R.string.bird_identifier_send_question),
                tint = VeryDarkGreenBase
            )
        }
    }
}