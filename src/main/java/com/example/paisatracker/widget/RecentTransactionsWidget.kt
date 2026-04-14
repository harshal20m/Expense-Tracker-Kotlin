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
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.paisatracker.data.PaisaTrackerDatabase
import com.example.paisatracker.data.PaisaTrackerRepository
import com.example.paisatracker.util.CurrencyUtils
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class RecentTransactionsWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(250.dp, 200.dp), DpSize(250.dp, 280.dp))
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

        val expenses = try {
            repository.getRecentExpensesWithDetails(5).first()
        } catch (e: Exception) {
            emptyList()
        }

        val today     = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val yesterday = today - 86_400_000L

        fun smartDate(millis: Long): String = when {
            millis >= today     -> "Today \u00b7 ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))}"
            millis >= yesterday -> "Yesterday \u00b7 ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))}"
            else                -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
        }

        provideContent {
            GlanceTheme(colors = WidgetColorScheme.colors) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.surface)
                        .clickable { },
                    contentAlignment = Alignment.TopStart
                ) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        // ── header ────────────────────────────────────────
                        Row(
                            modifier          = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent transactions",
                                style    = TextStyle(
                                    color      = GlanceTheme.colors.onBackground,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            if (expenses.isNotEmpty()) {
                                Text(
                                    "${expenses.size} recent",
                                    style = TextStyle(
                                        color    = GlanceTheme.colors.onSurface,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(10.dp))

                        if (expenses.isEmpty()) {
                            // ── empty state ───────────────────────────────
                            Column(
                                modifier            = GlanceModifier
                                    .fillMaxWidth()
                                    .defaultWeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalAlignment   = Alignment.CenterVertically
                            ) {
                                Text(
                                    "No transactions yet",
                                    style = TextStyle(
                                        color      = GlanceTheme.colors.onBackground,
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Text(
                                    "Add your first expense in PaisaTracker",
                                    style = TextStyle(
                                        color    = GlanceTheme.colors.onSurface,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        } else {
                            // ── transaction rows ──────────────────────────
                            expenses.forEachIndexed { index, expense ->
                                Row(
                                    modifier          = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Emoji icon box
                                    Box(
                                        modifier         = GlanceModifier
                                            .size(30.dp)
                                            .background(GlanceTheme.colors.background),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            // Emoji renders as text in Glance
                                            text  = expense.categoryEmoji.take(2),
                                            style = TextStyle(fontSize = 13.sp)
                                        )
                                    }

                                    Spacer(modifier = GlanceModifier.width(10.dp))

                                    Column(modifier = GlanceModifier.defaultWeight()) {
                                        Text(
                                            expense.description
                                                .ifEmpty { expense.categoryName }
                                                .take(22),
                                            style = TextStyle(
                                                color      = GlanceTheme.colors.onBackground,
                                                fontSize   = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                        Spacer(modifier = GlanceModifier.height(1.dp))
                                        Text(
                                            smartDate(expense.date),
                                            style = TextStyle(
                                                color    = GlanceTheme.colors.onSurface,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }

                                    Text(
                                        "-${CurrencyUtils.formatCurrency(expense.amount, context)}",
                                        style = TextStyle(
                                            color      = GlanceTheme.colors.error,
                                            fontSize   = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                // Divider
                                if (index < expenses.size - 1) {
                                    Box(
                                        modifier = GlanceModifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(GlanceTheme.colors.background)
                                    ) { /* divider */ }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}