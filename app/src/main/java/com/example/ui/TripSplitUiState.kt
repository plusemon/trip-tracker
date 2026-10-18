package com.example.ui

import com.example.data.entity.DepositEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.MemberEntity
import com.example.data.entity.SettlementRecordEntity
import com.example.data.entity.TripEntity
import com.example.domain.model.CategorySpending
import com.example.domain.model.MemberBalance
import com.example.domain.model.SettlementTransaction
import com.example.domain.model.TripSummary

enum class ActiveDialog {
    NONE,
    ADD_EXPENSE,
    ADD_DEPOSIT,
    CREATE_TRIP,
    EXPORT,
    RECORD_SETTLEMENT
}

data class TripSplitUiState(
    val isLoading: Boolean = true,
    val allTrips: List<TripEntity> = emptyList(),
    val currentTrip: TripEntity? = null,
    val members: List<MemberEntity> = emptyList(),
    val deposits: List<DepositEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val splits: List<ExpenseSplitEntity> = emptyList(),
    val settlements: List<SettlementRecordEntity> = emptyList(),
    val memberBalances: List<MemberBalance> = emptyList(),
    val settlementSteps: List<SettlementTransaction> = emptyList(),
    val tripSummary: TripSummary? = null,
    val categorySpendings: List<CategorySpending> = emptyList(),
    val selectedTab: Int = 0, // 0: Dashboard, 1: Expenses, 2: Settle Up, 3: Analytics
    val activeDialog: ActiveDialog = ActiveDialog.NONE,
    val userMessage: String? = null
)
