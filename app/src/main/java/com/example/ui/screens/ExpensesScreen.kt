package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CategoryIconHelper
import com.example.data.entity.DepositEntity
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpensePayerEntity
import com.example.data.entity.MemberEntity
import com.example.data.entity.PayerType
import com.example.data.entity.ReceiptAttachmentEntity
import com.example.domain.model.CurrencyFormatter
import com.example.ui.components.CategoryChip
import com.example.ui.components.CustomCategoryChip
import com.example.ui.components.MemberAvatar
import com.example.ui.components.ReceiptThumbnail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpensesScreen(
    expenses: List<ExpenseEntity>,
    deposits: List<DepositEntity>,
    members: List<MemberEntity>,
    attachments: List<ReceiptAttachmentEntity>,
    payers: List<ExpensePayerEntity>,
    currencySymbol: String,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onViewPhoto: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabFilter by remember { mutableStateOf(0) } // 0: All, 1: Expenses, 2: Incomes/Deposits
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    val memberMap = remember(members) { members.associateBy { it.id } }
    val attachmentsByExpense = remember(attachments) { attachments.groupBy { it.expenseId } }
    val payersByExpense = remember(payers) { payers.groupBy { it.expenseId } }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }

    val filteredExpenses = remember(expenses, selectedCategoryFilter, selectedTabFilter) {
        if (selectedTabFilter == 2) emptyList()
        else {
            if (selectedCategoryFilter == null) expenses
            else expenses.filter {
                (it.customCategoryName ?: it.category.displayName) == selectedCategoryFilter
            }
        }
    }

    val filteredDeposits = remember(deposits, selectedTabFilter) {
        if (selectedTabFilter == 1) emptyList()
        else deposits
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
        ) {
        // Tab Filter: All, Expenses, Incomes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTabFilter == 0,
                onClick = { selectedTabFilter = 0 },
                label = { Text("All (${expenses.size + deposits.size})") }
            )
            FilterChip(
                selected = selectedTabFilter == 1,
                onClick = { selectedTabFilter = 1 },
                label = { Text("Expenses (${expenses.size})") }
            )
            FilterChip(
                selected = selectedTabFilter == 2,
                onClick = { selectedTabFilter = 2 },
                label = { Text("Incomes / Deposits (${deposits.size})") }
            )
        }

        // Category Filter Chips
        if (selectedTabFilter != 2 && expenses.isNotEmpty()) {
            val availableCategories = expenses.map { it.customCategoryName ?: it.category.displayName }.distinct()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null },
                    label = { Text("All Categories") }
                )
                availableCategories.forEach { catName ->
                    FilterChip(
                        selected = selectedCategoryFilter == catName,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == catName) null else catName
                        },
                        label = { Text(catName) }
                    )
                }
            }
        }

        // List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Show Deposits if applicable
            if (selectedTabFilter != 1 && filteredDeposits.isNotEmpty()) {
                item {
                    Text(
                        text = "Fund Deposits & Inflows",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                items(filteredDeposits) { deposit ->
                    val contributor = deposit.contributorName.ifBlank {
                        deposit.memberId?.let { memberMap[it]?.name } ?: "Contributor"
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = deposit.categoryName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "From: $contributor • ${dateFormat.format(Date(deposit.date))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (deposit.notes.isNotBlank()) {
                                    Text(
                                        text = deposit.notes,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+${CurrencyFormatter.formatCurrency(deposit.amount, currencySymbol)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (deposit.currencyCode != "BDT" && deposit.originalAmount != deposit.amount) {
                                    Text(
                                        text = "${deposit.originalAmount} ${deposit.currencyCode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Show Expenses
            if (selectedTabFilter != 2) {
                if (filteredExpenses.isNotEmpty() && selectedTabFilter == 0) {
                    item {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                items(filteredExpenses, key = { it.id }) { expense ->
                    val expAttachments = attachmentsByExpense[expense.id] ?: emptyList()
                    val expPayers = payersByExpense[expense.id] ?: emptyList()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_card_${expense.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category Icon
                                val iconName = expense.customCategoryIcon ?: expense.category.iconName
                                val iconColorHex = expense.customCategoryColor ?: expense.category.defaultColorHex
                                val iconColor = runCatching { Color(android.graphics.Color.parseColor(iconColorHex)) }.getOrDefault(MaterialTheme.colorScheme.primary)

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(iconColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIconForName(iconName),
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = expense.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (expense.customCategoryName != null) {
                                            CustomCategoryChip(
                                                name = expense.customCategoryName,
                                                iconName = iconName,
                                                colorHex = iconColorHex
                                            )
                                        } else {
                                            CategoryChip(category = expense.category)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = dateFormat.format(Date(expense.date)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.formatCurrency(expense.amount, currencySymbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (expense.currencyCode != "BDT" && expense.originalAmount != expense.amount) {
                                        Text(
                                            text = "${expense.originalAmount} ${expense.currencyCode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    IconButton(
                                        onClick = { expenseToDelete = expense },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Payment source & payer summary
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val payerText = when (expense.payerType) {
                                    PayerType.COMMON_FUND -> "Paid from: Common Fund (\"চাঁদা\")"
                                    PayerType.MEMBER -> {
                                        val payerName = memberMap[expense.payerMemberId]?.name ?: "Member"
                                        "Paid by: $payerName"
                                    }
                                    PayerType.MULTI_PAYER -> {
                                        "Jointly paid by ${expPayers.size} members"
                                    }
                                }
                                Text(
                                    text = payerText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Split: ${expense.splitMode.name.lowercase().capitalize()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            // Multi-payer breakdown if applicable
                            if (expense.payerType == PayerType.MULTI_PAYER && expPayers.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    expPayers.forEach { p ->
                                        val mName = memberMap[p.memberId]?.name ?: "Member"
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = "$mName: ${CurrencyFormatter.formatCurrency(p.amountPaid, currencySymbol)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Receipt thumbnails strip
                            if (expAttachments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    expAttachments.forEach { att ->
                                        ReceiptThumbnail(
                                            filePath = att.filePath,
                                            onClick = { onViewPhoto(att.filePath) },
                                            size = 44.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

    // Delete Confirmation
    if (expenseToDelete != null) {
        val exp = expenseToDelete!!
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense?") },
            text = { Text("Are you sure you want to remove '${exp.title}' (${CurrencyFormatter.formatCurrency(exp.amount, currencySymbol)})? Debts will be recalculated.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(exp)
                        expenseToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
