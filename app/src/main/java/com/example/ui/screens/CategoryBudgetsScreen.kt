package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.CategoryBudget
import com.example.data.local.entity.Transaction
import com.example.data.local.repository.FinanceRepository
import com.example.domain.finance.BudgetRules
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CategoryBudgetsScreen(
    database: AppDatabase,
    transactions: List<Transaction>
) {
    val defaultMonth = remember {
        SimpleDateFormat("yyyy-MM", Locale.ROOT).format(Date())
    }
    var month by remember { mutableStateOf(defaultMonth) }
    var showEditor by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<CategoryBudget?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var carryoverAmounts by remember { mutableStateOf<Map<Long, Double>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    val repository = remember(database) { FinanceRepository(database) }
    val budgetFlow = remember(database, month) { database.categoryBudgetDao().observeForMonth(month) }
    val budgets by budgetFlow.collectAsState(initial = emptyList())
    val money = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA")) }

    LaunchedEffect(budgets, transactions, month) {
        loadError = null
        val previousMonth = runCatching {
            val parser = SimpleDateFormat("yyyy-MM", Locale.ROOT)
            val calendar = Calendar.getInstance().apply { time = requireNotNull(parser.parse(month)) }
            calendar.add(Calendar.MONTH, -1)
            parser.format(calendar.time)
        }.getOrNull()
        val carryovers = mutableMapOf<Long, Double>()
        budgets.forEach { budget ->
            val previous = previousMonth?.let { database.categoryBudgetDao().find(budget.category, it) }
            if (previous?.carryUnused == true) {
                val previousSpent = transactions.asSequence()
                    .filter { it.type.equals("expense", ignoreCase = true) }
                    .filter { it.date.startsWith(previous.month) && it.category.equals(previous.category, ignoreCase = true) }
                    .sumOf { it.amount.coerceAtLeast(0.0) }
                carryovers[budget.id] = (previous.limitAmount - previousSpent).coerceAtLeast(0.0)
            }
        }
        carryoverAmounts = carryovers
        budgets.filterNot { it.isPaused }.forEach { budget ->
            val spent = transactions.asSequence()
                .filter { it.type.equals("expense", ignoreCase = true) }
                .filter { it.date.startsWith(month) && it.category.equals(budget.category, ignoreCase = true) }
                .sumOf { it.amount.coerceAtLeast(0.0) }
            val effectiveLimit = budget.limitAmount + (carryovers[budget.id] ?: 0.0)
            runCatching { repository.recordCrossedBudgetAlerts(budget, spent, effectiveLimit) }
                .onFailure { loadError = it.message ?: "Could not save budget alert." }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingBudget = null
                showEditor = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add category budget")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Category budgets", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Set monthly limits and track real expenses against each category.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = month,
                onValueChange = { if (it.length <= 7) month = it },
                label = { Text("Month (yyyy-MM)") },
                supportingText = { Text("Use the format 2026-10") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (loadError != null) {
                Text(loadError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (!month.matches(Regex("\\d{4}-\\d{2}"))) {
                Text("Enter a valid month before adding budgets.", color = MaterialTheme.colorScheme.error)
            } else if (budgets.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No budgets for $month", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text("Add a category limit to start tracking your spending.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(budgets, key = { it.id }) { budget ->
                        val spent = transactions
                            .asSequence()
                            .filter { it.type.equals("expense", ignoreCase = true) }
                            .filter { it.date.startsWith(month) && it.category.equals(budget.category, ignoreCase = true) }
                            .sumOf { it.amount.coerceAtLeast(0.0) }
                        val effectiveLimit = budget.limitAmount + (carryoverAmounts[budget.id] ?: 0.0)
                        val progress = BudgetRules.progress(effectiveLimit, listOf("expense" to spent))
                        val percent = (progress.percentUsed / 100.0).toFloat().coerceIn(0f, 1f)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(budget.category, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (budget.isPaused) "Paused" else "${money.format(spent)} spent of ${money.format(effectiveLimit)}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    IconButton(onClick = { editingBudget = budget; showEditor = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit ${budget.category} budget")
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            runCatching { repository.deleteBudget(budget) }
                                                .onFailure { loadError = it.message ?: "Could not delete budget." }
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete ${budget.category} budget")
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { percent },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = when {
                                        progress.exceeded -> MaterialTheme.colorScheme.error
                                        progress.percentUsed >= 90 -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${progress.percentUsed.toInt()}% used",
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (progress.exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (progress.exceeded) "${money.format(spent - budget.limitAmount)} over budget"
                                        else "${money.format(progress.remaining)} remaining",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text("Pause", style = MaterialTheme.typography.labelSmall)
                                    Switch(
                                        checked = budget.isPaused,
                                        onCheckedChange = { paused ->
                                            scope.launch {
                                                runCatching { database.categoryBudgetDao().update(budget.copy(isPaused = paused)) }
                                                    .onFailure { loadError = it.message ?: "Could not update budget." }
                                            }
                                        }
                                    )
                                }
                                val crossed = BudgetRules.crossedThresholds(effectiveLimit, spent)
                                if (!budget.isPaused && crossed.isNotEmpty()) {
                                    Text(
                                        "Threshold reached: ${crossed.joinToString(", ") { "$it%" }}%",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        BudgetEditorDialog(
            month = month,
            initial = editingBudget,
            onDismiss = { showEditor = false },
            onSave = { category, limit, selectedMonth, carryUnused ->
                scope.launch {
                    runCatching {
                        repository.saveBudget(
                            (editingBudget ?: CategoryBudget(
                                category = category,
                                month = selectedMonth,
                                limitAmount = limit,
                                carryUnused = carryUnused
                            )).copy(
                                category = category.trim(),
                                month = selectedMonth,
                                limitAmount = limit,
                                carryUnused = carryUnused
                            )
                        )
                    }.onFailure { loadError = it.message ?: "Could not save budget." }
                    showEditor = false
                }
            }
        )
    }
}

@Composable
private fun BudgetEditorDialog(
    month: String,
    initial: CategoryBudget?,
    onDismiss: () -> Unit,
    onSave: (category: String, limit: Double, month: String, carryUnused: Boolean) -> Unit
) {
    var category by remember(initial) { mutableStateOf(initial?.category ?: "") }
    var limit by remember(initial) { mutableStateOf(initial?.limitAmount?.toString() ?: "") }
    var selectedMonth by remember(initial, month) { mutableStateOf(initial?.month ?: month) }
    var carryUnused by remember(initial) { mutableStateOf(initial?.carryUnused ?: false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New category budget" else "Edit category budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(category, { category = it; error = null }, label = { Text("Category") }, singleLine = true)
                OutlinedTextField(
                    limit,
                    { limit = it; error = null },
                    label = { Text("Monthly limit (R)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(selectedMonth, { selectedMonth = it; error = null }, label = { Text("Month (yyyy-MM)") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = carryUnused, onCheckedChange = { carryUnused = it })
                    Text("Carry unused amount forward")
                }
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = limit.trim().replace(',', '.').toDoubleOrNull()
                when {
                    category.isBlank() -> error = "Enter a category."
                    amount == null || !amount.isFinite() || amount <= 0.0 -> error = "Enter a limit greater than zero."
                    !selectedMonth.matches(Regex("\\d{4}-\\d{2}")) -> error = "Month must use yyyy-MM."
                    else -> onSave(category.trim(), amount, selectedMonth, carryUnused)
                }
            }) { Text("Save budget") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
