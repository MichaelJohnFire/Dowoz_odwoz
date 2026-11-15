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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UnfoldMore
import java.text.Collator
import java.util.Locale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttendanceRecord
import com.example.model.AttendanceSession
import com.example.model.AttendanceStatus
import com.example.model.CourseType
import com.example.model.RouteConfig
import com.example.model.Student
import com.example.ui.components.TopBarWithGuardian
import com.example.ui.theme.AttendanceAbsentBg
import com.example.ui.theme.AttendanceAbsentRed
import com.example.ui.theme.AttendancePresentBg
import com.example.ui.theme.AttendancePresentGreen
import com.example.ui.theme.AttendanceUnmarkedGrey

enum class AttendanceSortColumn {
    NAME,
    CLASS,
    TOWN,
    STATUS
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

@Composable
fun AttendanceChecklistScreen(
    courseType: CourseType,
    selectedDate: String,
    currentSession: AttendanceSession?,
    allStudents: List<Student>,
    routeConfigs: List<RouteConfig>,
    guardianName: String?,
    onCycleAttendance: (studentId: String) -> Unit,
    onSetAttendance: (studentId: String, isPresent: Boolean) -> Unit,
    onMarkAll: (Boolean) -> Unit,
    onResetAttendance: () -> Unit,
    onAddNewStudentOnTheFly: (firstName: String, studentClass: String, town: String) -> Unit,
    onUpdateScheduledTime: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    // Sorting state
    var sortColumn by remember { mutableStateOf(AttendanceSortColumn.NAME) }
    var sortDirection by remember { mutableStateOf(SortDirection.ASCENDING) }

    val currentRouteConfig = currentSession?.let { sess ->
        routeConfigs.find { it.id == sess.routeId }
    } ?: routeConfigs.find { it.courseType == courseType } ?: routeConfigs.firstOrNull()
    val assignedStudentIds = currentRouteConfig?.assignedStudentIds ?: emptySet()
    val sessionRecordMap = currentSession?.records?.associateBy { it.studentId } ?: emptyMap()

    // Combined unique list of students relevant to this session
    val displayStudentIds = (assignedStudentIds + sessionRecordMap.keys).toList()
    val studentsInSession = allStudents.filter { displayStudentIds.contains(it.id) }

    // Sorted student list based on selected column and direction
    val sortedStudents = remember(studentsInSession, sessionRecordMap, sortColumn, sortDirection) {
        val collator = Collator.getInstance(Locale("pl", "PL"))
        val sorted = when (sortColumn) {
            AttendanceSortColumn.NAME -> studentsInSession.sortedWith { a, b ->
                collator.compare(a.firstName, b.firstName)
            }
            AttendanceSortColumn.CLASS -> studentsInSession.sortedWith { a, b ->
                val cmp = collator.compare(a.studentClass, b.studentClass)
                if (cmp != 0) cmp else collator.compare(a.firstName, b.firstName)
            }
            AttendanceSortColumn.TOWN -> studentsInSession.sortedWith { a, b ->
                val cmp = collator.compare(a.town, b.town)
                if (cmp != 0) cmp else collator.compare(a.firstName, b.firstName)
            }
            AttendanceSortColumn.STATUS -> studentsInSession.sortedWith { a, b ->
                val statusA = when (sessionRecordMap[a.id]?.isPresent) {
                    null -> 0 // UNCHECKED (?)
                    true -> 1 // PRESENT (+)
                    false -> 2 // ABSENT (-)
                }
                val statusB = when (sessionRecordMap[b.id]?.isPresent) {
                    null -> 0
                    true -> 1
                    false -> 2
                }
                val cmp = statusA.compareTo(statusB)
                if (cmp != 0) cmp else collator.compare(a.firstName, b.firstName)
            }
        }
        if (sortDirection == SortDirection.DESCENDING) sorted.reversed() else sorted
    }

    val scheduledTime = currentRouteConfig?.scheduledTime ?: courseType.defaultTime
    val activeGuardian = currentSession?.guardianName ?: guardianName ?: "Opiekun dyżurny"

    // Statistics
    val presentCount = studentsInSession.count { sessionRecordMap[it.id]?.isPresent == true }
    val absentCount = studentsInSession.count { sessionRecordMap[it.id]?.isPresent == false }
    val uncheckedCount = studentsInSession.size - (presentCount + absentCount)

    Scaffold(
        topBar = {
            TopBarWithGuardian(
                title = "Lista Obecności",
                guardianName = guardianName,
                showBackButton = true,
                onBackClick = onBack
            )
        },
        floatingActionButton = {
            // Prominent + button in bottom right corner to add student on the fly
            FloatingActionButton(
                onClick = { showAddStudentDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_student_on_the_fly_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Dodaj ucznia doraźnie do kursu",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. HEADER SECTION
            ChecklistHeaderSection(
                courseName = currentRouteConfig?.name ?: courseType.displayName,
                date = selectedDate,
                scheduledTime = scheduledTime,
                presentCount = presentCount,
                absentCount = absentCount,
                onEditTimeClick = { showTimePickerDialog = true }
            )

            // 2. ENLARGED SORTABLE TABLE COLUMNS HEADER ROW
            TableListHeaderRow(
                currentSortColumn = sortColumn,
                sortDirection = sortDirection,
                onSortChange = { clickedColumn ->
                    if (sortColumn == clickedColumn) {
                        sortDirection = if (sortDirection == SortDirection.ASCENDING) {
                            SortDirection.DESCENDING
                        } else {
                            SortDirection.ASCENDING
                        }
                    } else {
                        sortColumn = clickedColumn
                        sortDirection = SortDirection.ASCENDING
                    }
                }
            )

            if (studentsInSession.isEmpty()) {
                EmptyChecklistNotice(
                    courseType = courseType,
                    onAddStudent = { showAddStudentDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(sortedStudents, key = { it.id }) { student ->
                        val record = sessionRecordMap[student.id]
                        val status = when (record?.isPresent) {
                            null -> AttendanceStatus.UNCHECKED
                            true -> AttendanceStatus.PRESENT
                            false -> AttendanceStatus.ABSENT
                        }

                        StudentTableRowCard(
                            student = student,
                            status = status,
                            onRowClick = { onCycleAttendance(student.id) },
                            onToggleAction = { onCycleAttendance(student.id) }
                        )
                    }
                }
            }
        }
    }

    // Modal to add student on the fly (requires input fields ONLY for: First Name, Class, Town)
    if (showAddStudentDialog) {
        AddStudentOnTheFlyDialog(
            onDismiss = { showAddStudentDialog = false },
            onConfirm = { firstName, studentClass, town ->
                onAddNewStudentOnTheFly(firstName, studentClass, town)
                showAddStudentDialog = false
            }
        )
    }

    // TimePicker Dialog for editable scheduled time
    if (showTimePickerDialog) {
        EditableTimePickerDialog(
            initialTime = scheduledTime,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { newTime ->
                onUpdateScheduledTime(newTime)
                showTimePickerDialog = false
            }
        )
    }
}

@Composable
fun ChecklistHeaderSection(
    courseName: String,
    date: String,
    scheduledTime: String,
    presentCount: Int,
    absentCount: Int,
    onEditTimeClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("checklist_header_section"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Row 1: Event Name & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = courseName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Departure Time (editable) & Counts of present and absent students
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Editable time button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onEditTimeClick)
                        .testTag("editable_scheduled_time_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Czas odjazdu",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Odjazd: $scheduledTime",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edytuj czas",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Present and Absent counts
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(
                        text = "$presentCount",
                        label = "Obecni",
                        textColor = AttendancePresentGreen,
                        bgColor = AttendancePresentBg
                    )
                    StatusPill(
                        text = "$absentCount",
                        label = "Nieobecni",
                        textColor = AttendanceAbsentRed,
                        bgColor = AttendanceAbsentBg
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPill(text: String, label: String, textColor: Color, bgColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}

@Composable
fun TableListHeaderRow(
    currentSortColumn: AttendanceSortColumn,
    sortDirection: SortDirection,
    onSortChange: (AttendanceSortColumn) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Imię
            SortableHeaderCell(
                title = "Imię",
                column = AttendanceSortColumn.NAME,
                currentSortColumn = currentSortColumn,
                sortDirection = sortDirection,
                modifier = Modifier.weight(1.5f),
                onClick = { onSortChange(AttendanceSortColumn.NAME) }
            )

            // 2. Klasa
            SortableHeaderCell(
                title = "Klasa",
                column = AttendanceSortColumn.CLASS,
                currentSortColumn = currentSortColumn,
                sortDirection = sortDirection,
                modifier = Modifier.weight(1f),
                onClick = { onSortChange(AttendanceSortColumn.CLASS) }
            )

            Spacer(modifier = Modifier.width(4.dp))

            // 3. Miejscowość
            SortableHeaderCell(
                title = "Miejscowość",
                column = AttendanceSortColumn.TOWN,
                currentSortColumn = currentSortColumn,
                sortDirection = sortDirection,
                modifier = Modifier.weight(1.5f),
                onClick = { onSortChange(AttendanceSortColumn.TOWN) }
            )

            // 4. Akcja
            SortableHeaderCell(
                title = "Akcja",
                column = AttendanceSortColumn.STATUS,
                currentSortColumn = currentSortColumn,
                sortDirection = sortDirection,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.Center,
                onClick = { onSortChange(AttendanceSortColumn.STATUS) }
            )
        }
    }
}

@Composable
fun SortableHeaderCell(
    title: String,
    column: AttendanceSortColumn,
    currentSortColumn: AttendanceSortColumn,
    sortDirection: SortDirection,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    onClick: () -> Unit
) {
    val isSelected = currentSortColumn == column

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .testTag("sort_header_${column.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (textAlign == TextAlign.Center) Arrangement.Center else Arrangement.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(3.dp))
            if (isSelected) {
                Icon(
                    imageVector = if (sortDirection == SortDirection.ASCENDING) {
                        Icons.Default.ArrowUpward
                    } else {
                        Icons.Default.ArrowDownward
                    },
                    contentDescription = if (sortDirection == SortDirection.ASCENDING) "Sortuj rosnąco" else "Sortuj malejąco",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.UnfoldMore,
                    contentDescription = "Kliknij, aby posortować",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun StudentTableRowCard(
    student: Student,
    status: AttendanceStatus,
    onRowClick: () -> Unit,
    onToggleAction: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onRowClick)
            .testTag("checklist_row_${student.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Student First Name (ONLY first name - NO last names!)
            Text(
                text = student.firstName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1.5f),
                maxLines = 1
            )

            // 2. Class (studentClass)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = student.studentClass,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // 3. Town (town)
            Text(
                text = student.town,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1.5f),
                maxLines = 1
            )

            // 4. Interactive Action Button to check attendance (?, +, -)
            Button(
                onClick = onToggleAction,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (status) {
                        AttendanceStatus.PRESENT -> AttendancePresentGreen
                        AttendanceStatus.ABSENT -> AttendanceAbsentRed
                        AttendanceStatus.UNCHECKED -> AttendanceUnmarkedGrey
                    },
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                    .width(80.dp)
                    .height(44.dp)
                    .testTag("action_toggle_${student.id}")
            ) {
                Text(
                    text = when (status) {
                        AttendanceStatus.PRESENT -> "+"
                        AttendanceStatus.ABSENT -> "-"
                        AttendanceStatus.UNCHECKED -> "?"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun EmptyChecklistNotice(
    courseType: CourseType,
    onAddStudent: () -> Unit
) {
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
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Brak uczniów na liście kursu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Dodaj ucznia doraźnie za pomocą przycisku '+' w prawym dolnym rogu lub skonfiguruj stałą grupę w module 'Grupy'.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddStudent,
                modifier = Modifier.testTag("empty_add_student_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dodaj pierwszego ucznia")
            }
        }
    }
}

// Dialog allowing user to add a new student on the fly (requires input fields ONLY for: First Name, Class, Town)
@Composable
fun AddStudentOnTheFlyDialog(
    onDismiss: () -> Unit,
    onConfirm: (firstName: String, studentClass: String, town: String) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var studentClass by remember { mutableStateOf("") }
    var town by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Dodaj ucznia doraźnie do kursu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Wprowadź dane ucznia wsiadającego do autobusu. Uczeń zostanie natychmiast oznaczony jako obecny.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Input 1: First Name ONLY
                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Imię ucznia *") },
                    placeholder = { Text("np. Michał") },
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("on_the_fly_firstname_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Input 2: Class
                OutlinedTextField(
                    value = studentClass,
                    onValueChange = { studentClass = it },
                    label = { Text("Klasa") },
                    placeholder = { Text("np. 3A, 4") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("on_the_fly_class_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Input 3: Town
                OutlinedTextField(
                    value = town,
                    onValueChange = { town = it },
                    label = { Text("Miejscowość / Przystanek") },
                    placeholder = { Text("np. Lipowa") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("on_the_fly_town_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (firstName.isNotBlank()) {
                        onConfirm(firstName.trim(), studentClass.trim(), town.trim())
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("confirm_on_the_fly_add_button")
            ) {
                Text("Dodaj i oznacz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
fun EditableTimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var timeInput by remember { mutableStateOf(initialTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Zmień planowy czas odjazdu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Jeśli autobus ma opóźnienie lub odjeżdża o innej godzinie, zaktualizuj czas:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = timeInput,
                    onValueChange = { timeInput = it },
                    label = { Text("Godzina odjazdu (GG:MM)") },
                    placeholder = { Text("np. 07:15, 14:30") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editable_time_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (timeInput.isNotBlank()) {
                        onConfirm(timeInput.trim())
                    }
                },
                modifier = Modifier.testTag("save_time_button")
            ) {
                Text("Zapisz czas")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
