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

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    private val sharedPrefs = application.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)

    // Raw stream of expenses
    val allExpenses: StateFlow<List<Expense>>

    // Custom budget state
    private val _budget = MutableStateFlow(2000.0)
    val budget: StateFlow<Double> = _budget.asStateFlow()

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

        // Load budget from shared preferences
        val savedBudget = sharedPrefs.getFloat("monthly_budget", 2000f).toDouble()
        _budget.value = savedBudget
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
        val totalIncome = list.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = list.filter { !it.isIncome }.sumOf { it.amount }
        val balance = totalIncome - totalExpense
        
        FinancialStats(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = balance
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

    // Set Budget
    fun setBudget(newBudget: Double) {
        viewModelScope.launch {
            _budget.value = newBudget
            sharedPrefs.edit().putFloat("monthly_budget", newBudget.toFloat()).apply()
        }
    }

    // Add Transaction
    fun addExpense(title: String, amount: Double, category: String, timestamp: Long, note: String, isIncome: Boolean) {
        viewModelScope.launch {
            repository.insert(
                Expense(
                    title = title,
                    amount = amount,
                    category = category,
                    timestamp = timestamp,
                    note = note,
                    isIncome = isIncome
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
}

// Data holder classes
data class FinancialStats(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0
)

data class CategoryShare(
    val category: String,
    val total: Double,
    val percentage: Float
)
