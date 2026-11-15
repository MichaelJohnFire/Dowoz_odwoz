package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AttendanceStatus
import com.example.model.CourseType
import com.example.model.RouteConfig
import com.example.ui.components.TopBarWithGuardian
import com.example.util.PdfReportGenerator
import com.example.util.ReportPrintData
import com.example.viewmodel.BusViewModel
import com.example.viewmodel.ReportPeriod
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintReportScreen(
    viewModel: BusViewModel,
    guardianName: String?,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val routeConfigs by viewModel.routeConfigs.collectAsState()
    val initialDate by viewModel.selectedDate.collectAsState()
    val allStudents by viewModel.students.collectAsState()
    val allGuardians by viewModel.guardians.collectAsState()
    val activeGuardianState by viewModel.activeGuardianName.collectAsState()

    // 1. Selected Date state (defaults to current date)
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var selectedDate by remember(initialDate) {
        mutableStateOf(if (initialDate.isNotBlank()) initialDate else sdf.format(Date()))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    // 2. Selected Route state
    var selectedRouteId by remember(routeConfigs) {
        mutableStateOf(routeConfigs.firstOrNull()?.id ?: CourseType.DOWOZ_PKS.name)
    }

    val selectedRouteConfig = routeConfigs.find { it.id == selectedRouteId }
        ?: routeConfigs.firstOrNull()
        ?: RouteConfig(
            id = CourseType.DOWOZ_PKS.name,
            name = CourseType.DOWOZ_PKS.displayName,
            courseType = CourseType.DOWOZ_PKS,
            scheduledTime = CourseType.DOWOZ_PKS.defaultTime
        )

    // 3. Selected Report Period state (Single Day vs. Entire Week)
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.SINGLE_DAY) }
    val weekRange = remember(selectedDate) { viewModel.getWeekRangeForDate(selectedDate) }

    // Preview data state
    var previewData by remember { mutableStateOf<ReportPrintData?>(null) }
    var isPreparingFile by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }

    // Dialog state for print preview window
    var showPreviewDialog by remember { mutableStateOf(false) }

    // Resolves the actual guardian full name, strictly filtering out "Opiekun dyżurny"
    val actualGuardianName = remember(guardianName, activeGuardianState, allGuardians, previewData?.guardianName) {
        val fromProp = guardianName?.trim()
        if (!fromProp.isNullOrBlank() && !fromProp.equals("Opiekun dyżurny", ignoreCase = true)) {
            fromProp
        } else {
            val fromState = activeGuardianState?.trim()
            if (!fromState.isNullOrBlank() && !fromState.equals("Opiekun dyżurny", ignoreCase = true)) {
                fromState
            } else {
                val fromReport = previewData?.guardianName?.trim()
                if (!fromReport.isNullOrBlank() && !fromReport.equals("Opiekun dyżurny", ignoreCase = true)) {
                    fromReport
                } else {
                    val fromList = allGuardians.firstOrNull {
                        it.fullName.isNotBlank() && !it.fullName.equals("Opiekun dyżurny", ignoreCase = true)
                    }?.fullName?.trim()
                    fromList ?: ""
                }
            }
        }
    }

    // Reload preview data whenever date, route, or period changes
    LaunchedEffect(selectedDate, selectedRouteConfig.id, selectedPeriod, allStudents) {
        val data = viewModel.preparePdfReportData(selectedDate, selectedRouteConfig, selectedPeriod)
        previewData = data
        generatedPdfFile = null // reset generated file when parameters change
    }

    Scaffold(
        topBar = {
            TopBarWithGuardian(
                title = "Wydruk Raportu",
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
            // =========================================================================
            // TOP TABS: "Dzień" (Day) and "Tydzień" (Week)
            // =========================================================================
            TabRow(
                selectedTabIndex = if (selectedPeriod == ReportPeriod.SINGLE_DAY) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("print_tabs")
            ) {
                Tab(
                    selected = selectedPeriod == ReportPeriod.SINGLE_DAY,
                    onClick = { selectedPeriod = ReportPeriod.SINGLE_DAY },
                    text = {
                        Text(
                            text = "Dzień",
                            fontWeight = if (selectedPeriod == ReportPeriod.SINGLE_DAY) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.testTag("tab_day")
                )
                Tab(
                    selected = selectedPeriod == ReportPeriod.ENTIRE_WEEK,
                    onClick = { selectedPeriod = ReportPeriod.ENTIRE_WEEK },
                    text = {
                        Text(
                            text = "Tydzień",
                            fontWeight = if (selectedPeriod == ReportPeriod.ENTIRE_WEEK) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.testTag("tab_week")
                )
            }

            // Scrollable tab content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedPeriod == ReportPeriod.SINGLE_DAY) {
                    // =========================================================
                    // 1. "DAY" TAB (Dzień):
                    //   - Option 1: Date selection
                    //   - Option 2: Course selection
                    //   - Bottom: "Zatwierdź" button -> opens print preview
                    // =========================================================

                    // 1.1 Date Selection Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("date_selection_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "1",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Wybierz datę zdarzenia",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Plain text/info displaying selected date with calendar icon next to it
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Wybrana data:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = selectedDate,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                IconButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                        .testTag("calendar_icon_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Wybierz inną datę z kalendarza",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 1.2 Route Selection Section
                    RouteSelectionCard(
                        routeConfigs = routeConfigs,
                        selectedRouteId = selectedRouteId,
                        onSelectRoute = { selectedRouteId = it }
                    )

                    // 1.3 Confirmation Button (Bottom)
                    PrintConfirmButton(
                        onClick = { showPreviewDialog = true }
                    )

                } else {
                    // =========================================================
                    // 2. "WEEK" TAB (Tydzień):
                    //   - Option 1: Week selection from calendar
                    //   - Option 2: Course selection
                    //   - Bottom: "Zatwierdź" button -> opens weekly preview
                    // =========================================================

                    // 2.1 Week Selection Section from Calendar
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("week_selection_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "1",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Wybierz tydzień z kalendarza",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "Wskaż dowolny dzień z wybranego tygodnia w kalendarzu, aby sporządzić zbiorczy raport obecności:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Display selected week with calendar button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Wybrany tydzień szkolny:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${weekRange.mondayDate} – ${weekRange.fridayDate}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Poniedziałek – Piątek (5 dni)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                IconButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                        .testTag("week_calendar_icon_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Wybierz tydzień z kalendarza",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2.2 Route Selection Section
                    RouteSelectionCard(
                        routeConfigs = routeConfigs,
                        selectedRouteId = selectedRouteId,
                        onSelectRoute = { selectedRouteId = it }
                    )

                    // 2.3 Confirmation Button (Bottom)
                    PrintConfirmButton(
                        onClick = { showPreviewDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- MATERIAL 3 DATE PICKER DIALOG ---
    if (showDatePicker) {
        val initialMillis = remember(selectedDate) {
            runCatching {
                val parts = selectedDate.split("-")
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                cal.clear()
                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 12, 0)
                cal.timeInMillis
            }.getOrDefault(System.currentTimeMillis())
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            cal.timeInMillis = millis
                            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            format.timeZone = TimeZone.getTimeZone("UTC")
                            selectedDate = format.format(cal.time)
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("date_picker_confirm")
                ) {
                    Text("Zatwierdź", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Anuluj")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // =========================================================================
    // NEXT DIALOG WINDOW: PRINT PREVIEW WITH "PREPARE FILE" ([ Przygotuj Plik ])
    // =========================================================================
    if (showPreviewDialog) {
        Dialog(
            onDismissRequest = { showPreviewDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.92f)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Podgląd wydruku",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showPreviewDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Zamknij podgląd",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Scrollable preview body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Summary chip box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Trasa: ${selectedRouteConfig.name}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val scopeInfo = if (selectedPeriod == ReportPeriod.SINGLE_DAY) {
                                        "Data: $selectedDate • Godz. ${selectedRouteConfig.scheduledTime}"
                                    } else {
                                        "Tydzień: ${weekRange.mondayDate} – ${weekRange.fridayDate} • Godz. ${selectedRouteConfig.scheduledTime}"
                                    }
                                    Text(
                                        text = scopeInfo,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val displayGuardian = actualGuardianName.ifBlank { previewData?.guardianName ?: "" }
                                Text(
                                    text = "Opiekun: ${displayGuardian.ifBlank { "—" }}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // A4 Document Sheet Visual Preview
                        previewData?.let { report ->
                            val studentsList = report.students
                            val presentCount = studentsList.count { it.status == AttendanceStatus.PRESENT }
                            val absentCount = studentsList.count { it.status == AttendanceStatus.ABSENT }
                            val totalStudents = studentsList.size
                            val attendancePercent = if (totalStudents > 0) (presentCount * 100) / totalStudents else 0

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                shadowElevation = 3.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    // Title
                                    Text(
                                        text = report.reportScopeTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "Szkolna ewidencja przewozu uczniów",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = Color(0xFFE2E8F0))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Metadata
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("${report.dateLabel} ${report.date}", color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                            Text("Trasa: ${report.routeName}", color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Planowy odjazd: ${report.scheduledTime}", color = Color(0xFF1E293B), fontSize = 11.sp)
                                            val guardianToDisplay = actualGuardianName.ifBlank { report.guardianName }
                                            Text("Opiekun: ${guardianToDisplay.ifBlank { "—" }}", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Table Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF2563EB), RoundedCornerShape(4.dp))
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Lp.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                        Text("Imię", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                                        Text("Klasa", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                                        Text("Miejscowość", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.1f))
                                        if (selectedPeriod == ReportPeriod.ENTIRE_WEEK) {
                                            for (d in 0 until 5) {
                                                val dayLabel = report.dailySummaries?.getOrNull(d)?.dayNumber?.toString()
                                                    ?: weekRange.dates.getOrNull(d)?.substringAfterLast("-")?.toIntOrNull()?.toString()
                                                    ?: "${d + 1}"
                                                Text(
                                                    text = dayLabel,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.width(22.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        } else {
                                            Text("Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
                                        }
                                    }

                                    // Table Rows
                                    if (studentsList.isEmpty()) {
                                        Text(
                                            text = "Brak uczniów przypisanych do tej trasy.",
                                            color = Color(0xFF64748B),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                                        )
                                    } else {
                                        studentsList.forEachIndexed { index, student ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(if (index % 2 == 0) Color.White else Color(0xFFF8FAFC))
                                                    .padding(vertical = 5.dp, horizontal = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("${student.number}.", color = Color(0xFF334155), fontSize = 10.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                                Text(student.firstName, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.2f))
                                                Text(student.studentClass, color = Color(0xFF334155), fontSize = 10.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                                                Text(student.town, color = Color(0xFF334155), fontSize = 10.sp, modifier = Modifier.weight(1.1f))

                                                if (selectedPeriod == ReportPeriod.ENTIRE_WEEK) {
                                                    for (d in 0 until 5) {
                                                        val sign = student.weeklyDaySigns?.getOrNull(d)
                                                            ?: when (student.weeklyDayStatuses?.getOrNull(d)) {
                                                                AttendanceStatus.PRESENT -> "+"
                                                                else -> "-"
                                                            }
                                                        val isPresent = sign == "+"
                                                        Text(
                                                            text = sign,
                                                            color = if (isPresent) Color(0xFF16A34A) else Color(0xFFDC2626),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            modifier = Modifier.width(22.dp),
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                } else {
                                                    val statusDisplay = when (student.status) {
                                                        AttendanceStatus.PRESENT -> "+"
                                                        AttendanceStatus.ABSENT -> "-"
                                                        AttendanceStatus.UNCHECKED -> "?"
                                                    }
                                                    val statusColor = when (student.status) {
                                                        AttendanceStatus.PRESENT -> Color(0xFF16A34A)
                                                        AttendanceStatus.ABSENT -> Color(0xFFDC2626)
                                                        AttendanceStatus.UNCHECKED -> Color(0xFF64748B)
                                                    }

                                                    Text(
                                                        text = statusDisplay,
                                                        color = statusColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        modifier = Modifier.width(70.dp),
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = Color(0xFFF1F5F9))
                                         }
                                     }

                                     Spacer(modifier = Modifier.height(12.dp))

                                     // Summary Box
                                     Surface(
                                         shape = RoundedCornerShape(6.dp),
                                         color = Color(0xFFF1F5F9),
                                         border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                         modifier = Modifier.fillMaxWidth()
                                     ) {
                                         Column(modifier = Modifier.padding(8.dp)) {
                                             Text("PODSUMOWANIE OBECNOŚCI:", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                             Spacer(modifier = Modifier.height(3.dp))
                                             if (selectedPeriod == ReportPeriod.ENTIRE_WEEK && !report.dailySummaries.isNullOrEmpty()) {
                                                 report.dailySummaries.forEach { daySum ->
                                                     Text(
                                                         text = "Dzień ${daySum.dayNumber} (${daySum.date}): Liczba obecnych (+): ${daySum.presentCount}  |  Liczba nieobecnych (-): ${daySum.absentCount}",
                                                         color = Color(0xFF334155),
                                                         fontWeight = FontWeight.SemiBold,
                                                         fontSize = 9.5.sp,
                                                         modifier = Modifier.padding(vertical = 1.dp)
                                                     )
                                                 }
                                             } else {
                                                 Text(
                                                     text = "Liczba obecnych (+): $presentCount  |  Liczba nieobecnych (-): $absentCount",
                                                     color = Color(0xFF334155),
                                                     fontWeight = FontWeight.SemiBold,
                                                     fontSize = 9.5.sp
                                                 )
                                             }
                                         }
                                     }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Signature Line
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("........................................................", color = Color(0xFF64748B), fontSize = 10.sp)
                                            Text("Podpis opiekuna", color = Color(0xFF94A3B8), fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Status of file generation & direct print / share options if ready
                        generatedPdfFile?.let { file ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFDCFCE7),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Plik PDF jest gotowy do wydruku!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF14532D)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val printTitle = if (selectedPeriod == ReportPeriod.ENTIRE_WEEK) {
                                                    "Raport_Tydzien_${weekRange.mondayDate}_${selectedRouteConfig.name}"
                                                } else {
                                                    "Raport_${selectedDate}_${selectedRouteConfig.name}"
                                                }
                                                PdfReportGenerator.printPdf(context, file, printTitle)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("action_print_pdf")
                                        ) {
                                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Drukuj", fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                PdfReportGenerator.sharePdf(context, file, "Udostępnij raport PDF")
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("action_share_pdf")
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Udostępnij", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Dialog Actions: [ Przygotuj Plik ] and Cancel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showPreviewDialog = false },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.4f)
                                .height(52.dp)
                        ) {
                            Text("Zamknij")
                        }

                        Button(
                            onClick = {
                                isPreparingFile = true
                                coroutineScope.launch {
                                    try {
                                        val data = viewModel.preparePdfReportData(selectedDate, selectedRouteConfig, selectedPeriod)
                                        val file = PdfReportGenerator.generateAttendancePdf(context, data)
                                        generatedPdfFile = file
                                        Toast.makeText(context, "Plik PDF został pomyślnie przygotowany!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Błąd generowania PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isPreparingFile = false
                                    }
                                }
                            },
                            enabled = !isPreparingFile,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.6f)
                                .height(52.dp)
                                .testTag("prepare_file_button")
                        ) {
                            if (isPreparingFile) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tworzenie...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Przygotuj plik",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteSelectionCard(
    routeConfigs: List<RouteConfig>,
    selectedRouteId: String,
    onSelectRoute: (String) -> Unit,
    sectionNumber: String = "2",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("route_selection_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            sectionNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Wybierz trasę / kurs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Wybierz kurs z poniższej listy, dla którego chcesz sporządzić raport:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Vertical single-column list of route items with larger touch targets
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                routeConfigs.forEach { route ->
                    val isSelected = route.id == selectedRouteId

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRoute(route.id) }
                            .testTag("route_item_${route.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            }
                        ),
                        border = if (isSelected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBus,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = route.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Godz. ${route.scheduledTime}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectRoute(route.id) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrintConfirmButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("confirm_selection_button")
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Zatwierdź",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}


