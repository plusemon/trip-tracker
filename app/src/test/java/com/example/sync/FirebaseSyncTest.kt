package com.example.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.repository.TripSplitRepository
import com.example.sync.model.CloudMember
import com.example.sync.model.CloudSettlement
import com.example.sync.model.CloudTransaction
import com.example.sync.model.CloudTrip
import com.example.sync.model.TripCloudMetadata
import com.example.util.TripCodeGenerator
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirebaseSyncTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TripSplitRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TripSplitRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testTripCodeGenerator() {
        val code1 = TripCodeGenerator.generateCode("Bandarban")
        assertEquals(6, code1.length)
        assertTrue(code1.startsWith("BAND"))
        assertTrue(code1.all { it.isLetterOrDigit() })

        val code2 = TripCodeGenerator.generateCode()
        assertEquals(6, code2.length)
        assertTrue(code2.all { it.isLetterOrDigit() })
    }

    @Test
    fun testSyncRemoteTripToLocalPersistence() = runTest {
        val cloudTrip = CloudTrip(
            metadata = TripCloudMetadata(
                tripId = "1",
                tripCode = "BNDR26",
                tripName = "Bandarban Expedition",
                leaderDeviceId = "dev_host_123",
                baseCurrency = "BDT",
                targetBudget = 80000.0,
                updatedAt = 1718000000000L
            ),
            members = listOf(
                CloudMember(id = "m1", name = "Arif", deposited = 5000.0),
                CloudMember(id = "m2", name = "Emon", deposited = 5000.0)
            ),
            transactions = mapOf(
                "tx_1" to CloudTransaction(
                    title = "Chander Gari",
                    amount = 8000.0,
                    category = "Transportation",
                    paidBy = "Common Fund",
                    timestamp = 1718000000000L
                )
            ),
            settlements = listOf(
                CloudSettlement(
                    payer = "Arif",
                    receiver = "Common Fund",
                    amount = 1612.0,
                    isSettled = false
                )
            )
        )

        // Sync to local Room DB
        val tripId = repository.syncRemoteTripToLocal(cloudTrip)
        assertTrue(tripId > 0)

        // Verify Trip
        val savedTrip = repository.getTripOnce(tripId)
        assertNotNull(savedTrip)
        assertEquals("Bandarban Expedition", savedTrip?.name)
        assertEquals("BNDR26", savedTrip?.tripCode)
        assertTrue(savedTrip?.isReadOnly == true)
        assertEquals("dev_host_123", savedTrip?.leaderDeviceId)
        assertEquals(80000.0, savedTrip?.budget ?: 0.0, 0.01)

        // Verify Members
        val savedMembers = repository.getMembersOnce(tripId)
        assertEquals(2, savedMembers.size)
        assertTrue(savedMembers.any { it.name == "Arif" })
        assertTrue(savedMembers.any { it.name == "Emon" })

        // Verify Deposits
        val savedDeposits = repository.getDepositsOnce(tripId)
        assertEquals(2, savedDeposits.size)
        assertEquals(10000.0, savedDeposits.sumOf { it.amount }, 0.01)

        // Verify Expenses & Splits
        val savedExpenses = repository.getExpensesOnce(tripId)
        assertEquals(1, savedExpenses.size)
        assertEquals("Chander Gari", savedExpenses[0].title)
        assertEquals(8000.0, savedExpenses[0].amount, 0.01)

        val savedSplits = repository.getAllSplitsOnce(tripId)
        assertEquals(2, savedSplits.size)
        assertEquals(4000.0, savedSplits[0].assignedAmount, 0.01)

        // Test subsequent update (live sync update replaces previous transactions cleanly)
        val updatedCloudTrip = cloudTrip.copy(
            transactions = mapOf(
                "tx_1" to CloudTransaction(
                    title = "Chander Gari",
                    amount = 8000.0,
                    category = "Transportation",
                    paidBy = "Common Fund",
                    timestamp = 1718000000000L
                ),
                "tx_2" to CloudTransaction(
                    title = "Resort Booking",
                    amount = 6000.0,
                    category = "Lodging & Resort",
                    paidBy = "Arif",
                    timestamp = 1718000100000L
                )
            )
        )

        val updatedTripId = repository.syncRemoteTripToLocal(updatedCloudTrip)
        assertEquals(tripId, updatedTripId)

        val reloadedExpenses = repository.getExpensesOnce(tripId)
        assertEquals(2, reloadedExpenses.size)
        assertEquals(14000.0, reloadedExpenses.sumOf { it.amount }, 0.01)
    }
}
