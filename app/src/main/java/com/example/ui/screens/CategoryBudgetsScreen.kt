package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.Transaction
import com.example.util.CategoryBudgetPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val budgetCategories = listOf(
    "Groceries", "Food & Dining", "Transportation", "Bills & Utilities",
    "Entertainment", "Health", "Shopping", "Education", "Other"
)

@Composable
fun CategoryBudgetsScreen(
    transactions: List<Transaction>,
    userId: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val month = remember { SimpleDateFormat("yyyy-MM", Locale.ROOT).format(Date()) }
    val monthLabel = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }
    val categories = remember(transactions) {
        (budgetCategories + transactions.map { it.category.trim() }.filter { it.isNotBlank() }).distinct().sorted()
    }
    val spentByCategory = remember(transactions, month) {
        transactions.filter { it.type.equals("expense", true) && it.date.startsWith(month) }
            .groupBy { it.category }.mapValues { (_, txs) -> txs.sumOf { it.amount.coerceAtLeast(0.0) } }
    }
    var refresh by remember(userId) { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<String?>(null) }
    var amountText by remember { mutableStateOf("") }
    val limits = remember(userId, categories, refresh) {
        categories.associateWith { CategoryBudgetPreferences.getBudget(context, userId, it) }
    }
    val totalSpent = spentByCategory.values.sum()
    val totalLimit = limits.values.sum()

    editing?.let { category ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Set $category budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Monthly spending limit in South African Rand.")
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.length <= 14 && it.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) amountText = it },
                        label = { Text("Monthly limit") },
                        prefix = { Text("R") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    TextButton(onClick = {
                        CategoryBudgetPreferences.clearBudget(context, userId, category)
                        refresh++
                        editing = null
                    }) { Text("Remove limit") }
                }
            },
            confirmButton = {
                Button(
                    enabled = amountText.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true,
                    onClick = {
                        amountText.toDoubleOrNull()?.let {
                            CategoryBudgetPreferences.setBudget(context, userId, category, it)
                            refresh++
                            editing = null
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Savings, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Your monthly plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text(monthLabel, style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Spent", style = MaterialTheme.typography.labelMedium)
                            Text(rand(totalSpent), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Category limits", style = MaterialTheme.typography.labelMedium)
                            Text(if (totalLimit > 0) rand(totalLimit) else "Not set",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (totalLimit > 0) {
                        LinearProgressIndicator(
                            progress = { (totalSpent / totalLimit).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            if (totalSpent > totalLimit) rand(totalSpent - totalLimit) + " over limits"
                            else rand(totalLimit - totalSpent) + " remaining across limits",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else Text("Set category limits below to track your progress.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Text("Category limits", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Only expenses from $monthLabel count. Limits are saved locally for this signed-in account.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(categories, key = { it }) { category ->
            val spent = spentByCategory[category] ?: 0.0
            val limit = limits[category] ?: 0.0
            val over = limit > 0 && spent > limit
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(category, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (limit > 0) rand(spent) + " of " + rand(limit) else rand(spent) + " spent · no limit set",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            amountText = if (limit > 0) "%.2f".format(Locale.US, limit) else ""
                            editing = category
                        }) { Icon(Icons.Default.Edit, contentDescription = "Edit $category budget") }
                    }
                    if (limit > 0) {
                        LinearProgressIndicator(
                            progress = { (spent / limit).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            if (over) rand(spent - limit) + " over limit" else rand(limit - spent) + " remaining",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            Text("You can change limits at any time without changing your transaction history.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun rand(value: Double): String = String.format(Locale.US, "R%,.2f", value)
