package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "omr_scans")
data class OmrScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "SUCCESS" or "KOD_XATO"
    val studentId: String?,
    val totalQuestions: Int = 30,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val scorePercentage: Double = 0.0,
    val rawJson: String,
    val questionDetailsJson: String? = null,
    val imagePath: String? = null,
    val syncedToSupabase: Boolean = false,
    val supabaseId: String? = null
)
