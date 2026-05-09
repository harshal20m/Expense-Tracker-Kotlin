package com.example.paisatracker.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.paisatracker.data.PaisaTrackerDatabase
import com.example.paisatracker.data.PaisaTrackerRepository
import com.example.paisatracker.util.CurrencyUtils
import kotlinx.coroutines.flow.first
import java.util.*

class SalaryWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(180.dp, 100.dp), DpSize(280.dp, 120.dp))
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
            salaryRecordDao = db.salaryRecordDao(),
            actionHistoryDao = db.actionHistoryDao(),
            bankAccountDao  = db.bankAccountDao()
        )

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)

        val salaryRecord = repository.getCurrentMonthSalary(currentMonth, currentYear).first()
        val totalSpent = salaryRecord?.let { repository.getTotalSpentSince(it.receivedAt).first() } ?: 0.0
        val balance = (salaryRecord?.amount ?: 0.0) - totalSpent
        val progress = if (salaryRecord != null && salaryRecord.amount > 0) {
            (totalSpent / salaryRecord.amount).toFloat().coerceIn(0f, 1f)
        } else 0f

        provideContent {
            GlanceTheme(colors = WidgetColorScheme.colors) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.background)
                        .clickable(actionRunCallback<RefreshSalaryAction>()),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "REMAINING SALARY",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        if (salaryRecord == null) {
                            Text(
                                "No salary set for this month",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 12.sp
                                )
                            )
                        } else {
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    CurrencyUtils.formatCurrency(balance, context),
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onBackground,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = GlanceModifier.width(4.dp))
                                Text(
                                    "left",
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Spacer(modifier = GlanceModifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = progress,
                                modifier = GlanceModifier.fillMaxWidth(),
                                color = if (progress > 0.9f) GlanceTheme.colors.error else GlanceTheme.colors.primary,
                                backgroundColor = GlanceTheme.colors.surface
                            )

                            Spacer(modifier = GlanceModifier.height(4.dp))

                            Row(modifier = GlanceModifier.fillMaxWidth()) {
                                Text(
                                    "${(progress * 100).toInt()}% spent",
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 10.sp
                                    ),
                                    modifier = GlanceModifier.defaultWeight()
                                )
                                Text(
                                    "Total: ${CurrencyUtils.formatCurrency(salaryRecord.amount, context)}",
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
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

class RefreshSalaryAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        SalaryWidget().updateAll(context)
    }
}
