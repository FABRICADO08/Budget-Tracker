package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Loan
import com.example.data.local.entity.Transaction
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.ZarMoneyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MonthSpendPoint(
    val yearMonth: String,
    val shortLabel: String,
    val fullLabel: String,
    val totalExpense: Double,
    val count: Int,
    val isCurrentMonth: Boolean
)

@Composable
fun ChartboardScreen(
    transactions: List<Transaction>,
    loans: List<Loan>,
    onAskAiAdvisor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var timeframeMonths by remember { mutableIntStateOf(6) } // 3, 6, or 12
    var selectedMonthYm by remember { mutableStateOf<String?>(null) }

    val monthFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val shortLabelFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val fullLabelFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val currentYm = remember { monthFormat.format(Date()) }

    // Generate monthly points according to timeframe
    val monthlyData = remember(transactions, timeframeMonths) {
        val list = mutableListOf<MonthSpendPoint>()
        for (i in (timeframeMonths - 1) downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i)
            }
            val ym = monthFormat.format(cal.time)
            val monthTx = transactions.filter { it.date.startsWith(ym) && it.type == "expense" }
            val sum = monthTx.sumOf { it.amount }
            list.add(
                MonthSpendPoint(
                    yearMonth = ym,
                    shortLabel = shortLabelFormat.format(cal.time),
                    fullLabel = fullLabelFormat.format(cal.time),
                    totalExpense = sum,
                    count = monthTx.size,
                    isCurrentMonth = ym == currentYm
                )
            )
        }
        list
    }

    // Default selected month to current month if null or not in list
    val activeMonthPoint = remember(monthlyData, selectedMonthYm) {
        monthlyData.firstOrNull { it.yearMonth == selectedMonthYm }
            ?: monthlyData.firstOrNull { it.isCurrentMonth }
            ?: monthlyData.lastOrNull()
    }

    // Month-over-month comparison (Current vs Previous Month)
    val currentMonthTotal = remember(transactions, currentYm) {
        transactions.filter { it.date.startsWith(currentYm) && it.type == "expense" }.sumOf { it.amount }
    }
    val prevMonthYm = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        monthFormat.format(cal.time)
    }
    val prevMonthTotal = remember(transactions, prevMonthYm) {
        transactions.filter { it.date.startsWith(prevMonthYm) && it.type == "expense" }.sumOf { it.amount }
    }
    val momDifference = currentMonthTotal - prevMonthTotal
    val momPercent = if (prevMonthTotal > 0) ((momDifference / prevMonthTotal) * 100) else 0.0

    // Filter transactions for category breakdown: either focused month or all in timeframe
    val focusedTransactions = remember(transactions, activeMonthPoint) {
        if (activeMonthPoint != null) {
            transactions.filter { it.date.startsWith(activeMonthPoint.yearMonth) && it.type == "expense" }
        } else {
            transactions.filter { it.type == "expense" }
        }
    }

    // Category breakdown
    val categoryBreakdown = remember(focusedTransactions) {
        val grouped = focusedTransactions.groupBy { it.category }
        val total = focusedTransactions.sumOf { it.amount }
        grouped.map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            val pct = if (total > 0) (sum / total * 100).toFloat() else 0f
            Triple(cat, sum, pct)
        }.sortedByDescending { it.second }
    }

    // Overall metrics
    val totalAllExpense = remember(transactions) {
        transactions.filter { it.type == "expense" }.sumOf { it.amount }
    }
    val totalAllIncome = remember(transactions) {
        transactions.filter { it.type == "income" }.sumOf { it.amount }
    }
    val netCashflow = totalAllIncome - totalAllExpense

    // Loans metrics
    val totalLent = remember(loans) { loans.sumOf { it.amount } }
    val totalRecovered = remember(loans) { loans.filter { it.isRepaid }.sumOf { it.amount } }
    val totalPending = totalLent - totalRecovered
    val loanRecoveryRate = if (totalLent > 0) (totalRecovered / totalLent).toFloat() else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("chartboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Chartboard Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Insights,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Financial Chartboard",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Monthly Spending Patterns & Analytics",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "ZAR (R)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Month-over-Month Highlight Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "This Month (${activeMonthPoint?.shortLabel ?: "Current"})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ZarMoneyFormatter.format(currentMonthTotal),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (prevMonthTotal > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (momDifference <= 0) IncomeGreen.copy(alpha = 0.12f) else ExpenseRed.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (momDifference <= 0) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = if (momDifference <= 0) IncomeGreen else ExpenseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "%s%.1f%% vs last mo".format(if (momDifference > 0) "+" else "", momPercent),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (momDifference <= 0) IncomeGreen else ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Timeframe Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )

                FilterChip(
                    selected = timeframeMonths == 3,
                    onClick = { timeframeMonths = 3 },
                    label = { Text("Last 3 Months") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                FilterChip(
                    selected = timeframeMonths == 6,
                    onClick = { timeframeMonths = 6 },
                    label = { Text("Last 6 Months") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                FilterChip(
                    selected = timeframeMonths == 12,
                    onClick = { timeframeMonths = 12 },
                    label = { Text("Last 12 Months") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // 3. Dynamic Bar Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Monthly Expenditure",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap any bar to inspect month details",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (activeMonthPoint != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = activeMonthPoint.fullLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // The Bar Chart Visual
                    val maxAmount = remember(monthlyData) {
                        (monthlyData.maxOfOrNull { it.totalExpense } ?: 0.0).coerceAtLeast(100.0)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            monthlyData.forEach { point ->
                                val isSelected = point.yearMonth == activeMonthPoint?.yearMonth
                                val proportion = (point.totalExpense / maxAmount).toFloat().coerceIn(0.04f, 1f)
                                val animatedHeight by animateFloatAsState(
                                    targetValue = proportion,
                                    animationSpec = tween(durationMillis = 600),
                                    label = "bar_anim_${point.yearMonth}"
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { selectedMonthYm = point.yearMonth },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    // Total value tag above bar
                                    if (point.totalExpense > 0) {
                                        Text(
                                            text = if (point.totalExpense >= 1000) "%.1fk".format(point.totalExpense / 1000.0) else "%.0f".format(point.totalExpense),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Text(
                                            text = "0",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Bar pill
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.65f)
                                            .fillMaxHeight(animatedHeight * 0.72f)
                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                            .background(
                                                when {
                                                    isSelected -> Brush.verticalGradient(
                                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                                    )
                                                    point.isCurrentMonth -> Brush.verticalGradient(
                                                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), MaterialTheme.colorScheme.primaryContainer)
                                                    )
                                                    point.totalExpense > 0 -> Brush.verticalGradient(
                                                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                                    )
                                                    else -> Brush.verticalGradient(
                                                        listOf(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                                                    )
                                                }
                                            )
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Month label
                                    Text(
                                        text = point.shortLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected || point.isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Active Month Summary row
                    if (activeMonthPoint != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected: ${activeMonthPoint.fullLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${ZarMoneyFormatter.format(activeMonthPoint.totalExpense)} (${activeMonthPoint.count} txns)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 4. Category Breakdown Spectrum & Bars
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeMonthPoint?.shortLabel ?: "All",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (categoryBreakdown.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No expenditure recorded for this period",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        categoryBreakdown.forEach { (catName, sum, pct) ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(
                                                    getChartboardCategoryColor(catName).copy(alpha = 0.15f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getChartboardCategoryIcon(catName),
                                                contentDescription = null,
                                                tint = getChartboardCategoryColor(catName),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = catName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = ZarMoneyFormatter.format(sum),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "%.1f%%".format(pct),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LinearProgressIndicator(
                                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = getChartboardCategoryColor(catName),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Cash Flow & Debt Recovery Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Inflow vs Outflow Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cash Flow",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Net Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = ZarMoneyFormatter.format(netCashflow),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (netCashflow >= 0) IncomeGreen else ExpenseRed
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("In", style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
                                Text("R%.0f".format(totalAllIncome), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Out", style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
                                Text("R%.0f".format(totalAllExpense), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Loan Recovery Status Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Handshake,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Loan Recovery",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Owed to You",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = ZarMoneyFormatter.format(totalPending),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalPending > 0) ExpenseRed else IncomeGreen
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { loanRecoveryRate.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = IncomeGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "%.0f%% recovered".format(loanRecoveryRate * 100),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 6. Smart AI Chartboard Analysis CTA Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AI Chartboard Insights",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Ask Gemini 3.8 Flash to analyze your spending trends",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val topCategory = categoryBreakdown.firstOrNull()?.first ?: "General"
                            val prompt = "Analyze my Chartboard spending patterns: My total expenditure this month is R%.2f. My top spending category is %s. I have R%.2f in outstanding loans owed to me. Give me 3 actionable tips in South African Rand (R) to optimize my budget and save more money."
                                .format(currentMonthTotal, topCategory, totalPending)
                            onAskAiAdvisor(prompt)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyze_chartboard_ai_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analyze My Chartboard with AI",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun getChartboardCategoryIcon(category: String): ImageVector {
    return when (category.lowercase(Locale.getDefault())) {
        "food & dining", "food" -> Icons.Default.Fastfood
        "groceries" -> Icons.Default.ShoppingCart
        "transportation", "transport" -> Icons.Default.DirectionsCar
        "bills & utilities", "bills", "utilities" -> Icons.Default.HomeRepairService
        "shopping" -> Icons.Default.Receipt
        "entertainment" -> Icons.Default.Movie
        "fitness", "health" -> Icons.Default.FitnessCenter
        "loans" -> Icons.Default.Handshake
        else -> Icons.Default.Receipt
    }
}

private fun getChartboardCategoryColor(category: String): Color {
    return when (category.lowercase(Locale.getDefault())) {
        "food & dining", "food" -> Color(0xFFFF9800)
        "groceries" -> Color(0xFF4CAF50)
        "transportation", "transport" -> Color(0xFF2196F3)
        "bills & utilities", "bills", "utilities" -> Color(0xFFE91E63)
        "shopping" -> Color(0xFF9C27B0)
        "entertainment" -> Color(0xFFFF5722)
        "fitness", "health" -> Color(0xFF009688)
        "loans" -> Color(0xFF673AB7)
        else -> Color(0xFF607D8B)
    }
}
