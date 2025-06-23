// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/birdinfo/BirdInfoScreen.kt
package com.android.birdlens.presentation.ui.screens.birdinfo

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.android.birdlens.R
import com.android.birdlens.data.model.ebird.EbirdTaxonomy
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.viewmodel.BirdInfoUiState
import com.android.birdlens.presentation.viewmodel.BirdInfoViewModel
import com.android.birdlens.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun BirdInfoScreen(
    navController: NavController,
    viewModel: BirdInfoViewModel // ViewModel provided by NavHost
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    AppScaffold(
        navController = navController,
        topBar = { /* Custom header is now part of the content */ },
        // Set showBottomBar based on your app's navigation pattern for this screen.
        // The example "Magpie in4" has a bottom bar.
        showBottomBar = true
    ) { innerPadding ->
        when (val state = uiState) {
            is BirdInfoUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding), // Apply padding from Scaffold
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TextWhite)
                }
            }
            is BirdInfoUiState.Success -> {
                BirdInfoContentNew(
                    modifier = Modifier.padding(innerPadding), // Apply padding from Scaffold
                    navController = navController,
                    birdData = state.birdData,
                    imageUrl = state.imageUrl,
                    onImageClick = {
                        // TODO: Implement full-screen image view navigation or dialog
                        Toast.makeText(context, "View full image (not implemented)", Toast.LENGTH_SHORT).show()
                    },
                    onBookmarkClick = {
                        // TODO: Implement bookmark functionality (call ViewModel)
                        Toast.makeText(context, "Bookmark action (not implemented)", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            is BirdInfoUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding) // Apply padding
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(id = R.string.error_loading_bird_info, state.message),
                        color = TextWhite.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.fetchBirdInformation() }, // Retry
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                    ) {
                        Text(stringResource(id = R.string.retry), color = TextWhite)
                    }
                }
            }
            is BirdInfoUiState.Idle -> {
                // Show a loading or placeholder if in Idle state, as ViewModel init triggers fetch
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(id = R.string.initializing), color = TextWhite.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun BirdInfoContentNew(
    modifier: Modifier = Modifier,
    navController: NavController,
    birdData: EbirdTaxonomy,
    imageUrl: String?,
    onImageClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VeryDarkGreenBase) // Ensuring background consistency
    ) {
        item {
            BirdImageHeader(
                navController = navController,
                commonName = birdData.commonName,
                scientificName = birdData.scientificName,
                imageUrl = imageUrl,
                onImageClick = onImageClick,
                onBookmarkClick = onBookmarkClick
            )
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                Text(
                    text = stringResource(R.string.bird_info_characteristics_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                BirdInfoDetailCard {
                    InfoRow(stringResource(R.string.bird_info_common_name), birdData.commonName)
                    InfoRow(stringResource(R.string.bird_info_scientific_name), birdData.scientificName)
                    InfoRow(stringResource(R.string.bird_info_species_code), birdData.speciesCode)
                    InfoRow(stringResource(R.string.bird_info_category), birdData.category)
                    birdData.birdOrder?.let { InfoRow(stringResource(R.string.bird_info_order), it) }
                    birdData.familyCommonName?.let { InfoRow(stringResource(R.string.bird_info_family_common), it) }
                    birdData.familyScientificName?.let { InfoRow(stringResource(R.string.bird_info_family_scientific), it) }
                    birdData.taxonOrder?.let { InfoRow(stringResource(R.string.bird_info_taxon_order), "%.0f".format(it)) }
                }

                Spacer(modifier = Modifier.height(24.dp))


                // Logic: The onClick now navigates to the BirdRangeMap screen and passes
                // the bird's scientific name as a route argument.
                Button(
                    onClick = {
                        navController.navigate(Screen.BirdRangeMap.createRoute(birdData.scientificName))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                ) {
                    Text("View Distribution Map", color = TextWhite)
                }
            }
        }
    }
}

@Composable
fun BirdImageHeader(
    navController: NavController,
    commonName: String,
    scientificName: String,
    imageUrl: String?,
    onImageClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    val context = LocalContext.current
    // Placeholder in case image URL is null
    val imageToLoad = imageUrl ?: "https://via.placeholder.com/600x400/CCCCCC/FFFFFF?Text=${commonName.replace(" ", "+")}"

    val imagePainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(imageToLoad)
            .crossfade(true)
            // .placeholder(R.drawable.your_placeholder_drawable) // Optional placeholder
            // .error(R.drawable.your_error_drawable) // Optional error image
            .build()
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp) // Increased height for a more prominent header image
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)) // Round bottom corners
            .clickable(onClick = onImageClick)
    ) {
        Image(
            painter = imagePainter,
            contentDescription = stringResource(R.string.bird_image_description, commonName),
            contentScale = ContentScale.Crop, // Crop to fill bounds
            modifier = Modifier.fillMaxSize()
        )

        // Scrim for text readability overlayed on the image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ImageScrimColor.copy(alpha = 0.2f), // Lighter at the top
                            Color.Transparent,
                            ImageScrimColor.copy(alpha = 0.8f)  // Darker at the bottom for text
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY // Ensures gradient covers the box
                    )
                )
        )

        // Top Action Icons (Back and Bookmark)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding() // Important for edge-to-edge
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = TextWhite
                )
            }
            IconButton(
                onClick = onBookmarkClick,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Default.BookmarkBorder, // Use Outlined or Filled based on state
                    contentDescription = stringResource(R.string.bookmark),
                    tint = TextWhite
                )
            }
        }

        // Bird Name and Subtitle Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp) // Adjusted padding
        ) {
            Text(
                text = commonName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    shadow = androidx.compose.ui.graphics.Shadow( // Subtle shadow for better contrast
                        color = Color.Black.copy(alpha = 0.7f),
                        offset = androidx.compose.ui.geometry.Offset(1f, 2f),
                        blurRadius = 3f
                    )
                ),
                maxLines = 2, // Allow for longer names
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = scientificName, // Using scientific name as subtitle
                style = MaterialTheme.typography.titleMedium.copy( // Slightly larger subtitle
                    color = TextWhite.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Normal,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                        blurRadius = 2f
                    )
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BirdInfoDetailCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBackground.copy(alpha = 0.4f), // Slightly more opaque card
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp // Increased elevation for more depth
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp) // Increased padding for better spacing
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextWhite.copy(alpha = 0.75f), // Slightly more opaque label
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 17.sp, // Slightly larger value text
            color = TextWhite,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = DividerColor.copy(alpha = 0.4f), thickness = 0.7.dp)
    }
}


