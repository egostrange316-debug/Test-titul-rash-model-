package com.example.data

import com.example.model.OmrCheckResult
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

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
}
