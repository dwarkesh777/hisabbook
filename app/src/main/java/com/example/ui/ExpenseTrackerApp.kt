package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Expense
import com.example.ui.theme.*
import com.example.viewmodel.ExpenseViewModel
import java.io.File
import java.io.FileWriter
import android.content.Context
import android.content.Intent
import android.app.DatePickerDialog
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerApp(viewModel: ExpenseViewModel) {
    val expenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val stats by viewModel.financialStats.collectAsStateWithLifecycle()
    val budget by viewModel.budget.collectAsStateWithLifecycle()
    val categoryShares by viewModel.categoryBreakdown.collectAsStateWithLifecycle()
    val paymentMethodStats by viewModel.paymentMethodBreakdown.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val userAvatar by viewModel.userAvatar.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(0) } // 0 = Dashboard, 1 = Analytics, 2 = History, 3 = Settings
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedExpenseForDetail by remember { mutableStateOf<Expense?>(null) }
    var showEditDialog by remember { mutableStateOf<Expense?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }

    val currencyFormatter = remember(currencySymbol) {
        val df = DecimalFormat("#,##0.00")
        df.positivePrefix = currencySymbol
        df.negativePrefix = "-$currencySymbol"
        df
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BentoNavActive),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Profile Wallet",
                                    tint = BentoPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Welcome back,",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BentoTextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = userName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BentoTextPrimary
                                    )
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .testTag("profile_logo_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BentoPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userName.take(1).uppercase().ifEmpty { "U" },
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = BentoNavBg,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoPrimary,
                        selectedTextColor = BentoPrimary,
                        indicatorColor = BentoNavActive,
                        unselectedIconColor = BentoTextSecondary,
                        unselectedTextColor = BentoTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoPrimary,
                        selectedTextColor = BentoPrimary,
                        indicatorColor = BentoNavActive,
                        unselectedIconColor = BentoTextSecondary,
                        unselectedTextColor = BentoTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_analytics")
                )
                // Center Action: Stylized Add Transaction button in footer
                NavigationBarItem(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(BentoPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Transaction",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = { Text("Add", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BentoPrimary)) },
                    selected = false,
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("nav_add_transaction")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "History") },
                    label = { Text("History") },
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoPrimary,
                        selectedTextColor = BentoPrimary,
                        indicatorColor = BentoNavActive,
                        unselectedIconColor = BentoTextSecondary,
                        unselectedTextColor = BentoTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_history")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoPrimary,
                        selectedTextColor = BentoPrimary,
                        indicatorColor = BentoNavActive,
                        unselectedIconColor = BentoTextSecondary,
                        unselectedTextColor = BentoTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> DashboardTab(
                    stats = stats,
                    budget = budget,
                    recentExpenses = allExpenses.take(3),
                    categoryShares = categoryShares,
                    currencyFormatter = currencyFormatter,
                    onNavigateToHistory = { currentTab = 2 }
                )
                1 -> AnalyticsScreen(
                    stats = paymentMethodStats,
                    currencyFormatter = currencyFormatter
                )
                2 -> {
                    val queryState by viewModel.searchQuery.collectAsStateWithLifecycle()
                    val catState by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
                    val typeState by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
                    HistoryTab(
                        expenses = expenses,
                        searchQuery = queryState,
                        onSearchQueryChange = { viewModel.searchQuery.value = it },
                        selectedCategory = catState,
                        onCategoryChange = { viewModel.selectedCategoryFilter.value = it },
                        selectedType = typeState,
                        onTypeChange = { viewModel.selectedTypeFilter.value = it },
                        currencyFormatter = currencyFormatter,
                        onSelectExpense = { selectedExpenseForDetail = it }
                    )
                }
                3 -> SettingsTab(
                    budget = budget,
                    onUpdateBudget = { viewModel.setBudget(it) },
                    onResetData = { viewModel.resetDatabase() },
                    totalExpensesCount = allExpenses.size,
                    currencySymbol = currencySymbol,
                    onUpdateCurrency = { viewModel.setCurrencySymbol(it) },
                    onExportCsv = { exportAndShareCsv(context, allExpenses) },
                    onImportCsv = { uri ->
                        viewModel.importCsv(
                            context.contentResolver,
                            uri,
                            onSuccess = { count ->
                                android.widget.Toast.makeText(context, "Successfully imported $count transactions", android.widget.Toast.LENGTH_LONG).show()
                            },
                            onFailure = { error ->
                                android.widget.Toast.makeText(context, "Import failed: $error", android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    onOpenPdfExport = {
                        showPdfDialog = true
                    }
                )
            }
        }

        // Add Dialog
        if (showAddDialog) {
            TransactionDialog(
                onDismiss = { showAddDialog = false },
                onSave = { title, amount, category, timestamp, note, isIncome, paymentMethod ->
                    viewModel.addExpense(title, amount, category, timestamp, note, isIncome, paymentMethod)
                    showAddDialog = false
                }
            )
        }

        // User Profile Dialog
        if (showProfileDialog) {
            UserProfileDialog(
                currentName = userName,
                currentEmail = userEmail,
                currentAvatar = userAvatar,
                onDismiss = { showProfileDialog = false },
                onSave = { name, email, avatar ->
                    viewModel.updateProfile(name, email, avatar)
                    showProfileDialog = false
                }
            )
        }

        // PDF Report Date Range Dialog
        if (showPdfDialog) {
            DateRangePdfDialog(
                onDismiss = { showPdfDialog = false },
                onGenerate = { start, end ->
                    showPdfDialog = false
                    exportToPdf(
                        context = context,
                        expenses = allExpenses,
                        startDate = start,
                        endDate = end,
                        currencySymbol = currencySymbol,
                        onSuccess = { file ->
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "com.example.fileprovider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "SpendWise Financial Statement")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share PDF Statement"))
                        },
                        onFailure = { error ->
                            android.widget.Toast.makeText(context, "PDF Export failed: $error", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        // Detail Dialog
        selectedExpenseForDetail?.let { expense ->
            DetailDialog(
                expense = expense,
                currencyFormatter = currencyFormatter,
                onDismiss = { selectedExpenseForDetail = null },
                onDelete = {
                    viewModel.deleteExpense(expense)
                    selectedExpenseForDetail = null
                },
                onEdit = {
                    showEditDialog = expense
                    selectedExpenseForDetail = null
                }
            )
        }

        // Edit Dialog
        showEditDialog?.let { expense ->
            TransactionDialog(
                expenseToEdit = expense,
                onDismiss = { showEditDialog = null },
                onSave = { title, amount, category, timestamp, note, isIncome, paymentMethod ->
                    viewModel.updateExpense(
                        expense.copy(
                            title = title,
                            amount = amount,
                            category = category,
                            timestamp = timestamp,
                            note = note,
                            isIncome = isIncome,
                            paymentMethod = paymentMethod
                        )
                    )
                    showEditDialog = null
                }
            )
        }
    }
}

// ==================== TABS ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardTab(
    stats: com.example.viewmodel.FinancialStats,
    budget: Double,
    recentExpenses: List<Expense>,
    categoryShares: List<com.example.viewmodel.CategoryShare>,
    currencyFormatter: DecimalFormat,
    onNavigateToHistory: () -> Unit
) {
    var activeBreakdownType by remember { mutableStateOf<String?>(null) }

    activeBreakdownType?.let { type ->
        SourceBreakdownDialog(
            type = type,
            stats = stats,
            currencyFormatter = currencyFormatter,
            onDismiss = { activeBreakdownType = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Primary Balance Card (Bento Large Rectangle)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("balance_card")
                    .combinedClickable(
                        onClick = { activeBreakdownType = "Balance" },
                        onLongClick = { activeBreakdownType = "Balance" }
                    ),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(BentoPrimary)
                        .padding(24.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL BALANCE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet Detail",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = currencyFormatter.format(stats.balance),
                            style = MaterialTheme.typography.displaySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = "Trend up icon",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "+12.5%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "vs last month",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }

        // Secondary Stats (Bento Squares Side-by-Side)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income Bento Square Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp)
                        .combinedClickable(
                            onClick = { activeBreakdownType = "Income" },
                            onLongClick = { activeBreakdownType = "Income" }
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoIncomeBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Income icon",
                                tint = BentoIncomeText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Income",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = BentoTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = currencyFormatter.format(stats.totalIncome),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = BentoTextPrimary,
                                    fontWeight = FontWeight.Black
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Spent Bento Square Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp)
                        .combinedClickable(
                            onClick = { activeBreakdownType = "Spent" },
                            onLongClick = { activeBreakdownType = "Spent" }
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoSpentBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Expenses icon",
                                tint = BentoSpentText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Spent",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = BentoTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = currencyFormatter.format(stats.totalExpense),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = BentoTextPrimary,
                                    fontWeight = FontWeight.Black
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Budget Warning Card (Horizontal Bento Block)
        item {
            val budgetRatio = if (budget > 0) (stats.totalExpense / budget).toFloat() else 0f
            val isBudgetExceeded = budgetRatio >= 1.0f
            val isBudgetWarning = budgetRatio >= 0.8f && budgetRatio < 1.0f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isBudgetExceeded -> CrimsonRed.copy(alpha = 0.08f)
                        isBudgetWarning -> SoftYellow.copy(alpha = 0.08f)
                        else -> Color.White
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isBudgetExceeded) Icons.Default.Warning else Icons.Default.PieChart,
                                contentDescription = "Budget Status Icon",
                                tint = when {
                                    isBudgetExceeded -> CrimsonRed
                                    isBudgetWarning -> SoftYellow
                                    else -> BentoPrimary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Monthly Budget",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextPrimary
                                )
                            )
                        }
                        Text(
                            text = "${(budgetRatio * 100).toInt()}% Used",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isBudgetExceeded -> CrimsonRed
                                    isBudgetWarning -> SoftYellow
                                    else -> BentoPrimary
                                }
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { budgetRatio.coerceAtMost(1.0f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = when {
                            isBudgetExceeded -> CrimsonRed
                            isBudgetWarning -> SoftYellow
                            else -> BentoPrimary
                        },
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Spent: ${currencyFormatter.format(stats.totalExpense)}",
                            style = MaterialTheme.typography.bodySmall.copy(color = BentoTextSecondary)
                        )
                        Text(
                            text = "Limit: ${currencyFormatter.format(budget)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BentoTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    if (isBudgetExceeded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ Budget Limit Exceeded! Reduce non-essential spending.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CrimsonRed,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    } else if (isBudgetWarning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚡ Warning: You've used over 80% of your budget.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SoftYellow,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // Recent Transactions (Bento Vertical Fill Rectangle - ColSpan 2)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recent_transactions_bento"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3EDF7)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary
                            )
                        )
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BentoPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable { onNavigateToHistory() }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (recentExpenses.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = "No recent transactions",
                                tint = SlateGrey.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "No Transactions",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = BentoTextSecondary
                                )
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            recentExpenses.forEach { expense ->
                                val categoryColor = remember(expense.category) { getCategoryColor(expense.category) }
                                val categoryIcon = remember(expense.category) { getCategoryIcon(expense.category) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color(0xFFF0F4F8), RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            categoryIcon,
                                            contentDescription = expense.category,
                                            tint = BentoTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = expense.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = BentoTextPrimary
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${expense.category} • ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(expense.timestamp))}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = BentoTextSecondary)
                                        )
                                    }
                                    Text(
                                        text = "${if (expense.isIncome) "+" else "-"}${currencyFormatter.format(expense.amount)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (expense.isIncome) EmeraldGreen else CrimsonRed
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category Breakdown Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Distribution",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary
                    )
                )
                Text(
                    text = "See All",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BentoPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable { onNavigateToHistory() }
                )
            }
        }

        // Empty state or actual categories
        if (categoryShares.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = "No data icon",
                            tint = SlateGrey.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Expenses Tracked Yet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Add expenses using the floating button below to populate category trends.",
                            style = MaterialTheme.typography.bodySmall.copy(color = SlateGrey),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(categoryShares) { share ->
                CategoryBreakdownItem(
                    share = share,
                    currencyFormatter = currencyFormatter
                )
            }
        }
    }
}

