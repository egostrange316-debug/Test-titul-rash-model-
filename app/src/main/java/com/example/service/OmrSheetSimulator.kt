package com.example.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.model.AnswerKey
import java.util.Locale
import kotlin.random.Random

data class SimulatedSheetData(
    val studentName: String = "Ali Valiyev",
    val subject: String = "Matematika",
    val group: String = "101-guruh",
    val date: String = "04.09.2026",
    // 10 digits; null means that column has no bubble filled. Multiple digits mean invalid.
    val idColumns: List<List<Int>> = listOf(
        listOf(1), listOf(2), listOf(3), listOf(4), listOf(5),
        listOf(6), listOf(7), listOf(8), listOf(9), listOf(0)
    ),
    // 30 answers, null or empty string means BELGILANMAGAN
    val studentAnswers: Map<Int, String> = (1..30).associateWith { "A" },
    // Columns where the student scribbled outside the bubble boundary
    val idOverflowColumns: Set<Int> = emptySet(),
    // Questions where the student scribbled outside the answer bubble boundary
    val answerOverflowQuestions: Set<Int> = emptySet()
) {
    val isIdStrictlyValid: Boolean
        get() = idColumns.size == 10 && idColumns.all { it.size == 1 } && idOverflowColumns.isEmpty()

    val formattedId: String?
        get() = if (isIdStrictlyValid) idColumns.joinToString("") { it.first().toString() } else null
}

object OmrSheetSimulator {

    fun createPresetValid(answerKey: AnswerKey): SimulatedSheetData {
        val answers = mutableMapOf<Int, String>()
        for (i in 1..30) {
            val correct = answerKey.getAnswerFor(i)
            // 25 correct, 3 wrong, 2 unmarked
            answers[i] = when {
                i in listOf(5, 15, 25) -> when (correct) {
                    "A" -> "B"
                    "B" -> "C"
                    "C" -> "D"
                    else -> "A"
                }
                i in listOf(10, 20) -> "" // unmarked
                else -> correct
            }
        }
        val idList = listOf(listOf(9), listOf(8), listOf(7), listOf(6), listOf(5), listOf(4), listOf(3), listOf(2), listOf(1), listOf(0))
        return SimulatedSheetData(
            studentName = "Zokirov Jasur",
            subject = "Informatika",
            group = "TATU 412-20",
            idColumns = idList,
            studentAnswers = answers
        )
    }

    fun createPresetInvalidId(reason: String = "Bitta ustun bo'yalmagan"): SimulatedSheetData {
        // Missing column 4
        val idList = mutableListOf(
            listOf(2), listOf(0), listOf(2), emptyList(), // index 3 empty!
            listOf(6), listOf(1), listOf(4), listOf(8), listOf(9), listOf(5)
        )
        return SimulatedSheetData(
            studentName = "Karimov Anvar",
            subject = "Ona tili",
            group = "11-A",
            idColumns = idList,
            studentAnswers = (1..30).associateWith { "B" }
        )
    }

    fun createPresetDoubleMarkedId(): SimulatedSheetData {
        // Double marked in column 2 (two bubbles: 3 and 7 filled)
        val idList = mutableListOf(
            listOf(5), listOf(3, 7), listOf(1), listOf(9),
            listOf(0), listOf(4), listOf(2), listOf(6), listOf(8), listOf(1)
        )
        return SimulatedSheetData(
            studentName = "Sobirova Laylo",
            subject = "Ingliz tili",
            group = "9-B",
            idColumns = idList,
            studentAnswers = (1..30).associateWith { "C" }
        )
    }

    fun createPresetIdOverflow(): SimulatedSheetData {
        // Column 3 has mark overflowing outside the circle perimeter
        val idList = listOf(listOf(1), listOf(2), listOf(3), listOf(4), listOf(5), listOf(6), listOf(7), listOf(8), listOf(9), listOf(0))
        return SimulatedSheetData(
            studentName = "Rustamov Diyor",
            subject = "Kimyo",
            group = "104-guruh",
            idColumns = idList,
            idOverflowColumns = setOf(2), // 3-ustundagi 3 raqami doirachadan chiqib ketgan!
            studentAnswers = (1..30).associateWith { "A" }
        )
    }

    fun createPresetAnswerOverflow(answerKey: AnswerKey): SimulatedSheetData {
        // Question 7 and Question 18 have marks overflowing outside the circle perimeter
        val answers = (1..30).associateWith { answerKey.getAnswerFor(it) }
        val idList = listOf(listOf(7), listOf(7), listOf(1), listOf(2), listOf(3), listOf(4), listOf(5), listOf(6), listOf(8), listOf(9))
        return SimulatedSheetData(
            studentName = "Nematov Sardor",
            subject = "Fizika",
            group = "3-kurs",
            idColumns = idList,
            studentAnswers = answers,
            answerOverflowQuestions = setOf(7, 18) // Savollarda doirachadan chiqib ketgan
        )
    }

