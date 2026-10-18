package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.SettlementRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlement_records WHERE tripId = :tripId ORDER BY settledDate DESC")
    fun getSettlementRecordsForTrip(tripId: Long): Flow<List<SettlementRecordEntity>>

    @Query("SELECT * FROM settlement_records WHERE tripId = :tripId ORDER BY settledDate DESC")
    suspend fun getSettlementRecordsForTripOnce(tripId: Long): List<SettlementRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlementRecord(record: SettlementRecordEntity): Long

    @Delete
    suspend fun deleteSettlementRecord(record: SettlementRecordEntity)
}
