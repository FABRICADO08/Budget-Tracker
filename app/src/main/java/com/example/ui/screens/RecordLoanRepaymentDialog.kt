package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.Loan
import com.example.data.local.repository.FinanceRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordLoanRepaymentDialog(
    database: AppDatabase,
    loan: Loan,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var amount by remember(loan.id) { mutableStateOf("") }
    var date by remember(loan.id) { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())) }
    var note by remember(loan.id) { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val repository = remember(database) { FinanceRepository(database) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Record repayment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Record a full or partial repayment from ${loan.borrowerName}.")
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.length <= 12 && it.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) amount = it },
                    label = { Text("Amount received") },
                    prefix = { Text("R") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it.take(10) },
                    label = { Text("Date received (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(160) },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving && amount.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true && date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")),
                onClick = {
                    saving = true
                    scope.launch {
                        runCatching {
                            repository.recordLoanRepayment(loan.id, amount.toDouble(), date, note)
                        }.onSuccess {
                            onSaved()
                        }.onFailure {
                            error = it.message ?: "Could not record repayment."
                            saving = false
                        }
                    }
                }
            ) { Text(if (saving) "Saving…" else "Save repayment") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } }
    )
}
