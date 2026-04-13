package com.example.paisatracker.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.LocalContext
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.lifecycle.lifecycleScope
import com.example.paisatracker.data.PaisaTrackerDatabase
import com.example.paisatracker.util.CurrencyUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalGlanceApi::class)
class QuickBalanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(160.dp, 160.dp), DpSize(250.dp, 180.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = PaisaTrackerDatabase.getDatabase(context)
        val repository = db.repository
        
        // Get today's expenses
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        val calendarEnd = Calendar.getInstance()
        calendarEnd.set(Calendar.HOUR_OF_DAY, 23)
        calendarEnd.set(Calendar.MINUTE, 59)
        calendarEnd.set(Calendar.SECOND, 59)
        calendarEnd.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendarEnd.timeInMillis
        
        val todayExpenses = repository.searchExpensesByDateRange(startOfDay, endOfDay, null).first()
        val todayTotal = todayExpenses.sumOf { it.amount }
        
        // Get monthly expenses
        val calendarMonth = Calendar.getInstance()
        calendarMonth.set(Calendar.DAY_OF_MONTH, 1)
        calendarMonth.set(Calendar.HOUR_OF_DAY, 0)
        calendarMonth.set(Calendar.MINUTE, 0)
        calendarMonth.set(Calendar.SECOND, 0)
        calendarMonth.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendarMonth.timeInMillis
        
        val monthExpenses = repository.searchExpensesByDateRange(startOfMonth, System.currentTimeMillis(), null).first()
        val monthTotal = monthExpenses.sumOf { it.amount }
        
        // Get active budget
        val budgets = repository.getAllActiveBudgets().first()
        val monthlyBudget = budgets.find { it.period == com.example.paisatracker.data.BudgetPeriod.MONTHLY }?.limitAmount ?: 0.0
        
        val progress = if (monthlyBudget > 0) (monthTotal / monthlyBudget).coerceIn(0.0, 1.0) else 0.0

        provideContent {
            ColorProviders(
                light = androidx.compose.material3.MaterialTheme.colorScheme,
                dark = androidx.compose.material3.darkColorScheme()
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(ColorProviders(light = Color(0xFF6200EE), dark = Color(0xFF3700B3)))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = GlanceModifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Balance",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = androidx.compose.ui.unit.sp(14.sp),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Column(
                                modifier = GlanceModifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Today",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                                        fontSize = androidx.compose.ui.unit.sp(12.sp)
                                    )
                                )
                                Text(
                                    text = CurrencyUtils.formatCurrency(todayTotal),
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = androidx.compose.ui.unit.sp(18.sp),
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = GlanceModifier.padding(top = 4.dp)
                                )
                            }
                            
                            Spacer(GlanceModifier.width(16.dp))
                            
                            Column(
                                modifier = GlanceModifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Monthly",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                                        fontSize = androidx.compose.ui.unit.sp(12.sp)
                                    )
                                )
                                Text(
                                    text = CurrencyUtils.formatCurrency(monthTotal),
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = androidx.compose.ui.unit.sp(18.sp),
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = GlanceModifier.padding(top = 4.dp)
                                )
                            }
                        }
                        
                        if (monthlyBudget > 0) {
                            Box(
                                modifier = GlanceModifier
                                    .size(80.dp)
                                    .padding(top = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = progress.toFloat(),
                                    modifier = GlanceModifier.size(80.dp),
                                    color = ColorProvider(Color.White),
                                    trackColor = ColorProvider(Color.White.copy(alpha = 0.3f))
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        style = TextStyle(
                                            color = ColorProvider(Color.White),
                                            fontSize = androidx.compose.ui.unit.sp(14.sp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "of budget",
                                        style = TextStyle(
                                            color = ColorProvider(Color.White.copy(alpha = 0.7f)),
                                            fontSize = androidx.compose.ui.unit.sp(10.sp)
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