@Composable
fun CategoryBreakdownItem(
    share: com.example.viewmodel.CategoryShare,
    currencyFormatter: DecimalFormat
) {
    val categoryColor = remember(share.category) { getCategoryColor(share.category) }
    val categoryIcon = remember(share.category) { getCategoryIcon(share.category) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3EDF7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFF0F4F8), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    categoryIcon,
                    contentDescription = share.category,
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = share.category,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoTextPrimary
                        )
                    )
                    Text(
                        text = currencyFormatter.format(share.total),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoTextPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { share.percentage },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = categoryColor,
                        trackColor = Color(0xFFF0F4F8)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(share.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BentoTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTab(
    expenses: List<Expense>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    selectedType: String,
    onTypeChange: (String) -> Unit,
    currencyFormatter: DecimalFormat,
    onSelectExpense: (Expense) -> Unit
) {
    var showCategoryDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_tab")
    ) {
        // Search & Filter Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search description, note...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filters Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Type Selection Tabs (All, Exp, Inc)
                    val types = listOf("All", "Expense", "Income")
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.weight(1f)
                    ) {
                        types.forEachIndexed { index, label ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                                onClick = { onTypeChange(label) },
                                selected = selectedType == label
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Category Dropdown Filter
                    Box(modifier = Modifier.wrapContentSize()) {
                        Button(
                            onClick = { showCategoryDropdown = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedCategory == "All") MaterialTheme.colorScheme.secondaryContainer else EmeraldGreen,
                                contentColor = if (selectedCategory == "All") MaterialTheme.colorScheme.onSecondaryContainer else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    selectedCategory,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Dropdown filter",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(start = 4.dp)
                                )
                            }
                        }

                        val categories = listOf("All", "Food", "Shopping", "Transport", "Housing", "Entertainment", "Health", "Salary", "Freelance", "Other")
                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        onCategoryChange(cat)
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Transactions List
        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "No match icon",
                        tint = SlateGrey.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No Transactions Found",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Try adjusting your filters or search query.",
                        style = MaterialTheme.typography.bodySmall.copy(color = SlateGrey)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("expenses_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(expenses, key = { it.id }) { expense ->
                    TransactionItem(
                        expense = expense,
                        currencyFormatter = currencyFormatter,
                        onClick = { onSelectExpense(expense) }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    expense: Expense,
    currencyFormatter: DecimalFormat,
    onClick: () -> Unit
) {
    val categoryColor = remember(expense.category) { getCategoryColor(expense.category) }
    val categoryIcon = remember(expense.category) { getCategoryIcon(expense.category) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("expense_item_${expense.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3EDF7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFF0F4F8), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    categoryIcon,
                    contentDescription = expense.category,
                    tint = BentoTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = categoryColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(BentoTextSecondary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatTimestamp(expense.timestamp),
                        style = MaterialTheme.typography.labelSmall.copy(color = BentoTextSecondary)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${if (expense.isIncome) "+" else "-"}${currencyFormatter.format(expense.amount)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (expense.isIncome) EmeraldGreen else CrimsonRed
                )
            )
        }
    }
}

@Composable
fun SettingsTab(
    budget: Double,
    onUpdateBudget: (Double) -> Unit,
    onResetData: () -> Unit,
    totalExpensesCount: Int,
    currencySymbol: String,
    onUpdateCurrency: (String) -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: (android.net.Uri) -> Unit,
    onOpenPdfExport: () -> Unit
) {
    var budgetInput by remember { mutableStateOf(budget.toString()) }
    var showResetDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            onImportCsv(uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_tab")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Budget Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Set Monthly Limit",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Set your limit to receive dynamic dashboard visual progress tracking and overspending warnings.",
                        style = MaterialTheme.typography.bodySmall.copy(color = SlateGrey),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Monthly Budget Limit ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_input_field"),
                        leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Budget Wallet") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val budgetValue = budgetInput.toDoubleOrNull() ?: 2000.0
                            onUpdateBudget(budgetValue)
                            keyboardController?.hide()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_budget_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Update Budget Limit", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Currency Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "App Currency",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Select the primary currency symbol to use throughout the application interface.",
                        style = MaterialTheme.typography.bodySmall.copy(color = SlateGrey),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    val currencies = listOf(
                        "$" to "US Dollar ($)",
                        "₹" to "Indian Rupee (₹)",
                        "€" to "Euro (€)",
                        "£" to "British Pound (£)",
                        "¥" to "Japanese Yen (¥)"
                    )
                    
                    currencies.forEach { (symbol, label) ->
                        val isSelected = currencySymbol == symbol
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) BentoNavActive else Color.Transparent)
                                .clickable { onUpdateCurrency(symbol) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) BentoPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = BentoPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        // Data Export Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Reports & Data Portability",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    
                    Text(
                        "Manage your transaction records by importing existing data, exporting backups, or generating elegant statements.",
                        style = MaterialTheme.typography.bodySmall.copy(color = SlateGrey)
                    )

                    // CSV Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onExportCsv,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_csv_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Export CSV", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Button(
                            onClick = { csvLauncher.launch("*/*") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_csv_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.UploadFile, contentDescription = "Import CSV", tint = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Import CSV", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // PDF Download Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = "PDF Report",
                                    tint = BentoPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column {
                                    Text(
                                        "Download Financial Statement",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        "Generate custom statements within date boundaries.",
                                        style = MaterialTheme.typography.labelSmall.copy(color = SlateGrey)
                                    )
                                }
                            }

                            Button(
                                onClick = onOpenPdfExport,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_pdf_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                                shape = RoundedCornerShape(12.dp)
                              ) {
                                  Row(
                                      verticalAlignment = Alignment.CenterVertically,
                                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                                  ) {
                                      Icon(Icons.Default.Download, contentDescription = "Download PDF", tint = Color.White)
                                      Text("Download as PDF", fontWeight = FontWeight.Bold, color = Color.White)
                                  }
                              }
                        }
                    }
                }
            }
        }

        // Data Management Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Data Administration",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Saved Entries:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "$totalExpensesCount transactions",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_db_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear all", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear All Transactions", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Developer", tint = EmeraldGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "SpendWise v1.0",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Secure offline ledger powered by Kotlin, Jetpack Compose, and Room Database.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = SlateGrey
                    )
                }
            }
        }
    }

    // Reset confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Ledger") },
            text = { Text("Are you sure you want to permanently clear your ledger history? This operation cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetData()
                        showResetDialog = false
                    }
                ) {
                    Text("Clear All", color = CrimsonRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = SlateGrey)
                }
            }
        )
    }
}

