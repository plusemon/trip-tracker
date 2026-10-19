package com.example.data.repository

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
import com.example.domain.model.NewMemberInfo
import com.example.export.TripBackupBundle
import com.example.sync.model.CloudTrip
import kotlinx.coroutines.flow.Flow

class TripSplitRepository(private val db: AppDatabase) {

    val allTrips: Flow<List<TripEntity>> = db.tripDao().getAllTrips()

    fun getTrip(tripId: Long): Flow<TripEntity?> = db.tripDao().getTripById(tripId)

    suspend fun getTripOnce(tripId: Long): TripEntity? = db.tripDao().getTripByIdOnce(tripId)

    suspend fun updateTrip(trip: TripEntity) = db.tripDao().updateTrip(trip)

    suspend fun deleteTrip(trip: TripEntity) = db.tripDao().deleteTrip(trip)

    suspend fun createTripWithDetailedMembers(
        trip: TripEntity,
        membersList: List<NewMemberInfo>
    ): Long {
        val tripId = db.tripDao().insertTrip(trip)

        // Seed default categories for this trip
        val defaultCategories = listOf(
            CategoryEntity(tripId = tripId, name = "Transportation", iconName = "bus", colorHex = "#00658E", isIncome = false),
            CategoryEntity(tripId = tripId, name = "Lodging & Resort", iconName = "hotel", colorHex = "#7B1FA2", isIncome = false),
            CategoryEntity(tripId = tripId, name = "Food & Dining", iconName = "food", colorHex = "#E65100", isIncome = false),
            CategoryEntity(tripId = tripId, name = "Guide & Permissions", iconName = "policy", colorHex = "#00796B", isIncome = false),
            CategoryEntity(tripId = tripId, name = "Sightseeing & Tickets", iconName = "ticket", colorHex = "#006C4C", isIncome = false),
            CategoryEntity(tripId = tripId, name = "Snacks & Refreshment", iconName = "snack", colorHex = "#835400", isIncome = false),
            // Default income categories
            CategoryEntity(tripId = tripId, name = "Group Fund Deposit", iconName = "money", colorHex = "#FFBA35", isIncome = true),
            CategoryEntity(tripId = tripId, name = "Sponsorship / Gift", iconName = "gift", colorHex = "#2E7D32", isIncome = true),
            CategoryEntity(tripId = tripId, name = "Refund / Cashback", iconName = "ticket", colorHex = "#0288D1", isIncome = true)
        )
        db.categoryDao().insertCategories(defaultCategories)

        // Members insertion
        val defaultColors = listOf(
            "#006C4C", "#00658E", "#835400", "#7B1FA2",
            "#C2185B", "#00796B", "#E65100", "#303F9F",
            "#5D4037", "#455A64"
        )
        val membersToInsert = membersList.mapIndexed { idx, info ->
            val color = if (info.colorHex.isNotBlank() && info.colorHex.startsWith("#")) {
                info.colorHex
            } else {
                defaultColors[idx % defaultColors.size]
            }
            val initials = info.name.trim().split("\\s+".toRegex()).let { parts ->
                if (parts.size >= 2) "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
                else parts[0].take(2).uppercase()
            }
            MemberEntity(
                tripId = tripId,
                name = info.name.trim(),
                phoneNumber = info.phoneNumber.trim(),
                colorHex = color,
                avatarInitials = initials,
                isManager = info.isManager
            )
        }
        val insertedIds = db.memberDao().insertMembers(membersToInsert)

        // Insert initial deposits if any
        membersList.forEachIndexed { idx, info ->
            if (info.initialDeposit > 0) {
                val memberId = insertedIds.getOrNull(idx) ?: return@forEachIndexed
                db.depositDao().insertDeposit(
                    DepositEntity(
                        tripId = tripId,
                        memberId = memberId,
                        contributorName = info.name,
                        amount = info.initialDeposit,
                        categoryName = "Group Fund Deposit",
                        notes = "Initial Advance Deposit"
                    )
                )
            }
        }
        return tripId
    }

    suspend fun createTripWithMembers(trip: TripEntity, memberNames: List<String>): Long {
        return createTripWithDetailedMembers(
            trip = trip,
            membersList = memberNames.mapIndexed { idx, name ->
                NewMemberInfo(
                    name = name,
                    isManager = (idx == 0)
                )
            }
        )
    }

    // Members
    fun getMembers(tripId: Long): Flow<List<MemberEntity>> = db.memberDao().getMembersForTrip(tripId)
    suspend fun getMembersOnce(tripId: Long): List<MemberEntity> = db.memberDao().getMembersForTripOnce(tripId)
    suspend fun addMember(member: MemberEntity): Long = db.memberDao().insertMember(member)
    suspend fun deleteMember(member: MemberEntity) = db.memberDao().deleteMember(member)

