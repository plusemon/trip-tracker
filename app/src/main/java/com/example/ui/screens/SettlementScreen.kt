package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.entity.SettlementRecordEntity
import com.example.domain.engine.SettlementEngine
import com.example.domain.model.CurrencyFormatter
import com.example.domain.model.MemberBalance
import com.example.domain.model.SettlementTransaction
import com.example.ui.components.MemberAvatar
import com.example.ui.components.SettlementItemCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettlementScreen(
    balances: List<MemberBalance>,
    settlements: List<SettlementTransaction>,
    settledRecords: List<SettlementRecordEntity>,
    currencySymbol: String,
    onRecordSettlement: (fromMemberId: Long, toMemberId: Long, fromMemberName: String, toMemberName: String, amount: Double) -> Unit,
    isReadOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }

    val totalRemainingAmount = settlements.sumOf { it.amount }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview Summary Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Smart Debt Settlement",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Optimized via Minimum Cash Flow Algorithm",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Metric Row
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Transfers to Clear",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "${settlements.size} Pending",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Remaining",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatTaka(totalRemainingAmount, currencySymbol),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Filter Chips Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))

                    val filters = listOf("All (${settlements.size})", "Pending", "Settled History")
                    filters.forEach { filter ->
                        val isSelected = selectedFilter == filter || (selectedFilter == "All" && filter.startsWith("All"))
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedFilter = if (filter.startsWith("All")) "All" else filter
                            },
                            label = { Text(filter, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Section: Settlement Instructions / Transactions
            if (selectedFilter != "Settled History") {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Payment Instructions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (settlements.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF1B6D24),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.all_accounts_settled),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B6D24)
                                )
                                Text(
                                    text = stringResource(R.string.no_pending_debts),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(settlements) { st ->
                        SettlementItemCard(
                            fromName = st.fromMemberName,
                            toName = st.toMemberName,
                            amount = st.amount,
                            currencySymbol = currencySymbol,
                            isSettled = false,
                            onSettleClick = {
                                onRecordSettlement(st.fromMemberId, st.toMemberId, st.fromMemberName, st.toMemberName, st.amount)
                            },
                            isReadOnly = isReadOnly,
                            fromMemberId = st.fromMemberId,
                            toMemberId = st.toMemberId,
                            modifier = Modifier.testTag("settlement_step_${st.fromMemberId}_to_${st.toMemberId}")
                        )
                    }
                }
            }

            // Section: Individual Member Balances
            if (selectedFilter == "All" || selectedFilter == "Pending") {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Individual Member Balances",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(balances) { mb ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MemberAvatar(member = mb.member, size = 42.dp)
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mb.member.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                val depositStr = CurrencyFormatter.formatTaka(mb.totalDeposited, currencySymbol)
                                val pocketStr = if (mb.totalPocketPaid > 0) " + Pocket ${CurrencyFormatter.formatTaka(mb.totalPocketPaid, currencySymbol)}" else ""
                                Text(
                                    text = "Paid: $depositStr$pocketStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Consumed: ${CurrencyFormatter.formatTaka(mb.totalConsumed, currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Net Status Badge
                            val net = mb.netBalance
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    mb.isCreditor -> Color(0xFFE8F5E9)
                                    mb.isDebtor -> Color(0xFFFFEBEE)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    val badgeTitle = when {
                                        mb.isCreditor -> "Gets Back"
                                        mb.isDebtor -> "Owes"
                                        else -> "Settled"
                                    }
                                    val badgeColor = when {
                                        mb.isCreditor -> Color(0xFF1B6D24)
                                        mb.isDebtor -> Color(0xFFBA1A1A)
                                        else -> MaterialTheme.colorScheme.outline
                                    }
                                    Text(
                                        text = badgeTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatTaka(kotlin.math.abs(net), currencySymbol),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = badgeColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: Settled History Log
            if ((selectedFilter == "All" || selectedFilter == "Settled History") && settledRecords.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Settlement History Log (${settledRecords.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(settledRecords) { record ->
                    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val isFromFund = record.fromMemberId == SettlementEngine.COMMON_FUND_ID ||
                                    record.fromMemberName.contains("Common Fund", ignoreCase = true) ||
                                    record.fromMemberName.contains("Group Fund", ignoreCase = true) ||
                                    record.fromMemberName.contains("গ্রুপ ফান্ড")
                                val isToFund = record.toMemberId == SettlementEngine.COMMON_FUND_ID ||
                                    record.toMemberName.contains("Common Fund", ignoreCase = true) ||
                                    record.toMemberName.contains("Group Fund", ignoreCase = true) ||
                                    record.toMemberName.contains("গ্রুপ ফান্ড")
                                val displayFrom = if (isFromFund) stringResource(R.string.group_fund) else record.fromMemberName
                                val displayTo = if (isToFund) stringResource(R.string.group_fund) else record.toMemberName

                                val historyTitle = if (displayFrom.isNotBlank() && displayTo.isNotBlank()) {
                                    "$displayFrom ➡️ $displayTo"
                                } else {
                                    "Settlement Payment Cleared"
                                }
                                Text(
                                    text = historyTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dateFormat.format(Date(record.settledDate)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = CurrencyFormatter.formatTaka(record.amount, currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1B6D24),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
