package com.example.paisatracker.widget

import android.content.Context
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

class QuickBalanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(180.dp, 160.dp), DpSize(280.dp, 180.dp))
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

        var todayTotal:  Double
        var monthTotal:  Double
        var budgetLimit: Double
        var fetchFailed: Boolean

        try {
            val now          = System.currentTimeMillis()
            val startOfDay   = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val endOfDay     = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59);      set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            val startOfMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            todayTotal  = repository.searchExpensesByDateRange(startOfDay, endOfDay, null)
                .first().sumOf { it.amount }
            monthTotal  = repository.searchExpensesByDateRange(startOfMonth, now, null)
                .first().sumOf { it.amount }
            budgetLimit = repository.getAllActiveBudgets().first()
                .find { it.period == BudgetPeriod.MONTHLY }?.limitAmount ?: 0.0
            fetchFailed = false
        } catch (e: Exception) {
            todayTotal  = 0.0
            monthTotal  = 0.0
            budgetLimit = 0.0
            fetchFailed = true
        }

        val hasBudget    = budgetLimit > 0.0
        val progress     = if (hasBudget) (monthTotal / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f
        val isOverBudget = hasBudget && monthTotal > budgetLimit

        provideContent {
            GlanceTheme(colors = WidgetColorScheme.colors) {
                val progressColor = when {
                    isOverBudget     -> GlanceTheme.colors.error
                    progress >= 0.8f -> GlanceTheme.colors.secondary
                    else             -> GlanceTheme.colors.primary
                }

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.background)
                        .clickable { },
                    contentAlignment = Alignment.TopStart
                ) {
                    if (fetchFailed) {
                        Column(
                            modifier            = GlanceModifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment   = Alignment.CenterVertically
                        ) {
                            Text(
                                "Unable to load",
                                style = TextStyle(
                                    color      = GlanceTheme.colors.onBackground,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                "Tap to open app",
                                style = TextStyle(
                                    color    = GlanceTheme.colors.onSurface,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        return@Box
                    }

                    Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {

                        // ── header ────────────────────────────────────────
                        Row(
                            modifier          = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "QUICK BALANCE",
                                style    = TextStyle(
                                    color    = GlanceTheme.colors.onSurface,
                                    fontSize = 9.sp
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            // Status dot
                            Box(
                                modifier = GlanceModifier
                                    .size(6.dp)
                                    .background(progressColor)
                            ) { /* dot */ }
                        }

                        Spacer(modifier = GlanceModifier.height(14.dp))

                        // ── today / month split ───────────────────────────
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            Column(
                                modifier            = GlanceModifier.defaultWeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Today",
                                    style = TextStyle(
                                        color    = GlanceTheme.colors.onSurface,
                                        fontSize = 10.sp
                                    )
                                )
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    CurrencyUtils.formatCurrency(todayTotal, context),
                                    style = TextStyle(
                                        color      = GlanceTheme.colors.onBackground,
                                        fontSize   = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            // Vertical divider
                            Box(
                                modifier = GlanceModifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(GlanceTheme.colors.surface)
                            ) { /* divider */ }

                            Column(
                                modifier            = GlanceModifier.defaultWeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "This month",
                                    style = TextStyle(
                                        color    = GlanceTheme.colors.onSurface,
                                        fontSize = 10.sp
                                    )
                                )
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    CurrencyUtils.formatCurrency(monthTotal, context),
                                    style = TextStyle(
                                        color      = GlanceTheme.colors.onBackground,
                                        fontSize   = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(14.dp))

                        // ── budget pill ───────────────────────────────────
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(GlanceTheme.colors.surface)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            if (!hasBudget) {
                                Text(
                                    "No budget set \u00b7 tap to configure",
                                    style = TextStyle(
                                        color    = GlanceTheme.colors.onSurface,
                                        fontSize = 11.sp
                                    )
                                )
                            } else {
                                Column(modifier = GlanceModifier.fillMaxWidth()) {
                                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                                        Text(
                                            "Monthly budget",
                                            style    = TextStyle(
                                                color    = GlanceTheme.colors.onSurface,
                                                fontSize = 10.sp
                                            ),
                                            modifier = GlanceModifier.defaultWeight()
                                        )
                                        Text(
                                            if (isOverBudget) "Over budget!"
                                            else "${(progress * 100).toInt()}% used",
                                            style = TextStyle(
                                                color      = progressColor,
                                                fontSize   = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                    Spacer(modifier = GlanceModifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress        = progress,
                                        modifier        = GlanceModifier.fillMaxWidth(),
                                        color           = progressColor,
                                        backgroundColor = GlanceTheme.colors.background
                                    )
                                    Spacer(modifier = GlanceModifier.height(6.dp))
                                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                                        Text(
                                            "${CurrencyUtils.formatCurrency(monthTotal, context)} spent",
                                            style    = TextStyle(
                                                color    = GlanceTheme.colors.onSurface,
                                                fontSize = 10.sp
                                            ),
                                            modifier = GlanceModifier.defaultWeight()
                                        )
                                        Text(
                                            "of ${CurrencyUtils.formatCurrency(budgetLimit, context)}",
                                            style = TextStyle(
                                                color    = GlanceTheme.colors.onSurface,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}