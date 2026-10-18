package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val destination: String,
    val startDate: Long,
    val endDate: Long,
    val budget: Double = 0.0,
    val currencySymbol: String = "৳",
    val managerName: String = "Tour Manager",
    val createdAt: Long = System.currentTimeMillis()
)
