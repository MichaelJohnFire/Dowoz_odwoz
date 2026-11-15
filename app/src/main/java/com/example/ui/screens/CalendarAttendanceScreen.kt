package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.model.AttendanceSession
import com.example.model.CourseType
import com.example.model.RouteConfig
import com.example.model.Student
import com.example.ui.components.TopBarWithGuardian
import com.example.ui.theme.AttendanceAbsentBg
import com.example.ui.theme.AttendanceAbsentRed
import com.example.ui.theme.AttendancePresentBg
import com.example.ui.theme.AttendancePresentGreen

@Composable
fun CalendarAttendanceScreen(
    selectedDate: String,
    selectedCourse: CourseType,
    currentSession: AttendanceSession?,
    allStudents: List<Student>,
    routeConfigs: List<RouteConfig>,
    guardianName: String?,
    onDateShift: (Int) -> Unit,
    onCourseSelect: (CourseType) -> Unit,
    onToggleAttendance: (studentId: String, currentStatus: Boolean) -> Unit,
    onMarkAll: (Boolean) -> Unit,
    onAddStudentToSession: (studentId: String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }

    // Map student IDs from route config and current session
    val currentRouteConfig = routeConfigs.find { it.courseType == selectedCourse }
    val assignedStudentIds = currentRouteConfig?.assignedStudentIds ?: emptySet()
    val sessionRecordMap = currentSession?.records?.associateBy { it.studentId } ?: emptyMap()

    // Combined unique list of students relevant to this session
    val displayStudentIds = (assignedStudentIds + sessionRecordMap.keys).toList()
    val studentsInSession = allStudents.filter { displayStudentIds.contains(it.id) }

    val presentCount = studentsInSession.count { sessionRecordMap[it.id]?.isPresent == true }
    val totalCount = studentsInSession.size

    Scaffold(
        topBar = {
            TopBarWithGuardian(
                title = "Kalendarz Obecności",
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
            // Date Selector Bar
            DateSelectorBar(
                selectedDate = selectedDate,
                onPreviousDay = { onDateShift(-1) },
                onNextDay = { onDateShift(1) }
            )

            // Course Type Filter Tabs (The 5 Courses)
            CourseTypeChipsRow(
                selectedCourse = selectedCourse,
                onCourseSelect = onCourseSelect
            )

            // Summary Card (Stats & Quick Actions)
            AttendanceSummaryCard(
                presentCount = presentCount,
                totalCount = totalCount,
                guardianName = currentSession?.guardianName ?: guardianName,
                onMarkAllPresent = { onMarkAll(true) },
                onMarkAllAbsent = { onMarkAll(false) },
                onAddExtraStudent = { showAddStudentDialog = true },
                onOpenNotes = { showNotesDialog = true },
                hasNotes = !currentSession?.notes.isNullOrBlank()
            )

            // Student Attendance List
            if (studentsInSession.isEmpty()) {
                EmptySessionNotice(
                    courseType = selectedCourse,
                    onAddStudent = { showAddStudentDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Lista Uczniów (${studentsInSession.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(studentsInSession, key = { it.id }) { student ->
                        val isPresent = sessionRecordMap[student.id]?.isPresent == true
                        StudentAttendanceRowCard(
                            student = student,
                            isPresent = isPresent,
                            onToggle = { onToggleAttendance(student.id, isPresent) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Modal to add an ad-hoc student not yet in this route
    if (showAddStudentDialog) {
        val nonAssignedStudents = allStudents.filter { !displayStudentIds.contains(it.id) }
        AddStudentToSessionDialog(
            availableStudents = nonAssignedStudents,
            onDismiss = { showAddStudentDialog = false },
            onSelectStudent = { studentId ->
                onAddStudentToSession(studentId)
                showAddStudentDialog = false
            }
        )
    }

    // Notes editing dialog
    if (showNotesDialog) {
        NotesDialog(
            initialNotes = currentSession?.notes ?: "",
            onDismiss = { showNotesDialog = false },
            onSave = { notes ->
                onUpdateNotes(notes)
                showNotesDialog = false
            }
        )
    }
}

@Composable
fun DateSelectorBar(
    selectedDate: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPreviousDay,
                modifier = Modifier.testTag("date_prev_button")
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Poprzedni dzień")
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = selectedDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            IconButton(
                onClick = onNextDay,
                modifier = Modifier.testTag("date_next_button")
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Następny dzień")
            }
        }
    }
}

@Composable
fun CourseTypeChipsRow(
    selectedCourse: CourseType,
    onCourseSelect: (CourseType) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CourseType.entries.toTypedArray()) { course ->
            val isSelected = course == selectedCourse
            FilterChip(
                selected = isSelected,
                onClick = { onCourseSelect(course) },
                label = {
                    Text(
                        text = course.displayName,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("course_chip_${course.name}")
            )
        }
    }
}

@Composable
fun AttendanceSummaryCard(
    presentCount: Int,
    totalCount: Int,
    guardianName: String?,
    onMarkAllPresent: () -> Unit,
    onMarkAllAbsent: () -> Unit,
    onAddExtraStudent: () -> Unit,
    onOpenNotes: () -> Unit,
    hasNotes: Boolean
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Obecność:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$presentCount z $totalCount obecnych",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (totalCount > 0 && presentCount == totalCount) AttendancePresentBg else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    val percent = if (totalCount > 0) (presentCount * 100) / totalCount else 0
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalCount > 0 && presentCount == totalCount) AttendancePresentGreen else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Actions in a moving vehicle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onMarkAllPresent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_all_present_button"),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Wszyscy", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onMarkAllAbsent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_all_absent_button"),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Icon(Icons.Default.RemoveDone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Zeruj", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onAddExtraStudent,
                    modifier = Modifier.testTag("add_extra_student_button")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Uczeń")
                }

                IconButton(
                    onClick = onOpenNotes,
                    modifier = Modifier.testTag("session_notes_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Notatka",
                        tint = if (hasNotes) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StudentAttendanceRowCard(
    student: Student,
    isPresent: Boolean,
    onToggle: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag("student_row_${student.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isPresent) AttendancePresentBg else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.firstName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Klasa: ${student.studentClass}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (student.town.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = student.town,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Big, vehicle-friendly presence toggle button
            Button(
                onClick = onToggle,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPresent) AttendancePresentGreen else MaterialTheme.colorScheme.outlineVariant,
                    contentColor = if (isPresent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .height(52.dp)
                    .width(130.dp)
                    .testTag("toggle_attendance_${student.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isPresent) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPresent) "OBECNY" else "NIEOBECNY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySessionNotice(
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
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Brak przypisanych uczniów do trasy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Na trasie ${courseType.displayName} nie ma jeszcze uczniów. Przypisz uczniów w module 'Grupy' lub dodaj ucznia doraźnie.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddStudent,
                modifier = Modifier.testTag("empty_session_add_student_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dodaj ucznia do tego kursu")
            }
        }
    }
}

@Composable
fun AddStudentToSessionDialog(
    availableStudents: List<Student>,
    onDismiss: () -> Unit,
    onSelectStudent: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = availableStudents.filter {
        it.firstName.contains(searchQuery, ignoreCase = true) ||
                it.studentClass.contains(searchQuery, ignoreCase = true) ||
                it.town.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj ucznia do kursu") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Szukaj (Imię, Klasa, Miejscowość)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (filtered.isEmpty()) {
                    Text(
                        text = if (availableStudents.isEmpty()) "Wszyscy zarejestrowani uczniowie są już na liście tego kursu." else "Brak pasujących uczniów.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        items(filtered) { student ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectStudent(student.id) }
                                    .padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = student.firstName,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Klasa: ${student.studentClass} • ${student.town}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Dodaj",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        }
    )
}

@Composable
fun NotesDialog(
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var notes by remember { mutableStateOf(initialNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notatka do kursu") },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Uwagi (np. opóźnienie, zastępstwo kierowcy)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                maxLines = 5
            )
        },
        confirmButton = {
            Button(onClick = { onSave(notes) }) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
