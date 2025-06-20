// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/components/Charts.kt
package com.android.birdlens.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.birdlens.ui.theme.GreenWave2
import com.android.birdlens.ui.theme.TextWhite

@Composable
fun <T> BarChart(
    data: List<T>,
    labelSelector: (T) -> String,
    valueSelector: (T) -> Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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