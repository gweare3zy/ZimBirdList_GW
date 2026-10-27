package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.catalog.ZimbabweBirdCatalog
import com.example.data.model.BirdSpecies
import com.example.data.model.Trip
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TripPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 40f
    private const val COL_GAP = 20f
    private const val BOTTOM_MARGIN = 45f

    private data class BirdLayout(
        val bird: BirdSpecies,
        val lines: List<String>,
        val tagLineIndex: Int?,
        val tagText: String?,
        val height: Float
    )

    fun generateTripPdf(
        context: Context,
        trip: Trip,
        seenBirds: List<BirdSpecies>
    ): File {
        val pdfDocument = PdfDocument()

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x1B, 0x43, 0x1B) // Forest / Acacia Green
            textSize = 17f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x30, 0x30, 0x30)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x1B, 0x1B, 0x1B)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x55, 0x55, 0x55)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0xA6, 0x3D, 0x40) // Terracotta for rarity
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val headerBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x2D, 0x5A, 0x27)
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0xDD, 0xDD, 0xDD)
            strokeWidth = 0.8f
        }

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x2D, 0x5A, 0x27)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x2D, 0x5A, 0x27)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val tripDateStr = dateFormatter.format(Date(trip.dateMillis))
        val generatedDateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val colWidth = (PAGE_WIDTH - 2 * MARGIN - COL_GAP) / 2
        val col1X = MARGIN
        val col2X = MARGIN + colWidth + COL_GAP
        val checkboxIndent = 13f
        val maxTextWidth = colWidth - checkboxIndent

        val birds = seenBirds.sortedBy { it.commonName }
        val totalBirds = birds.size
        var birdIndex = 0
        var pageNumber = 1

        if (totalBirds == 0) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val startY = drawPageHeader(
                canvas = canvas,
                trip = trip,
                tripDateStr = tripDateStr,
                seenCount = totalBirds,
                headerBarPaint = headerBarPaint,
                titlePaint = titlePaint,
                subtitlePaint = subtitlePaint,
                metaPaint = metaPaint,
                linePaint = linePaint
            )

            canvas.drawText(
                "No species marked as seen for this trip yet.",
                MARGIN,
                startY + 15f,
                boldBodyPaint
            )

            drawPageFooter(canvas, pageNumber, generatedDateStr, metaPaint)
            pdfDocument.finishPage(page)
        } else {
            val maxY = PAGE_HEIGHT - BOTTOM_MARGIN

            while (birdIndex < totalBirds) {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val startY = if (pageNumber == 1) {
                    drawPageHeader(
                        canvas = canvas,
                        trip = trip,
                        tripDateStr = tripDateStr,
                        seenCount = totalBirds,
                        headerBarPaint = headerBarPaint,
                        titlePaint = titlePaint,
                        subtitlePaint = subtitlePaint,
                        metaPaint = metaPaint,
                        linePaint = linePaint
                    )
                } else {
                    drawRunningHeader(canvas, trip, linePaint, metaPaint)
                }

                // Fill Column 1
                var col1Y = startY
                while (birdIndex < totalBirds) {
                    val bird = birds[birdIndex]
                    val layout = measureBird(bird, maxTextWidth, boldBodyPaint, tagPaint)
                    if (col1Y + layout.height > maxY && col1Y > startY) {
                        break
                    }
                    drawBirdEntry(canvas, layout, col1X, col1Y, boldBodyPaint, tagPaint, boxPaint, checkPaint)
                    col1Y += layout.height + 3f
                    birdIndex++
                }

                // Fill Column 2
                var col2Y = startY
                while (birdIndex < totalBirds) {
                    val bird = birds[birdIndex]
                    val layout = measureBird(bird, maxTextWidth, boldBodyPaint, tagPaint)
                    if (col2Y + layout.height > maxY && col2Y > startY) {
                        break
                    }
                    drawBirdEntry(canvas, layout, col2X, col2Y, boldBodyPaint, tagPaint, boxPaint, checkPaint)
                    col2Y += layout.height + 3f
                    birdIndex++
                }

                drawPageFooter(canvas, pageNumber, generatedDateStr, metaPaint)
                pdfDocument.finishPage(page)
                pageNumber++
            }
        }

        // Save output to cache directory
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val sanitizedTripName = trip.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(24)
        val file = File(reportsDir, "BirdList_${sanitizedTripName}_${trip.id}.pdf")

        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }

        return file
    }

    fun wrapText(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (paint.measureText(trimmed) <= maxWidth) {
            return listOf(trimmed)
        }

        val words = trimmed.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = ""
                }
                // Handle single word wider than column (e.g., hyphenated bird names)
                if (paint.measureText(word) > maxWidth) {
                    if (word.contains("-")) {
                        val subParts = word.split("-")
                        for (idx in subParts.indices) {
                            val partWithHyphen = if (idx < subParts.size - 1) "${subParts[idx]}-" else subParts[idx]
                            val testSub = if (currentLine.isEmpty()) partWithHyphen else "$currentLine $partWithHyphen"
                            if (paint.measureText(testSub) <= maxWidth) {
                                currentLine = testSub
                            } else {
                                if (currentLine.isNotEmpty()) lines.add(currentLine)
                                currentLine = partWithHyphen
                            }
                        }
                    } else {
                        // Fallback character wrapping
                        for (ch in word) {
                            val testChar = currentLine + ch
                            if (paint.measureText(testChar) <= maxWidth) {
                                currentLine = testChar
                            } else {
                                if (currentLine.isNotEmpty()) lines.add(currentLine)
                                currentLine = ch.toString()
                            }
                        }
                    }
                } else {
                    currentLine = word
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }

    private fun measureBird(
        bird: BirdSpecies,
        maxTextWidth: Float,
        namePaint: Paint,
        tagPaint: Paint
    ): BirdLayout {
        val tagText = bird.rarityLabel?.let { "[$it]" }
        val tagWidth = tagText?.let { tagPaint.measureText(it) + 4f } ?: 0f

        val rawLines = wrapText(bird.commonName, namePaint, maxTextWidth)
        val finalLines = if (rawLines.isEmpty()) mutableListOf(bird.commonName) else rawLines.toMutableList()
        val tagLineIndex: Int?

        if (tagText != null) {
            val lastLine = finalLines.last()
            val lastLineWidth = namePaint.measureText(lastLine)
            if (lastLineWidth + tagWidth <= maxTextWidth) {
                tagLineIndex = finalLines.size - 1
            } else {
                // Wrap tag to its own line if it doesn't fit on the last line
                finalLines.add("")
                tagLineIndex = finalLines.size - 1
            }
        } else {
            tagLineIndex = null
        }

        val lineAdvance = 11.5f
        val height = finalLines.size * lineAdvance + 1.5f

        return BirdLayout(
            bird = bird,
            lines = finalLines,
            tagLineIndex = tagLineIndex,
            tagText = tagText,
            height = height
        )
    }

    private fun drawBirdEntry(
        canvas: Canvas,
        layout: BirdLayout,
        x: Float,
        y: Float,
        namePaint: Paint,
        tagPaint: Paint,
        boxPaint: Paint,
        checkPaint: Paint
    ) {
        val boxSize = 7.5f
        val boxY = y + 1f
        canvas.drawRect(x, boxY, x + boxSize, boxY + boxSize, boxPaint)
        canvas.drawText("✓", x + 1f, boxY + boxSize - 0.8f, checkPaint)

        val textX = x + boxSize + 5f
        var currentY = y + 8f
        val lineAdvance = 11.5f

        for (i in layout.lines.indices) {
            val line = layout.lines[i]
            if (line.isNotEmpty()) {
                canvas.drawText(line, textX, currentY, namePaint)
            }
            if (i == layout.tagLineIndex && layout.tagText != null) {
                val lineWidth = if (line.isNotEmpty()) namePaint.measureText(line) + 4f else 0f
                canvas.drawText(layout.tagText, textX + lineWidth, currentY, tagPaint)
            }
            currentY += lineAdvance
        }
    }

    private fun drawPageHeader(
        canvas: Canvas,
        trip: Trip,
        tripDateStr: String,
        seenCount: Int,
        headerBarPaint: Paint,
        titlePaint: Paint,
        subtitlePaint: Paint,
        metaPaint: Paint,
        linePaint: Paint
    ): Float {
        // Decorative top bar
        canvas.drawRect(MARGIN, 22f, PAGE_WIDTH - MARGIN, 26f, headerBarPaint)

        // Main Title
        canvas.drawText("ZIMBABWE BIRD SIGHTING REPORT", MARGIN, 46f, titlePaint)

        // Trip Name & Date
        canvas.drawText("Trip: ${trip.name}", MARGIN, 64f, subtitlePaint)
        canvas.drawText("Date: $tripDateStr", MARGIN, 78f, metaPaint)

        var currentY = 92f

        if (trip.location.isNotBlank()) {
            val locLines = wrapText("Location: ${trip.location}", metaPaint, PAGE_WIDTH - 2 * MARGIN - 170f)
            for (line in locLines) {
                canvas.drawText(line, MARGIN, currentY, metaPaint)
                currentY += 11f
            }
        }

        // Stats summary box on top-right
        val statText1 = "Observed: $seenCount species"
        val statText2 = "Checklist total: ${ZimbabweBirdCatalog.totalCount} species"
        val statX = PAGE_WIDTH - MARGIN - 165f
        canvas.drawText(statText1, statX, 64f, subtitlePaint)
        canvas.drawText(statText2, statX, 78f, metaPaint)

        if (trip.notes.isNotBlank()) {
            val notesLines = wrapText("Notes: ${trip.notes}", metaPaint, PAGE_WIDTH - 2 * MARGIN)
            currentY = maxOf(currentY, 92f)
            for (noteLine in notesLines) {
                canvas.drawText(noteLine, MARGIN, currentY, metaPaint)
                currentY += 11f
            }
        }

        currentY += 4f
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, linePaint)
        return currentY + 14f
    }

    private fun drawRunningHeader(
        canvas: Canvas,
        trip: Trip,
        linePaint: Paint,
        metaPaint: Paint
    ): Float {
        canvas.drawText("ZIMBABWE BIRD CHECKLIST — ${trip.name} (Continued)", MARGIN, 36f, metaPaint)
        canvas.drawLine(MARGIN, 42f, PAGE_WIDTH - MARGIN, 42f, linePaint)
        return 56f
    }

    private fun drawPageFooter(
        canvas: Canvas,
        pageNumber: Int,
        generatedDateStr: String,
        metaPaint: Paint
    ) {
        val y = PAGE_HEIGHT - 22f
        canvas.drawText("Generated by Zim Bird Checklist — $generatedDateStr", MARGIN, y, metaPaint)
        val pageText = "Page $pageNumber"
        val pageTextWidth = metaPaint.measureText(pageText)
        canvas.drawText(pageText, PAGE_WIDTH - MARGIN - pageTextWidth, y, metaPaint)
    }

    fun sharePdf(context: Context, file: File, tripTitle: String) {
        val targetFile = file.canonicalFile
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Zimbabwe Bird Sighting Report: $tripTitle")
            putExtra(
                Intent.EXTRA_TEXT,
                "Attached is the bird sighting report for '$tripTitle' recorded with the Zim Bird Checklist app."
            )
            clipData = android.content.ClipData.newRawUri("report", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Bird Report: $tripTitle").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context,
                "Unable to open share sheet: ${e.localizedMessage}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}
