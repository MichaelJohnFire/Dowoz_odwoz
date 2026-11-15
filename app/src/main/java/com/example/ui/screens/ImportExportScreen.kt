package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ImportDialogState
import com.example.model.ImportStrategy
import com.example.model.ParsedBackup
import com.example.model.ParsedRoute
import com.example.model.ParsedStudent
import com.example.ui.components.TopBarWithGuardian
import com.example.viewmodel.BusViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ImportExportScreen(
    viewModel: BusViewModel,
    guardianName: String?,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Export, 1: Import

    val students by viewModel.students.collectAsStateWithLifecycle()
    val routes by viewModel.routeConfigs.collectAsStateWithLifecycle()
    val dialogState by viewModel.importDialogState.collectAsStateWithLifecycle()

    // State for viewing JSON preview dialog
    var previewTitle by remember { mutableStateOf<String?>(null) }
    var previewContent by remember { mutableStateOf("") }
    var previewFileName by remember { mutableStateOf("") }

    // State for Manual Paste Text Import Dialog
    var manualPasteTarget by remember { mutableStateOf<String?>(null) } // "students", "routes", "backup"
    var manualPasteText by remember { mutableStateOf("") }

    // SAF Launchers for file picking
    val studentFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            readTextFromUri(context, uri) { content ->
                viewModel.handleStudentFileContent(content)
            }
        }
    }

    val routesFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            readTextFromUri(context, uri) { content ->
                viewModel.handleRoutesFileContent(content)
            }
        }
    }

    val fullBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            readTextFromUri(context, uri) { content ->
                viewModel.handleFullBackupFileContent(content)
            }
        }
    }

    Scaffold(
        topBar = {
            TopBarWithGuardian(
                title = "Import / Eksport Danych",
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Eksport Danych")
                        }
                    },
                    modifier = Modifier.testTag("tab_export")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Danych")
                        }
                    },
                    modifier = Modifier.testTag("tab_import")
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == 0) {
                    // ==========================================
                    // SECTION A: EXPORT DATA
                    // ==========================================
                    Text(
                        text = "Wybierz dane do wyeksportowania",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 1. Export All Students List
                    ExportOptionCard(
                        title = "1. Lista Wszystkich Uczniów",
                        subtitle = "Plik JSON zawierający centralny katalog uczniów (pola: first_name, class, town). Bezpieczny format bez nazwisk.",
                        badgeText = "${students.size} uczniów",
                        icon = Icons.Default.FormatListBulleted,
                        iconTint = Color(0xFF0284C7),
                        iconBg = Color(0xFFE0F2FE),
                        onExportAndShare = {
                            scope.launch {
                                val (fileName, content) = viewModel.getExportStudents()
                                shareJsonFile(
                                    context = context,
                                    fileName = fileName,
                                    jsonContent = content,
                                    shareTitle = "Udostępnij listę uczniów"
                                )
                            }
                        },
                        onPreview = {
                            scope.launch {
                                val (fileName, content) = viewModel.getExportStudents()
                                previewTitle = "Podgląd: Lista Uczniów JSON"
                                previewFileName = fileName
                                previewContent = content
                            }
                        },
                        shareTestTag = "export_students_button",
                        previewTestTag = "preview_students_button"
                    )

                    // 2. Export Routes and Groups
                    ExportOptionCard(
                        title = "2. Trasy i Grupy Autobusowe",
                        subtitle = "Plik JSON zawierający nazwy tras, planowane godziny odjazdów oraz listę uczniów przypisanych do każdego kursu.",
                        badgeText = "${routes.size} tras",
                        icon = Icons.Default.AltRoute,
                        iconTint = Color(0xFFD97706),
                        iconBg = Color(0xFFFEF3C7),
                        onExportAndShare = {
                            scope.launch {
                                val (fileName, content) = viewModel.getExportRoutes()
                                shareJsonFile(
                                    context = context,
                                    fileName = fileName,
                                    jsonContent = content,
                                    shareTitle = "Udostępnij trasy i grupy"
                                )
                            }
                        },
                        onPreview = {
                            scope.launch {
                                val (fileName, content) = viewModel.getExportRoutes()
                                previewTitle = "Podgląd: Trasy i Grupy JSON"
                                previewFileName = fileName
                                previewContent = content
                            }
                        },
                        shareTestTag = "export_routes_button",
                        previewTestTag = "preview_routes_button"
                    )

                    // 3. Export Full Application Backup
                    ExportOptionCard(
                        title = "3. Pełna Kopia Zapasowa (Backup)",
                        subtitle = "Kompleksowa kopia zapasowa wszystkich danych: uczniowie, trasy, opiekunowie dyżurni oraz pełna historia sprawdzania obecności.",
                        badgeText = "Kompletny backup",
                        icon = Icons.Default.Save,
                        iconTint = Color(0xFF059669),
                        iconBg = Color(0xFFD1FAE5),
                        onExportAndShare = {
                            scope.launch {
                                val (fileName, content) = viewModel.getExportFullBackup()
                                shareJsonFile(
                                    context = context,
                                    fileName = fileName,
                                    jsonContent = content,
                                    shareTitle = "Udostępnij pełną kopię zapasową"
                                )
                            }
                        },
                        onPreview = {
                            scope.launch {
                                val (fileName, content) = viewModel.getExportFullBackup()
                                previewTitle = "Podgląd: Pełna Kopia Zapasowa JSON"
                                previewFileName = fileName
                                previewContent = content
                            }
                        },
                        shareTestTag = "export_full_backup_button",
                        previewTestTag = "preview_full_backup_button"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    // ==========================================
                    // SECTION B: IMPORT DATA
                    // ==========================================
                    Text(
                        text = "Wczytaj dane z pliku JSON",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Możesz wczytać plik wyeksportowany z innego telefonu lub komputera. Po wybraniu pliku aplikacja zweryfikuje jego strukturę i pozwoli wybrać scalenie (Merge) lub zastąpienie (Overwrite).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. Import Students List
                    ImportOptionCard(
                        title = "1. Importuj Listę Uczniów",
                        subtitle = "Wczytuje listę uczniów do centralnego katalogu. Wymagane pola: first_name, class, town.",
                        icon = Icons.Default.FormatListBulleted,
                        iconTint = Color(0xFF0284C7),
                        iconBg = Color(0xFFE0F2FE),
                        onPickFile = {
                            studentFileLauncher.launch("*/*")
                        },
                        onPasteText = {
                            manualPasteTarget = "students"
                            manualPasteText = ""
                        },
                        fileTestTag = "import_students_picker_button",
                        pasteTestTag = "import_students_paste_button"
                    )

                    // 2. Import Routes and Groups
                    ImportOptionCard(
                        title = "2. Importuj Trasy i Grupy",
                        subtitle = "Przywraca konfigurację tras, godziny oraz przypisania uczniów. Uczniowie z tras zostaną automatycznie dopisani do bazy.",
                        icon = Icons.Default.AltRoute,
                        iconTint = Color(0xFFD97706),
                        iconBg = Color(0xFFFEF3C7),
                        onPickFile = {
                            routesFileLauncher.launch("*/*")
                        },
                        onPasteText = {
                            manualPasteTarget = "routes"
                            manualPasteText = ""
                        },
                        fileTestTag = "import_routes_picker_button",
                        pasteTestTag = "import_routes_paste_button"
                    )

                    // 3. Import Full Backup
                    ImportOptionCard(
                        title = "3. Przywróć Pełną Kopię Zapasową",
                        subtitle = "Przywraca kompletną bazę danych (uczniowie, trasy, opiekunowie, historia sesji).",
                        icon = Icons.Default.Save,
                        iconTint = Color(0xFF059669),
                        iconBg = Color(0xFFD1FAE5),
                        onPickFile = {
                            fullBackupFileLauncher.launch("*/*")
                        },
                        onPasteText = {
                            manualPasteTarget = "backup"
                            manualPasteText = ""
                        },
                        fileTestTag = "import_full_backup_picker_button",
                        pasteTestTag = "import_full_backup_paste_button"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // ==========================================
    // IMPORT CONFIRMATION & STRATEGY DIALOGS
    // ==========================================
    when (val state = dialogState) {
        is ImportDialogState.None -> { /* No dialog */ }

        is ImportDialogState.StudentsPending -> {
            StudentsImportConfirmationDialog(
                students = state.students,
                onConfirm = { strategy ->
                    viewModel.executeStudentImport(state.students, strategy)
                },
                onDismiss = { viewModel.dismissImportDialog() }
            )
        }

        is ImportDialogState.RoutesPending -> {
            RoutesImportConfirmationDialog(
                routes = state.routes,
                onConfirm = { strategy ->
                    viewModel.executeRoutesImport(state.routes, strategy)
                },
                onDismiss = { viewModel.dismissImportDialog() }
            )
        }

        is ImportDialogState.FullBackupPending -> {
            FullBackupImportConfirmationDialog(
                backup = state.backup,
                onConfirm = { strategy ->
                    viewModel.executeFullBackupImport(state.rawJson, strategy)
                },
                onDismiss = { viewModel.dismissImportDialog() }
            )
        }

        is ImportDialogState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissImportDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = { Text(state.title, fontWeight = FontWeight.Bold) },
                text = { Text(state.message) },
                confirmButton = {
                    Button(onClick = { viewModel.dismissImportDialog() }) {
                        Text("Rozumiem")
                    }
                }
            )
        }
    }

    // ==========================================
    // JSON PREVIEW DIALOG
    // ==========================================
    if (previewTitle != null) {
        Dialog(onDismissRequest = { previewTitle = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = previewTitle ?: "Podgląd JSON",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { previewTitle = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Zamknij")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val scrollState = rememberScrollState()
                        Text(
                            text = previewContent,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(scrollState)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(previewFileName, previewContent)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Skopiowano do schowka!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kopiuj")
                        }

                        Button(
                            onClick = {
                                shareJsonFile(
                                    context = context,
                                    fileName = previewFileName,
                                    jsonContent = previewContent,
                                    shareTitle = previewTitle ?: "Udostępnij JSON"
                                )
                                previewTitle = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Udostępnij")
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // MANUAL PASTE TEXT DIALOG
    // ==========================================
    if (manualPasteTarget != null) {
        Dialog(onDismissRequest = { manualPasteTarget = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Wklej tekst JSON",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Możesz wkleić zawartość JSON skopiowaną ze schowka:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val item = clipboard.primaryClip?.getItemAt(0)
                            val text = item?.text?.toString()
                            if (!text.isNullOrBlank()) {
                                manualPasteText = text
                                Toast.makeText(context, "Wklejono tekst ze schowka", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Schowek jest pusty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Wklej ze schowka")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = manualPasteText,
                        onValueChange = { manualPasteText = it },
                        label = { Text("Zawartość JSON") },
                        placeholder = { Text("{\"version\": \"1.0\", ...}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        maxLines = 12
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { manualPasteTarget = null }) {
                            Text("Anuluj")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val target = manualPasteTarget
                                val text = manualPasteText
                                manualPasteTarget = null
                                when (target) {
                                    "students" -> viewModel.handleStudentFileContent(text)
                                    "routes" -> viewModel.handleRoutesFileContent(text)
                                    "backup" -> viewModel.handleFullBackupFileContent(text)
                                }
                            },
                            enabled = manualPasteText.isNotBlank()
                        ) {
                            Text("Zweryfikuj")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportOptionCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onExportAndShare: () -> Unit,
    onPreview: () -> Unit,
    shareTestTag: String,
    previewTestTag: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
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
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = iconBg,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconBg
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = iconTint,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag(previewTestTag)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Podgląd")
                }

                Button(
                    onClick = onExportAndShare,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag(shareTestTag)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Udostępnij JSON")
                }
            }
        }
    }
}

@Composable
private fun ImportOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onPickFile: () -> Unit,
    onPasteText: () -> Unit,
    fileTestTag: String,
    pasteTestTag: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconBg,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPickFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(fileTestTag)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Wybierz plik JSON z urządzenia")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onPasteText,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag(pasteTestTag)
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Wklej tekst ze schowka", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ==========================================
// CONFIRMATION DIALOGS IMPLEMENTATION
// ==========================================

@Composable
private fun StudentsImportConfirmationDialog(
    students: List<ParsedStudent>,
    onConfirm: (ImportStrategy) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStrategy by remember { mutableStateOf(ImportStrategy.MERGE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Zweryfikowano listę uczniów", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "W pliku znaleziono: ${students.size} uczniów",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Przykładowe pozycje:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                students.take(3).forEach { s ->
                    Text(
                        text = "• ${s.firstName} (klasa ${s.studentClass}, ${s.town})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (students.size > 3) {
                    Text(
                        text = "... oraz ${students.size - 3} innych",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Wybierz sposób importu:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Option: MERGE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedStrategy = ImportStrategy.MERGE }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    RadioButton(
                        selected = selectedStrategy == ImportStrategy.MERGE,
                        onClick = { selectedStrategy = ImportStrategy.MERGE }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Scal dane (Merge) - Zalecane",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dopisuje nowych uczniów. Istniejący uczniowie nie zostaną usunięci.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Option: OVERWRITE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedStrategy = ImportStrategy.OVERWRITE }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    RadioButton(
                        selected = selectedStrategy == ImportStrategy.OVERWRITE,
                        onClick = { selectedStrategy = ImportStrategy.OVERWRITE }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Zastąp dane (Overwrite)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Uwaga: usunie obecną listę uczniów i zastąpi ją danymi z pliku.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedStrategy) },
                colors = if (selectedStrategy == ImportStrategy.OVERWRITE) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier.testTag("confirm_import_button")
            ) {
                Text(if (selectedStrategy == ImportStrategy.OVERWRITE) "Zastąp i Wczytaj" else "Scal i Wczytaj")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_import_button")
            ) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun RoutesImportConfirmationDialog(
    routes: List<ParsedRoute>,
    onConfirm: (ImportStrategy) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStrategy by remember { mutableStateOf(ImportStrategy.MERGE) }
    val totalAssignedStudents = routes.sumOf { it.students.size }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Zweryfikowano plik tras", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Znaleziono: ${routes.size} tras (oraz $totalAssignedStudents przypisań uczniów)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Trasy w pliku:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                routes.take(4).forEach { r ->
                    Text(
                        text = "• ${r.routeName} (${r.scheduledTime}) - ${r.students.size} uczniów",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Wybierz sposób importu:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedStrategy = ImportStrategy.MERGE }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    RadioButton(
                        selected = selectedStrategy == ImportStrategy.MERGE,
                        onClick = { selectedStrategy = ImportStrategy.MERGE }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Scal dane (Merge) - Zalecane",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aktualizuje istniejące trasy i łączy przypisania uczniów.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedStrategy = ImportStrategy.OVERWRITE }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    RadioButton(
                        selected = selectedStrategy == ImportStrategy.OVERWRITE,
                        onClick = { selectedStrategy = ImportStrategy.OVERWRITE }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Zastąp dane (Overwrite)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Zastępuje całą obecną konfigurację tras zawartością pliku.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedStrategy) },
                colors = if (selectedStrategy == ImportStrategy.OVERWRITE) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier.testTag("confirm_import_button")
            ) {
                Text(if (selectedStrategy == ImportStrategy.OVERWRITE) "Zastąp i Wczytaj" else "Scal i Wczytaj")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_import_button")
            ) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun FullBackupImportConfirmationDialog(
    backup: ParsedBackup,
    onConfirm: (ImportStrategy) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStrategy by remember { mutableStateOf(ImportStrategy.MERGE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pełna Kopia Zapasowa", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Zawartość kopii zapasowej:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "• Uczniowie: ${backup.students.size}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "• Trasy: ${backup.routes.size}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "• Opiekunowie: ${backup.guardians.size}", style = MaterialTheme.typography.bodySmall)
                        if (backup.sessionsCount > 0) {
                            Text(text = "• Sesje obecności: ${backup.sessionsCount}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Wybierz sposób importu:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedStrategy = ImportStrategy.MERGE }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    RadioButton(
                        selected = selectedStrategy == ImportStrategy.MERGE,
                        onClick = { selectedStrategy = ImportStrategy.MERGE }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Scal z obecnymi danymi (Merge)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Zachowuje obecne rekordy i dopisuje brakujące.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedStrategy = ImportStrategy.OVERWRITE }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    RadioButton(
                        selected = selectedStrategy == ImportStrategy.OVERWRITE,
                        onClick = { selectedStrategy = ImportStrategy.OVERWRITE }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Pełne przywrócenie (Overwrite)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Czyści obecną bazę i wgrywa dokładnie stan z kopii zapasowej.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedStrategy) },
                colors = if (selectedStrategy == ImportStrategy.OVERWRITE) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier.testTag("confirm_import_button")
            ) {
                Text(if (selectedStrategy == ImportStrategy.OVERWRITE) "Przywróć (Zastąp)" else "Przywróć (Scal)")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_import_button")
            ) {
                Text("Anuluj")
            }
        }
    )
}

// ==========================================
// UTILITY FUNCTIONS
// ==========================================

private fun shareJsonFile(
    context: Context,
    fileName: String,
    jsonContent: String,
    shareTitle: String
) {
    try {
        val cacheFile = File(context.cacheDir, fileName)
        cacheFile.writeText(jsonContent)

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            putExtra(Intent.EXTRA_TEXT, jsonContent)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, shareTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Błąd udostępniania: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

private fun readTextFromUri(context: Context, uri: Uri, onContentRead: (String) -> Unit) {
    try {
        val content = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().use { it.readText() }
        }
        if (!content.isNullOrBlank()) {
            onContentRead(content)
        } else {
            Toast.makeText(context, "Wybrany plik jest pusty", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Błąd odczytu pliku: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
