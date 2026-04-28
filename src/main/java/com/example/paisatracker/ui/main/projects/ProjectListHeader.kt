package com.example.paisatracker.ui.main.projects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.R

@Composable
fun ProjectListHeader(
    onAddProjectClick: () -> Unit,
    onQuickAddClick: () -> Unit,
    labelsVisible: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val animatedElevation by infiniteTransition.animateFloat(
        initialValue  = 4f,
        targetValue   = 12f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "elevation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.surface
                    ),
                    startY = 0f,
                    endY   = 150f
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // ── Logo + title ──────────────────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.weight(1f)
            ) {
                Surface(
                    shape           = CircleShape,
                    color           = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation  = 8.dp,
                    shadowElevation = 4.dp,
                    modifier        = Modifier.size(52.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter           = painterResource(id = R.drawable.ic_project_icon_header),
                            contentDescription= null,
                            tint              = Color.Unspecified,
                            modifier          = Modifier.size(30.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(
                        text          = "PaisaTracker",
                        style         = MaterialTheme.typography.headlineMedium,
                        fontWeight    = FontWeight.Bold,
                        color         = MaterialTheme.colorScheme.primary,
                        fontSize      = 22.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text       = "Track Your Projects",
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 13.sp
                    )
                }
            }

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.Bottom
            ) {
                // New Project
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FloatingActionButton(
                        onClick        = onAddProjectClick,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor   = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape          = RoundedCornerShape(16.dp),
                        modifier       = Modifier.size(52.dp),
                        elevation      = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Project",
                            modifier           = Modifier.size(26.dp)
                        )
                    }
                    AnimatedVisibility(
                        visible = labelsVisible,
                        enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit    = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Text(
                            text       = "Project",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines   = 1
                        )
                    }
                }

                // Quick Add Expense (pulsing glow)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FloatingActionButton(
                        onClick        = onQuickAddClick,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = MaterialTheme.colorScheme.onPrimary,
                        shape          = RoundedCornerShape(14.dp),
                        modifier       = Modifier.size(52.dp),
                        elevation      = FloatingActionButtonDefaults.elevation(
                            defaultElevation = animatedElevation.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.FlashOn,
                            contentDescription = "Quick Add Expense",
                            modifier           = Modifier.size(28.dp)
                        )
                    }
                    AnimatedVisibility(
                        visible = labelsVisible,
                        enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit    = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Text(
                            text       = "Expense",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines   = 1
                        )
                    }
                }
            }
        }
    }
}