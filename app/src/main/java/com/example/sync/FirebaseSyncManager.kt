package com.example.sync

import android.util.Log
import com.example.MainApplication
import com.example.data.entity.DepositEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.MemberEntity
import com.example.data.entity.PayerType
import com.example.data.entity.SettlementRecordEntity
import com.example.data.entity.TripEntity
import com.example.domain.model.SettlementTransaction
import com.example.sync.model.CloudMember
import com.example.sync.model.CloudSettlement
import com.example.sync.model.CloudTransaction
import com.example.sync.model.CloudTrip
import com.example.sync.model.TripCloudMetadata
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"

    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance(MainApplication.FIREBASE_DB_URL)
    }

    private fun getTripsRef(): DatabaseReference {
        return database.reference.child("trips")
    }

    private fun getTripNodeRef(tripCode: String): DatabaseReference {
        return getTripsRef().child(tripCode.uppercase().trim())
    }

    /**
     * Leader pushes local trip data to Firebase Realtime Database
     */
    fun syncTripToCloud(
        trip: TripEntity,
        members: List<MemberEntity>,
        deposits: List<DepositEntity>,
        expenses: List<ExpenseEntity>,
        settlements: List<SettlementRecordEntity>,
        calculatedSettlements: List<SettlementTransaction> = emptyList(),
        onSuccess: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        val code = trip.tripCode.uppercase().trim()
        if (code.isBlank()) {
            Log.w(TAG, "Cannot sync trip with empty tripCode")
            return
        }

        val metadata = TripCloudMetadata(
            tripId = trip.id.toString(),
            tripCode = code,
            tripName = trip.name,
            leaderDeviceId = trip.leaderDeviceId,
            baseCurrency = trip.baseCurrency,
            targetBudget = trip.budget,
            updatedAt = System.currentTimeMillis()
        )

        val memberMap = members.associateBy { it.id }

        val cloudMembers = members.map { m ->
            val totalDeposited = deposits.filter { it.memberId == m.id }.sumOf { it.amount }
            CloudMember(
                id = "m_${m.id}",
                name = m.name,
                deposited = totalDeposited
            )
        }

        val cloudTransactions = mutableMapOf<String, CloudTransaction>()
        expenses.forEachIndexed { index, exp ->
            val paidBy = when (exp.payerType) {
                PayerType.COMMON_FUND -> "Group Fund"
                PayerType.MEMBER -> exp.payerMemberId?.let { memberMap[it]?.name } ?: "Member"
                PayerType.MULTI_PAYER -> "Multi-Payer"
            }
            val txKey = "tx_${exp.id.takeIf { it > 0 } ?: (index + 1)}"
            val catName = exp.customCategoryName ?: exp.category.displayName
            cloudTransactions[txKey] = CloudTransaction(
                title = exp.title,
                amount = exp.amount,
                category = catName,
                paidBy = paidBy,
                timestamp = exp.date
            )
        }

        val cloudSettlements = if (calculatedSettlements.isNotEmpty()) {
            calculatedSettlements.map { step ->
                CloudSettlement(
                    payer = step.fromMemberName,
                    receiver = step.toMemberName,
                    amount = step.amount,
                    isSettled = step.isSettled
                )
            }
        } else {
            settlements.map { s ->
                val payerName = s.fromMemberName.ifBlank { memberMap[s.fromMemberId]?.name ?: "Member" }
                val receiverName = s.toMemberName.ifBlank { memberMap[s.toMemberId]?.name ?: "Group Fund" }
                CloudSettlement(
                    payer = payerName,
                    receiver = receiverName,
                    amount = s.amount,
                    isSettled = true
                )
            }
        }

        val cloudTrip = CloudTrip(
            metadata = metadata,
            members = cloudMembers,
            transactions = cloudTransactions,
            settlements = cloudSettlements
        )

        getTripNodeRef(code).setValue(cloudTrip)
            .addOnSuccessListener {
                Log.d(TAG, "Trip $code successfully synced to Firebase RTDB")
                onSuccess?.invoke()
            }
            .addOnFailureListener { ex ->
                Log.e(TAG, "Failed to sync trip $code: ${ex.message}", ex)
                onError?.invoke(ex)
            }
    }

    /**
     * Fetch a trip snapshot once (used for validation before joining)
     */
    fun fetchTripOnce(
        tripCode: String,
        onResult: (CloudTrip?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val code = tripCode.uppercase().trim()
        getTripNodeRef(code).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        val trip = parseCloudTrip(snapshot)
                        onResult(trip)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing trip snapshot: ${e.message}", e)
                        onError(e)
                    }
                } else {
                    onResult(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        })
    }

    /**
     * Attach a live ValueEventListener for real-time member synchronization
     */
    fun observeTrip(
        tripCode: String,
        onUpdate: (CloudTrip?) -> Unit,
        onError: (Exception) -> Unit
    ): ValueEventListener {
        val code = tripCode.uppercase().trim()
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        val trip = parseCloudTrip(snapshot)
                        onUpdate(trip)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing live update: ${e.message}", e)
                        onError(e)
                    }
                } else {
                    onUpdate(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Live sync cancelled: ${error.message}")
                onError(error.toException())
            }
        }
        getTripNodeRef(code).addValueEventListener(listener)
        return listener
    }

    fun removeTripObserver(tripCode: String, listener: ValueEventListener) {
        val code = tripCode.uppercase().trim()
        getTripNodeRef(code).removeEventListener(listener)
    }

    private fun parseCloudTrip(snapshot: DataSnapshot): CloudTrip {
        // Parse metadata
        val metaSnap = snapshot.child("metadata")
        val metadata = TripCloudMetadata(
            tripId = metaSnap.child("tripId").getValue(String::class.java) ?: "",
            tripCode = metaSnap.child("tripCode").getValue(String::class.java) ?: snapshot.key ?: "",
            tripName = metaSnap.child("tripName").getValue(String::class.java) ?: "Shared Trip",
            leaderDeviceId = metaSnap.child("leaderDeviceId").getValue(String::class.java) ?: "",
            baseCurrency = metaSnap.child("baseCurrency").getValue(String::class.java) ?: "BDT",
            targetBudget = metaSnap.child("targetBudget").getValue(Double::class.java) ?: 0.0,
            updatedAt = metaSnap.child("updatedAt").getValue(Long::class.java) ?: 0L
        )

        // Parse members list
        val membersList = mutableListOf<CloudMember>()
        val membersSnap = snapshot.child("members")
        for (mChild in membersSnap.children) {
            val id = mChild.child("id").getValue(String::class.java) ?: mChild.key ?: ""
            val name = mChild.child("name").getValue(String::class.java) ?: "Member"
            val deposited = mChild.child("deposited").getValue(Double::class.java) ?: 0.0
            membersList.add(CloudMember(id = id, name = name, deposited = deposited))
        }

        // Parse transactions map
        val txMap = mutableMapOf<String, CloudTransaction>()
        val txSnap = snapshot.child("transactions")
        for (tChild in txSnap.children) {
            val title = tChild.child("title").getValue(String::class.java) ?: "Expense"
            val amount = tChild.child("amount").getValue(Double::class.java) ?: 0.0
            val category = tChild.child("category").getValue(String::class.java) ?: "Other"
            val paidBy = tChild.child("paidBy").getValue(String::class.java) ?: "Group Fund"
            val timestamp = tChild.child("timestamp").getValue(Long::class.java) ?: 0L
            txMap[tChild.key ?: "tx_${txMap.size}"] = CloudTransaction(
                title = title,
                amount = amount,
                category = category,
                paidBy = paidBy,
                timestamp = timestamp
            )
        }

        // Parse settlements
        val settlementsList = mutableListOf<CloudSettlement>()
        val setSnap = snapshot.child("settlements")
        for (sChild in setSnap.children) {
            val payer = sChild.child("payer").getValue(String::class.java) ?: ""
            val receiver = sChild.child("receiver").getValue(String::class.java) ?: ""
            val amount = sChild.child("amount").getValue(Double::class.java) ?: 0.0
            val isSettled = sChild.child("isSettled").getValue(Boolean::class.java) ?: false
            settlementsList.add(CloudSettlement(payer = payer, receiver = receiver, amount = amount, isSettled = isSettled))
        }

        return CloudTrip(
            metadata = metadata,
            members = membersList,
            transactions = txMap,
            settlements = settlementsList
        )
    }
}
