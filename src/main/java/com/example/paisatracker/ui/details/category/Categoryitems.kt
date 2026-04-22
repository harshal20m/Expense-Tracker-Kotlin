package com.example.paisatracker.ui.details.category

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.data.CategoryWithTotal
import com.example.paisatracker.util.formatCurrency

// ── Add new placeholders ───────────────────────────────────────────────────────

@Composable
fun AddCategoryListItem(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dashColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .drawWithCache {
                val stroke = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f))
                onDrawWithContent { drawContent(); drawRoundRect(color = dashColor, style = stroke, cornerRadius = CornerRadius(16.dp.toPx())) }
            }
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text("Add New Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AddCategoryGridItem(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dashColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .drawWithCache {
                val stroke = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f))
                onDrawWithContent { drawContent(); drawRoundRect(color = dashColor, style = stroke, cornerRadius = CornerRadius(14.dp.toPx())) }
            }
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Add Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ── Category list item ─────────────────────────────────────────────────────────

@Composable
fun CategoryListItem(
    categoryWithTotal: CategoryWithTotal,
    totalAmountAllCategories: Double,
    onCategoryClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val shareOfTotal = if (totalAmountAllCategories > 0) (categoryWithTotal.totalAmount / totalAmountAllCategories).toFloat() else 0f
    val progress by animateFloatAsState(targetValue = shareOfTotal, animationSpec = spring(dampingRatio = 0.7f), label = "progress")

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onCategoryClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(categoryWithTotal.category.emoji, fontSize = 20.sp) }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        Text(categoryWithTotal.category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${categoryWithTotal.expenseCount} expense${if (categoryWithTotal.expenseCount != 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(formatCurrency(categoryWithTotal.totalAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                        Text("${(shareOfTotal * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { onEditClick(); menuExpanded = false }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { onDeleteClick(); menuExpanded = false }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                    }
                }
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

// ── Category grid item ─────────────────────────────────────────────────────────

@Composable
fun CategoryGridItem(
    categoryWithTotal: CategoryWithTotal,
    totalAmountAllCategories: Double,
    onCategoryClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val shareOfTotal = if (totalAmountAllCategories > 0) (categoryWithTotal.totalAmount / totalAmountAllCategories).toFloat() else 0f

    Card(
        modifier = modifier.fillMaxWidth().height(160.dp).clickable(onClick = onCategoryClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), MaterialTheme.colorScheme.surface))
            )
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text(categoryWithTotal.category.emoji, fontSize = 20.sp) }
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(text = { Text("Edit", style = MaterialTheme.typography.bodySmall) }, onClick = { onEditClick(); menuExpanded = false })
                                DropdownMenuItem(text = { Text("Delete", style = MaterialTheme.typography.bodySmall) }, onClick = { onDeleteClick(); menuExpanded = false })
                            }
                        }
                    }
                    Text(categoryWithTotal.category.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(formatCurrency(categoryWithTotal.totalAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${categoryWithTotal.expenseCount} exp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                            Text("${(shareOfTotal * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

