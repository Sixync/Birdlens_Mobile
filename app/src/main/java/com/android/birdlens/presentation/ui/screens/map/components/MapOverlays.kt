// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/map/components/MapOverlays.kt
package com.android.birdlens.presentation.ui.screens.map.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.birdlens.R
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.presentation.viewmodel.MapUiState
import com.android.birdlens.presentation.viewmodel.MapViewModel
import com.android.birdlens.ui.theme.*

data class FloatingMapActionItem(
    val icon: @Composable () -> Unit,
    val contentDescriptionResId: Int,
    val onClick: () -> Unit,
    val badgeCount: Int? = null,
    val isSelected: Boolean = false,
    val key: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingMapButton(item: FloatingMapActionItem, modifier: Modifier = Modifier) {
    BadgedBox(
        badge = {
            if (item.badgeCount != null) {
                Badge(
                    containerColor = ActionButtonLightGray,
                    contentColor = ActionButtonTextDark,
                    modifier = Modifier.offset(x = (-6).dp, y = 6.dp)
                ) {
                    Text(item.badgeCount.toString())
                }
            }
        },
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (item.isSelected) GreenWave2.copy(alpha = 0.9f) else ButtonGreen.copy(alpha = 0.9f))
            .clickable(onClick = item.onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            item.icon()
        }
    }
}

@Composable
fun PermissionRationaleUI(onGrantPermissions: () -> Unit, showRationale: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.map_permission_required_message),
            color = TextWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = onGrantPermissions,
            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
        ) {
            Text(stringResource(R.string.map_grant_permissions_button), color = TextWhite)
        }
        if (showRationale) {
            Text(
                stringResource(R.string.map_permission_denied_rationale),
                color = TextWhite.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareModeBottomBar(
    selectedHotspots: List<EbirdNearbyHotspot>,
    onClearSelection: () -> Unit,
    onCompareClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground.copy(alpha = 0.95f),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.map_compare_selected_info, selectedHotspots.size, MapViewModel.MAX_COMPARISON_ITEMS),
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onClearSelection) {
                    Text(stringResource(R.string.map_compare_clear_selection), color = GreenWave2)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectedHotspots, key = { it.locId }) { hotspot ->
                    SuggestionChip(
                        onClick = { /* Optional: allow deselecting by tapping chip */ },
                        label = { Text(hotspot.locName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = ButtonGreen.copy(alpha = 0.7f),
                            labelColor = TextWhite
                        ),
                        border = null
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCompareClick,
                enabled = selectedHotspots.size >= 2,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
            ) {
                Text(stringResource(R.string.map_compare_button_text, selectedHotspots.size), color = TextWhite)
            }
        }
    }
}

@Composable
fun LoadingErrorUI(mapUiState: MapUiState) {
    val context = LocalContext.current
    if (mapUiState is MapUiState.Loading) {
        val loadingMessage = mapUiState.message
        if (!loadingMessage.isNullOrBlank()){
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = GreenWave2.copy(alpha = 0.7f), strokeWidth = 5.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = loadingMessage,
                    color = TextWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TextWhite)
            }
        }
    }

    (mapUiState as? MapUiState.Error)?.let { errorState ->
        LaunchedEffect(errorState.message) {
            Toast.makeText(context, errorState.message, Toast.LENGTH_LONG).show()
        }
    }
}