package com.example.paisatracker.data
data class BudgetWithSpending(
    val budget: Budget,
    val spent: Double,
    val categoryName: String? = null,
    val projectName: String? = null
) {
    val remaining: Double get() = budget.limitAmount - spent
    val percentUsed: Float get() = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
    val isOverBudget: Boolean get() = spent > budget.limitAmount
    val isNearLimit: Boolean get() = percentUsed >= 0.8f && !isOverBudget
}
data class ProjectBudgetSpend(
    val projectId: Long,
    val projectName: String,
    val projectEmoji: String,
    val totalSpent: Double
)