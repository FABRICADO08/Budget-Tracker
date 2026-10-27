package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.importer.BankCsvParser
import com.example.data.local.entity.Transaction
import com.example.util.ZarMoneyFormatter
import java.text.SimpleDateFormat
import java.util.Locale

private data class CsvImportCandidate(
    val lineNumber: Int,
    val transaction: Transaction,
    val balance: Double?
)

private data class CsvImportPreviewState(
    val candidates: List<CsvImportCandidate> = emptyList(),
    val invalidRowCount: Int = 0,
    val duplicateRowCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankCsvImportDialog(
    csvText: String,
    existingTransactions: List<Transaction>,
    onDismiss: () -> Unit,
    onImport: (List<Transaction>) -> Unit
) {
    val parsedResult = remember(csvText) { runCatching { BankCsvParser.parse(csvText) } }
    val parsed = parsedResult.getOrNull()
    var dateColumn by remember(csvText, parsed) {
        mutableStateOf(findColumn(parsed?.headers.orEmpty(), listOf("date", "transaction date", "posted")))
    }
    var descriptionColumn by remember(csvText, parsed) {
        mutableStateOf(findColumn(parsed?.headers.orEmpty(), listOf("description", "details", "merchant", "payee", "narration")))
    }
    var amountColumn by remember(csvText, parsed) {
        mutableStateOf(findColumn(parsed?.headers.orEmpty(), listOf("amount", "value")))
    }
    var debitColumn by remember(csvText, parsed) {
        mutableStateOf(findColumn(parsed?.headers.orEmpty(), listOf("debit", "withdrawal", "withdrawals", "money out", "paid out")))
    }
    var creditColumn by remember(csvText, parsed) {
        mutableStateOf(findColumn(parsed?.headers.orEmpty(), listOf("credit", "deposit", "deposits", "money in", "paid in")))
    }
    var balanceColumn by remember(csvText, parsed) {
        mutableStateOf(findColumn(parsed?.headers.orEmpty(), listOf("running balance", "available balance", "balance")))
    }
    var categoryColumn by remember(csvText, parsed) {
        mutableStateOf(findColumn(parsed?.headers.orEmpty(), listOf("category", "type of spend")))
    }
    var typeColumn by remember(csvText, parsed) {
        mutableStateOf(findColumn(parsed?.headers.orEmpty(), listOf("transaction type", "income expense", "direction", "type")))
    }
    var decimalComma by remember(csvText) { mutableStateOf(false) }
    var expandedField by remember { mutableStateOf<String?>(null) }
    var errorText by remember(csvText) { mutableStateOf(parsedResult.exceptionOrNull()?.message) }
    var excludedFingerprints by remember(csvText, parsed) { mutableStateOf<Set<String>>(emptySet()) }

    val usesDebitCreditColumns = debitColumn >= 0 || creditColumn >= 0
    val mappingsAreValid = dateColumn >= 0 && descriptionColumn >= 0 &&
        (if (usesDebitCreditColumns) true else amountColumn >= 0)

    val previewState = remember(
        parsed, dateColumn, descriptionColumn, amountColumn, debitColumn, creditColumn, balanceColumn,
        categoryColumn, typeColumn, decimalComma, existingTransactions, mappingsAreValid
    ) {
        if (parsed == null || !mappingsAreValid) {
            CsvImportPreviewState()
        } else {
            val mappedRows = parsed.rows.map { row ->
                val rawDate = row.values.getOrNull(dateColumn).orEmpty().trim()
                val rawDescription = row.values.getOrNull(descriptionColumn).orEmpty().trim()
                val normalizedDate = normalizeDate(rawDate)
                val movement = BankCsvParser.parseMoneyMovement(
                    amountRaw = if (usesDebitCreditColumns) null else row.values.getOrNull(amountColumn).orEmpty(),
                    debitRaw = if (usesDebitCreditColumns && debitColumn >= 0) row.values.getOrNull(debitColumn).orEmpty() else null,
                    creditRaw = if (usesDebitCreditColumns && creditColumn >= 0) row.values.getOrNull(creditColumn).orEmpty() else null,
                    decimalComma = decimalComma
                )
                val category = row.values.getOrNull(categoryColumn).orEmpty().trim().ifBlank { "Other" }
                val rawType = row.values.getOrNull(typeColumn).orEmpty().trim().lowercase(Locale.ROOT)
                val amountHeader = parsed.headers.getOrNull(amountColumn).orEmpty().lowercase(Locale.ROOT)
                val movementType = if (usesDebitCreditColumns) movement?.type else when {
                    rawType.contains("income") || rawType.contains("credit") || rawType.contains("deposit") -> "income"
                    rawType.contains("expense") || rawType.contains("debit") || rawType.contains("withdraw") -> "expense"
                    amountHeader.contains("debit") -> "expense"
                    amountHeader.contains("credit") -> "income"
                    else -> movement?.type
                }

                val transaction = if (
                    normalizedDate != null && rawDescription.isNotBlank() && movement != null && movementType != null
                ) {
                    Transaction(
                        amount = movement.amount,
                        category = category,
                        date = normalizedDate,
                        description = rawDescription,
                        type = movementType
                    )
                } else {
                    null
                }
                row.lineNumber to (transaction to row.values.getOrNull(balanceColumn).orEmpty().takeIf { balanceColumn >= 0 && it.isNotBlank() }?.let {
                    BankCsvParser.parseAmount(it, decimalComma)
                })
            }

            val validRows = mappedRows.mapNotNull { (lineNumber, result) ->
                val transaction = result.first
                transaction?.let { CsvImportCandidate(lineNumber, it, result.second) }
            }
            val uniqueRows = validRows.distinctBy { fingerprint(it.transaction) }
            val withoutExistingDuplicates = uniqueRows.filterNot { candidate ->
                existingTransactions.any { existing -> isProbableDuplicate(existing, candidate.transaction) }
            }
            CsvImportPreviewState(
                candidates = withoutExistingDuplicates,
                invalidRowCount = mappedRows.count { it.second == null },
                duplicateRowCount = mappedRows.count { it.second != null } - withoutExistingDuplicates.size
            )
        }
    }

    val readyToImport = previewState.candidates.filterNot {
        fingerprint(it.transaction) in excludedFingerprints
    }
    val canImport = parsed != null && mappingsAreValid && readyToImport.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Import bank statement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Check the column mapping, review the proposed transactions and exclude any rows you do not want. Nothing is saved until you confirm the import.")
                if (parsed == null) {
                    Text(errorText ?: "This CSV could not be read.", color = MaterialTheme.colorScheme.error)
                } else {
                    Text(
                        "${parsed.rows.size} statement rows · ${readyToImport.size} selected to import · " +
                            "${previewState.invalidRowCount} invalid · ${previewState.duplicateRowCount} probable duplicates skipped",
                        style = MaterialTheme.typography.labelLarge
                    )
                    MappingDropdown("Date column", parsed.headers, dateColumn, "date", expandedField, { expandedField = it }) { dateColumn = it }
                    MappingDropdown("Description column", parsed.headers, descriptionColumn, "description", expandedField, { expandedField = it }) { descriptionColumn = it }

                    Text("Choose one amount mapping method:", style = MaterialTheme.typography.labelLarge)
                    MappingDropdown("Single signed amount (optional)", parsed.headers, amountColumn, "amount", expandedField, { expandedField = it }) { amountColumn = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            MappingDropdown("Debit column (optional)", parsed.headers, debitColumn, "debit", expandedField, { expandedField = it }) { debitColumn = it }
                        }
                        Column(Modifier.weight(1f)) {
                            MappingDropdown("Credit column (optional)", parsed.headers, creditColumn, "credit", expandedField, { expandedField = it }) { creditColumn = it }
                    MappingDropdown("Balance column (optional)", parsed.headers, balanceColumn, "balance", expandedField, { expandedField = it }) { balanceColumn = it }
                        }
                    }
                    MappingDropdown("Category column (optional)", parsed.headers, categoryColumn, "category", expandedField, { expandedField = it }) { categoryColumn = it }
                    MappingDropdown("Type column (optional)", parsed.headers, typeColumn, "type", expandedField, { expandedField = it }) { typeColumn = it }
                    TextButton(onClick = { decimalComma = !decimalComma }) {
                        Text(if (decimalComma) "Amount format: decimal comma ✓" else "Amount format: decimal point · tap to change")
                    }
                    if (dateColumn < 0 || descriptionColumn < 0 || (!usesDebitCreditColumns && amountColumn < 0)) {
                        Text(
                            "Map a date, description and either a signed amount or at least one debit/credit column.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("Review transactions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).padding(top = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(previewState.candidates, key = { fingerprint(it.transaction) }) { candidate ->
                            val key = fingerprint(candidate.transaction)
                            val selected = key !in excludedFingerprints
                            androidx.compose.material3.Card(shape = RoundedCornerShape(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selected,
                                        onCheckedChange = { checked ->
                                            excludedFingerprints = if (checked) {
                                                excludedFingerprints - key
                                            } else {
                                                excludedFingerprints + key
                                            }
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(candidate.transaction.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(
                                            buildString {
                                                append("Row ${candidate.lineNumber} · ${candidate.transaction.date} · ${candidate.transaction.category} · ${candidate.transaction.type}")
                                                candidate.balance?.let { append(" · Balance ${ZarMoneyFormatter.format(it)}") }
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        ZarMoneyFormatter.format(candidate.transaction.amount),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                    if (previewState.candidates.isEmpty()) {
                        Text(
                            "No importable rows. Check the column mappings, dates and amount format.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    errorText?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = canImport, onClick = { onImport(readyToImport.map { it.transaction }) }) {
                Text("Import ${readyToImport.size} rows")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MappingDropdown(
    label: String,
    headers: List<String>,
    selected: Int,
    field: String,
    expandedField: String?,
    onExpanded: (String?) -> Unit,
    onSelect: (Int) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expandedField == field,
        onExpandedChange = { onExpanded(if (it) field else null) }
    ) {
        OutlinedTextField(
            value = if (selected >= 0) headers.getOrNull(selected).orEmpty() else "Not mapped",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedField == field) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expandedField == field, onDismissRequest = { onExpanded(null) }) {
            DropdownMenuItem(text = { Text("Not mapped") }, onClick = { onSelect(-1); onExpanded(null) })
            headers.forEachIndexed { index, header ->
                DropdownMenuItem(
                    text = { Text(header.ifBlank { "Column ${index + 1}" }) },
                    onClick = { onSelect(index); onExpanded(null) }
                )
            }
        }
    }
}

private fun findColumn(headers: List<String>, candidates: List<String>): Int =
    headers.indexOfFirst { header ->
        candidates.any { header.trim().equals(it, ignoreCase = true) || header.trim().contains(it, ignoreCase = true) }
    }

private fun normalizeDate(raw: String): String? {
    val formats = listOf("yyyy-MM-dd", "dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy", "d-M-yyyy", "MM/dd/yyyy")
    formats.forEach { pattern ->
        val formatter = SimpleDateFormat(pattern, Locale.ROOT).apply { isLenient = false }
        runCatching { formatter.parse(raw) }.getOrNull()?.let { parsed ->
            return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(parsed)
        }
    }
    return null
}

private fun fingerprint(transaction: Transaction): String =
    "${transaction.date.lowercase(Locale.ROOT)}|${transaction.amount}|${transaction.type.lowercase(Locale.ROOT)}|${transaction.description.trim().lowercase(Locale.ROOT)}|${transaction.category.trim().lowercase(Locale.ROOT)}"

private fun isProbableDuplicate(existing: Transaction, candidate: Transaction): Boolean =
    existing.date == candidate.date &&
        existing.type.equals(candidate.type, ignoreCase = true) &&
        existing.category.equals(candidate.category, ignoreCase = true) &&
        kotlin.math.abs(existing.amount - candidate.amount) < 0.005 &&
        existing.description.trim().equals(candidate.description.trim(), ignoreCase = true)
