// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/comparison/HotspotComparisonScreen.kt
package com.android.birdlens.presentation.ui.screens.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.android.birdlens.data.model.MonthlyStat
import com.android.birdlens.data.model.VisitingTimesAnalysis
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.ui.screens.login.CustomTextField
import com.android.birdlens.presentation.viewmodel.*
import com.android.birdlens.ui.theme.*

@Composable
fun HotspotComparisonScreen(
    navController: NavController,
    viewModel: HotspotComparisonViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val targetSpeciesName by viewModel.targetSpeciesName.collectAsState()
    var showTargetSpeciesDialog by remember { mutableStateOf(false) }

    // Logic: Get the AccountInfoViewModel to check the user's subscription status.
    val accountInfoViewModel: AccountInfoViewModel = viewModel()
    val accountState by accountInfoViewModel.uiState.collectAsState()
    val isExBirdUser = (accountState as? AccountInfoUiState.Success)?.user?.subscription == "ExBird"

    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar(
                title = "Hotspot Comparison",
                onNavigateBack = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTargetSpeciesDialog = true },
                containerColor = ButtonGreen
            ) {
                Icon(Icons.Filled.Search, "Set Target Species", tint = TextWhite)
            }
        },
        showBottomBar = false
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(VeryDarkGreenBase)
        ) {
            when (val state = uiState) {
                is HotspotComparisonUiState.Loading -> {
                    CircularProgressIndicator(color = TextWhite, modifier = Modifier.align(Alignment.Center))
                }
                is HotspotComparisonUiState.Error -> {
                    Text(
                        text = state.message,
                        color = TextWhite.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                is HotspotComparisonUiState.Success -> {
                    if (state.comparisonData.isEmpty()) {
                        Text(
                            "No data to compare. Please select hotspots.",
                            color = TextWhite.copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.comparisonData, key = { it.locId }) { metrics ->
                                // Pass the subscription status to the card.
                                HotspotComparisonCard(metrics = metrics, isExBirdUser = isExBirdUser)
                            }
                        }
                    }
                }
                HotspotComparisonUiState.Idle -> {
                    Text("Select hotspots to compare.", color = TextWhite.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }

    if (showTargetSpeciesDialog) {
        TargetSpeciesDialog(
            currentValue = targetSpeciesName ?: "",
            onDismiss = { showTargetSpeciesDialog = false },
            onConfirm = { speciesName ->
                viewModel.setTargetSpecies(speciesName.ifBlank { null })
                showTargetSpeciesDialog = false
            }
        )
    }
}

@Composable
fun HotspotComparisonCard(metrics: HotspotComparisonMetrics, isExBirdUser: Boolean) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = metrics.locName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextWhite),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "ID: ${metrics.locId}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextWhite.copy(alpha = 0.6f))
            )
            Spacer(modifier = Modifier.height(12.dp))

            ComparisonMetricItem("All-Time Species", metrics.allTimeSpeciesCount?.toString() ?: "N/A")
            ComparisonMetricItem("Latest Observation", metrics.latestObservationDate ?: "N/A")

            ComparisonMetricItem("Recent Sightings")
            if (metrics.recentObservationsSummary.isNotEmpty()) {
                metrics.recentObservationsSummary.forEach {
                    Text("  • $it", style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite.copy(alpha = 0.9f)))
                }
            } else {
                Text("  • None recently", style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite.copy(alpha = 0.7f)))
            }
            Spacer(modifier = Modifier.height(8.dp))


            metrics.targetSpeciesInfo?.let { targetInfo ->
                ComparisonMetricItem("Target: ${targetInfo.speciesName}")
                Text(
                    text = when {
                        targetInfo.wasSeenRecently -> "  ✓ Seen recently (${targetInfo.lastSeenDate})"
                        targetInfo.isPotential -> "  ○ Potential (recorded previously)"
                        else -> "  ✗ Not recorded"
                    },
                    color = if (targetInfo.wasSeenRecently) GreenWave2 else if (targetInfo.isPotential) TextWhite.copy(alpha = 0.8f) else Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Logic: Conditionally display the analysis or the upsell message.
            if (isExBirdUser) {
                metrics.visitingTimes?.let {
                    VisitingTimesSection(analysis = it)
                } ?: ComparisonMetricItem("Best Times to Visit", "Analysis data not available.")
            } else {
                PremiumUpsell()
            }
        }
    }
}

@Composable
fun VisitingTimesSection(analysis: VisitingTimesAnalysis) {
    Column {
        ComparisonMetricItem("Best Months to Visit")
        if(analysis.monthlyActivity.isNotEmpty()){
            BarChart(
                data = analysis.monthlyActivity,
                labelSelector = { it.month.substring(0, 3) },
                valueSelector = { it.relativeFrequency }
            )
        } else {
            Text("  Not enough data.", style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite.copy(alpha = 0.7f)))
        }

        Spacer(modifier = Modifier.height(8.dp))
        ComparisonMetricItem("Best Times of Day")
        if(analysis.hourlyActivity.isNotEmpty()){
            BarChart(
                data = analysis.hourlyActivity,
                labelSelector = { "${it.hour}:00" },
                valueSelector = { it.relativeFrequency }
            )
        } else {
            Text("  Not enough data.", style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite.copy(alpha = 0.7f)))
        }
    }
}

@Composable
fun <T> BarChart(
    data: List<T>,
    labelSelector: (T) -> String,
    valueSelector: (T) -> Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .height((valueSelector(item) * 80).dp.coerceAtLeast(2.dp))
                        .width(20.dp)
                        .background(GreenWave2, shape = RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = labelSelector(item),
                    fontSize = 10.sp,
                    color = TextWhite.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}

@Composable
fun PremiumUpsell() {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = "Premium Feature", tint = GreenWave2)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Best Times to Visit",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = TextWhite.copy(alpha = 0.9f))
            )
        }
        Text(
            "Unlock detailed activity analysis with an ExBird subscription.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite.copy(alpha = 0.8f)),
            modifier = Modifier.padding(start = 32.dp, top = 4.dp)
        )
        HorizontalDivider(color = DividerColor.copy(alpha = 0.3f), modifier = Modifier.padding(top = 6.dp))
    }
}


@Composable
fun ComparisonMetricItem(label: String, value: String? = null) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = TextWhite.copy(alpha = 0.9f))
        )
        value?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite.copy(alpha = 0.8f)),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        HorizontalDivider(color = DividerColor.copy(alpha = 0.3f), modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun TargetSpeciesDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentValue) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AuthCardBackground
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Set Target Species", style = MaterialTheme.typography.titleLarge, color = TextWhite)
                Spacer(modifier = Modifier.height(16.dp))
                CustomTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "Enter bird name (e.g., Blue Jay)",
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CardBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextWhite.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(text) },
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                    ) {
                        Text("Confirm", color = TextWhite)
                    }
                }
            }
        }
    }
}