// ==================== DIALOGS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDialog(
    expenseToEdit: Expense? = null,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, Long, String, Boolean, String) -> Unit
) {
    var title by remember { mutableStateOf(expenseToEdit?.title ?: "") }
    var amountString by remember { mutableStateOf(expenseToEdit?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(expenseToEdit?.category ?: "Food") }
    var note by remember { mutableStateOf(expenseToEdit?.note ?: "") }
    var isIncome by remember { mutableStateOf(expenseToEdit?.isIncome ?: false) }
    var paymentMethod by remember { mutableStateOf(expenseToEdit?.paymentMethod ?: "Online") }
    
    var categoryExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val categoriesList = if (isIncome) {
        listOf("Salary", "Freelance", "Other")
    } else {
        listOf("Food", "Shopping", "Transport", "Housing", "Entertainment", "Health", "Other")
    }

    // Auto-update categories list if user toggles Income / Expense
    LaunchedEffect(isIncome) {
        if (isIncome && category !in listOf("Salary", "Freelance", "Other")) {
            category = "Salary"
        } else if (!isIncome && category in listOf("Salary", "Freelance")) {
            category = "Food"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transaction_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (expenseToEdit == null) "Add Transaction" else "Edit Transaction",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                // Toggle Row for Expense / Income
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isIncome) CrimsonRed else Color.Transparent)
                            .clickable { isIncome = false }
                            .testTag("toggle_expense"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Expense",
                            color = if (!isIncome) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isIncome) EmeraldGreen else Color.Transparent)
                            .clickable { isIncome = true }
                            .testTag("toggle_income"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Income",
                            color = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_title"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Amount Input
                OutlinedTextField(
                    value = amountString,
                    onValueChange = { amountString = it },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_amount"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Selection Spinner Box
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("category_spinner"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categoriesList.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        category = selectionOption
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Notes Input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes / Tags (Optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_notes"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Payment Method Selector (Online vs Cash)
                Text(
                    text = "Payment Source",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (paymentMethod == "Online") Color(0xFF3B82F6) else Color.Transparent)
                            .clickable { paymentMethod = "Online" }
                            .testTag("payment_online"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Online",
                            color = if (paymentMethod == "Online") Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (paymentMethod == "Cash") Color(0xFF10B981) else Color.Transparent)
                            .clickable { paymentMethod = "Cash" }
                            .testTag("payment_cash"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Cash",
                            color = if (paymentMethod == "Cash") Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                errorMsg?.let { msg ->
                    Text(msg, color = CrimsonRed, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                // Actions Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = SlateGrey)
                    }

                    Button(
                        onClick = {
                            val amount = amountString.toDoubleOrNull()
                            if (title.isBlank()) {
                                errorMsg = "Title cannot be empty!"
                            } else if (amount == null || amount <= 0) {
                                errorMsg = "Please enter a valid positive amount!"
                            } else {
                                onSave(
                                    title.trim(),
                                    amount,
                                    category,
                                    expenseToEdit?.timestamp ?: System.currentTimeMillis(),
                                    note.trim(),
                                    isIncome,
                                    paymentMethod
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_transaction_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isIncome) EmeraldGreen else CrimsonRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailDialog(
    expense: Expense,
    currencyFormatter: DecimalFormat,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val categoryColor = remember(expense.category) { getCategoryColor(expense.category) }
    val categoryIcon = remember(expense.category) { getCategoryIcon(expense.category) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("detail_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(categoryColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                categoryIcon,
                                contentDescription = expense.category,
                                tint = categoryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = categoryColor
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (expense.isIncome) EmeraldGreen.copy(alpha = 0.15f) else CrimsonRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (expense.isIncome) "Income" else "Expense",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (expense.isIncome) EmeraldGreen else CrimsonRed
                            )
                        )
                    }
                }

                // Title
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
                )

                // Amount
                Text(
                    text = "${if (expense.isIncome) "+" else "-"}${currencyFormatter.format(expense.amount)}",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        color = if (expense.isIncome) EmeraldGreen else CrimsonRed
                    )
                )

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Metadata details
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Date & Time:", style = MaterialTheme.typography.bodyMedium.copy(color = SlateGrey))
                        Text(
                            formatTimestamp(expense.timestamp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (expense.note.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Note / Tag:", style = MaterialTheme.typography.bodyMedium.copy(color = SlateGrey))
                            Text(
                                expense.note,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("detail_edit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Transaction", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onDelete,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("detail_delete_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Transaction", modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// Helpers
fun getCategoryIcon(category: String) = when (category) {
    "Food" -> Icons.Default.Fastfood
    "Shopping" -> Icons.Default.ShoppingBag
    "Transport" -> Icons.Default.DirectionsCar
    "Housing" -> Icons.Default.Home
    "Entertainment" -> Icons.Default.Movie
    "Health" -> Icons.Default.MedicalServices
    "Salary" -> Icons.Default.AttachMoney
    "Freelance" -> Icons.Default.Work
    else -> Icons.Default.AccountBalanceWallet
}

fun getCategoryColor(category: String) = when (category) {
    "Food" -> WarmCoral
    "Shopping" -> LightBlue
    "Transport" -> OceanTeal
    "Housing" -> RoyalPurple
    "Entertainment" -> Pink40
    "Health" -> CrimsonRed
    "Salary" -> EmeraldGreen
    "Freelance" -> MintGreen
    else -> SlateGrey
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun SourceBreakdownDialog(
    type: String, // "Balance", "Income", "Spent"
    stats: com.example.viewmodel.FinancialStats,
    currencyFormatter: DecimalFormat,
    onDismiss: () -> Unit
) {
    val title = when (type) {
        "Balance" -> "Balance Source Split"
        "Income" -> "Income Source Split"
        "Spent" -> "Spent Source Split"
        else -> "Source Breakdown"
    }

    val totalAmount = when (type) {
        "Balance" -> stats.balance
        "Income" -> stats.totalIncome
        "Spent" -> stats.totalExpense
        else -> 0.0
    }

    val onlineAmount = when (type) {
        "Balance" -> stats.onlineBalance
        "Income" -> stats.onlineIncome
        "Spent" -> stats.onlineExpense
        else -> 0.0
    }

    val cashAmount = when (type) {
        "Balance" -> stats.cashBalance
        "Income" -> stats.cashIncome
        "Spent" -> stats.cashExpense
        else -> 0.0
    }

    val onlineColor = Color(0xFF3B82F6) // Online Blue
    val cashColor = Color(0xFF10B981)   // Cash Green

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("source_breakdown_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Total Sum Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TOTAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormatter.format(totalAmount),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Splits
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Online Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(onlineColor.copy(alpha = 0.08f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(onlineColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CreditCard,
                                    contentDescription = "Online",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Online Payment",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                )
                                Text(
                                    "Digital transfers & cards",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                                )
                            }
                        }
                        Text(
                            text = currencyFormatter.format(onlineAmount),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = onlineColor
                            )
                        )
                    }

                    // Cash Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cashColor.copy(alpha = 0.08f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(cashColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Payments,
                                    contentDescription = "Cash",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Cash Wallet",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                )
                                Text(
                                    "Physical notes & coins",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                                )
                            }
                        }
                        Text(
                            text = currencyFormatter.format(cashAmount),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = cashColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom CTA
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileDialog(
    currentName: String,
    currentEmail: String,
    currentAvatar: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    var selectedAvatar by remember { mutableStateOf(currentAvatar) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("user_profile_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Edit Profile",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Avatar Choice Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val avatars = listOf("avatar_1", "avatar_2", "avatar_3", "avatar_4")
                    val avatarColors = listOf(
                        Color(0xFF3B82F6), // Blue
                        Color(0xFF10B981), // Green
                        Color(0xFFEF4444), // Red
                        Color(0xFF8B5CF6)  // Purple
                    )

                    avatars.forEachIndexed { index, avatarId ->
                        val isSelected = selectedAvatar == avatarId
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(avatarColors[index])
                                .clickable { selectedAvatar = avatarId }
                                .let {
                                    if (isSelected) {
                                        it.background(avatarColors[index], CircleShape)
                                            .padding(3.dp)
                                            .background(Color.White, CircleShape)
                                            .padding(3.dp)
                                            .background(avatarColors[index], CircleShape)
                                    } else {
                                        it
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.take(1).uppercase().ifEmpty { "U" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") }
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(name.trim(), email.trim(), selectedAvatar)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

fun exportAndShareCsv(context: Context, expenses: List<Expense>) {
    try {
        val fileName = "spendwise_transactions_${System.currentTimeMillis()}.csv"
        val cacheFile = File(context.cacheDir, fileName)
        
        FileWriter(cacheFile).use { writer ->
            writer.append("ID,Title,Amount,Category,Type,Payment Method,Date,Note\n")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            for (expense in expenses) {
                val type = if (expense.isIncome) "Income" else "Expense"
                val dateStr = sdf.format(Date(expense.timestamp))
                val titleEscaped = expense.title.replace("\"", "\"\"")
                val categoryEscaped = expense.category.replace("\"", "\"\"")
                val noteEscaped = expense.note.replace("\"", "\"\"")
                writer.append("${expense.id},\"$titleEscaped\",${expense.amount},\"$categoryEscaped\",$type,\"${expense.paymentMethod}\",$dateStr,\"$noteEscaped\"\n")
            }
        }
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            cacheFile
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SpendWise Transactions Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Transactions CSV"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun DateRangePdfDialog(
    onDismiss: () -> Unit,
    onGenerate: (Long, Long) -> Unit
) {
    val context = LocalContext.current
    
    // Default start date is 30 days ago, end date is today
    var startDateMillis by remember { 
        mutableStateOf(
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -30)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    
    var endDateMillis by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        )
    }
    
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("date_range_pdf_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Export PDF Report",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Text(
                    "Select the start and end date boundaries for your PDF statement.",
                    style = MaterialTheme.typography.bodySmall.copy(color = SlateGrey)
                )
                
                // Start Date Field Wrapped in Clickable Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth, 0, 0, 0)
                                    }
                                    startDateMillis = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    OutlinedTextField(
                        value = dateFormatter.format(Date(startDateMillis)),
                        onValueChange = {},
                        label = { Text("Start Date") },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Start Date") }
                    )
                }
                
                // End Date Field Wrapped in Clickable Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = endDateMillis }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth, 23, 59, 59)
                                    }
                                    endDateMillis = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    OutlinedTextField(
                        value = dateFormatter.format(Date(endDateMillis)),
                        onValueChange = {},
                        label = { Text("End Date") },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = "End Date") }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { onGenerate(startDateMillis, endDateMillis) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Generate Statement", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

fun exportToPdf(
    context: Context,
    expenses: List<Expense>,
    startDate: Long,
    endDate: Long,
    currencySymbol: String,
    onSuccess: (File) -> Unit,
    onFailure: (String) -> Unit
) {
    try {
        val filtered = expenses.filter { it.timestamp in startDate..endDate }
            .sortedByDescending { it.timestamp }

        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()
        
        var pageNumber = 1
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points
        
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        var y = 50f
        
        // 1. Draw PDF Header
        titlePaint.textSize = 20f
        titlePaint.isFakeBoldText = true
        titlePaint.color = android.graphics.Color.parseColor("#0F172A") // Slate-900
        canvas.drawText("SpendWise Financial Statement", 40f, y, titlePaint)
        
        y += 25f
        paint.textSize = 10f
        paint.color = android.graphics.Color.parseColor("#475569") // Slate-600
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        canvas.drawText("Statement Period: ${sdfDate.format(Date(startDate))} to ${sdfDate.format(Date(endDate))}", 40f, y, paint)
        
        y += 15f
        canvas.drawText("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 40f, y, paint)
        
        // Horizontal line
        y += 15f
        paint.color = android.graphics.Color.parseColor("#CBD5E1") // Slate-300
        canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, paint)
        
        // 2. Draw Financial Summary
        y += 30f
        // Colored summary banner background
        paint.color = android.graphics.Color.parseColor("#F8FAFC") // Slate-50
        canvas.drawRect(40f, y - 15f, (pageWidth - 40).toFloat(), y + 45f, paint)
        
        val totalIncome = filtered.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = filtered.filter { !it.isIncome }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense
        
        paint.color = android.graphics.Color.parseColor("#0F172A")
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("SUMMARY STATS", 50f, y, paint)
        
        paint.isFakeBoldText = false
        paint.textSize = 9f
        paint.color = android.graphics.Color.parseColor("#64748B")
        canvas.drawText("Total Income", 50f, y + 20f, paint)
        canvas.drawText("Total Expenses", 200f, y + 20f, paint)
        canvas.drawText("Net Balance", 350f, y + 20f, paint)
        
        paint.isFakeBoldText = true
        paint.textSize = 12f
        
        // Income (Green)
        paint.color = android.graphics.Color.parseColor("#10B981")
        canvas.drawText("$currencySymbol${String.format("%.2f", totalIncome)}", 50f, y + 35f, paint)
        
        // Expense (Red)
        paint.color = android.graphics.Color.parseColor("#EF4444")
        canvas.drawText("$currencySymbol${String.format("%.2f", totalExpense)}", 200f, y + 35f, paint)
        
        // Balance (Blue / Slate)
        paint.color = if (netBalance >= 0) android.graphics.Color.parseColor("#3B82F6") else android.graphics.Color.parseColor("#EF4444")
        canvas.drawText("$currencySymbol${String.format("%.2f", netBalance)}", 350f, y + 35f, paint)
        
        y += 70f
        
        // 3. Draw Table Headers
        paint.color = android.graphics.Color.parseColor("#0F172A")
        paint.textSize = 10f
        paint.isFakeBoldText = true
        
        canvas.drawText("Date", 40f, y, paint)
        canvas.drawText("Title", 130f, y, paint)
        canvas.drawText("Category", 280f, y, paint)
        canvas.drawText("Method", 400f, y, paint)
        canvas.drawText("Amount", 480f, y, paint)
        
        y += 8f
        paint.color = android.graphics.Color.parseColor("#0F172A")
        canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, paint)
        
        y += 18f
        
        // Iterate and draw list
        val sdfRow = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        paint.isFakeBoldText = false
        paint.textSize = 9f
        
        for (expense in filtered) {
            // Check page boundary
            if (y > pageHeight - 60f) {
                // Finish page
                pdfDocument.finishPage(page)
                
                // Start a new page
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
                
                // Draw headers on new page
                paint.color = android.graphics.Color.parseColor("#0F172A")
                paint.textSize = 10f
                paint.isFakeBoldText = true
                canvas.drawText("Date", 40f, y, paint)
                canvas.drawText("Title", 130f, y, paint)
                canvas.drawText("Category", 280f, y, paint)
                canvas.drawText("Method", 400f, y, paint)
                canvas.drawText("Amount", 480f, y, paint)
                
                y += 8f
                paint.color = android.graphics.Color.parseColor("#0F172A")
                canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, paint)
                
                y += 18f
                paint.isFakeBoldText = false
                paint.textSize = 9f
            }
            
            // Alternating row background helper
            paint.color = android.graphics.Color.parseColor("#475569") // Default text color
            
            val dateText = sdfRow.format(Date(expense.timestamp))
            val titleText = if (expense.title.length > 25) expense.title.take(22) + "..." else expense.title
            val catText = expense.category
            val methodText = expense.paymentMethod
            val amtPrefix = if (expense.isIncome) "+" else "-"
            val amtText = "$amtPrefix$currencySymbol${String.format("%.2f", expense.amount)}"
            
            canvas.drawText(dateText, 40f, y, paint)
            canvas.drawText(titleText, 130f, y, paint)
            canvas.drawText(catText, 280f, y, paint)
            canvas.drawText(methodText, 400f, y, paint)
            
            // Right-aligned / colored amount
            val amtPaint = Paint(paint)
            amtPaint.isFakeBoldText = true
            amtPaint.color = if (expense.isIncome) android.graphics.Color.parseColor("#10B981") else android.graphics.Color.parseColor("#EF4444")
            canvas.drawText(amtText, 480f, y, amtPaint)
            
            // Draw subtle divider
            y += 10f
            paint.color = android.graphics.Color.parseColor("#F1F5F9") // Slate-100
            canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, paint)
            
            y += 15f
        }
        
        // Draw footer (page number) on last page
        paint.color = android.graphics.Color.parseColor("#94A3B8")
        paint.textSize = 8f
        canvas.drawText("Page $pageNumber", (pageWidth / 2 - 15).toFloat(), (pageHeight - 30).toFloat(), paint)
        
        pdfDocument.finishPage(page)
        
        // Save PDF to cache or public downloads
        val fileName = "spendwise_report_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(context.cacheDir, fileName)
        pdfDocument.writeTo(pdfFile.outputStream())
        pdfDocument.close()
        
        onSuccess(pdfFile)
    } catch (e: Exception) {
        onFailure(e.message ?: "Failed to generate PDF")
    }
}

