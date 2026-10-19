package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ExchangeRateEntity
import com.example.data.entity.MemberEntity
import com.example.domain.model.CurrencyFormatter
import com.example.domain.model.CurrencyPresets
import com.example.ui.components.MemberAvatar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DepositFundDialog(
    members: List<MemberEntity>,
    categories: List<CategoryEntity>,
    exchangeRates: List<ExchangeRateEntity>,
    baseCurrency: String,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAddIncome: (
        amount: Double,
        originalAmount: Double,
        currencyCode: String,
        exchangeRate: Double,
        contributorName: String,
        memberId: Long?,
        categoryName: String,
        notes: String,
        date: Long
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isFromMember by remember { mutableStateOf(true) }
    var selectedMemberId by remember { mutableStateOf(members.firstOrNull()?.id) }
    var externalContributor by remember { mutableStateOf("") }

    var amountText by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(baseCurrency) }
    var customRateText by remember {
        val r = exchangeRates.find { it.fromCurrency == baseCurrency }?.rate ?: 1.0
        mutableStateOf(r.toString())
    }

    var selectedCategoryName by remember { mutableStateOf("Common Fund Deposit") }
    var notes by remember { mutableStateOf("") }

    val quickAmounts = listOf("1000", "2000", "3000", "5000", "10000")

    val originalAmount = amountText.toDoubleOrNull() ?: 0.0
    val exchangeRate = if (selectedCurrency == baseCurrency) 1.0 else (customRateText.toDoubleOrNull() ?: 1.0)
    val convertedBaseAmount = originalAmount * exchangeRate

    val canSubmit = originalAmount > 0 && (if (isFromMember) selectedMemberId != null else externalContributor.isNotBlank())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enter Income / Fund Pool",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_deposit_dialog")) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Inflow Source Type: Member vs External
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = isFromMember,
                    onClick = { isFromMember = true },
                    label = { Text("Member Deposit (\"চাঁদা\")") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !isFromMember,
                    onClick = { isFromMember = false },
                    label = { Text("External / Inflow") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Member Selector or External Name
            if (isFromMember) {
                Text(
                    text = "Select Contributor",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    members.forEach { m ->
                        FilterChip(
                            selected = selectedMemberId == m.id,
                            onClick = { selectedMemberId = m.id },
                            label = { Text(m.name) },
                            leadingIcon = { MemberAvatar(member = m, size = 20.dp) }
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = externalContributor,
                    onValueChange = { externalContributor = it },
                    label = { Text("Source / Contributor Name") },
                    placeholder = { Text("e.g., Tour Sponsor, Resort Refund, Personal Allowance") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Income Category Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Income Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                val defaultIncomeCats = listOf(
                    "Common Fund Deposit (\"চাঁদা\")",
                    "Tour Sponsorship / Gift",
                    "Vendor / Booking Refund",
                    "Personal Travel Allowance",
                    "Other Inflow"
                )
                val customIncomes = categories.filter { it.isIncome }.map { it.name }
                val allInflowCats = (defaultIncomeCats + customIncomes).distinct()

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allInflowCats.forEach { catName ->
                        FilterChip(
                            selected = selectedCategoryName == catName,
                            onClick = { selectedCategoryName = catName },
                            label = { Text(catName) }
                        )
                    }
                }
            }

            // Currency & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($selectedCurrency)") },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("deposit_amount_input")
                )

                // Currency selector
                Column(modifier = Modifier.weight(1f)) {
                    Text("Currency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(baseCurrency, "USD", "THB", "INR", "MYR", "EUR").distinct().forEach { code ->
                            FilterChip(
                                selected = selectedCurrency == code,
                                onClick = {
                                    selectedCurrency = code
                                    val r = exchangeRates.find { it.fromCurrency == code }?.rate
                                        ?: CurrencyPresets.PRESETS.find { it.code == code }?.defaultRateToBdt ?: 1.0
                                    customRateText = r.toString()
                                },
                                label = { Text(code) }
                            )
                        }
                    }
                }
            }

            // Quick amounts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickAmounts.forEach { qAmt ->
                    Surface(
                        modifier = Modifier.clickable { amountText = qAmt },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "$currencySymbol$qAmt",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Currency conversion preview if non-base
            if (selectedCurrency != baseCurrency) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("1 $selectedCurrency = ", style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(
                                value = customRateText,
                                onValueChange = { customRateText = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(90.dp)
                            )
                            Text(" $baseCurrency", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = "≈ ${CurrencyFormatter.formatCurrency(convertedBaseAmount, currencySymbol)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Method") },
                placeholder = { Text("e.g., bKash, Cash, Bank Transfer") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deposit_notes_input"),
                singleLine = true
            )

            // Submit Button
            Button(
                onClick = {
                    if (canSubmit) {
                        val member = if (isFromMember) members.find { it.id == selectedMemberId } else null
                        val contributor = if (isFromMember) (member?.name ?: "Member") else externalContributor.trim()
                        val mId = if (isFromMember) selectedMemberId else null

                        onAddIncome(
                            convertedBaseAmount,
                            originalAmount,
                            selectedCurrency,
                            exchangeRate,
                            contributor,
                            mId,
                            selectedCategoryName,
                            notes.trim(),
                            System.currentTimeMillis()
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_deposit_button"),
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm Deposit into Fund", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
