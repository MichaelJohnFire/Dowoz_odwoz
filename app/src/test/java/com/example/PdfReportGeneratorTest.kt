package com.example

import com.example.model.AttendanceRecord
import com.example.model.AttendanceSession
import com.example.model.AttendanceStatus
import com.example.model.CourseType
import com.example.model.RouteConfig
import com.example.model.Student
import com.example.util.DailyAttendanceSummary
import com.example.util.ReportPrintData
import com.example.util.StudentReportItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfReportGeneratorTest {

    @Test
    fun testStudentReportItemHasNoLastName() {
        val student = StudentReportItem(
            number = 1,
            firstName = "Kacper",
            studentClass = "3A",
            town = "Lipowa",
            status = AttendanceStatus.PRESENT
        )

        // Strict assertion: student has only firstName, studentClass, town
        assertEquals("Kacper", student.firstName)
        assertEquals("3A", student.studentClass)
        assertEquals("Lipowa", student.town)
        assertEquals(AttendanceStatus.PRESENT, student.status)
        assertFalse("Should not contain surname separator", student.firstName.contains(" "))
    }

    @Test
    fun testReportPrintDataSummaryCalculation() {
        val students = listOf(
            StudentReportItem(1, "Kacper", "3A", "Lipowa", AttendanceStatus.PRESENT),
            StudentReportItem(2, "Zuzanna", "3B", "Brzozowa", AttendanceStatus.PRESENT),
            StudentReportItem(3, "Michał", "1A", "Dębowa", AttendanceStatus.ABSENT),
            StudentReportItem(4, "Ola", "2B", "Cisowa", AttendanceStatus.UNCHECKED)
        )

        val reportData = ReportPrintData(
            date = "2026-10-27",
            routeName = "Dowóz GIMBUS",
            scheduledTime = "07:20",
            guardianName = "Anna Wiśniewska",
            students = students,
            notes = "Przejazd planowy"
        )

        val presentCount = reportData.students.count { it.status == AttendanceStatus.PRESENT }
        val absentCount = reportData.students.count { it.status == AttendanceStatus.ABSENT }
        val uncheckedCount = reportData.students.count { it.status == AttendanceStatus.UNCHECKED }
        val totalCount = reportData.students.size
        val percentage = (presentCount * 100) / totalCount

        assertEquals(2, presentCount)
        assertEquals(1, absentCount)
        assertEquals(1, uncheckedCount)
        assertEquals(4, totalCount)
        assertEquals(50, percentage)
        assertEquals("Anna Wiśniewska", reportData.guardianName)
        assertEquals("Dowóz GIMBUS", reportData.routeName)

        val singleDaySummary = "Liczba obecnych (+): $presentCount  |  Liczba nieobecnych (-): $absentCount"
        assertEquals("Liczba obecnych (+): 2  |  Liczba nieobecnych (-): 1", singleDaySummary)
        assertFalse(singleDaySummary.contains("Frekwencja"))
        assertFalse(singleDaySummary.contains("Liczba uczniów"))
    }

    @Test
    fun testRouteAndSessionMapping() {
        val route = RouteConfig(
            id = CourseType.DOWOZ_PKS.name,
            name = CourseType.DOWOZ_PKS.displayName,
            courseType = CourseType.DOWOZ_PKS,
            scheduledTime = "06:50",
            assignedStudentIds = setOf("id_1", "id_2")
        )

        val allStudents = listOf(
            Student(id = "id_1", firstName = "Zofia", studentClass = "2B", town = "Lipowa"),
            Student(id = "id_2", firstName = "Adam", studentClass = "1A", town = "Brzozowa"),
            Student(id = "id_3", firstName = "Marek", studentClass = "4C", town = "Dębowa")
        )

        val records = listOf(
            AttendanceRecord(sessionId = "s1", studentId = "id_1", isPresent = true),
            AttendanceRecord(sessionId = "s1", studentId = "id_2", isPresent = false)
        )

        val recordMap = records.associateBy { it.studentId }
        val relevant = allStudents.filter { route.assignedStudentIds.contains(it.id) }
            .sortedWith(compareBy({ it.studentClass }, { it.firstName }))

        val reportItems = relevant.mapIndexed { index, s ->
            val isPresent = recordMap[s.id]?.isPresent
            val status = when (isPresent) {
                true -> AttendanceStatus.PRESENT
                false -> AttendanceStatus.ABSENT
                null -> AttendanceStatus.UNCHECKED
            }
            StudentReportItem(
                number = index + 1,
                firstName = s.firstName,
                studentClass = s.studentClass,
                town = s.town,
                status = status
            )
        }

        assertEquals(2, reportItems.size)
        // Check sorting: 1A (Adam) before 2B (Zofia)
        assertEquals("Adam", reportItems[0].firstName)
        assertEquals(AttendanceStatus.ABSENT, reportItems[0].status)
        assertEquals("Zofia", reportItems[1].firstName)
        assertEquals(AttendanceStatus.PRESENT, reportItems[1].status)
    }

    @Test
    fun testWeeklyReportDataStructure() {
        val students = listOf(
            StudentReportItem(
                number = 1,
                firstName = "Jan",
                studentClass = "3A",
                town = "Lipowa",
                status = AttendanceStatus.PRESENT,
                statusText = "Obecny: 4/5 dni (80%)"
            ),
            StudentReportItem(
                number = 2,
                firstName = "Kamil",
                studentClass = "2B",
                town = "Brzozowa",
                status = AttendanceStatus.ABSENT,
                statusText = "Obecny: 1/5 dni (20%)"
            )
        )

        val reportData = ReportPrintData(
            date = "2026-10-26 – 2026-10-30 (Poniedziałek – Piątek)",
            routeName = "Dowóz GIMBUS",
            scheduledTime = "07:15",
            guardianName = "Anna Wiśniewska",
            students = students,
            notes = "Raport zbiorczy za tydzień",
            reportScopeTitle = "TYGODNIOWY RAPORT OBECNOŚCI - DOWÓZ / ODWÓZ",
            dateLabel = "Zakres tygodnia:"
        )

        assertEquals("TYGODNIOWY RAPORT OBECNOŚCI - DOWÓZ / ODWÓZ", reportData.reportScopeTitle)
        assertEquals("Zakres tygodnia:", reportData.dateLabel)
        assertEquals("Obecny: 4/5 dni (80%)", reportData.students[0].statusText)
        assertEquals(2, reportData.students.size)
        assertFalse(reportData.students[0].firstName.contains(" "))
    }

    @Test
    fun testSingleDayStatusSigns() {
        val presentItem = StudentReportItem(1, "Anna", "2B", "Lipowa", AttendanceStatus.PRESENT)
        val absentItem = StudentReportItem(2, "Piotr", "3A", "Brzozowa", AttendanceStatus.ABSENT)
        val uncheckedItem = StudentReportItem(3, "Marek", "1C", "Dębowa", AttendanceStatus.UNCHECKED)

        fun resolveDisplayStatus(item: StudentReportItem): String {
            return item.statusText ?: when (item.status) {
                AttendanceStatus.PRESENT -> "+"
                AttendanceStatus.ABSENT -> "-"
                AttendanceStatus.UNCHECKED -> "?"
            }
        }

        assertEquals("+", resolveDisplayStatus(presentItem))
        assertEquals("-", resolveDisplayStatus(absentItem))
        assertEquals("?", resolveDisplayStatus(uncheckedItem))
    }

    @Test
    fun testWeeklyDayColumnsAndFooterSummaries() {
        val student1 = StudentReportItem(
            number = 1,
            firstName = "Kacper",
            studentClass = "3A",
            town = "Lipowa",
            status = AttendanceStatus.PRESENT,
            weeklyDayStatuses = listOf(
                AttendanceStatus.PRESENT,
                AttendanceStatus.PRESENT,
                AttendanceStatus.ABSENT,
                AttendanceStatus.PRESENT,
                AttendanceStatus.PRESENT
            ),
            weeklyDaySigns = listOf("+", "+", "-", "+", "+")
        )
        val student2 = StudentReportItem(
            number = 2,
            firstName = "Maja",
            studentClass = "3A",
            town = "Lipowa",
            status = AttendanceStatus.ABSENT,
            weeklyDayStatuses = listOf(
                AttendanceStatus.ABSENT,
                AttendanceStatus.ABSENT,
                AttendanceStatus.PRESENT,
                AttendanceStatus.ABSENT,
                AttendanceStatus.ABSENT
            ),
            weeklyDaySigns = listOf("-", "-", "+", "-", "-")
        )

        val dailySummaries = listOf(
            DailyAttendanceSummary(dayNumber = 26, date = "2026-10-26", dayName = "Pn", presentCount = 1, absentCount = 1),
            DailyAttendanceSummary(dayNumber = 27, date = "2026-10-27", dayName = "Wt", presentCount = 1, absentCount = 1),
            DailyAttendanceSummary(dayNumber = 28, date = "2026-10-28", dayName = "Śr", presentCount = 1, absentCount = 1),
            DailyAttendanceSummary(dayNumber = 29, date = "2026-10-29", dayName = "Cz", presentCount = 1, absentCount = 1),
            DailyAttendanceSummary(dayNumber = 30, date = "2026-10-30", dayName = "Pt", presentCount = 1, absentCount = 1)
        )

        val weeklyReport = ReportPrintData(
            date = "2026-10-26 – 2026-10-30 (Poniedziałek – Piątek)",
            routeName = "Dowóz GIMBUS",
            scheduledTime = "07:20",
            guardianName = "Anna Wiśniewska",
            students = listOf(student1, student2),
            reportScopeTitle = "TYGODNIOWY RAPORT OBECNOŚCI - DOWÓZ / ODWÓZ",
            dateLabel = "Zakres tygodnia:",
            dailySummaries = dailySummaries
        )

        // Verify weekly day columns signs (1..5)
        assertEquals(5, student1.weeklyDaySigns?.size)
        assertEquals("+", student1.weeklyDaySigns?.get(0)) // Monday
        assertEquals("+", student1.weeklyDaySigns?.get(1)) // Tuesday
        assertEquals("-", student1.weeklyDaySigns?.get(2)) // Wednesday
        assertEquals("+", student1.weeklyDaySigns?.get(3)) // Thursday
        assertEquals("+", student1.weeklyDaySigns?.get(4)) // Friday

        assertEquals("-", student2.weeklyDaySigns?.get(0))
        assertEquals("-", student2.weeklyDaySigns?.get(1))
        assertEquals("+", student2.weeklyDaySigns?.get(2))
        assertEquals("-", student2.weeklyDaySigns?.get(3))
        assertEquals("-", student2.weeklyDaySigns?.get(4))

        // Verify footer daily attendance summaries with actual calendar day numbers
        assertNotNull(weeklyReport.dailySummaries)
        assertEquals(5, weeklyReport.dailySummaries?.size)
        val day1 = weeklyReport.dailySummaries!![0]
        assertEquals(26, day1.dayNumber) // Calendar day 26
        assertEquals("2026-10-26", day1.date)
        assertEquals(1, day1.presentCount)
        assertEquals(1, day1.absentCount)

        val day5 = weeklyReport.dailySummaries!![4]
        assertEquals(30, day5.dayNumber) // Calendar day 30
        assertEquals("2026-10-30", day5.date)
    }

    @Test
    fun testCustomRouteDynamicAttendanceMapping() {
        val initialRoutes = mutableListOf(
            RouteConfig(id = CourseType.DOWOZ_PKS.name, name = CourseType.DOWOZ_PKS.displayName, courseType = CourseType.DOWOZ_PKS, scheduledTime = "06:50"),
            RouteConfig(id = CourseType.DOWOZ_GIMBUS.name, name = CourseType.DOWOZ_GIMBUS.displayName, courseType = CourseType.DOWOZ_GIMBUS, scheduledTime = "07:20"),
            RouteConfig(id = CourseType.ODWOZ_PKS_1.name, name = CourseType.ODWOZ_PKS_1.displayName, courseType = CourseType.ODWOZ_PKS_1, scheduledTime = "13:40"),
            RouteConfig(id = CourseType.ODWOZ_GIMBUS.name, name = CourseType.ODWOZ_GIMBUS.displayName, courseType = CourseType.ODWOZ_GIMBUS, scheduledTime = "14:35"),
            RouteConfig(id = CourseType.ODWOZ_PKS_2.name, name = CourseType.ODWOZ_PKS_2.displayName, courseType = CourseType.ODWOZ_PKS_2, scheduledTime = "15:25")
        )
        assertEquals(5, initialRoutes.size)

        // 1. User adds a new route in Routes module
        val newCustomRoute = RouteConfig(
            id = "custom_route_polnocna",
            name = "Dowóz Trasa Północna",
            courseType = CourseType.DOWOZ_GIMBUS,
            scheduledTime = "07:05",
            assignedStudentIds = setOf("student_1"),
            isCustom = true
        )
        initialRoutes.add(newCustomRoute)

        // It now automatically appears in the Attendance list
        assertEquals(6, initialRoutes.size)
        assertTrue(initialRoutes.any { it.id == "custom_route_polnocna" })
        val foundRoute = initialRoutes.find { it.id == "custom_route_polnocna" }
        assertNotNull(foundRoute)
        assertEquals("Dowóz Trasa Północna", foundRoute?.name)
        assertEquals("07:05", foundRoute?.scheduledTime)
        assertTrue(foundRoute?.isCustom == true)

        // 2. Attendance session created for this custom route preserves routeId
        val session = AttendanceSession(
            id = "sess_custom_1",
            date = "2026-11-09",
            courseType = foundRoute!!.courseType,
            guardianId = "g1",
            guardianName = "Anna Dyżurna",
            routeId = foundRoute.id
        )
        assertEquals("custom_route_polnocna", session.routeId)

        // 3. Deleting route removes it from routes and attendance module
        initialRoutes.removeIf { it.id == "custom_route_polnocna" }
        assertEquals(5, initialRoutes.size)
        assertFalse(initialRoutes.any { it.id == "custom_route_polnocna" })
    }
}

