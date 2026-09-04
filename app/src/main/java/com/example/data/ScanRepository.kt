package com.example.data

import com.example.model.OmrCheckResult
import com.example.service.SupabaseSyncService
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

/**
 * Supabase'ga sinxronlash natijasi bo'yicha qisqacha hisobot.
 */
data class SyncSummary(val success: Int, val failed: Int, val total: Int)

class ScanRepository(private val dao: OmrScanDao) {
    val allScans: Flow<List<OmrScanEntity>> = dao.getAllScans()

    suspend fun saveScanResult(result: OmrCheckResult, imagePath: String? = null): Long {
        val detailsJson = if (result.question_details.isNotEmpty()) {
            val arr = JSONArray()
            for (item in result.question_details) {
                arr.put(item.toJson())
            }
            arr.toString()
        } else null

        val entity = OmrScanEntity(
            status = result.status,
            studentId = result.student_id,
            totalQuestions = result.total_questions,
            correctCount = result.correct_count,
            incorrectCount = result.incorrect_count,
            scorePercentage = result.score_percentage,
            rawJson = result.toCleanJson(),
            questionDetailsJson = detailsJson,
            imagePath = imagePath
        )
        return dao.insertScan(entity)
    }

    suspend fun deleteScan(scan: OmrScanEntity) {
        dao.deleteScan(scan)
    }

    suspend fun clearAll() {
        dao.deleteAllScans()
    }

    /**
     * Hali Supabase'ga yuborilmagan barcha skanerlash natijalarini bulutga sinxronlaydi.
     * Har bir yozuv `device_id` + `device_scan_id` bo'yicha upsert qilinadi, shu sababli
     * qayta chaqirilsa ham dublikat yaratilmaydi.
     */
    suspend fun syncPendingScans(deviceId: String): SyncSummary {
        val pending = dao.getUnsyncedScans()
        var success = 0
        var failed = 0
        for (scan in pending) {
            val result = SupabaseSyncService.uploadScan(scan, deviceId)
            if (result.isSuccess) {
                dao.markSynced(scan.id, result.getOrNull())
                success++
            } else {
                failed++
            }
        }
        return SyncSummary(success = success, failed = failed, total = pending.size)
    }
}
