package com.example.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Student
import com.example.viewmodel.AppScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MenuModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String? = null,
    val iconColor: Color,
    val iconBgColor: Color,
    val targetScreen: AppScreen,
    val testTag: String
)

@Composable
fun MainMenuScreen(
    guardianName: String?,
    students: List<Student>,
    selectedDate: String,
    routesCount: Int = 5,
    isDarkTheme: Boolean = false,
    onSwitchToLightMode: () -> Unit = {},
    onSwitchToDarkMode: () -> Unit = {},
    onNavigate: (AppScreen) -> Unit
) {
    val polishDateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("pl", "PL"))
    val formattedDate = try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)
        if (parsed != null) polishDateFormat.format(parsed).replaceFirstChar { it.uppercase() }
        else selectedDate
    } catch (_: Exception) {
        selectedDate
    }

    val menuModules = listOf(
        MenuModule(
            id = "calendar",
            title = "Frekwencja",
            subtitle = "Sprawdzanie obecności",
            icon = Icons.Default.CalendarMonth,
            badge = "Kursy dzienne",
            iconColor = MaterialTheme.colorScheme.primary,
            iconBgColor = MaterialTheme.colorScheme.primaryContainer,
            targetScreen = AppScreen.CALENDAR_COURSE_SELECT,
            testTag = "menu_card_calendar"
        ),
        MenuModule(
            id = "groups",
            title = "Trasy",
            subtitle = "Konfiguracja i przydział",
            icon = Icons.Default.Groups,
            badge = "$routesCount tras",
            iconColor = MaterialTheme.colorScheme.secondary,
            iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
            targetScreen = AppScreen.GROUPS_LIST,
            testTag = "menu_card_groups"
        ),
        MenuModule(
            id = "students",
            title = "Lista",
            subtitle = "Katalog i historia",
            icon = Icons.Default.FormatListBulleted,
            badge = "${students.size} uczniów",
            iconColor = Color(0xFF0284C7),
            iconBgColor = Color(0xFFE0F2FE),
            targetScreen = AppScreen.STUDENT_LIST,
            testTag = "menu_card_students"
        ),
        MenuModule(
            id = "guardian",
            title = "Opiekun",
            subtitle = "Dyżury i nauczyciele",
            icon = Icons.Default.SupervisorAccount,
            badge = if (!guardianName.isNullOrBlank()) "Aktywny" else "Wybierz",
            iconColor = Color(0xFF7C3AED),
            iconBgColor = Color(0xFFEDE9FE),
            targetScreen = AppScreen.GUARDIAN,
            testTag = "menu_card_guardians"
        ),
        MenuModule(
            id = "import_export",
            title = "Import-Eksport",
            subtitle = "Kopia bazy JSON",
            icon = Icons.Default.FileDownload,
            badge = "Plik JSON",
            iconColor = Color(0xFFD97706),
            iconBgColor = Color(0xFFFEF3C7),
            targetScreen = AppScreen.IMPORT_EXPORT,
            testTag = "menu_card_import_export"
        ),
        MenuModule(
            id = "print",
            title = "Wydruk",
            subtitle = "Raporty i wydruk PDF",
            icon = Icons.Default.Print,
            badge = "Drukuj / PDF",
            iconColor = Color(0xFF059669),
            iconBgColor = Color(0xFFD1FAE5),
            targetScreen = AppScreen.PRINT_REPORT,
            testTag = "menu_card_print"
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("main_menu_grid"),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Vehicle Header Card Span across both columns
        item(span = { GridItemSpan(2) }) {
            VehicleHeaderCard(
                guardianName = guardianName,
                formattedDate = formattedDate,
                studentCount = students.size,
                isDarkTheme = isDarkTheme,
                onSwitchToLightMode = onSwitchToLightMode,
                onSwitchToDarkMode = onSwitchToDarkMode,
                onGuardianClick = { onNavigate(AppScreen.GUARDIAN) }
            )
        }

        // Section Title
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Moduły Aplikacji",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        // 6 Primary Navigation Module Cards
        items(menuModules.size) { index ->
            val module = menuModules[index]
            MainMenuModuleCard(
                module = module,
                onClick = { onNavigate(module.targetScreen) }
            )
        }

        // Bottom space
        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun VehicleHeaderCard(
    guardianName: String?,
    formattedDate: String,
    studentCount: Int,
    isDarkTheme: Boolean = false,
    onSwitchToLightMode: () -> Unit = {},
    onSwitchToDarkMode: () -> Unit = {},
    onGuardianClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("main_menu_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = "Autobus",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Dowóz-odwóz",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Sun Icon (Light Mode)
                            IconButton(
                                onClick = onSwitchToLightMode,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("theme_switch_light")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (!isDarkTheme) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.LightMode,
                                            contentDescription = "Włącz tryb jasny (słońce)",
                                            tint = if (!isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }

                            // Crescent Moon Icon (Dark Mode)
                            IconButton(
                                onClick = onSwitchToDarkMode,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("theme_switch_dark")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDarkTheme) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.DarkMode,
                                            contentDescription = "Włącz tryb ciemny (półksiężyc)",
                                            tint = if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Guardian Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onGuardianClick),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Aktywny opiekun dyżurny:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = guardianName ?: "Brak opiekuna (kliknij, aby wybrać)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Text(
                        text = "Zmień",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun MainMenuModuleCard(
    module: MenuModule,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clickable(onClick = onClick)
            .testTag(module.testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = module.iconBgColor,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = module.icon,
                        contentDescription = module.title,
                        tint = module.iconColor,
                        modifier = Modifier.padding(11.dp)
                    )
                }

                if (module.badge != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = module.iconBgColor
                    ) {
                        Text(
                            text = module.badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = module.iconColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = module.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = module.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}
