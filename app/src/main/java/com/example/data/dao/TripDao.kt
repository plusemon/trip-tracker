package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    suspend fun getAllTripsOnce(): List<TripEntity>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun getTripById(tripId: Long): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripByIdOnce(tripId: Long): TripEntity?

    @Query("SELECT * FROM trips WHERE tripCode = :tripCode LIMIT 1")
    suspend fun getTripByCode(tripCode: String): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)
}
