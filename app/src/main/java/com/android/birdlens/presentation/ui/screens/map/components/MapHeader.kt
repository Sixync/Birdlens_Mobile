// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/map/components/MapHeader.kt
package com.android.birdlens.presentation.ui.screens.map.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.birdlens.R
import com.android.birdlens.data.local.BirdSpecies
import com.android.birdlens.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onClearSearch: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    onShowTutorial: () -> Unit,
    searchResults: List<BirdSpecies>,
    onSearchResultClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(bottom = 8.dp)) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    stringResource(R.string.map_screen_title_birdlens).uppercase(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, color = GreenWave2, letterSpacing = 1.sp)
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = TextWhite)
                }
            },
            actions = {
                IconButton(onClick = onShowTutorial) {
                    Icon(Icons.Outlined.HelpOutline, contentDescription = stringResource(id = R.string.show_tutorial_content_description), tint = TextWhite)
                }
                IconButton(onClick = onNavigateToCart) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = stringResource(R.string.icon_cart_description), tint = TextWhite, modifier = Modifier.size(28.dp))
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text(stringResource(R.string.map_search_placeholder), color = TextWhite.copy(alpha = 0.7f)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.map_search_icon_description), tint = TextWhite) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.map_search_clear_description), tint = TextWhite)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = SearchBarBackground,
                unfocusedContainerColor = SearchBarBackground,
                disabledContainerColor = SearchBarBackground,
                cursorColor = TextWhite,
                focusedBorderColor = GreenWave2,
                unfocusedBorderColor = Color.Transparent,
                focusedLeadingIconColor = TextWhite,
                unfocusedLeadingIconColor = TextWhite.copy(alpha = 0.7f),
                focusedTrailingIconColor = TextWhite,
                unfocusedTrailingIconColor = TextWhite.copy(alpha = 0.7f),
                focusedPlaceholderColor = TextWhite.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = TextWhite.copy(alpha = 0.7f)
            )
        )

        if (searchResults.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(searchResults, key = { it.speciesCode }) { bird ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSearchResultClick(bird.commonName) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = TextWhite.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(text = bird.commonName, color = TextWhite)
                                Text(
                                    text = bird.scientificName,
                                    color = TextWhite.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}