// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/hotspotdetail/HotspotDetailScreen.kt
package com.android.birdlens.presentation.ui.screens.hotspotdetail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.android.birdlens.R
import com.android.birdlens.data.model.VisitingTimesAnalysis
import com.android.birdlens.data.model.ebird.EbirdObservation
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.BarChart
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
// Logic: The import for the obsolete Stripe CheckoutActivity is removed.
import com.android.birdlens.presentation.viewmodel.HotspotDetailData
import com.android.birdlens.presentation.viewmodel.HotspotDetailUiState
import com.android.birdlens.presentation.viewmodel.HotspotDetailViewModel
import com.android.birdlens.presentation.viewmodel.HotspotDetailViewModelFactory
import com.android.birdlens.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HotspotDetailScreen(
    navController: NavController,
    viewModel: HotspotDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    AppScaffold(
        navController = navController,
        topBar = { SimpleTopAppBar(title = "Hotspot Details", onNavigateBack = { navController.popBackStack() }) },
        showBottomBar = true
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(VeryDarkGreenBase)
        ) {
            when (val state = uiState) {
                is HotspotDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                }
                is HotspotDetailUiState.Error -> {
                    Text(
                        state.message,
                        color = TextWhite.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                is HotspotDetailUiState.Success -> {
                    HotspotDetailContent(
                        data = state.data,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun HotspotDetailContent(data: HotspotDetailData, navController: NavController) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            HeaderSection(
                locName = data.basicInfo.locName,
                country = formatCountry(data.basicInfo.countryCode, data.basicInfo.subnational1Code),
                onCompareClick = {
                    navController.navigate(Screen.Map.route) {
                    }
                }
            )
        }

        item {
            InfoCard(
                icon = Icons.Filled.Directions,
                title = "Location Info",
                content = {
                    Text("Total Species (All-Time): ${data.basicInfo.numSpeciesAllTime ?: "N/A"}", color = TextWhite)
                    data.basicInfo.latestObsDt?.let {
                        Text("Latest Sighting: ${formatDateSimple(it)}", color = TextWhite.copy(alpha = 0.8f))
                    }
                }
            )
        }

        item {
            InfoCard(
                icon = painterResource(id = R.drawable.ic_launcher_foreground),
                title = "Recent Sightings (Last 7 Days)",
                content = {
                    if (data.recentSightings.isEmpty()) {
                        Text("No recent sightings reported.", color = TextWhite.copy(alpha = 0.7f))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            data.recentSightings.take(5).forEach { sighting ->
                                SightingRow(sighting = sighting, onClick = {
                                    navController.navigate(Screen.BirdInfo.createRoute(sighting.speciesCode))
                                })
                            }
                            if (data.recentSightings.size > 5) {
                                TextButton(onClick = { navController.navigate(Screen.HotspotBirdList.createRoute(data.basicInfo.locId)) }) {
                                    Text("View all ${data.recentSightings.size} sightings...", color = GreenWave2)
                                }
                            }
                        }
                    }
                }
            )
        }

        item {
            if (data.isSubscribed && data.analysis != null) {
                AnalysisSection(analysis = data.analysis)
            } else {
                PremiumUpsellCard(navController)
            }
        }
    }
}

@Composable
fun HeaderSection(locName: String, country: String, onCompareClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = locName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        Text(
            text = country,
            style = MaterialTheme.typography.titleMedium,
            color = TextWhite.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderActionButton(
                icon = Icons.Filled.CompareArrows,
                text = "Compare",
                onClick = onCompareClick,
                modifier = Modifier.weight(1f)
            )
            HeaderActionButton(
                icon = Icons.Filled.BookmarkBorder,
                text = "Bookmark",
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun InfoCard(icon: Any, title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (icon) {
                    is ImageVector -> Icon(icon, contentDescription = null, tint = GreenWave2)
                    is androidx.compose.ui.graphics.painter.Painter -> Icon(icon, contentDescription = null, tint = GreenWave2)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = TextWhite)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DividerColor)
            content()
        }
    }
}

@Composable
fun SightingRow(sighting: EbirdObservation, onClick: () -> Unit) {
    Text(
        text = "• ${sighting.comName} (${formatDateSimple(sighting.obsDt)})",
        color = TextWhite.copy(alpha = 0.9f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    )
}

@Composable
fun AnalysisSection(analysis: VisitingTimesAnalysis) {
    if (analysis.monthlyActivity.isNotEmpty()) {
        InfoCard(icon = Icons.Default.CalendarToday, title = "Best Months to Visit") {
            BarChart(
                data = analysis.monthlyActivity,
                labelSelector = { it.month.substring(0, 3) },
                valueSelector = { it.relativeFrequency }
            )
        }
    }
    if (analysis.hourlyActivity.isNotEmpty()) {
        InfoCard(icon = Icons.Default.AccessTime, title = "Best Times of Day") {
            BarChart(
                data = analysis.hourlyActivity,
                labelSelector = { "${it.hour}:00" },
                valueSelector = { it.relativeFrequency }
            )
        }
    }
}

@Composable
fun PremiumUpsellCard(navController: NavController) {
    val context = LocalContext.current
    InfoCard(icon = Icons.Filled.WorkspacePremium, title = "Unlock Pro Analysis") {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Get detailed charts on the best times to visit this hotspot with an ExBird subscription.",
                color = TextWhite.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    // Logic: The onClick action now navigates to the PremiumScreen,
                    // which contains the PayOS payment flow, instead of the old Stripe activity.
                    navController.navigate(Screen.Premium.route)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
            ) {
                Text("Go Premium", color = TextWhite, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
fun HeaderActionButton(icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CardBackground.copy(alpha = 0.6f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = text, tint = TextWhite)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, color = TextWhite)
        }
    }
}

private fun formatCountry(countryCode: String, subnationalCode: String?): String {
    val countryName = Locale("", countryCode).displayCountry
    return if (subnationalCode != null) {
        val state = subnationalCode.split("-").lastOrNull() ?: subnationalCode
        "$state, $countryName"
    } else {
        countryName
    }
}

private fun formatDateSimple(dateTimeString: String): String {
    return try {
        val datePart = dateTimeString.split(" ").first()
        val date = LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE)
        date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    } catch (e: Exception) {
        dateTimeString
    }
}