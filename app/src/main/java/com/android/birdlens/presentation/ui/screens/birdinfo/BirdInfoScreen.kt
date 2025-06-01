package com.android.birdlens.presentation.ui.screens.birdinfo


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.android.birdlens.data.model.ebird.EbirdTaxonomy
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.viewmodel.BirdInfoUiState
import com.android.birdlens.presentation.viewmodel.BirdInfoViewModel
import com.android.birdlens.ui.theme.*

@Composable
fun BirdInfoScreen(
    navController: NavController,
    viewModel: BirdInfoViewModel // ViewModel will be provided by NavHost
) {
    val uiState by viewModel.uiState.collectAsState()

    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar(
                title = "Bird Information",
                onNavigateBack = { navController.popBackStack() }
            )
        },
        showBottomBar = false // Typically no main bottom bar on detail screens
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is BirdInfoUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                }
                is BirdInfoUiState.Success -> {
                    BirdDetailsContent(birdData = state.birdData, imageUrl = state.imageUrl)
                }
                is BirdInfoUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: ${state.message}", color = TextWhite.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.fetchBirdInformation() }, // Retry
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                        ) {
                            Text("Retry", color = TextWhite)
                        }
                    }
                }
                is BirdInfoUiState.Idle -> {
                    // Optionally show something or let it transition to Loading
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Initializing...", color = TextWhite.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun BirdDetailsContent(birdData: EbirdTaxonomy, imageUrl: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imageToLoad = imageUrl ?: "https://via.placeholder.com/300x200/CCCCCC/FFFFFF?Text=${birdData.commonName.replace(" ", "+")}"
        val imagePainter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageToLoad)
                .crossfade(true)
                // Add placeholder/error for Coil if desired
                // .placeholder(R.drawable.placeholder_image)
                // .error(R.drawable.error_image)
                .build()
        )

        Image(
            painter = imagePainter,
            contentDescription = birdData.commonName,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // You could also use .aspectRatio() if you prefer a consistent shape for the image frame
                .clip(RoundedCornerShape(16.dp))
                .background(Color.DarkGray.copy(alpha = 0.3f)), // Background for the image area, visible with ContentScale.Fit
            contentScale = ContentScale.Fit // Changed from ContentScale.Crop
        )


        Spacer(modifier = Modifier.height(24.dp))

        BirdInfoDetailCard {
            InfoRow("Common Name", birdData.commonName)
            InfoRow("Scientific Name", birdData.scientificName)
            InfoRow("Species Code", birdData.speciesCode)
            InfoRow("Category", birdData.category)
            birdData.birdOrder?.let { InfoRow("Order", it) }
            birdData.familyCommonName?.let { InfoRow("Family (Common)", it) }
            birdData.familyScientificName?.let { InfoRow("Family (Scientific)", it) }
            birdData.taxonOrder?.let { InfoRow("Taxon Order", it.toString())}
        }
        Spacer(modifier = Modifier.height(16.dp))
        // You can add more sections here, e.g., for recent observations if you fetch that data.
    }
}

@Composable
fun BirdInfoDetailCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBackground.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextWhite.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 18.sp,
            color = TextWhite,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = DividerColor.copy(alpha = 0.5f), thickness = 0.5.dp)
    }
}


@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun BirdInfoScreenPreview_Success() {
    val dummyBirdData = EbirdTaxonomy(
        scientificName = "Passer domesticus",
        commonName = "House Sparrow",
        speciesCode = "houspa",
        category = "species",
        taxonOrder = 29401.0,
        birdOrder = "Passeriformes",
        familyCommonName = "Old World Sparrows",
        familyScientificName = "Passeridae"
    )
    val dummyImageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/15/Passer_domesticus_female_Hiddensee_P1080333_adjusted.jpg/500px-Passer_domesticus_female_Hiddensee_P1080333_adjusted.jpg"

    BirdlensTheme {
        AppScaffold(navController = rememberNavController(), topBar = { SimpleTopAppBar("Bird Information") }) {
            Box(modifier = Modifier.padding(it).fillMaxSize()) {
                BirdDetailsContent(birdData = dummyBirdData, imageUrl = dummyImageUrl)
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun BirdInfoScreenPreview_Success_NoImage() {
    val dummyBirdData = EbirdTaxonomy(
        scientificName = "Passer domesticus",
        commonName = "House Sparrow",
        speciesCode = "houspa",
        category = "species",
        taxonOrder = 29401.0,
        birdOrder = "Passeriformes",
        familyCommonName = "Old World Sparrows",
        familyScientificName = "Passeridae"
    )

    BirdlensTheme {
        AppScaffold(navController = rememberNavController(), topBar = { SimpleTopAppBar("Bird Information") }) {
            Box(modifier = Modifier.padding(it).fillMaxSize()) {
                BirdDetailsContent(birdData = dummyBirdData, imageUrl = null)
            }
        }
    }
}


@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun BirdInfoScreenPreview_Error() {
    BirdlensTheme {
        AppScaffold(navController = rememberNavController(), topBar = { SimpleTopAppBar("Bird Information") }) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: Failed to load bird data. Please try again.", color = TextWhite.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { /* Retry */ },
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                    ) {
                        Text("Retry", color = TextWhite)
                    }
                }
            }
        }
    }
}