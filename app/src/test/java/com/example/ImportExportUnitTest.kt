package com.example

import com.example.data.BusDao
import com.example.data.BusRepository
import com.example.data.PreferencesManager
import com.example.model.CourseType
import com.example.model.ImportStrategy
import com.example.model.ParsedRoute
import com.example.model.ParsedStudent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ImportExportUnitTest {

    @Test
    fun testStudentListJsonValidation() {
        val json = """
            {
              "version": "1.0",
              "export_type": "STUDENTS_LIST",
              "data": [
                { "first_name": "Jan", "class": "3A", "town": "Lipowa" },
                { "first_name": "Anna", "class": "2B", "town": "Dąbrowa" }
              ]
            }
        """.trimIndent()

        val context = RuntimeEnvironment.getApplication()
        val prefs = PreferencesManager(context)
        val db = com.example.data.BusDatabase.getInstance(context)
        val repo = BusRepository(db.busDao(), prefs)

        val result = repo.validateStudentsJson(json)
        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(2, list.size)
        assertEquals("Jan", list[0].firstName)
        assertEquals("3A", list[0].studentClass)
        assertEquals("Lipowa", list[0].town)
        assertEquals("Anna", list[1].firstName)
    }

    @Test
    fun testRoutesAndGroupsJsonValidation() {
        val json = """
            {
              "version": "1.0",
              "export_type": "ROUTES_AND_GROUPS",
              "data": [
                {
                  "route_id": "DOWOZ_PKS",
                  "route_name": "Dowóz PKS",
                  "scheduled_time": "06:50",
                  "students": [
                    { "first_name": "Jan", "class": "3A", "town": "Lipowa" }
                  ]
                }
              ]
            }
        """.trimIndent()

        val context = RuntimeEnvironment.getApplication()
        val prefs = PreferencesManager(context)
        val db = com.example.data.BusDatabase.getInstance(context)
        val repo = BusRepository(db.busDao(), prefs)

        val result = repo.validateRoutesJson(json)
        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(1, list.size)
        assertEquals("Dowóz PKS", list[0].routeName)
        assertEquals("06:50", list[0].scheduledTime)
        assertEquals(1, list[0].students.size)
        assertEquals("Jan", list[0].students[0].firstName)
    }

    @Test
    fun testFullBackupJsonValidation() {
        val json = """
            {
              "version": "1.0",
              "export_type": "FULL_BACKUP",
              "app_name": "Dowóz-odwóz",
              "exported_at": "2026-10-27 10:30:00",
              "data": {
                "students": [
                  { "first_name": "Jan", "class": "3A", "town": "Lipowa" }
                ],
                "routes": [
                  {
                    "route_id": "DOWOZ_PKS",
                    "route_name": "Dowóz PKS",
                    "scheduled_time": "06:50",
                    "students": []
                  }
                ],
                "guardians": [
                  { "full_name": "Jan Nowak" }
                ]
              }
            }
        """.trimIndent()

        val context = RuntimeEnvironment.getApplication()
        val prefs = PreferencesManager(context)
        val db = com.example.data.BusDatabase.getInstance(context)
        val repo = BusRepository(db.busDao(), prefs)

        val result = repo.validateFullBackupJson(json)
        assertTrue(result.isSuccess)
        val backup = result.getOrThrow()
        assertEquals(1, backup.students.size)
        assertEquals(1, backup.routes.size)
        assertEquals(1, backup.guardians.size)
        assertEquals("Jan Nowak", backup.guardians[0])
    }

    @Test
    fun testInvalidJsonValidationFailsGracefully() {
        val invalidJson = "{ invalid json content }"
        val context = RuntimeEnvironment.getApplication()
        val prefs = PreferencesManager(context)
        val db = com.example.data.BusDatabase.getInstance(context)
        val repo = BusRepository(db.busDao(), prefs)

        val result = repo.validateStudentsJson(invalidJson)
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }
}
