package com.example.paisatracker.ui.common

import android.graphics.Color
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

@Composable
fun PieChartWithLegend(
    modifier: Modifier = Modifier,
    entries: List<PieEntry>,
    description: String
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pie Chart (Left Side) - Centered
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f),
                factory = { context ->
                    PieChart(context).apply {
                        // Disable description
                        this.description.isEnabled = false

                        // Hole configuration
                        isDrawHoleEnabled = true
                        setHoleColor(Color.TRANSPARENT)
                        holeRadius = 50f
                        transparentCircleRadius = 55f

                        // Disable built-in legend
                        legend.isEnabled = false

                        // Disable entry labels
                        setDrawEntryLabels(false)

                        // Disable percentage values on pie
                        setDrawSliceText(false)

                        // Rotation
                        rotationAngle = 0f
                        isRotationEnabled = true
                        isHighlightPerTapEnabled = true

                        // No padding
                        setExtraOffsets(0f, 0f, 0f, 0f)

                        // Disable touch
                        setTouchEnabled(false)
                    }
                },
                update = { chart ->
                    val dataSet = PieDataSet(entries, "").apply {
                        colors = ColorTemplate.MATERIAL_COLORS.toList()

                        // Disable values completely
                        setDrawValues(false)

                        // Slice space
                        sliceSpace = 2f
                        selectionShift = 5f
                    }

                    chart.data = PieData(dataSet)

                    // Animate the chart
                    chart.animateY(1000)
                    chart.invalidate()
                }
            )
        }

        // Legend (Right Side) - Centered
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            val totalValue = entries.sumOf { it.value.toDouble() }.toFloat()

            entries.forEachIndexed { index, entry ->
                LegendItem(
                    label = entry.label ?: "Unknown",
                    percentage = (entry.value / totalValue) * 100,
                    color = ColorTemplate.MATERIAL_COLORS[index % ColorTemplate.MATERIAL_COLORS.size]
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    percentage: Float,
    color: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color Dot + Label
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Color Dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color(color))
            )

            // Label
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }

        // Percentage
        Text(
            text = "%.1f%%".format(percentage),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// Backward compatibility
@Composable
fun PieChart(modifier: Modifier = Modifier, entries: List<PieEntry>, description: String) {
    PieChartWithLegend(modifier = modifier, entries = entries, description = description)
}
