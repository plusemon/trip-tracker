package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CurrencyFormatter
import com.example.domain.model.NewMemberInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ADVENTURE_PALETTE = listOf(
    "#006C4C", // Forest Emerald
    "#00658E", // River Indigo
    "#835400", // Warm Ochre
    "#7B1FA2", // Deep Purple
    "#C2185B", // Berry Pink
    "#00796B", // Mountain Teal
    "#E65100", // Sunset Amber
    "#303F9F", // Deep Navy
    "#455A64"  // Slate Stone
)

private val POPULAR_DESTINATIONS = listOf(
    "Bandarban" to "Bandarban Expedition",
    "Sajek Valley" to "Sajek Valley Cloud Tour",
    "Cox's Bazar" to "Cox's Bazar Sea Tour",
    "Sreemangal" to "Sreemangal Tea Tour",
    "Tanguar Haor" to "Tanguar Haor Boat Tour",
    "Saint Martin" to "Saint Martin Island Tour",
    "Kuakata" to "Kuakata Sunset Tour"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSetupScreen(
    onCreateTrip: (
        name: String,
        destination: String,
        startDate: Long,
        endDate: Long,
        budget: Double,
        currencySymbol: String,
        managerName: String,
        members: List<NewMemberInfo>
    ) -> Unit,
    onLoadSample: () -> Unit,
    modifier: Modifier = Modifier,
    canCancel: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    var currentStep by remember { mutableIntStateOf(0) }

    // Step 1: Trip Details State
    var tripName by remember { mutableStateOf("Bandarban Expedition") }
    var destination by remember { mutableStateOf("Bandarban, Bangladesh") }
    var budgetText by remember { mutableStateOf("60000") }
    var currencySymbol by remember { mutableStateOf("৳") }
    val now = remember { System.currentTimeMillis() }
    var startDate by remember { mutableLongStateOf(now) }
    var durationDays by remember { mutableIntStateOf(4) }
    val endDate = remember(startDate, durationDays) {
        startDate + (durationDays * 24 * 60 * 60 * 1000L)
    }

    // Step 2: Members State
    var managerName by remember { mutableStateOf("Emon (Lead)") }
    val memberList = remember {
        mutableStateListOf(
            NewMemberInfo(name = "Emon", phoneNumber = "01711000001", colorHex = "#006C4C", isManager = true),
            NewMemberInfo(name = "Shakib", phoneNumber = "01711000002", colorHex = "#00658E", isManager = false),
            NewMemberInfo(name = "Tanvir", phoneNumber = "01711000003", colorHex = "#835400", isManager = false),
            NewMemberInfo(name = "Nafis", phoneNumber = "01711000004", colorHex = "#7B1FA2", isManager = false),
            NewMemberInfo(name = "Arif", phoneNumber = "01711000005", colorHex = "#C2185B", isManager = false),
            NewMemberInfo(name = "Rifat", phoneNumber = "01711000006", colorHex = "#00796B", isManager = false)
        )
    }

    // Step 3: Advance Fund ("চাঁদা") State
    var collectAdvanceFund by remember { mutableStateOf(true) }
    var advanceDepositAmountText by remember { mutableStateOf("5000") }

    // Handle back button for step navigation
    BackHandler(enabled = true) {
        if (currentStep > 0) {
            currentStep -= 1
        } else if (canCancel && onDismiss != null) {
            onDismiss()
        }
    }

    val totalBudget = budgetText.toDoubleOrNull() ?: 0.0
    val advanceDeposit = if (collectAdvanceFund) advanceDepositAmountText.toDoubleOrNull() ?: 0.0 else 0.0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Trip Setup Wizard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (currentStep) {
                                0 -> "Step 1 of 3: Tour Information"
                                1 -> "Step 2 of 3: Group Travelers"
                                else -> "Step 3 of 3: Shared Fund & Review"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (currentStep > 0) {
                        IconButton(
                            onClick = { currentStep -= 1 },
                            modifier = Modifier.testTag("wizard_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Step")
                        }
                    } else if (canCancel && onDismiss != null) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("wizard_cancel_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Setup")
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = onLoadSample,
                        modifier = Modifier.testTag("load_sample_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sample Tour",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep -= 1 },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back")
                        }
                    } else if (canCancel && onDismiss != null) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Cancel")
                        }
                    } else {
                        // Spacer to keep layout balanced
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (currentStep < 2) {
                        Button(
                            onClick = {
                                if (currentStep == 0) {
                                    if (tripName.isNotBlank()) currentStep = 1
                                } else if (currentStep == 1) {
                                    if (memberList.size >= 2) currentStep = 2
                                }
                            },
                            enabled = if (currentStep == 0) tripName.isNotBlank() else memberList.size >= 2,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("wizard_next_button")
                        ) {
                            Text(if (currentStep == 0) "Next: Members" else "Next: Fund & Review")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                val activeManager = memberList.firstOrNull { it.isManager }?.name
                                    ?: memberList.firstOrNull()?.name
                                    ?: managerName.ifBlank { "Tour Lead" }

                                val finalMembers = memberList.map { m ->
                                    if (collectAdvanceFund && advanceDeposit > 0) {
                                        m.copy(initialDeposit = advanceDeposit)
                                    } else {
                                        m
                                    }
                                }

                                onCreateTrip(
                                    tripName.trim(),
                                    destination.trim(),
                                    startDate,
                                    endDate,
                                    totalBudget,
                                    currencySymbol,
                                    activeManager,
                                    finalMembers
                                )
                            },
                            enabled = tripName.isNotBlank() && memberList.size >= 2,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("create_trip_button")
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Trip & Open Wallet", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
            ) {
            // Wizard Stepper Indicator
            WizardStepperHeader(
                currentStep = currentStep,
                onStepClicked = { step ->
                    if (step < currentStep) {
                        currentStep = step
                    } else if (step == 1 && tripName.isNotBlank()) {
                        currentStep = 1
                    } else if (step == 2 && tripName.isNotBlank() && memberList.size >= 2) {
                        currentStep = 2
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("wizard_step_indicator")
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Step Content with animation
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "WizardStepTransition",
                modifier = Modifier.fillMaxSize()
            ) { step ->
                when (step) {
                    0 -> TripDetailsStep(
                        tripName = tripName,
                        onTripNameChange = { tripName = it },
                        destination = destination,
                        onDestinationChange = { destination = it },
                        budgetText = budgetText,
                        onBudgetTextChange = { budgetText = it },
                        currencySymbol = currencySymbol,
                        onCurrencyChange = { currencySymbol = it },
                        durationDays = durationDays,
                        onDurationDaysChange = { durationDays = it },
                        startDate = startDate,
                        endDate = endDate,
                        onLoadSample = onLoadSample,
                        managerName = managerName,
                        onManagerNameChange = { managerName = it }
                    )
                    1 -> TripMembersStep(
                        members = memberList,
                        onAddMember = { newMember ->
                            memberList.add(newMember)
                        },
                        onRemoveMember = { index ->
                            if (memberList.size > 2) {
                                memberList.removeAt(index)
                            }
                        },
                        onSetManager = { index ->
                            val updated = memberList.mapIndexed { idx, item ->
                                item.copy(isManager = idx == index)
                            }
                            memberList.clear()
                            memberList.addAll(updated)
                            managerName = memberList[index].name
                        },
                        totalBudget = totalBudget,
                        currencySymbol = currencySymbol
                    )
                    2 -> TripReviewStep(
                        tripName = tripName,
                        destination = destination,
                        durationDays = durationDays,
                        startDate = startDate,
                        endDate = endDate,
                        totalBudget = totalBudget,
                        currencySymbol = currencySymbol,
                        members = memberList,
                        collectAdvanceFund = collectAdvanceFund,
                        onCollectAdvanceFundChange = { collectAdvanceFund = it },
                        advanceDepositAmountText = advanceDepositAmountText,
                        onAdvanceDepositAmountChange = { advanceDepositAmountText = it }
                    )
                }
            }
        }
    }
}
}