@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = false)
@Composable
fun BirdInfoScreenNewPreview_Success() {
    val dummyBirdData = EbirdTaxonomy(
        scientificName = "Geopelia striata",
        commonName = "Zebra Dove",
        speciesCode = "zebdov",
        category = "species",
        taxonOrder = 12345.0,
        birdOrder = "Columbiformes",
        familyCommonName = "Pigeons and Doves",
        familyScientificName = "Columbidae"
    )
    val dummyImageUrl = "https://images.unsplash.com/photo-1599316898584-53c9876078f6?w=800&auto=format&fit=crop&q=60" // Example image

    // Simplified ViewModel mock for preview
    class MockBirdInfoViewModel : BirdInfoViewModel(SavedStateHandle(mapOf("speciesCode" to "zebdov"))) {
        init {
            (super._uiState as MutableStateFlow<BirdInfoUiState>).value = BirdInfoUiState.Success(dummyBirdData, dummyImageUrl)
        }
    }

    BirdlensTheme {
        Surface(color = VeryDarkGreenBase) { // Ensure preview background matches app
            BirdInfoScreen(
                navController = rememberNavController(),
                viewModel = MockBirdInfoViewModel()
            )
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = false)
@Composable
fun BirdInfoScreenNewPreview_Success_NoImage() {
    val dummyBirdData = EbirdTaxonomy(
        scientificName = "Corvus splendens",
        commonName = "House Crow with a Very Long Common Name to Test Multi-line Ellipsis Behavior",
        speciesCode = "houcro",
        category = "species",
        taxonOrder = 30000.0,
        birdOrder = "Passeriformes",
        familyCommonName = "Crows, Jays, and Magpies",
        familyScientificName = "Corvidae"
    )
    class MockBirdInfoViewModel : BirdInfoViewModel(SavedStateHandle(mapOf("speciesCode" to "houcro"))) {
        init {
            (super._uiState as MutableStateFlow<BirdInfoUiState>).value = BirdInfoUiState.Success(dummyBirdData, null)
        }
    }
    BirdlensTheme {
        Surface(color = VeryDarkGreenBase) {
            BirdInfoScreen(
                navController = rememberNavController(),
                viewModel = MockBirdInfoViewModel()
            )
        }
    }
}