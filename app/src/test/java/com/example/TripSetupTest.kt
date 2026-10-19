package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.entity.TripEntity
import com.example.data.repository.TripSplitRepository
import com.example.domain.model.NewMemberInfo
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
class TripSetupTest {

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
    fun testTripCreationWithDetailedMembersAndInitialDeposits() = runTest {
        val trip = TripEntity(
            name = "Sajek Valley Cloud Expedition",
            destination = "Sajek, Rangamati",
            startDate = 1000L,
            endDate = 5000L,
            budget = 40000.0,
            currencySymbol = "৳",
            managerName = "Emon"
        )

        val members = listOf(
            NewMemberInfo(
                name = "Emon",
                phoneNumber = "01711000001",
                colorHex = "#006C4C",
                isManager = true,
                initialDeposit = 5000.0
            ),
            NewMemberInfo(
                name = "Shakib",
                phoneNumber = "01711000002",
                colorHex = "#00658E",
                isManager = false,
                initialDeposit = 5000.0
            ),
            NewMemberInfo(
                name = "Tanvir",
                phoneNumber = "01711000003",
                colorHex = "#835400",
                isManager = false,
                initialDeposit = 5000.0
            )
        )

        val tripId = repository.createTripWithDetailedMembers(trip, members)
        assertTrue(tripId > 0)

        // Verify Trip
        val savedTrip = repository.getTripOnce(tripId)
        assertNotNull(savedTrip)
        assertEquals("Sajek Valley Cloud Expedition", savedTrip?.name)
        assertEquals("Sajek, Rangamati", savedTrip?.destination)
        assertEquals(40000.0, savedTrip?.budget ?: 0.0, 0.01)

        // Verify Members
        val savedMembers = repository.getMembersOnce(tripId)
        assertEquals(3, savedMembers.size)
        val lead = savedMembers.firstOrNull { it.isManager }
        assertNotNull(lead)
        assertEquals("Emon", lead?.name)
        assertEquals("01711000001", lead?.phoneNumber)
        assertEquals("#006C4C", lead?.colorHex)

        // Verify Group Fund Advance Deposits
        val savedDeposits = repository.getDepositsOnce(tripId)
        assertEquals(3, savedDeposits.size)
        val totalDeposited = savedDeposits.sumOf { it.amount }
        assertEquals(15000.0, totalDeposited, 0.01)
    }

    @Test
    fun testTripCreationWithoutInitialDeposits() = runTest {
        val trip = TripEntity(
            name = "Bandarban Tour",
            destination = "Bandarban",
            startDate = 1000L,
            endDate = 4000L,
            budget = 30000.0
        )

        val members = listOf(
            NewMemberInfo(name = "Rahim", isManager = true, initialDeposit = 0.0),
            NewMemberInfo(name = "Karim", isManager = false, initialDeposit = 0.0)
        )

        val tripId = repository.createTripWithDetailedMembers(trip, members)
        val savedDeposits = repository.getDepositsOnce(tripId)
        assertEquals(0, savedDeposits.size)

        val savedMembers = repository.getMembersOnce(tripId)
        assertEquals(2, savedMembers.size)
    }
}