    // Deposits / Inflow
    fun getDeposits(tripId: Long): Flow<List<DepositEntity>> = db.depositDao().getDepositsForTrip(tripId)
    suspend fun getDepositsOnce(tripId: Long): List<DepositEntity> = db.depositDao().getDepositsForTripOnce(tripId)
    suspend fun addDeposit(deposit: DepositEntity): Long = db.depositDao().insertDeposit(deposit)
    suspend fun deleteDeposit(deposit: DepositEntity) = db.depositDao().deleteDeposit(deposit)

    // Expenses & Splits
    fun getExpenses(tripId: Long): Flow<List<ExpenseEntity>> = db.expenseDao().getExpensesForTrip(tripId)
    suspend fun getExpensesOnce(tripId: Long): List<ExpenseEntity> = db.expenseDao().getExpensesForTripOnce(tripId)
    suspend fun getExpenseById(expenseId: Long): ExpenseEntity? = db.expenseDao().getExpenseById(expenseId)

    suspend fun addExpenseWithDetails(
        expense: ExpenseEntity,
        splits: List<ExpenseSplitEntity>,
        payers: List<ExpensePayerEntity> = emptyList(),
        attachments: List<ReceiptAttachmentEntity> = emptyList()
    ): Long {
        val expenseId = db.expenseDao().insertExpenseWithSplitsAndPayers(expense, splits, payers)
        if (attachments.isNotEmpty()) {
            val attachmentsWithId = attachments.map { it.copy(expenseId = expenseId) }
            db.attachmentDao().insertAttachments(attachmentsWithId)
        }
        return expenseId
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        db.expenseDao().deleteExpenseAndAllRelations(expense)
        db.attachmentDao().deleteAttachmentsForExpense(expense.id)
    }

    fun getAllSplits(tripId: Long): Flow<List<ExpenseSplitEntity>> = db.expenseDao().getAllSplitsForTrip(tripId)
    suspend fun getAllSplitsOnce(tripId: Long): List<ExpenseSplitEntity> = db.expenseDao().getAllSplitsForTripOnce(tripId)

    fun getAllPayers(tripId: Long): Flow<List<ExpensePayerEntity>> = db.expenseDao().getAllPayersForTrip(tripId)
    suspend fun getAllPayersOnce(tripId: Long): List<ExpensePayerEntity> = db.expenseDao().getAllPayersForTripOnce(tripId)

    // Attachments
    fun getAllAttachments(tripId: Long): Flow<List<ReceiptAttachmentEntity>> = db.attachmentDao().getAllAttachmentsForTrip(tripId)
    suspend fun addAttachment(attachment: ReceiptAttachmentEntity): Long = db.attachmentDao().insertAttachment(attachment)
    suspend fun deleteAttachment(attachment: ReceiptAttachmentEntity) = db.attachmentDao().deleteAttachment(attachment)

    // Categories
    fun getCategories(tripId: Long): Flow<List<CategoryEntity>> = db.categoryDao().getCategoriesForTrip(tripId)
    suspend fun getCategoriesOnce(tripId: Long): List<CategoryEntity> = db.categoryDao().getCategoriesForTripOnce(tripId)
    suspend fun addCategory(category: CategoryEntity): Long = db.categoryDao().insertCategory(category)
    suspend fun deleteCategory(category: CategoryEntity) = db.categoryDao().deleteCategory(category)

    // Settlements
    fun getSettlements(tripId: Long): Flow<List<SettlementRecordEntity>> = db.settlementDao().getSettlementRecordsForTrip(tripId)
    suspend fun getSettlementsOnce(tripId: Long): List<SettlementRecordEntity> = db.settlementDao().getSettlementRecordsForTripOnce(tripId)
    suspend fun addSettlement(record: SettlementRecordEntity): Long = db.settlementDao().insertSettlementRecord(record)
    suspend fun deleteSettlement(record: SettlementRecordEntity) = db.settlementDao().deleteSettlementRecord(record)

