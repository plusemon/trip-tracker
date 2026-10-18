package com.example.ui

import com.example.data.entity.CategoryEntity
import com.example.data.entity.DepositEntity
import com.example.data.entity.ExchangeRateEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpensePayerEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.MemberEntity
import com.example.data.entity.ReceiptAttachmentEntity
import com.example.data.entity.SettlementRecordEntity
import com.example.data.entity.TripEntity
import com.example.data.entity.TripStatus
import com.example.domain.model.CategorySpending
import com.example.domain.model.MemberBalance
import com.example.domain.model.SettlementTransaction
import com.example.domain.model.TripSummary
import java.io.File

enum class ActiveDialog {
    NONE,
    ADD_EXPENSE,
    ADD_INCOME,
    CREATE_TRIP,
    EDIT_TRIP,
    MANAGE_TRIPS,
    MANAGE_CATEGORIES,
    CREATE_CATEGORY,
    MANAGE_EXCHANGE_RATES,
    EXPORT,
    BACKUP_RESTORE,
    RECORD_SETTLEMENT,
    VIEW_PHOTO,
    ABOUT_DEV
}

data class TripSplitUiState(
    val isLoading: Boolean = true,
    val allTrips: List<TripEntity> = emptyList(),
    val currentTrip: TripEntity? = null,
    val members: List<MemberEntity> = emptyList(),
    val deposits: List<DepositEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val splits: List<ExpenseSplitEntity> = emptyList(),
    val payers: List<ExpensePayerEntity> = emptyList(),
    val attachments: List<ReceiptAttachmentEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val exchangeRates: List<ExchangeRateEntity> = emptyList(),
    val settlements: List<SettlementRecordEntity> = emptyList(),
    val memberBalances: List<MemberBalance> = emptyList(),
    val settlementSteps: List<SettlementTransaction> = emptyList(),
    val tripSummary: TripSummary? = null,
    val categorySpendings: List<CategorySpending> = emptyList(),
    val selectedTab: Int = 0, // 0: Dashboard, 1: Expenses/Incomes, 2: Settle Up, 3: Analytics, 4: Settings
    val activeDialog: ActiveDialog = ActiveDialog.NONE,
    val viewingPhotoPath: String? = null,
    val userMessage: String? = null
)
