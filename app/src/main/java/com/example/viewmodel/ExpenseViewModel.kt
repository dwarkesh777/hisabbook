package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Expense
import com.example.data.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    private val sharedPrefs = application.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)

    // Raw stream of expenses
    val allExpenses: StateFlow<List<Expense>>

    // Custom budget state
    private val _budget = MutableStateFlow(2000.0)
    val budget: StateFlow<Double> = _budget.asStateFlow()

    // Currency state
    private val _currencySymbol = MutableStateFlow("$")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    // User profile state
    private val _userName = MutableStateFlow("Jordan Smith")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("jordan.smith@example.com")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userAvatar = MutableStateFlow("avatar_1")
    val userAvatar: StateFlow<String> = _userAvatar.asStateFlow()

    // Filters
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow("All")
    val selectedTypeFilter = MutableStateFlow("All") // "All", "Expense", "Income"

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = ExpenseRepository(database.expenseDao())

        // Collect expenses
        allExpenses = repository.allExpenses.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Load settings from shared preferences
        val savedBudget = sharedPrefs.getFloat("monthly_budget", 2000f).toDouble()
        _budget.value = savedBudget

        _currencySymbol.value = sharedPrefs.getString("app_currency", "$") ?: "$"
        _userName.value = sharedPrefs.getString("user_name", "Jordan Smith") ?: "Jordan Smith"
        _userEmail.value = sharedPrefs.getString("user_email", "jordan.smith@example.com") ?: "jordan.smith@example.com"
        _userAvatar.value = sharedPrefs.getString("user_avatar", "avatar_1") ?: "avatar_1"
    }

    // Filtered Expenses
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        allExpenses,
        searchQuery,
        selectedCategoryFilter,
        selectedTypeFilter
    ) { expenses, query, category, type ->
        expenses.filter { expense ->
            val matchesQuery = expense.title.contains(query, ignoreCase = true) ||
                    expense.note.contains(query, ignoreCase = true) ||
                    expense.category.contains(query, ignoreCase = true)
            
            val matchesCategory = category == "All" || expense.category == category
            
            val matchesType = when (type) {
                "Expense" -> !expense.isIncome
                "Income" -> expense.isIncome
                else -> true
            }

            matchesQuery && matchesCategory && matchesType
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Financial calculations
    val financialStats = allExpenses.map { list ->
        val incomeList = list.filter { it.isIncome }
        val expenseList = list.filter { !it.isIncome }

        val totalIncome = incomeList.sumOf { it.amount }
        val totalExpense = expenseList.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        val onlineIncome = incomeList.filter { it.paymentMethod == "Online" }.sumOf { it.amount }
        val cashIncome = incomeList.filter { it.paymentMethod == "Cash" }.sumOf { it.amount }

        val onlineExpense = expenseList.filter { it.paymentMethod == "Online" }.sumOf { it.amount }
        val cashExpense = expenseList.filter { it.paymentMethod == "Cash" }.sumOf { it.amount }

        val onlineBalance = onlineIncome - onlineExpense
        val cashBalance = cashIncome - cashExpense
        
        FinancialStats(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = balance,
            onlineIncome = onlineIncome,
            cashIncome = cashIncome,
            onlineExpense = onlineExpense,
            cashExpense = cashExpense,
            onlineBalance = onlineBalance,
            cashBalance = cashBalance
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialStats()
    )

    // Category breakdown for expenses
    val categoryBreakdown = allExpenses.map { list ->
        val expensesOnly = list.filter { !it.isIncome }
        val total = expensesOnly.sumOf { it.amount }
        if (total == 0.0) return@map emptyList<CategoryShare>()

        expensesOnly.groupBy { it.category }
            .map { (category, items) ->
                val categorySum = items.sumOf { it.amount }
                CategoryShare(
                    category = category,
                    total = categorySum,
                    percentage = (categorySum / total).toFloat()
                )
            }.sortedByDescending { it.total }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Payment method breakdown for analytics charts
    val paymentMethodBreakdown = allExpenses.map { list ->
        val expensesOnly = list.filter { !it.isIncome }
        val onlineTotal = expensesOnly.filter { it.paymentMethod == "Online" }.sumOf { it.amount }
        val cashTotal = expensesOnly.filter { it.paymentMethod == "Cash" }.sumOf { it.amount }

        val categoryPaymentList = expensesOnly.groupBy { it.category }
            .map { (category, items) ->
                val online = items.filter { it.paymentMethod == "Online" }.sumOf { it.amount }
                val cash = items.filter { it.paymentMethod == "Cash" }.sumOf { it.amount }
                CategoryPaymentShare(category, online, cash)
            }.sortedByDescending { it.onlineAmount + it.cashAmount }

        PaymentMethodStats(
            onlineTotal = onlineTotal,
            cashTotal = cashTotal,
            categoryBreakdown = categoryPaymentList
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PaymentMethodStats()
    )

    // Set Budget
    fun setBudget(newBudget: Double) {
        viewModelScope.launch {
            _budget.value = newBudget
            sharedPrefs.edit().putFloat("monthly_budget", newBudget.toFloat()).apply()
        }
    }

    // Set Currency
    fun setCurrencySymbol(symbol: String) {
        viewModelScope.launch {
            _currencySymbol.value = symbol
            sharedPrefs.edit().putString("app_currency", symbol).apply()
        }
    }

    // Update Profile Detail
    fun updateProfile(name: String, email: String, avatar: String) {
        viewModelScope.launch {
            _userName.value = name
            _userEmail.value = email
            _userAvatar.value = avatar
            sharedPrefs.edit()
                .putString("user_name", name)
                .putString("user_email", email)
                .putString("user_avatar", avatar)
                .apply()
        }
    }

    // Add Transaction
    fun addExpense(title: String, amount: Double, category: String, timestamp: Long, note: String, isIncome: Boolean, paymentMethod: String = "Online") {
        viewModelScope.launch {
            repository.insert(
                Expense(
                    title = title,
                    amount = amount,
                    category = category,
                    timestamp = timestamp,
                    note = note,
                    isIncome = isIncome,
                    paymentMethod = paymentMethod
                )
            )
        }
    }

    // Update Transaction
    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.update(expense)
        }
    }

    // Delete Transaction
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.delete(expense)
        }
    }

    // Reset Database
    fun resetDatabase() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // Import CSV from uri
    fun importCsv(contentResolver: android.content.ContentResolver, uri: android.net.Uri, onSuccess: (Int) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    onFailure("Could not open file.")
                    return@launch
                }
                val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream))
                val headerLine = reader.readLine() // Read header
                var importedCount = 0
                var line: String? = reader.readLine()
                
                val hasId = headerLine?.startsWith("ID", ignoreCase = true) == true
                val offset = if (hasId) 1 else 0

                while (line != null) {
                    if (line.trim().isEmpty()) {
                        line = reader.readLine()
                        continue
                    }
                    
                    val tokens = parseCsvLine(line)
                    if (tokens.size >= (3 + offset)) {
                        val title = tokens.getOrNull(0 + offset)?.trim() ?: "Imported"
                        val amountStr = tokens.getOrNull(1 + offset)?.trim() ?: "0.0"
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        val category = tokens.getOrNull(2 + offset)?.trim() ?: "Other"
                        val type = tokens.getOrNull(3 + offset)?.trim() ?: "Expense"
                        val isIncome = type.equals("Income", ignoreCase = true)
                        val paymentMethod = tokens.getOrNull(4 + offset)?.trim() ?: "Online"
                        val dateStr = tokens.getOrNull(5 + offset)?.trim() ?: ""
                        val note = tokens.getOrNull(6 + offset)?.trim() ?: ""
                        
                        val timestamp = try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                        
                        repository.insert(
                            Expense(
                                title = title,
                                amount = amount,
                                category = category,
                                timestamp = timestamp,
                                note = note,
                                isIncome = isIncome,
                                paymentMethod = paymentMethod
                            )
                        )
                        importedCount++
                    }
                    line = reader.readLine()
                }
                reader.close()
                onSuccess(importedCount)
            } catch (e: Exception) {
                onFailure(e.message ?: "Unknown error")
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '\"') {
                    if (i + 1 < line.length && line[i + 1] == '\"') {
                        curVal.append('\"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    curVal.append(ch)
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true
                } else if (ch == ',') {
                    result.add(curVal.toString())
                    curVal = StringBuilder()
                } else {
                    curVal.append(ch)
                }
            }
            i++
        }
        result.add(curVal.toString())
        return result
    }
}

// Data holder classes
data class FinancialStats(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val onlineIncome: Double = 0.0,
    val cashIncome: Double = 0.0,
    val onlineExpense: Double = 0.0,
    val cashExpense: Double = 0.0,
    val onlineBalance: Double = 0.0,
    val cashBalance: Double = 0.0
)

data class CategoryShare(
    val category: String,
    val total: Double,
    val percentage: Float
)

data class CategoryPaymentShare(
    val category: String,
    val onlineAmount: Double,
    val cashAmount: Double
)

data class PaymentMethodStats(
    val onlineTotal: Double = 0.0,
    val cashTotal: Double = 0.0,
    val categoryBreakdown: List<CategoryPaymentShare> = emptyList()
)
