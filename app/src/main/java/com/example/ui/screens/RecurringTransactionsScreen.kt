package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.RecurringOccurrence
import com.example.data.local.entity.RecurringTransaction
import com.example.data.local.repository.FinanceRepository
import com.example.domain.finance.RecurringDates
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsScreen(
    database: AppDatabase,
    modifier: Modifier = Modifier
) {
    val repository = remember(database) { FinanceRepository(database) }
    val schedules by repository.observeRecurringSchedules().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showEditor by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Bills & Utilities") }
    var frequency by remember { mutableStateOf("MONTHLY") }
    var dueDate by remember { mutableStateOf(todayIso()) }
    var reminderDays by remember { mutableStateOf("1") }
    var type by remember { mutableStateOf("expense") }
    var categoryMenu by remember { mutableStateOf(false) }
    var frequencyMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var workingId by remember { mutableStateOf<Long?>(null) }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false; error = null },
            title = { Text("Add recurring bill") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Create a schedule for rent, subscriptions, insurance or regular income.")
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it.take(70) },
                        label = { Text("Name") },
                        placeholder = { Text("e.g. Monthly rent") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.length <= 12 && it.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) amount = it },
                        label = { Text("Amount (ZAR)") },
                        prefix = { Text("R") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(expanded = categoryMenu, onExpandedChange = { categoryMenu = it }) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        androidx.compose.material3.ExposedDropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                            listOf("Bills & Utilities", "Housing", "Groceries", "Transportation", "Entertainment", "Health", "Income", "Other").forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { category = it; categoryMenu = false })
                            }
                        }
                    }
                    ExposedDropdownMenuBox(expanded = frequencyMenu, onExpandedChange = { frequencyMenu = it }) {
                        OutlinedTextField(
                            value = frequency.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Repeat") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        androidx.compose.material3.ExposedDropdownMenu(expanded = frequencyMenu, onDismissRequest = { frequencyMenu = false }) {
                            listOf("WEEKLY", "BIWEEKLY", "MONTHLY", "QUARTERLY", "ANNUALLY").forEach {
                                DropdownMenuItem(text = { Text(it.lowercase().replaceFirstChar { c -> c.uppercase() }) }, onClick = { frequency = it; frequencyMenu = false })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = reminderDays,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) reminderDays = it },
                        label = { Text("Remind me days before (0–30)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it.take(10) },
                        label = { Text("First due date (yyyy-MM-dd)") },
                        supportingText = { Text("Use ISO date format, for example 2026-11-01.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { type = "expense" }, enabled = type != "expense") { Text("Expense") }
                        TextButton(onClick = { type = "income" }, enabled = type != "income") { Text("Income") }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                Button(
                    enabled = description.isNotBlank() && amount.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true && isIsoDate(dueDate),
                    onClick = {
                        scope.launch {
                            try {
                                val id = repository.createRecurringSchedule(
                                    description = description,
                                    amount = amount.toDouble(),
                                    category = category,
                                    type = type,
                                    startDate = dueDate,
                                    nextDueDate = dueDate,
                                    frequency = frequency,
                                    reminderDaysBefore = reminderDays.toIntOrNull() ?: 1
                                )
                                database.recurringTransactionDao().insertOccurrenceIfNew(
                                    RecurringOccurrence(recurringId = id, dueDate = dueDate, amount = amount.toDouble())
                                )
                                showEditor = false
                                description = ""
                                amount = ""
                                error = null
                            } catch (exception: Exception) {
                                error = exception.message ?: "Could not save this schedule."
                            }
                        }
                    }
                ) { Text("Save schedule") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false; error = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { showEditor = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Add recurring bill", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Bills & recurring money", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Text("Plan predictable costs, keep due dates visible, and record each payment once.", style = MaterialTheme.typography.bodyMedium)
                        Text("${schedules.count { it.isActive }} active schedules", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (schedules.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Nothing scheduled yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Add rent, subscriptions, insurance or a recurring income source to start tracking due dates.", style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = { showEditor = true }) { Text("Add first schedule") }
                        }
                    }
                }
            }
            items(schedules, key = { it.id }) { schedule ->
                RecurringScheduleCard(
                    schedule = schedule,
                    isWorking = workingId == schedule.id,
                    onPay = {
                        workingId = schedule.id
                        scope.launch {
                            try {
                                val dao = database.recurringTransactionDao()
                                val occurrence = dao.getOccurrence(schedule.id, schedule.nextDueDate)
                                val occurrenceId = occurrence?.id ?: dao.insertOccurrenceIfNew(
                                    RecurringOccurrence(recurringId = schedule.id, dueDate = schedule.nextDueDate, amount = schedule.amount)
                                )
                                val actualOccurrence = if (occurrenceId > 0) occurrenceId else dao.getOccurrence(schedule.id, schedule.nextDueDate)?.id ?: 0L
                                if (actualOccurrence > 0 && repository.markRecurringBillPaid(actualOccurrence)) {
                                    val next = RecurringDates.nextDueDate(schedule.nextDueDate, RecurringDates.Frequency.valueOf(schedule.frequency))
                                    val active = schedule.endDate == null || next <= schedule.endDate
                                    dao.update(schedule.copy(nextDueDate = next, isActive = active))
                                    if (active) dao.insertOccurrenceIfNew(RecurringOccurrence(recurringId = schedule.id, dueDate = next, amount = schedule.amount))
                                }
                            } finally {
                                workingId = null
                            }
                        }
                    },
                    onDelete = { scope.launch { database.recurringTransactionDao().delete(schedule) } }
                )
            }
        }
    }
}

@Composable
private fun RecurringScheduleCard(
    schedule: RecurringTransaction,
    isWorking: Boolean,
    onPay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(schedule.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${schedule.category} · ${schedule.frequency.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(money(schedule.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete ${schedule.description}") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Next due: ${schedule.nextDueDate}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                val dueState = when {
                    !schedule.isActive -> "Ended"
                    schedule.nextDueDate < todayIso() -> "Overdue"
                    schedule.nextDueDate <= datePlusDaysIso(schedule.reminderDaysBefore) -> "Due soon"
                    else -> "Upcoming"
                }
                Text(
                    dueState,
                    color = when (dueState) {
                        "Overdue" -> MaterialTheme.colorScheme.error
                        "Due soon" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Button(onClick = onPay, enabled = schedule.isActive && !isWorking, modifier = Modifier.fillMaxWidth()) {
                Text(if (isWorking) "Recording…" else if (schedule.type == "income") "Record income" else "Record payment")
            }
        }
    }
}

private fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
private fun isIsoDate(value: String): Boolean = value.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }.parse(value) }.getOrNull() != null
private fun money(value: Double): String = String.format(Locale.US, "R%,.2f", value)

private fun datePlusDaysIso(days: Int): String {
    val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"), Locale.ROOT)
    calendar.add(java.util.Calendar.DAY_OF_MONTH, days.coerceIn(0, 30))
    return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(calendar.time)
}
