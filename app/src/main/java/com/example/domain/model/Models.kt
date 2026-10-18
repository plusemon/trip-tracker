package com.example.domain.model

import com.example.data.entity.ExpenseCategory
import com.example.data.entity.MemberEntity
import com.example.data.entity.TripEntity
import java.text.NumberFormat
import java.util.Locale

data class MemberBalance(
    val member: MemberEntity,
    val totalDeposited: Double,
    val totalPocketPaid: Double,
    val totalConsumed: Double
) {
    val totalPaid: Double = totalDeposited + totalPocketPaid
    val netBalance: Double = totalPaid - totalConsumed

    val isCreditor: Boolean get() = netBalance > 0.01
    val isDebtor: Boolean get() = netBalance < -0.01
    val isBalanced: Boolean get() = !isCreditor && !isDebtor
}

data class SettlementTransaction(
    val fromMemberId: Long,
    val fromMemberName: String,
    val fromMemberColor: String,
    val toMemberId: Long,
    val toMemberName: String,
    val toMemberColor: String,
    val amount: Double,
    val isSettled: Boolean = false
)

data class CategorySpending(
    val category: ExpenseCategory,
    val totalAmount: Double,
    val percentage: Float,
    val count: Int
)

data class TripSummary(
    val trip: TripEntity,
    val totalDeposited: Double,
    val commonFundSpent: Double,
    val remainingCommonFund: Double,
    val totalSpentOverall: Double,
    val budget: Double,
    val budgetProgress: Float,
    val highestCategory: ExpenseCategory?,
    val highestCategoryAmount: Double,
    val topSpender: MemberEntity?,
    val topSpenderAmount: Double,
    val memberCount: Int,
    val expenseCount: Int
)

object CurrencyFormatter {
    fun formatTaka(amount: Double, symbol: String = "৳"): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        val formattedNumber = formatter.format(amount)
        return "$symbol$formattedNumber"
    }

    fun formatSignedTaka(amount: Double, symbol: String = "৳"): String {
        val sign = if (amount > 0.01) "+" else if (amount < -0.01) "-" else ""
        val absAmount = kotlin.math.abs(amount)
        return "$sign${formatTaka(absAmount, symbol)}"
    }
}

data class NewMemberInfo(
    val name: String,
    val phoneNumber: String = "",
    val colorHex: String = "#006C4C",
    val isManager: Boolean = false,
    val initialDeposit: Double = 0.0
)
