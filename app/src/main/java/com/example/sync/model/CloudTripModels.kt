package com.example.sync.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class TripCloudMetadata(
    val tripId: String = "",
    val tripCode: String = "",
    val tripName: String = "",
    val leaderDeviceId: String = "",
    val baseCurrency: String = "BDT",
    val targetBudget: Double = 0.0,
    val updatedAt: Long = 0L
)

@IgnoreExtraProperties
data class CloudMember(
    val id: String = "",
    val name: String = "",
    val deposited: Double = 0.0
)

@IgnoreExtraProperties
data class CloudTransaction(
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "Other",
    val paidBy: String = "Group Fund",
    val timestamp: Long = 0L
)

@IgnoreExtraProperties
data class CloudSettlement(
    val payer: String = "",
    val receiver: String = "",
    val amount: Double = 0.0,
    val isSettled: Boolean = false
)

@IgnoreExtraProperties
data class CloudTrip(
    val metadata: TripCloudMetadata = TripCloudMetadata(),
    val members: List<CloudMember> = emptyList(),
    val transactions: Map<String, CloudTransaction> = emptyMap(),
    val settlements: List<CloudSettlement> = emptyList()
)
