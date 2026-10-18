package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.entity.DepositEntity
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.MemberEntity
import com.example.data.entity.PayerType
import com.example.data.entity.SettlementRecordEntity
import com.example.data.entity.SplitMode
import com.example.data.entity.TripEntity
import com.example.data.repository.TripSplitRepository
import com.example.domain.engine.SettlementEngine
import com.example.domain.model.CategorySpending
import com.example.domain.model.TripSummary
import com.example.export.PdfReportGenerator
import com.example.export.ShareSummaryFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class TripSplitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TripSplitRepository(AppDatabase.getInstance(application))

    private val _uiState = MutableStateFlow(TripSplitUiState())
    val uiState: StateFlow<TripSplitUiState> = _uiState.asStateFlow()

    private val _currentTripId = MutableStateFlow<Long?>(null)

    init {
        // Observe all trips
        viewModelScope.launch {
            repository.allTrips.collectLatest { trips ->
                _uiState.update { it.copy(allTrips = trips, isLoading = false) }
                if (_currentTripId.value == null && trips.isNotEmpty()) {
                    _currentTripId.value = trips.first().id
                }
            }
        }

        // Observe current trip details reactively
        viewModelScope.launch {
            _currentTripId.collectLatest { tripId ->
                if (tripId == null) {
                    _uiState.update {
                        it.copy(
                            currentTrip = null,
                            members = emptyList(),
                            deposits = emptyList(),
                            expenses = emptyList(),
                            splits = emptyList(),
                            settlements = emptyList(),
                            memberBalances = emptyList(),
                            settlementSteps = emptyList(),
                            tripSummary = null,
                            categorySpendings = emptyList()
                        )
                    }
                } else {
                    val baseDataFlow = combine(
                        repository.getTrip(tripId),
                        repository.getMembers(tripId),
                        repository.getDeposits(tripId),
                        repository.getExpenses(tripId),
                        repository.getAllSplits(tripId)
                    ) { trip, members, deposits, expenses, splits ->
                        TripBaseData(trip, members, deposits, expenses, splits)
                    }

                    baseDataFlow.combine(repository.getSettlements(tripId)) { base, settlements ->
                        recalculateState(base.trip, base.members, base.deposits, base.expenses, base.splits, settlements)
                    }.collectLatest { calculatedState ->
                        _uiState.update { current ->
                            current.copy(
                                currentTrip = calculatedState.currentTrip,
                                members = calculatedState.members,
                                deposits = calculatedState.deposits,
                                expenses = calculatedState.expenses,
                                splits = calculatedState.splits,
                                settlements = calculatedState.settlements,
                                memberBalances = calculatedState.memberBalances,
                                settlementSteps = calculatedState.settlementSteps,
                                tripSummary = calculatedState.tripSummary,
                                categorySpendings = calculatedState.categorySpendings
                            )
                        }
                    }
                }
            }
        }
    }

    private fun recalculateState(
        trip: TripEntity?,
        members: List<MemberEntity>,
        deposits: List<DepositEntity>,
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
        settlements: List<SettlementRecordEntity>
    ): TripSplitUiState {
        if (trip == null) {
            return TripSplitUiState(currentTrip = null)
        }

        // 1. Calculate Member Balances
        val balances = SettlementEngine.calculateMemberBalances(members, deposits, expenses, splits)

        // 2. Calculate Minimized Settlements
        val settlementSteps = SettlementEngine.calculateMinimizedSettlements(
            members = members,
            deposits = deposits,
            expenses = expenses,
            splits = splits,
            settledRecords = settlements
        )

        // 3. Category Spendings
        val totalSpentOverall = expenses.sumOf { it.amount }
        val categorySpendings = ExpenseCategory.values().map { cat ->
            val catExpenses = expenses.filter { it.category == cat }
            val amount = catExpenses.sumOf { it.amount }
            val pct = if (totalSpentOverall > 0) ((amount / totalSpentOverall) * 100).toFloat() else 0f
            CategorySpending(
                category = cat,
                totalAmount = amount,
                percentage = pct,
                count = catExpenses.size
            )
        }.filter { it.totalAmount > 0 }.sortedByDescending { it.totalAmount }

        // 4. Common Fund & Summary
        val totalDeposited = deposits.sumOf { it.amount }
        val commonFundSpent = expenses.filter { it.payerType == PayerType.COMMON_FUND }.sumOf { it.amount }
        val remainingCommonFund = totalDeposited - commonFundSpent

        val highestCategory = categorySpendings.firstOrNull()?.category
        val highestCategoryAmount = categorySpendings.firstOrNull()?.totalAmount ?: 0.0

        val topSpenderBalance = balances.maxByOrNull { it.totalPaid }
        val topSpender = if ((topSpenderBalance?.totalPaid ?: 0.0) > 0.0) topSpenderBalance?.member else null
        val topSpenderAmount = topSpenderBalance?.totalPaid ?: 0.0

        val budgetProgress = if (trip.budget > 0) {
            ((totalSpentOverall / trip.budget).toFloat()).coerceIn(0f, 1f)
        } else 0f

        val summary = TripSummary(
            trip = trip,
            totalDeposited = totalDeposited,
            commonFundSpent = commonFundSpent,
            remainingCommonFund = remainingCommonFund,
            totalSpentOverall = totalSpentOverall,
            budget = trip.budget,
            budgetProgress = budgetProgress,
            highestCategory = highestCategory,
            highestCategoryAmount = highestCategoryAmount,
            topSpender = topSpender,
            topSpenderAmount = topSpenderAmount,
            memberCount = members.size,
            expenseCount = expenses.size
        )

        return TripSplitUiState(
            currentTrip = trip,
            members = members,
            deposits = deposits,
            expenses = expenses,
            splits = splits,
            settlements = settlements,
            memberBalances = balances,
            settlementSteps = settlementSteps,
            tripSummary = summary,
            categorySpendings = categorySpendings
        )
    }

    fun selectTrip(tripId: Long) {
        _currentTripId.value = tripId
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun showDialog(dialog: ActiveDialog) {
        _uiState.update { it.copy(activeDialog = dialog) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(activeDialog = ActiveDialog.NONE) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    // CRUD Actions
    fun createTripDetailed(
        name: String,
        destination: String,
        startDate: Long,
        endDate: Long,
        budget: Double,
        currencySymbol: String,
        managerName: String,
        members: List<com.example.domain.model.NewMemberInfo>
    ) {
        viewModelScope.launch {
            val validMembers = members.filter { it.name.isNotBlank() }
            val determinedManager = managerName.ifBlank {
                validMembers.firstOrNull { it.isManager }?.name
                    ?: validMembers.firstOrNull()?.name
                    ?: "Tour Lead"
            }
            val trip = TripEntity(
                name = name.ifBlank { "Tour ${destination.ifBlank { "Trip" }}" },
                destination = destination.ifBlank { "Bangladesh" },
                startDate = startDate,
                endDate = endDate,
                budget = budget,
                currencySymbol = currencySymbol.ifBlank { "৳" },
                managerName = determinedManager
            )
            val tripId = repository.createTripWithDetailedMembers(trip, validMembers)
            _currentTripId.value = tripId
            _uiState.update { it.copy(activeDialog = ActiveDialog.NONE, userMessage = "Trip created successfully with ${validMembers.size} members!") }
        }
    }

    fun createTrip(
        name: String,
        destination: String,
        startDate: Long,
        endDate: Long,
        budget: Double,
        currencySymbol: String,
        managerName: String,
        memberNames: List<String>
    ) {
        val detailed = memberNames.mapIndexed { idx, nameStr ->
            com.example.domain.model.NewMemberInfo(
                name = nameStr,
                isManager = (idx == 0)
            )
        }
        createTripDetailed(
            name = name,
            destination = destination,
            startDate = startDate,
            endDate = endDate,
            budget = budget,
            currencySymbol = currencySymbol,
            managerName = managerName,
            members = detailed
        )
    }

    fun loadSampleBandarbanTour() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val tripId = repository.loadBandarbanSample()
            _currentTripId.value = tripId
            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeDialog = ActiveDialog.NONE,
                    userMessage = "Bandarban Expedition sample loaded!"
                )
            }
        }
    }

    fun addDeposit(memberId: Long, amount: Double, notes: String) {
        val tripId = _currentTripId.value ?: return
        viewModelScope.launch {
            repository.addDeposit(
                DepositEntity(
                    tripId = tripId,
                    memberId = memberId,
                    amount = amount,
                    notes = notes
                )
            )
            _uiState.update { it.copy(activeDialog = ActiveDialog.NONE, userMessage = "Deposit added to Common Fund!") }
        }
    }

    fun addExpense(
        title: String,
        amount: Double,
        category: ExpenseCategory,
        date: Long,
        payerType: PayerType,
        payerMemberId: Long?,
        splitMode: SplitMode,
        customShares: Map<Long, Double>,
        selectedMemberIds: Set<Long>,
        notes: String
    ) {
        val tripId = _currentTripId.value ?: return
        viewModelScope.launch {
            val expense = ExpenseEntity(
                tripId = tripId,
                title = title,
                amount = amount,
                category = category,
                date = date,
                payerType = payerType,
                payerMemberId = if (payerType == PayerType.MEMBER) payerMemberId else null,
                splitMode = splitMode,
                notes = notes
            )

            val splits = when (splitMode) {
                SplitMode.EQUAL -> {
                    val allMembers = _uiState.value.members
                    if (allMembers.isNotEmpty()) {
                        val share = amount / allMembers.size
                        allMembers.map { ExpenseSplitEntity(0, 0, it.id, share) }
                    } else emptyList()
                }
                SplitMode.SELECTIVE -> {
                    val payees = _uiState.value.members.filter { selectedMemberIds.contains(it.id) }
                    if (payees.isNotEmpty()) {
                        val share = amount / payees.size
                        payees.map { ExpenseSplitEntity(0, 0, it.id, share) }
                    } else emptyList()
                }
                SplitMode.CUSTOM -> {
                    customShares.map { (mId, share) ->
                        ExpenseSplitEntity(0, 0, mId, share)
                    }
                }
            }

            repository.addExpenseWithSplits(expense, splits)
            _uiState.update { it.copy(activeDialog = ActiveDialog.NONE, userMessage = "Expense recorded successfully!") }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _uiState.update { it.copy(userMessage = "Expense deleted") }
        }
    }

    fun deleteDeposit(deposit: DepositEntity) {
        viewModelScope.launch {
            repository.deleteDeposit(deposit)
            _uiState.update { it.copy(userMessage = "Deposit removed") }
        }
    }

    fun recordSettlement(
        fromMemberId: Long,
        toMemberId: Long,
        fromMemberName: String,
        toMemberName: String,
        amount: Double,
        notes: String = ""
    ) {
        val tripId = _currentTripId.value ?: return
        viewModelScope.launch {
            repository.recordSettlement(
                SettlementRecordEntity(
                    tripId = tripId,
                    fromMemberId = fromMemberId,
                    toMemberId = toMemberId,
                    fromMemberName = fromMemberName,
                    toMemberName = toMemberName,
                    amount = amount,
                    notes = notes
                )
            )
            _uiState.update { it.copy(activeDialog = ActiveDialog.NONE, userMessage = "Settlement payment recorded!") }
        }
    }

    fun generateWhatsAppText(): String {
        val state = _uiState.value
        val trip = state.currentTrip ?: return ""
        val summary = state.tripSummary
        return ShareSummaryFormatter.generateWhatsAppText(
            trip = trip,
            balances = state.memberBalances,
            settlements = state.settlementSteps,
            expenses = state.expenses,
            totalDeposits = summary?.totalDeposited ?: 0.0,
            commonFundSpent = summary?.commonFundSpent ?: 0.0,
            remainingCommonFund = summary?.remainingCommonFund ?: 0.0
        )
    }

    fun exportPdfReport(): File? {
        val state = _uiState.value
        val trip = state.currentTrip ?: return null
        val summary = state.tripSummary
        return runCatching {
            PdfReportGenerator.generateReport(
                context = getApplication(),
                trip = trip,
                balances = state.memberBalances,
                settlements = state.settlementSteps,
                expenses = state.expenses,
                totalDeposits = summary?.totalDeposited ?: 0.0,
                commonFundSpent = summary?.commonFundSpent ?: 0.0,
                remainingCommonFund = summary?.remainingCommonFund ?: 0.0
            )
        }.getOrNull()
    }
}

private data class TripBaseData(
    val trip: TripEntity?,
    val members: List<MemberEntity>,
    val deposits: List<DepositEntity>,
    val expenses: List<ExpenseEntity>,
    val splits: List<ExpenseSplitEntity>
)
