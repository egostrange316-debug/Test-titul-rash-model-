package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Exact JSON format requested by the user:
 * {
 *   "status": "SUCCESS" or "KOD_XATO",
 *   "student_id": "aniqlangan_10_talik_kod_yoki_null",
 *   "total_questions": 30,
 *   "correct_count": to'g'ri_topilgan_savollar_soni_yoki_0,
 *   "incorrect_count": noto'g'ri_topilgan_savollar_soni_yoki_0,
 *   "score_percentage": foizdagi_natija_yoki_0
 * }
 */
data class OmrCheckResult(
    val status: String, // "SUCCESS" or "KOD_XATO"
    val student_id: String?,
    val total_questions: Int = 30,
    val correct_count: Int = 0,
    val incorrect_count: Int = 0,
    val score_percentage: Double = 0.0,
    val question_details: List<QuestionResult> = emptyList(),
    val raw_model_response: String? = null
) {
    val isSuccess: Boolean
        get() = status == "SUCCESS"

    /**
     * Produces clean JSON string without markdown code fences as requested.
     */
    fun toCleanJson(): String {
        val json = JSONObject()
        json.put("status", status)
        if (student_id != null) {
            json.put("student_id", student_id)
        } else {
            json.put("student_id", JSONObject.NULL)
        }
        json.put("total_questions", total_questions)
        json.put("correct_count", correct_count)
        json.put("incorrect_count", incorrect_count)
        val formattedPercentage = String.format(Locale.US, "%.2f", score_percentage).toDoubleOrNull() ?: score_percentage
        json.put("score_percentage", formattedPercentage)
        return json.toString(2)
    }

    companion object {
        fun error(reason: String = "ID kod to'liq yoki to'g'ri belgilanmagan"): OmrCheckResult {
            return OmrCheckResult(
                status = "KOD_XATO",
                student_id = null,
                total_questions = 30,
                correct_count = 0,
                incorrect_count = 0,
                score_percentage = 0.0,
                raw_model_response = reason
            )
        }

        fun parseFromJson(jsonString: String): OmrCheckResult {
            // Strip any accidental markdown formatting if present
            var clean = jsonString.trim()
            if (clean.startsWith("```json")) {
                clean = clean.removePrefix("```json")
            } else if (clean.startsWith("```")) {
                clean = clean.removePrefix("```")
            }
            if (clean.endsWith("```")) {
                clean = clean.removeSuffix("```")
            }
            clean = clean.trim()

            val obj = JSONObject(clean)
            val status = obj.optString("status", "KOD_XATO")
            val studentId = if (obj.isNull("student_id")) null else obj.optString("student_id").takeIf { it.isNotEmpty() }
            val totalQuestions = obj.optInt("total_questions", 30)
            val correctCount = obj.optInt("correct_count", 0)
            val incorrectCount = obj.optInt("incorrect_count", 0)
            val scorePercentage = obj.optDouble("score_percentage", 0.0)

            val details = mutableListOf<QuestionResult>()
            if (obj.has("question_details")) {
                val array = obj.getJSONArray("question_details")
                for (i in 0 until array.length()) {
                    val qObj = array.getJSONObject(i)
                    details.add(
                        QuestionResult(
                            questionNumber = qObj.optInt("number", i + 1),
                            studentAnswer = qObj.optString("student_answer", "BELGILANMAGAN"),
                            correctAnswer = qObj.optString("correct_answer", ""),
                            isCorrect = qObj.optBoolean("is_correct", false)
                        )
                    )
                }
            }

            return OmrCheckResult(
                status = status,
                student_id = studentId,
                total_questions = totalQuestions,
                correct_count = correctCount,
                incorrect_count = incorrectCount,
                score_percentage = scorePercentage,
                question_details = details,
                raw_model_response = clean
            )
        }
    }
}

data class QuestionResult(
    val questionNumber: Int,
    val studentAnswer: String, // "A", "B", "C", "D", or "BELGILANMAGAN"
    val correctAnswer: String, // "A", "B", "C", "D"
    val isCorrect: Boolean
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("number", questionNumber)
        obj.put("student_answer", studentAnswer)
        obj.put("correct_answer", correctAnswer)
        obj.put("is_correct", isCorrect)
        return obj
    }
}
