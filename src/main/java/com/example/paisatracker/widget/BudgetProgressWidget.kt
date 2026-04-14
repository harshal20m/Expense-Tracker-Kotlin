package com.example.paisatracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.paisatracker.data.BudgetPeriod
import com.example.paisatracker.data.PaisaTrackerDatabase
import com.example.paisatracker.data.PaisaTrackerRepository
import com.example.paisatracker.util.CurrencyUtils
import kotlinx.coroutines.flow.first
import java.util.*

class BudgetProgressWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(180.dp, 180.dp), DpSize(280.dp, 200.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = PaisaTrackerDatabase.getDatabase(context)
        val repository = PaisaTrackerRepository(
            projectDao      = db.projectDao(),
            categoryDao     = db.categoryDao(),
            expenseDao      = db.expenseDao(),
            assetDao        = db.assetDao(),
            backupDao       = db.backupDao(),
            budgetDao       = db.budgetDao(),
            flapDao         = db.flapDao(),
            salaryRecordDao = db.salaryRecordDao()
        )

        val data = try {
            val budgets      = repository.getAllActiveBudgets().first()
            val monthly      = budgets.find { it.period == BudgetPeriod.MONTHLY }
            val startOfMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val expenses = repository
                .searchExpensesByDateRange(startOfMonth, System.currentTimeMillis(), null)
                .first()
            BudgetData(
                monthTotal   = expenses.sumOf { it.amount },
                budgetLimit  = monthly?.limitAmount ?: 0.0,
                budgetName   = monthly?.name ?: "",
                expenseCount = expenses.size
            )
        } catch (e: Exception) {
            BudgetData(fetchFailed = true)
        }

        val progress     = if (data.budgetLimit > 0)
            (data.monthTotal / data.budgetLimit).toFloat().coerceIn(0f, 1f) else 0f
        val isOverBudget = data.budgetLimit > 0 && data.monthTotal > data.budgetLimit
        val percentage   = (progress * 100).toInt()
        val remaining    = data.budgetLimit - data.monthTotal
        val daysLeft     = Calendar.getInstance().let {
            it.getActualMaximum(Calendar.DAY_OF_MONTH) - it.get(Calendar.DAY_OF_MONTH)
        }

        provideContent {
            GlanceTheme(colors = WidgetColorScheme.colors) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(
                            if (isOverBudget) GlanceTheme.colors.errorContainer
                            else GlanceTheme.colors.background
                        )
                        .clickable { },
                    contentAlignment = Alignment.TopStart
                ) {
                    when {
                        data.fetchFailed -> EmptyState(
                            "Unable to load",
                            "Tap to open app"
                        )
                        data.budgetLimit <= 0.0 -> EmptyState(
                            "No budget set",
                            "Open app to create a monthly budget"
                        )
                        data.expenseCount == 0 -> EmptyState(
                            "Budget ready  \u2022  ${CurrencyUtils.formatCurrency(data.budgetLimit, context)}",
                            "No expenses recorded this month"
                        )
                        else -> BudgetContent(
                            data         = data,
                            progress     = progress,
                            isOverBudget = isOverBudget,
                            percentage   = percentage,
                            remaining    = remaining,
                            daysLeft     = daysLeft,
                            context      = context
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyState(line1: String, line2: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Text(
                text  = line1,
                style = TextStyle(
                    color      = GlanceTheme.colors.onBackground,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text  = line2,
                style = TextStyle(
                    color    = GlanceTheme.colors.onSurface,
                    fontSize = 11.sp
                )
            )
        }
    }

    @Composable
    private fun BudgetContent(
        data: BudgetData,
        progress: Float,
        isOverBudget: Boolean,
        percentage: Int,
        remaining: Double,
        daysLeft: Int,
        context: Context
    ) {
        // Pick semantic color: green=primary, amber=secondary, red=error
        val accentColor = when {
            isOverBudget     -> GlanceTheme.colors.error
            progress >= 0.8f -> GlanceTheme.colors.secondary
            else             -> GlanceTheme.colors.primary
        }

        Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {

            // ── header ────────────────────────────────────────────────────
            Row(
                modifier          = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text  = "BUDGET",
                        style = TextStyle(
                            color    = GlanceTheme.colors.onSurface,
                            fontSize = 9.sp
                        )
                    )
                    Text(
                        text  = data.budgetName.ifEmpty { "Monthly" },
                        style = TextStyle(
                            color      = GlanceTheme.colors.onBackground,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Text(
                    text  = if (isOverBudget) "OVER BUDGET" else "$daysLeft days left",
                    style = TextStyle(
                        color    = if (isOverBudget) GlanceTheme.colors.error
                        else GlanceTheme.colors.onSurface,
                        fontSize = 9.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(14.dp))

            // ── percentage box + spent / remaining ────────────────────────
            Row(
                modifier          = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier          = GlanceModifier
                        .size(56.dp)
                        .background(GlanceTheme.colors.surface),
                    contentAlignment  = Alignment.Center
                ) {
                    Text(
                        text  = "$percentage%",
                        style = TextStyle(
                            color      = accentColor,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(14.dp))

                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        "Spent",
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 10.sp)
                    )
                    Text(
                        CurrencyUtils.formatCurrency(data.monthTotal, context),
                        style = TextStyle(
                            color      = GlanceTheme.colors.onBackground,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Text(
                        if (isOverBudget) "Over by" else "Remaining",
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 10.sp)
                    )
                    Text(
                        CurrencyUtils.formatCurrency(
                            if (isOverBudget) data.monthTotal - data.budgetLimit else remaining,
                            context
                        ),
                        style = TextStyle(
                            color      = if (isOverBudget) GlanceTheme.colors.error
                            else GlanceTheme.colors.primary,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            // ── determinate progress bar ──────────────────────────────────
            LinearProgressIndicator(
                progress         = progress,
                modifier         = GlanceModifier.fillMaxWidth(),
                color            = accentColor,
                backgroundColor  = GlanceTheme.colors.surface
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            // ── footer ────────────────────────────────────────────────────
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                FooterStat(
                    GlanceModifier.defaultWeight(),
                    CurrencyUtils.formatCurrency(data.budgetLimit, context),
                    "Budget"
                )
                FooterStat(GlanceModifier.defaultWeight(), "$daysLeft", "Days left")
                FooterStat(GlanceModifier.defaultWeight(), "${data.expenseCount}", "Expenses")
            }
        }
    }

    @Composable
    private fun FooterStat(modifier: GlanceModifier, value: String, label: String) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                style = TextStyle(
                    color      = GlanceTheme.colors.onBackground,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                label,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 9.sp)
            )
        }
    }

    private data class BudgetData(
        val monthTotal: Double   = 0.0,
        val budgetLimit: Double  = 0.0,
        val budgetName: String   = "",
        val expenseCount: Int    = 0,
        val fetchFailed: Boolean = false
    )
}