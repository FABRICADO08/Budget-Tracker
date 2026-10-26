package com.example.domain.finance

/** Pure budget rules shared by the dashboard, category budgets and alert generation. */
object BudgetRules {
    val alertThresholds: List<Int> = listOf(70, 90, 100)

    data class Progress(
        val limit: Double,
        val spent: Double,
        val remaining: Double,
        val percentUsed: Double,
        val exceeded: Boolean
    )

    fun progress(limit: Double, expenses: Iterable<Pair<String, Double>>): Progress {
        require(limit.isFinite() && limit >= 0.0) { "Budget limit must be a finite non-negative amount." }
        val spent = expenses
            .asSequence()
            .filter { it.first.equals("expense", ignoreCase = true) }
            .map { it.second }
            .filter { it.isFinite() && it > 0.0 }
            .sum()
        val percent = if (limit == 0.0) {
            if (spent > 0.0) 100.0 else 0.0
        } else {
            (spent / limit * 100.0)
        }
        return Progress(
            limit = limit,
            spent = spent,
            remaining = (limit - spent).coerceAtLeast(0.0),
            percentUsed = percent,
            exceeded = spent > limit
        )
    }

    /** Returns thresholds crossed by the current spend, suitable for insert-if-new alert records. */
    fun crossedThresholds(limit: Double, spent: Double): List<Int> {
        require(limit.isFinite() && limit >= 0.0) { "Budget limit must be a finite non-negative amount." }
        require(spent.isFinite() && spent >= 0.0) { "Spend must be a finite non-negative amount." }
        if (limit == 0.0) return if (spent > 0.0) listOf(70, 90, 100) else emptyList()
        val percent = spent / limit * 100.0
        return alertThresholds.filter { percent >= it }
    }
}
