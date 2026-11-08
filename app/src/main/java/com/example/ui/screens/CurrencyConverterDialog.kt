package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.ZarMoneyFormatter
import java.util.Locale

data class CurrencyRate(
    val code: String,
    val name: String,
    val flag: String,
    val symbol: String,
    // Rate relative to 1 ZAR (e.g. 1 USD = 18.25 ZAR means zarPerUnit = 18.25)
    val zarPerUnit: Double
)

val SupportedCurrencies = listOf(
    CurrencyRate("USD", "US Dollar", "🇺🇸", "$", 18.25),
    CurrencyRate("EUR", "Euro", "🇪🇺", "€", 19.85),
    CurrencyRate("GBP", "British Pound", "🇬🇧", "£", 23.45),
    CurrencyRate("AUD", "Australian Dollar", "🇦🇺", "A$", 11.95),
    CurrencyRate("CAD", "Canadian Dollar", "🇨🇦", "C$", 13.35),
    CurrencyRate("JPY", "Japanese Yen (100)", "🇯🇵", "¥", 0.122), // per 1 JPY
    CurrencyRate("INR", "Indian Rupee", "🇮🇳", "₹", 0.218),
    CurrencyRate("AED", "UAE Dirham", "🇦🇪", "د.إ", 4.97)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterDialog(
    onDismiss: () -> Unit,
    onApplyExpense: (zarAmount: Double, note: String) -> Unit
) {
    var selectedCurrency by remember { mutableStateOf(SupportedCurrencies[0]) } // USD
    var foreignAmountInput by remember { mutableStateOf("") }
    var itemNote by remember { mutableStateOf("") }
    var isCurrencyMenuExpanded by remember { mutableStateOf(false) }

    val foreignAmount = foreignAmountInput.toDoubleOrNull() ?: 0.0
    val convertedZar = foreignAmount * selectedCurrency.zarPerUnit

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
                .testTag("currency_converter_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Currency & Travel Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Convert foreign prices to South African Rand",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("currency_converter_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Currency Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = isCurrencyMenuExpanded,
                    onExpandedChange = { isCurrencyMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedCurrency.flag} ${selectedCurrency.code} - ${selectedCurrency.name}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Foreign Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCurrencyMenuExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isCurrencyMenuExpanded,
                        onDismissRequest = { isCurrencyMenuExpanded = false }
                    ) {
                        SupportedCurrencies.forEach { curr ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${curr.flag} ${curr.code} (${curr.symbol})")
                                        Text(
                                            "1 ${curr.code} = R%.2f".format(curr.zarPerUnit),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                onClick = {
                                    selectedCurrency = curr
                                    isCurrencyMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quick Currency Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SupportedCurrencies.take(5).forEach { curr ->
                        val isSelected = selectedCurrency.code == curr.code
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCurrency = curr },
                            label = { Text("${curr.flag} ${curr.code}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }

                // Amount Input
                OutlinedTextField(
                    value = foreignAmountInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*([.,]\d{0,2})?$"""))) {
                            foreignAmountInput = input.replace(',', '.')
                        }
                    },
                    label = { Text("Amount in ${selectedCurrency.code} (${selectedCurrency.symbol})") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Text(
                            text = selectedCurrency.symbol,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("foreign_amount_input")
                )

                // Quick Amount Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 25, 50, 100).forEach { preset ->
                        Surface(
                            onClick = { foreignAmountInput = preset.toString() },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${selectedCurrency.symbol}$preset",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Note / Expense Description
                OutlinedTextField(
                    value = itemNote,
                    onValueChange = { itemNote = it },
                    label = { Text("Item Note / Description (Optional)") },
                    placeholder = { Text("e.g. Souvenir, Coffee in NYC, Flight add-on") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FlightTakeoff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Result Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Exchange Rate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "1 ${selectedCurrency.code} = R%.2f ZAR".format(selectedCurrency.zarPerUnit),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Converted in Rand",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${selectedCurrency.symbol}%.2f ${selectedCurrency.code}".format(foreignAmount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = ZarMoneyFormatter.format(convertedZar),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Action: Apply to New Expense
                Button(
                    onClick = {
                        if (convertedZar > 0.0) {
                            val desc = if (itemNote.isNotBlank()) {
                                "$itemNote (${selectedCurrency.symbol}%.2f %s)".format(Locale.ROOT, foreignAmount, selectedCurrency.code)
                            } else {
                                "Foreign purchase: ${selectedCurrency.symbol}%.2f %s".format(Locale.ROOT, foreignAmount, selectedCurrency.code)
                            }
                            onApplyExpense(convertedZar, desc)
                            onDismiss()
                        }
                    },
                    enabled = convertedZar > 0.0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("apply_converted_expense_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log as Expense (${ZarMoneyFormatter.format(convertedZar)})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
