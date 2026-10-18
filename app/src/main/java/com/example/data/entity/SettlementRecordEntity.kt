package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "settlement_records",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["fromMemberId"]),
        Index(value = ["toMemberId"])
    ]
)
data class SettlementRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val fromMemberId: Long,
    val toMemberId: Long,
    val fromMemberName: String = "",
    val toMemberName: String = "",
    val amount: Double,
    val settledDate: Long = System.currentTimeMillis(),
    val notes: String = ""
)
