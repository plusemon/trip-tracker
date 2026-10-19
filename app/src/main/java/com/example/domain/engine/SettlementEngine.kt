package com.example.domain.engine

import com.example.data.entity.DepositEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpensePayerEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.MemberEntity
import com.example.data.entity.PayerType
import com.example.data.entity.SettlementRecordEntity
import com.example.domain.model.MemberBalance
import com.example.domain.model.SettlementTransaction
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

object SettlementEngine {

    const val COMMON_FUND_ID = -1L
    const val COMMON_FUND_NAME = "Group Fund"
    const val GROUP_FUND_NAME = "Group Fund"
    const val COMMON_FUND_COLOR = "#FFBA35"

    /**
     * Calculates the individual financial ledger balance for each member.
     */
    fun calculateMemberBalances(
        members: List<MemberEntity>,
        deposits: List<DepositEntity>,
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
        payers: List<ExpensePayerEntity> = emptyList()
    ): List<MemberBalance> {
        val memberMap = members.associateBy { it.id }

        // Sum deposits per member
        val depositTotals = mutableMapOf<Long, Double>()
        for (d in deposits) {
            if (d.memberId != null) {
                depositTotals[d.memberId] = (depositTotals[d.memberId] ?: 0.0) + d.amount
            }
        }

        // Sum pocket expenses paid per member
        val pocketTotals = mutableMapOf<Long, Double>()
        val nonIncomeExpenses = expenses.filter { !it.isIncome }
        val payersByExpense = payers.groupBy { it.expenseId }

        for (e in nonIncomeExpenses) {
            when (e.payerType) {
                PayerType.MEMBER -> {
                    if (e.payerMemberId != null) {
                        pocketTotals[e.payerMemberId] = (pocketTotals[e.payerMemberId] ?: 0.0) + e.amount
                    }
                }
                PayerType.MULTI_PAYER -> {
                    val expPayers = payersByExpense[e.id] ?: emptyList()
                    for (p in expPayers) {
                        pocketTotals[p.memberId] = (pocketTotals[p.memberId] ?: 0.0) + p.amountPaid
                    }
                }
                PayerType.COMMON_FUND -> {
                    // Paid from common fund pool
                }
            }
        }

        // Sum consumed shares per member
        val consumedTotals = mutableMapOf<Long, Double>()
        for (s in splits) {
            consumedTotals[s.memberId] = (consumedTotals[s.memberId] ?: 0.0) + s.assignedAmount
        }

        return members.map { member ->
            MemberBalance(
                member = member,
                totalDeposited = depositTotals[member.id] ?: 0.0,
                totalPocketPaid = pocketTotals[member.id] ?: 0.0,
                totalConsumed = consumedTotals[member.id] ?: 0.0
            )
        }
    }

    /**
     * Executes the Minimum Cash Flow greedy algorithm.
     * Minimizes the total number of transactions needed to settle all debts.
     */
    fun calculateMinimizedSettlements(
        members: List<MemberEntity>,
        deposits: List<DepositEntity>,
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
        settledRecords: List<SettlementRecordEntity> = emptyList(),
        payers: List<ExpensePayerEntity> = emptyList()
    ): List<SettlementTransaction> {
        if (members.isEmpty()) return emptyList()

        val memberBalances = calculateMemberBalances(members, deposits, expenses, splits, payers)
        val memberMap = members.associateBy { it.id }

        // Total cash remaining in Group Fund
        val totalDeposits = deposits.sumOf { it.amount }
        val commonFundSpent = expenses.filter { !it.isIncome && it.payerType == PayerType.COMMON_FUND }.sumOf { it.amount }
        val remainingCommonFund = totalDeposits - commonFundSpent

        // Net balances map
        val netMap = mutableMapOf<Long, Double>()
        for (mb in memberBalances) {
            netMap[mb.member.id] = mb.netBalance
        }

        // Include Group Fund entity if there is remaining cash or deficit in the group wallet
        if (abs(remainingCommonFund) > 0.5) {
            // If Group Fund has leftover cash (+R), it owes R back to members (-R in balance terms)
            netMap[COMMON_FUND_ID] = -remainingCommonFund
        }

        // Adjust for already settled records
        for (rec in settledRecords) {
            val from = rec.fromMemberId
            val to = rec.toMemberId
            // rec means 'from' paid 'to' amount
            netMap[from] = (netMap[from] ?: 0.0) + rec.amount
            netMap[to] = (netMap[to] ?: 0.0) - rec.amount
        }

        // Separate into Debtors (< -0.5) and Creditors (> 0.5)
        data class Node(val id: Long, var amount: Double)

        val debtors = PriorityQueue<Node> { a, b -> b.amount.compareTo(a.amount) }
        val creditors = PriorityQueue<Node> { a, b -> b.amount.compareTo(a.amount) }

        for ((id, balance) in netMap) {
            val rounded = (balance * 100.0).roundToInt() / 100.0
            if (rounded < -0.5) {
                debtors.add(Node(id, abs(rounded)))
            } else if (rounded > 0.5) {
                creditors.add(Node(id, rounded))
            }
        }

        val transactions = mutableListOf<SettlementTransaction>()

        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            val debtor = debtors.poll()!!
            val creditor = creditors.poll()!!

            val transferAmount = min(debtor.amount, creditor.amount)
            val roundedTransfer = (transferAmount * 100.0).roundToInt() / 100.0

            if (roundedTransfer > 0.5) {
                val fromName = if (debtor.id == COMMON_FUND_ID) COMMON_FUND_NAME else (memberMap[debtor.id]?.name ?: "Member #${debtor.id}")
                val fromColor = if (debtor.id == COMMON_FUND_ID) COMMON_FUND_COLOR else (memberMap[debtor.id]?.colorHex ?: "#888888")

                val toName = if (creditor.id == COMMON_FUND_ID) COMMON_FUND_NAME else (memberMap[creditor.id]?.name ?: "Member #${creditor.id}")
                val toColor = if (creditor.id == COMMON_FUND_ID) COMMON_FUND_COLOR else (memberMap[creditor.id]?.colorHex ?: "#888888")

                transactions.add(
                    SettlementTransaction(
                        fromMemberId = debtor.id,
                        fromMemberName = fromName,
                        fromMemberColor = fromColor,
                        toMemberId = creditor.id,
                        toMemberName = toName,
                        toMemberColor = toColor,
                        amount = roundedTransfer,
                        isSettled = false
                    )
                )
            }

            debtor.amount -= transferAmount
            creditor.amount -= transferAmount

            if (debtor.amount > 0.5) {
                debtors.add(debtor)
            }
            if (creditor.amount > 0.5) {
                creditors.add(creditor)
            }
        }

        return transactions
    }
}
