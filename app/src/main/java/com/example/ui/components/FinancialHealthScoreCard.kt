package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Loan
import com.example.data.local.entity.Transaction
import com.example.util.ZarMoneyFormatter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class FinancialHealthAuditResult(
    val score: Int, // 0 - 100
    val grade: String, // A+, A, B, C, D
    val gradeColor: Color,
    val totalIncome: Double,
    val totalExpense: Double,
    val needsSpend: Double,
    val needsPct: Double,
    val wantsSpend: Double,
    val wantsPct: Double,
    val savingsSpend: Double,
    val savingsPct: Double,
    val statusSummary: String,
    val primaryRecommendation: String
)

object FinancialHealthCalculator {
    private val NEEDS_CATEGORIES = setOf(
        "groceries", "bills", "bills & utilities", "utilities", "rent",
        "housing", "transport", "transportation", "healthcare", "medical",
        "education", "insurance"
    )
    private val WANTS_CATEGORIES = setOf(
        "food & dining", "dining", "entertainment", "shopping", "personal",
        "personal care", "travel", "subscriptions", "leisure", "hobbies"
    )
    private val SAVINGS_CATEGORIES = setOf(
        "savings", "investment", "emergency fund", "debt repayment"
    )

    fun calculateAudit(
        transactions: List<Transaction>,
        loans: List<Loan> = emptyList()
    ): FinancialHealthAuditResult {
        val incomeTransactions = transactions.filter { it.type.equals("income", ignoreCase = true) }
        val expenseTransactions = transactions.filter { it.type.equals("expense", ignoreCase = true) }

        val totalIncome = incomeTransactions.sumOf { it.amount }
        val totalExpense = expenseTransactions.sumOf { it.amount }

        var needs = 0.0
        var wants = 0.0
        var savings = 0.0

        for (tx in expenseTransactions) {
            val cat = tx.category.trim().lowercase()
            when {
                NEEDS_CATEGORIES.any { cat.contains(it) } -> needs += tx.amount
                SAVINGS_CATEGORIES.any { cat.contains(it) } -> savings += tx.amount
                WANTS_CATEGORIES.any { cat.contains(it) } -> wants += tx.amount
                else -> wants += tx.amount // Default uncategorized to Wants
            }
        }

        // Add loan repayments received or made
        val repaidLoansAmount = loans.filter { it.isRepaid }.sumOf { it.amount }
        savings += (repaidLoansAmount * 0.5) // Repaid debt counts towards savings/debt health

        // Base denominator: use totalIncome if available, otherwise totalExpense
        val effectiveIncome = if (totalIncome > 0.0) totalIncome else max(totalExpense, 1.0)

        val needsPct = (needs / effectiveIncome) * 100.0
        val wantsPct = (wants / effectiveIncome) * 100.0
        val savingsPct = if (totalIncome > 0.0) {
            max(0.0, ((totalIncome - totalExpense) / totalIncome) * 100.0)
        } else {
            (savings / effectiveIncome) * 100.0
        }

        // Compute 0-100 Financial Health Score
        // 50/30/20 Ideal Benchmark:
        // Needs <= 50% (+40 pts)
        // Wants <= 30% (+30 pts)
        // Savings >= 20% (+30 pts)
        var score = 100

        // Needs penalty if over 50%
        if (needsPct > 50.0) {
            val penalty = ((needsPct - 50.0) * 1.2).toInt()
            score -= min(35, penalty)
        }

        // Wants penalty if over 30%
        if (wantsPct > 30.0) {
            val penalty = ((wantsPct - 30.0) * 1.4).toInt()
            score -= min(35, penalty)
        }

        // Savings penalty if under 20%
        if (savingsPct < 20.0) {
            val penalty = ((20.0 - savingsPct) * 1.5).toInt()
            score -= min(30, penalty)
        }

        // Severe deficit penalty
        if (totalIncome > 0.0 && totalExpense > totalIncome) {
            score -= 15
        }

        score = score.coerceIn(15, 100)

        val (grade, gradeColor) = when {
            score >= 90 -> "A+ Excellent" to Color(0xFF2E7D32)
            score >= 80 -> "A Healthy" to Color(0xFF388E3C)
            score >= 70 -> "B Balanced" to Color(0xFF1976D2)
            score >= 55 -> "C Needs Care" to Color(0xFFF57C00)
            else -> "D High Burn" to Color(0xFFD32F2F)
        }

        val statusSummary = when {
            score >= 85 -> "Your budget is exceptionally disciplined and well within the 50/30/20 standard."
            score >= 70 -> "Good financial balance, with minor adjustments recommended on discretionary wants."
            score >= 55 -> "Your non-essential spending is starting to encroach on your savings capacity."
            else -> "Monthly outflows exceed healthy benchmarks. Prioritize pausing discretionary subscriptions."
        }

        val primaryRecommendation = when {
            wantsPct > 35.0 -> "Trimming ${ZarMoneyFormatter.format(wants * 0.15)} from Dining & Shopping will return your Wants to the recommended 30% ratio."
            needsPct > 60.0 -> "Fixed essential expenses account for %.0f%% of your funds. Look for energy, grocery, or mobile contract savings.".format(needsPct)
            savingsPct < 15.0 -> "Aim to save at least 20%% of inflow. Setting an automated R500 weekly transfer builds a durable buffer."
            else -> "You are hitting your financial milestones! Allocate surplus funds to long-term goals or investments."
        }

        return FinancialHealthAuditResult(
            score = score,
            grade = grade,
            gradeColor = gradeColor,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            needsSpend = needs,
            needsPct = needsPct,
            wantsSpend = wants,
            wantsPct = wantsPct,
            savingsSpend = savings,
            savingsPct = savingsPct,
            statusSummary = statusSummary,
            primaryRecommendation = primaryRecommendation
        )
    }
}

