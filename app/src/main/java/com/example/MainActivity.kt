package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ActiveDialog
import com.example.ui.TripSplitUiState
import com.example.ui.TripSplitViewModel
import com.example.ui.screens.AddExpenseDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DepositFundDialog
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.ExportDialog
import com.example.ui.screens.SettlementScreen
import com.example.ui.screens.TripSetupScreen
import com.example.ui.theme.TripSplitTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TripSplitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TripSplitTheme {
                TripSplitApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSplitApp(viewModel: TripSplitViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var tripMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // If no trip exists or actively creating new trip
    if (uiState.currentTrip == null || uiState.activeDialog == ActiveDialog.CREATE_TRIP) {
        TripSetupScreen(
            onCreateTrip = { name, dest, start, end, budget, curr, mgr, members ->
                viewModel.createTripDetailed(name, dest, start, end, budget, curr, mgr, members)
            },
            onLoadSample = {
                viewModel.loadSampleBandarbanTour()
            },
            canCancel = uiState.currentTrip != null,
            onDismiss = {
                viewModel.dismissDialog()
            }
        )
        return
    }

    val currentTrip = uiState.currentTrip!!

    // Back button handler for tab switching
    BackHandler(enabled = uiState.selectedTab != 0) {
        viewModel.selectTab(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TripSplit BD",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "OFFLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = currentTrip.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.showDialog(ActiveDialog.EXPORT) },
                        modifier = Modifier.testTag("top_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Report")
                    }

                    // Trip switcher menu
                    Box {
                        IconButton(
                            onClick = { tripMenuExpanded = true },
                            modifier = Modifier.testTag("trip_switcher_button")
                        ) {
                            Icon(Icons.Default.Dashboard, contentDescription = "Switch Trips")
                        }

                        DropdownMenu(
                            expanded = tripMenuExpanded,
                            onDismissRequest = { tripMenuExpanded = false }
                        ) {
                            Text(
                                text = "Switch Trip",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            uiState.allTrips.forEach { trip ->
                                DropdownMenuItem(
                                    text = { Text(trip.name) },
                                    onClick = {
                                        viewModel.selectTrip(trip.id)
                                        tripMenuExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Create New Trip", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    tripMenuExpanded = false
                                    viewModel.showDialog(ActiveDialog.CREATE_TRIP)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = {
                        Icon(
                            if (uiState.selectedTab == 0) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Dashboard") },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = {
                        Icon(
                            if (uiState.selectedTab == 1) Icons.Default.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = "Expenses"
                        )
                    },
                    label = { Text("Expenses") },
                    modifier = Modifier.testTag("nav_tab_expenses")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = {
                        Icon(
                            if (uiState.selectedTab == 2) Icons.Default.Handshake else Icons.Outlined.Handshake,
                            contentDescription = "Settle Up"
                        )
                    },
                    label = { Text("Settle Up") },
                    modifier = Modifier.testTag("nav_tab_settle")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = {
                        Icon(
                            if (uiState.selectedTab == 3) Icons.Default.BarChart else Icons.Outlined.BarChart,
                            contentDescription = "Analytics"
                        )
                    },
                    label = { Text("Analytics") },
                    modifier = Modifier.testTag("nav_tab_analytics")
                )
            }
        },
        floatingActionButton = {
            if (uiState.selectedTab == 0 || uiState.selectedTab == 1) {
                FloatingActionButton(
                    onClick = { viewModel.showDialog(ActiveDialog.ADD_EXPENSE) },
                    modifier = Modifier.testTag("fab_add_expense"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                0 -> DashboardScreen(
                    trip = currentTrip,
                    summary = uiState.tripSummary,
                    recentExpenses = uiState.expenses,
                    onAddExpenseClick = { viewModel.showDialog(ActiveDialog.ADD_EXPENSE) },
                    onAddDepositClick = { viewModel.showDialog(ActiveDialog.ADD_DEPOSIT) },
                    onExportClick = { viewModel.showDialog(ActiveDialog.EXPORT) },
                    onViewAllExpensesClick = { viewModel.selectTab(1) },
                    onNewTripClick = { viewModel.showDialog(ActiveDialog.CREATE_TRIP) }
                )
                1 -> ExpensesScreen(
                    expenses = uiState.expenses,
                    members = uiState.members,
                    currencySymbol = currentTrip.currencySymbol,
                    onDeleteExpense = { viewModel.deleteExpense(it) }
                )
                2 -> SettlementScreen(
                    balances = uiState.memberBalances,
                    settlements = uiState.settlementSteps,
                    settledRecords = uiState.settlements,
                    currencySymbol = currentTrip.currencySymbol,
                    onRecordSettlement = { fromId, toId, fromName, toName, amount ->
                        viewModel.recordSettlement(fromId, toId, fromName, toName, amount, "Settlement payment cleared")
                    }
                )
                3 -> AnalyticsScreen(
                    summary = uiState.tripSummary,
                    categorySpendings = uiState.categorySpendings,
                    expenses = uiState.expenses,
                    balances = uiState.memberBalances,
                    currencySymbol = currentTrip.currencySymbol
                )
            }
        }
    }

    // Active Dialogs & Bottom Sheets
    when (uiState.activeDialog) {
        ActiveDialog.ADD_EXPENSE -> {
            AddExpenseDialog(
                members = uiState.members,
                currencySymbol = currentTrip.currencySymbol,
                onDismiss = { viewModel.dismissDialog() },
                onAddExpense = { title, amount, cat, date, payerType, payerMemberId, mode, customShares, selectedMemberIds, notes ->
                    viewModel.addExpense(
                        title, amount, cat, date, payerType, payerMemberId, mode, customShares, selectedMemberIds, notes
                    )
                }
            )
        }

        ActiveDialog.ADD_DEPOSIT -> {
            DepositFundDialog(
                members = uiState.members,
                currencySymbol = currentTrip.currencySymbol,
                onDismiss = { viewModel.dismissDialog() },
                onAddDeposit = { memberId, amount, notes ->
                    viewModel.addDeposit(memberId, amount, notes)
                }
            )
        }

        ActiveDialog.EXPORT -> {
            ExportDialog(
                summaryText = viewModel.generateWhatsAppText(),
                onGeneratePdf = { viewModel.exportPdfReport() },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        else -> {}
    }
}
