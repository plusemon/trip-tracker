package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.entity.CategoryEntity
import com.example.data.entity.DepositEntity
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpensePayerEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.MemberEntity
import com.example.data.entity.PayerType
import com.example.data.entity.ReceiptAttachmentEntity
import com.example.data.entity.SettlementRecordEntity
import com.example.data.entity.SplitMode
import com.example.data.entity.TripEntity
import com.example.data.entity.TripStatus
import com.example.data.repository.TripSplitRepository
import com.example.data.util.ImageStorageManager
import com.example.domain.engine.SettlementEngine
import com.example.domain.model.CategorySpending
import com.example.domain.model.NewMemberInfo
import com.example.domain.model.TripSummary
import com.example.export.JsonBackupManager
import com.example.export.PdfReportGenerator
import com.example.export.ShareSummaryFormatter
import com.example.export.TripBackupBundle
import com.example.sync.FirebaseSyncManager
import com.example.util.DeviceIdHelper
import com.example.util.TripCodeGenerator
import com.google.firebase.database.ValueEventListener
import android.util.Log
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

    private var activeRemoteTripCode: String? = null
    private var activeRemoteListener: ValueEventListener? = null

    init {
        // 1. Observe all trips
        viewModelScope.launch {
            repository.allTrips.collectLatest { trips ->
                _uiState.update { it.copy(allTrips = trips, isLoading = false) }
                if (trips.isEmpty()) {
                    val context = getApplication<Application>()
                    val backupFile = File(context.filesDir, "TripTracker_AutoBackup.json")
                    if (backupFile.exists() && backupFile.length() > 0) {
                        runCatching {
                            val jsonString = backupFile.readText()
                            val bundle = JsonBackupManager.parseTripFromJson(jsonString)
                            if (bundle != null) {
                                val newId = repository.importTripBundle(bundle)
                                _currentTripId.value = newId
                                Log.i("TripSplitVM", "Successfully auto-restored trip from local backup after update/reinstall!")
                            } else {
                                tryRestoreFromFirebase(context)
                            }
                        }.onFailure {
                            tryRestoreFromFirebase(context)
                        }
                    } else {
                        tryRestoreFromFirebase(context)
                    }
                } else if (_currentTripId.value == null) {
                    // Default to first active trip or first trip
                    val activeTrip = trips.find { it.status == TripStatus.ACTIVE } ?: trips.first()
                    _currentTripId.value = activeTrip.id
                }
            }
        }

        // 2. Observe current trip details reactively
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
                            payers = emptyList(),
                            attachments = emptyList(),
                            categories = emptyList(),
                            settlements = emptyList(),
                            memberBalances = emptyList(),
                            settlementSteps = emptyList(),
                            tripSummary = null,
                            categorySpendings = emptyList()
                        )
                    }
                } else {
                    val flow1 = combine(
                        repository.getTrip(tripId),
                        repository.getMembers(tripId),
                        repository.getDeposits(tripId),
                        repository.getExpenses(tripId),
                        repository.getAllSplits(tripId)
                    ) { trip, members, deposits, expenses, splits ->
                        Triple(trip, Pair(members, deposits), Pair(expenses, splits))
                    }

                    val flow2 = combine(
                        repository.getAllPayers(tripId),
                        repository.getAllAttachments(tripId),
                        repository.getCategories(tripId),
                        repository.getSettlements(tripId)
                    ) { payers, attachments, categories, settlements ->
                        Triple(payers, attachments, Pair(categories, settlements))
                    }

                    flow1.combine(flow2) { f1, f2 ->
                        val trip = f1.first
                        val members = f1.second.first
                        val deposits = f1.second.second
                        val expenses = f1.third.first
                        val splits = f1.third.second

                        val payers = f2.first
                        val attachments = f2.second
                        val categories = f2.third.first
                        val settlements = f2.third.second

                        recalculateState(
                            trip = trip,
                            members = members,
                            deposits = deposits,
                            expenses = expenses,
                            splits = splits,
                            payers = payers,
                            attachments = attachments,
                            categories = categories,
                            settlements = settlements
                        )
                    }.collectLatest { calculated ->
                        _uiState.update { current ->
                            current.copy(
                                currentTrip = calculated.currentTrip,
                                members = calculated.members,
                                deposits = calculated.deposits,
                                expenses = calculated.expenses,
                                splits = calculated.splits,
                                payers = calculated.payers,
                                attachments = calculated.attachments,
                                categories = calculated.categories,
                                settlements = calculated.settlements,
                                memberBalances = calculated.memberBalances,
                                settlementSteps = calculated.settlementSteps,
                                tripSummary = calculated.tripSummary,
                                categorySpendings = calculated.categorySpendings
                            )
                        }

                        val trip = calculated.currentTrip
                        if (trip != null) {
                            exportAndSaveAutoBackup(
                                trip = trip,
                                members = calculated.members,
                                deposits = calculated.deposits,
                                expenses = calculated.expenses,
                                splits = calculated.splits,
                                payers = calculated.payers,
                                categories = calculated.categories,
                                settlements = calculated.settlements
                            )
                            if (trip.isReadOnly) {
                                setupRemoteTripObserver(trip)
                            } else {
                                if (trip.tripCode.isBlank()) {
                                    val generatedCode = TripCodeGenerator.generateCode(trip.destination.ifBlank { trip.name })
                                    val deviceId = DeviceIdHelper.getDeviceId(getApplication())
                                    viewModelScope.launch {
                                        repository.updateTrip(trip.copy(tripCode = generatedCode, leaderDeviceId = deviceId))
                                    }
                                } else {
                                    FirebaseSyncManager.syncTripToCloud(
                                        trip = trip,
                                        members = calculated.members,
                                        deposits = calculated.deposits,
                                        expenses = calculated.expenses,
                                        settlements = calculated.settlements,
                                        calculatedSettlements = calculated.settlementSteps
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun exportAndSaveAutoBackup(
        trip: TripEntity,
        members: List<MemberEntity>,
        deposits: List<DepositEntity>,
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
        payers: List<ExpensePayerEntity>,
        categories: List<CategoryEntity>,
        settlements: List<SettlementRecordEntity>
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val bundle = TripBackupBundle(
                    trip = trip,
                    members = members,
                    deposits = deposits,
                    expenses = expenses,
                    splits = splits,
                    payers = payers,
                    categories = categories,
                    settlements = settlements
                )
                val json = JsonBackupManager.exportTripToJson(bundle)
                val context = getApplication<Application>()
                val backupFile = File(context.filesDir, "TripTracker_AutoBackup.json")
                backupFile.writeText(json)
            }
        }
    }

    private fun recalculateState(
        trip: TripEntity?,
        members: List<MemberEntity>,
        deposits: List<DepositEntity>,
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
        payers: List<ExpensePayerEntity>,
        attachments: List<ReceiptAttachmentEntity>,
        categories: List<CategoryEntity>,
        settlements: List<SettlementRecordEntity>
    ): TripSplitUiState {
        if (trip == null) {
            return TripSplitUiState(
                currentTrip = null,
                members = members,
                deposits = deposits,
                expenses = expenses,
                splits = splits,
                payers = payers,
                attachments = attachments,
                categories = categories,
                settlements = settlements
            )
        }

        val balances = SettlementEngine.calculateMemberBalances(
            members = members,
            deposits = deposits,
            expenses = expenses,
            splits = splits,
            payers = payers
        )

        val steps = SettlementEngine.calculateMinimizedSettlements(
            members = members,
            deposits = deposits,
            expenses = expenses,
            splits = splits,
            settledRecords = settlements,
            payers = payers
        )

        val totalDeposited = deposits.sumOf { it.amount }
        val commonFundSpent = expenses.filter { !it.isIncome && it.payerType == PayerType.COMMON_FUND }.sumOf { it.amount }
        val remainingCommonFund = totalDeposited - commonFundSpent
        val totalSpentOverall = expenses.filter { !it.isIncome }.sumOf { it.amount }
        val budgetProgress = if (trip.budget > 0) (totalSpentOverall / trip.budget).toFloat().coerceIn(0f, 1f) else 0f

        // Category breakdown
        val expenseItems = expenses.filter { !it.isIncome }
        val categoryTotals = mutableMapOf<String, Double>()
        val categoryIcons = mutableMapOf<String, String>()
        val categoryColors = mutableMapOf<String, String>()
        val categoryCounts = mutableMapOf<String, Int>()

        for (exp in expenseItems) {
            val catName = exp.customCategoryName ?: exp.category.displayName
            val catIcon = exp.customCategoryIcon ?: exp.category.iconName
            val catColor = exp.customCategoryColor ?: exp.category.defaultColorHex

            categoryTotals[catName] = (categoryTotals[catName] ?: 0.0) + exp.amount
            categoryIcons[catName] = catIcon
            categoryColors[catName] = catColor
            categoryCounts[catName] = (categoryCounts[catName] ?: 0) + 1
        }

        val categorySpendings = categoryTotals.map { (catName, total) ->
            CategorySpending(
                categoryName = catName,
                iconName = categoryIcons[catName] ?: "ticket",
                colorHex = categoryColors[catName] ?: "#006C4C",
                totalAmount = total,
                percentage = if (totalSpentOverall > 0) (total / totalSpentOverall).toFloat() else 0f,
                count = categoryCounts[catName] ?: 1
            )
        }.sortedByDescending { it.totalAmount }

        val topCategory = categorySpendings.firstOrNull()
        val topSpender = balances.maxByOrNull { it.totalPaid }

        val summary = TripSummary(
            trip = trip,
            totalDeposited = totalDeposited,
            commonFundSpent = commonFundSpent,
            remainingCommonFund = remainingCommonFund,
            totalSpentOverall = totalSpentOverall,
            budget = trip.budget,
            budgetProgress = budgetProgress,
            highestCategory = topCategory?.categoryName,
            highestCategoryAmount = topCategory?.totalAmount ?: 0.0,
            topSpender = topSpender?.member,
            topSpenderAmount = topSpender?.totalPaid ?: 0.0,
            memberCount = members.size,
            expenseCount = expenseItems.size
        )

        return TripSplitUiState(
            currentTrip = trip,
            members = members,
            deposits = deposits,
            expenses = expenses,
            splits = splits,
            payers = payers,
            attachments = attachments,
            categories = categories,
            settlements = settlements,
            memberBalances = balances,
            settlementSteps = steps,
            tripSummary = summary,
            categorySpendings = categorySpendings
        )
    }

    // --- Actions ---

    fun selectTrip(tripId: Long) {
        _currentTripId.value = tripId
    }

    fun setSelectedTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun openDialog(dialog: ActiveDialog) {
        _uiState.update { it.copy(activeDialog = dialog) }
    }

    fun closeDialog() {
        _uiState.update { it.copy(activeDialog = ActiveDialog.NONE) }
    }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(userMessage = msg) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun viewPhoto(filePath: String) {
        _uiState.update { it.copy(viewingPhotoPath = filePath, activeDialog = ActiveDialog.VIEW_PHOTO) }
    }

    fun closePhotoViewer() {
        _uiState.update { it.copy(viewingPhotoPath = null, activeDialog = ActiveDialog.NONE) }
    }

    fun createTrip(trip: TripEntity, members: List<NewMemberInfo>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val deviceId = DeviceIdHelper.getDeviceId(context)
            val code = if (trip.tripCode.isNotBlank()) trip.tripCode.uppercase().trim()
                       else TripCodeGenerator.generateCode(trip.destination.ifBlank { trip.name })
            val preparedTrip = trip.copy(
                tripCode = code,
                isReadOnly = false,
                leaderDeviceId = deviceId
            )
            val newTripId = repository.createTripWithDetailedMembers(preparedTrip, members)
            _currentTripId.value = newTripId
            closeDialog()
            showMessage("Tour created! Join Code: $code")
        }
    }

    fun updateTripStatus(trip: TripEntity, newStatus: TripStatus) {
        viewModelScope.launch {
            repository.updateTrip(trip.copy(status = newStatus))
            showMessage("Trip status marked as ${newStatus.displayName}")
        }
    }

    fun deleteCurrentTrip() {
        val trip = _uiState.value.currentTrip ?: return
        viewModelScope.launch {
            repository.deleteTrip(trip)
            _currentTripId.value = null
            closeDialog()
            showMessage("Trip deleted.")
        }
    }

    fun addIncome(
        amount: Double,
        contributorName: String,
        memberId: Long?,
        categoryName: String,
        notes: String,
        date: Long
    ) {
        val trip = _uiState.value.currentTrip ?: return
        viewModelScope.launch {
            val deposit = DepositEntity(
                tripId = trip.id,
                memberId = memberId,
                contributorName = contributorName,
                amount = amount,
                categoryName = categoryName,
                date = date,
                notes = notes
            )
            repository.addDeposit(deposit)
            closeDialog()
            showMessage("Income/Deposit recorded: ৳$amount")
        }
    }

    fun addExpenseWithDetails(
        title: String,
        amount: Double,
        category: ExpenseCategory,
        customCategoryName: String?,
        customCategoryIcon: String?,
        customCategoryColor: String?,
        payerType: PayerType,
        payerMemberId: Long?,
        splitMode: SplitMode,
        splits: List<ExpenseSplitEntity>,
        payers: List<ExpensePayerEntity>,
        photoUris: List<Uri>,
        notes: String
    ) {
        val trip = _uiState.value.currentTrip ?: return
        viewModelScope.launch {
            // Save local attachments to scoped storage
            val context = getApplication<Application>()
            val attachments = photoUris.mapNotNull { uri ->
                val localPath = ImageStorageManager.saveImageToInternalStorage(context, uri)
                if (localPath != null) {
                    ReceiptAttachmentEntity(expenseId = 0, filePath = localPath)
                } else null
            }

            val expense = ExpenseEntity(
                tripId = trip.id,
                title = title,
                amount = amount,
                category = category,
                customCategoryName = customCategoryName,
                customCategoryIcon = customCategoryIcon,
                customCategoryColor = customCategoryColor,
                payerType = payerType,
                payerMemberId = payerMemberId,
                splitMode = splitMode,
                notes = notes
            )

            repository.addExpenseWithDetails(
                expense = expense,
                splits = splits,
                payers = payers,
                attachments = attachments
            )
            closeDialog()
            showMessage("Expense added: ৳$amount")
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            showMessage("Expense deleted.")
        }
    }

    fun addCustomCategory(name: String, iconName: String, colorHex: String, isIncome: Boolean) {
        val trip = _uiState.value.currentTrip ?: return
        viewModelScope.launch {
            repository.addCategory(
                CategoryEntity(
                    tripId = trip.id,
                    name = name.trim(),
                    iconName = iconName,
                    colorHex = colorHex,
                    isIncome = isIncome
                )
            )
            closeDialog()
            showMessage("Category '$name' created!")
        }
    }

    fun recordSettlement(
        fromMemberId: Long,
        toMemberId: Long,
        fromName: String,
        toName: String,
        amount: Double,
        notes: String
    ) {
        val trip = _uiState.value.currentTrip ?: return
        viewModelScope.launch {
            repository.addSettlement(
                SettlementRecordEntity(
                    tripId = trip.id,
                    fromMemberId = fromMemberId,
                    toMemberId = toMemberId,
                    fromMemberName = fromName,
                    toMemberName = toName,
                    amount = amount,
                    notes = notes
                )
            )
            showMessage("Settlement recorded: $fromName paid $toName ৳$amount")
        }
    }

    fun getWhatsAppShareText(): String {
        val state = _uiState.value
        val trip = state.currentTrip ?: return "No trip selected."
        return ShareSummaryFormatter.generateWhatsAppText(
            trip = trip,
            balances = state.memberBalances,
            settlements = state.settlementSteps,
            expenses = state.expenses,
            totalDeposits = state.tripSummary?.totalDeposited ?: 0.0,
            commonFundSpent = state.tripSummary?.commonFundSpent ?: 0.0,
            remainingCommonFund = state.tripSummary?.remainingCommonFund ?: 0.0
        )
    }

    fun exportPdfReport(): File? {
        val state = _uiState.value
        val trip = state.currentTrip ?: return null
        return runCatching {
            PdfReportGenerator.generateReport(
                context = getApplication(),
                trip = trip,
                balances = state.memberBalances,
                settlements = state.settlementSteps,
                expenses = state.expenses,
                totalDeposits = state.tripSummary?.totalDeposited ?: 0.0,
                commonFundSpent = state.tripSummary?.commonFundSpent ?: 0.0,
                remainingCommonFund = state.tripSummary?.remainingCommonFund ?: 0.0
            )
        }.getOrNull()
    }

    fun exportJsonBackup(): String {
        val state = _uiState.value
        val trip = state.currentTrip ?: return ""
        val bundle = TripBackupBundle(
            trip = trip,
            members = state.members,
            deposits = state.deposits,
            expenses = state.expenses,
            splits = state.splits,
            payers = state.payers,
            categories = state.categories,
            settlements = state.settlements
        )
        return JsonBackupManager.exportTripToJson(bundle)
    }

    fun importJsonBackup(jsonString: String) {
        viewModelScope.launch {
            val bundle = JsonBackupManager.parseTripFromJson(jsonString)
            if (bundle != null) {
                val newTripId = repository.importTripBundle(bundle)
                _currentTripId.value = newTripId
                closeDialog()
                showMessage("Trip '${bundle.trip.name}' imported successfully!")
            } else {
                showMessage("Failed to parse JSON backup. Please check format.")
            }
        }
    }

    private fun setupRemoteTripObserver(trip: TripEntity) {
        val code = trip.tripCode.uppercase().trim()
        if (code.isBlank()) return

        if (activeRemoteTripCode == code && activeRemoteListener != null) {
            return
        }

        activeRemoteListener?.let { listener ->
            activeRemoteTripCode?.let { oldCode ->
                FirebaseSyncManager.removeTripObserver(oldCode, listener)
            }
        }

        activeRemoteTripCode = code
        activeRemoteListener = FirebaseSyncManager.observeTrip(
            tripCode = code,
            onUpdate = { cloudTrip ->
                if (cloudTrip != null) {
                    viewModelScope.launch {
                        repository.syncRemoteTripToLocal(cloudTrip)
                    }
                }
            },
            onError = { ex ->
                Log.e("TripSplitVM", "Live sync error: ${ex.message}")
            }
        )
    }

    fun openJoinTripDialog() {
        openDialog(ActiveDialog.JOIN_TRIP)
    }

    fun openShareTripCodeDialog() {
        openDialog(ActiveDialog.SHARE_TRIP_CODE)
    }

    fun triggerCloudSync() {
        val state = _uiState.value
        val trip = state.currentTrip ?: return
        if (trip.isReadOnly || trip.tripCode.isBlank()) return

        _uiState.update { it.copy(isSyncing = true) }
        FirebaseSyncManager.syncTripToCloud(
            trip = trip,
            members = state.members,
            deposits = state.deposits,
            expenses = state.expenses,
            settlements = state.settlements,
            calculatedSettlements = state.settlementSteps,
            onSuccess = {
                _uiState.update { it.copy(isSyncing = false) }
                showMessage("Trip synced live to Firebase!")
            },
            onError = { ex ->
                _uiState.update { it.copy(isSyncing = false) }
                showMessage("Sync failed: ${ex.message}")
            }
        )
    }

    fun joinTripWithCode(code: String, onComplete: (Boolean, String?) -> Unit) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.length != 6) {
            onComplete(false, "Trip code must be exactly 6 characters")
            return
        }

        _uiState.update { it.copy(isSyncing = true) }

        FirebaseSyncManager.fetchTripOnce(
            tripCode = cleanCode,
            onResult = { cloudTrip ->
                if (cloudTrip != null) {
                    viewModelScope.launch {
                        try {
                            val tripId = repository.syncRemoteTripToLocal(cloudTrip)
                            _currentTripId.value = tripId
                            _uiState.update { it.copy(isSyncing = false) }
                            closeDialog()
                            showMessage("Joined ${cloudTrip.metadata.tripName} in View-Only mode!")
                            onComplete(true, null)
                        } catch (e: Exception) {
                            _uiState.update { it.copy(isSyncing = false) }
                            onComplete(false, "Failed to save trip: ${e.message}")
                        }
                    }
                } else {
                    _uiState.update { it.copy(isSyncing = false) }
                    onComplete(false, "No trip found with code $cleanCode")
                }
            },
            onError = { ex ->
                _uiState.update { it.copy(isSyncing = false) }
                onComplete(false, "Network error: ${ex.message}")
            }
        )
    }

    private fun tryRestoreFromFirebase(context: android.content.Context) {
        FirebaseSyncManager.fetchAllCloudTrips(
            onResult = { cloudTrips ->
                if (cloudTrips.isNotEmpty()) {
                    viewModelScope.launch {
                        var lastId = 0L
                        for (cloudTrip in cloudTrips) {
                            runCatching {
                                val id = repository.syncRemoteTripToLocal(cloudTrip)
                                lastId = id
                            }
                        }
                        if (lastId > 0) {
                            _currentTripId.value = lastId
                        }
                        showMessage("Successfully restored trip(s) from Firebase cloud!")
                    }
                } else {
                    _currentTripId.value = null
                }
            },
            onError = { ex ->
                Log.w("TripSplitVM", "Cloud restoration failed: ${ex.message}")
                _currentTripId.value = null
            }
        )
    }

    fun restoreAllTripsWithAdminPassword(password: String, onComplete: (Boolean, String) -> Unit) {
        if (password.trim() != "plusemon") {
            onComplete(false, "Incorrect super admin password.")
            return
        }

        _uiState.update { it.copy(isSyncing = true) }
        FirebaseSyncManager.fetchAllCloudTrips(
            onResult = { cloudTrips ->
                if (cloudTrips.isNotEmpty()) {
                    viewModelScope.launch {
                        try {
                            var lastId = 0L
                            for (cloudTrip in cloudTrips) {
                                val id = repository.syncRemoteTripToLocal(cloudTrip)
                                lastId = id
                            }
                            if (lastId > 0) {
                                _currentTripId.value = lastId
                            }
                            _uiState.update { it.copy(isSyncing = false) }
                            onComplete(true, "Successfully force-restored ${cloudTrips.size} trip(s) with super admin password!")
                        } catch (e: Exception) {
                            _uiState.update { it.copy(isSyncing = false) }
                            onComplete(false, "Failed saving restored trips: ${e.message}")
                        }
                    }
                } else {
                    _uiState.update { it.copy(isSyncing = false) }
                    onComplete(false, "No trips found in Firebase cloud database.")
                }
            },
            onError = { ex ->
                _uiState.update { it.copy(isSyncing = false) }
                onComplete(false, "Cloud connection error: ${ex.message}")
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        activeRemoteListener?.let { listener ->
            activeRemoteTripCode?.let { code ->
                FirebaseSyncManager.removeTripObserver(code, listener)
            }
        }
    }
}
