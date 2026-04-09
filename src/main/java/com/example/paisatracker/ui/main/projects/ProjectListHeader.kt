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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.R

@Composable
fun ProjectListHeader(
    onAddProjectClick: () -> Unit,
    onQuickAddClick: () -> Unit,
    onScanClick: () -> Unit,
    labelsVisible: Boolean = true
) {
    // Pulsing glow for Quick Add
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val animatedElevation by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue  = 11f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "elev"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.surface
                    ),
                    startY = 0f, endY = 160f
                )
            )
    ) {
        // ── Row 1: Logo + App title ───────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Logo
            Surface(
                shape           = CircleShape,
                color           = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation  = 8.dp,
                shadowElevation = 4.dp,
                modifier        = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter           = painterResource(id = R.drawable.ic_project_icon_header),
                        contentDescription= null,
                        tint              = Color.Unspecified,
                        modifier          = Modifier.size(26.dp)
                    )
                }
            }
            // Title block
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text          = "PaisaTracker",
                    style         = MaterialTheme.typography.headlineMedium,
                    fontWeight    = FontWeight.Bold,
                    color         = MaterialTheme.colorScheme.primary,
                    fontSize      = 22.sp,
                    letterSpacing = (-0.5).sp,
                    maxLines      = 1
                )
                Text(
                    text       = "Track Your Projects",
                    style      = MaterialTheme.typography.bodySmall,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 12.sp,
                    maxLines   = 1
                )
            }
        }

        // ── Row 2: Action buttons — full width, always below title ───────────
        AnimatedVisibility(
            visible = true,
            enter   = slideInVertically { -it / 2 } + fadeIn(),
            exit    = slideOutVertically { -it / 2 } + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── New Project ───────────────────────────────────────────────
                HeaderActionButton(
                    label       = "New Project",
                    icon        = Icons.Default.Add,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor   = MaterialTheme.colorScheme.onSecondaryContainer,
                    elevation      = 4.dp,
                    showLabel      = labelsVisible,
                    modifier       = Modifier.weight(1f),
                    onClick        = onAddProjectClick
                )

                // ── Quick Add ─────────────────────────────────────────────────
                HeaderActionButton(
                    label       = "Quick Expense",
                    icon        = Icons.Outlined.FlashOn,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary,
                    elevation      = animatedElevation.dp,
                    showLabel      = labelsVisible,
                    modifier       = Modifier.weight(1f),
                    onClick        = onQuickAddClick,
                    isPrimary      = true
                )

                // ── Scan QR ───────────────────────────────────────────────────
                HeaderActionButton(
                    label       = "Scan & Pay",
                    icon        = Icons.Default.QrCodeScanner,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor   = MaterialTheme.colorScheme.onTertiaryContainer,
                    elevation      = 3.dp,
                    showLabel      = labelsVisible,
                    modifier       = Modifier.weight(1f),
                    onClick        = onScanClick
                )
            }
        }
    }
}

// ─── Reusable action button tile ──────────────────────────────────────────────

@Composable
private fun HeaderActionButton(
    label: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    elevation: Dp,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        onClick   = onClick
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = contentColor,
                modifier           = Modifier.size(if (isPrimary) 28.dp else 24.dp)
            )
            AnimatedVisibility(
                visible = showLabel,
                enter   = fadeIn(tween(200)) + slideInVertically { it },
                exit    = fadeOut(tween(150)) + slideOutVertically { it }
            ) {
                Text(
                    text      = label,
                    style     = MaterialTheme.typography.labelSmall,
                    fontWeight= if (isPrimary) FontWeight.Bold else FontWeight.Medium,
                    color     = contentColor.copy(alpha = 0.9f),
                    fontSize  = 10.sp,
                    maxLines  = 1
                )
            }
        }
    }
}