package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.MemberEntity
import com.example.data.entity.PayerType
import com.example.data.entity.SplitMode
import com.example.domain.model.CurrencyFormatter
import com.example.ui.components.MemberAvatar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseDialog(
    members: List<MemberEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAddExpense: (
        title: String,
        amount: Double,
        category: ExpenseCategory,
        date: Long,
        payerType: PayerType,
        payerMemberId: Long?,
        splitMode: SplitMode,
        customShares: Map<Long, Double>,
        selectedMemberIds: Set<Long>,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.TRANSPORTATION) }
    var payerType by remember { mutableStateOf(PayerType.COMMON_FUND) } // Default Common Fund as confirmed
    var selectedPayerMemberId by remember { mutableStateOf(members.firstOrNull()?.id) }
    var splitMode by remember { mutableStateOf(SplitMode.EQUAL) }
    var notes by remember { mutableStateOf("") }

    // Selected members for Selective Split (default all selected)
    val selectedMembersForSplit = remember {
        mutableStateMapOf<Long, Boolean>().apply {
            members.forEach { put(it.id, true) }
        }
    }

    // Custom exact amounts per member
    val customAmounts = remember {
        mutableStateMapOf<Long, String>().apply {
            members.forEach { put(it.id, "") }
        }
    }

    val totalAmount = amountText.toDoubleOrNull() ?: 0.0

    // Quick suggestion titles for Bangladeshi tours
    val quickSuggestions = listOf(
        "Chander Gari", "Bamboo Chicken", "Resort Cottage",
        "Engine Boat", "Army Entry Fee", "Cha & Snacks"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Log Tour Expense",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_expense_dialog")) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick suggestion chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickSuggestions.forEach { suggestion ->
                    Surface(
                        modifier = Modifier.clickable {
                            title = suggestion
                            when (suggestion) {
                                "Chander Gari" -> selectedCategory = ExpenseCategory.TRANSPORTATION
                                "Bamboo Chicken" -> selectedCategory = ExpenseCategory.FOOD
                                "Resort Cottage" -> selectedCategory = ExpenseCategory.LODGING
                                "Engine Boat" -> selectedCategory = ExpenseCategory.TRANSPORTATION
                                "Army Entry Fee" -> selectedCategory = ExpenseCategory.GUIDE_PERMISSIONS
                                "Cha & Snacks" -> selectedCategory = ExpenseCategory.SNACKS_PERSONAL
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title & Amount
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Expense Title (e.g. Chander Gari, Lunch)") },
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_title_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount ($currencySymbol BDT)") },
                leadingIcon = { Icon(Icons.Default.Paid, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_amount_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Chips
            Text(
                text = "Expense Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ExpenseCategory.values().forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.displayName) },
                        leadingIcon = {
                            Icon(cat.getIcon(), contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Who Paid? (Payer Source)
            Text(
                text = "Who Paid for this?",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = payerType == PayerType.COMMON_FUND,
                    onClick = { payerType = PayerType.COMMON_FUND },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Common Fund")
                }
                SegmentedButton(
                    selected = payerType == PayerType.MEMBER,
                    onClick = { payerType = PayerType.MEMBER },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Member Pocket")
                }
            }

            // Member selector if Member Pocket is selected
            if (payerType == PayerType.MEMBER) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    members.forEach { m ->
                        FilterChip(
                            selected = selectedPayerMemberId == m.id,
                            onClick = { selectedPayerMemberId = m.id },
                            label = { Text(m.name) },
                            leadingIcon = {
                                MemberAvatar(member = m, size = 20.dp)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Split Mode Selector
            Text(
                text = "Splitting Method",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SplitMode.values().forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = splitMode == mode,
                        onClick = { splitMode = mode },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = SplitMode.values().size)
                    ) {
                        Text(
                            when (mode) {
                                SplitMode.EQUAL -> "Equal"
                                SplitMode.SELECTIVE -> "Selective"
                                SplitMode.CUSTOM -> "Custom Exact"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Splitting Details based on mode
            when (splitMode) {
                SplitMode.EQUAL -> {
                    val share = if (members.isNotEmpty() && totalAmount > 0) totalAmount / members.size else 0.0
                    Text(
                        text = "Split equally among all ${members.size} members: ${CurrencyFormatter.formatTaka(share, currencySymbol)} each",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                SplitMode.SELECTIVE -> {
                    val selectedCount = selectedMembersForSplit.values.count { it }
                    val share = if (selectedCount > 0 && totalAmount > 0) totalAmount / selectedCount else 0.0
                    Text(
                        text = "Select who participated ($selectedCount of ${members.size}): ${CurrencyFormatter.formatTaka(share, currencySymbol)} each",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    members.forEach { member ->
                        val isChecked = selectedMembersForSplit[member.id] ?: true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMembersForSplit[member.id] = !isChecked }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { selectedMembersForSplit[member.id] = it }
                            )
                            MemberAvatar(member = member, size = 26.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(member.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                SplitMode.CUSTOM -> {
                    val allocatedSum = customAmounts.values.mapNotNull { it.toDoubleOrNull() }.sum()
                    val diff = totalAmount - allocatedSum
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Allocated: ${CurrencyFormatter.formatTaka(allocatedSum, currencySymbol)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (kotlin.math.abs(diff) < 0.1) "✓ Balanced" else "Remaining: ${CurrencyFormatter.formatTaka(diff, currencySymbol)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (kotlin.math.abs(diff) < 0.1) Color(0xFF1B6D24) else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    members.forEach { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MemberAvatar(member = member, size = 26.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(member.name, modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = customAmounts[member.id] ?: "",
                                onValueChange = { customAmounts[member.id] = it },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(110.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            val canSubmit = title.isNotBlank() && totalAmount > 0.0 && (
                splitMode != SplitMode.SELECTIVE || selectedMembersForSplit.values.any { it }
            )

            Button(
                onClick = {
                    val customMap = when (splitMode) {
                        SplitMode.CUSTOM -> customAmounts.mapNotNull { (mId, text) ->
                            val amt = text.toDoubleOrNull() ?: 0.0
                            if (amt > 0) mId to amt else null
                        }.toMap()
                        else -> emptyMap()
                    }
                    val selectedSet = selectedMembersForSplit.filter { it.value }.keys

                    onAddExpense(
                        title.trim(),
                        totalAmount,
                        selectedCategory,
                        System.currentTimeMillis(),
                        payerType,
                        if (payerType == PayerType.MEMBER) selectedPayerMemberId else null,
                        splitMode,
                        customMap,
                        selectedSet,
                        notes.trim()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_expense_button"),
                enabled = canSubmit
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Expense", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
