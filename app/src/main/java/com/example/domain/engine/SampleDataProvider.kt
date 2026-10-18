package com.example.domain.engine

import com.example.data.dao.DepositDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.MemberDao
import com.example.data.dao.TripDao
import com.example.data.entity.DepositEntity
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.MemberEntity
import com.example.data.entity.PayerType
import com.example.data.entity.SplitMode
import com.example.data.entity.TripEntity

object SampleDataProvider {

    suspend fun populateBandarbanTour(
        tripDao: TripDao,
        memberDao: MemberDao,
        depositDao: DepositDao,
        expenseDao: ExpenseDao
    ): Long {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        // 1. Create Trip
        val tripId = tripDao.insertTrip(
            TripEntity(
                name = "Bandarban Expedition 2026",
                destination = "Bandarban (Nilgiri - Thanchi - Nafakhum)",
                startDate = now - 2 * dayMillis,
                endDate = now + 2 * dayMillis,
                budget = 80000.0,
                currencySymbol = "৳",
                managerName = "Emon (Tour Lead)"
            )
        )

        // 2. Add 8 Tour Members
        val members = listOf(
            MemberEntity(tripId = tripId, name = "Emon", phoneNumber = "+8801711000001", colorHex = "#006C4C", avatarInitials = "EM", isManager = true),
            MemberEntity(tripId = tripId, name = "Shakib", phoneNumber = "+8801811000002", colorHex = "#00658E", avatarInitials = "SK"),
            MemberEntity(tripId = tripId, name = "Tanvir", phoneNumber = "+8801911000003", colorHex = "#835400", avatarInitials = "TV"),
            MemberEntity(tripId = tripId, name = "Nafis", phoneNumber = "+8801611000004", colorHex = "#7B1FA2", avatarInitials = "NF"),
            MemberEntity(tripId = tripId, name = "Arif", phoneNumber = "+8801511000005", colorHex = "#C2185B", avatarInitials = "AR"),
            MemberEntity(tripId = tripId, name = "Rifat", phoneNumber = "+8801311000006", colorHex = "#00796B", avatarInitials = "RF"),
            MemberEntity(tripId = tripId, name = "Zubair", phoneNumber = "+8801711000007", colorHex = "#E65100", avatarInitials = "ZB"),
            MemberEntity(tripId = tripId, name = "Hasib", phoneNumber = "+8801811000008", colorHex = "#303F9F", avatarInitials = "HB")
        )
        val memberIds = memberDao.insertMembers(members)

        // 3. Add Advance Deposits ("চাঁদা" Pool: ৳6,000 per member into Tour Manager Emon's pool)
        for (mId in memberIds) {
            depositDao.insertDeposit(
                DepositEntity(
                    tripId = tripId,
                    memberId = mId,
                    amount = 6000.0,
                    date = now - 2 * dayMillis,
                    notes = "Advance Tour Fund (চাঁদা Round 1)"
                )
            )
        }

        // 4. Add Realistic Bandarban Tour Expenses
        // Expense 1: AC Bus Tickets Dhaka -> Bandarban (Common Fund, 8 members equal split)
        val exp1 = ExpenseEntity(
            tripId = tripId,
            title = "Saintmartin Paribahan AC Bus (Dhaka -> Bandarban)",
            amount = 12000.0,
            category = ExpenseCategory.TRANSPORTATION,
            date = now - 2 * dayMillis + 3600000L,
            payerType = PayerType.COMMON_FUND,
            splitMode = SplitMode.EQUAL,
            notes = "8 tickets @ ৳1,500"
        )
        val splits1 = memberIds.map {
            ExpenseSplitEntity(expenseId = 0, memberId = it, assignedAmount = 1500.0)
        }
        expenseDao.insertExpenseWithSplits(exp1, splits1)

        // Expense 2: Nilgiri Eco Resort Cottage Booking (Common Fund)
        val exp2 = ExpenseEntity(
            tripId = tripId,
            title = "Nilgiri Eco Resort (2 Cottages, 1 Night)",
            amount = 16000.0,
            category = ExpenseCategory.LODGING,
            date = now - 1 * dayMillis,
            payerType = PayerType.COMMON_FUND,
            splitMode = SplitMode.EQUAL,
            notes = "Army Authority Cottage booking"
        )
        val splits2 = memberIds.map {
            ExpenseSplitEntity(expenseId = 0, memberId = it, assignedAmount = 2000.0)
        }
        expenseDao.insertExpenseWithSplits(exp2, splits2)

        // Expense 3: Chander Gari 3-Day Reserve (Bandarban - Nilgiri - Thanchi)
        val exp3 = ExpenseEntity(
            tripId = tripId,
            title = "Chander Gari Reserve (3 Days Mountain Tour)",
            amount = 18000.0,
            category = ExpenseCategory.TRANSPORTATION,
            date = now - 1 * dayMillis + 7200000L,
            payerType = PayerType.COMMON_FUND,
            splitMode = SplitMode.EQUAL,
            notes = "Including driver food & fuel allowance"
        )
        val splits3 = memberIds.map {
            ExpenseSplitEntity(expenseId = 0, memberId = it, assignedAmount = 2250.0)
        }
        expenseDao.insertExpenseWithSplits(exp3, splits3)

        // Expense 4: Thanchi Army Camp & BGB Permission Fees + Registered Guide
        val exp4 = ExpenseEntity(
            tripId = tripId,
            title = "Thanchi BGB Permission & Guide Honorarium",
            amount = 4500.0,
            category = ExpenseCategory.GUIDE_PERMISSIONS,
            date = now - 1 * dayMillis + 14400000L,
            payerType = PayerType.COMMON_FUND,
            splitMode = SplitMode.EQUAL,
            notes = "Official guide fee + administration slips"
        )
        val splits4 = memberIds.map {
            ExpenseSplitEntity(expenseId = 0, memberId = it, assignedAmount = 562.5)
        }
        expenseDao.insertExpenseWithSplits(exp4, splits4)

        // Expense 5: Boga Lake Traditional Bamboo Chicken Dinner (Pocket paid by Tanvir!)
        val exp5 = ExpenseEntity(
            tripId = tripId,
            title = "Traditional Bamboo Chicken Dinner @ Boga Lake",
            amount = 4800.0,
            category = ExpenseCategory.FOOD,
            date = now - 12 * 3600000L,
            payerType = PayerType.MEMBER,
            payerMemberId = memberIds[2], // Tanvir paid
            splitMode = SplitMode.EQUAL,
            notes = "Tanvir paid out of pocket for all 8 members"
        )
        val splits5 = memberIds.map {
            ExpenseSplitEntity(expenseId = 0, memberId = it, assignedAmount = 600.0)
        }
        expenseDao.insertExpenseWithSplits(exp5, splits5)

        // Expense 6: Remakri Engine Boat Ride (Selective Split: 6 members went, 2 stayed back at resort)
        val wentMembers = memberIds.take(6)
        val exp6 = ExpenseEntity(
            tripId = tripId,
            title = "Engine Boat Ride (Thanchi to Remakri)",
            amount = 3600.0,
            category = ExpenseCategory.TRANSPORTATION,
            date = now - 6 * 3600000L,
            payerType = PayerType.COMMON_FUND,
            splitMode = SplitMode.SELECTIVE,
            notes = "6 members participated (Zubair & Hasib stayed at camp)"
        )
        val splits6 = wentMembers.map {
            ExpenseSplitEntity(expenseId = 0, memberId = it, assignedAmount = 600.0)
        }
        expenseDao.insertExpenseWithSplits(exp6, splits6)

        // Expense 7: Evening Hill Cha & Papaya Snacks (Pocket paid by Shakib)
        val exp7 = ExpenseEntity(
            tripId = tripId,
            title = "Hilltop Raw Cha & Fresh Papaya Snacks",
            amount = 800.0,
            category = ExpenseCategory.SNACKS_PERSONAL,
            date = now - 2 * 3600000L,
            payerType = PayerType.MEMBER,
            payerMemberId = memberIds[1], // Shakib paid
            splitMode = SplitMode.EQUAL,
            notes = "Shakib treated evening tea"
        )
        val splits7 = memberIds.map {
            ExpenseSplitEntity(expenseId = 0, memberId = it, assignedAmount = 100.0)
        }
        expenseDao.insertExpenseWithSplits(exp7, splits7)

        return tripId
    }
}
