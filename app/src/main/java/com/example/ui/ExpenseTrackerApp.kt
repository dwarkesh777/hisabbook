package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

    var currentTab by remember { mutableStateOf(0) } // 0 = Dashboard, 1 = History, 2 = Settings
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedExpenseForDetail by remember { mutableStateOf<Expense?>(null) }
    var showEditDialog by remember { mutableStateOf<Expense?>(null) }

    val currencyFormatter = remember { DecimalFormat("$#,##0.00") }

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
                                    text = "Jordan Smith",
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
                        onClick = { /* Demo notification toggle */ },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = BentoTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
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
                    icon = { Icon(Icons.Default.List, contentDescription = "History") },
                    label = { Text("History") },
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
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
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
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
        floatingActionButton = {
            if (currentTab != 2) {
                ExtendedFloatingActionButton(
                    text = { Text("Transaction") },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Transaction") },
                    onClick = { showAddDialog = true },
                    containerColor = BentoPrimary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .testTag("fab_add_transaction")
                        .padding(bottom = 8.dp)
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
                    onNavigateToHistory = { currentTab = 1 }
                )
                1 -> {
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
                2 -> SettingsTab(
                    budget = budget,
                    onUpdateBudget = { viewModel.setBudget(it) },
                    onResetData = { viewModel.resetDatabase() },
                    totalExpensesCount = allExpenses.size
                )
            }
        }

        // Add Dialog
        if (showAddDialog) {
            TransactionDialog(
                onDismiss = { showAddDialog = false },
                onSave = { title, amount, category, timestamp, note, isIncome ->
                    viewModel.addExpense(title, amount, category, timestamp, note, isIncome)
                    showAddDialog = false
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
                onSave = { title, amount, category, timestamp, note, isIncome ->
                    viewModel.updateExpense(
                        expense.copy(
                            title = title,
                            amount = amount,
                            category = category,
                            timestamp = timestamp,
                            note = note,
                            isIncome = isIncome
                        )
                    )
                    showEditDialog = null
                }
            )
        }
    }
}

// ==================== TABS ====================

@Composable
fun DashboardTab(
    stats: com.example.viewmodel.FinancialStats,
    budget: Double,
    recentExpenses: List<Expense>,
    categoryShares: List<com.example.viewmodel.CategoryShare>,
    currencyFormatter: DecimalFormat,
    onNavigateToHistory: () -> Unit
) {
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
                    .testTag("balance_card"),
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
                        .height(130.dp),
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
                        .height(130.dp),
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
    totalExpensesCount: Int
) {
    var budgetInput by remember { mutableStateOf(budget.toString()) }
    var showResetDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

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
                        label = { Text("Monthly Budget Limit ($)") },
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
    onSave: (String, Double, String, Long, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(expenseToEdit?.title ?: "") }
    var amountString by remember { mutableStateOf(expenseToEdit?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(expenseToEdit?.category ?: "Food") }
    var note by remember { mutableStateOf(expenseToEdit?.note ?: "") }
    var isIncome by remember { mutableStateOf(expenseToEdit?.isIncome ?: false) }
    
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
                                    isIncome
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