@Composable
fun FinancialHealthScoreCard(
    transactions: List<Transaction>,
    loans: List<Loan> = emptyList(),
    onAskAiAdvisor: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val audit = remember(transactions, loans) {
        FinancialHealthCalculator.calculateAudit(transactions, loans)
    }

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("financial_health_score_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Badge & Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(audit.gradeColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = audit.gradeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "50/30/20 Budget Health",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Financial Wellness Score",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Score pill
                Surface(
                    color = audit.gradeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${audit.score}/100",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = audit.gradeColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${audit.grade.takeWhile { it != ' ' }})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = audit.gradeColor
                        )
                    }
                }
            }

            // Summary text
            Text(
                text = audit.statusSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 50 / 30 / 20 Segmented Distribution Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Needs (Ideal 50%)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                    Text("Wants (Ideal 30%)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE65100))
                    Text("Savings (Ideal 20%)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1565C0))
                }

                // Visual segmented bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val safeNeeds = (audit.needsPct / 100.0).toFloat().coerceIn(0.05f, 0.90f)
                    val safeWants = (audit.wantsPct / 100.0).toFloat().coerceIn(0.05f, 0.90f)
                    val safeSavings = (audit.savingsPct / 100.0).toFloat().coerceIn(0.05f, 0.90f)
                    val sum = safeNeeds + safeWants + safeSavings

                    Box(
                        modifier = Modifier
                            .weight(safeNeeds / sum)
                            .background(Color(0xFF4CAF50))
                    )
                    Box(
                        modifier = Modifier
                            .weight(safeWants / sum)
                            .background(Color(0xFFFF9800))
                    )
                    Box(
                        modifier = Modifier
                            .weight(safeSavings / sum)
                            .background(Color(0xFF2196F3))
                    )
                }

                // Percentages
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("%.0f%% actual".format(audit.needsPct), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("%.0f%% actual".format(audit.wantsPct), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("%.0f%% actual".format(audit.savingsPct), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Recommendation Box
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = audit.primaryRecommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Expand/Collapse Details Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide category breakdown" else "View category breakdown",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Detail rows
                    AuditMetricRow(
                        title = "Needs (Housing, Groceries, Utilities)",
                        amount = audit.needsSpend,
                        pct = audit.needsPct,
                        targetPct = 50.0,
                        color = Color(0xFF4CAF50)
                    )
                    AuditMetricRow(
                        title = "Wants (Dining, Shopping, Entertainment)",
                        amount = audit.wantsSpend,
                        pct = audit.wantsPct,
                        targetPct = 30.0,
                        color = Color(0xFFFF9800)
                    )
                    AuditMetricRow(
                        title = "Savings & Debt Repayments",
                        amount = audit.savingsSpend,
                        pct = audit.savingsPct,
                        targetPct = 20.0,
                        color = Color(0xFF2196F3)
                    )

                    if (onAskAiAdvisor != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                onAskAiAdvisor("Analyze my 50/30/20 budget score (${audit.score}/100, ${audit.grade}) and suggest 3 concrete steps to optimize my savings.")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ask AI to Optimize This Score", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditMetricRow(
    title: String,
    amount: Double,
    pct: Double,
    targetPct: Double,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${ZarMoneyFormatter.format(amount)} (%.0f%%)".format(pct),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}
