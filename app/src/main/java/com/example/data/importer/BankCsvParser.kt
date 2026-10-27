package com.example.data.importer

/** Configurable CSV parser foundation for bank statements. No file is imported automatically. */
object BankCsvParser {
    data class Row(val lineNumber: Int, val values: List<String>)
    data class ParsedStatement(val headers: List<String>, val rows: List<Row>, val delimiter: Char)
    data class MoneyMovement(val amount: Double, val type: String)

    /**
     * Converts either a signed Amount column or separate Debit/Credit columns into one movement.
     * For separate columns, the category is determined by the populated side, not by sign.
     * Rows with invalid values, no movement, or both debit and credit populated are rejected.
     */
    fun parseMoneyMovement(
        amountRaw: String?,
        debitRaw: String? = null,
        creditRaw: String? = null,
        decimalComma: Boolean = false
    ): MoneyMovement? {
        if (debitRaw != null || creditRaw != null) {
            val debitText = debitRaw.orEmpty().trim()
            val creditText = creditRaw.orEmpty().trim()
            if (debitText.isBlank() && creditText.isBlank()) return null

            val debit = if (debitText.isBlank()) 0.0 else parseAmount(debitText, decimalComma) ?: return null
            val credit = if (creditText.isBlank()) 0.0 else parseAmount(creditText, decimalComma) ?: return null
            val hasDebit = kotlin.math.abs(debit) > 0.0
            val hasCredit = kotlin.math.abs(credit) > 0.0

            if (hasDebit == hasCredit) return null // both zero/blank or both sides populated
            return if (hasDebit) {
                MoneyMovement(kotlin.math.abs(debit), "expense")
            } else {
                MoneyMovement(kotlin.math.abs(credit), "income")
            }
        }

        val parsed = amountRaw?.let { parseAmount(it, decimalComma) } ?: return null
        if (parsed == 0.0) return null
        return MoneyMovement(kotlin.math.abs(parsed), if (parsed < 0.0) "expense" else "income")
    }

    /**
     * Parses CSV with quoted delimiters, escaped quotes and CRLF/LF line endings.
     * Semicolon-delimited exports are supported for decimal-comma locales.
     */
    fun parse(text: String, delimiter: Char? = null): ParsedStatement {
        val normalized = text.removePrefix("\uFEFF")
        val selectedDelimiter = delimiter ?: detectDelimiter(normalized)
        require(selectedDelimiter == ',' || selectedDelimiter == ';' || selectedDelimiter == '\t') {
            "Supported delimiters are comma, semicolon and tab."
        }
        val records = parseRecords(normalized, selectedDelimiter)
        require(records.isNotEmpty()) { "The selected file is empty." }
        val headers = records.first().map(String::trim)
        require(headers.any(String::isNotBlank)) { "The statement must contain a header row." }
        val rows = records.drop(1)
            .mapIndexedNotNull { index, values ->
                if (values.all(String::isBlank)) null
                else Row(lineNumber = index + 2, values = values)
            }
        return ParsedStatement(headers, rows, selectedDelimiter)
    }

    /** Parses amounts from common ZA formats: 1 234,56; R1,234.56; -123.45. */
    fun parseAmount(raw: String, decimalComma: Boolean = false): Double? {
        var value = raw.trim()
            .replace("R", "", ignoreCase = true)
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace("'", "")
        if (value.isBlank()) return null

        value = if (decimalComma) {
            value.replace(".", "").replace(',', '.')
        } else if (value.contains(',') && value.contains('.')) {
            if (value.lastIndexOf(',') > value.lastIndexOf('.')) {
                value.replace(".", "").replace(',', '.')
            } else {
                value.replace(",", "")
            }
        } else if (value.count { it == ',' } == 1 && !value.contains('.')) {
            // A lone comma is treated as a decimal separator (e.g. 123,45).
            value.replace(',', '.')
        } else {
            value.replace(",", "")
        }
        return value.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    private fun detectDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        return listOf(',', ';', '\t').maxByOrNull { delimiter ->
            countOutsideQuotes(firstLine, delimiter)
        } ?: ','
    }

    private fun countOutsideQuotes(line: String, delimiter: Char): Int {
        var quoted = false
        var count = 0
        var i = 0
        while (i < line.length) {
            when (line[i]) {
                '"' -> if (quoted && i + 1 < line.length && line[i + 1] == '"') i++ else quoted = !quoted
                delimiter -> if (!quoted) count++
            }
            i++
        }
        return count
    }

    private fun parseRecords(text: String, delimiter: Char): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '"' && quoted && i + 1 < text.length && text[i + 1] == '"' -> {
                    cell.append('"')
                    i++
                }
                ch == '"' -> quoted = !quoted
                ch == delimiter && !quoted -> {
                    row.add(cell.toString())
                    cell.setLength(0)
                }
                (ch == '\n' || ch == '\r') && !quoted -> {
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(cell.toString())
                    cell.setLength(0)
                    if (row.any(String::isNotEmpty)) records.add(row)
                    row = mutableListOf()
                }
                else -> cell.append(ch)
            }
            i++
        }
        require(!quoted) { "The CSV contains an unterminated quoted field." }
        row.add(cell.toString())
        if (row.any(String::isNotEmpty)) records.add(row)
        return records
    }
}
