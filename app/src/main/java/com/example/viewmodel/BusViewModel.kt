package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BusDatabase
import com.example.data.BusRepository
import com.example.data.PreferencesManager
import com.example.model.AttendanceRecord
import com.example.model.AttendanceSession
import com.example.model.AttendanceStatus
import com.example.model.CourseType
import com.example.model.Guardian
import com.example.model.ImportDialogState
import com.example.util.DailyAttendanceSummary
import com.example.util.ReportPrintData
import com.example.util.StudentReportItem
import com.example.model.ImportStrategy
import com.example.model.ParsedBackup
import com.example.model.ParsedRoute
import com.example.model.ParsedStudent
import com.example.model.RouteConfig
import com.example.model.Student
import com.example.model.StudentHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AppScreen {
    MAIN_MENU,
    CALENDAR_COURSE_SELECT,
    ATTENDANCE_CHECKLIST,
    GROUPS_LIST,
    GROUP_DETAILS,
    STUDENT_LIST,
    GUARDIAN,
    IMPORT_EXPORT,
    PRINT_REPORT
}

class BusViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BusDatabase.getInstance(application)
    private val prefs = PreferencesManager(application)
    private val repository = BusRepository(db.busDao(), prefs)

    // Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Selected Group ID for Screen B
    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    // Onboarding State
    private val _isOnboardingCompleted = MutableStateFlow(repository.isOnboardingCompleted())
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    // Active Guardian
    private val _activeGuardianName = MutableStateFlow(repository.getActiveGuardianName())
    val activeGuardianName: StateFlow<String?> = _activeGuardianName.asStateFlow()

    // Dark Mode Theme State (null = follow system)
    private val _isDarkMode = MutableStateFlow<Boolean?>(prefs.isDarkMode())
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    fun setDarkMode(isDark: Boolean) {
        prefs.setDarkMode(isDark)
        _isDarkMode.value = isDark
    }

    // Data Flows
    val students: StateFlow<List<Student>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val guardians: StateFlow<List<Guardian>> = repository.allGuardians
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routeConfigs: StateFlow<List<RouteConfig>> = repository.allRouteConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calendar & Attendance State
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val _selectedDate = MutableStateFlow(dateFormat.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedCourse = MutableStateFlow(CourseType.DOWOZ_PKS)
    val selectedCourse: StateFlow<CourseType> = _selectedCourse.asStateFlow()

    private val _selectedRouteId = MutableStateFlow(CourseType.DOWOZ_PKS.name)
    val selectedRouteId: StateFlow<String> = _selectedRouteId.asStateFlow()

    private val _currentSession = MutableStateFlow<AttendanceSession?>(null)
    val currentSession: StateFlow<AttendanceSession?> = _currentSession.asStateFlow()

    // Student History Modal State
    private val _historyStudent = MutableStateFlow<Student?>(null)
    val historyStudent: StateFlow<Student?> = _historyStudent.asStateFlow()

    private val _studentHistoryItems = MutableStateFlow<List<StudentHistoryItem>>(emptyList())
    val studentHistoryItems: StateFlow<List<StudentHistoryItem>> = _studentHistoryItems.asStateFlow()

    // Toast / Feedback message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Import confirmation dialog state
    private val _importDialogState = MutableStateFlow<ImportDialogState>(ImportDialogState.None)
    val importDialogState: StateFlow<ImportDialogState> = _importDialogState.asStateFlow()

    init {
        // Load initial session if onboarding is completed
        loadSessionForCurrentSelection()
        viewModelScope.launch {
            repository.ensureDefaultRoutesInitialized()
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen == AppScreen.CALENDAR_COURSE_SELECT || screen == AppScreen.ATTENDANCE_CHECKLIST) {
            loadSessionForCurrentSelection()
        }
    }

    fun selectRouteAndOpenChecklist(routeId: String) {
        _selectedRouteId.value = routeId
        val predefined = CourseType.entries.find { it.name == routeId }
        if (predefined != null) {
            _selectedCourse.value = predefined
        }
        viewModelScope.launch {
            val date = _selectedDate.value
            val session = repository.getOrCreateSession(date, routeId)
            _currentSession.value = session
            _currentScreen.value = AppScreen.ATTENDANCE_CHECKLIST
        }
    }

    fun selectCourseAndOpenChecklist(courseType: CourseType) {
        selectRouteAndOpenChecklist(courseType.name)
    }

    fun completeOnboarding(fullName: String) {
        if (fullName.isBlank()) return
        viewModelScope.launch {
            val guardian = repository.saveInitialGuardian(fullName)
            _activeGuardianName.value = guardian.fullName
            _isOnboardingCompleted.value = true
            loadSessionForCurrentSelection()
        }
    }

    fun setDate(dateString: String) {
        _selectedDate.value = dateString
        loadSessionForCurrentSelection()
    }

    fun shiftDate(days: Int) {
        try {
            val date = dateFormat.parse(_selectedDate.value) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.DAY_OF_YEAR, days)
            _selectedDate.value = dateFormat.format(cal.time)
            loadSessionForCurrentSelection()
        } catch (_: Exception) {}
    }

    fun setCourse(courseType: CourseType) {
        _selectedCourse.value = courseType
        loadSessionForCurrentSelection()
    }

    fun loadSessionForCurrentSelection() {
        viewModelScope.launch {
            val date = _selectedDate.value
            val routeId = _selectedRouteId.value
            val session = repository.getOrCreateSession(date, routeId)
            _currentSession.value = session
        }
    }

    // Toggle / Cycle attendance status
    fun cycleStudentAttendance(studentId: String) {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            repository.cycleStudentAttendance(session.id, studentId)
            loadSessionForCurrentSelection()
        }
    }

    fun setStudentAttendance(studentId: String, isPresent: Boolean) {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            repository.setStudentAttendance(session.id, studentId, isPresent)
            loadSessionForCurrentSelection()
        }
    }

    fun markAllStudents(isPresent: Boolean) {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            val config = repository.getRouteConfigById(session.routeId)
            val sessionStudentIds = session.records.map { it.studentId }.toSet()
            val allTargetIds = (config.assignedStudentIds + sessionStudentIds).toList()

            repository.markAllAttendance(session.id, allTargetIds, isPresent)
            loadSessionForCurrentSelection()
        }
    }

    fun resetSessionAttendance() {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            repository.resetSessionAttendance(session.id)
            loadSessionForCurrentSelection()
            _statusMessage.value = "Zresetowano listę do stanu początkowego (niesprawdzone)"
        }
    }

    fun addNewStudentOnTheFly(firstName: String, studentClass: String, town: String) {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            val created = repository.addStudentToRouteOnTheFly(
                routeId = session.routeId,
                firstName = firstName,
                studentClass = studentClass,
                town = town
            )
            repository.setStudentAttendance(session.id, created.id, true)
            loadSessionForCurrentSelection()
            _statusMessage.value = "Dodano i oznaczono obecnego: ${created.firstName}"
        }
    }

    fun addStudentToSession(studentId: String) {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            repository.setStudentAttendance(session.id, studentId, true)
            loadSessionForCurrentSelection()
        }
    }

    fun updateSessionNotes(notes: String) {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            repository.updateSessionNotes(session.id, notes)
            _currentSession.value = _currentSession.value?.copy(notes = notes)
        }
    }

    fun updateScheduledTimeForCurrentRoute(time: String) {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            val config = repository.getRouteConfigById(session.routeId)
            repository.saveRouteConfig(config.copy(scheduledTime = time))
            _statusMessage.value = "Zmieniono czas odjazdu: $time"
        }
    }

    // --- Groups / Routes Module Actions ---
    fun openGroupDetails(groupId: String) {
        _selectedGroupId.value = groupId
        _currentScreen.value = AppScreen.GROUP_DETAILS
    }

    fun addNewGroup(name: String, scheduledTime: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addNewRoute(name, scheduledTime)
            _statusMessage.value = "Dodano nową trasę: $name"
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            repository.deleteRoute(groupId)
            if (_selectedGroupId.value == groupId) {
                _selectedGroupId.value = null
                _currentScreen.value = AppScreen.GROUPS_LIST
            }
            if (_selectedRouteId.value == groupId) {
                val remaining = repository.getAllRouteConfigsStatic()
                _selectedRouteId.value = remaining.firstOrNull()?.id ?: CourseType.DOWOZ_PKS.name
            }
            _statusMessage.value = "Usunięto trasę"
        }
    }

    fun updateGroupTime(groupId: String, newTime: String) {
        viewModelScope.launch {
            val config = repository.getRouteConfigById(groupId)
            repository.saveRouteConfig(config.copy(scheduledTime = newTime))
            _statusMessage.value = "Zmieniono czas odjazdu: $newTime"
        }
    }

    fun updateRouteName(groupId: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updateRouteName(groupId, newName)
            loadSessionForCurrentSelection()
            _statusMessage.value = "Zmieniono nazwę trasy na: $newName"
        }
    }

    fun removeStudentFromGroup(groupId: String, studentId: String) {
        viewModelScope.launch {
            repository.removeStudentFromRoute(groupId, studentId)
            loadSessionForCurrentSelection()
            _statusMessage.value = "Usunięto ucznia z tej trasy"
        }
    }

    fun addExistingStudentToGroup(groupId: String, studentId: String) {
        viewModelScope.launch {
            repository.addStudentToRoute(groupId, studentId)
            loadSessionForCurrentSelection()
            _statusMessage.value = "Przypisano ucznia do trasy"
        }
    }

    fun addStudentToGroupOnTheFly(groupId: String, firstName: String, studentClass: String, town: String) {
        viewModelScope.launch {
            val created = repository.addStudentToRouteOnTheFly(groupId, firstName, studentClass, town)
            loadSessionForCurrentSelection()
            _statusMessage.value = "Dodano ucznia ${created.firstName} do trasy"
        }
    }

    fun updateStudentDetails(studentId: String, firstName: String, studentClass: String, town: String) {
        viewModelScope.launch {
            repository.updateStudent(studentId, firstName, studentClass, town)
            _statusMessage.value = "Zaktualizowano dane ucznia: $firstName"
        }
    }

    // --- Student Directory Actions ---
    fun addStudent(firstName: String, studentClass: String, town: String) {
        viewModelScope.launch {
            val student = Student(
                firstName = firstName,
                studentClass = studentClass,
                town = town
            )
            repository.insertStudent(student)
            _statusMessage.value = "Dodano ucznia: ${student.firstName}"
        }
    }

    fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            repository.deleteStudent(studentId)
            _statusMessage.value = "Usunięto ucznia"
            loadSessionForCurrentSelection()
        }
    }

    fun openStudentHistory(student: Student) {
        _historyStudent.value = student
        viewModelScope.launch {
            val history = repository.getStudentHistory(student.id)
            _studentHistoryItems.value = history
        }
    }

    fun closeStudentHistory() {
        _historyStudent.value = null
        _studentHistoryItems.value = emptyList()
    }

    // --- Guardian Actions ---
    fun addGuardian(fullName: String) {
        if (fullName.isBlank()) return
        viewModelScope.launch {
            val guardian = Guardian(fullName = fullName.trim())
            repository.insertGuardian(guardian)
            _statusMessage.value = "Dodano opiekuna: ${guardian.fullName}"
        }
    }

    fun setActiveGuardian(guardian: Guardian) {
        repository.setActiveGuardian(guardian)
        _activeGuardianName.value = guardian.fullName
        _statusMessage.value = "Aktywny opiekun: ${guardian.fullName}"
        loadSessionForCurrentSelection()
    }

    fun deleteGuardian(guardianId: String) {
        viewModelScope.launch {
            repository.deleteGuardian(guardianId)
            _activeGuardianName.value = repository.getActiveGuardianName()
            if (_activeGuardianName.value == null) {
                _isOnboardingCompleted.value = false
            }
            _statusMessage.value = "Usunięto opiekuna"
        }
    }

    // --- Import / Export ---

    suspend fun getExportStudents(): Pair<String, String> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "uczniowie_lista_${timeStamp}.json"
        val content = repository.exportStudentsListJson()
        return Pair(fileName, content)
    }

    suspend fun getExportRoutes(): Pair<String, String> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "trasy_i_grupy_${timeStamp}.json"
        val content = repository.exportRoutesAndGroupsJson()
        return Pair(fileName, content)
    }

    suspend fun getExportFullBackup(): Pair<String, String> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "kopia_pelna_dowoz_odwoz_${timeStamp}.json"
        val content = repository.exportFullBackupJson()
        return Pair(fileName, content)
    }

    fun handleStudentFileContent(jsonText: String) {
        val result = repository.validateStudentsJson(jsonText)
        if (result.isSuccess) {
            _importDialogState.value = ImportDialogState.StudentsPending(result.getOrThrow(), jsonText)
        } else {
            _importDialogState.value = ImportDialogState.Error(
                title = "Błąd walidacji listy uczniów",
                message = result.exceptionOrNull()?.localizedMessage ?: "Nieprawidłowy format pliku JSON."
            )
        }
    }

    fun handleRoutesFileContent(jsonText: String) {
        val result = repository.validateRoutesJson(jsonText)
        if (result.isSuccess) {
            _importDialogState.value = ImportDialogState.RoutesPending(result.getOrThrow(), jsonText)
        } else {
            _importDialogState.value = ImportDialogState.Error(
                title = "Błąd walidacji tras",
                message = result.exceptionOrNull()?.localizedMessage ?: "Nieprawidłowy format pliku JSON tras."
            )
        }
    }

    fun handleFullBackupFileContent(jsonText: String) {
        val result = repository.validateFullBackupJson(jsonText)
        if (result.isSuccess) {
            _importDialogState.value = ImportDialogState.FullBackupPending(result.getOrThrow(), jsonText)
        } else {
            _importDialogState.value = ImportDialogState.Error(
                title = "Błąd walidacji kopii zapasowej",
                message = result.exceptionOrNull()?.localizedMessage ?: "Nieprawidłowy format pliku kopii zapasowej."
            )
        }
    }

    fun executeStudentImport(students: List<ParsedStudent>, strategy: ImportStrategy) {
        viewModelScope.launch {
            val result = repository.importStudentsList(students, strategy)
            if (result.isSuccess) {
                _statusMessage.value = result.getOrThrow()
                loadSessionForCurrentSelection()
            } else {
                _statusMessage.value = "Błąd importu: ${result.exceptionOrNull()?.localizedMessage}"
            }
            _importDialogState.value = ImportDialogState.None
        }
    }

    fun executeRoutesImport(routes: List<ParsedRoute>, strategy: ImportStrategy) {
        viewModelScope.launch {
            val result = repository.importRoutesAndGroups(routes, strategy)
            if (result.isSuccess) {
                _statusMessage.value = result.getOrThrow()
                loadSessionForCurrentSelection()
            } else {
                _statusMessage.value = "Błąd importu tras: ${result.exceptionOrNull()?.localizedMessage}"
            }
            _importDialogState.value = ImportDialogState.None
        }
    }

    fun executeFullBackupImport(jsonText: String, strategy: ImportStrategy) {
        viewModelScope.launch {
            val result = repository.importFullBackup(jsonText, strategy)
            if (result.isSuccess) {
                _statusMessage.value = result.getOrThrow()
                _activeGuardianName.value = repository.getActiveGuardianName()
                if (_activeGuardianName.value != null) {
                    _isOnboardingCompleted.value = true
                }
                loadSessionForCurrentSelection()
            } else {
                _statusMessage.value = "Błąd przywracania kopii: ${result.exceptionOrNull()?.localizedMessage}"
            }
            _importDialogState.value = ImportDialogState.None
        }
    }

    fun dismissImportDialog() {
        _importDialogState.value = ImportDialogState.None
    }

    suspend fun exportJson(): String {
        return repository.exportDataToJson()
    }

    fun importJson(json: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.importDataFromJson(json)
            if (result.isSuccess) {
                _activeGuardianName.value = repository.getActiveGuardianName()
                if (_activeGuardianName.value != null) {
                    _isOnboardingCompleted.value = true
                }
                loadSessionForCurrentSelection()
                onResult(true, result.getOrDefault("Import zakończony sukcesem."))
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Błąd formatu JSON.")
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun getWeekRangeForDate(dateStr: String): WeekRange {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance(Locale.getDefault())
        try {
            cal.time = sdf.parse(dateStr) ?: Date()
        } catch (e: Exception) {
            cal.time = Date()
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysSinceMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)

        val weekDates = mutableListOf<String>()
        for (i in 0 until 5) {
            weekDates.add(sdf.format(cal.time))
            if (i < 4) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        val mon = weekDates.first()
        val fri = weekDates.last()
        return WeekRange(
            mondayDate = mon,
            fridayDate = fri,
            dates = weekDates,
            displayRange = "$mon – $fri (Pon – Pt)"
        )
    }

    suspend fun preparePdfReportData(
        date: String,
        routeConfig: RouteConfig,
        period: ReportPeriod = ReportPeriod.SINGLE_DAY
    ): ReportPrintData {
        val allStudentsList = repository.getAllStudentsStatic()
        val assignedIds = routeConfig.assignedStudentIds

        if (period == ReportPeriod.SINGLE_DAY) {
            val session = repository.getOrCreateSession(date, routeConfig.courseType)
            val recordMap = session.records.associateBy { it.studentId }
            val displayIds = (assignedIds + recordMap.keys).toSet()

            val relevantStudents = allStudentsList
                .filter { displayIds.contains(it.id) }
                .sortedWith(compareBy({ it.studentClass }, { it.firstName }))

            val studentReportItems = relevantStudents.mapIndexed { index, student ->
                val record = recordMap[student.id]
                val status = when (record?.isPresent) {
                    true -> AttendanceStatus.PRESENT
                    false -> AttendanceStatus.ABSENT
                    null -> AttendanceStatus.UNCHECKED
                }
                StudentReportItem(
                    number = index + 1,
                    firstName = student.firstName,
                    studentClass = student.studentClass,
                    town = student.town,
                    status = status
                )
            }

            val actualGuardian = getActualGuardianFullName(
                session.guardianName.takeIf {
                    !it.isNullOrBlank() && !it.equals("Opiekun dyżurny", ignoreCase = true)
                }
            )

            return ReportPrintData(
                date = date,
                routeName = routeConfig.name,
                scheduledTime = routeConfig.scheduledTime,
                guardianName = actualGuardian,
                students = studentReportItems,
                notes = session.notes.ifBlank { null },
                reportScopeTitle = "RAPORT OBECNOŚCI - DOWÓZ / ODWÓZ",
                dateLabel = "Data zdarzenia:"
            )
        } else {
            val weekRange = getWeekRangeForDate(date)
            val sessions = weekRange.dates.map { day ->
                repository.getOrCreateSession(day, routeConfig.courseType)
            }

            val allRecordedIds = sessions.flatMap { it.records.map { r -> r.studentId } }.toSet()
            val displayIds = (assignedIds + allRecordedIds).toSet()

            val relevantStudents = allStudentsList
                .filter { displayIds.contains(it.id) }
                .sortedWith(compareBy({ it.studentClass }, { it.firstName }))

            val dayNames = listOf("Pn", "Wt", "Śr", "Cz", "Pt")
            val dailySummaries = weekRange.dates.mapIndexed { dayIndex, dayDate ->
                val session = sessions.getOrNull(dayIndex)
                val recMap = session?.records?.associateBy { it.studentId } ?: emptyMap()
                val present = relevantStudents.count { s -> recMap[s.id]?.isPresent == true }
                val absent = relevantStudents.count { s -> recMap[s.id]?.isPresent == false }
                val calendarDay = dayDate.substringAfterLast("-").toIntOrNull() ?: (dayIndex + 1)
                DailyAttendanceSummary(
                    dayNumber = calendarDay,
                    date = dayDate,
                    dayName = dayNames.getOrElse(dayIndex) { "${dayIndex + 1}" },
                    presentCount = present,
                    absentCount = absent
                )
            }

            val studentReportItems = relevantStudents.mapIndexed { index, student ->
                var presentDaysCount = 0
                var absentDaysCount = 0
                var recordedDaysCount = 0

                val weeklyStatuses = sessions.map { s ->
                    val rec = s.records.find { it.studentId == student.id }
                    if (rec != null) {
                        recordedDaysCount++
                        if (rec.isPresent) presentDaysCount++ else absentDaysCount++
                    }
                    when (rec?.isPresent) {
                        true -> AttendanceStatus.PRESENT
                        false -> AttendanceStatus.ABSENT
                        null -> AttendanceStatus.UNCHECKED
                    }
                }
                val weeklySigns = weeklyStatuses.map { st ->
                    if (st == AttendanceStatus.PRESENT) "+" else "-"
                }

                val status: AttendanceStatus
                val statusText: String

                if (recordedDaysCount == 0) {
                    status = AttendanceStatus.UNCHECKED
                    statusText = "[?] NIESPRAWDZONY"
                } else {
                    val pct = (presentDaysCount * 100) / recordedDaysCount
                    status = if (presentDaysCount >= absentDaysCount) AttendanceStatus.PRESENT else AttendanceStatus.ABSENT
                    statusText = "Obecny: $presentDaysCount/$recordedDaysCount dni ($pct%)"
                }

                StudentReportItem(
                    number = index + 1,
                    firstName = student.firstName,
                    studentClass = student.studentClass,
                    town = student.town,
                    status = status,
                    statusText = statusText,
                    weeklyDayStatuses = weeklyStatuses,
                    weeklyDaySigns = weeklySigns
                )
            }

            // Next to the "guardian" label, display the actual guardian's full name instead of "Opiekun dyżurny"
            val actualGuardian = getActualGuardianFullName(
                sessions.firstOrNull {
                    !it.guardianName.isNullOrBlank() && !it.guardianName.equals("Opiekun dyżurny", ignoreCase = true)
                }?.guardianName
            )

            return ReportPrintData(
                date = "${weekRange.mondayDate} – ${weekRange.fridayDate} (Poniedziałek – Piątek)",
                routeName = routeConfig.name,
                scheduledTime = routeConfig.scheduledTime,
                guardianName = actualGuardian,
                students = studentReportItems,
                notes = "Raport zbiorczy za okres tygodnia: ${weekRange.mondayDate} do ${weekRange.fridayDate}",
                reportScopeTitle = "TYGODNIOWY RAPORT OBECNOŚCI - DOWÓZ / ODWÓZ",
                dateLabel = "Zakres tygodnia:",
                dailySummaries = dailySummaries
            )
        }
    }

    suspend fun getActualGuardianFullName(preferredName: String? = null): String {
        val pref = preferredName?.trim()
        if (!pref.isNullOrBlank() && !pref.equals("Opiekun dyżurny", ignoreCase = true)) {
            return pref
        }
        val active = _activeGuardianName.value?.trim()
        if (!active.isNullOrBlank() && !active.equals("Opiekun dyżurny", ignoreCase = true)) {
            return active
        }
        val fromRepoPrefs = repository.getActiveGuardianName()?.trim()
        if (!fromRepoPrefs.isNullOrBlank() && !fromRepoPrefs.equals("Opiekun dyżurny", ignoreCase = true)) {
            return fromRepoPrefs
        }
        val firstFromDb = repository.getAllGuardiansStatic().firstOrNull {
            it.fullName.isNotBlank() && !it.fullName.equals("Opiekun dyżurny", ignoreCase = true)
        }?.fullName?.trim()
        if (!firstFromDb.isNullOrBlank()) {
            return firstFromDb
        }
        return ""
    }
}

enum class ReportPeriod {
    SINGLE_DAY,
    ENTIRE_WEEK
}

data class WeekRange(
    val mondayDate: String,
    val fridayDate: String,
    val dates: List<String>,
    val displayRange: String
)
