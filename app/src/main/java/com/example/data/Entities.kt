package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val firstName: String,
    val studentClass: String,
    val town: String
)

@Entity(tableName = "guardians")
data class GuardianEntity(
    @PrimaryKey val id: String,
    val fullName: String
)

@Entity(tableName = "route_configs")
data class RouteConfigEntity(
    @PrimaryKey val courseType: String,
    val scheduledTime: String,
    val assignedStudentIdsJson: String, // Comma or JSON-separated IDs
    val customName: String? = null
)

@Entity(
    tableName = "attendance_sessions",
    indices = [Index(value = ["date", "courseType"], unique = true)]
)
data class AttendanceSessionEntity(
    @PrimaryKey val id: String,
    val date: String,
    val courseType: String,
    val guardianId: String,
    val guardianName: String,
    val notes: String
)

@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["sessionId", "studentId"], unique = true),
        Index(value = ["studentId"])
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val studentId: String,
    val isPresent: Boolean,
    val timestamp: Long
)
