package com.example.model

import java.util.UUID

// Attendance Status Indicator for vehicle checklist
enum class AttendanceStatus {
    UNCHECKED, // ⚪ Niesprawdzony
    PRESENT,   // 🟢 Obecny
    ABSENT     // 🔴 Nieobecny
}

// Student Data Model - First Name ONLY, Class, Town - NO last names for students
data class Student(
    val id: String = UUID.randomUUID().toString(),
    val firstName: String,
    val studentClass: String,
    val town: String,
    val isPresent: Boolean = false
)

// Guardian Data Model
data class Guardian(
    val id: String = UUID.randomUUID().toString(),
    val fullName: String
)

// Route Types Enum
enum class CourseType(val displayName: String, val defaultTime: String, val isPickup: Boolean) {
    DOWOZ_PKS("Dowóz PKS", "06:50", true),
    DOWOZ_GIMBUS("Dowóz GIMBUS", "07:20", true),
    ODWOZ_PKS_1("Odwóz PKS I", "13:40", false),
    ODWOZ_GIMBUS("Odwóz GIMBUS", "14:35", false),
    ODWOZ_PKS_2("Odwóz PKS II", "15:25", false);

    companion object {
        fun fromString(value: String): CourseType {
            return entries.find { it.name == value || it.displayName.equals(value, ignoreCase = true) }
                ?: DOWOZ_PKS
        }
    }
}

// Scheduled route configuration
data class RouteConfig(
    val id: String = courseType.name,
    val name: String = courseType.displayName,
    val courseType: CourseType = CourseType.fromString(id),
    val scheduledTime: String,
    val assignedStudentIds: Set<String> = emptySet(),
    val isCustom: Boolean = false
)

// An attendance session for a given date and route
data class AttendanceSession(
    val id: String = UUID.randomUUID().toString(),
    val date: String, // YYYY-MM-DD
    val courseType: CourseType,
    val guardianId: String,
    val guardianName: String,
    val notes: String = "",
    val records: List<AttendanceRecord> = emptyList(),
    val routeId: String = courseType.name
)

// Attendance record for an individual student in a session
data class AttendanceRecord(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val studentId: String,
    val isPresent: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// Student attendance history entry for detailed profile
data class StudentHistoryItem(
    val date: String,
    val courseType: CourseType,
    val guardianName: String,
    val isPresent: Boolean,
    val notes: String = ""
)
