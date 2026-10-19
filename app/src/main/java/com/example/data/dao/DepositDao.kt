package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.DepositEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DepositDao {
    @Query("SELECT * FROM deposits WHERE tripId = :tripId ORDER BY date DESC")
    fun getDepositsForTrip(tripId: Long): Flow<List<DepositEntity>>

    @Query("SELECT * FROM deposits WHERE tripId = :tripId ORDER BY date DESC")
    suspend fun getDepositsForTripOnce(tripId: Long): List<DepositEntity>

    @Query("SELECT SUM(amount) FROM deposits WHERE tripId = :tripId")
    fun getTotalDepositsForTrip(tripId: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeposit(deposit: DepositEntity): Long

    @Update
    suspend fun updateDeposit(deposit: DepositEntity)

    @Delete
    suspend fun deleteDeposit(deposit: DepositEntity)

    @Query("DELETE FROM deposits WHERE tripId = :tripId")
    suspend fun deleteDepositsForTrip(tripId: Long)
}
