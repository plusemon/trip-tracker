package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates WHERE tripId = :tripId ORDER BY fromCurrency ASC")
    fun getRatesForTrip(tripId: Long): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates WHERE tripId = :tripId ORDER BY fromCurrency ASC")
    suspend fun getRatesForTripOnce(tripId: Long): List<ExchangeRateEntity>

    @Query("SELECT * FROM exchange_rates WHERE tripId = :tripId AND fromCurrency = :fromCurr AND toCurrency = :toCurr LIMIT 1")
    suspend fun getRate(tripId: Long, fromCurr: String, toCurr: String): ExchangeRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: ExchangeRateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRateEntity>)

    @Update
    suspend fun updateRate(rate: ExchangeRateEntity)

    @Delete
    suspend fun deleteRate(rate: ExchangeRateEntity)
}
