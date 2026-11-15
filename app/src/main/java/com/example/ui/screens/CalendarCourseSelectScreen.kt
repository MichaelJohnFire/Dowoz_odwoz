package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CourseType
import com.example.model.RouteConfig
import com.example.model.Student
import com.example.ui.components.TopBarWithGuardian
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CourseDisplayItem(
    val courseType: CourseType,
    val subtitle: String,
    val isMorning: Boolean,
    val isGimbus: Boolean,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarCourseSelectScreen(
    selectedDate: String,
    guardianName: String?,
    routeConfigs: List<RouteConfig>,
    allStudents: List<Student>,
    onDateShift: (Int) -> Unit,
    onDateSelect: (String) -> Unit,
    onCourseSelect: (CourseType) -> Unit = {},
    onRouteSelect: (RouteConfig) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var showDatePickerDialog by remember { mutableStateOf(false) }

    val polishDateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("pl", "PL"))
    val formattedDate = try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)
        if (parsed != null) polishDateFormat.format(parsed).replaceFirstChar { it.uppercase() }
        else selectedDate
    } catch (_: Exception) {
        selectedDate
    }

    val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val isToday = selectedDate == todayString

    Scaffold(
        topBar = {
            TopBarWithGuardian(
                title = "Wybór Kursu",
                guardianName = guardianName,
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. DATE SELECTOR HEADER
            DateSelectorHeader(
                selectedDate = selectedDate,
                formattedDate = formattedDate,
                isToday = isToday,
                onTodayClick = { onDateSelect(todayString) },
                onOpenDatePicker = { showDatePickerDialog = true }
            )

            // Section explanation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Wybierz kurs do sprawdzenia obecności:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "${routeConfigs.size} tras",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 2. COURSE SELECTION OPTIONS (Dynamic list from routeConfigs)
            if (routeConfigs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Brak skonfigurowanych tras",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Przejdź do modułu 'Trasy', aby utworzyć nową trasę autobusową.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(routeConfigs, key = { it.id }) { route ->
                        val isGimbus = route.name.contains("GIMBUS", ignoreCase = true)
                        val isMorning = route.name.contains("DOWÓZ", ignoreCase = true) ||
                                route.name.contains("DOWOZ", ignoreCase = true) ||
                                route.scheduledTime < "12:00"

                        val subtitle = when {
                            route.isCustom -> "Trasa dodana przez użytkownika"
                            route.courseType == CourseType.DOWOZ_PKS -> "Poranny dowóz uczniów autobusem PKS"
                            route.courseType == CourseType.DOWOZ_GIMBUS -> "Poranny dowóz żółtym gimbusem szkolnym"
                            route.courseType == CourseType.ODWOZ_PKS_1 -> "Pierwszy popołudniowy odwóz uczniów PKS"
                            route.courseType == CourseType.ODWOZ_GIMBUS -> "Popołudniowy odwóz gimbusem szkolnym"
                            route.courseType == CourseType.ODWOZ_PKS_2 -> "Drugi popołudniowy odwóz uczniów PKS"
                            isMorning -> "Poranny kurs dowozu"
                            else -> "Popołudniowy kurs odwozu"
                        }

                        val cardTestTag = when (route.id) {
                            CourseType.DOWOZ_PKS.name -> "course_card_dowoz_pks"
                            CourseType.DOWOZ_GIMBUS.name -> "course_card_dowoz_gimbus"
                            CourseType.ODWOZ_PKS_1.name -> "course_card_odwoz_pks_1"
                            CourseType.ODWOZ_GIMBUS.name -> "course_card_odwoz_gimbus"
                            CourseType.ODWOZ_PKS_2.name -> "course_card_odwoz_pks_2"
                            else -> "course_card_${route.id}"
                        }

                        CourseSelectionCard(
                            displayName = route.name,
                            subtitle = subtitle,
                            scheduledTime = route.scheduledTime,
                            isMorning = isMorning,
                            isGimbus = isGimbus,
                            testTag = cardTestTag,
                            onClick = {
                                onRouteSelect(route)
                                onCourseSelect(route.courseType)
                            }
                        )
                    }
                }
            }
        }
    }

    // Material 3 DatePickerDialog
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)?.time
                    ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val pickedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
                            onDateSelect(pickedDate)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Wybierz")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Anuluj")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun DateSelectorHeader(
    selectedDate: String,
    formattedDate: String,
    isToday: Boolean,
    onTodayClick: () -> Unit,
    onOpenDatePicker: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("date_selector_header"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenDatePicker)
                    .padding(vertical = 4.dp, horizontal = 4.dp)
                    .testTag("calendar_date_clickable_area")
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenDatePicker)
                        .testTag("calendar_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Zmień datę w kalendarzu",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = selectedDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isToday) {
                OutlinedButton(
                    onClick = onTodayClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("jump_today_button")
                ) {
                    Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dzisiaj")
                }
            }
        }
    }
}

@Composable
fun CourseSelectionCard(
    displayName: String,
    subtitle: String,
    scheduledTime: String,
    isMorning: Boolean,
    isGimbus: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val iconBgColor = when {
        isGimbus -> Color(0xFFFEF3C7) // Amber/Yellow bus container
        isMorning -> Color(0xFFDBEAFE) // Blue container
        else -> Color(0xFFE0E7FF) // Indigo container
    }

    val iconColor = when {
        isGimbus -> Color(0xFFB45309) // Amber/Yellow
        isMorning -> Color(0xFF1D4ED8) // Blue
        else -> Color(0xFF4338CA) // Indigo
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = iconBgColor,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = displayName,
                        tint = iconColor,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = scheduledTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Przejdź do obecności",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
