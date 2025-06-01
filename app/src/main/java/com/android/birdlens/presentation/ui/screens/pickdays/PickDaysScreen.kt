// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/pickdays/PickDaysScreen.kt
package com.android.birdlens.presentation.ui.screens.pickdays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.R
import com.android.birdlens.presentation.ui.components.AuthScreenLayout
import com.android.birdlens.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PickDaysScreen(
    navController: NavController,
    tourId: Long,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    AuthScreenLayout {
        PickDaysHeader(
            onNavigateBack = { navController.popBackStack() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            color = AuthCardBackground,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = GreenWave2,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DaysOfWeekHeader()
                CalendarGrid(
                    yearMonth = currentMonth,
                    selectedDate = selectedDate,
                    today = today,
                    onDateSelected = { date ->
                        selectedDate = if (selectedDate == date) null else date
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (selectedDate != null) {
                            showSuccessDialog = true
                        }
                    },
                    enabled = selectedDate != null,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.confirm), color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (showSuccessDialog) {
            PurchaseSuccessDialog(
                onDismissRequest = {
                    showSuccessDialog = false
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun PickDaysHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = TextWhite)
        }
        Text(
            text = stringResource(R.string.pick_days_title),
            style = MaterialTheme.typography.titleLarge.copy(
                color = TextWhite,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}


@Composable
fun DaysOfWeekHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        val days = listOf(
            stringResource(id = R.string.days_of_week_mon),
            stringResource(id = R.string.days_of_week_tue),
            stringResource(id = R.string.days_of_week_wed),
            stringResource(id = R.string.days_of_week_thu),
            stringResource(id = R.string.days_of_week_fri),
            stringResource(id = R.string.days_of_week_sat),
            stringResource(id = R.string.days_of_week_sun)
        )
        days.forEach { day ->
            Text(
                text = day,
                color = TextWhite.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeekValue = firstDayOfMonth.dayOfWeek.value
    val leadingEmptyCells = (firstDayOfWeekValue - DayOfWeek.MONDAY.value + 7) % 7

    val calendarDays = mutableListOf<LocalDate?>()
    repeat(leadingEmptyCells) { calendarDays.add(null) }
    (1..daysInMonth).forEach { day ->
        calendarDays.add(yearMonth.atDay(day))
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(calendarDays) { date ->
            if (date != null) {
                val isSelected = date == selectedDate
                val isPast = date.isBefore(today)
                val isToday = date.isEqual(today)

                DateCell(
                    date = date,
                    isSelected = isSelected,
                    isPast = isPast,
                    isToday = isToday,
                    onDateSelected = { if (!isPast) onDateSelected(date) }
                )
            } else {
                Spacer(Modifier.aspectRatio(1f))
            }
        }
    }
}

@Composable
fun DateCell(
    date: LocalDate,
    isSelected: Boolean,
    isPast: Boolean,
    isToday: Boolean,
    onDateSelected: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> GreenWave2
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> VeryDarkGreenBase
        isPast -> TextWhite.copy(alpha = 0.4f)
        else -> TextWhite
    }
    val fontWeight = if (isToday && !isSelected) FontWeight.Bold else FontWeight.Normal

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(enabled = !isPast, onClick = onDateSelected)
            .padding(4.dp)
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = textColor,
            fontSize = 14.sp,
            fontWeight = fontWeight
        )
    }
}

@Composable
fun PurchaseSuccessDialog(onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AuthCardBackground,
            modifier = Modifier
                .fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.purchase_success_icon_description),
                    tint = GreenWave2,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.purchase_success_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = TextWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.back), color = TextWhite)
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=780dp,dpi=480")
@Composable
fun PickDaysScreenPreview() {
    BirdlensTheme {
        PickDaysScreen(navController = rememberNavController(), tourId = 1L)
    }
}

@Preview
@Composable
fun PurchaseSuccessDialogPreview() {
    BirdlensTheme {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(VeryDarkGreenBase)) {
            PurchaseSuccessDialog(onDismissRequest = {})
        }
    }
}