package com.example.service

import android.util.Log
import com.example.BuildConfig
import com.example.data.OmrScanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Skanerlash natijalarini Supabase (PostgreSQL) jadvaliga to'g'ridan-to'g'ri
 * qurilmadan sinxronlaydigan servis. Supabase'ning PostgREST REST API'sidan
 * foydalanadi, shu sababli qo'shimcha Supabase SDK/Kotlin kutubxonasi shart emas
 * (loyihada allaqachon mavjud bo'lgan OkHttp ishlatiladi).
 */
object SupabaseSyncService {
    private const val TAG = "SupabaseSyncService"
    private const val TABLE_NAME = "omr_scan_results"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val supabaseUrl: String
        get() = try { BuildConfig.SUPABASE_URL.trim().trimEnd('/') } catch (e: Throwable) { "" }

    private val supabaseAnonKey: String
        get() = try { BuildConfig.SUPABASE_ANON_KEY.trim() } catch (e: Throwable) { "" }

    /** Supabase URL va kaliti haqiqiy qiymat bilan (placeholder emas) sozlanganmi. */
    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() &&
            supabaseUrl != "MY_SUPABASE_URL" &&
            supabaseAnonKey.isNotBlank() &&
            supabaseAnonKey != "MY_SUPABASE_ANON_KEY"

    /**
     * Bitta skanerlash natijasini Supabase'dagi `omr_scan_results` jadvaliga yuboradi.
     * `device_id` + `device_scan_id` bo'yicha upsert qilinadi (qayta yuborilsa ham dublikat bo'lmaydi).
     *
     * @return muvaffaqiyat bo'lsa Supabase tomonidan qaytarilgan qator UUID'si.
     */
    suspend fun uploadScan(scan: OmrScanEntity, deviceId: String): Result<String?> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Supabase sozlanmagan. .env fayliga SUPABASE_URL va SUPABASE_ANON_KEY qiymatlarini kiriting."
                )
            )
        }

        try {
            val body = JSONObject().apply {
                put("device_id", deviceId)
                put("device_scan_id", scan.id)
                put("status", scan.status)
                put("student_id", scan.studentId ?: JSONObject.NULL)
                put("total_questions", scan.totalQuestions)
                put("correct_count", scan.correctCount)
                put("incorrect_count", scan.incorrectCount)
                put("score_percentage", scan.scorePercentage)
                put("raw_json", scan.rawJson)
                put("question_details_json", scan.questionDetailsJson ?: JSONObject.NULL)
            }

            val requestBody = body.toString().toRequestBody("application/json".toMediaType())
            val url = "$supabaseUrl/rest/v1/$TABLE_NAME?on_conflict=device_id,device_scan_id"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation,resolution=merge-duplicates")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w(TAG, "Supabase upload xatosi: ${response.code} $responseBody")
                    return@withContext Result.failure(Exception("Supabase xatosi (${response.code}): $responseBody"))
                }
                val remoteId = try {
                    val array = JSONArray(responseBody)
                    if (array.length() > 0) array.getJSONObject(0).optString("id") else null
                } catch (e: Exception) {
                    null
                }
                Result.success(remoteId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supabase'ga ulanishda xatolik: ${e.message}")
            Result.failure(e)
        }
    }
}
