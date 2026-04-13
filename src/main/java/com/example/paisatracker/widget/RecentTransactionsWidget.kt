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
import com.example.paisatracker.data.PaisaTrackerDatabase
import com.example.paisatracker.util.CurrencyUtils
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class RecentTransactionsWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(250.dp, 180.dp), DpSize(250.dp, 250.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = PaisaTrackerDatabase.getDatabase(context)
        val repository = db.repository
        
        // Get recent expenses (last 5)
        val recentExpenses = repository.getRecentExpensesWithDetails(5).first()

        provideContent {
            ColorProviders(
                colors = androidx.glance.material3.ColorProviders(
                    light = androidx.compose.material3.lightColorScheme(),
                    dark = androidx.compose.material3.darkColorScheme()
                )
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(ColorProvider(Color(0xFF03DAC6)))
                        .clickable { },
                    contentAlignment = Alignment.TopStart
                ) {
                    Column(
                        modifier = GlanceModifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "Recent Transactions",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.padding(bottom = 12.dp)
                        )
                        
                        if (recentExpenses.isEmpty()) {
                            Text(
                                text = "No recent transactions",
                                style = TextStyle(
                                    color = ColorProvider(Color.White.copy(alpha = 0.7f)),
                                    fontSize = 14.sp
                                ),
                                modifier = GlanceModifier.padding(vertical = 8.dp)
                            )
                        } else {
                            recentExpenses.take(5).forEachIndexed { index, expense ->
                                Row(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = GlanceModifier.weight(1f)
                                    ) {
                                        Text(
                                            text = expense.categoryEmoji,
                                            style = TextStyle(
                                                fontSize = 18.sp
                                            ),
                                            modifier = GlanceModifier.padding(end = 8.dp)
                                        )
                                        Column {
                                            Text(
                                                text = expense.description.ifEmpty { expense.categoryName },
                                                style = TextStyle(
                                                    color = ColorProvider(Color.White),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                maxLines = 1
                                            )
                                            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                            val dateStr = dateFormat.format(Date(expense.date))
                                            Text(
                                                text = dateStr,
                                                style = TextStyle(
                                                    color = ColorProvider(Color.White.copy(alpha = 0.7f)),
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                    Text(
                                        text = "-${CurrencyUtils.formatCurrency(expense.amount)}",
                                        style = TextStyle(
                                            color = ColorProvider(Color.White),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                
                                if (index < recentExpenses.size - 1) {
                                    Divider(
                                        modifier = GlanceModifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        color = ColorProvider(Color.White.copy(alpha = 0.3f))
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
