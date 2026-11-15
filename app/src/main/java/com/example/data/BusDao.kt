package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BusDao {

    // --- Students ---
    @Query("SELECT * FROM students ORDER BY studentClass ASC, firstName ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students")
    suspend fun getAllStudentsStatic(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudent(id: String)

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()

    // --- Guardians ---
    @Query("SELECT * FROM guardians ORDER BY fullName ASC")
    fun getAllGuardians(): Flow<List<GuardianEntity>>

    @Query("SELECT * FROM guardians")
    suspend fun getAllGuardiansStatic(): List<GuardianEntity>

    @Query("SELECT * FROM guardians WHERE id = :id LIMIT 1")
    suspend fun getGuardianById(id: String): GuardianEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuardian(guardian: GuardianEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuardians(guardians: List<GuardianEntity>)

    @Query("DELETE FROM guardians WHERE id = :id")
    suspend fun deleteGuardian(id: String)

    @Query("DELETE FROM guardians")
    suspend fun deleteAllGuardians()

    // --- Route Configs ---
    @Query("SELECT * FROM route_configs")
    fun getAllRouteConfigs(): Flow<List<RouteConfigEntity>>

    @Query("SELECT * FROM route_configs")
    suspend fun getAllRouteConfigsStatic(): List<RouteConfigEntity>

    @Query("SELECT * FROM route_configs WHERE courseType = :courseType LIMIT 1")
    suspend fun getRouteConfig(courseType: String): RouteConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouteConfig(config: RouteConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouteConfigs(configs: List<RouteConfigEntity>)

    @Query("DELETE FROM route_configs WHERE courseType = :courseType")
    suspend fun deleteRouteConfig(courseType: String)

    @Query("DELETE FROM route_configs")
    suspend fun deleteAllRouteConfigs()

    // --- Attendance Sessions ---
    @Query("SELECT * FROM attendance_sessions WHERE date = :date AND courseType = :courseType LIMIT 1")
    suspend fun getSession(date: String, courseType: String): AttendanceSessionEntity?

    @Query("SELECT * FROM attendance_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): AttendanceSessionEntity?

    @Query("SELECT * FROM attendance_sessions WHERE date = :date")
    suspend fun getSessionsForDate(date: String): List<AttendanceSessionEntity>

    @Query("SELECT * FROM attendance_sessions ORDER BY date DESC")
    fun getAllSessionsFlow(): Flow<List<AttendanceSessionEntity>>

    @Query("SELECT * FROM attendance_sessions ORDER BY date DESC")
    suspend fun getAllSessionsStatic(): List<AttendanceSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AttendanceSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<AttendanceSessionEntity>)

    @Query("UPDATE attendance_sessions SET notes = :notes WHERE id = :sessionId")
    suspend fun updateSessionNotes(sessionId: String, notes: String)

    @Query("SELECT * FROM attendance_sessions WHERE courseType = :courseType")
    suspend fun getSessionsForCourse(courseType: String): List<AttendanceSessionEntity>

    @Query("DELETE FROM attendance_sessions WHERE courseType = :courseType")
    suspend fun deleteSessionsForCourse(courseType: String)

    @Query("DELETE FROM attendance_sessions")
    suspend fun deleteAllSessions()

    // --- Attendance Records ---
    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId")
    suspend fun getRecordsForSession(sessionId: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId")
    fun getRecordsForSessionFlow(sessionId: String): Flow<List<AttendanceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>)

    @Query("DELETE FROM attendance_records WHERE sessionId = :sessionId")
    suspend fun deleteRecordsForSession(sessionId: String)

    @Query("DELETE FROM attendance_records WHERE sessionId = :sessionId AND studentId = :studentId")
    suspend fun deleteRecordForStudent(sessionId: String, studentId: String)

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY timestamp DESC")
    suspend fun getRecordsForStudent(studentId: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllRecords(): List<AttendanceRecordEntity>

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAllRecords()

    @Transaction
    suspend fun clearAllData() {
        deleteAllRecords()
        deleteAllSessions()
        deleteAllRouteConfigs()
        deleteAllGuardians()
        deleteAllStudents()
    }
}
