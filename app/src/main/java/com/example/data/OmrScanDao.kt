package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OmrScanDao {
    @Query("SELECT * FROM omr_scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<OmrScanEntity>>

    @Query("SELECT * FROM omr_scans ORDER BY timestamp ASC")
    fun getAllScansAsc(): Flow<List<OmrScanEntity>>

    @Query("SELECT * FROM omr_scans ORDER BY scorePercentage DESC")
    fun getAllScansByScoreDesc(): Flow<List<OmrScanEntity>>

    @Query("SELECT * FROM omr_scans WHERE id = :id LIMIT 1")
    suspend fun getScanById(id: Long): OmrScanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: OmrScanEntity): Long

    @Delete
    suspend fun deleteScan(scan: OmrScanEntity)

    @Query("DELETE FROM omr_scans")
    suspend fun deleteAllScans()
}
