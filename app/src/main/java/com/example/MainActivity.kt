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
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.HorizontalDivider
import com.example.ui.screens.AddExpenseDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CustomCategoryDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DepositFundDialog
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.ExportDialog
import com.example.ui.screens.JoinTripDialog
import com.example.ui.screens.PhotoViewerDialog
import com.example.ui.screens.ShareTripCodeDialog
import com.example.ui.screens.SettlementScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TripSetupScreen
import com.example.ui.theme.TripSplitTheme
import com.example.updater.ui.AppUpdateDialog
import com.example.updater.ui.UpdateViewModel

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.res.stringResource
import com.example.util.LanguageManager
import com.example.util.ThemeManager
import com.example.util.ThemeMode

class MainActivity : AppCompatActivity() {

    private val viewModel: TripSplitViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanguageManager.applySavedLocale(this)
        ThemeManager.init(this)
        enableEdgeToEdge()
        setContent {
            val themeMode by ThemeManager.themeMode.collectAsStateWithLifecycle()
            TripSplitTheme(themeMode = themeMode) {
                TripSplitApp(
                    viewModel = viewModel,
                    updateViewModel = updateViewModel,
                    currentThemeMode = themeMode,
                    onThemeModeChange = { newMode ->
                        ThemeManager.setThemeMode(this, newMode)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSplitApp(
    viewModel: TripSplitViewModel,
    updateViewModel: UpdateViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
                        currencySymbol = "৳",
                        baseCurrency = "BDT",
                        status = TripStatus.ACTIVE,
                        managerName = mgr
                    ),
                    members = members
                )
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
                                text = stringResource(R.string.app_name),
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
                                    text = stringResource(id = R.string.offline_synced),
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
                        if (currentTrip.isReadOnly) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.padding(end = 4.dp).testTag("top_live_sync_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "👀 Live Sync",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        } else if (currentTrip.tripCode.isNotBlank()) {
                            IconButton(
                                onClick = { viewModel.openShareTripCodeDialog() },
                                modifier = Modifier.testTag("top_share_code_button")
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = "Share Trip Code", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        IconButton(
                            onClick = { viewModel.openDialog(ActiveDialog.EXPORT) },
                            modifier = Modifier.testTag("top_share_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(id = R.string.share_report))
                        }

                        // Trip switcher menu
                        Box {
                            IconButton(
                                onClick = { tripMenuExpanded = true },
                                modifier = Modifier.testTag("trip_switcher_button")
                            ) {
                                Icon(Icons.Default.CardTravel, contentDescription = stringResource(id = R.string.switch_trips))
                            }

                            DropdownMenu(
                                expanded = tripMenuExpanded,
                                onDismissRequest = { tripMenuExpanded = false }
                            ) {
                                Text(
                                    text = stringResource(id = R.string.switch_active_trip),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                uiState.allTrips.forEach { trip ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    trip.name,
                                                    fontWeight = if (trip.id == currentTrip.id) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (trip.isReadOnly) {
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        " (View-Only)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.tertiary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectTrip(trip.id)
                                            tripMenuExpanded = false
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Join Trip with Code", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = {
                                        tripMenuExpanded = false
                                        viewModel.openJoinTripDialog()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(id = R.string.create_new_trip), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = {
                                        tripMenuExpanded = false
                                        viewModel.openDialog(ActiveDialog.CREATE_TRIP)
                                    }
                                )
                            }
                        }
                    } else {
                        Row {
                            IconButton(
                                onClick = { viewModel.openJoinTripDialog() },
                                modifier = Modifier.testTag("top_join_button")
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = "Join Trip", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { viewModel.setSelectedTab(4) },
                                modifier = Modifier.testTag("top_settings_button")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
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
                                contentDescription = stringResource(id = R.string.nav_dashboard)
                            )
                        },
                        label = { Text(stringResource(id = R.string.nav_dashboard), fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.setSelectedTab(1) },
                        icon = {
                            Icon(
                                if (uiState.selectedTab == 1) Icons.Default.ReceiptLong else Icons.Outlined.ReceiptLong,
                                contentDescription = stringResource(id = R.string.nav_expenses)
                            )
                        },
                        label = { Text(stringResource(id = R.string.nav_expenses), fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_expenses")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == 2,
                        onClick = { viewModel.setSelectedTab(2) },
                        icon = {
                            Icon(
                                if (uiState.selectedTab == 2) Icons.Default.SwapHoriz else Icons.Outlined.SwapHoriz,
                                contentDescription = stringResource(id = R.string.nav_settle_up)
                            )
                        },
                        label = { Text(stringResource(id = R.string.nav_settle_up), fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_settle")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == 3,
                        onClick = { viewModel.setSelectedTab(3) },
                        icon = {
                            Icon(
                                if (uiState.selectedTab == 3) Icons.Default.PieChart else Icons.Outlined.PieChart,
                                contentDescription = stringResource(id = R.string.nav_analytics)
                            )
                        },
                        label = { Text(stringResource(id = R.string.nav_analytics), fontWeight = if (uiState.selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_analytics")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == 4,
                        onClick = { viewModel.setSelectedTab(4) },
                        icon = {
                            Icon(
                                if (uiState.selectedTab == 4) Icons.Default.Settings else Icons.Outlined.Settings,
                                contentDescription = stringResource(id = R.string.nav_settings)
                            )
                        },
                        label = { Text(stringResource(id = R.string.nav_settings), fontWeight = if (uiState.selectedTab == 4) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            } else {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar_empty"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.setSelectedTab(0) },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard", fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_empty_dashboard")
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == 4,
                        onClick = { viewModel.setSelectedTab(4) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontWeight = if (uiState.selectedTab == 4) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_empty_settings")
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTrip != null && !currentTrip.isReadOnly && (uiState.selectedTab == 0 || uiState.selectedTab == 1)) {
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentTrip == null) {
                if (uiState.selectedTab == 4) {
                    SettingsScreen(
                        currentTrip = null,
                        allTrips = uiState.allTrips,
                        onSelectTrip = { viewModel.selectTrip(it) },
                        onUpdateTripStatus = { trip, status -> viewModel.updateTripStatus(trip, status) },
                        onCreateNewTrip = { viewModel.openDialog(ActiveDialog.CREATE_TRIP) },
                        onDeleteTrip = { viewModel.deleteCurrentTrip() },
                        onOpenCustomCategories = { viewModel.openDialog(ActiveDialog.CREATE_CATEGORY) },
                        onExportJson = { viewModel.exportJsonBackup() },
                        onImportJson = { viewModel.importJsonBackup(it) },
                        onAdminRestore = { pwd, cb -> viewModel.restoreAllTripsWithAdminPassword(pwd, cb) },
                        currentThemeMode = currentThemeMode,
                        onThemeModeChange = onThemeModeChange,
                        isCheckingUpdates = updateUiState.isChecking,
                        updateStatus = updateUiState.lastCheckStatus,
                        onCheckForUpdates = { updateViewModel.checkForUpdates(manual = true) }
                    )
                } else {
                    DashboardScreen(
                        trip = null,
                        summary = null,
                        recentExpenses = emptyList(),
                        onAddExpenseClick = { viewModel.openDialog(ActiveDialog.CREATE_TRIP) },
                        onAddDepositClick = { viewModel.openDialog(ActiveDialog.CREATE_TRIP) },
                        onExportClick = {},
                        onViewAllExpensesClick = {},
                        onNewTripClick = { viewModel.openDialog(ActiveDialog.CREATE_TRIP) },
                        onOpenSettleDebts = {},
                        onJoinTripClick = { viewModel.openJoinTripDialog() },
                        onOpenSettingsClick = { viewModel.setSelectedTab(4) }
                    )
                }
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
                        onOpenSettleDebts = { viewModel.setSelectedTab(2) },
                        onShareCodeClick = { viewModel.openShareTripCodeDialog() },
                        onJoinTripClick = { viewModel.openJoinTripDialog() }
                    )
                    1 -> ExpensesScreen(
                        expenses = uiState.expenses,
                        deposits = uiState.deposits,
                        members = uiState.members,
                        attachments = uiState.attachments,
                        payers = uiState.payers,
                        currencySymbol = currentTrip.currencySymbol,
                        onDeleteExpense = { viewModel.deleteExpense(it) },
                        onViewPhoto = { viewModel.viewPhoto(it) },
                        isReadOnly = currentTrip.isReadOnly
                    )
                    2 -> SettlementScreen(
                        balances = uiState.memberBalances,
                        settlements = uiState.settlementSteps,
                        settledRecords = uiState.settlements,
                        currencySymbol = currentTrip.currencySymbol,
                        onRecordSettlement = { fromId, toId, fromName, toName, amount ->
                            viewModel.recordSettlement(fromId, toId, fromName, toName, amount, "Settlement payment cleared")
                        },
                        isReadOnly = currentTrip.isReadOnly
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
                        onOpenCustomCategories = { viewModel.openDialog(ActiveDialog.CREATE_CATEGORY) },
                        onExportJson = { viewModel.exportJsonBackup() },
                        onImportJson = { viewModel.importJsonBackup(it) },
                        onAdminRestore = { pwd, cb -> viewModel.restoreAllTripsWithAdminPassword(pwd, cb) },
                        currentThemeMode = currentThemeMode,
                        onThemeModeChange = onThemeModeChange,
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
                    onDismiss = { viewModel.closeDialog() },
                    onOpenCreateCategory = { viewModel.openDialog(ActiveDialog.CREATE_CATEGORY) },
                    onAddExpense = { title, amt, cat, custName, custIcon, custColor, pType, pMemId, splitM, splits, payers, uris, notes ->
                        viewModel.addExpenseWithDetails(
                            title, amt, cat, custName, custIcon, custColor, pType, pMemId, splitM, splits, payers, uris, notes
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
                    onDismiss = { viewModel.closeDialog() },
                    onAddIncome = { amt, contributor, mId, catName, notes, date ->
                        viewModel.addIncome(amt, contributor, mId, catName, notes, date)
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

        ActiveDialog.EXPORT -> {
            ExportDialog(
                summaryText = viewModel.getWhatsAppShareText(),
                tripName = currentTrip?.name ?: "Tour",
                onGeneratePdf = { viewModel.exportPdfReport() },
                onDismiss = { viewModel.closeDialog() },
                onShowSnackbar = { message, actionLabel, onAction ->
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = actionLabel,
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            onAction()
                        }
                    }
                }
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

        ActiveDialog.JOIN_TRIP -> {
            JoinTripDialog(
                onDismissRequest = { viewModel.closeDialog() },
                onJoin = { code, callback ->
                    viewModel.joinTripWithCode(code, callback)
                },
                isSyncing = uiState.isSyncing
            )
        }

        ActiveDialog.SHARE_TRIP_CODE -> {
            if (currentTrip != null) {
                ShareTripCodeDialog(
                    trip = currentTrip,
                    onDismissRequest = { viewModel.closeDialog() },
                    onManualSync = { viewModel.triggerCloudSync() },
                    isSyncing = uiState.isSyncing
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
            onSkipVersion = { updateViewModel.skipVersion(updateUiState.updateInfo!!.tagName) },
            onDismiss = { updateViewModel.dismissDialog() }
        )
    }
}