    // Import Backup Bundle
    suspend fun importTripBundle(bundle: TripBackupBundle): Long {
        val newTripId = db.tripDao().insertTrip(bundle.trip.copy(id = 0))

        // Insert categories
        val categories = bundle.categories.map { it.copy(id = 0, tripId = newTripId) }
        if (categories.isNotEmpty()) {
            db.categoryDao().insertCategories(categories)
        }

        // Insert members and map old originalId -> new insertedId
        val memberMap = mutableMapOf<Long, Long>()
        bundle.members.forEach { m ->
            val oldId = m.id
            val newId = db.memberDao().insertMember(m.copy(id = 0, tripId = newTripId))
            memberMap[oldId] = newId
        }

        // Insert deposits
        bundle.deposits.forEach { d ->
            val mappedMemberId = d.memberId?.let { memberMap[it] }
            db.depositDao().insertDeposit(
                d.copy(id = 0, tripId = newTripId, memberId = mappedMemberId)
            )
        }

        // Insert expenses, splits, payers
        bundle.expenses.forEach { exp ->
            val mappedPayerId = exp.payerMemberId?.let { memberMap[it] }
            val newExpId = db.expenseDao().insertExpense(
                exp.copy(id = 0, tripId = newTripId, payerMemberId = mappedPayerId)
            )

            // Splits for this expense
            val splitsForExp = bundle.splits.filter { it.expenseId == exp.id }
            val mappedSplits = splitsForExp.mapNotNull { s ->
                val newMemberId = memberMap[s.memberId] ?: return@mapNotNull null
                ExpenseSplitEntity(
                    id = 0,
                    expenseId = newExpId,
                    memberId = newMemberId,
                    assignedAmount = s.assignedAmount
                )
            }
            if (mappedSplits.isNotEmpty()) {
                db.expenseDao().insertSplits(mappedSplits)
            }

            // Payers for this expense
            val payersForExp = bundle.payers.filter { it.expenseId == exp.id }
            val mappedPayers = payersForExp.mapNotNull { p ->
                val newMemberId = memberMap[p.memberId] ?: return@mapNotNull null
                ExpensePayerEntity(
                    id = 0,
                    expenseId = newExpId,
                    memberId = newMemberId,
                    amountPaid = p.amountPaid
                )
            }
            if (mappedPayers.isNotEmpty()) {
                db.expenseDao().insertPayers(mappedPayers)
            }
        }

        return newTripId
    }

    suspend fun getAllTripBundlesOnce(): List<TripBackupBundle> {
        val trips = db.tripDao().getAllTripsOnce()
        return trips.map { trip ->
            val members = db.memberDao().getMembersForTripOnce(trip.id)
            val deposits = db.depositDao().getDepositsForTripOnce(trip.id)
            val expenses = db.expenseDao().getExpensesForTripOnce(trip.id)
            val splits = db.expenseDao().getAllSplitsForTripOnce(trip.id)
            val payers = db.expenseDao().getAllPayersForTripOnce(trip.id)
            val categories = db.categoryDao().getCategoriesForTripOnce(trip.id)
            val settlements = db.settlementDao().getSettlementRecordsForTripOnce(trip.id)
            TripBackupBundle(
                trip = trip,
                members = members,
                deposits = deposits,
                expenses = expenses,
                splits = splits,
                payers = payers,
                categories = categories,
                settlements = settlements
            )
        }
    }

    suspend fun replaceAllDataWithBundles(bundles: List<TripBackupBundle>): Long {
        db.clearAllTables()
        var firstTripId = -1L
        bundles.forEach { bundle ->
            val tripId = importTripBundle(bundle)
            if (firstTripId == -1L) {
                firstTripId = tripId
            }
        }
        return if (firstTripId != -1L) firstTripId else 0L
    }

    suspend fun getTripByCode(tripCode: String): TripEntity? {
        return db.tripDao().getTripByCode(tripCode.uppercase().trim())
    }

