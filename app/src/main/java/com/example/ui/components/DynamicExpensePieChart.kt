package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Transaction
import com.example.util.ZarMoneyFormatter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class PieSliceData(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Color
)

object CategoryColorPalette {
    private val colorMap = mapOf(
        "food & dining" to Color(0xFFFF7043),
        "dining" to Color(0xFFFF7043),
        "groceries" to Color(0xFFFFA726),
        "transportation" to Color(0xFF26A69A),
        "transport" to Color(0xFF26A69A),
        "bills & utilities" to Color(0xFF42A5F5),
        "bills" to Color(0xFF42A5F5),
        "utilities" to Color(0xFF42A5F5),
        "shopping" to Color(0xFFAB47BC),
        "entertainment" to Color(0xFFEC407A),
        "healthcare" to Color(0xFF00ACC1),
        "medical" to Color(0xFF00ACC1),
        "education" to Color(0xFF5C6BC0),
        "savings" to Color(0xFF66BB6A),
        "personal care" to Color(0xFFFF8A65),
        "housing" to Color(0xFF8D6E63),
        "rent" to Color(0xFF8D6E63)
    )

    private val fallbackColors = listOf(
        Color(0xFFFF7043),
        Color(0xFF42A5F5),
        Color(0xFF66BB6A),
        Color(0xFFAB47BC),
        Color(0xFFFFA726),
        Color(0xFF26A69A),
        Color(0xFFEC407A),
        Color(0xFF00ACC1),
        Color(0xFF5C6BC0),
        Color(0xFFFF8A65),
        Color(0xFF78909C)
    )

    fun getColor(category: String, index: Int = 0): Color {
        val key = category.trim().lowercase()
        return colorMap[key] ?: fallbackColors[index % fallbackColors.size]
    }
}

/**
 * Native Jetpack Compose Dynamic Pie & Donut Chart
 * Provides animated entry, interactive slice selection on touch, center donut readout,
 * and interactive category legends.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicExpensePieChart(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier,
    title: String = "Expense Distribution"
) {
    val expenseTransactions = remember(transactions) {
        transactions.filter { it.type.equals("expense", ignoreCase = true) }
    }

    val totalExpense = remember(expenseTransactions) {
        expenseTransactions.sumOf { it.amount }
    }

    val categoryBreakdown = remember(expenseTransactions, totalExpense) {
        val grouped = expenseTransactions.groupBy { it.category.trim() }
        var currentAngle = -90f // Start at 12 o'clock

        grouped.entries
            .map { (category, list) ->
                val sum = list.sumOf { it.amount }
                val pct = if (totalExpense > 0) (sum / totalExpense * 100).toFloat() else 0f
                category to Pair(sum, pct)
            }
            .sortedByDescending { it.second.first }
            .mapIndexed { index, (category, stats) ->
                val (sum, pct) = stats
                val sweep = if (totalExpense > 0) (sum / totalExpense * 360f).toFloat() else 0f
                val slice = PieSliceData(
                    category = category,
                    amount = sum,
                    percentage = pct,
                    startAngle = currentAngle,
                    sweepAngle = sweep,
                    color = CategoryColorPalette.getColor(category, index)
                )
                currentAngle += sweep
                slice
            }
    }

    var selectedSlice by remember { mutableStateOf<PieSliceData?>(null) }

    // Clear selected slice if it's no longer present in category breakdown
    LaunchedEffect(categoryBreakdown) {
        if (selectedSlice != null && categoryBreakdown.none { it.category == selectedSlice?.category }) {
            selectedSlice = null
        }
    }

    // Animation progress (0f to 1f)
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(categoryBreakdown) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dynamic_expense_pie_chart"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (categoryBreakdown.isNotEmpty()) {
                                "${categoryBreakdown.size} categories · Tap slice to inspect"
                            } else {
                                "No expenses in this timeframe"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (selectedSlice != null) {
                    Surface(
                        onClick = { selectedSlice = null },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (categoryBreakdown.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No category data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Interactive Donut Chart Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progress = animationProgress.value

                    Canvas(
                        modifier = Modifier
                            .size(210.dp)
                            .testTag("pie_chart_canvas")
                            .pointerInput(categoryBreakdown) {
                                detectTapGestures { offset ->
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val dx = offset.x - centerX
                                    val dy = offset.y - centerY
                                    val distance = sqrt(dx * dx + dy * dy)
                                    val outerRadius = size.width / 2f
                                    val innerRadius = outerRadius * 0.55f

                                    // Check if tap is within donut ring
                                    if (distance in (innerRadius * 0.8f)..(outerRadius * 1.15f)) {
                                        var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        // Standardize to 0..360 where -90 (top) is start
                                        // normalize so start at -90
                                        var normalizedAngle = (touchAngle + 90f)
                                        if (normalizedAngle < 0f) normalizedAngle += 360f

                                        // Find which slice covers this normalized angle
                                        val clicked = categoryBreakdown.firstOrNull { slice ->
                                            var sliceStart = (slice.startAngle + 90f)
                                            if (sliceStart < 0f) sliceStart += 360f
                                            val sliceEnd = sliceStart + slice.sweepAngle
                                            if (sliceEnd <= 360f) {
                                                normalizedAngle in sliceStart..sliceEnd
                                            } else {
                                                normalizedAngle >= sliceStart || normalizedAngle <= (sliceEnd % 360f)
                                            }
                                        }

                                        selectedSlice = if (selectedSlice?.category == clicked?.category) null else clicked
                                    } else if (distance < innerRadius) {
                                        // Tapping inside center clears selection
                                        selectedSlice = null
                                    }
                                }
                            }
                    ) {
                        val strokeWidth = 36.dp.toPx()
                        val selectedStrokeWidth = 46.dp.toPx()
                        val diameter = size.minDimension - selectedStrokeWidth
                        val topLeftOffset = (size.minDimension - diameter) / 2f

                        categoryBreakdown.forEach { slice ->
                            val isSelected = selectedSlice?.category == slice.category
                            val currentStroke = if (isSelected) selectedStrokeWidth else strokeWidth
                            val sliceAlpha = if (selectedSlice == null || isSelected) 1f else 0.4f

                            val animatedSweep = slice.sweepAngle * progress

                            drawArc(
                                color = slice.color.copy(alpha = sliceAlpha),
                                startAngle = slice.startAngle,
                                sweepAngle = animatedSweep,
                                useCenter = false,
                                topLeft = Offset(topLeftOffset, topLeftOffset),
                                size = Size(diameter, diameter),
                                style = Stroke(width = currentStroke, cap = StrokeCap.Butt)
                            )
                        }
                    }

                    // Center Donut Readout
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .size(115.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { selectedSlice = null }
                            .padding(8.dp)
                    ) {
                        if (selectedSlice != null) {
                            val active = selectedSlice!!
                            Text(
                                text = active.category,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = active.color,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = ZarMoneyFormatter.format(active.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "%.1f%% of total".format(active.percentage),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "TOTAL SPEND",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = ZarMoneyFormatter.format(totalExpense),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${categoryBreakdown.size} categories",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Interactive Legend Chips (wrapping)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoryBreakdown.forEach { slice ->
                        val isSelected = selectedSlice?.category == slice.category
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedSlice = if (isSelected) null else slice
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(slice.color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${slice.category} (%.0f%%)".format(slice.percentage),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = slice.color.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }
    }
}
