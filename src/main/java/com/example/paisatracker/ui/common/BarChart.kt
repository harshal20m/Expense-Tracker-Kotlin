package com.example.paisatracker.ui.common

import android.graphics.Color
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

@Composable
fun BarChart(
    modifier: Modifier = Modifier,
    entries: List<BarEntry>,
    labels: List<String>,
    description: String
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            BarChart(context).apply {
                // Chart description
                this.description.text = description
                this.description.textSize = 14f
                this.description.textColor = Color.GRAY
                this.description.setPosition(0f, 0f)

                // Enable legend
                legend.isEnabled = true
                legend.textSize = 12f
                legend.textColor = Color.BLACK

                // Enable touch gestures
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)

                // Set background
                setDrawGridBackground(false)
                setDrawBarShadow(false)

                // X-Axis configuration
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    textColor = Color.BLACK
                    textSize = 11f
                    labelRotationAngle = -45f
                    valueFormatter = IndexAxisValueFormatter(labels)
                }

                // Left Y-Axis configuration
                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = Color.LTGRAY
                    textColor = Color.BLACK
                    textSize = 11f
                    axisMinimum = 0f
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return "₹${value.toInt()}"
                        }
                    }
                }

                // Right Y-Axis (disabled)
                axisRight.isEnabled = false

                // Extra offsets for label visibility
                setExtraOffsets(10f, 10f, 10f, 20f)
            }
        },
        update = { chart ->
            val dataSet = BarDataSet(entries, "Expenses by Category").apply {
                colors = ColorTemplate.MATERIAL_COLORS.toList()
                valueTextColor = Color.BLACK
                valueTextSize = 10f
                valueTypeface = Typeface.DEFAULT_BOLD

                // Value formatter for rupees
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "₹${value.toInt()}"
                    }
                }

                // Bar styling
                setDrawValues(true)
                highLightAlpha = 255
            }

            chart.data = BarData(dataSet).apply {
                barWidth = 0.8f
            }

            // Animate the chart
            chart.animateY(1000)
            chart.invalidate()
        }
    )
}
