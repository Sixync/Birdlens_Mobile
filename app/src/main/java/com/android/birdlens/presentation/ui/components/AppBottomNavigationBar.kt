// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/components/AppBottomNavigationBar.kt
package com.android.birdlens.presentation.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.birdlens.ui.theme.BottomNavGreen
import com.android.birdlens.ui.theme.TextWhite

@Composable
fun AppBottomNavigationBar(
    items: List<BottomNavItemData>,
    selectedItemRoute: String?,
    onItemSelected: (BottomNavItemData) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.height(70.dp), // Standard height
        containerColor = BottomNavGreen,
        contentColor = TextWhite
    ) {
        items.forEach { item ->
            val isSelected = selectedItemRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(item) },
                icon = {
                    if (isSelected) item.selectedIcon() else item.icon()
                },
                label = { Text(item.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TextWhite,
                    unselectedIconColor = TextWhite.copy(alpha = 0.7f),
                    selectedTextColor = TextWhite,
                    unselectedTextColor = TextWhite.copy(alpha = 0.7f),
                    indicatorColor = BottomNavGreen // Or a slightly different shade for selection
                )
            )
        }
    }
}