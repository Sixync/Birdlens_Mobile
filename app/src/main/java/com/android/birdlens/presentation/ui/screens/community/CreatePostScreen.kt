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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.viewmodel.CommunityViewModel
import com.android.birdlens.presentation.viewmodel.GenericUiState
import com.android.birdlens.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    navController: NavController,
    communityViewModel: CommunityViewModel = viewModel()
) {
    var content by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf<String?>(null) }
    var latitude by remember { mutableStateOf<String?>(null) } // String for TextField
    var longitude by remember { mutableStateOf<String?>(null) } // String for TextField
    var privacyLevel by remember { mutableStateOf("public") } // Default privacy
    var postType by remember { mutableStateOf("general") } // 'general' or 'sighting'
    var isFeatured by remember { mutableStateOf(false) }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // State for sighting-specific fields
    var sightingDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var taggedSpeciesCode by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val createPostState by communityViewModel.createPostState.collectAsState()

    // Permission for reading images
    val readImagesPermissionState = rememberPermissionState(
        permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )

    // Launcher for picking multiple images
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImageUris = selectedImageUris + uris // Append new selections
    }

    LaunchedEffect(createPostState) {
        when (val state = createPostState) {
            is GenericUiState.Success -> {
                Toast.makeText(context, "Post created successfully!", Toast.LENGTH_SHORT).show()
                communityViewModel.resetCreatePostState()
                navController.popBackStack()
            }
            is GenericUiState.Error -> {
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                communityViewModel.resetCreatePostState()
            }
            else -> { /* Idle or Loading */ }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = Instant.now().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        sightingDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }


    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar(
                title = "Create Post",
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
                .padding(16.dp)
        ) {
            // Post Type Selector
            // Logic: The TabRow is placed inside a Box with a clip modifier to achieve the rounded corners,
            // as the TabRow composable itself does not have a shape parameter.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(CardBackground.copy(alpha = 0.3f))
                    .height(48.dp)
            ) {
                val selectedIndex = if (postType == "general") 0 else 1
                TabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = Color.Transparent, // Make the TabRow transparent to see the Box background
                    contentColor = TextWhite,
                    indicator = { tabPositions ->
                        // The custom indicator that fills the selected tab's background
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedIndex])
                                .fillMaxHeight()
                                .padding(4.dp)
                                .clip(RoundedCornerShape(50)),
                            color = GreenWave2.copy(alpha = 0.8f)
                        )
                    },
                    divider = {} // Remove the default divider line
                ) {
                    Tab(
                        selected = postType == "general",
                        onClick = { postType = "general" },
                        text = { Text("General Post") },
                        selectedContentColor = TextWhite, // Text color when selected
                        unselectedContentColor = TextWhite.copy(alpha = 0.8f)
                    )
                    Tab(
                        selected = postType == "sighting",
                        onClick = { postType = "sighting" },
                        text = { Text("Sighting") },
                        selectedContentColor = TextWhite,
                        unselectedContentColor = TextWhite.copy(alpha = 0.8f)
                    )
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            // Content TextField
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("What's on your mind?", color = TextWhite.copy(alpha = 0.7f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = CardBackground.copy(alpha = 0.5f),
                    unfocusedContainerColor = CardBackground.copy(alpha = 0.3f),
                    cursorColor = GreenWave2,
                    focusedIndicatorColor = GreenWave2,
                    unfocusedIndicatorColor = DividerColor,
                    focusedLabelColor = GreenWave2,
                    unfocusedLabelColor = TextWhite.copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = postType == "sighting") {
                Column {
                    // Sighting Date
                    OutlinedTextField(
                        value = sightingDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sighting Date", color = TextWhite.copy(alpha = 0.7f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Sighting Date", tint = TextWhite.copy(alpha = 0.7f))},
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                            focusedContainerColor = CardBackground.copy(alpha = 0.5f),
                            unfocusedContainerColor = CardBackground.copy(alpha = 0.3f),
                            disabledTextColor = TextWhite, // Important for readOnly
                            disabledIndicatorColor = DividerColor, // Important for readOnly
                            disabledLabelColor = TextWhite.copy(alpha = 0.7f), // Important for readOnly
                            disabledLeadingIconColor = TextWhite.copy(alpha = 0.7f),
                            cursorColor = GreenWave2, focusedIndicatorColor = GreenWave2,
                            unfocusedIndicatorColor = DividerColor,
                            focusedLabelColor = GreenWave2, unfocusedLabelColor = TextWhite.copy(alpha = 0.7f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Tagged Species
                    OutlinedTextField(
                        value = taggedSpeciesCode ?: "",
                        onValueChange = { taggedSpeciesCode = it.ifBlank { null } },
                        label = { Text("Bird Species Code (e.g., 'houspa')", color = TextWhite.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp), colors = TextFieldDefaults.colors(
                            focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                            focusedContainerColor = CardBackground.copy(alpha = 0.5f),
                            unfocusedContainerColor = CardBackground.copy(alpha = 0.3f),
                            cursorColor = GreenWave2, focusedIndicatorColor = GreenWave2,
                            unfocusedIndicatorColor = DividerColor,
                            focusedLabelColor = GreenWave2, unfocusedLabelColor = TextWhite.copy(alpha = 0.7f))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }


            // Image Picker Button
            Button(
                onClick = {
                    if (readImagesPermissionState.status.isGranted) {
                        imagePickerLauncher.launch("image/*")
                    } else {
                        readImagesPermissionState.launchPermissionRequest()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Images", tint = TextWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Photos/Videos", color = TextWhite)
            }

            if (!readImagesPermissionState.status.isGranted && readImagesPermissionState.status.shouldShowRationale) {
                Text(
                    "Permission to read images is needed to add photos to your post.",
                    color = TextWhite.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }


            // Selected Images Preview
            if (selectedImageUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedImageUris) { uri ->
                        Box(modifier = Modifier.size(100.dp)) {
                            Image(
                                painter = rememberAsyncImagePainter(model = uri),
                                contentDescription = "Selected image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            IconButton(
                                onClick = { selectedImageUris = selectedImageUris - uri },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Remove image", tint = TextWhite, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }


            // Optional Fields
            OutlinedTextField(
                value = locationName ?: "",
                onValueChange = { locationName = it.ifBlank { null } },
                label = { Text("Location Name (Optional)", color = TextWhite.copy(alpha = 0.7f)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = "Location Icon", tint = TextWhite.copy(alpha = 0.7f)) },
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                    focusedContainerColor = CardBackground.copy(alpha = 0.5f),
                    unfocusedContainerColor = CardBackground.copy(alpha = 0.3f),
                    cursorColor = GreenWave2, focusedIndicatorColor = GreenWave2,
                    unfocusedIndicatorColor = DividerColor,
                    focusedLabelColor = GreenWave2, unfocusedLabelColor = TextWhite.copy(alpha = 0.7f))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)){
                OutlinedTextField(
                    value = latitude ?: "",
                    onValueChange = { latitude = it.filter { char -> char.isDigit() || char == '.' || char == '-' }.ifBlank { null } },
                    label = { Text("Latitude (Optional)", color = TextWhite.copy(alpha = 0.7f)) },
                    modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp), colors = TextFieldDefaults.colors(
                        focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                        focusedContainerColor = CardBackground.copy(alpha = 0.5f),
                        unfocusedContainerColor = CardBackground.copy(alpha = 0.3f),
                        cursorColor = GreenWave2, focusedIndicatorColor = GreenWave2,
                        unfocusedIndicatorColor = DividerColor,
                        focusedLabelColor = GreenWave2, unfocusedLabelColor = TextWhite.copy(alpha = 0.7f))
                )
                OutlinedTextField(
                    value = longitude ?: "",
                    onValueChange = { longitude = it.filter { char -> char.isDigit() || char == '.' || char == '-' }.ifBlank { null } },
                    label = { Text("Longitude (Optional)", color = TextWhite.copy(alpha = 0.7f)) },
                    modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp), colors = TextFieldDefaults.colors(
                        focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                        focusedContainerColor = CardBackground.copy(alpha = 0.5f),
                        unfocusedContainerColor = CardBackground.copy(alpha = 0.3f),
                        cursorColor = GreenWave2, focusedIndicatorColor = GreenWave2,
                        unfocusedIndicatorColor = DividerColor,
                        focusedLabelColor = GreenWave2, unfocusedLabelColor = TextWhite.copy(alpha = 0.7f))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Privacy Level (Example: Dropdown or RadioGroup)
            var expandedPrivacy by remember { mutableStateOf(false) }
            val privacyOptions = listOf("public", "friends", "private") // Ensure these match backend expectations
            ExposedDropdownMenuBox(
                expanded = expandedPrivacy,
                onExpandedChange = { expandedPrivacy = !expandedPrivacy }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = privacyLevel,
                    onValueChange = {},
                    label = { Text("Privacy Level", color = TextWhite.copy(alpha = 0.7f)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPrivacy) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                        focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                        focusedContainerColor = CardBackground.copy(alpha = 0.5f),
                        unfocusedContainerColor = CardBackground.copy(alpha = 0.3f),
                        cursorColor = GreenWave2, focusedIndicatorColor = GreenWave2,
                        unfocusedIndicatorColor = DividerColor,
                        focusedLabelColor = GreenWave2, unfocusedLabelColor = TextWhite.copy(alpha = 0.7f),
                        focusedTrailingIconColor = TextWhite, unfocusedTrailingIconColor = TextWhite.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Public, contentDescription = "Privacy Icon", tint = TextWhite.copy(alpha = 0.7f))},
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedPrivacy,
                    onDismissRequest = { expandedPrivacy = false },
                    modifier = Modifier.background(CardBackground)
                ) {
                    privacyOptions.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, color = TextWhite) },
                            onClick = {
                                privacyLevel = selectionOption
                                expandedPrivacy = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Button(
                onClick = {
                    if (content.isBlank()) {
                        Toast.makeText(context, "Post content cannot be empty.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (postType == "sighting" && (sightingDate == null || taggedSpeciesCode.isNullOrBlank())) {
                        Toast.makeText(context, "For a sighting, please provide a date and species code.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val sightingDateString = sightingDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toString()

                    communityViewModel.createPost(
                        content = content,
                        locationName = locationName,
                        latitude = latitude?.toDoubleOrNull(),
                        longitude = longitude?.toDoubleOrNull(),
                        privacyLevel = privacyLevel,
                        type = postType,
                        isFeatured = isFeatured,
                        mediaUris = selectedImageUris,
                        sightingDate = sightingDateString,
                        taggedSpeciesCode = taggedSpeciesCode
                    )
                },
                enabled = createPostState !is GenericUiState.Loading,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
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