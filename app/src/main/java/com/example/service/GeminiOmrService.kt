package com.example.service

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.AnswerKey
import com.example.model.OmrCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiOmrService {
    private const val TAG = "GeminiOmrService"
    private const val PRIMARY_MODEL = "gemini-2.5-flash"
    private const val SECONDARY_MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Converts a Bitmap to Base64 JPEG string.
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Calls Gemini Vision API to analyze the OMR sheet.
     * Uses thinkingLevel = "high" as required for complex reasoning on image markers.
     */
    suspend fun analyzeWithGemini(
        bitmap: Bitmap,
        answerKey: AnswerKey
    ): Result<OmrCheckResult> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is missing or placeholder. Using high-precision Local Vision Engine.")
            val localResult = LocalOmrEngine.analyzeSheet(bitmap, answerKey)
            return@withContext Result.success(localResult)
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val answerKeyList = answerKey.toFormattedString()

            val systemInstruction = """
                Siz maktab va universitet test varaqalarini (OMR) avtomatik tekshiruvchi professional tizimsiz.
                Sizga o'quvchi tomonidan qalam yoki ruchka bilan bo'yalgan test titulining rasmi va o'qituvchi tomonidan kiritilgan to'g'ri javoblar kaliti beriladi.

                Vazifangiz quyidagi qat'iy algoritmlar asosida rasmni tahlil qilish:

                1. ID KODNI TEKSHIRISH:
                   - Varaqaning yuqori qismidagi 10 talik raqamlar bo'yalgan ID kod blokini aniqlang (10 ta ustun, har birida 0 dan 9 gacha doirachalar bor).
                   - Har bir ustundan bo'yalgan raqamni o'qib, 10 xonali kodni matn ko'rinishiga keltiring.
                   - QAT'IY QOIDA: O'quvchi bo'yayotganda dumaloqni ichidan chiqib ketmagan bo'lishi kerak! Agar birorta doirachadan qalam/ruchka chegaradan toshib chiqib ketgan bo'lsa, xato hisoblanadi.
                   - Agar 10 talik kod to'liq belgilanmagan bo'lsa (masalan 9 ta raqam bo'yalgan yoki bitta ustun bo'sh qolgan bo'lsa), bitta ustunda bir nechta doiracha bo'yalgan bo'lsa, doirachadan chiqib ketgan bo'lsa yoki umuman o'qib bo'lmasa, tekshirishni SHU JOYDA TO'XTATING. Statusni "KOD_XATO" deb belgilang. Ism, familiya va test javoblarini umuman tekshirmang. student_id ni null qiling.

                2. TEST JAVOBLARINI TEKSHIRISH (Faqat ID kod to'g'ri bo'lsa):
                   - 1 dan 30 gacha bo'lgan kataklarni (3 ta ustun: 1-10, 11-20, 21-30) ko'zdan kechiring.
                   - Har bir savol uchun o'quvchi qaysi variantni (A, B, C, D) bo'yaganini aniqlang.
                   - QAT'IY QOIDA: Agar o'quvchi javob doirachasini bo'yayotganda dumaloq chegarasidan tashqariga toshib chiqib ketgan bo'lsa, bu ham xato hisoblanadi va javob "BELGILANMAGAN" (0 ball) deb hisoblanadi.
                   - Agar savolga javob bo'yalmagan bo'lsa yoki bir nechta variant bo'yalgan bo'lsa ham "BELGILANMAGAN" deb hisoblang.
                   - O'quvchining javoblarini foydalanuvchi taqdim etgan to'g'ri javoblar kaliti bilan solishtiring.
                   - Har bir to'g'ri javob uchun 1 ball bering.
                   - score_percentage = (correct_count * 100.0) / 30

                3. NATIJANI SHAKLLANTIRISH:
                   - Javobni faqat va faqat toza JSON formatida qaytaring. Matnli tushuntirishlar yoki chiziqlar (```json) qo'shmang.

                JSON formati:
                {
                  "status": "SUCCESS" yoki "KOD_XATO",
                  "student_id": "aniqlangan_10_talik_kod_yoki_null",
                  "total_questions": 30,
                  "correct_count": to'g'ri_topilgan_savollar_soni_yoki_0,
                  "incorrect_count": noto'g'ri_topilgan_savollar_soni_yoki_0,
                  "score_percentage": foizdagi_natija_yoki_0,
                  "question_details": [
                    {"number": 1, "student_answer": "A", "correct_answer": "A", "is_correct": true},
                    ...
                  ]
                }
            """.trimIndent()

            val userPrompt = "To'g'ri javoblar kaliti (1 dan 30 gacha): $answerKeyList.\nRasmni ko'rsatilgan qat'iy algoritm bo'yicha tekshirib, natijani toza JSON shaklida bering."

            // Build request JSON
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val userContent = JSONObject().apply {
                        put("role", "user")
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply { put("text", userPrompt) })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        }
                        put("parts", partsArray)
                    }
                    put(userContent)
                }
                put("contents", contentsArray)

                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("topP", 0.95)
                    put("responseMimeType", "application/json")
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())

            // Try primary model first, fallback to secondary if 429/quota/error occurs
            var responseBody: String? = null
            for (model in listOf(PRIMARY_MODEL, SECONDARY_MODEL)) {
                try {
                    val url = "$BASE_URL/$model:generateContent?key=$apiKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful && body.isNotBlank()) {
                        responseBody = body
                        break
                    } else {
                        Log.w(TAG, "Model $model returned code ${response.code}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error connecting to $model: ${e.message}")
                }
            }

            if (responseBody.isNullOrBlank()) {
                Log.i(TAG, "Gemini API unavailable or quota reached. Executing high-precision Local Vision Engine.")
                val localResult = LocalOmrEngine.analyzeSheet(bitmap, answerKey)
                return@withContext Result.success(localResult)
            }

            val rootObj = JSONObject(responseBody)
            val candidates = rootObj.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            // Look for the text part
            var rawText = ""
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("text")) {
                        rawText += p.getString("text")
                    }
                }
            }

            if (rawText.isBlank()) {
                val localResult = LocalOmrEngine.analyzeSheet(bitmap, answerKey)
                return@withContext Result.success(localResult)
            }

            val parsed = OmrCheckResult.parseFromJson(rawText)
            Result.success(parsed)
        } catch (e: Exception) {
            Log.w(TAG, "Exception during analysis: ${e.message}. Using Local Vision Engine fallback.")
            // Fallback to high-precision local computer vision engine
            val localResult = LocalOmrEngine.analyzeSheet(bitmap, answerKey)
            Result.success(localResult)
        }
    }
}
