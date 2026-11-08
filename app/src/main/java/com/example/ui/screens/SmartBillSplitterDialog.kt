package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.ZarMoneyFormatter
import kotlin.math.roundToInt

@Composable
fun SmartBillSplitterDialog(
    onDismiss: () -> Unit,
    onSaveMyShareExpense: (amount: Double, description: String, category: String) -> Unit,
    onSaveFriendLoan: (friendName: String, amount: Double, note: String) -> Unit
) {
    var billAmountInput by remember { mutableStateOf("") }
    var billDescription by remember { mutableStateOf("Dinner & Drinks") }
    var tipPercentage by remember { mutableDoubleStateOf(10.0) } // Default 10%
    var customTipInput by remember { mutableStateOf("") }
    var isCustomTip by remember { mutableStateOf(false) }
    var peopleCount by remember { mutableIntStateOf(2) }

    // Dialog state for recording friend's loan
    var showRecordLoanDialog by remember { mutableStateOf(false) }
    var loanFriendName by remember { mutableStateOf("") }

    val rawBill = billAmountInput.toDoubleOrNull() ?: 0.0
    val activeTipPercent = if (isCustomTip) {
        customTipInput.toDoubleOrNull() ?: 0.0
    } else {
        tipPercentage
    }

    val tipAmount = (rawBill * (activeTipPercent / 100.0))
    val grandTotal = rawBill + tipAmount
    val perPersonShare = if (peopleCount > 0) grandTotal / peopleCount else 0.0
    val friendsTotalToCollect = grandTotal - perPersonShare

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
                .testTag("bill_splitter_dialog"),
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
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Smart Bill Splitter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Calculate tips & split fairly with friends",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("bill_splitter_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Bill Amount & Note Inputs
                OutlinedTextField(
                    value = billAmountInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*([.,]\d{0,2})?$"""))) {
                            billAmountInput = input.replace(',', '.')
                        }
                    },
                    label = { Text("Total Bill Amount (R)") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Text(
                            text = "R",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bill_amount_input")
                )

                OutlinedTextField(
                    value = billDescription,
                    onValueChange = { billDescription = it },
                    label = { Text("Description / Place") },
                    placeholder = { Text("e.g. Spur, Italian dinner, Uber ride") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bill_description_input")
                )

                // Tip Percentage Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tip Percentage",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tip: ${ZarMoneyFormatter.format(tipAmount)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val tipOptions = listOf(0.0, 10.0, 12.5, 15.0, 20.0)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tipOptions.forEach { pct ->
                            val isSelected = !isCustomTip && tipPercentage == pct
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    isCustomTip = false
                                    tipPercentage = pct
                                },
                                label = { Text("${pct.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }}%") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        FilterChip(
                            selected = isCustomTip,
                            onClick = { isCustomTip = true },
                            label = { Text("Custom") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }

                    if (isCustomTip) {
                        OutlinedTextField(
                            value = customTipInput,
                            onValueChange = { customTipInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Enter Custom Tip %") },
                            trailingIcon = { Text("%", fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Number of People Stepper
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Split Between ($peopleCount people)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick people presets
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 3, 4, 5).forEach { count ->
                                Surface(
                                    selected = peopleCount == count,
                                    onClick = { peopleCount = count },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (peopleCount == count) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(width = 44.dp, height = 38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$count",
                                            fontWeight = if (peopleCount == count) FontWeight.Bold else FontWeight.Medium,
                                            color = if (peopleCount == count) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Stepper (- / +)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { if (peopleCount > 1) peopleCount-- },
                                enabled = peopleCount > 1,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease people")
                            }
                            Text(
                                text = "$peopleCount",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            IconButton(
                                onClick = { if (peopleCount < 30) peopleCount++ },
                                enabled = peopleCount < 30,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase people")
                            }
                        }
                    }
                }

                // Split Calculation Breakdown Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(ZarMoneyFormatter.format(rawBill), fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tip (${activeTipPercent}%):", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(ZarMoneyFormatter.format(tipAmount), fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grand Total:", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = ZarMoneyFormatter.format(grandTotal),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
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
                                    text = "Each Person Pays",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Your share: ${ZarMoneyFormatter.format(perPersonShare)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = ZarMoneyFormatter.format(perPersonShare),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Log My Share as Expense
                    Button(
                        onClick = {
                            if (perPersonShare > 0.0) {
                                val desc = if (billDescription.isNotBlank()) "Split: $billDescription" else "Split dining bill"
                                onSaveMyShareExpense(perPersonShare, desc, "Food & Dining")
                                onDismiss()
                            }
                        },
                        enabled = perPersonShare > 0.0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("log_my_share_expense_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log My Share (${ZarMoneyFormatter.format(perPersonShare)})",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 2. Record Friend's Unpaid Portion as Loan
                    if (peopleCount > 1 && friendsTotalToCollect > 0.0) {
                        OutlinedButton(
                            onClick = {
                                loanFriendName = ""
                                showRecordLoanDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("record_friend_loan_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Handshake,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Record Friend's Share as Loan",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Mini Dialog to Record Friend Loan
    if (showRecordLoanDialog) {
        AlertDialog(
            onDismissRequest = { showRecordLoanDialog = false },
            title = {
                Text(
                    text = "Track Friend's Portion",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Total to collect from others: ${ZarMoneyFormatter.format(friendsTotalToCollect)} (${peopleCount - 1} friend${if (peopleCount > 2) "s" else ""}). " +
                            "Enter the borrower's name to track this loan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = loanFriendName,
                        onValueChange = { loanFriendName = it },
                        label = { Text("Friend / Borrower Name") },
                        placeholder = { Text("e.g. John, Alex, Group") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("loan_friend_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = loanFriendName.trim().ifEmpty { "Friend" }
                        val loanAmount = if (peopleCount == 2) perPersonShare else friendsTotalToCollect
                        val note = "Split bill: $billDescription"
                        onSaveFriendLoan(name, loanAmount, note)
                        showRecordLoanDialog = false
                        onDismiss()
                    },
                    enabled = loanFriendName.isNotBlank()
                ) {
                    Text("Save Loan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordLoanDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
