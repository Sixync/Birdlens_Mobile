// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/community/CreatePostScreen.kt
package com.android.birdlens.presentation.ui.screens.community

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.R
import com.android.birdlens.data.local.BirdSpecies
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.viewmodel.CommunityViewModel
import com.android.birdlens.presentation.viewmodel.CreatePostViewModel
import com.android.birdlens.presentation.viewmodel.CreatePostUiState
import com.android.birdlens.presentation.viewmodel.GenericUiState
import com.android.birdlens.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    navController: NavController,
    viewModel: CreatePostViewModel,
    communityViewModel: CommunityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val createPostState by communityViewModel.createPostState.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    )

    val readImagesPermissionState = rememberPermissionState(
        permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
    )
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris: List<Uri> ->
            viewModel.onImageUrisChange(uiState.selectedImageUris + uris)
        }
    )

    LaunchedEffect(createPostState) {
        if (createPostState is GenericUiState.Success) {
            Toast.makeText(context, "Post created successfully!", Toast.LENGTH_SHORT).show()
            communityViewModel.resetCreatePostState()
            navController.popBackStack()
        } else if (createPostState is GenericUiState.Error) {
            Toast.makeText(context, (createPostState as GenericUiState.Error).message, Toast.LENGTH_LONG).show()
            communityViewModel.resetCreatePostState()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onSightingDateChange(LocalDate.ofEpochDay(it / (1000 * 60 * 60 * 24))) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    AppScaffold(
        navController = navController,
        topBar = { SimpleTopAppBar(title = "Create Post", onNavigateBack = { navController.popBackStack() }) },
        showBottomBar = false
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            PostTypeSelector(selectedType = uiState.postType, onTypeSelected = viewModel::onPostTypeChange)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = uiState.content, onValueChange = viewModel::onContentChange, label = {Text("What's on your mind?")}, modifier = Modifier
                .fillMaxWidth()
                .height(150.dp))
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = uiState.postType == "sighting") {
                SightingSpecificFields(
                    uiState = uiState,
                    viewModel = viewModel,
                    onShowDatePicker = { showDatePicker = true }
                )
            }

            ImageSelectionSection(
                selectedImageUris = uiState.selectedImageUris,
                onAddImagesClick = { if (readImagesPermissionState.status.isGranted) imagePickerLauncher.launch("image/*") else readImagesPermissionState.launchPermissionRequest() },
                onRemoveImage = { uri -> viewModel.onImageUrisChange(uiState.selectedImageUris - uri) }
            )

            LocationFields(
                uiState = uiState,
                onUseCurrentLocation = {
                    if (locationPermissionsState.allPermissionsGranted) viewModel.fetchCurrentLocation() else locationPermissionsState.launchMultiplePermissionRequest()
                },
                onPickOnMap = { navController.navigate(Screen.LocationPicker.route) }
            )

            Button(
                onClick = {
                    if (uiState.content.isBlank()) {
                        Toast.makeText(context, "Post content cannot be empty.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (uiState.postType == "sighting" && (uiState.sightingDate == null || uiState.taggedSpecies == null)) {
                        Toast.makeText(context, "For a sighting, please select a date and species.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    communityViewModel.createPost(
                        content = uiState.content,
                        locationName = uiState.locationName,
                        latitude = uiState.latitude,
                        longitude = uiState.longitude,
                        privacyLevel = uiState.privacyLevel,
                        type = uiState.postType,
                        isFeatured = uiState.isFeatured,
                        mediaUris = uiState.selectedImageUris,
                        sightingDate = viewModel.getSightingDateAsString(),
                        taggedSpeciesCode = uiState.taggedSpecies?.speciesCode
                    )
                },
                enabled = createPostState !is GenericUiState.Loading,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(top = 16.dp)
            ) {
                if (createPostState is GenericUiState.Loading) {
                    CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(24.dp))
                } else {
                    Text("Create Post", color = TextWhite)
                }
            }
        }
    }
}

