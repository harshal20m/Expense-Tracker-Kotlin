package com.example.paisatracker.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.ExperimentalGlanceApi
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
import com.example.paisatracker.data.BudgetPeriod
import com.example.paisatracker.data.PaisaTrackerDatabase
import com.example.paisatracker.util.CurrencyUtils
import kotlinx.coroutines.flow.first
import java.util.*

@OptIn(ExperimentalGlanceApi::class)
class BudgetProgressWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(160.dp, 160.dp), DpSize(250.dp, 180.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = PaisaTrackerDatabase.getDatabase(context)
        val repository = db.repository
        
        // Get active monthly budget
        val budgets = repository.getAllActiveBudgets().first()
        val monthlyBudget = budgets.find { it.period == BudgetPeriod.MONTHLY }
        
        // Calculate monthly expenses
        val calendarMonth = Calendar.getInstance()
        calendarMonth.set(Calendar.DAY_OF_MONTH, 1)
        calendarMonth.set(Calendar.HOUR_OF_DAY, 0)
        calendarMonth.set(Calendar.MINUTE, 0)
        calendarMonth.set(Calendar.SECOND, 0)
        calendarMonth.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendarMonth.timeInMillis
        
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
                light = androidx.compose.material3.MaterialTheme.colorScheme,
                dark = androidx.compose.material3.darkColorScheme()
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(ColorProviders(light = Color(0xFFE8F5E9), dark = Color(0xFF1B5E20)))
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
                                fontSize = androidx.compose.ui.unit.sp(14.sp),
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
                                        fontSize = androidx.compose.ui.unit.sp(20.sp),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "used",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                                        fontSize = androidx.compose.ui.unit.sp(11.sp)
                                    )
                                )
                            }
                        }
                        
                        Spacer(GlanceModifier.height(12.dp))
                        
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Horizontal.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Spent",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                                        fontSize = androidx.compose.ui.unit.sp(11.sp)
                                    )
                                )
                                Text(
                                    text = CurrencyUtils.formatCurrency(monthTotal),
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = androidx.compose.ui.unit.sp(14.sp),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Remaining",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                                        fontSize = androidx.compose.ui.unit.sp(11.sp)
                                    )
                                )
                                Text(
                                    text = CurrencyUtils.formatCurrency(remaining.coerceAtLeast(0.0)),
                                    style = TextStyle(
                                        color = ColorProvider(if (remaining >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)),
                                        fontSize = androidx.compose.ui.unit.sp(14.sp),
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
                                    fontSize = androidx.compose.ui.unit.sp(11.sp)
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
