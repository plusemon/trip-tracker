package com.example

import com.example.data.entity.DepositEntity
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.MemberEntity
import com.example.data.entity.PayerType
import com.example.data.entity.SplitMode
import com.example.domain.engine.SettlementEngine
import com.example.domain.model.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SettlementEngineTest {

    @Test
    fun testCurrencyFormatting() {
        assertEquals("৳12,500", CurrencyFormatter.formatTaka(12500.0))
        assertEquals("৳0", CurrencyFormatter.formatTaka(0.0))
        assertEquals("৳1,000,000", CurrencyFormatter.formatTaka(1000000.0))
    }

    @Test
    fun testPeerToPeerDebtSimplification() {
        val members = listOf(
            MemberEntity(id = 1, tripId = 1, name = "A"),
            MemberEntity(id = 2, tripId = 1, name = "B"),
            MemberEntity(id = 3, tripId = 1, name = "C")
        )

        // A pays 3000 out of pocket for dinner for all 3 members (1000 each)
        val expense = ExpenseEntity(
            id = 10,
            tripId = 1,
            title = "Dinner",
            amount = 3000.0,
            category = ExpenseCategory.FOOD,
            payerType = PayerType.MEMBER,
            payerMemberId = 1,
            splitMode = SplitMode.EQUAL
        )

        val splits = listOf(
            ExpenseSplitEntity(id = 1, expenseId = 10, memberId = 1, assignedAmount = 1000.0),
            ExpenseSplitEntity(id = 2, expenseId = 10, memberId = 2, assignedAmount = 1000.0),
            ExpenseSplitEntity(id = 3, expenseId = 10, memberId = 3, assignedAmount = 1000.0)
        )

        val balances = SettlementEngine.calculateMemberBalances(
            members = members,
            deposits = emptyList(),
            expenses = listOf(expense),
            splits = splits
        )

        val balA = balances.first { it.member.id == 1L }
        val balB = balances.first { it.member.id == 2L }
        val balC = balances.first { it.member.id == 3L }

        // A paid 3000, consumed 1000 -> net +2000
        assertEquals(2000.0, balA.netBalance, 0.01)
        // B paid 0, consumed 1000 -> net -1000
        assertEquals(-1000.0, balB.netBalance, 0.01)
        // C paid 0, consumed 1000 -> net -1000
        assertEquals(-1000.0, balC.netBalance, 0.01)

        val settlements = SettlementEngine.calculateMinimizedSettlements(
            members = members,
            deposits = emptyList(),
            expenses = listOf(expense),
            splits = splits
        )

        // Exactly 2 transactions needed (B -> A: 1000, C -> A: 1000)
        assertEquals(2, settlements.size)
        assertTrue(settlements.all { it.toMemberId == 1L })
        assertEquals(2000.0, settlements.sumOf { it.amount }, 0.01)
    }

    @Test
    fun testCommonFundCollectionAndRefund() {
        val members = listOf(
            MemberEntity(id = 1, tripId = 1, name = "Emon"),
            MemberEntity(id = 2, tripId = 1, name = "Shakib")
        )

        // Each deposits 5000 in Common Fund (Total 10,000)
        val deposits = listOf(
            DepositEntity(id = 1, tripId = 1, memberId = 1, amount = 5000.0),
            DepositEntity(id = 2, tripId = 1, memberId = 2, amount = 5000.0)
        )

        // 6000 spent from Common Fund for Chander Gari (3000 each)
        val expense = ExpenseEntity(
            id = 20,
            tripId = 1,
            title = "Chander Gari",
            amount = 6000.0,
            category = ExpenseCategory.TRANSPORTATION,
            payerType = PayerType.COMMON_FUND,
            splitMode = SplitMode.EQUAL
        )
        val splits = listOf(
            ExpenseSplitEntity(id = 1, expenseId = 20, memberId = 1, assignedAmount = 3000.0),
            ExpenseSplitEntity(id = 2, expenseId = 20, memberId = 2, assignedAmount = 3000.0)
        )

        val balances = SettlementEngine.calculateMemberBalances(
            members = members,
            deposits = deposits,
            expenses = listOf(expense),
            splits = splits
        )

        // Both deposited 5000, consumed 3000 -> both get back 2000
        for (b in balances) {
            assertEquals(2000.0, b.netBalance, 0.01)
        }

        val settlements = SettlementEngine.calculateMinimizedSettlements(
            members = members,
            deposits = deposits,
            expenses = listOf(expense),
            splits = splits
        )

        // Common Fund has 4000 cash in hand, refunds 2000 to Emon and 2000 to Shakib
        assertEquals(2, settlements.size)
        assertTrue(settlements.all { it.fromMemberId == SettlementEngine.COMMON_FUND_ID })
        assertEquals(4000.0, settlements.sumOf { it.amount }, 0.01)

        // Now simulate settling one of the refund transactions (Common Fund refunds 2000 to Emon)
        val settledRecord = com.example.data.entity.SettlementRecordEntity(
            tripId = 1,
            fromMemberId = SettlementEngine.COMMON_FUND_ID,
            toMemberId = 1,
            fromMemberName = SettlementEngine.COMMON_FUND_NAME,
            toMemberName = "Emon",
            amount = 2000.0
        )

        val remainingSettlements = SettlementEngine.calculateMinimizedSettlements(
            members = members,
            deposits = deposits,
            expenses = listOf(expense),
            splits = splits,
            settledRecords = listOf(settledRecord)
        )

        // Only 1 settlement left: Common Fund -> Shakib 2000
        assertEquals(1, remainingSettlements.size)
        assertEquals(2L, remainingSettlements[0].toMemberId)
        assertEquals(2000.0, remainingSettlements[0].amount, 0.01)
    }
}