@Composable
fun PostTypeSelector(selectedType: String, onTypeSelected: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(CardBackground.copy(alpha = 0.3f))
            .height(48.dp)
    ) {
        val selectedIndex = if (selectedType == "general") 0 else 1
        TabRow(
            selectedTabIndex = selectedIndex,
            containerColor = Color.Transparent,
            contentColor = TextWhite,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedIndex])
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(50)),
                    color = GreenWave2.copy(alpha = 0.8f)
                )
            },
            divider = {}
        ) {
            Tab(selected = selectedType == "general", onClick = { onTypeSelected("general") }, text = { Text("General Post") }, selectedContentColor = TextWhite, unselectedContentColor = TextWhite.copy(alpha = 0.8f))
            Tab(selected = selectedType == "sighting", onClick = { onTypeSelected("sighting") }, text = { Text("Sighting") }, selectedContentColor = TextWhite, unselectedContentColor = TextWhite.copy(alpha = 0.8f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SightingSpecificFields(
    uiState: CreatePostUiState,
    viewModel: CreatePostViewModel,
    onShowDatePicker: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var isSpeciesSearchFocused by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = uiState.sightingDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "",
            onValueChange = {}, readOnly = true,
            label = { Text("Sighting Date") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onShowDatePicker),
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Sighting Date") }
        )
        Spacer(Modifier.height(8.dp))

        Box {
            OutlinedTextField(
                value = uiState.speciesSearchQuery,
                onValueChange = viewModel::onSpeciesSearchQueryChange,
                label = { Text("Bird Species") },
                placeholder = { Text("Start typing bird name...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isSpeciesSearchFocused = it.isFocused },
                leadingIcon = { Icon(painterResource(id = R.drawable.ic_bird_placeholder), contentDescription = "Species") },
                trailingIcon = { if (uiState.isSearchingSpecies) CircularProgressIndicator(modifier = Modifier.size(20.dp)) }
            )
            DropdownMenu(
                expanded = uiState.speciesSearchResults.isNotEmpty() && isSpeciesSearchFocused,
                onDismissRequest = { isSpeciesSearchFocused = false } // Dismiss when focus is lost or clicked outside
            ) {
                uiState.speciesSearchResults.forEach { bird ->
                    DropdownMenuItem(
                        text = { Text("${bird.commonName} (${bird.scientificName})") },
                        onClick = {
                            viewModel.onSpeciesSelected(bird)
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ImageSelectionSection(
    selectedImageUris: List<Uri>,
    onAddImagesClick: () -> Unit,
    onRemoveImage: (Uri) -> Unit
) {
    Button(onClick = onAddImagesClick) {
        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Images")
        Spacer(Modifier.width(8.dp))
        Text("Add Photos")
    }
    if (selectedImageUris.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            items(selectedImageUris) { uri ->
                Box(Modifier.size(100.dp)) {
                    Image(painter = rememberAsyncImagePainter(model = uri), contentDescription = null, modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    IconButton(onClick = { onRemoveImage(uri) }, modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                        Icon(Icons.Default.Clear, contentDescription = "Remove", tint=Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun LocationFields(
    uiState: CreatePostUiState,
    onUseCurrentLocation: () -> Unit,
    onPickOnMap: () -> Unit
) {
    Spacer(Modifier.height(16.dp))
    Text("Location", style = MaterialTheme.typography.titleMedium)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onUseCurrentLocation, colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen.copy(alpha = 0.7f))) {
            Icon(Icons.Default.MyLocation, contentDescription = "Use Current Location", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Current")
        }
        Button(onClick = onPickOnMap, colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen.copy(alpha = 0.7f))) {
            Icon(Icons.Default.Map, contentDescription = "Pick on Map", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("From Map")
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = uiState.latitude?.let { "%.4f".format(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = {Text("Latitude")},
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = uiState.longitude?.let { "%.4f".format(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = {Text("Longitude")},
            modifier = Modifier.weight(1f)
        )
    }
}