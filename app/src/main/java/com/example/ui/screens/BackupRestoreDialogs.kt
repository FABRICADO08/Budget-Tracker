package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.backup.BackupRestoreMode
import com.example.data.backup.FinanceBackupPreview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreDialog(
    busy: Boolean,
    error: String?,
    onCreateBackup: () -> Unit,
    onSelectBackup: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup & restore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Create a versioned JSON file or restore one through Android's file picker. " +
                        "The backup includes this account's Room transactions, category budgets and alerts, recurring schedules and occurrence history, loans and repayments, monthly budget cap, Firestore expenses, savings goals, and profile settings."
                )
                Text(
                    "This file contains sensitive financial information. Store it somewhere private. Authentication tokens, passwords and API keys are never exported.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator()
                        Text("Preparing secure backup operation…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(
                    onClick = onCreateBackup,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Create JSON backup") }
                OutlinedButton(
                    onClick = onSelectBackup,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Select backup to restore") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Done") } }
    )
}

@Composable
fun BackupRestoreConfirmationDialog(
    preview: FinanceBackupPreview,
    currentUserId: String,
    mode: BackupRestoreMode,
    busy: Boolean,
    error: String?,
    onModeChange: (BackupRestoreMode) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("en", "ZA"))
        .format(Date(preview.createdAtMillis))
    AlertDialog(
        onDismissRequest = { if (!busy) onCancel() },
        title = { Text("Review backup before restoring") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Created: $date")
                Text("Backup source account: ${preview.sourceUserId}", style = MaterialTheme.typography.bodySmall)
                Text("Restore target account: $currentUserId", style = MaterialTheme.typography.bodySmall)
                if (preview.sourceUserId != currentUserId) {
                    Text(
                        "This file comes from a different account. Restoring copies its backup data into the signed-in account shown above; it does not change the source account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Text(
                    "Records in file: ${preview.transactionCount} transactions, ${preview.loanCount} loans, " +
                        "${preview.budgetCount} category budgets, ${preview.budgetAlertCount} budget alerts, " +
                        "${preview.recurringScheduleCount} recurring schedules, ${preview.recurringOccurrenceCount} occurrences, " +
                        "${preview.repaymentCount} repayments, ${preview.cloudExpenseCount} cloud expenses, " +
                        "${preview.savingsGoalCount} savings goals.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Choose how to apply it:", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = mode == BackupRestoreMode.MERGE,
                        onClick = { if (!busy) onModeChange(BackupRestoreMode.MERGE) },
                        enabled = !busy
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Merge without duplicates", fontWeight = FontWeight.Medium)
                        Text("Keep existing records and settings; add backup records not already present.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = mode == BackupRestoreMode.REPLACE,
                        onClick = { if (!busy) onModeChange(BackupRestoreMode.REPLACE) },
                        enabled = !busy
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Replace this account's records", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                        Text(
                            "Replace current local finance records and this account's cloud expenses, savings goals and profile settings. Other accounts are not changed.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text(
                    "The file is validated before changes. Room restoration runs in one database transaction; the app also keeps a cloud snapshot to attempt rollback if a restore step fails.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator()
                        Text("Restoring data…")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !busy
            ) {
                Text(if (mode == BackupRestoreMode.REPLACE) "Restore & replace" else "Merge backup")
            }
        },
        dismissButton = { TextButton(onClick = onCancel, enabled = !busy) { Text("Cancel") } }
    )
}
