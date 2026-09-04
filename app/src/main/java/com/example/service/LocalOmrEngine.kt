package com.example.service

import android.graphics.Bitmap
import com.example.model.AnswerKey
import com.example.model.OmrCheckResult
import com.example.model.QuestionResult
import java.util.Locale

object LocalOmrEngine {

    /**
     * Inspects the OMR sheet bitmap strictly following the user's algorithmic rules:
     * 1. Check ID Code: 10 columns, each must have exactly 1 filled bubble (0-9).
     *    - Strict rule: Dumaloq chegarasidan tashqariga chiqib ketgan bo'lsa (overflow), bu xato!
     *    - If not valid or overflowed, immediately STOP and return KOD_XATO.
     * 2. Check 30 questions: Read marked option.
     *    - If marked bubble spills outside the circle, it is flagged as XATO / invalid (0 points).
     *    - Compare valid answers with AnswerKey.
     * 3. Return clean structured result.
     */
    fun analyzeSheet(bitmap: Bitmap, answerKey: AnswerKey): OmrCheckResult {
        val width = bitmap.width
        val height = bitmap.height

        // Advanced Image Processing Pipeline:
        // 1. Grayscale luminance extraction
        // 2. Contrast Enhancement (percentile-based stretching)
        // 3. Noise Reduction (3x3 smoothing filter)
        // 4. Adaptive Binarization (Otsu thresholding)
        val binarized = ImageProcessor.preprocessBitmap(bitmap)

        val scaleX = width / 595.0
        val scaleY = height / 842.0

        // 1. Check ID Code (10 columns, each with 0..9 bubbles)
        val topSectionTop = 82.0 * scaleY
        val idCardLeft = 305.0 * scaleX
        val colWidth = 22.0 * scaleX
        val idGridStartX = idCardLeft + (14.0 * scaleX)
        val idBoxTop = topSectionTop + (30.0 * scaleY)
        val idBoxHeight = 16.0 * scaleY
        val bubblesStartCenterY = idBoxTop + idBoxHeight + (12.0 * scaleY)
        val bubbleStepY = 16.0 * scaleY
        val bubbleRadius = (6.0 * scaleX).toInt().coerceAtLeast(3)

        val detectedIdDigits = mutableListOf<Int?>()
        var idHasError = false
        var idErrorMessage = ""

        for (col in 0 until 10) {
            val colCenterX = idGridStartX + col * colWidth + colWidth / 2.0
            val validDigitsInCol = mutableListOf<Int>()
            var colHasOverflow = false

            for (digit in 0..9) {
                val cy = bubblesStartCenterY + digit * bubbleStepY
                val inspection = ImageProcessor.inspectBubble(
                    binarized = binarized,
                    width = width,
                    height = height,
                    centerX = colCenterX.toInt(),
                    centerY = cy.toInt(),
                    radius = bubbleRadius
                )

                if (inspection.isFilled) {
                    if (inspection.hasOverflow) {
                        // Foydalanuvchi talabi: "dumaloqni ichidan chiqib ketmagan bo'lishi kerak chiqqan bo'lsa ham xato hisoblanadi"
                        colHasOverflow = true
                    } else {
                        validDigitsInCol.add(digit)
                    }
                }
            }

            if (colHasOverflow) {
                idHasError = true
                idErrorMessage = "${col + 1}-ustundagi ID raqam doirachadan chiqib ketgan holda bo'yalgan (Xato)."
                detectedIdDigits.add(null)
            } else if (validDigitsInCol.size == 1) {
                detectedIdDigits.add(validDigitsInCol.first())
            } else {
                idHasError = true
                if (validDigitsInCol.isEmpty()) {
                    idErrorMessage = "${col + 1}-ustunda ID raqam bo'yalmagan."
                } else {
                    idErrorMessage = "${col + 1}-ustunda bittadan ortiq ID raqam bo'yalgan."
                }
                detectedIdDigits.add(null)
            }
        }

        // Rule 1: "Agar 10 talik kod to'liq belgilanmagan bo'lsa, xato bo'yalgan bo'lsa
        // yoki umuman o'qib bo'lmasa, tekshirishni SHU JOYDA TO'XTATING. Statusni 'KOD_XATO' deb belgilang."
        if (idHasError || detectedIdDigits.size != 10 || detectedIdDigits.any { it == null }) {
            return OmrCheckResult(
                status = "KOD_XATO",
                student_id = null,
                total_questions = 30,
                correct_count = 0,
                incorrect_count = 0,
                score_percentage = 0.0,
                question_details = emptyList(),
                raw_model_response = "10 talik ID kod to'liq yoki to'g'ri belgilanmagan (KOD_XATO). $idErrorMessage Tekshirish to'xtatildi."
            )
        }

        val studentIdString = detectedIdDigits.joinToString("") { it.toString() }

        // Rule 2: TEST JAVOBLARINI TEKSHIRISH (Faqat ID kod to'g'ri bo'lsa)
        val answersBoxTop = topSectionTop + (225.0 * scaleY) + (16.0 * scaleY)
        val answersBoxBottom = height - (45.0 * scaleY)
        val totalColWidth = (width - (80.0 * scaleX)) / 3.0
        val colStarts = listOf(1, 11, 21)
        val options = listOf("A", "B", "C", "D")
        val ansBubbleStepX = 23.0 * scaleX
        val ansBubbleRadius = (7.5 * scaleX).toInt().coerceAtLeast(4)
        val rowHeight = (answersBoxBottom - answersBoxTop - (25.0 * scaleY)) / 10.0

        val questionDetails = mutableListOf<QuestionResult>()
        var correctCount = 0
        var incorrectCount = 0

        for (c in 0..2) {
            val colLeft = (40.0 * scaleX) + c * totalColWidth
            val firstQ = colStarts[c]
            val optionsStartX = colLeft + (52.0 * scaleX)

            for (r in 0 until 10) {
                val qNum = firstQ + r
                val rowCenterY = answersBoxTop + (20.0 * scaleY) + r * rowHeight + rowHeight / 2.0

                // Check 4 bubbles (A, B, C, D)
                val validMarkedOptions = mutableListOf<String>()
                var questionHasOverflow = false

                for (optIdx in 0 until 4) {
                    val cx = optionsStartX + optIdx * ansBubbleStepX
                    val inspection = ImageProcessor.inspectBubble(
                        binarized = binarized,
                        width = width,
                        height = height,
                        centerX = cx.toInt(),
                        centerY = rowCenterY.toInt(),
                        radius = ansBubbleRadius
                    )

                    if (inspection.isFilled) {
                        if (inspection.hasOverflow) {
                            // Dumaloqdan chiqib ketgan bo'lsa -> xato
                            questionHasOverflow = true
                        } else {
                            validMarkedOptions.add(options[optIdx])
                        }
                    }
                }

                val studentAnswer = when {
                    questionHasOverflow -> "BELGILANMAGAN" // Dumaloqdan chiqqani uchun qabul qilinmaydi
                    validMarkedOptions.isEmpty() -> "BELGILANMAGAN"
                    validMarkedOptions.size == 1 -> validMarkedOptions.first()
                    else -> "BELGILANMAGAN" // Multiple marks considered invalid
                }

                val correctAnswer = answerKey.getAnswerFor(qNum)
                val isCorrect = studentAnswer == correctAnswer

                if (isCorrect) {
                    correctCount++
                } else {
                    incorrectCount++
                }

                questionDetails.add(
                    QuestionResult(
                        questionNumber = qNum,
                        studentAnswer = studentAnswer,
                        correctAnswer = correctAnswer,
                        isCorrect = isCorrect
                    )
                )
            }
        }

        val percentage = (correctCount * 100.0) / 30.0
        val roundedPercentage = String.format(Locale.US, "%.2f", percentage).toDoubleOrNull() ?: percentage

        return OmrCheckResult(
            status = "SUCCESS",
            student_id = studentIdString,
            total_questions = 30,
            correct_count = correctCount,
            incorrect_count = incorrectCount,
            score_percentage = roundedPercentage,
            question_details = questionDetails,
            raw_model_response = "Kengaytirilgan kontrast va chegarani tekshiruvchi kompyuter ko'rish tizimi orqali muvaffaqiyatli tahlil qilindi."
        )
    }
}
