package com.example.paisatracker.ui.main.projects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.data.ProjectWithTotal
import com.example.paisatracker.util.CurrentCurrency
import com.example.paisatracker.util.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectListItemWithReorder(
    projectWithTotal: ProjectWithTotal,
    currentIndex: Int,
    totalItems: Int,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onProjectClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded    by remember { mutableStateOf(false) }
    var isAmountVisible by remember { mutableStateOf(false) }
    val canMoveUp   = currentIndex > 0
    val canMoveDown = currentIndex < totalItems - 1
    val currency    = CurrentCurrency.get()

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, hoveredElevation = 6.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape     = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

                // ── Top row: emoji + name + Explore + ⋮ ──────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        modifier              = Modifier.weight(1f),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(projectWithTotal.project.emoji, fontSize = 26.sp)
                        }
                        Text(
                            text      = projectWithTotal.project.name,
                            style     = MaterialTheme.typography.titleLarge,
                            fontWeight= FontWeight.Bold,
                            color     = MaterialTheme.colorScheme.onSurface,
                            maxLines  = 2,
                            overflow  = TextOverflow.Ellipsis,
                            fontSize  = 17.sp,
                            lineHeight= 22.sp,
                            modifier  = Modifier.weight(1f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick         = onProjectClick,
                            modifier        = Modifier.height(40.dp),
                            shape           = RoundedCornerShape(10.dp),
                            colors          = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor   = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding  = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Text("Explore", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        IconButton(
                            onClick  = { menuExpanded = !menuExpanded },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // ── Expanded menu ─────────────────────────────────────────────
                AnimatedVisibility(visible = menuExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MenuActionButton(
                                icon   = Icons.Default.Edit,
                                label  = "Edit",
                                onClick= { onEditClick(); menuExpanded = false },
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                            MenuActionButton(
                                icon     = Icons.Default.KeyboardArrowUp,
                                label    = "To Top",
                                onClick  = { if (canMoveUp) onReorder(currentIndex, 0); menuExpanded = false },
                                enabled  = canMoveUp,
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                            MenuActionButton(
                                icon     = Icons.Default.KeyboardArrowDown,
                                label    = "To Bottom",
                                onClick  = { if (canMoveDown) onReorder(currentIndex, totalItems - 1); menuExpanded = false },
                                enabled  = canMoveDown,
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        }
                        MenuActionButton(
                            icon           = Icons.Default.Delete,
                            label          = "Delete Project",
                            onClick        = { onDeleteClick(); menuExpanded = false },
                            modifier       = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            contentColor   = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // ── Stats row (shown when menu is closed) ─────────────────────
                AnimatedVisibility(visible = !menuExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Spacer(modifier = Modifier.height(0.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CompactStatBox("Categories", "${projectWithTotal.categoryCount}", Modifier.weight(1f))
                            CompactStatBox("Expenses",   "${projectWithTotal.expenseCount}",  Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                DateChip("Created", formatDateCompact(projectWithTotal.project.createdAt))
                                DateChip("Updated", formatDateCompact(projectWithTotal.project.lastModified))
                            }
                            // Amount reveal
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    )))
                                    .clickable { isAmountVisible = !isAmountVisible }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text      = if (isAmountVisible) formatCurrency(projectWithTotal.totalAmount) else "${currency.symbol} ••••••",
                                        style     = MaterialTheme.typography.titleLarge,
                                        fontWeight= FontWeight.Bold,
                                        color     = MaterialTheme.colorScheme.primary,
                                        fontSize  = 17.sp
                                    )
                                    Text(
                                        text    = if (isAmountVisible) "Tap to hide" else "Tap to show",
                                        style   = MaterialTheme.typography.labelSmall,
                                        color   = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        fontSize= 9.sp
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

// ── Sub-components used only by ProjectListItem ───────────────────────────────

@Composable
fun CompactStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)) {
        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 18.sp)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

@Composable
fun DateChip(label: String, date: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$label:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp)
        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp)
        }
    }
}

@Composable
fun MenuActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: androidx.compose.ui.graphics.Color   = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.height(48.dp), shape = RoundedCornerShape(12.dp), color = if (enabled) containerColor else containerColor.copy(alpha = 0.5f)) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (enabled) contentColor else contentColor.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f), fontSize = 13.sp)
        }
    }
}

fun formatDateCompact(timestamp: Long): String =
    SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))