package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.CategoryEntity
import com.example.data.entity.CategoryIconHelper
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.ExpensePayerEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.MemberEntity
import com.example.data.entity.PayerType
import com.example.data.entity.SplitMode
import com.example.domain.model.CurrencyFormatter
import com.example.ui.components.MemberAvatar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseDialog(
    members: List<MemberEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onOpenCreateCategory: () -> Unit,
    onAddExpense: (
        title: String,
        amount: Double,
        category: ExpenseCategory,
        customCatName: String?,
        customCatIcon: String?,
        customCatColor: String?,
        payerType: PayerType,
        payerMemberId: Long?,
        splitMode: SplitMode,
        splits: List<ExpenseSplitEntity>,
        payers: List<ExpensePayerEntity>,
        photoUris: List<Uri>,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    // Category
    var selectedStandardCategory by remember { mutableStateOf<ExpenseCategory?>(ExpenseCategory.FOOD) }
    var selectedCustomCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    // Payer
    var payerType by remember { mutableStateOf(PayerType.COMMON_FUND) }
    var selectedSinglePayerId by remember { mutableStateOf(members.firstOrNull()?.id) }
    val jointPayerAmounts = remember { mutableStateMapOf<Long, String>() }

    // Split
    var splitMode by remember { mutableStateOf(SplitMode.EQUAL) }
    val selectedMemberIds = remember {
        mutableStateListOf<Long>().apply { addAll(members.map { it.id }) }
    }
    val customShareTexts = remember { mutableStateMapOf<Long, String>() }

    // Attachments
    val attachedUris = remember { mutableStateListOf<Uri>() }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        uris.forEach { uri ->
            if (!attachedUris.contains(uri)) attachedUris.add(uri)
        }
    }

    var notes by remember { mutableStateOf("") }

    val amount = amountText.toDoubleOrNull() ?: 0.0

    // Joint payer total check
    val jointTotalPaid = remember(jointPayerAmounts.toMap(), amount) {
        jointPayerAmounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
    }

    // Validation
    val isAmountValid = amount > 0.0
    val isTitleValid = title.isNotBlank()
    val isJointValid = payerType != PayerType.MULTI_PAYER || kotlin.math.abs(jointTotalPaid - amount) < 0.5
    val canSubmit = isAmountValid && isTitleValid && isJointValid

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Log Group Expense",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Expense Title") },
                placeholder = { Text("e.g., Resort Cottage, Bamboo Chicken, Fuel") },
                leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_title_input")
            )

            // Amount Input with Permanent ৳ Prefix
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (৳)") },
                leadingIcon = {
                    Text(
                        text = "৳",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_amount_input")
            )

            // Categories Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onOpenCreateCategory) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Custom Category", fontSize = 12.sp)
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Standard categories
                    ExpenseCategory.values().forEach { cat ->
                        val isSelected = selectedStandardCategory == cat && selectedCustomCategory == null
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedStandardCategory = cat
                                selectedCustomCategory = null
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.getIcon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(cat.displayName) }
                        )
                    }

                    // Custom categories
                    categories.filter { !it.isIncome }.forEach { customCat ->
                        val isSelected = selectedCustomCategory?.id == customCat.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCustomCategory = customCat
                                selectedStandardCategory = null
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryIconHelper.getIconForName(customCat.iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(android.graphics.Color.parseColor(customCat.colorHex))
                                )
                            },
                            label = { Text(customCat.name) }
                        )
                    }
                }
            }

            // Payer Source Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Paid By", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = payerType == PayerType.COMMON_FUND,
                        onClick = { payerType = PayerType.COMMON_FUND },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Common Fund", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    SegmentedButton(
                        selected = payerType == PayerType.MEMBER,
                        onClick = { payerType = PayerType.MEMBER },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Single Member", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    SegmentedButton(
                        selected = payerType == PayerType.MULTI_PAYER,
                        onClick = { payerType = PayerType.MULTI_PAYER },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("Joint / Split", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                when (payerType) {
                    PayerType.COMMON_FUND -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Paid directly from the group Common Fund pool (\"চাঁদা\"). Deducts cash in hand.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    PayerType.MEMBER -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            members.forEach { m ->
                                val isSelected = selectedSinglePayerId == m.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSinglePayerId = m.id },
                                    leadingIcon = { MemberAvatar(member = m, size = 20.dp) },
                                    label = { Text(m.name) }
                                )
                            }
                        }
                    }
                    PayerType.MULTI_PAYER -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Enter amount paid out-of-pocket by each member (Total must equal ${CurrencyFormatter.formatCurrency(amount)}):",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                members.forEach { m ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            MemberAvatar(member = m, size = 24.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(m.name, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        OutlinedTextField(
                                            value = jointPayerAmounts[m.id] ?: "",
                                            onValueChange = { jointPayerAmounts[m.id] = it },
                                            placeholder = { Text("0") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.width(110.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Current Sum: ${CurrencyFormatter.formatCurrency(jointTotalPaid)} / ${CurrencyFormatter.formatCurrency(amount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isJointValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Split Mode Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Split Between", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = splitMode == SplitMode.EQUAL,
                        onClick = { splitMode = SplitMode.EQUAL },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Equal All", fontSize = 12.sp)
                    }
                    SegmentedButton(
                        selected = splitMode == SplitMode.SELECTIVE,
                        onClick = { splitMode = SplitMode.SELECTIVE },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Selective", fontSize = 12.sp)
                    }
                    SegmentedButton(
                        selected = splitMode == SplitMode.CUSTOM,
                        onClick = { splitMode = SplitMode.CUSTOM },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("Custom", fontSize = 12.sp)
                    }
                }

                when (splitMode) {
                    SplitMode.EQUAL -> {
                        val share = if (members.isNotEmpty()) amount / members.size else 0.0
                        Text(
                            text = "Split equally among all ${members.size} members (${CurrencyFormatter.formatCurrency(share)} each).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    SplitMode.SELECTIVE -> {
                        val participating = members.filter { selectedMemberIds.contains(it.id) }
                        val share = if (participating.isNotEmpty()) amount / participating.size else 0.0
                        Text(
                            text = "Split equally among ${participating.size} selected members (${CurrencyFormatter.formatCurrency(share)} each):",
                            style = MaterialTheme.typography.bodySmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            members.forEach { m ->
                                val isChecked = selectedMemberIds.contains(m.id)
                                FilterChip(
                                    selected = isChecked,
                                    onClick = {
                                        if (isChecked) {
                                            if (selectedMemberIds.size > 1) selectedMemberIds.remove(m.id)
                                        } else {
                                            selectedMemberIds.add(m.id)
                                        }
                                    },
                                    leadingIcon = { MemberAvatar(member = m, size = 20.dp) },
                                    label = { Text(m.name) }
                                )
                            }
                        }
                    }
                    SplitMode.CUSTOM -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            members.forEach { m ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        MemberAvatar(member = m, size = 24.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(m.name, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    OutlinedTextField(
                                        value = customShareTexts[m.id] ?: "",
                                        onValueChange = { customShareTexts[m.id] = it },
                                        placeholder = { Text("0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.width(110.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Photo Attachments Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Receipt Photos & Memories", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Photos", fontSize = 12.sp)
                    }
                }

                if (attachedUris.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        attachedUris.forEach { uri ->
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Attached receipt",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { attachedUris.remove(uri) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(22.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Optional: Attach receipt bills, tickets, or tour photos saved locally offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Notes field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Description (Optional)") },
                placeholder = { Text("e.g. Paid cash to driver, slip #42") },
                modifier = Modifier.fillMaxWidth()
            )

            // Submit Button
            Button(
                onClick = {
                    if (canSubmit) {
                        // Calculate splits
                        val splitsToSave = when (splitMode) {
                            SplitMode.EQUAL -> {
                                val share = amount / members.size
                                members.map {
                                    ExpenseSplitEntity(expenseId = 0, memberId = it.id, assignedAmount = share)
                                }
                            }
                            SplitMode.SELECTIVE -> {
                                val active = members.filter { selectedMemberIds.contains(it.id) }
                                val share = if (active.isNotEmpty()) amount / active.size else 0.0
                                active.map {
                                    ExpenseSplitEntity(expenseId = 0, memberId = it.id, assignedAmount = share)
                                }
                            }
                            SplitMode.CUSTOM -> {
                                members.mapNotNull { m ->
                                    val amt = customShareTexts[m.id]?.toDoubleOrNull() ?: 0.0
                                    if (amt > 0) ExpenseSplitEntity(expenseId = 0, memberId = m.id, assignedAmount = amt)
                                    else null
                                }
                            }
                        }

                        // Calculate multi-payers
                        val payersToSave = if (payerType == PayerType.MULTI_PAYER) {
                            members.mapNotNull { m ->
                                val amt = jointPayerAmounts[m.id]?.toDoubleOrNull() ?: 0.0
                                if (amt > 0) ExpensePayerEntity(expenseId = 0, memberId = m.id, amountPaid = amt)
                                else null
                            }
                        } else emptyList()

                        val resolvedCategory = selectedStandardCategory ?: ExpenseCategory.FOOD
                        val customCatName = selectedCustomCategory?.name
                        val customCatIcon = selectedCustomCategory?.iconName
                        val customCatColor = selectedCustomCategory?.colorHex

                        onAddExpense(
                            title.trim(),
                            amount,
                            resolvedCategory,
                            customCatName,
                            customCatIcon,
                            customCatColor,
                            payerType,
                            selectedSinglePayerId,
                            splitMode,
                            splitsToSave,
                            payersToSave,
                            attachedUris.toList(),
                            notes.trim()
                        )
                    }
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_expense_button")
            ) {
                Text("Save Expense", fontWeight = FontWeight.Bold)
            }
        }
    }
}
