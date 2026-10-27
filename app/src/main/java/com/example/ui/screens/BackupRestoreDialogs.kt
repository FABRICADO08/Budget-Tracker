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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.data.backup.BackupRestoreMode
import com.example.data.backup.FinanceBackupCryptography
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
                    "Create a password-encrypted, versioned JSON file or restore one through Android's file picker. " +
                        "The backup includes this account's Room transactions, category budgets and alerts, recurring schedules and occurrence history, loans and repayments, monthly budget cap, Firestore expenses, savings goals, and profile settings."
                )
                Text(
                    "New exports are encrypted before they leave the app. Keep the backup password safe; it cannot be recovered. Older unencrypted backups can still be restored. Authentication tokens, passwords and API keys are never exported.",
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
                ) { Text("Create encrypted backup") }
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


enum class BackupPasswordMode { CREATE, RESTORE }

@Composable
fun BackupPasswordDialog(
    mode: BackupPasswordMode,
    busy: Boolean,
    error: String?,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember(mode) { mutableStateOf("") }
    var confirmation by remember(mode) { mutableStateOf("") }
    val passwordLongEnough = password.length >= FinanceBackupCryptography.MIN_PASSWORD_LENGTH
    val matches = mode == BackupPasswordMode.RESTORE || password == confirmation
    val canSubmit = !busy && passwordLongEnough && matches

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Text(if (mode == BackupPasswordMode.CREATE) "Protect your backup" else "Unlock backup")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (mode == BackupPasswordMode.CREATE) {
                        "Choose a password with at least ${FinanceBackupCryptography.MIN_PASSWORD_LENGTH} characters. Financial data will be encrypted with AES-GCM before the file is saved."
                    } else {
                        "Enter the password that was used when this encrypted backup was created. Your data is not changed until the decrypted file passes validation and you confirm the restore."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    label = { Text("Backup password") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                if (mode == BackupPasswordMode.CREATE) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        label = { Text("Confirm password") },
                        singleLine = true,
                        enabled = !busy,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = confirmation.isNotEmpty() && !matches
                    )
                    Text(
                        "The app does not store or recover this password. Keep it separately from the backup file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (password.isNotEmpty() && !passwordLongEnough) {
                        Text(
                            "Use at least ${FinanceBackupCryptography.MIN_PASSWORD_LENGTH} characters.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (confirmation.isNotEmpty() && !matches) {
                        Text("The passwords do not match.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (busy) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(if (mode == BackupPasswordMode.CREATE) "Encrypting backup…" else "Decrypting and validating…")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val passwordChars = password.toCharArray()
                    password = ""
                    confirmation = ""
                    onConfirm(passwordChars)
                },
                enabled = canSubmit
            ) {
                Text(if (mode == BackupPasswordMode.CREATE) "Encrypt backup" else "Unlock backup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
        }
    )
}
