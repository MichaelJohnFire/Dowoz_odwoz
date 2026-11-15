package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.CourseType
import com.example.model.RouteConfig
import com.example.ui.components.OnboardingGuardianDialog
import com.example.ui.screens.AttendanceChecklistScreen
import com.example.ui.screens.CalendarCourseSelectScreen
import com.example.ui.screens.GroupDetailsScreen
import com.example.ui.screens.GroupsListScreen
import com.example.ui.screens.GuardianScreen
import com.example.ui.screens.ImportExportScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.PrintReportScreen
import com.example.ui.screens.StudentListScreen
import com.example.ui.theme.DowozOdwozTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BusViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BusViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = isDarkMode ?: systemDark

            DowozOdwozTheme(darkTheme = effectiveDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DowozOdwozApp(
                        viewModel = viewModel,
                        isDarkTheme = effectiveDark
                    )
                }
            }
        }
    }
}

@Composable
fun DowozOdwozApp(
    viewModel: BusViewModel = viewModel(),
    isDarkTheme: Boolean = false
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val activeGuardianName by viewModel.activeGuardianName.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val guardians by viewModel.guardians.collectAsStateWithLifecycle()
    val routeConfigs by viewModel.routeConfigs.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedCourse by viewModel.selectedCourse.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val historyStudent by viewModel.historyStudent.collectAsStateWithLifecycle()
    val studentHistoryItems by viewModel.studentHistoryItems.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                AppScreen.MAIN_MENU -> {
                    MainMenuScreen(
                        guardianName = activeGuardianName,
                        students = students,
                        selectedDate = selectedDate,
                        routesCount = routeConfigs.size,
                        isDarkTheme = isDarkTheme,
                        onSwitchToLightMode = { viewModel.setDarkMode(false) },
                        onSwitchToDarkMode = { viewModel.setDarkMode(true) },
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                }
                AppScreen.CALENDAR_COURSE_SELECT -> {
                    CalendarCourseSelectScreen(
                        selectedDate = selectedDate,
                        guardianName = activeGuardianName,
                        routeConfigs = routeConfigs,
                        allStudents = students,
                        onDateShift = { viewModel.shiftDate(it) },
                        onDateSelect = { viewModel.setDate(it) },
                        onCourseSelect = { viewModel.selectCourseAndOpenChecklist(it) },
                        onRouteSelect = { viewModel.selectRouteAndOpenChecklist(it.id) },
                        onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                    )
                }
                AppScreen.ATTENDANCE_CHECKLIST -> {
                    AttendanceChecklistScreen(
                        courseType = selectedCourse,
                        selectedDate = selectedDate,
                        currentSession = currentSession,
                        allStudents = students,
                        routeConfigs = routeConfigs,
                        guardianName = activeGuardianName,
                        onCycleAttendance = { viewModel.cycleStudentAttendance(it) },
                        onSetAttendance = { id, status -> viewModel.setStudentAttendance(id, status) },
                        onMarkAll = { viewModel.markAllStudents(it) },
                        onResetAttendance = { viewModel.resetSessionAttendance() },
                        onAddNewStudentOnTheFly = { firstName, studentClass, town ->
                            viewModel.addNewStudentOnTheFly(firstName, studentClass, town)
                        },
                        onUpdateScheduledTime = { viewModel.updateScheduledTimeForCurrentRoute(it) },
                        onBack = { viewModel.navigateTo(AppScreen.CALENDAR_COURSE_SELECT) }
                    )
                }
                AppScreen.GROUPS_LIST -> {
                    GroupsListScreen(
                        routeConfigs = routeConfigs,
                        guardianName = activeGuardianName,
                        onGroupClick = { viewModel.openGroupDetails(it) },
                        onAddNewGroup = { name, time -> viewModel.addNewGroup(name, time) },
                        onDeleteGroup = { viewModel.deleteGroup(it) },
                        onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                    )
                }
                AppScreen.GROUP_DETAILS -> {
                    val currentRoute = routeConfigs.find { it.id == selectedGroupId }
                        ?: routeConfigs.firstOrNull()
                        ?: RouteConfig(courseType = CourseType.DOWOZ_PKS, scheduledTime = "06:50")

                    GroupDetailsScreen(
                        route = currentRoute,
                        allStudents = students,
                        guardianName = activeGuardianName,
                        onUpdateRouteName = { viewModel.updateRouteName(currentRoute.id, it) },
                        onUpdateTime = { viewModel.updateGroupTime(currentRoute.id, it) },
                        onRemoveStudent = { viewModel.removeStudentFromGroup(currentRoute.id, it) },
                        onAddStudentOnTheFly = { firstName, studentClass, town ->
                            viewModel.addStudentToGroupOnTheFly(currentRoute.id, firstName, studentClass, town)
                        },
                        onAddExistingStudent = { viewModel.addExistingStudentToGroup(currentRoute.id, it) },
                        onUpdateStudent = { id, f, c, t -> viewModel.updateStudentDetails(id, f, c, t) },
                        onBack = { viewModel.navigateTo(AppScreen.GROUPS_LIST) }
                    )
                }
                AppScreen.STUDENT_LIST -> {
                    StudentListScreen(
                        students = students,
                        guardianName = activeGuardianName,
                        historyStudent = historyStudent,
                        studentHistoryItems = studentHistoryItems,
                        onAddStudent = { firstName, studentClass, town ->
                            viewModel.addStudent(firstName, studentClass, town)
                        },
                        onUpdateStudent = { id, f, c, t ->
                            viewModel.updateStudentDetails(id, f, c, t)
                        },
                        onDeleteStudent = { viewModel.deleteStudent(it) },
                        onOpenStudentHistory = { viewModel.openStudentHistory(it) },
                        onCloseStudentHistory = { viewModel.closeStudentHistory() },
                        onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                    )
                }
                AppScreen.GUARDIAN -> {
                    GuardianScreen(
                        guardians = guardians,
                        activeGuardianName = activeGuardianName,
                        onAddGuardian = { viewModel.addGuardian(it) },
                        onSetActiveGuardian = { viewModel.setActiveGuardian(it) },
                        onDeleteGuardian = { viewModel.deleteGuardian(it) },
                        onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                    )
                }
                AppScreen.IMPORT_EXPORT -> {
                    ImportExportScreen(
                        viewModel = viewModel,
                        guardianName = activeGuardianName,
                        onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                    )
                }
                AppScreen.PRINT_REPORT -> {
                    PrintReportScreen(
                        viewModel = viewModel,
                        guardianName = activeGuardianName,
                        onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                    )
                }
            }
        }

        // First-launch un-dismissable onboarding modal
        if (!isOnboardingCompleted) {
            OnboardingGuardianDialog(
                onConfirm = { fullName ->
                    viewModel.completeOnboarding(fullName)
                }
            )
        }
    }
}
