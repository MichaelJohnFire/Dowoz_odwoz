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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.model.Student
import com.example.model.StudentHistoryItem
import com.example.ui.components.TopBarWithGuardian
import com.example.ui.theme.AttendanceAbsentBg
import com.example.ui.theme.AttendanceAbsentRed
import com.example.ui.theme.AttendancePresentBg
import com.example.ui.theme.AttendancePresentGreen

@Composable
fun StudentListScreen(
    students: List<Student>,
    guardianName: String?,
    historyStudent: Student?,
    studentHistoryItems: List<StudentHistoryItem>,
    onAddStudent: (firstName: String, studentClass: String, town: String) -> Unit,
    onUpdateStudent: (id: String, firstName: String, studentClass: String, town: String) -> Unit = { _, _, _, _ -> },
    onDeleteStudent: (String) -> Unit,
    onOpenStudentHistory: (Student) -> Unit,
    onCloseStudentHistory: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var showAddDialog by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<Student?>(null) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableStateOf("Wszystkie") }

    val availableClasses = listOf("Wszystkie") + students.map { it.studentClass }.filter { it.isNotBlank() }.distinct().sorted()

    val filteredStudents = students.filter { student ->
        val matchesSearch = student.firstName.contains(searchQuery, ignoreCase = true) ||
                student.town.contains(searchQuery, ignoreCase = true)
        val matchesClass = selectedClassFilter == "Wszystkie" || student.studentClass == selectedClassFilter
        matchesSearch && matchesClass
    }

    Scaffold(
        topBar = {
            TopBarWithGuardian(
                title = "Katalog Uczniów",
                guardianName = guardianName,
                showBackButton = true,
                onBackClick = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_student_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj ucznia")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search and Class Filters
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Szukaj (Tylko imię, miejscowość)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_search_input")
                )

                if (availableClasses.size > 2) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(availableClasses) { cls ->
                            FilterChip(
                                selected = cls == selectedClassFilter,
                                onClick = { selectedClassFilter = cls },
                                label = { Text(if (cls == "Wszystkie") "Wszystkie klasy" else "Klasa $cls") }
                            )
                        }
                    }
                }
            }

            // Stats summary banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Łącznie uczniów: ${students.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Dotknij ucznia, aby zobaczyć historię",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (students.isEmpty()) "Baza uczniów jest pusta" else "Brak wyników wyszukiwania",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (students.isEmpty()) "Naciśnij przycisk '+' poniżej, aby dodać pierwszego ucznia." else "Zmień parametry wyszukiwania.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredStudents, key = { it.id }) { student ->
                        StudentDirectoryCard(
                            student = student,
                            onClick = { onOpenStudentHistory(student) },
                            onEdit = { studentToEdit = student },
                            onDelete = { studentToDelete = student }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Add Student Dialog
    if (showAddDialog) {
        AddStudentDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { firstName, studentClass, town ->
                onAddStudent(firstName, studentClass, town)
                showAddDialog = false
            }
        )
    }

    // Edit Student Dialog (modifying First Name, Class, Town)
    if (studentToEdit != null) {
        val s = studentToEdit!!
        EditStudentDialog(
            student = s,
            onDismiss = { studentToEdit = null },
            onConfirm = { firstName, studentClass, town ->
                onUpdateStudent(s.id, firstName, studentClass, town)
                studentToEdit = null
            }
        )
    }

    // Delete confirmation dialog
    if (studentToDelete != null) {
        val s = studentToDelete!!
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Usunąć ucznia?") },
            text = {
                Text("Czy na pewno chcesz usunąć ucznia: ${s.firstName} (klasa ${s.studentClass}) z bazy?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteStudent(s.id)
                        studentToDelete = null
                    }
                ) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Detailed Student Attendance History Dialog
    if (historyStudent != null) {
        StudentHistoryDialog(
            student = historyStudent,
            history = studentHistoryItems,
            onDismiss = onCloseStudentHistory
        )
    }
}

@Composable
fun StudentDirectoryCard(
    student: Student,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("student_card_${student.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = student.firstName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Klasa: ${student.studentClass}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (student.town.isNotBlank()) {
                            Text(
                                text = "• ${student.town}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pencil icon to edit student data
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("student_edit_btn_${student.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edytuj dane ucznia",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onClick,
                    modifier = Modifier.testTag("student_history_btn_${student.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Historia obecności",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("student_delete_btn_${student.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Usuń",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun EditStudentDialog(
    student: Student,
    onDismiss: () -> Unit,
    onConfirm: (firstName: String, studentClass: String, town: String) -> Unit
) {
    var firstName by remember { mutableStateOf(student.firstName) }
    var studentClass by remember { mutableStateOf(student.studentClass) }
    var town by remember { mutableStateOf(student.town) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edytuj Dane Ucznia") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Zgodnie z zasadami prywatności wprowadzamy wyłącznie Imię ucznia (bez nazwiska).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Imię ucznia *") },
                    placeholder = { Text("np. Kacper") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_student_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = studentClass,
                    onValueChange = { studentClass = it },
                    label = { Text("Klasa") },
                    placeholder = { Text("np. 3A, 4B, 1") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_student_class_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = town,
                    onValueChange = { town = it },
                    label = { Text("Miejscowość / Przystanek") },
                    placeholder = { Text("np. Lipnica, Osiedle") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_student_town_input")
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
                modifier = Modifier.testTag("edit_student_confirm_btn")
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("edit_student_dismiss_btn")
            ) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
fun AddStudentDialog(
    onDismiss: () -> Unit,
    onConfirm: (firstName: String, studentClass: String, town: String) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var studentClass by remember { mutableStateOf("") }
    var town by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj Nowego Ucznia") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Zgodnie z zasadami prywatności wprowadzamy wyłącznie Imię ucznia (bez nazwiska).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Imię ucznia *") },
                    placeholder = { Text("np. Kacper") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_student_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = studentClass,
                    onValueChange = { studentClass = it },
                    label = { Text("Klasa") },
                    placeholder = { Text("np. 3A, 4B, 1") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_student_class_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = town,
                    onValueChange = { town = it },
                    label = { Text("Miejscowość / Przystanek") },
                    placeholder = { Text("np. Lipnica, Osiedle") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_student_town_input")
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
                modifier = Modifier.testTag("confirm_add_student_button")
            ) {
                Text("Dodaj ucznia")
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
fun StudentHistoryDialog(
    student: Student,
    history: List<StudentHistoryItem>,
    onDismiss: () -> Unit
) {
    val presentCount = history.count { it.isPresent }
    val totalCount = history.size
    val attendanceRate = if (totalCount > 0) (presentCount * 100) / totalCount else 100

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Historia obecności",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${student.firstName} (Klasa: ${student.studentClass})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Łącznie przejazdów: $totalCount", style = MaterialTheme.typography.bodyMedium)
                            Text("Obecności: $presentCount", style = MaterialTheme.typography.bodyMedium)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "$attendanceRate%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (history.isEmpty()) {
                    Text(
                        text = "Brak odnotowanych kursów dla tego ucznia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(history) { item ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (item.isPresent) AttendancePresentBg else AttendanceAbsentBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "${item.date} • ${item.courseType.displayName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Opiekun: ${item.guardianName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (item.isPresent) AttendancePresentGreen else AttendanceAbsentRed
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (item.isPresent) Icons.Default.Check else Icons.Default.Close,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (item.isPresent) "Obecny" else "Nieobecny",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        }
    )
}
