package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.TripEntity
import com.example.data.entity.TripStatus
import com.example.ui.ActiveDialog
import com.example.ui.TripSplitUiState
import com.example.ui.TripSplitViewModel
import com.example.ui.screens.AddExpenseDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CustomCategoryDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DepositFundDialog
import com.example.ui.screens.ExchangeRatesDialog
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.ExportDialog
import com.example.ui.screens.PhotoViewerDialog
import com.example.ui.screens.SettlementScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TripSetupScreen
import com.example.ui.theme.TripSplitTheme
import com.example.updater.ui.AppUpdateDialog
import com.example.updater.ui.UpdateViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TripSplitViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TripSplitTheme {
                TripSplitApp(viewModel = viewModel, updateViewModel = updateViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSplitApp(
    viewModel: TripSplitViewModel,
    updateViewModel: UpdateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var tripMenuExpanded by remember { mutableStateOf(false) }

    // Automatic update check on app launch
    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdates(manual = false)
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(updateUiState.userMessage) {
        updateUiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            updateViewModel.clearMessage()
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // If actively creating new trip via wizard
    if (uiState.activeDialog == ActiveDialog.CREATE_TRIP) {
        TripSetupScreen(
            onCreateTrip = { name, dest, start, end, budget, curr, mgr, members ->
                viewModel.createTrip(
                    trip = TripEntity(
                        name = name,
                        destination = dest,
                        startDate = start,
                        endDate = end,
                        budget = budget,
                        currencySymbol = curr,
                        baseCurrency = if (curr == "৳") "BDT" else if (curr == "$") "USD" else "BDT",
                        status = TripStatus.ACTIVE,
                        managerName = mgr
                    ),
                    members = members
                )
            },
            onLoadSample = {
                viewModel.loadDemoBandarbanTour()
                viewModel.closeDialog()
            },
            canCancel = uiState.currentTrip != null,
            onDismiss = {
                viewModel.closeDialog()
            }
        )
        return
    }

    val currentTrip = uiState.currentTrip

    // Back button handler for tab switching
    BackHandler(enabled = uiState.selectedTab != 0) {
        viewModel.setSelectedTab(0)
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
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "OFFLINE SYNCED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (currentTrip != null) {
                            Text(
                                text = currentTrip.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (currentTrip != null) {
                        IconButton(
                            onClick = { viewModel.openDialog(ActiveDialog.EXPORT) },
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
                                    text = "Switch Active Trip",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                uiState.allTrips.forEach { trip ->
                                    DropdownMenuItem(
                                        text = { Text(trip.name, fontWeight = if (trip.id == currentTrip.id) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            viewModel.selectTrip(trip.id)
                                            tripMenuExpanded = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("+ Create New Trip", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        tripMenuExpanded = false
                                        viewModel.openDialog(ActiveDialog.CREATE_TRIP)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (currentTrip != null) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.setSelectedTab(0) },
                        icon = {
                            Icon(
                                if (uiState.selectedTab == 0) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Dashboard"
                            )
                        },
                        label = { Text("Dashboard", fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.setSelectedTab(1) },
                        icon = {
                            Icon(
                                if (uiState.selectedTab == 1) Icons.Default.ReceiptLong else Icons.Outlined.ReceiptLong,
                                contentDescription = "Expenses"
                            )
                        },
                        label = { Text("Expenses", fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_expenses")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == 2,
                        onClick = { viewModel.setSelectedTab(2) },
                        icon = {
                            Icon(
                                if (uiState.selectedTab == 2) Icons.Default.Handshake else Icons.Outlined.Handshake,
                                contentDescription = "Settle Up"
                            )
                        },
                        label = { Text("Settle Up", fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_settle")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == 3,
                        onClick = { viewModel.setSelectedTab(3) },
                        icon = {
                            Icon(
                                if (uiState.selectedTab == 3) Icons.Default.BarChart else Icons.Outlined.BarChart,
                                contentDescription = "Analytics"
                            )
                        },
                        label = { Text("Analytics", fontWeight = if (uiState.selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_analytics")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == 4,
                        onClick = { viewModel.setSelectedTab(4) },
                        icon = {
                            Icon(
                                if (uiState.selectedTab == 4) Icons.Default.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings", fontWeight = if (uiState.selectedTab == 4) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTrip != null && (uiState.selectedTab == 0 || uiState.selectedTab == 1)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.openDialog(ActiveDialog.ADD_INCOME) },
                        modifier = Modifier.testTag("fab_add_income"),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = "Deposit Fund")
                    }

                    FloatingActionButton(
                        onClick = { viewModel.openDialog(ActiveDialog.ADD_EXPENSE) },
                        modifier = Modifier.testTag("fab_add_expense"),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Expense")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentTrip == null) {
                DashboardScreen(
                    trip = null,
                    summary = null,
                    recentExpenses = emptyList(),
                    onAddExpenseClick = { viewModel.openDialog(ActiveDialog.CREATE_TRIP) },
                    onAddDepositClick = { viewModel.openDialog(ActiveDialog.CREATE_TRIP) },
                    onExportClick = {},
                    onViewAllExpensesClick = {},
                    onNewTripClick = { viewModel.openDialog(ActiveDialog.CREATE_TRIP) },
                    onOpenExchangeRates = {},
                    onOpenSettleDebts = {},
                    onLoadDemoTrip = { viewModel.loadDemoBandarbanTour() }
                )
            } else {
                when (uiState.selectedTab) {
                    0 -> DashboardScreen(
                        trip = currentTrip,
                        summary = uiState.tripSummary,
                        recentExpenses = uiState.expenses,
                        onAddExpenseClick = { viewModel.openDialog(ActiveDialog.ADD_EXPENSE) },
                        onAddDepositClick = { viewModel.openDialog(ActiveDialog.ADD_INCOME) },
                        onExportClick = { viewModel.openDialog(ActiveDialog.EXPORT) },
                        onViewAllExpensesClick = { viewModel.setSelectedTab(1) },
                        onNewTripClick = { viewModel.openDialog(ActiveDialog.CREATE_TRIP) },
                        onOpenExchangeRates = { viewModel.openDialog(ActiveDialog.MANAGE_EXCHANGE_RATES) },
                        onOpenSettleDebts = { viewModel.setSelectedTab(2) },
                        onLoadDemoTrip = { viewModel.loadDemoBandarbanTour() }
                    )
                    1 -> ExpensesScreen(
                        expenses = uiState.expenses,
                        deposits = uiState.deposits,
                        members = uiState.members,
                        attachments = uiState.attachments,
                        payers = uiState.payers,
                        currencySymbol = currentTrip.currencySymbol,
                        onDeleteExpense = { viewModel.deleteExpense(it) },
                        onViewPhoto = { viewModel.viewPhoto(it) }
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
                    4 -> SettingsScreen(
                        currentTrip = currentTrip,
                        allTrips = uiState.allTrips,
                        onSelectTrip = { viewModel.selectTrip(it) },
                        onUpdateTripStatus = { trip, status -> viewModel.updateTripStatus(trip, status) },
                        onCreateNewTrip = { viewModel.openDialog(ActiveDialog.CREATE_TRIP) },
                        onDeleteTrip = { viewModel.deleteCurrentTrip() },
                        onOpenExchangeRates = { viewModel.openDialog(ActiveDialog.MANAGE_EXCHANGE_RATES) },
                        onOpenCustomCategories = { viewModel.openDialog(ActiveDialog.CREATE_CATEGORY) },
                        onExportJson = { viewModel.exportJsonBackup() },
                        onImportJson = { viewModel.importJsonBackup(it) },
                        onLoadDemo = { viewModel.loadDemoBandarbanTour() },
                        isCheckingUpdates = updateUiState.isChecking,
                        updateStatus = updateUiState.lastCheckStatus,
                        onCheckForUpdates = { updateViewModel.checkForUpdates(manual = true) }
                    )
                }
            }
        }
    }

    // Dialogs & Bottom Sheets
    when (uiState.activeDialog) {
        ActiveDialog.ADD_EXPENSE -> {
            if (currentTrip != null) {
                AddExpenseDialog(
                    members = uiState.members,
                    categories = uiState.categories,
                    exchangeRates = uiState.exchangeRates,
                    baseCurrency = currentTrip.baseCurrency,
                    currencySymbol = currentTrip.currencySymbol,
                    onDismiss = { viewModel.closeDialog() },
                    onOpenCreateCategory = { viewModel.openDialog(ActiveDialog.CREATE_CATEGORY) },
                    onAddExpense = { title, amt, origAmt, currCode, rate, cat, custName, custIcon, custColor, pType, pMemId, splitM, splits, payers, uris, notes ->
                        viewModel.addExpenseWithDetails(
                            title, amt, origAmt, currCode, rate, cat, custName, custIcon, custColor, pType, pMemId, splitM, splits, payers, uris, notes
                        )
                    }
                )
            }
        }

        ActiveDialog.ADD_INCOME -> {
            if (currentTrip != null) {
                DepositFundDialog(
                    members = uiState.members,
                    categories = uiState.categories,
                    exchangeRates = uiState.exchangeRates,
                    baseCurrency = currentTrip.baseCurrency,
                    currencySymbol = currentTrip.currencySymbol,
                    onDismiss = { viewModel.closeDialog() },
                    onAddIncome = { amt, origAmt, currCode, rate, contributor, mId, catName, notes, date ->
                        viewModel.addIncome(amt, origAmt, currCode, rate, contributor, mId, catName, notes, date)
                    }
                )
            }
        }

        ActiveDialog.CREATE_CATEGORY -> {
            CustomCategoryDialog(
                onDismiss = { viewModel.closeDialog() },
                onSaveCategory = { name, iconName, colorHex, isIncome ->
                    viewModel.addCustomCategory(name, iconName, colorHex, isIncome)
                }
            )
        }

        ActiveDialog.MANAGE_EXCHANGE_RATES -> {
            if (currentTrip != null) {
                ExchangeRatesDialog(
                    baseCurrency = currentTrip.baseCurrency,
                    rates = uiState.exchangeRates,
                    onDismiss = { viewModel.closeDialog() },
                    onSaveRate = { fromCurr, toCurr, rate ->
                        viewModel.saveExchangeRate(fromCurr, toCurr, rate)
                    }
                )
            }
        }

        ActiveDialog.EXPORT -> {
            ExportDialog(
                summaryText = viewModel.getWhatsAppShareText(),
                onGeneratePdf = { viewModel.exportPdfReport() },
                onDismiss = { viewModel.closeDialog() }
            )
        }

        ActiveDialog.VIEW_PHOTO -> {
            uiState.viewingPhotoPath?.let { path ->
                PhotoViewerDialog(
                    filePath = path,
                    onDismiss = { viewModel.closePhotoViewer() }
                )
            }
        }

        else -> {}
    }

    // Over-the-Air Update Dialog
    if (updateUiState.showDialog && updateUiState.updateInfo != null) {
        AppUpdateDialog(
            updateInfo = updateUiState.updateInfo!!,
            isDownloading = updateUiState.isDownloading,
            downloadProgress = updateUiState.downloadProgress,
            downloadedBytes = updateUiState.downloadedBytes,
            totalBytes = updateUiState.totalBytes,
            needsInstallPermission = updateUiState.needsInstallPermission,
            onStartDownload = { updateViewModel.startUpdateDownload(context) },
            onRequestPermission = { updateViewModel.openPermissionSettings(context) },
            onDismiss = { updateViewModel.dismissDialog() }
        )
    }
}
