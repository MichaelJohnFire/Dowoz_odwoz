package com.example.data

import com.example.model.AttendanceRecord
import com.example.model.AttendanceSession
import com.example.model.CourseType
import com.example.model.Guardian
import com.example.model.ImportStrategy
import com.example.model.ParsedBackup
import com.example.model.ParsedRoute
import com.example.model.ParsedStudent
import com.example.model.RouteConfig
import com.example.model.Student
import com.example.model.StudentHistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BusRepository(
    private val dao: BusDao,
    private val preferences: PreferencesManager
) {

    // --- Onboarding & Active Guardian ---
    fun isOnboardingCompleted(): Boolean = preferences.isOnboardingCompleted()

    fun getActiveGuardianName(): String? = preferences.getActiveGuardianName()

    fun getActiveGuardianId(): String? = preferences.getActiveGuardianId()

    fun setActiveGuardian(guardian: Guardian) {
        preferences.setActiveGuardian(guardian.id, guardian.fullName)
    }

    suspend fun saveInitialGuardian(fullName: String): Guardian {
        val trimmed = fullName.trim()
        val guardian = Guardian(id = UUID.randomUUID().toString(), fullName = trimmed)
        dao.insertGuardian(GuardianEntity(id = guardian.id, fullName = guardian.fullName))
        setActiveGuardian(guardian)
        return guardian
    }

    // --- Students ---
    val allStudents: Flow<List<Student>> = dao.getAllStudents().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun getAllStudentsStatic(): List<Student> {
        return dao.getAllStudentsStatic().map { it.toModel() }
    }

    suspend fun insertStudent(student: Student) {
        dao.insertStudent(
            StudentEntity(
                id = student.id,
                firstName = student.firstName.trim(),
                studentClass = student.studentClass.trim(),
                town = student.town.trim()
            )
        )
    }

    suspend fun deleteStudent(studentId: String) {
        dao.deleteStudent(studentId)
        // Also remove from route configs
        val configs = CourseType.entries.map { getRouteConfig(it) }
        configs.forEach { config ->
            if (config.assignedStudentIds.contains(studentId)) {
                val updated = config.assignedStudentIds - studentId
                saveRouteConfig(config.copy(assignedStudentIds = updated))
            }
        }
    }

    suspend fun getStudentHistory(studentId: String): List<StudentHistoryItem> {
        val records = dao.getRecordsForStudent(studentId)
        val history = mutableListOf<StudentHistoryItem>()
        records.forEach { record ->
            // Find session for record
            val allSessions = dao.getAllSessionsFlow()
            val session = dao.getSessionById(record.sessionId)
            if (session != null) {
                history.add(
                    StudentHistoryItem(
                        date = session.date,
                        courseType = CourseType.fromString(session.courseType),
                        guardianName = session.guardianName,
                        isPresent = record.isPresent,
                        notes = session.notes
                    )
                )
            }
        }
        return history.sortedByDescending { it.date }
    }

    // --- Guardians ---
    val allGuardians: Flow<List<Guardian>> = dao.getAllGuardians().map { entities ->
        entities.map { Guardian(it.id, it.fullName) }
    }

    suspend fun getAllGuardiansStatic(): List<Guardian> {
        return dao.getAllGuardiansStatic().map { Guardian(it.id, it.fullName) }
    }

    suspend fun insertGuardian(guardian: Guardian) {
        dao.insertGuardian(GuardianEntity(guardian.id, guardian.fullName.trim()))
    }

    suspend fun deleteGuardian(guardianId: String) {
        dao.deleteGuardian(guardianId)
        if (preferences.getActiveGuardianId() == guardianId) {
            preferences.clear()
        }
    }

    // --- Route Configs ---
    suspend fun ensureDefaultRoutesInitialized() {
        if (!preferences.areRoutesInitialized()) {
            val existing = dao.getAllRouteConfigsStatic()
            if (existing.isEmpty()) {
                val defaults = CourseType.entries.map { course ->
                    RouteConfigEntity(
                        courseType = course.name,
                        scheduledTime = course.defaultTime,
                        assignedStudentIdsJson = "[]",
                        customName = course.displayName
                    )
                }
                dao.insertRouteConfigs(defaults)
            }
            preferences.setRoutesInitialized(true)
        }
    }

    val allRouteConfigs: Flow<List<RouteConfig>> = dao.getAllRouteConfigs().map { entities ->
        if (entities.isEmpty() && !preferences.areRoutesInitialized()) {
            CourseType.entries.map { course ->
                RouteConfig(
                    id = course.name,
                    name = course.displayName,
                    courseType = course,
                    scheduledTime = course.defaultTime,
                    assignedStudentIds = emptySet(),
                    isCustom = false
                )
            }
        } else {
            entities.map { entity ->
                val predefined = CourseType.entries.find { it.name == entity.courseType }
                RouteConfig(
                    id = entity.courseType,
                    name = entity.customName ?: predefined?.displayName ?: entity.courseType,
                    courseType = predefined ?: CourseType.DOWOZ_GIMBUS,
                    scheduledTime = entity.scheduledTime,
                    assignedStudentIds = parseIdsJson(entity.assignedStudentIdsJson),
                    isCustom = predefined == null
                )
            }
        }
    }

    suspend fun getRouteConfig(courseType: CourseType): RouteConfig {
        return getRouteConfigById(courseType.name)
    }

    suspend fun getRouteConfigById(routeId: String): RouteConfig {
        val entity = dao.getRouteConfig(routeId)
        val predefined = CourseType.entries.find { it.name == routeId }
        return if (entity != null) {
            RouteConfig(
                id = entity.courseType,
                name = entity.customName ?: predefined?.displayName ?: entity.courseType,
                courseType = predefined ?: CourseType.DOWOZ_GIMBUS,
                scheduledTime = entity.scheduledTime,
                assignedStudentIds = parseIdsJson(entity.assignedStudentIdsJson),
                isCustom = predefined == null
            )
        } else {
            RouteConfig(
                id = predefined?.name ?: routeId,
                name = predefined?.displayName ?: routeId,
                courseType = predefined ?: CourseType.DOWOZ_GIMBUS,
                scheduledTime = predefined?.defaultTime ?: "07:00",
                assignedStudentIds = emptySet(),
                isCustom = predefined == null
            )
        }
    }

    suspend fun saveRouteConfig(config: RouteConfig) {
        val existing = dao.getRouteConfig(config.id)
        dao.insertRouteConfig(
            RouteConfigEntity(
                courseType = config.id,
                scheduledTime = config.scheduledTime,
                assignedStudentIdsJson = serializeIdsJson(config.assignedStudentIds),
                customName = existing?.customName ?: if (config.name != config.courseType.displayName) config.name else null
            )
        )
    }

    suspend fun updateRouteName(routeId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        val existing = dao.getRouteConfig(routeId)
        val predefined = CourseType.entries.find { it.name == routeId }
        if (existing != null) {
            dao.insertRouteConfig(existing.copy(customName = trimmed))
        } else {
            dao.insertRouteConfig(
                RouteConfigEntity(
                    courseType = routeId,
                    scheduledTime = predefined?.defaultTime ?: "07:00",
                    assignedStudentIdsJson = "[]",
                    customName = trimmed
                )
            )
        }
    }

    suspend fun addNewRoute(name: String, time: String): RouteConfig {
        val trimmed = name.trim()
        val id = UUID.randomUUID().toString()
        val newRoute = RouteConfig(
            id = id,
            name = trimmed,
            courseType = CourseType.DOWOZ_GIMBUS,
            scheduledTime = time.trim(),
            assignedStudentIds = emptySet(),
            isCustom = true
        )
        dao.insertRouteConfig(
            RouteConfigEntity(
                courseType = id,
                scheduledTime = newRoute.scheduledTime,
                assignedStudentIdsJson = "[]",
                customName = trimmed
            )
        )
        return newRoute
    }

    suspend fun getAllRouteConfigsStatic(): List<RouteConfig> {
        val entities = dao.getAllRouteConfigsStatic()
        return entities.map { entity ->
            val predefined = CourseType.entries.find { it.name == entity.courseType }
            RouteConfig(
                id = entity.courseType,
                name = entity.customName ?: predefined?.displayName ?: entity.courseType,
                courseType = predefined ?: CourseType.DOWOZ_GIMBUS,
                scheduledTime = entity.scheduledTime,
                assignedStudentIds = parseIdsJson(entity.assignedStudentIdsJson),
                isCustom = predefined == null
            )
        }
    }

    suspend fun deleteRoute(routeId: String) {
        val sessions = dao.getSessionsForCourse(routeId)
        sessions.forEach { s ->
            dao.deleteRecordsForSession(s.id)
        }
        dao.deleteSessionsForCourse(routeId)
        dao.deleteRouteConfig(routeId)
        preferences.setRoutesInitialized(true)
    }

    suspend fun removeStudentFromRoute(routeId: String, studentId: String) {
        val config = getRouteConfigById(routeId)
        val updated = config.assignedStudentIds - studentId
        saveRouteConfig(config.copy(assignedStudentIds = updated))
    }

    suspend fun addStudentToRoute(routeId: String, studentId: String) {
        val config = getRouteConfigById(routeId)
        val updated = config.assignedStudentIds + studentId
        saveRouteConfig(config.copy(assignedStudentIds = updated))
    }

    suspend fun addStudentToRouteOnTheFly(
        routeId: String,
        firstName: String,
        studentClass: String,
        town: String
    ): Student {
        val student = Student(
            id = UUID.randomUUID().toString(),
            firstName = firstName.trim(),
            studentClass = studentClass.trim(),
            town = town.trim()
        )
        dao.insertStudent(
            StudentEntity(
                id = student.id,
                firstName = student.firstName,
                studentClass = student.studentClass,
                town = student.town
            )
        )
        addStudentToRoute(routeId, student.id)
        return student
    }

    suspend fun updateStudent(studentId: String, firstName: String, studentClass: String, town: String) {
        dao.insertStudent(
            StudentEntity(
                id = studentId,
                firstName = firstName.trim(),
                studentClass = studentClass.trim(),
                town = town.trim()
            )
        )
    }

    // --- Attendance Sessions & Records ---
    suspend fun getOrCreateSession(date: String, routeId: String): AttendanceSession {
        var sessionEntity = dao.getSession(date, routeId)
        val activeGuardianId = preferences.getActiveGuardianId() ?: "default_id"
        val activeGuardianName = preferences.getActiveGuardianName() ?: "Opiekun dyżurny"

        if (sessionEntity == null) {
            val newSession = AttendanceSessionEntity(
                id = UUID.randomUUID().toString(),
                date = date,
                courseType = routeId,
                guardianId = activeGuardianId,
                guardianName = activeGuardianName,
                notes = ""
            )
            dao.insertSession(newSession)
            sessionEntity = newSession

            // Leave records empty so assigned students start as UNCHECKED ⚪
        }

        val records = dao.getRecordsForSession(sessionEntity.id).map { it.toModel() }
        val predefinedCourse = CourseType.entries.find { it.name == sessionEntity.courseType }
        return AttendanceSession(
            id = sessionEntity.id,
            date = sessionEntity.date,
            courseType = predefinedCourse ?: CourseType.fromString(sessionEntity.courseType),
            guardianId = sessionEntity.guardianId,
            guardianName = sessionEntity.guardianName,
            notes = sessionEntity.notes,
            records = records,
            routeId = sessionEntity.courseType
        )
    }

    suspend fun getOrCreateSession(date: String, courseType: CourseType): AttendanceSession {
        return getOrCreateSession(date, courseType.name)
    }

    suspend fun cycleStudentAttendance(sessionId: String, studentId: String) {
        val existingRecords = dao.getRecordsForSession(sessionId)
        val existing = existingRecords.find { it.studentId == studentId }
        when (existing?.isPresent) {
            null -> {
                // 1. Unchecked (?) -> Present (+)
                val record = AttendanceRecordEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    studentId = studentId,
                    isPresent = true,
                    timestamp = System.currentTimeMillis()
                )
                dao.insertRecord(record)
            }
            true -> {
                // 2. Present (+) -> Absent (-)
                val record = AttendanceRecordEntity(
                    id = existing.id,
                    sessionId = sessionId,
                    studentId = studentId,
                    isPresent = false,
                    timestamp = System.currentTimeMillis()
                )
                dao.insertRecord(record)
            }
            false -> {
                // 3. Absent (-) -> Unchecked (?)
                dao.deleteRecordForStudent(sessionId, studentId)
            }
        }
    }

    suspend fun setStudentAttendance(sessionId: String, studentId: String, isPresent: Boolean) {
        val existingRecords = dao.getRecordsForSession(sessionId)
        val existing = existingRecords.find { it.studentId == studentId }
        val record = AttendanceRecordEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            sessionId = sessionId,
            studentId = studentId,
            isPresent = isPresent,
            timestamp = System.currentTimeMillis()
        )
        dao.insertRecord(record)
    }

    suspend fun toggleStudentAttendance(sessionId: String, studentId: String, newStatus: Boolean) {
        setStudentAttendance(sessionId, studentId, newStatus)
    }

    suspend fun resetSessionAttendance(sessionId: String) {
        dao.deleteRecordsForSession(sessionId)
    }

    suspend fun addNewStudentOnTheFly(
        sessionId: String,
        courseType: CourseType,
        firstName: String,
        studentClass: String,
        town: String
    ): Student {
        val student = Student(
            id = UUID.randomUUID().toString(),
            firstName = firstName.trim(),
            studentClass = studentClass.trim(),
            town = town.trim()
        )
        dao.insertStudent(
            StudentEntity(
                id = student.id,
                firstName = student.firstName,
                studentClass = student.studentClass,
                town = student.town
            )
        )
        // Assign to course
        val config = getRouteConfig(courseType)
        saveRouteConfig(config.copy(assignedStudentIds = config.assignedStudentIds + student.id))

        // Mark as Present in current session
        dao.insertRecord(
            AttendanceRecordEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                studentId = student.id,
                isPresent = true,
                timestamp = System.currentTimeMillis()
            )
        )
        return student
    }

    suspend fun markAllAttendance(sessionId: String, studentIds: List<String>, isPresent: Boolean) {
        val now = System.currentTimeMillis()
        val records = studentIds.map { studentId ->
            AttendanceRecordEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                studentId = studentId,
                isPresent = isPresent,
                timestamp = now
            )
        }
        dao.insertRecords(records)
    }

    suspend fun updateSessionNotes(sessionId: String, notes: String) {
        dao.updateSessionNotes(sessionId, notes)
    }

    // --- Specialized JSON Export & Import ---

    suspend fun exportStudentsListJson(): String {
        val root = JSONObject()
        root.put("version", "1.0")
        root.put("export_type", "STUDENTS_LIST")

        val dataArray = JSONArray()
        dao.getAllStudentsStatic().forEach { s ->
            val item = JSONObject()
            item.put("first_name", s.firstName)
            item.put("class", s.studentClass)
            item.put("town", s.town)
            dataArray.put(item)
        }
        root.put("data", dataArray)
        return root.toString(2)
    }

    suspend fun exportRoutesAndGroupsJson(): String {
        val root = JSONObject()
        root.put("version", "1.0")
        root.put("export_type", "ROUTES_AND_GROUPS")

        val dataArray = JSONArray()
        val allStudentsMap = dao.getAllStudentsStatic().associateBy { it.id }
        val allRoutes = dao.getAllRouteConfigsStatic()

        allRoutes.forEach { r ->
            val routeObj = JSONObject()
            val predefined = CourseType.entries.find { it.name == r.courseType }
            val routeName = r.customName ?: predefined?.displayName ?: r.courseType
            routeObj.put("route_id", r.courseType)
            routeObj.put("route_name", routeName)
            routeObj.put("scheduled_time", r.scheduledTime)

            val studentIds = parseIdsJson(r.assignedStudentIdsJson)
            val studentsArr = JSONArray()
            studentIds.forEach { id ->
                val s = allStudentsMap[id]
                if (s != null) {
                    val sObj = JSONObject()
                    sObj.put("first_name", s.firstName)
                    sObj.put("class", s.studentClass)
                    sObj.put("town", s.town)
                    studentsArr.put(sObj)
                }
            }
            routeObj.put("students", studentsArr)
            dataArray.put(routeObj)
        }

        root.put("data", dataArray)
        return root.toString(2)
    }

    suspend fun exportFullBackupJson(): String {
        val root = JSONObject()
        root.put("version", "1.0")
        root.put("export_type", "FULL_BACKUP")
        root.put("app_name", "Dowóz-odwóz")
        root.put("exported_at", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        val dataObj = JSONObject()

        // Students
        val studentsArr = JSONArray()
        val allStudents = dao.getAllStudentsStatic()
        val allStudentsMap = allStudents.associateBy { it.id }
        allStudents.forEach { s ->
            val sObj = JSONObject()
            sObj.put("first_name", s.firstName)
            sObj.put("class", s.studentClass)
            sObj.put("town", s.town)
            studentsArr.put(sObj)
        }
        dataObj.put("students", studentsArr)

        // Routes
        val routesArr = JSONArray()
        val allRoutes = dao.getAllRouteConfigsStatic()
        allRoutes.forEach { r ->
            val routeObj = JSONObject()
            val predefined = CourseType.entries.find { it.name == r.courseType }
            routeObj.put("route_id", r.courseType)
            routeObj.put("route_name", r.customName ?: predefined?.displayName ?: r.courseType)
            routeObj.put("scheduled_time", r.scheduledTime)
            val assignedIds = parseIdsJson(r.assignedStudentIdsJson)
            val assignedStudentsArr = JSONArray()
            assignedIds.forEach { id ->
                val s = allStudentsMap[id]
                if (s != null) {
                    val sObj = JSONObject()
                    sObj.put("first_name", s.firstName)
                    sObj.put("class", s.studentClass)
                    sObj.put("town", s.town)
                    assignedStudentsArr.put(sObj)
                }
            }
            routeObj.put("students", assignedStudentsArr)
            routesArr.put(routeObj)
        }
        dataObj.put("routes", routesArr)

        // Guardians
        val guardiansArr = JSONArray()
        dao.getAllGuardiansStatic().forEach { g ->
            val gObj = JSONObject()
            gObj.put("full_name", g.fullName)
            guardiansArr.put(gObj)
        }
        dataObj.put("guardians", guardiansArr)

        // Attendance Sessions & Records
        val sessionsArr = JSONArray()
        val allSessions = dao.getAllSessionsStatic()
        val allRecords = dao.getAllRecords().groupBy { it.sessionId }

        allSessions.forEach { sess ->
            val sessObj = JSONObject()
            sessObj.put("date", sess.date)
            sessObj.put("course_type", sess.courseType)
            sessObj.put("guardian_name", sess.guardianName)
            sessObj.put("notes", sess.notes)

            val sessRecords = allRecords[sess.id] ?: emptyList()
            val recArr = JSONArray()
            sessRecords.forEach { rec ->
                val s = allStudentsMap[rec.studentId]
                if (s != null) {
                    val recObj = JSONObject()
                    recObj.put("first_name", s.firstName)
                    recObj.put("class", s.studentClass)
                    recObj.put("town", s.town)
                    recObj.put("is_present", rec.isPresent)
                    recArr.put(recObj)
                }
            }
            sessObj.put("records", recArr)
            sessionsArr.put(sessObj)
        }
        dataObj.put("attendance_sessions", sessionsArr)

        root.put("data", dataObj)
        return root.toString(2)
    }

    // --- Validation and Import Logic ---

    fun validateStudentsJson(jsonText: String): Result<List<ParsedStudent>> {
        return try {
            val trimmed = jsonText.trim()
            val list = mutableListOf<ParsedStudent>()

            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    parseStudentFromObject(obj)?.let { list.add(it) }
                }
            } else {
                val root = JSONObject(trimmed)
                val array = when {
                    root.has("data") && root.get("data") is JSONArray -> root.getJSONArray("data")
                    root.has("students") && root.get("students") is JSONArray -> root.getJSONArray("students")
                    root.has("data") && root.getJSONObject("data").has("students") -> root.getJSONObject("data").getJSONArray("students")
                    else -> null
                }
                if (array != null) {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        parseStudentFromObject(obj)?.let { list.add(it) }
                    }
                }
            }

            if (list.isEmpty()) {
                Result.failure(Exception("Nie znaleziono poprawnych danych uczniów w pliku. Upewnij się, że plik zawiera pola 'first_name', 'class', 'town'."))
            } else {
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Błąd składni pliku JSON: ${e.localizedMessage}"))
        }
    }

    private fun parseStudentFromObject(obj: JSONObject): ParsedStudent? {
        val firstName = when {
            obj.has("first_name") -> obj.optString("first_name")
            obj.has("firstName") -> obj.optString("firstName")
            obj.has("name") -> obj.optString("name")
            obj.has("imie") -> obj.optString("imie")
            else -> ""
        }.trim()

        if (firstName.isBlank()) return null

        val studentClass = when {
            obj.has("class") -> obj.optString("class")
            obj.has("studentClass") -> obj.optString("studentClass")
            obj.has("klasa") -> obj.optString("klasa")
            else -> ""
        }.trim()

        val town = when {
            obj.has("town") -> obj.optString("town")
            obj.has("city") -> obj.optString("city")
            obj.has("miejscowosc") -> obj.optString("miejscowosc")
            else -> ""
        }.trim()

        return ParsedStudent(firstName, studentClass, town)
    }

    suspend fun importStudentsList(students: List<ParsedStudent>, strategy: ImportStrategy): Result<String> {
        return try {
            if (strategy == ImportStrategy.OVERWRITE) {
                dao.deleteAllStudents()
                val entities = students.map { s ->
                    StudentEntity(
                        id = UUID.randomUUID().toString(),
                        firstName = s.firstName,
                        studentClass = s.studentClass,
                        town = s.town
                    )
                }
                dao.insertStudents(entities)
                Result.success("Zastąpiono bazę uczniów: pomyślnie wczytano ${entities.size} uczniów.")
            } else {
                val existing = dao.getAllStudentsStatic()
                val existingKeySet = existing.map {
                    "${it.firstName.lowercase().trim()}_${it.studentClass.lowercase().trim()}_${it.town.lowercase().trim()}"
                }.toSet()

                val toInsert = mutableListOf<StudentEntity>()
                var duplicatesCount = 0

                students.forEach { s ->
                    val key = "${s.firstName.lowercase().trim()}_${s.studentClass.lowercase().trim()}_${s.town.lowercase().trim()}"
                    if (!existingKeySet.contains(key)) {
                        toInsert.add(
                            StudentEntity(
                                id = UUID.randomUUID().toString(),
                                firstName = s.firstName,
                                studentClass = s.studentClass,
                                town = s.town
                            )
                        )
                    } else {
                        duplicatesCount++
                    }
                }

                if (toInsert.isNotEmpty()) {
                    dao.insertStudents(toInsert)
                }
                Result.success("Scalono pomyślnie: dodano ${toInsert.size} nowych uczniów (pominięto $duplicatesCount istniejących).")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun validateRoutesJson(jsonText: String): Result<List<ParsedRoute>> {
        return try {
            val trimmed = jsonText.trim()
            val list = mutableListOf<ParsedRoute>()

            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    parseRouteFromObject(obj)?.let { list.add(it) }
                }
            } else {
                val root = JSONObject(trimmed)
                val array = when {
                    root.has("data") && root.get("data") is JSONArray -> root.getJSONArray("data")
                    root.has("routes") && root.get("routes") is JSONArray -> root.getJSONArray("routes")
                    root.has("routeConfigs") && root.get("routeConfigs") is JSONArray -> root.getJSONArray("routeConfigs")
                    root.has("data") && root.getJSONObject("data").has("routes") -> root.getJSONObject("data").getJSONArray("routes")
                    else -> null
                }
                if (array != null) {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        parseRouteFromObject(obj)?.let { list.add(it) }
                    }
                }
            }

            if (list.isEmpty()) {
                Result.failure(Exception("Nie znaleziono poprawnych definicji tras w pliku. Upewnij się, że plik zawiera nazwy tras i godziny odjazdu."))
            } else {
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Błąd składni pliku JSON tras: ${e.localizedMessage}"))
        }
    }

    private fun parseRouteFromObject(obj: JSONObject): ParsedRoute? {
        val routeName = when {
            obj.has("route_name") -> obj.optString("route_name")
            obj.has("name") -> obj.optString("name")
            obj.has("customName") -> obj.optString("customName")
            obj.has("displayName") -> obj.optString("displayName")
            else -> ""
        }.trim()

        val routeId = when {
            obj.has("route_id") -> obj.optString("route_id")
            obj.has("courseType") -> obj.optString("courseType")
            obj.has("id") -> obj.optString("id")
            else -> null
        }?.trim()

        val time = when {
            obj.has("scheduled_time") -> obj.optString("scheduled_time")
            obj.has("scheduledTime") -> obj.optString("scheduledTime")
            obj.has("time") -> obj.optString("time")
            obj.has("godzina") -> obj.optString("godzina")
            else -> "07:00"
        }.trim()

        if (routeName.isBlank() && routeId.isNullOrBlank()) return null
        val finalName = if (routeName.isNotBlank()) routeName else (routeId ?: "Trasa")

        val studentsList = mutableListOf<ParsedStudent>()
        val studentsArr = when {
            obj.has("students") && obj.get("students") is JSONArray -> obj.getJSONArray("students")
            obj.has("assigned_students") && obj.get("assigned_students") is JSONArray -> obj.getJSONArray("assigned_students")
            else -> null
        }

        if (studentsArr != null) {
            for (j in 0 until studentsArr.length()) {
                val sObj = studentsArr.optJSONObject(j)
                if (sObj != null) {
                    parseStudentFromObject(sObj)?.let { studentsList.add(it) }
                }
            }
        }

        return ParsedRoute(
            routeId = routeId,
            routeName = finalName,
            scheduledTime = time,
            students = studentsList
        )
    }

    suspend fun importRoutesAndGroups(routes: List<ParsedRoute>, strategy: ImportStrategy): Result<String> {
        return try {
            // First, ensure all students referenced in routes exist in the database
            val existingStudents = dao.getAllStudentsStatic().toMutableList()
            fun getOrCreateStudentId(s: ParsedStudent): String {
                val match = existingStudents.find {
                    it.firstName.equals(s.firstName, ignoreCase = true) &&
                    it.studentClass.equals(s.studentClass, ignoreCase = true) &&
                    it.town.equals(s.town, ignoreCase = true)
                }
                if (match != null) return match.id
                val newEntity = StudentEntity(
                    id = UUID.randomUUID().toString(),
                    firstName = s.firstName,
                    studentClass = s.studentClass,
                    town = s.town
                )
                existingStudents.add(newEntity)
                return newEntity.id
            }

            // Collect all student entities to insert
            val studentsToInsert = mutableListOf<StudentEntity>()
            routes.forEach { r ->
                r.students.forEach { s ->
                    val match = existingStudents.find {
                        it.firstName.equals(s.firstName, ignoreCase = true) &&
                        it.studentClass.equals(s.studentClass, ignoreCase = true) &&
                        it.town.equals(s.town, ignoreCase = true)
                    }
                    if (match == null) {
                        val newEntity = StudentEntity(
                            id = UUID.randomUUID().toString(),
                            firstName = s.firstName,
                            studentClass = s.studentClass,
                            town = s.town
                        )
                        existingStudents.add(newEntity)
                        studentsToInsert.add(newEntity)
                    }
                }
            }
            if (studentsToInsert.isNotEmpty()) {
                dao.insertStudents(studentsToInsert)
            }

            if (strategy == ImportStrategy.OVERWRITE) {
                // Delete all existing route configs
                dao.deleteAllRouteConfigs()
                val entities = routes.map { r ->
                    val courseMatch = CourseType.entries.find {
                        it.name == r.routeId || it.displayName.equals(r.routeName, ignoreCase = true)
                    }
                    val courseKey = courseMatch?.name ?: (r.routeId ?: UUID.randomUUID().toString())
                    val studentIds = r.students.map { getOrCreateStudentId(it) }.toSet()
                    RouteConfigEntity(
                        courseType = courseKey,
                        scheduledTime = r.scheduledTime,
                        assignedStudentIdsJson = serializeIdsJson(studentIds),
                        customName = r.routeName
                    )
                }
                dao.insertRouteConfigs(entities)
                Result.success("Zastąpiono trasy: pomyślnie wczytano ${entities.size} tras i przypisania uczniów.")
            } else {
                // Merge mode: update or append
                val currentRoutes = dao.getAllRouteConfigsStatic()
                var updatedCount = 0
                var newCount = 0

                routes.forEach { r ->
                    val courseMatch = CourseType.entries.find {
                        it.name == r.routeId || it.displayName.equals(r.routeName, ignoreCase = true)
                    }
                    val courseKey = courseMatch?.name ?: (r.routeId ?: UUID.randomUUID().toString())
                    val existing = currentRoutes.find { it.courseType == courseKey || it.customName.equals(r.routeName, ignoreCase = true) }

                    val incomingStudentIds = r.students.map { getOrCreateStudentId(it) }.toSet()

                    if (existing != null) {
                        val currentIds = parseIdsJson(existing.assignedStudentIdsJson)
                        val mergedIds = currentIds + incomingStudentIds
                        dao.insertRouteConfig(
                            existing.copy(
                                scheduledTime = r.scheduledTime,
                                assignedStudentIdsJson = serializeIdsJson(mergedIds),
                                customName = r.routeName
                            )
                        )
                        updatedCount++
                    } else {
                        dao.insertRouteConfig(
                            RouteConfigEntity(
                                courseType = courseKey,
                                scheduledTime = r.scheduledTime,
                                assignedStudentIdsJson = serializeIdsJson(incomingStudentIds),
                                customName = r.routeName
                            )
                        )
                        newCount++
                    }
                }
                Result.success("Scalono trasy: zaktualizowano $updatedCount tras, dodano $newCount nowych tras.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun validateFullBackupJson(jsonText: String): Result<ParsedBackup> {
        return try {
            val root = JSONObject(jsonText.trim())
            val container = if (root.has("data") && root.get("data") is JSONObject) {
                root.getJSONObject("data")
            } else {
                root
            }

            val studentsList = mutableListOf<ParsedStudent>()
            if (container.has("students")) {
                val arr = container.getJSONArray("students")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    parseStudentFromObject(obj)?.let { studentsList.add(it) }
                }
            }

            val routesList = mutableListOf<ParsedRoute>()
            val routesArr = when {
                container.has("routes") -> container.getJSONArray("routes")
                container.has("routeConfigs") -> container.getJSONArray("routeConfigs")
                else -> null
            }
            if (routesArr != null) {
                for (i in 0 until routesArr.length()) {
                    val obj = routesArr.getJSONObject(i)
                    parseRouteFromObject(obj)?.let { routesList.add(it) }
                }
            }

            val guardiansList = mutableListOf<String>()
            if (container.has("guardians")) {
                val arr = container.getJSONArray("guardians")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("full_name", obj.optString("fullName", ""))
                    if (name.isNotBlank()) guardiansList.add(name)
                }
            }

            var sessionCount = 0
            if (container.has("attendance_sessions")) {
                sessionCount = container.getJSONArray("attendance_sessions").length()
            }

            if (studentsList.isEmpty() && routesList.isEmpty()) {
                Result.failure(Exception("Plik kopii zapasowej nie zawiera uczniów ani tras."))
            } else {
                Result.success(
                    ParsedBackup(
                        students = studentsList,
                        routes = routesList,
                        guardians = guardiansList,
                        sessionsCount = sessionCount
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Błąd odczytu pliku kopii zapasowej: ${e.localizedMessage}"))
        }
    }

    suspend fun importFullBackup(jsonText: String, strategy: ImportStrategy): Result<String> {
        return try {
            val validation = validateFullBackupJson(jsonText)
            if (validation.isFailure) {
                return Result.failure(validation.exceptionOrNull() ?: Exception("Błąd walidacji"))
            }
            val backup = validation.getOrThrow()

            if (strategy == ImportStrategy.OVERWRITE) {
                dao.clearAllData()
            }

            // Import students
            importStudentsList(backup.students, strategy)

            // Import routes
            importRoutesAndGroups(backup.routes, strategy)

            // Import guardians
            if (backup.guardians.isNotEmpty()) {
                val guardianEntities = backup.guardians.map {
                    GuardianEntity(id = UUID.randomUUID().toString(), fullName = it)
                }
                dao.insertGuardians(guardianEntities)
                if (preferences.getActiveGuardianName() == null && guardianEntities.isNotEmpty()) {
                    preferences.setActiveGuardian(guardianEntities.first().id, guardianEntities.first().fullName)
                }
            }

            // Parse and restore attendance sessions if available
            val root = JSONObject(jsonText.trim())
            val container = if (root.has("data") && root.get("data") is JSONObject) root.getJSONObject("data") else root
            var restoredSessions = 0
            if (container.has("attendance_sessions")) {
                val sessArr = container.getJSONArray("attendance_sessions")
                val allStudents = dao.getAllStudentsStatic()
                val allGuardians = dao.getAllGuardiansStatic()
                val fallbackGuardianId = allGuardians.firstOrNull()?.id ?: UUID.randomUUID().toString()

                for (i in 0 until sessArr.length()) {
                    val sObj = sessArr.getJSONObject(i)
                    val date = sObj.optString("date", "")
                    val courseType = sObj.optString("course_type", CourseType.DOWOZ_PKS.name)
                    val guardianName = sObj.optString("guardian_name", "Opiekun")
                    val notes = sObj.optString("notes", "")

                    if (date.isNotBlank()) {
                        val sessionId = UUID.randomUUID().toString()
                        dao.insertSession(
                            AttendanceSessionEntity(
                                id = sessionId,
                                date = date,
                                courseType = courseType,
                                guardianId = fallbackGuardianId,
                                guardianName = guardianName,
                                notes = notes
                            )
                        )
                        restoredSessions++

                        if (sObj.has("records")) {
                            val recArr = sObj.getJSONArray("records")
                            val recordsToInsert = mutableListOf<AttendanceRecordEntity>()
                            for (j in 0 until recArr.length()) {
                                val rObj = recArr.getJSONObject(j)
                                val fName = rObj.optString("first_name", "")
                                val sClass = rObj.optString("class", "")
                                val town = rObj.optString("town", "")
                                val isPresent = rObj.optBoolean("is_present", false)

                                val studentMatch = allStudents.find {
                                    it.firstName.equals(fName, ignoreCase = true) &&
                                    it.studentClass.equals(sClass, ignoreCase = true) &&
                                    it.town.equals(town, ignoreCase = true)
                                }
                                if (studentMatch != null) {
                                    recordsToInsert.add(
                                        AttendanceRecordEntity(
                                            id = UUID.randomUUID().toString(),
                                            sessionId = sessionId,
                                            studentId = studentMatch.id,
                                            isPresent = isPresent,
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                            if (recordsToInsert.isNotEmpty()) {
                                dao.insertRecords(recordsToInsert)
                            }
                        }
                    }
                }
            }

            Result.success("Przywrócono pełną kopię: ${backup.students.size} uczniów, ${backup.routes.size} tras, ${backup.guardians.size} opiekunów, $restoredSessions sesji obecności.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Backwards-compatibility wrapper
    suspend fun exportDataToJson(): String {
        return exportFullBackupJson()
    }

    suspend fun importDataFromJson(jsonString: String): Result<String> {
        return importFullBackup(jsonString, ImportStrategy.MERGE)
    }

    private fun parseIdsJson(json: String): Set<String> {
        return try {
            val arr = JSONArray(json)
            val result = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                result.add(arr.getString(i))
            }
            result
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun serializeIdsJson(ids: Set<String>): String {
        val arr = JSONArray()
        ids.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun StudentEntity.toModel(): Student =
        Student(id = id, firstName = firstName, studentClass = studentClass, town = town)

    private fun AttendanceRecordEntity.toModel(): AttendanceRecord =
        AttendanceRecord(
            id = id,
            sessionId = sessionId,
            studentId = studentId,
            isPresent = isPresent,
            timestamp = timestamp
        )
}