@Composable
private fun WizardStepperHeader(
    currentStep: Int,
    onStepClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = listOf("Details", "Members", "Shared Fund")
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, title ->
            val isActive = index == currentStep
            val isCompleted = index < currentStep

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onStepClicked(index) }
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> MaterialTheme.colorScheme.primary
                                isActive -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (index < steps.size - 1) {
                Divider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    color = if (index < currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TripDetailsStep(
    tripName: String,
    onTripNameChange: (String) -> Unit,
    destination: String,
    onDestinationChange: (String) -> Unit,
    budgetText: String,
    onBudgetTextChange: (String) -> Unit,
    currencySymbol: String,
    onCurrencyChange: (String) -> Unit,
    durationDays: Int,
    onDurationDaysChange: (Int) -> Unit,
    startDate: Long,
    endDate: Long,
    onLoadSample: () -> Unit,
    managerName: String,
    onManagerNameChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val dateFormat = remember { SimpleDateFormat("dd MMM, yyyy", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sample Tour Card Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sample_trip_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = "Sample",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quick Start: Bandarban Tour",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Explore with 8 members, Chander Gari & sample deposits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Button(
                    onClick = onLoadSample,
                    modifier = Modifier.testTag("load_sample_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Explore", fontSize = 12.sp)
                }
            }
        }

        // Popular Destinations Chips
        Column {
            Text(
                text = "Popular Tour Destinations",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                POPULAR_DESTINATIONS.forEach { (place, suggestedTitle) ->
                    val isSelected = destination.contains(place, ignoreCase = true)
                    InputChip(
                        selected = isSelected,
                        onClick = {
                            onDestinationChange("$place, Bangladesh")
                            if (tripName.isBlank() || tripName == "Bandarban Expedition" || POPULAR_DESTINATIONS.any { it.second == tripName }) {
                                onTripNameChange(suggestedTitle)
                            }
                        },
                        label = { Text(place, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }

        // Trip Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Adventure Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = tripName,
                    onValueChange = onTripNameChange,
                    label = { Text("Trip / Tour Name") },
                    leadingIcon = { Icon(Icons.Default.Explore, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = destination,
                    onValueChange = onDestinationChange,
                    label = { Text("Destination (e.g. Bandarban, Sajek)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("destination_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = managerName,
                    onValueChange = onManagerNameChange,
                    label = { Text("Tour Manager / Fund Holder") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manager_name_input"),
                    singleLine = true,
                    supportingText = { Text("Carries the Common Fund cash in pocket") }
                )
            }
        }

        // Duration & Budget Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Duration & Target Budget",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Duration Selector
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tour Duration: $durationDays Days (${durationDays - 1} Nights)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${dateFormat.format(Date(startDate))} – ${dateFormat.format(Date(endDate))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2 to "Weekend (2D)", 3 to "3D / 2N", 4 to "4D / 3N", 5 to "5 Days", 7 to "1 Week").forEach { (days, label) ->
                            FilterChip(
                                selected = durationDays == days,
                                onClick = { onDurationDaysChange(days) },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Budget Input
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = onBudgetTextChange,
                    label = { Text("Target Tour Budget ($currencySymbol)") },
                    leadingIcon = { Icon(Icons.Default.Paid, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_input"),
                    singleLine = true
                )

                // Quick Budget Suggestions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("30000", "50000", "80000", "120000").forEach { amount ->
                        OutlinedButton(
                            onClick = { onBudgetTextChange(amount) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("৳${amount.toInt() / 1000}k", fontSize = 11.sp)
                        }
                    }
                }

                // Currency selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Currency:", style = MaterialTheme.typography.labelMedium)
                    listOf("৳" to "BDT (৳)", "$" to "USD ($)", "€" to "EUR (€)").forEach { (sym, lbl) ->
                        FilterChip(
                            selected = currencySymbol == sym,
                            onClick = { onCurrencyChange(sym) },
                            label = { Text(lbl, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TripMembersStep(
    members: MutableList<NewMemberInfo>,
    onAddMember: (NewMemberInfo) -> Unit,
    onRemoveMember: (Int) -> Unit,
    onSetManager: (Int) -> Unit,
    totalBudget: Double,
    currencySymbol: String
) {
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(ADVENTURE_PALETTE[0]) }
    var isManagerRole by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary & Budget per head banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${members.size} Group Members",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "4–10 recommended for Chander Gari / Jeep",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (members.isNotEmpty() && totalBudget > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Est. Share",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = CurrencyFormatter.formatTaka(totalBudget / members.size, currencySymbol),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Quick Preset Groups
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    members.clear()
                    members.addAll(
                        listOf(
                            NewMemberInfo("Emon", "01711000001", "#006C4C", isManager = true),
                            NewMemberInfo("Shakib", "01711000002", "#00658E", isManager = false),
                            NewMemberInfo("Tanvir", "01711000003", "#835400", isManager = false),
                            NewMemberInfo("Nafis", "01711000004", "#7B1FA2", isManager = false),
                            NewMemberInfo("Arif", "01711000005", "#C2185B", isManager = false),
                            NewMemberInfo("Rifat", "01711000006", "#00796B", isManager = false)
                        )
                    )
                },
                modifier = Modifier.weight(1f).testTag("preset_6_members_button")
            ) {
                Text("6 Friends Roster", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    members.clear()
                    members.addAll(
                        listOf(
                            NewMemberInfo("Emon", "01711000001", "#006C4C", isManager = true),
                            NewMemberInfo("Shakib", "01711000002", "#00658E", isManager = false),
                            NewMemberInfo("Tanvir", "01711000003", "#835400", isManager = false),
                            NewMemberInfo("Nafis", "01711000004", "#7B1FA2", isManager = false),
                            NewMemberInfo("Arif", "01711000005", "#C2185B", isManager = false),
                            NewMemberInfo("Rifat", "01711000006", "#00796B", isManager = false),
                            NewMemberInfo("Fahim", "01711000007", "#E65100", isManager = false),
                            NewMemberInfo("Joy", "01711000008", "#303F9F", isManager = false)
                        )
                    )
                },
                modifier = Modifier.weight(1f).testTag("preset_8_members_button")
            ) {
                Text("8 Expedition Crew", fontSize = 11.sp)
            }
        }

        // Add Member Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Add Traveler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Traveler Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_member_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = newPhone,
                    onValueChange = { newPhone = it },
                    label = { Text("Phone Number (Optional - for WhatsApp)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_member_phone_input"),
                    singleLine = true
                )

                // Color Swatches
                Column {
                    Text(text = "Avatar Color Badge", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ADVENTURE_PALETTE.forEach { colorHex ->
                            val color = Color(android.graphics.Color.parseColor(colorHex))
                            val isSelected = selectedColor == colorHex
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = colorHex }
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Make manager toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Designate as Tour Manager", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Carries group funds & logs expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = isManagerRole,
                        onCheckedChange = { isManagerRole = it }
                    )
                }

                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            val newMember = NewMemberInfo(
                                name = newName.trim(),
                                phoneNumber = newPhone.trim(),
                                colorHex = selectedColor,
                                isManager = isManagerRole
                            )
                            if (isManagerRole) {
                                members.forEachIndexed { i, m ->
                                    members[i] = m.copy(isManager = false)
                                }
                            }
                            onAddMember(newMember)
                            newName = ""
                            newPhone = ""
                            isManagerRole = false
                            selectedColor = ADVENTURE_PALETTE[(members.size) % ADVENTURE_PALETTE.size]
                        }
                    },
                    enabled = newName.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_member_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Member to Roster")
                }
            }
        }

        // Members Roster List
        Text(
            text = "Enrolled Travelers (${members.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        members.forEachIndexed { index, member ->
            val memberColor = runCatching { Color(android.graphics.Color.parseColor(member.colorHex)) }
                .getOrDefault(MaterialTheme.colorScheme.primary)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (member.isManager) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(memberColor),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = if (member.name.isNotBlank()) {
                            val parts = member.name.trim().split("\\s+".toRegex())
                            if (parts.size >= 2) "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
                            else member.name.take(2).uppercase()
                        } else "M"
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (member.isManager) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "TOUR LEAD",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (member.phoneNumber.isNotBlank()) {
                            Text(
                                text = member.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!member.isManager) {
                        TextButton(
                            onClick = { onSetManager(index) }
                        ) {
                            Text("Make Lead", fontSize = 11.sp)
                        }
                    }

                    if (members.size > 2) {
                        IconButton(
                            onClick = { onRemoveMember(index) },
                            modifier = Modifier.size(32.dp).testTag("remove_member_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove ${member.name}",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripReviewStep(
    tripName: String,
    destination: String,
    durationDays: Int,
    startDate: Long,
    endDate: Long,
    totalBudget: Double,
    currencySymbol: String,
    members: List<NewMemberInfo>,
    collectAdvanceFund: Boolean,
    onCollectAdvanceFundChange: (Boolean) -> Unit,
    advanceDepositAmountText: String,
    onAdvanceDepositAmountChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val dateFormat = remember { SimpleDateFormat("dd MMM, yyyy", Locale.US) }
    val depositPerMember = advanceDepositAmountText.toDoubleOrNull() ?: 0.0
    val totalInitialFund = if (collectAdvanceFund) depositPerMember * members.size else 0.0
    val manager = members.firstOrNull { it.isManager } ?: members.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shared Fund ("চাঁদা") Setup Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Advance Common Fund (\"চাঁদা\" Pool)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Text(
                    text = "In Bangladeshi tours, advance funds are pooled into the tour manager's pocket to pay for shared items like Chander Gari jeep, fuel, boat rentals, and hotel rooms.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Record Initial Advance Deposits",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Starts the Common Fund with pooled cash",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = collectAdvanceFund,
                        onCheckedChange = onCollectAdvanceFundChange,
                        modifier = Modifier.testTag("advance_fund_switch")
                    )
                }

                if (collectAdvanceFund) {
                    OutlinedTextField(
                        value = advanceDepositAmountText,
                        onValueChange = onAdvanceDepositAmountChange,
                        label = { Text("Advance Contribution per Member ($currencySymbol)") },
                        leadingIcon = { Icon(Icons.Default.Paid, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("advance_amount_input"),
                        singleLine = true
                    )

                    // Quick increments
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("3000", "5000", "7500", "10000").forEach { preset ->
                            OutlinedButton(
                                onClick = { onAdvanceDepositAmountChange(preset) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("৳${preset.toInt() / 1000}k", fontSize = 11.sp)
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Initial Common Fund Cash",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "${members.size} members × ${CurrencyFormatter.formatTaka(depositPerMember, currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = CurrencyFormatter.formatTaka(totalInitialFund, currencySymbol),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // Final Tour Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Expedition Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tripName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = destination,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("$durationDays Days", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Dates", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("${dateFormat.format(Date(startDate))}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                    Column {
                        Text("Tour Lead", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(manager?.name ?: "Leader", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Target Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(CurrencyFormatter.formatTaka(totalBudget, currencySymbol), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Pooled Cash (\"চাঁদা\")", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            CurrencyFormatter.formatTaka(totalInitialFund, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Column {
                        Text("Travelers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("${members.size} Members", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }

                // Member Avatars Stack Preview
                Column {
                    Text("Traveler Roster Preview", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        members.forEach { m ->
                            val color = runCatching { Color(android.graphics.Color.parseColor(m.colorHex)) }.getOrDefault(MaterialTheme.colorScheme.primary)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = color.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = m.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (m.isManager) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (m.isManager) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Lead",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
