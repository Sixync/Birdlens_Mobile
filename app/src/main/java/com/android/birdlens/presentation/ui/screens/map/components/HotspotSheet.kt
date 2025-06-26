// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/map/components/HotspotSheet.kt
package com.android.birdlens.presentation.ui.screens.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.R
import com.android.birdlens.presentation.viewmodel.HotspotSheetDetails
import com.android.birdlens.ui.theme.ButtonGreen
import com.android.birdlens.ui.theme.GreenWave2
import com.android.birdlens.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotDetailsSheetContent(
    details: HotspotSheetDetails,
    onNavigateToFullDetails: () -> Unit,
    onBookmarkToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 200.dp)
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = details.hotspot.locName,
            style = MaterialTheme.typography.headlineSmall,
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Visibility, contentDescription = stringResource(R.string.map_sheet_recent_sightings), tint = TextWhite.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.map_sheet_recent_sightings) + ": ${details.recentSightingsCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite.copy(alpha = 0.8f)
            )
        }
        details.hotspot.numSpeciesAllTime?.let {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top=4.dp)) {
                Icon(Icons.Outlined.ListAlt, contentDescription = stringResource(R.string.map_sheet_total_species), tint = TextWhite.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.map_sheet_total_species) + ": $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite.copy(alpha = 0.8f)
                )
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        Text(
            stringResource(R.string.map_sheet_notable_bird),
            style = MaterialTheme.typography.titleMedium,
            color = TextWhite
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = details.notableBirdImageUrl,
                    placeholder = painterResource(id = R.drawable.ic_bird_placeholder),
                    error = painterResource(id = R.drawable.ic_bird_placeholder)
                ),
                contentDescription = "Notable bird",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.3f)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Example: Northern Cardinal",
                style = MaterialTheme.typography.bodyLarge,
                color = TextWhite
            )
        }

        if (details.speciesList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.map_sheet_species_list_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite
            )
            LazyRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(details.speciesList.take(5)) { species ->
                    AssistChip(
                        onClick = { /* TODO: Navigate to bird info for species.speciesCode */ },
                        label = { Text(species.commonName, color = TextWhite) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = ButtonGreen.copy(alpha = 0.7f)),
                        border = null
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onBookmarkToggle,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenWave2),
                border = BorderStroke(1.dp, GreenWave2)
            ) {
                Icon(
                    if (details.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (details.isBookmarked) stringResource(R.string.map_action_unbookmark_hotspot) else stringResource(R.string.map_action_bookmark_hotspot)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (details.isBookmarked) stringResource(R.string.map_action_unbookmark_hotspot) else stringResource(R.string.map_action_bookmark_hotspot))
            }
            Button(
                onClick = onNavigateToFullDetails,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
            ) {
                // Logic: The main action button in the sheet now navigates to the bird list.
                Text("View Bird List", color = TextWhite)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextWhite)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}