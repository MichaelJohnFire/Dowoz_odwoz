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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import com.example.model.RouteConfig
import com.example.ui.components.TopBarWithGuardian

@Composable
fun GroupsListScreen(
    routeConfigs: List<RouteConfig>,
    guardianName: String?,
    onGroupClick: (String) -> Unit,
    onAddNewGroup: (name: String, time: String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var showAddRouteDialog by remember { mutableStateOf(false) }
    var routeToDelete by remember { mutableStateOf<RouteConfig?>(null) }

    Scaffold(
        topBar = {
            TopBarWithGuardian(
                title = "Trasy",
                guardianName = guardianName,
                showBackButton = true,
                onBackClick = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddRouteDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_new_group_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Dodaj nową trasę / grupę",
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
            // Header summary
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Konfiguracja tras autobusowych",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kliknij trasę, aby zarządzać przypisanymi uczniami",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${routeConfigs.size} tras",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Routes List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(routeConfigs, key = { it.id }) { route ->
                    GroupRouteCard(
                        route = route,
                        onClick = { onGroupClick(route.id) },
                        onDelete = { routeToDelete = route }
                    )
                }
            }
        }
    }

    // Modal to add a new route/group (Route Name + Scheduled Time)
    if (showAddRouteDialog) {
        AddNewRouteDialog(
            onDismiss = { showAddRouteDialog = false },
            onConfirm = { name, time ->
                onAddNewGroup(name, time)
                showAddRouteDialog = false
            }
        )
    }

    // Delete route confirmation
    if (routeToDelete != null) {
        val r = routeToDelete!!
        AlertDialog(
            onDismissRequest = { routeToDelete = null },
            title = {
                Text(
                    text = "Usunięcie trasy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Czy na pewno chcesz usunąć trasę: ${r.name}?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroup(r.id)
                        routeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_route_button")
                ) {
                    Text("Usuń trasę")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { routeToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_route_button")
                ) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
fun GroupRouteCard(
    route: RouteConfig,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isGimbus = route.name.contains("GIMBUS", ignoreCase = true)
    val iconBgColor = if (isGimbus) Color(0xFFFEF3C7) else Color(0xFFDBEAFE)
    val iconColor = if (isGimbus) Color(0xFFB45309) else Color(0xFF1D4ED8)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("group_route_card_${route.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconBgColor,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = route.name,
                        tint = iconColor,
                        modifier = Modifier.padding(11.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = route.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = route.scheduledTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_route_${route.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Usuń trasę",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Szczegóły",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(9.dp)
                    )
                }
            }
        }
    }
}

// Modal dialog with Route Name (Text input) and Scheduled Time (Picker / Time input)
@Composable
fun AddNewRouteDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, time: String) -> Unit
) {
    var routeName by remember { mutableStateOf("") }
    var scheduledTime by remember { mutableStateOf("07:15") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Dodaj Nową Trasę / Grupę",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Wprowadź nazwę nowej trasy oraz planowaną godzinę odjazdu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Field 1: Route Name
                OutlinedTextField(
                    value = routeName,
                    onValueChange = {
                        routeName = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Nazwa trasy *") },
                    placeholder = { Text("np. Dowóz Trasa Północna") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_route_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Field 2: Scheduled Time
                OutlinedTextField(
                    value = scheduledTime,
                    onValueChange = { scheduledTime = it },
                    label = { Text("Planowa godzina (GG:MM) *") },
                    placeholder = { Text("np. 07:15, 14:00") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.AccessTime, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_route_time_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (routeName.isNotBlank() && scheduledTime.isNotBlank()) {
                        onConfirm(routeName.trim(), scheduledTime.trim())
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("confirm_add_route_button")
            ) {
                Text("Utwórz trasę")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
