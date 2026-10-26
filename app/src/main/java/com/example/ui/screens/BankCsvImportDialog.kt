package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.importer.BankCsvParser
import com.example.data.local.entity.Transaction
import java.text.SimpleDateFormat
import java.util.Locale

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
    var dateColumn by remember(csvText, parsed) { mutableStateOf(findColumn(parsed?.headers.orEmpty(), listOf("date", "transaction date", "posted"))) }
    var descriptionColumn by remember(csvText, parsed) { mutableStateOf(findColumn(parsed?.headers.orEmpty(), listOf("description", "details", "merchant", "payee", "narration"))) }
    var amountColumn by remember(csvText, parsed) { mutableStateOf(findColumn(parsed?.headers.orEmpty(), listOf("amount", "value", "debit", "credit"))) }
    var categoryColumn by remember(csvText, parsed) { mutableStateOf(findColumn(parsed?.headers.orEmpty(), listOf("category", "type of spend"))) }
    var decimalComma by remember(csvText) { mutableStateOf(false) }
    var expandedField by remember { mutableStateOf<String?>(null) }
    var errorText by remember(csvText) { mutableStateOf(parsedResult.exceptionOrNull()?.message) }

    val preview = remember(parsed, dateColumn, descriptionColumn, amountColumn, categoryColumn, decimalComma, existingTransactions) {
        if (parsed == null || dateColumn < 0 || amountColumn < 0) emptyList()
        else parsed.rows.mapNotNull { row ->
            val rawDate = row.values.getOrNull(dateColumn).orEmpty().trim()
            val rawDescription = row.values.getOrNull(descriptionColumn).orEmpty().trim().ifBlank { "Imported transaction" }
            val rawAmount = row.values.getOrNull(amountColumn).orEmpty()
            val amount = BankCsvParser.parseAmount(rawAmount, decimalComma)?.takeIf { it != 0.0 } ?: return@mapNotNull null
            val date = normalizeDate(rawDate) ?: return@mapNotNull null
            val category = row.values.getOrNull(categoryColumn).orEmpty().trim().ifBlank { "Other" }
            val type = if (amount < 0.0) "expense" else "expense"
            Transaction(
                amount = kotlin.math.abs(amount),
                category = category,
                date = date,
                description = rawDescription,
                type = type
            )
        }.distinctBy { fingerprint(it) }
            .filterNot { candidate ->
                existingTransactions.any { existing ->
                    existing.date == candidate.date &&
                        kotlin.math.abs(existing.amount - candidate.amount) < 0.005 &&
                        existing.description.trim().equals(candidate.description.trim(), ignoreCase = true)
                }
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import bank statement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Review the column mapping and preview the rows before anything is added. Matching transactions are skipped.")
                if (parsed == null) {
                    Text(errorText ?: "This CSV could not be read.", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("${parsed.rows.size} rows found · ${preview.size} new transactions ready", style = MaterialTheme.typography.labelLarge)
                    MappingDropdown("Date column", parsed.headers, dateColumn, "date", expandedField, { expandedField = it }) { dateColumn = it }
                    MappingDropdown("Description column", parsed.headers, descriptionColumn, "description", expandedField, { expandedField = it }) { descriptionColumn = it }
                    MappingDropdown("Amount column", parsed.headers, amountColumn, "amount", expandedField, { expandedField = it }) { amountColumn = it }
                    MappingDropdown("Category column (optional)", parsed.headers, categoryColumn, "category", expandedField, { expandedField = it }) { categoryColumn = it }
                    TextButton(onClick = { decimalComma = !decimalComma }) {
                        Text(if (decimalComma) "Amount format: decimal comma ✓" else "Amount format: decimal point · tap to change")
                    }
                    Text("Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(preview.take(5), key = { fingerprint(it) }) { item ->
                            androidx.compose.material3.Card(shape = RoundedCornerShape(12.dp)) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text("${item.date} · ${item.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(String.format(Locale.US, "R%,.2f", item.amount), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                    if (preview.isEmpty()) Text("No importable rows. Check the date and amount mappings.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    errorText?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = {
            Button(enabled = parsed != null && preview.isNotEmpty() && dateColumn >= 0 && amountColumn >= 0, onClick = { onImport(preview) }) {
                Text("Import ${preview.size} rows")
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
    ExposedDropdownMenuBox(expanded = expandedField == field, onExpandedChange = { onExpanded(if (it) field else null) }) {
        OutlinedTextField(
            value = if (selected >= 0) headers.getOrNull(selected).orEmpty() else "Not mapped",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedField == field) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true
        )
        androidx.compose.material3.ExposedDropdownMenu(expanded = expandedField == field, onDismissRequest = { onExpanded(null) }) {
            DropdownMenuItem(text = { Text("Not mapped") }, onClick = { onSelect(-1); onExpanded(null) })
            headers.forEachIndexed { index, header ->
                DropdownMenuItem(text = { Text(header.ifBlank { "Column ${index + 1}" }) }, onClick = { onSelect(index); onExpanded(null) })
            }
        }
    }
}

private fun findColumn(headers: List<String>, candidates: List<String>): Int =
    headers.indexOfFirst { header -> candidates.any { header.trim().equals(it, ignoreCase = true) || header.trim().contains(it, ignoreCase = true) } }

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
    "${transaction.date.lowercase(Locale.ROOT)}|${transaction.amount}|${transaction.description.trim().lowercase(Locale.ROOT)}|${transaction.category.trim().lowercase(Locale.ROOT)}"
