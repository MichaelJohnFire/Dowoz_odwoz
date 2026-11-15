package com.example.model

enum class ImportStrategy {
    MERGE,      // Append new entries without removing existing ones
    OVERWRITE   // Replace current data with imported data
}

data class ParsedStudent(
    val firstName: String,
    val studentClass: String,
    val town: String
)

data class ParsedRoute(
    val routeId: String?,
    val routeName: String,
    val scheduledTime: String,
    val students: List<ParsedStudent> = emptyList()
)

data class ParsedBackup(
    val students: List<ParsedStudent>,
    val routes: List<ParsedRoute>,
    val guardians: List<String>,
    val sessionsCount: Int
)

sealed class ImportDialogState {
    data object None : ImportDialogState()
    data class StudentsPending(val students: List<ParsedStudent>, val rawJson: String) : ImportDialogState()
    data class RoutesPending(val routes: List<ParsedRoute>, val rawJson: String) : ImportDialogState()
    data class FullBackupPending(val backup: ParsedBackup, val rawJson: String) : ImportDialogState()
    data class Error(val title: String, val message: String) : ImportDialogState()
}
