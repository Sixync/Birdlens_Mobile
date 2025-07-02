// app/src/main/java/com/android/birdlens/presentation/ui/screens/birdidentifier/BirdIdentifierScreen.kt
package com.android.birdlens.presentation.ui.screens.birdidentifier

import android.Manifest
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.android.birdlens.R
import com.android.birdlens.data.model.ChatMessage
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.viewmodel.BirdIdentifierUiState
import com.android.birdlens.presentation.viewmodel.BirdIdentifierViewModel
import com.android.birdlens.presentation.viewmodel.BirdPossibility
import com.android.birdlens.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BirdIdentifierScreen(
    navController: NavController,
    viewModel: BirdIdentifierViewModel = viewModel(),
    triggerAd: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
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
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            when (val state = uiState) {
                is BirdIdentifierUiState.ConversationReady -> {
                    ConversationScreen(
                        uiState = state,
                        onSendMessage = { viewModel.askQuestion(it) }
                    )
                }
                else -> {
                    InitialLayout(
                        state = state,
                        selectedImageBitmap = selectedImageBitmap,
                        onImageSelectRequest = {
                            if (readImagesPermissionState.status.isGranted) {
                                imagePickerLauncher.launch("image/*")
                            } else {
                                readImagesPermissionState.launchPermissionRequest()
                            }
                        },
                        onIdentify = { text ->
                            if (selectedImageBitmap != null) {
                                viewModel.startChatWithImage(selectedImageBitmap!!, text)
                            } else {
                                viewModel.startChatWithText(text)
                            }
                        },
                        onPossibilitySelected = { birdName ->
                            viewModel.selectBirdAndStartConversation(birdName)
                        },
                        onRetry = { viewModel.resetState() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun InitialLayout(
    state: BirdIdentifierUiState,
    selectedImageBitmap: Bitmap?,
    onImageSelectRequest: () -> Unit,
    onIdentify: (String) -> Unit,
    onPossibilitySelected: (String) -> Unit,
    onRetry: () -> Unit
) {
    var userPrompt by remember { mutableStateOf("") }
    val readImagesPermissionState = rememberPermissionState(permission = Manifest.permission.READ_MEDIA_IMAGES)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ImageSelectionBox(
                bitmap = selectedImageBitmap,
                onClick = onImageSelectRequest
            )

            if (!readImagesPermissionState.status.isGranted && readImagesPermissionState.status.shouldShowRationale) {
                Text(
                    stringResource(id = R.string.bird_identifier_permission_rationale),
                    color = TextWhite.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(targetState = state, label = "StateContent") { targetState ->
                when (targetState) {
                    is BirdIdentifierUiState.Idle -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            InitialPromptView()
                            Spacer(modifier = Modifier.height(16.dp))
                            UserInputSection(
                                userPrompt = userPrompt,
                                onPromptChange = { userPrompt = it },
                                selectedImageBitmap = selectedImageBitmap,
                                onIdentifyClick = { onIdentify(userPrompt) },
                                isLoading = false
                            )
                        }
                    }
                    is BirdIdentifierUiState.IdentificationSuccess -> {
                        PossibilitiesList(
                            possibilities = targetState.possibilities,
                            onBirdSelected = onPossibilitySelected
                        )
                    }
                    else -> {}
                }
            }
        }

        when (state) {
            is BirdIdentifierUiState.Loading -> LoadingView(message = state.message)
            is BirdIdentifierUiState.Error -> ErrorView(message = state.errorMessage, onRetry = onRetry)
            else -> {}
        }
    }
}

@Composable
private fun ImageSelectionBox(bitmap: Bitmap?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground.copy(alpha = 0.4f))
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
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
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun InitialPromptView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "AI Bird Identifier",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Upload a photo or just ask a question to start identifying.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(enabled = false, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = GreenWave2)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = TextWhite,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "An Error Occurred",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen.copy(alpha = 0.9f))
                ) {
                    Text(stringResource(id = R.string.bird_identifier_try_again))
                }
            }
        }
    }
}

@Composable
fun ConversationScreen(
    uiState: BirdIdentifierUiState.ConversationReady,
    onSendMessage: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        ConversationFeed(
            messages = uiState.messages,
            modifier = Modifier.weight(1f)
        )
        InputBar(
            onSendMessage = onSendMessage,
            isLoading = uiState.isLoading
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationFeed(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            items(
                items = messages.reversed(),
                key = { it.id }
            ) { message ->
                Box(modifier = Modifier.animateItem()) {
                    when (message.role) {
                        "user" -> UserMessageBubble(message)
                        "assistant" -> AssistantMessageBubble(message)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent, VeryDarkGreenBase),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
    }
}

@Composable
fun UserMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary // Use solid color for user messages
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun AssistantMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground.copy(alpha = 0.6f) // Increased opacity for better contrast
            ),
            shape = RoundedCornerShape(
                topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                message.imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Bird identification image",
                        placeholder = painterResource(id = R.drawable.ic_bird_placeholder),
                        error = painterResource(id = R.drawable.ic_bird_placeholder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun InputBar(
    onSendMessage: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        tonalElevation = 0.dp,
        modifier = modifier,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about this bird...") },
                enabled = !isLoading,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = CardBackground.copy(alpha = 0.6f),
                    unfocusedContainerColor = CardBackground.copy(alpha = 0.6f),
                    cursorColor = GreenWave2,
                    focusedIndicatorColor = GreenWave2,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (textFieldValue.text.isNotBlank()) {
                        onSendMessage(textFieldValue.text)
                        textFieldValue = TextFieldValue()
                        keyboardController?.hide()
                    }
                },
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                containerColor = ButtonGreen.copy(alpha = 0.9f),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message"
                    )
                }
            }
        }
    }
}

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
            placeholder = { Text(placeholderText, style = MaterialTheme.typography.bodyLarge) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(50.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = CardBackground.copy(alpha = 0.6f),
                unfocusedContainerColor = CardBackground.copy(alpha = 0.6f),
                cursorColor = GreenWave2,
                focusedIndicatorColor = GreenWave2,
                unfocusedIndicatorColor = Color.Transparent,
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onIdentifyClick,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(ButtonGreen.copy(alpha = 0.9f)),
            enabled = (userPrompt.isNotBlank() || selectedImageBitmap != null) && !isLoading
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(id = R.string.bird_identifier_start_conversation),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun PossibilitiesList(
    possibilities: List<BirdPossibility>,
    onBirdSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.bird_identifier_possibilities_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.bird_identifier_possibilities_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(possibilities) { bird ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBirdSelected(bird.name) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(durationMillis = 500))
                        ) {
                            AsyncImage(
                                model = bird.imageUrl,
                                contentDescription = "Image of ${bird.name}",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = bird.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                            color = GreenWave2,
                        )
                    }
                }
            }
        }
    }
}