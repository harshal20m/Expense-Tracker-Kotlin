package com.example.paisatracker.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.paisatracker.data.*
import com.example.paisatracker.util.CurrencyUtils
import kotlinx.coroutines.flow.first
import java.util.*

class BudgetProgressWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(160.dp, 160.dp), DpSize(250.dp, 180.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = PaisaTrackerDatabase.getDatabase(context)
        
        // Create repository instance with all required DAOs
        val repository = PaisaTrackerRepository(
            projectDao = db.projectDao(),
            categoryDao = db.categoryDao(),
            expenseDao = db.expenseDao(),
            assetDao = db.assetDao(),
            backupDao = db.backupDao(),
            budgetDao = db.budgetDao(),
            flapDao = db.flapDao(),
            salaryRecordDao = db.salaryRecordDao()
        )
        
        // Get active monthly budget
        val budgets = repository.getAllActiveBudgets().first()
        val monthlyBudget = budgets.find { it.period == BudgetPeriod.MONTHLY }
        
        // Calculate monthly expenses
        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val monthExpenses = repository.searchExpensesByDateRange(startOfMonth, System.currentTimeMillis(), null).first()
        val monthTotal = monthExpenses.sumOf { it.amount }
        
        val budgetLimit = monthlyBudget?.limitAmount ?: 0.0
        val progress = if (budgetLimit > 0) (monthTotal / budgetLimit).coerceIn(0.0, 1.0) else 0.0
        val remaining = budgetLimit - monthTotal
        val percentage = (progress * 100).toInt()
        
        // Determine color based on usage
        val progressColor = when {
            progress < 0.5 -> Color(0xFF4CAF50)  // Green
            progress < 0.75 -> Color(0xFFFFC107) // Yellow
            else -> Color(0xFFF44336)            // Red
        }

        provideContent {
            ColorProviders(
                light = androidx.glance.material3.ProvidedValues.lightColorScheme(),
                dark = androidx.glance.material3.ProvidedValues.darkColorScheme()
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(ColorProvider(Color(0xFFE8F5E9)))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = GlanceModifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = monthlyBudget?.name ?: "Monthly Budget",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.padding(bottom = 12.dp)
                        )
                        
                        Box(
                            modifier = GlanceModifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = progress.toFloat(),
                                modifier = GlanceModifier.size(100.dp),
                                color = ColorProvider(progressColor),
                                trackColor = ColorProvider(Color.White.copy(alpha = 0.3f))
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$percentage%",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "used",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                        
                        Spacer(GlanceModifier.height(12.dp))
                        
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = GlanceModifier.weight(1f)
                            ) {
                                Text(
                                    text = "Spent",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = CurrencyUtils.formatCurrency(monthTotal),
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = GlanceModifier.weight(1f)
                            ) {
                                Text(
                                    text = "Remaining",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = CurrencyUtils.formatCurrency(remaining.coerceAtLeast(0.0)),
                                    style = TextStyle(
                                        color = ColorProvider(if (remaining >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        
                        if (budgetLimit > 0) {
                            Text(
                                text = "of ${CurrencyUtils.formatCurrency(budgetLimit)}",
                                style = TextStyle(
                                    color = ColorProvider(Color.White.copy(alpha = 0.7f)),
                                    fontSize = 11.sp
                                ),
                                modifier = GlanceModifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
