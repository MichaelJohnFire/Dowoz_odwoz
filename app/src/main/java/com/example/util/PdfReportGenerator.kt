package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.model.AttendanceStatus
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data structures for PDF Report generation.
 * STRICTLY NO LAST NAMES for students as per system requirements.
 */
data class DailyAttendanceSummary(
    val dayNumber: Int,
    val date: String,
    val dayName: String,
    val presentCount: Int,
    val absentCount: Int
)

data class StudentReportItem(
    val number: Int,
    val firstName: String,
    val studentClass: String,
    val town: String,
    val status: AttendanceStatus,
    val statusText: String? = null,
    val weeklyDayStatuses: List<AttendanceStatus>? = null,
    val weeklyDaySigns: List<String>? = null
)

data class ReportPrintData(
    val date: String,
    val routeName: String,
    val scheduledTime: String,
    val guardianName: String,
    val students: List<StudentReportItem>,
    val notes: String? = null,
    val reportScopeTitle: String = "RAPORT OBECNOŚCI - DOWÓZ / ODWÓZ",
    val dateLabel: String = "Data zdarzenia:",
    val dailySummaries: List<DailyAttendanceSummary>? = null
)

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points (72 DPI)
    private const val PAGE_HEIGHT = 842 // A4 standard height in points (72 DPI)
    private const val MARGIN_LEFT = 36f
    private const val MARGIN_RIGHT = 559f // PAGE_WIDTH - 36
    private const val MARGIN_TOP = 36f
    private const val MARGIN_BOTTOM = 806f // PAGE_HEIGHT - 36
    private const val CONTENT_WIDTH = 523f // MARGIN_RIGHT - MARGIN_LEFT

    /**
     * Generates a native Android PDF document using PdfDocument API and saves it into the cache directory.
     */
    fun generateAttendancePdf(context: Context, data: ReportPrintData): File {
        val pdfDocument = PdfDocument()

        val totalStudents = data.students.size
        val presentCount = data.students.count { it.status == AttendanceStatus.PRESENT }
        val absentCount = data.students.count { it.status == AttendanceStatus.ABSENT }
        val attendancePercent = if (totalStudents > 0) (presentCount * 100) / totalStudents else 0

        // Pagination setup: ~24 items per page with header & footer
        val itemsPerPage = 24
        val pagesCount = if (data.students.isEmpty()) 1 else ((totalStudents + itemsPerPage - 1) / itemsPerPage)

        // Set up paints
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 9.5f
            typeface = Typeface.DEFAULT
        }

        val textBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.DEFAULT_BOLD
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105) // Slate 600
            textSize = 9.5f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }

        val metaLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
        }

        val metaValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 10f
            typeface = Typeface.DEFAULT
        }

        val tableHeaderFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(37, 99, 235) // Primary Blue (2563EB)
            style = Paint.Style.FILL
        }

        val tableHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.DEFAULT_BOLD
        }

        val rowEvenPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val rowOddPaint = Paint().apply {
            color = Color.rgb(248, 250, 252) // Slate 50
            style = Paint.Style.FILL
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
        }

        val statusPresentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(21, 128, 61) // Green 700
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val statusAbsentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(185, 28, 28) // Red 700
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val statusUncheckedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139) // Slate 500
            textSize = 9f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }

        val metaBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249) // Slate 100
            style = Paint.Style.FILL
        }

        val metaBoxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225) // Slate 300
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
        }

        val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 8f
        }

        for (pageIdx in 0 until pagesCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIdx + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var currentY = MARGIN_TOP

            // --- 1. HEADER SECTION ---
            currentY += 16f
            canvas.drawText(data.reportScopeTitle, PAGE_WIDTH / 2f, currentY, titlePaint)
            currentY += 14f
            canvas.drawText("Szkolna ewidencja przewozu uczniów na trasach autobusowych", PAGE_WIDTH / 2f, currentY, subtitlePaint)
            currentY += 12f

            // Metadata card (rounded rect)
            val metaRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 44f)
            canvas.drawRoundRect(metaRect, 6f, 6f, metaBoxPaint)
            canvas.drawRoundRect(metaRect, 6f, 6f, metaBoxBorder)

            val metaLeftX = MARGIN_LEFT + 12f
            val metaRightX = MARGIN_LEFT + (CONTENT_WIDTH / 2f) + 10f

            // Left column
            canvas.drawText(data.dateLabel, metaLeftX, currentY + 18f, metaLabelPaint)
            canvas.drawText(data.date, metaLeftX + 85f, currentY + 18f, metaValPaint)

            canvas.drawText("Trasa / Kurs:", metaLeftX, currentY + 34f, metaLabelPaint)
            canvas.drawText(data.routeName, metaLeftX + 85f, currentY + 34f, metaValPaint)

            // Right column
            canvas.drawText("Planowy odjazd:", metaRightX, currentY + 18f, metaLabelPaint)
            canvas.drawText(data.scheduledTime, metaRightX + 90f, currentY + 18f, metaValPaint)

            val cleanGuardianName = if (data.guardianName.equals("Opiekun dyżurny", ignoreCase = true)) "" else data.guardianName.trim()
            val guardianText = cleanGuardianName.ifBlank { "—" }
            canvas.drawText("Opiekun:", metaRightX, currentY + 34f, metaLabelPaint)
            canvas.drawText(guardianText, metaRightX + 90f, currentY + 34f, metaValPaint)

            currentY += 54f

            // --- 2. STUDENT ATTENDANCE TABLE ---
            val isWeekly = !data.dailySummaries.isNullOrEmpty() ||
                    data.students.any { it.weeklyDaySigns != null || it.weeklyDayStatuses != null } ||
                    data.reportScopeTitle.contains("TYGODNIOWY", ignoreCase = true)

            val colLpX = MARGIN_LEFT
            val colNameX = colLpX + (if (isWeekly) 30f else 32f)
            val colClassX = colNameX + (if (isWeekly) 130f else 125f)
            val colTownX = colClassX + (if (isWeekly) 55f else 65f)
            val colStatusX = colTownX + (if (isWeekly) 148f else 145f)
            val colDaysX = FloatArray(5) { i -> colStatusX + (i * 32f) }

            val headerHeight = 22f
            val headerRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + headerHeight)
            canvas.drawRoundRect(headerRect, 4f, 4f, tableHeaderFillPaint)

            // Table header text
            tableHeaderTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Lp.", colLpX + (if (isWeekly) 15f else 16f), currentY + 14.5f, tableHeaderTextPaint)

            tableHeaderTextPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Imię ucznia", colNameX + 8f, currentY + 14.5f, tableHeaderTextPaint)

            tableHeaderTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Klasa", colClassX + (if (isWeekly) 27.5f else 32.5f), currentY + 14.5f, tableHeaderTextPaint)

            tableHeaderTextPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Miejscowość", colTownX + 8f, currentY + 14.5f, tableHeaderTextPaint)

            if (isWeekly) {
                tableHeaderTextPaint.textAlign = Paint.Align.CENTER
                for (i in 0 until 5) {
                    val dayHeader = data.dailySummaries?.getOrNull(i)?.dayNumber?.toString()
                        ?: "${i + 1}"
                    canvas.drawText(dayHeader, colDaysX[i] + 16f, currentY + 14.5f, tableHeaderTextPaint)
                }
            } else {
                tableHeaderTextPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("Status", colStatusX + 78f, currentY + 14.5f, tableHeaderTextPaint)
            }

            currentY += headerHeight

            // Slice students for this page
            val startIndex = pageIdx * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, totalStudents)
            val pageStudents = if (startIndex < totalStudents) data.students.subList(startIndex, endIndex) else emptyList()

            val rowHeight = 20f

            if (data.students.isEmpty()) {
                val emptyRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 32f)
                canvas.drawRect(emptyRect, rowEvenPaint)
                canvas.drawLine(MARGIN_LEFT, currentY + 32f, MARGIN_RIGHT, currentY + 32f, linePaint)
                val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(100, 116, 139)
                    textSize = 10f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Brak przypisanych uczniów do wybranej trasy i sesji.", PAGE_WIDTH / 2f, currentY + 20f, emptyPaint)
                currentY += 32f
            } else {
                pageStudents.forEachIndexed { rowIdx, student ->
                    val rowTop = currentY
                    val rowBottom = currentY + rowHeight
                    val bgPaint = if (rowIdx % 2 == 0) rowEvenPaint else rowOddPaint

                    canvas.drawRect(MARGIN_LEFT, rowTop, MARGIN_RIGHT, rowBottom, bgPaint)
                    canvas.drawLine(MARGIN_LEFT, rowBottom, MARGIN_RIGHT, rowBottom, linePaint)

                    // Cell content:
                    // 1. Lp.
                    val centerLp = colLpX + (if (isWeekly) 15f else 16f)
                    val lpPaint = Paint(textPaint).apply { textAlign = Paint.Align.CENTER }
                    canvas.drawText("${student.number}.", centerLp, rowTop + 14f, lpPaint)

                    // 2. Imię (strictly firstName, no last names)
                    canvas.drawText(student.firstName, colNameX + 8f, rowTop + 14f, textBoldPaint)

                    // 3. Klasa
                    val classPaint = Paint(textPaint).apply { textAlign = Paint.Align.CENTER }
                    canvas.drawText(student.studentClass, colClassX + (if (isWeekly) 27.5f else 32.5f), rowTop + 14f, classPaint)

                    // 4. Miejscowość
                    canvas.drawText(student.town, colTownX + 8f, rowTop + 14f, textPaint)

                    // 5. Day status columns (weekly) or single Status column (single-day)
                    if (isWeekly) {
                        for (d in 0 until 5) {
                            val sign = student.weeklyDaySigns?.getOrNull(d)
                                ?: when (student.weeklyDayStatuses?.getOrNull(d)) {
                                    AttendanceStatus.PRESENT -> "+"
                                    else -> "-"
                                }
                            val signPaint = Paint(if (sign == "+") statusPresentPaint else statusAbsentPaint).apply {
                                textSize = 12f
                                typeface = Typeface.DEFAULT_BOLD
                                textAlign = Paint.Align.CENTER
                            }
                            canvas.drawText(sign, colDaysX[d] + 16f, rowTop + 14f, signPaint)
                        }
                    } else {
                        val centerStatus = colStatusX + 78f
                        val isSingleDay = student.statusText == null
                        val statusTextToDraw = student.statusText ?: when (student.status) {
                            AttendanceStatus.PRESENT -> "+"
                            AttendanceStatus.ABSENT -> "-"
                            AttendanceStatus.UNCHECKED -> "?"
                        }
                        val baseStatusPaint = when (student.status) {
                            AttendanceStatus.PRESENT -> statusPresentPaint
                            AttendanceStatus.ABSENT -> statusAbsentPaint
                            AttendanceStatus.UNCHECKED -> statusUncheckedPaint
                        }
                        val statusPaintToUse = Paint(baseStatusPaint).apply {
                            if (isSingleDay) {
                                textSize = 13f
                                typeface = Typeface.DEFAULT_BOLD
                            } else {
                                textSize = 8.5f
                            }
                        }
                        canvas.drawText(statusTextToDraw, centerStatus, rowTop + 14f, statusPaintToUse)
                    }

                    currentY += rowHeight
                }
            }

            // Outer vertical border lines for table
            canvas.drawLine(MARGIN_LEFT, currentY - (pageStudents.size * rowHeight) - headerHeight, MARGIN_LEFT, currentY, linePaint)
            canvas.drawLine(MARGIN_RIGHT, currentY - (pageStudents.size * rowHeight) - headerHeight, MARGIN_RIGHT, currentY, linePaint)

            // --- 3. FOOTER SECTION (Only on final page) ---
            if (pageIdx == pagesCount - 1) {
                currentY += 16f

                // Summary Box
                val hasDailySummaries = isWeekly && !data.dailySummaries.isNullOrEmpty()
                val summaryBoxHeight = if (hasDailySummaries) {
                    20f + (data.dailySummaries!!.size * 13f) + (if (!data.notes.isNullOrBlank()) 18f else 0f) + 8f
                } else {
                    if (!data.notes.isNullOrBlank()) 52f else 38f
                }

                val summaryRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + summaryBoxHeight)
                canvas.drawRoundRect(summaryRect, 6f, 6f, metaBoxPaint)
                canvas.drawRoundRect(summaryRect, 6f, 6f, metaBoxBorder)

                canvas.drawText("PODSUMOWANIE OBECNOŚCI:", MARGIN_LEFT + 12f, currentY + 16f, metaLabelPaint)

                if (hasDailySummaries) {
                    var lineY = currentY + 30f
                    data.dailySummaries!!.forEach { daySum ->
                        val line = "Dzień ${daySum.dayNumber} (${daySum.date}): Liczba obecnych (+): ${daySum.presentCount}  |  Liczba nieobecnych (-): ${daySum.absentCount}"
                        canvas.drawText(line, MARGIN_LEFT + 12f, lineY, textPaint)
                        lineY += 13f
                    }
                    if (!data.notes.isNullOrBlank()) {
                        val noteLabel = Paint(metaLabelPaint).apply { textSize = 8.5f }
                        val noteVal = Paint(textPaint).apply { textSize = 8.5f }
                        canvas.drawText("Uwagi opiekuna:", MARGIN_LEFT + 12f, lineY + 6f, noteLabel)
                        canvas.drawText(data.notes, MARGIN_LEFT + 95f, lineY + 6f, noteVal)
                    }
                } else {
                    val summaryStats = "Liczba obecnych (+): $presentCount  |  Liczba nieobecnych (-): $absentCount"
                    canvas.drawText(summaryStats, MARGIN_LEFT + 12f, currentY + 30f, textBoldPaint)
                    if (!data.notes.isNullOrBlank()) {
                        val noteLabel = Paint(metaLabelPaint).apply { textSize = 8.5f }
                        val noteVal = Paint(textPaint).apply { textSize = 8.5f }
                        canvas.drawText("Uwagi opiekuna:", MARGIN_LEFT + 12f, currentY + 45f, noteLabel)
                        canvas.drawText(data.notes, MARGIN_LEFT + 95f, currentY + 45f, noteVal)
                    }
                }

                currentY += summaryBoxHeight + 28f

                // Guardian Signature Box (right aligned)
                val signatureBoxRight = MARGIN_RIGHT - 10f
                val signaturePaint = Paint(textBoldPaint).apply {
                    textAlign = Paint.Align.RIGHT
                    textSize = 10f
                }
                val signatureSubPaint = Paint(subtitlePaint).apply {
                    textAlign = Paint.Align.RIGHT
                    textSize = 8.5f
                }

                canvas.drawText("Podpis opiekuna: .....................................................................", signatureBoxRight, currentY, signaturePaint)
                canvas.drawText("(czytelny podpis osoby sprawdzającej obecność)", signatureBoxRight, currentY + 14f, signatureSubPaint)
            }

            // Bottom Page Footnote
            footerTextPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Szkolny system obecności 'Dowóz-Odwóz' • Wydruk urzędowy", MARGIN_LEFT, MARGIN_BOTTOM + 16f, footerTextPaint)

            footerTextPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Strona ${pageIdx + 1} z $pagesCount", MARGIN_RIGHT, MARGIN_BOTTOM + 16f, footerTextPaint)

            pdfDocument.finishPage(page)
        }

        // Save generated PDF to cache
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanRoute = data.routeName.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val cleanDate = data.date.replace("[^0-9]".toRegex(), "")
        val fileName = "Raport_${cleanDate}_${cleanRoute}_$timeStamp.pdf"
        val outputFile = File(context.cacheDir, fileName)

        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(outputFile)
            pdfDocument.writeTo(outputStream)
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            pdfDocument.close()
        }

        return outputFile
    }

    /**
     * Shares the generated PDF file using native Android Intent.ACTION_SEND sheet.
     */
    fun sharePdf(context: Context, file: File, title: String = "Udostępnij raport obecności PDF") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            putExtra(Intent.EXTRA_TEXT, "Załączam wygenerowany raport obecności (${file.name}).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Prints the generated PDF file using the Android PrintManager API.
     */
    fun printPdf(context: Context, file: File, jobTitle: String = "Raport_Obecnosci") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = PdfFilePrintDocumentAdapter(file)
        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobTitle, printAdapter, printAttributes)
    }
}

/**
 * Standard Android PrintDocumentAdapter for streaming a pre-rendered PDF File to PrintManager.
 */
class PdfFilePrintDocumentAdapter(private val file: File) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder(file.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()

        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onWriteCancelled()
            return
        }

        if (destination == null) {
            callback?.onWriteFailed("Brak docelowego deskryptora pliku do wydruku.")
            return
        }

        var input: FileInputStream? = null
        var output: FileOutputStream? = null

        try {
            input = FileInputStream(file)
            output = FileOutputStream(destination.fileDescriptor)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } >= 0) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }
                output.write(buffer, 0, bytesRead)
            }

            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message ?: "Błąd zapisu pliku PDF do wydruku.")
        } finally {
            try { input?.close() } catch (_: Exception) {}
            try { output?.close() } catch (_: Exception) {}
        }
    }
}
