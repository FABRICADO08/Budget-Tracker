package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.Transaction
import com.example.data.local.repository.LocalTransactionRepository
import com.example.data.model.Expense
import com.example.data.model.SavingsGoal
import com.example.data.model.UserProfile
import com.example.data.repository.BudgetRepository
import com.example.ui.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DailyBudgetSummary(
    val dailyBudget: Double = 50.0,
    val currencySymbol: String = "$",
    val spentToday: Double = 0.0,
    val remainingToday: Double = 50.0,
    val progress: Float = 0f,
    val isOverBudget: Boolean = false,
    val totalIncomeToday: Double = 0.0
)

data class SavingsSummary(
    val totalSaved: Double = 0.0,
    val totalTarget: Double = 0.0,
    val overallProgress: Float = 0f,
    val completedGoalsCount: Int = 0,
    val activeGoalsCount: Int = 0
)

class BudgetViewModel(
    private val repository: BudgetRepository,
    private val localTransactionRepository: LocalTransactionRepository? = null,
    val userId: String
) : ViewModel() {

    val localTransactions: StateFlow<List<Transaction>> = localTransactionRepository?.allTransactions
        ?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        ) ?: MutableStateFlow(emptyList())

    private val _selectedDate = MutableStateFlow(DateUtils.getTodayIso())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val userProfile: StateFlow<UserProfile> = repository.observeUserProfile(userId)
        .map { it ?: UserProfile(userId = userId, dailyBudget = 500.0, currencySymbol = "R") }
        .catch { e -> _errorMessage.value = e.localizedMessage }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserProfile(userId = userId, dailyBudget = 500.0, currencySymbol = "R")
        )

    val allExpenses: StateFlow<List<Expense>> = repository.observeExpenses(userId)
        .catch { e -> _errorMessage.value = e.localizedMessage }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val savingsGoals: StateFlow<List<SavingsGoal>> = repository.observeSavingsGoals(userId)
        .catch { e -> _errorMessage.value = e.localizedMessage }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Filtered expenses for selected date and category
    val currentDayExpenses: StateFlow<List<Expense>> = combine(
        allExpenses,
        _selectedDate,
        _selectedCategory
    ) { expenses, date, cat ->
        expenses.filter { exp ->
            exp.date == date && (cat == null || exp.category == cat)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // Daily budget summary for selected date
    val dailySummary: StateFlow<DailyBudgetSummary> = combine(
        userProfile,
        allExpenses,
        _selectedDate
    ) { profile, expenses, date ->
        val dayExpenses = expenses.filter { it.date == date }
        val spent = dayExpenses.filter { it.type == "expense" }.sumOf { it.amount }
        val income = dayExpenses.filter { it.type == "income" }.sumOf { it.amount }
        val budget = profile.dailyBudget
        val remaining = budget - spent
        val progress = if (budget > 0) ((spent / budget).toFloat()).coerceIn(0f, 2f) else 0f
        DailyBudgetSummary(
            dailyBudget = budget,
            currencySymbol = profile.currencySymbol,
            spentToday = spent,
            remainingToday = remaining,
            progress = progress,
            isOverBudget = spent > budget,
            totalIncomeToday = income
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DailyBudgetSummary()
    )

    // Savings summary
    val savingsSummary: StateFlow<SavingsSummary> = savingsGoals.combine(userProfile) { goals, _ ->
        val totalSaved = goals.sumOf { it.currentAmount }
        val totalTarget = goals.sumOf { it.targetAmount }
        val progress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f
        val completed = goals.count { it.isCompleted }
        SavingsSummary(
            totalSaved = totalSaved,
            totalTarget = totalTarget,
            overallProgress = progress,
            completedGoalsCount = completed,
            activeGoalsCount = goals.size - completed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SavingsSummary()
    )

    init {
        // Initialize user profile in background if missing
        viewModelScope.launch {
            try {
                repository.saveUserProfile(UserProfile(userId = userId, dailyBudget = 500.0, currencySymbol = "R"))
            } catch (ignored: Exception) {}
        }
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun shiftDate(days: Int) {
        _selectedDate.value = DateUtils.addDaysToIso(_selectedDate.value, days)
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun addExpense(
        title: String,
        amount: Double,
        category: String,
        date: String,
        type: String,
        note: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                localTransactionRepository?.insert(
                    Transaction(
                        amount = amount,
                        category = category,
                        date = date,
                        description = if (note.isNotBlank()) "${title.trim()} - ${note.trim()}" else title.trim(),
                        type = type
                    )
                )
                repository.addExpense(
                    Expense(
                        userId = userId,
                        title = title.trim(),
                        amount = amount,
                        category = category,
                        date = date,
                        type = type,
                        note = note.trim()
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to add transaction"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateExpense(expense)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update transaction"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteExpense(expenseId)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to delete transaction"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSavingsGoal(
        title: String,
        targetAmount: Double,
        currentAmount: Double,
        targetDate: String,
        category: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.addSavingsGoal(
                    SavingsGoal(
                        userId = userId,
                        title = title.trim(),
                        targetAmount = targetAmount,
                        currentAmount = currentAmount,
                        targetDate = targetDate,
                        category = category,
                        colorHex = colorHex
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to add savings goal"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateSavingsGoal(goal)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update savings goal"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun contributeToGoal(goal: SavingsGoal, additionalAmount: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.addContributionToGoal(goal.id, goal.currentAmount, additionalAmount)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to contribute to goal"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSavingsGoal(goalId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteSavingsGoal(goalId)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to delete savings goal"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateBudgetSettings(dailyBudget: Double, currency: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.saveUserProfile(
                    UserProfile(
                        userId = userId,
                        dailyBudget = dailyBudget,
                        currencySymbol = currency
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to save budget settings"
            } finally {
                _isLoading.value = false
            }
        }
    }

    class Factory(
        private val repository: BudgetRepository,
        private val localTransactionRepository: LocalTransactionRepository? = null,
        private val userId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BudgetViewModel(repository, localTransactionRepository, userId) as T
        }
    }
}
