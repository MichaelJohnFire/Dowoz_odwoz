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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.RouteConfig
import com.example.model.Student
import com.example.ui.components.TopBarWithGuardian

@Composable
fun GroupDetailsScreen(
    route: RouteConfig,
    allStudents: List<Student>,
    guardianName: String?,
    onUpdateRouteName: (newName: String) -> Unit,
    onUpdateTime: (newTime: String) -> Unit,
    onRemoveStudent: (studentId: String) -> Unit,
    onAddStudentOnTheFly: (firstName: String, studentClass: String, town: String) -> Unit,
    onAddExistingStudent: (studentId: String) -> Unit,
    onUpdateStudent: (studentId: String, firstName: String, studentClass: String, town: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showPickExistingDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditTimeDialog by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<Student?>(null) }
    var studentToRemove by remember { mutableStateOf<Student?>(null) }

    val assignedStudents = allStudents.filter { route.assignedStudentIds.contains(it.id) }
    val unassignedStudents = allStudents.filter { !route.assignedStudentIds.contains(it.id) }

    Scaffold(
        topBar = {
            TopBarWithGuardian(
                title = "Szczegóły Trasy",
                guardianName = guardianName,
                showBackButton = true,
                onBackClick = onBack
            )
        },
        floatingActionButton = {
            // Floating Action Button (+) allowing adding a new student on-the-fly (First Name, Class, Town ONLY)
            FloatingActionButton(
                onClick = { showAddStudentDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_student_to_group_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Dodaj ucznia do trasy",
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
            // 1. HEADER: Displays the group name and scheduled time
            GroupDetailsHeaderCard(
                groupName = route.name,
                scheduledTime = route.scheduledTime,
                studentCount = assignedStudents.size,
                hasUnassignedStudents = unassignedStudents.isNotEmpty(),
                onEditNameClick = { showEditNameDialog = true },
                onEditTimeClick = { showEditTimeDialog = true },
                onPickExistingClick = { showPickExistingDialog = true }
            )

            // 2. STUDENTS TABLE / LIST: Each row strictly First Name, Class, Town, Action
            GroupStudentsTableHeader()

            if (assignedStudents.isEmpty()) {
                EmptyGroupNotice(
                    groupName = route.name,
                    hasUnassignedStudents = unassignedStudents.isNotEmpty(),
                    onAddNew = { showAddStudentDialog = true },
                    onPickExisting = { showPickExistingDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    itemsIndexed(assignedStudents, key = { _, s -> s.id }) { index, student ->
                        GroupStudentTableRow(
                            index = index + 1,
                            student = student,
                            onRemove = { studentToRemove = student }
                        )
                    }
                }
            }
        }
    }

    // Modal to add a new student on-the-fly (requiring ONLY: First Name, Class, Town)
    if (showAddStudentDialog) {
        AddStudentToGroupOnTheFlyDialog(
            groupName = route.name,
            onDismiss = { showAddStudentDialog = false },
            onConfirm = { firstName, studentClass, town ->
                onAddStudentOnTheFly(firstName, studentClass, town)
                showAddStudentDialog = false
            }
        )
    }

    // Modal to assign an existing student from the master directory
    if (showPickExistingDialog) {
        PickExistingStudentDialog(
            availableStudents = unassignedStudents,
            onDismiss = { showPickExistingDialog = false },
            onSelect = { studentId ->
                onAddExistingStudent(studentId)
                showPickExistingDialog = false
            }
        )
    }

    // Modal to edit student details (First Name, Class, Town)
    if (studentToEdit != null) {
        val s = studentToEdit!!
        EditGroupStudentDialog(
            student = s,
            onDismiss = { studentToEdit = null },
            onConfirm = { firstName, studentClass, town ->
                onUpdateStudent(s.id, firstName, studentClass, town)
                studentToEdit = null
            }
        )
    }

    // Remove confirmation dialog
    if (studentToRemove != null) {
        val s = studentToRemove!!
        AlertDialog(
            onDismissRequest = { studentToRemove = null },
            title = { Text("Wypisać ucznia z trasy?") },
            text = {
                Text("Czy na pewno chcesz usunąć ucznia ${s.firstName} (klasa ${s.studentClass}) z trasy ${route.name}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveStudent(s.id)
                        studentToRemove = null
                    }
                ) {
                    Text("Usuń z trasy")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToRemove = null }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Edit route name dialog
    if (showEditNameDialog) {
        EditRouteNameDialog(
            currentName = route.name,
            onDismiss = { showEditNameDialog = false },
            onSave = { newName ->
                onUpdateRouteName(newName)
                showEditNameDialog = false
            }
        )
    }

    // Edit scheduled time dialog
    if (showEditTimeDialog) {
        GroupTimeEditDialog(
            currentTime = route.scheduledTime,
            onDismiss = { showEditTimeDialog = false },
            onSave = { newTime ->
                onUpdateTime(newTime)
                showEditTimeDialog = false
            }
        )
    }
}

@Composable
fun GroupDetailsHeaderCard(
    groupName: String,
    scheduledTime: String,
    studentCount: Int,
    hasUnassignedStudents: Boolean,
    onEditNameClick: () -> Unit,
    onEditTimeClick: () -> Unit,
    onPickExistingClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
            .testTag("group_details_header_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onEditNameClick)
                                .testTag("group_details_title_text")
                        )
                        IconButton(
                            onClick = onEditNameClick,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("edit_group_name_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Zmień nazwę trasy",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Przypisani uczniowie: $studentCount",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Scheduled Time Chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onEditTimeClick)
                        .testTag("edit_group_time_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Czas",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = scheduledTime,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Zmień czas",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (hasUnassignedStudents) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onPickExistingClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_existing_student_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Przypisz istniejącego ucznia z katalogu")
                }
            }
        }
    }
}

@Composable
fun GroupStudentsTableHeader() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lp.",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Imię",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1.5f)
            )
            Text(
                text = "Klasa",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Miejscowość",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1.5f)
            )
            Text(
                text = "Akcja",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(52.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Student Row strictly containing: First Name, Class, Town, Action button to remove from group
@Composable
fun GroupStudentTableRow(
    index: Int,
    student: Student,
    onRemove: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("group_student_row_${student.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center
            )

            // 1. Student First Name (Strictly no last names!)
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

            Spacer(modifier = Modifier.width(6.dp))

            // 3. Town (town)
            Text(
                text = student.town,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1.5f),
                maxLines = 1
            )

            // 4. Action button to remove the student from this route/transport (pencil icon removed)
            Box(
                modifier = Modifier.width(52.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("remove_group_student_${student.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Usuń ucznia z grupy",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyGroupNotice(
    groupName: String,
    hasUnassignedStudents: Boolean,
    onAddNew: () -> Unit,
    onPickExisting: () -> Unit
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
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Brak uczniów na trasie",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Do trasy $groupName nie został jeszcze przypisany żaden uczeń. Dodaj ucznia przyciskiem '+' lub wybierz z bazy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddNew,
                modifier = Modifier.testTag("empty_group_add_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dodaj ucznia do tej trasy")
            }
        }
    }
}

// Modal dialog allowing adding a new student to this group on-the-fly (ONLY: First Name, Class, Town)
@Composable
fun AddStudentToGroupOnTheFlyDialog(
    groupName: String,
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
                text = "Dodaj ucznia do trasy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Trasa: $groupName. Wprowadź dane nowego ucznia (tylko imię, klasa, miejscowość):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Field 1: First Name ONLY
                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Imię ucznia *") },
                    placeholder = { Text("np. Zuzanna") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_add_student_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Field 2: Class
                OutlinedTextField(
                    value = studentClass,
                    onValueChange = { studentClass = it },
                    label = { Text("Klasa") },
                    placeholder = { Text("np. 2B") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_add_student_class_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Field 3: Town
                OutlinedTextField(
                    value = town,
                    onValueChange = { town = it },
                    label = { Text("Miejscowość / Przystanek") },
                    placeholder = { Text("np. Lipowa") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_add_student_town_input")
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
                modifier = Modifier.testTag("confirm_group_add_student_button")
            ) {
                Text("Zapisz do trasy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

// Edit existing student details dialog
@Composable
fun EditGroupStudentDialog(
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
        title = { Text("Edytuj dane ucznia") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Imię ucznia *") },
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = studentClass,
                    onValueChange = { studentClass = it },
                    label = { Text("Klasa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = town,
                    onValueChange = { town = it },
                    label = { Text("Miejscowość / Przystanek") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                }
            ) {
                Text("Zapisz zmiany")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

// Dialog to pick an existing student from the master list
@Composable
fun PickExistingStudentDialog(
    availableStudents: List<Student>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = availableStudents.filter {
        it.firstName.contains(searchQuery, ignoreCase = true) ||
                it.studentClass.contains(searchQuery, ignoreCase = true) ||
                it.town.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wybierz ucznia z katalogu") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Szukaj (Imię, Klasa, Miejscowość)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (filtered.isEmpty()) {
                    Text(
                        text = if (availableStudents.isEmpty()) "Wszyscy zarejestrowani uczniowie są już przypisani do tej trasy." else "Brak pasujących uczniów.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        items(filtered) { s ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelect(s.id) }
                                    .padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = s.firstName, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "Klasa: ${s.studentClass} • ${s.town}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Przypisz",
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
fun GroupTimeEditDialog(
    currentTime: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var timeInput by remember { mutableStateOf(currentTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zmień planową godzinę odjazdu") },
        text = {
            OutlinedTextField(
                value = timeInput,
                onValueChange = { timeInput = it },
                label = { Text("Godzina odjazdu (np. 07:15, 14:35)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (timeInput.isNotBlank()) {
                        onSave(timeInput.trim())
                    }
                }
            ) {
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

@Composable
fun EditRouteNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Zmień nazwę trasy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Wprowadź nową nazwę dla wybranej trasy autobusowej:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Nazwa trasy *") },
                    placeholder = { Text("np. Dowóz PKS - Trasa Główna") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_route_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onSave(nameInput.trim())
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("confirm_edit_route_name_button")
            ) {
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