    suspend fun syncRemoteTripToLocal(cloudTrip: CloudTrip): Long {
        val code = cloudTrip.metadata.tripCode.uppercase().trim()
        val existingTrip = db.tripDao().getTripByCode(code)

        val tripId = if (existingTrip != null) {
            val updated = existingTrip.copy(
                name = cloudTrip.metadata.tripName.ifBlank { "Tour $code" },
                budget = cloudTrip.metadata.targetBudget,
                baseCurrency = cloudTrip.metadata.baseCurrency.ifBlank { "BDT" },
                isReadOnly = true,
                leaderDeviceId = cloudTrip.metadata.leaderDeviceId
            )
            db.tripDao().updateTrip(updated)
            existingTrip.id
        } else {
            val newTrip = TripEntity(
                name = cloudTrip.metadata.tripName.ifBlank { "Tour $code" },
                destination = cloudTrip.metadata.tripName.ifBlank { "Tour $code" },
                startDate = if (cloudTrip.metadata.updatedAt > 0) cloudTrip.metadata.updatedAt else System.currentTimeMillis(),
                endDate = (if (cloudTrip.metadata.updatedAt > 0) cloudTrip.metadata.updatedAt else System.currentTimeMillis()) + (7 * 86400000L),
                budget = cloudTrip.metadata.targetBudget,
                baseCurrency = cloudTrip.metadata.baseCurrency.ifBlank { "BDT" },
                tripCode = code,
                isReadOnly = true,
                leaderDeviceId = cloudTrip.metadata.leaderDeviceId
            )
            val newId = db.tripDao().insertTrip(newTrip)

            // Seed default categories
            val defaultCategories = listOf(
                CategoryEntity(tripId = newId, name = "Transportation", iconName = "bus", colorHex = "#00658E", isIncome = false),
                CategoryEntity(tripId = newId, name = "Lodging & Resort", iconName = "hotel", colorHex = "#7B1FA2", isIncome = false),
                CategoryEntity(tripId = newId, name = "Food & Dining", iconName = "food", colorHex = "#E65100", isIncome = false),
                CategoryEntity(tripId = newId, name = "Sightseeing & Tickets", iconName = "ticket", colorHex = "#006C4C", isIncome = false),
                CategoryEntity(tripId = newId, name = "Group Fund Deposit", iconName = "money", colorHex = "#FFBA35", isIncome = true)
            )
            db.categoryDao().insertCategories(defaultCategories)
            newId
        }

        // Clean out previous records for this synced trip to ensure authoritative sync
        db.settlementDao().deleteSettlementsForTrip(tripId)
        db.expenseDao().deleteSplitsForTrip(tripId)
        db.expenseDao().deletePayersForTrip(tripId)
        db.expenseDao().deleteExpensesForTrip(tripId)
        db.depositDao().deleteDepositsForTrip(tripId)
        db.memberDao().deleteMembersForTrip(tripId)

        // Insert members
        val defaultColors = listOf(
            "#006C4C", "#00658E", "#835400", "#7B1FA2",
            "#C2185B", "#00796B", "#E65100", "#303F9F"
        )
        val memberNameMap = mutableMapOf<String, Long>()

        cloudTrip.members.forEachIndexed { idx, m ->
            val initials = m.name.trim().split("\\s+".toRegex()).let { parts ->
                if (parts.size >= 2) "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
                else parts[0].take(2).uppercase()
            }
            val memberEntity = MemberEntity(
                id = 0,
                tripId = tripId,
                name = m.name.trim(),
                colorHex = defaultColors[idx % defaultColors.size],
                avatarInitials = initials,
                isManager = idx == 0
            )
            val insertedMemberId = db.memberDao().insertMember(memberEntity)
            memberNameMap[m.name.trim()] = insertedMemberId

            // If member has deposited fund, insert deposit
            if (m.deposited > 0) {
                db.depositDao().insertDeposit(
                    DepositEntity(
                        tripId = tripId,
                        memberId = insertedMemberId,
                        contributorName = m.name.trim(),
                        amount = m.deposited,
                        categoryName = "Group Fund Deposit",
                        date = if (cloudTrip.metadata.updatedAt > 0) cloudTrip.metadata.updatedAt else System.currentTimeMillis()
                    )
                )
            }
        }

        // Insert transactions
        cloudTrip.transactions.values.forEach { tx ->
            val isCommonFund = tx.paidBy.equals("Group Fund", ignoreCase = true) || tx.paidBy.equals("Common Fund", ignoreCase = true)
            val payerId = if (!isCommonFund) memberNameMap[tx.paidBy.trim()] else null
            val payerType = if (isCommonFund || payerId == null) PayerType.COMMON_FUND else PayerType.MEMBER

            val expCategory = ExpenseCategory.entries.find {
                it.displayName.equals(tx.category, ignoreCase = true)
            } ?: ExpenseCategory.TRANSPORTATION

            val expenseEntity = ExpenseEntity(
                id = 0,
                tripId = tripId,
                title = tx.title,
                amount = tx.amount,
                category = expCategory,
                customCategoryName = tx.category,
                payerType = payerType,
                payerMemberId = payerId,
                splitMode = SplitMode.EQUAL,
                date = if (tx.timestamp > 0) tx.timestamp else System.currentTimeMillis()
            )
            val expId = db.expenseDao().insertExpense(expenseEntity)

            // Equal splits across all members
            if (memberNameMap.isNotEmpty() && tx.amount > 0) {
                val perPerson = tx.amount / memberNameMap.size
                val splits = memberNameMap.values.map { mId ->
                    ExpenseSplitEntity(
                        id = 0,
                        expenseId = expId,
                        memberId = mId,
                        assignedAmount = perPerson
                    )
                }
                db.expenseDao().insertSplits(splits)
            }
        }

        // Insert settlements
        cloudTrip.settlements.forEach { s ->
            val payerId = memberNameMap[s.payer.trim()] ?: 0L
            val receiverId = memberNameMap[s.receiver.trim()] ?: 0L
            db.settlementDao().insertSettlementRecord(
                SettlementRecordEntity(
                    id = 0,
                    tripId = tripId,
                    fromMemberId = payerId,
                    toMemberId = receiverId,
                    fromMemberName = s.payer.trim(),
                    toMemberName = s.receiver.trim(),
                    amount = s.amount,
                    settledDate = if (cloudTrip.metadata.updatedAt > 0) cloudTrip.metadata.updatedAt else System.currentTimeMillis(),
                    notes = "Real-time synced settlement"
                )
            )
        }

        return tripId
    }
}