    /**
     * Renders a high-resolution Bitmap of the OMR sheet with student pencil marks filled in.
     */
    fun renderToBitmap(sheetData: SimulatedSheetData, width: Int = 1190, height: Int = 1684): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val scaleX = width / 595f
        val scaleY = height / 842f

        canvas.save()
        canvas.scale(scaleX, scaleY)

        // 1. Draw base sheet
        PdfGenerator.drawOmrSheet(canvas)

        // 2. Draw handwritten student info
        val handPaint = Paint().apply {
            color = Color.rgb(20, 30, 80) // dark blue pen
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val topSectionTop = 82f
        val studentCardLeft = 40f
        val fieldStart = topSectionTop + 55f
        val fieldStep = 36f

        canvas.drawText(sheetData.studentName, studentCardLeft + 80f, fieldStart - 1f, handPaint)
        canvas.drawText(sheetData.subject, studentCardLeft + 80f, fieldStart + fieldStep - 1f, handPaint)
        canvas.drawText(sheetData.group, studentCardLeft + 95f, fieldStart + fieldStep * 2 - 1f, handPaint)
        canvas.drawText(sheetData.date, studentCardLeft + 80f, fieldStart + fieldStep * 3 - 1f, handPaint)
        canvas.drawText("J.Zokirov", studentCardLeft + 80f, fieldStart + fieldStep * 4 - 1f, handPaint)

        // 3. Draw filled ID bubbles (pencil/pen dark graphite color)
        val pencilPaint = Paint().apply {
            color = Color.rgb(30, 30, 35) // dark graphite fill
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val idCardLeft = 305f
        val colWidth = 22f
        val idGridStartX = idCardLeft + 14f
        val idBoxTop = topSectionTop + 30f
        val idBoxHeight = 16f
        val bubbleRadius = 7f
        val bubbleStepY = 16f
        val bubblesStartCenterY = idBoxTop + idBoxHeight + 12f

        for (col in 0 until minOf(10, sheetData.idColumns.size)) {
            val colCenterX = idGridStartX + col * colWidth + colWidth / 2f
            val filledDigits = sheetData.idColumns[col]

            // Top box number text if filled
            if (filledDigits.size == 1) {
                val digit = filledDigits.first()
                val boxTextPaint = Paint().apply {
                    color = Color.rgb(20, 30, 80)
                    textSize = 11f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText(digit.toString(), colCenterX, idBoxTop + 12.5f, boxTextPaint)
            }

            // Fill bubbles
            for (digit in filledDigits) {
                if (digit in 0..9) {
                    val cy = bubblesStartCenterY + digit * bubbleStepY
                    val isOverflow = sheetData.idOverflowColumns.contains(col)
                    if (isOverflow) {
                        // O'quvchi doirachadan toshirib yuborgan (spill outside bubble)
                        canvas.drawCircle(colCenterX, cy, bubbleRadius + 4f, pencilPaint)
                    } else {
                        canvas.drawCircle(colCenterX, cy, bubbleRadius - 0.5f, pencilPaint)
                    }
                }
            }
        }

        // 4. Draw filled question bubbles (1..30)
        val answersBoxTop = topSectionTop + 225f + 16f
        val answersBoxBottom = 842f - 45f
        val totalColWidth = (595f - 80f) / 3f
        val colStarts = listOf(1, 11, 21)
        val options = listOf("A", "B", "C", "D")
        val ansBubbleRadius = 8.5f
        val ansBubbleStepX = 23f
        val rowHeight = (answersBoxBottom - answersBoxTop - 25f) / 10f

        for (c in 0..2) {
            val colLeft = 40f + c * totalColWidth
            val firstQ = colStarts[c]
            val optionsStartX = colLeft + 52f

            for (r in 0 until 10) {
                val qNum = firstQ + r
                val studentAns = sheetData.studentAnswers[qNum]
                if (!studentAns.isNullOrBlank() && studentAns != "BELGILANMAGAN") {
                    val optIdx = options.indexOf(studentAns.uppercase(Locale.US))
                    if (optIdx != -1) {
                        val rowCenterY = answersBoxTop + 20f + r * rowHeight + rowHeight / 2f
                        val cx = optionsStartX + optIdx * ansBubbleStepX
                        val isOverflow = sheetData.answerOverflowQuestions.contains(qNum)
                        if (isOverflow) {
                            // Doirachadan toshib ketgan bo'yash
                            canvas.drawCircle(cx, rowCenterY, ansBubbleRadius + 4.5f, pencilPaint)
                        } else {
                            canvas.drawCircle(cx, rowCenterY, ansBubbleRadius - 0.5f, pencilPaint)
                        }
                    }
                }
            }
        }

        canvas.restore()
        return bitmap
    }
}
