// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/map/components/MapDialogs.kt
package com.android.birdlens.presentation.ui.screens.map.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.android.birdlens.R
import com.android.birdlens.data.CountrySetting
import com.android.birdlens.data.UserSettingsManager
import com.android.birdlens.ui.theme.AuthCardBackground
import com.android.birdlens.ui.theme.GreenWave2
import com.android.birdlens.ui.theme.TextWhite

@Composable
fun HomeCountrySelectionDialog(
    currentHomeCountryCode: String,
    onDismiss: () -> Unit,
    onCountrySelected: (CountrySetting) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = AuthCardBackground) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.dialog_select_home_country),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(modifier = Modifier.heightIn(max=300.dp)){
                    items(UserSettingsManager.PREDEFINED_COUNTRIES) { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCountrySelected(country); onDismiss() }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (country.code == currentHomeCountryCode),
                                onClick = { onCountrySelected(country); onDismiss() },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = GreenWave2,
                                    unselectedColor = TextWhite.copy(alpha = 0.7f)
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(country.name, color = TextWhite, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(android.R.string.cancel).uppercase(), color = GreenWave2)
                }
            }
        }
    }
}