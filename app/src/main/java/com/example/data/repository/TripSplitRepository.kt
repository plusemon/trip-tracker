package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.entity.DepositEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.MemberEntity
import com.example.data.entity.SettlementRecordEntity
import com.example.data.entity.TripEntity
import com.example.domain.engine.SampleDataProvider
import kotlinx.coroutines.flow.Flow

class TripSplitRepository(private val db: AppDatabase) {

    val allTrips: Flow<List<TripEntity>> = db.tripDao().getAllTrips()

    fun getTrip(tripId: Long): Flow<TripEntity?> = db.tripDao().getTripById(tripId)

    suspend fun getTripOnce(tripId: Long): TripEntity? = db.tripDao().getTripByIdOnce(tripId)

    suspend fun createTripWithDetailedMembers(trip: TripEntity, membersList: List<com.example.domain.model.NewMemberInfo>): Long {
        val tripId = db.tripDao().insertTrip(trip)
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
                        amount = info.initialDeposit,
                        notes = "Initial Advance চাঁদা"
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
                com.example.domain.model.NewMemberInfo(
                    name = name,
                    isManager = (idx == 0)
                )
            }
        )
    }

    suspend fun deleteTrip(trip: TripEntity) = db.tripDao().deleteTrip(trip)

    // Members
    fun getMembers(tripId: Long): Flow<List<MemberEntity>> = db.memberDao().getMembersForTrip(tripId)

    suspend fun getMembersOnce(tripId: Long): List<MemberEntity> = db.memberDao().getMembersForTripOnce(tripId)

    suspend fun addMember(member: MemberEntity): Long = db.memberDao().insertMember(member)

    suspend fun deleteMember(member: MemberEntity) = db.memberDao().deleteMember(member)

    // Deposits
    fun getDeposits(tripId: Long): Flow<List<DepositEntity>> = db.depositDao().getDepositsForTrip(tripId)

    suspend fun getDepositsOnce(tripId: Long): List<DepositEntity> = db.depositDao().getDepositsForTripOnce(tripId)

    suspend fun addDeposit(deposit: DepositEntity): Long = db.depositDao().insertDeposit(deposit)

    suspend fun deleteDeposit(deposit: DepositEntity) = db.depositDao().deleteDeposit(deposit)

    // Expenses & Splits
    fun getExpenses(tripId: Long): Flow<List<ExpenseEntity>> = db.expenseDao().getExpensesForTrip(tripId)

    suspend fun getExpensesOnce(tripId: Long): List<ExpenseEntity> = db.expenseDao().getExpensesForTripOnce(tripId)

    fun getAllSplits(tripId: Long): Flow<List<ExpenseSplitEntity>> = db.expenseDao().getAllSplitsForTrip(tripId)

    suspend fun getAllSplitsOnce(tripId: Long): List<ExpenseSplitEntity> = db.expenseDao().getAllSplitsForTripOnce(tripId)

    suspend fun addExpenseWithSplits(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>): Long =
        db.expenseDao().insertExpenseWithSplits(expense, splits)

    suspend fun deleteExpense(expense: ExpenseEntity) = db.expenseDao().deleteExpenseAndSplits(expense)

    // Settlements
    fun getSettlements(tripId: Long): Flow<List<SettlementRecordEntity>> =
        db.settlementDao().getSettlementRecordsForTrip(tripId)

    suspend fun recordSettlement(record: SettlementRecordEntity): Long =
        db.settlementDao().insertSettlementRecord(record)

    suspend fun deleteSettlement(record: SettlementRecordEntity) =
        db.settlementDao().deleteSettlementRecord(record)

    // Sample data loader
    suspend fun loadBandarbanSample(): Long {
        return SampleDataProvider.populateBandarbanTour(
            tripDao = db.tripDao(),
            memberDao = db.memberDao(),
            depositDao = db.depositDao(),
            expenseDao = db.expenseDao()
        )
    }
}
