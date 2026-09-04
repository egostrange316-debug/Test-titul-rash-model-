package com.example.service

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PdfGenerator {

    /**
     * Standard A4 page size in PostScript points: 595 x 842 (72 dpi).
     */
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun generateOmrSheetPdf(context: Context): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawOmrSheet(canvas)

        pdfDocument.finishPage(page)

        // Save to internal cache for sharing/viewing
        val file = File(context.cacheDir, "OMR_Titul_Varaqasi_30.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    /**
     * Saves a copy of the PDF to the device's public Downloads directory.
     */
    fun saveToPublicDownloads(context: Context): Uri? {
        val file = generateOmrSheetPdf(context)
        val fileName = "OMR_Titul_Varaqasi_30.pdf"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { input -> input.copyTo(out) }
                    }
                    uri
                } else null
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destFile = File(downloadsDir, fileName)
                file.copyTo(destFile, overwrite = true)
                Uri.fromFile(destFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Triggers standard share intent for the generated PDF.
     */
    fun sharePdf(context: Context, file: File = generateOmrSheetPdf(context)) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "OMR Test Titul Varaqasi")
                putExtra(Intent.EXTRA_TEXT, "OMR Test Titul Varaqasi (30 talik test uchun).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "OMR Titul PDF ni ulashish"))
        } catch (e: Exception) {
            Toast.makeText(context, "Ulashishda xatolik: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens PDF in external PDF reader app.
     */
    fun openPdf(context: Context, file: File = generateOmrSheetPdf(context)) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "PDF ochuvchi dastur topilmadi", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Vector drawing of the exact A4 OMR sheet layout provided by the user.
     */
    fun drawOmrSheet(canvas: Canvas) {
        // Background white
        canvas.drawColor(Color.WHITE)

        val blackPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        val strokePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val boldTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val markerSize = 35f // ~12mm in points
        val markerMargin = 22f // ~8mm in points

        // 1. OMR 4 corner detection markers (black squares)
        // Top-Left
        canvas.drawRect(markerMargin, markerMargin, markerMargin + markerSize, markerMargin + markerSize, blackPaint)
        // Top-Right
        canvas.drawRect(PAGE_WIDTH - markerMargin - markerSize, markerMargin, PAGE_WIDTH - markerMargin, markerMargin + markerSize, blackPaint)
        // Bottom-Left
        canvas.drawRect(markerMargin, PAGE_HEIGHT - markerMargin - markerSize, markerMargin + markerSize, PAGE_HEIGHT - markerMargin, blackPaint)
        // Bottom-Right
        canvas.drawRect(PAGE_WIDTH - markerMargin - markerSize, PAGE_HEIGHT - markerMargin - markerSize, PAGE_WIDTH - markerMargin, PAGE_HEIGHT - markerMargin, blackPaint)

        // 2. Header
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val headerText = "JAVOBLAR VARAQASI (OMR TITUL)"
        canvas.drawText(headerText, PAGE_WIDTH / 2f, 55f, headerPaint)

        // Header underline
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2f
        }
        canvas.drawLine(40f, 68f, PAGE_WIDTH - 40f, 68f, linePaint)

        // 3. Top Section: Student Card (left) and 10-digit ID Block (right)
        val topSectionTop = 82f
        val topSectionHeight = 225f

        // Student Card
        val studentCardLeft = 40f
        val studentCardRight = 290f
        val studentCardRect = RectF(studentCardLeft, topSectionTop, studentCardRight, topSectionTop + topSectionHeight)
        canvas.drawRoundRect(studentCardRect, 4f, 4f, strokePaint)

        // Student Card Title
        boldTextPaint.textSize = 10f
        boldTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("TALABA / O‘QUVCHI MA’LUMOTLARI", (studentCardLeft + studentCardRight) / 2f, topSectionTop + 18f, boldTextPaint)
        canvas.drawLine(studentCardLeft + 10f, topSectionTop + 26f, studentCardRight - 10f, topSectionTop + 26f, strokePaint)

        // Fields
        val fields = listOf("F.I.SH:", "Fan nomi:", "Sinf / Guruh:", "Sana:", "Imzo:")
        val fieldStart = topSectionTop + 55f
        val fieldStep = 36f
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.LEFT

        val underLinePaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 1f
        }

        for (i in fields.indices) {
            val y = fieldStart + i * fieldStep
            canvas.drawText(fields[i], studentCardLeft + 12f, y, textPaint)
            val labelWidth = textPaint.measureText(fields[i])
            val lineStartX = studentCardLeft + 16f + maxOf(labelWidth, 70f)
            canvas.drawLine(lineStartX, y + 2f, studentCardRight - 12f, y + 2f, underLinePaint)
        }

        // ID Card
        val idCardLeft = 305f
        val idCardRight = PAGE_WIDTH - 40f
        val idCardRect = RectF(idCardLeft, topSectionTop, idCardRight, topSectionTop + topSectionHeight)
        canvas.drawRoundRect(idCardRect, 4f, 4f, strokePaint)

        // ID Card Title
        canvas.drawText("ID KOD (10 XONA)", (idCardLeft + idCardRight) / 2f, topSectionTop + 18f, boldTextPaint)

        // 10 columns grid
        val colWidth = 22f
        val idGridStartX = idCardLeft + 14f
        val idBoxTop = topSectionTop + 30f
        val idBoxHeight = 16f
        val bubbleRadius = 7f
        val bubbleStepY = 16f
        val bubblesStartCenterY = idBoxTop + idBoxHeight + 12f

        val bubbleTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        for (col in 0 until 10) {
            val colCenterX = idGridStartX + col * colWidth + colWidth / 2f
            val boxLeft = colCenterX - 8f
            val boxRight = colCenterX + 8f

            // Top box for writing number
            canvas.drawRect(boxLeft, idBoxTop, boxRight, idBoxTop + idBoxHeight, strokePaint)

            // 10 bubbles (0 to 9)
            for (digit in 0..9) {
                val cy = bubblesStartCenterY + digit * bubbleStepY
                canvas.drawCircle(colCenterX, cy, bubbleRadius, strokePaint)
                canvas.drawText(digit.toString(), colCenterX, cy + 2.8f, bubbleTextPaint)
            }
        }

        // 4. 30 Questions Answer Box (3 columns: 1-10, 11-20, 21-30)
        val answersBoxTop = topSectionTop + topSectionHeight + 16f
        val answersBoxBottom = PAGE_HEIGHT - 45f
        val answersBoxRect = RectF(40f, answersBoxTop, PAGE_WIDTH - 40f, answersBoxBottom)
        canvas.drawRoundRect(answersBoxRect, 4f, 4f, strokePaint)

        val totalColWidth = (PAGE_WIDTH - 80f) / 3f

        // Vertical dividers between columns
        val dividerPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1.2f
        }
        canvas.drawLine(40f + totalColWidth, answersBoxTop + 10f, 40f + totalColWidth, answersBoxBottom - 10f, dividerPaint)
        canvas.drawLine(40f + totalColWidth * 2f, answersBoxTop + 10f, 40f + totalColWidth * 2f, answersBoxBottom - 10f, dividerPaint)

        // Draw 3 columns (1..10, 11..20, 21..30)
        val colStarts = listOf(1, 11, 21)
        val options = listOf("A", "B", "C", "D")
        val ansBubbleRadius = 8.5f
        val ansBubbleStepX = 23f
        val rowHeight = (answersBoxBottom - answersBoxTop - 25f) / 10f

        val qNumPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val dottedLinePaint = Paint().apply {
            color = Color.rgb(230, 230, 230)
            strokeWidth = 1f
        }

        val ansLetterPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        for (c in 0..2) {
            val colLeft = 40f + c * totalColWidth
            val firstQ = colStarts[c]

            for (r in 0 until 10) {
                val qNum = firstQ + r
                val rowCenterY = answersBoxTop + 20f + r * rowHeight + rowHeight / 2f

                // Question number
                canvas.drawText("$qNum.", colLeft + 35f, rowCenterY + 3.5f, qNumPaint)

                // Dotted row bottom line
                canvas.drawLine(colLeft + 12f, rowCenterY + rowHeight / 2f - 2f, colLeft + totalColWidth - 12f, rowCenterY + rowHeight / 2f - 2f, dottedLinePaint)

                // 4 Options (A, B, C, D)
                val optionsStartX = colLeft + 52f
                for (optIdx in 0 until 4) {
                    val cx = optionsStartX + optIdx * ansBubbleStepX
                    canvas.drawCircle(cx, rowCenterY, ansBubbleRadius, strokePaint)
                    canvas.drawText(options[optIdx], cx, rowCenterY + 3.2f, ansLetterPaint)
                }
            }
        }
    }
}
