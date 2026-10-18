package com.example.domain.model

import com.example.data.entity.CategoryEntity
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpensePayerEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.MemberEntity
import com.example.data.entity.ReceiptAttachmentEntity
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
    val categoryName: String,
    val iconName: String,
    val colorHex: String,
    val totalAmount: Double,
    val percentage: Float,
    val count: Int
)

data class ExpenseWithDetails(
    val expense: ExpenseEntity,
    val splits: List<ExpenseSplitEntity> = emptyList(),
    val payers: List<ExpensePayerEntity> = emptyList(),
    val attachments: List<ReceiptAttachmentEntity> = emptyList()
)

data class TripSummary(
    val trip: TripEntity,
    val totalDeposited: Double,
    val commonFundSpent: Double,
    val remainingCommonFund: Double,
    val totalSpentOverall: Double,
    val budget: Double,
    val budgetProgress: Float,
    val highestCategory: String?,
    val highestCategoryAmount: Double,
    val topSpender: MemberEntity?,
    val topSpenderAmount: Double,
    val memberCount: Int,
    val expenseCount: Int
)

object CurrencyFormatter {
    fun formatTaka(amount: Double, symbol: String = "৳"): String = formatCurrency(amount, symbol)
    fun formatSignedTaka(amount: Double, symbol: String = "৳"): String = formatSignedCurrency(amount, symbol)

    fun formatCurrency(amount: Double, symbol: String = "৳"): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        val formattedNumber = formatter.format(amount)
        return "$symbol$formattedNumber"
    }

    fun formatCurrencyDecimal(amount: Double, symbol: String = "৳"): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        val formattedNumber = formatter.format(amount)
        return "$symbol$formattedNumber"
    }

    fun formatSignedCurrency(amount: Double, symbol: String = "৳"): String {
        val sign = if (amount > 0.01) "+" else if (amount < -0.01) "-" else ""
        val absAmount = kotlin.math.abs(amount)
        return "$sign${formatCurrency(absAmount, symbol)}"
    }
}

data class NewMemberInfo(
    val name: String,
    val phoneNumber: String = "",
    val colorHex: String = "#006C4C",
    val isManager: Boolean = false,
    val initialDeposit: Double = 0.0
)

data class CurrencyPreset(
    val code: String,
    val name: String,
    val symbol: String,
    val defaultRateToBdt: Double
)

object CurrencyPresets {
    val PRESETS = listOf(
        CurrencyPreset("BDT", "Bangladeshi Taka", "৳", 1.0),
        CurrencyPreset("USD", "US Dollar", "$", 122.0),
        CurrencyPreset("THB", "Thai Baht", "฿", 3.6),
        CurrencyPreset("INR", "Indian Rupee", "₹", 1.45),
        CurrencyPreset("MYR", "Malaysian Ringgit", "RM", 28.0),
        CurrencyPreset("EUR", "Euro", "€", 132.0),
        CurrencyPreset("SGD", "Singapore Dollar", "S$", 92.0),
        CurrencyPreset("AED", "UAE Dirham", "AED", 33.2),
        CurrencyPreset("SAR", "Saudi Riyal", "SAR", 32.5)
    )
}
