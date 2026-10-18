package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["payerMemberId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["payerMemberId"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val title: String,
    val amount: Double, // in base currency
    val originalAmount: Double = amount,
    val currencyCode: String = "BDT",
    val exchangeRate: Double = 1.0,
    val category: ExpenseCategory = ExpenseCategory.FOOD,
    val customCategoryName: String? = null,
    val customCategoryIcon: String? = null,
    val customCategoryColor: String? = null,
    val isIncome: Boolean = false,
    val date: Long = System.currentTimeMillis(),
    val payerType: PayerType = PayerType.COMMON_FUND,
    val payerMemberId: Long? = null,
    val splitMode: SplitMode = SplitMode.EQUAL,
    val notes: String = ""
)
