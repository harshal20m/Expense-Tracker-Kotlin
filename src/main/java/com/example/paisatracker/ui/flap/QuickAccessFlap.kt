package com.example.paisatracker.ui.flap

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.paisatracker.PaisaTrackerViewModel

// ──────────────────────────────────────────────────────────────────────────────
//  QuickAccessFlap
//  • Collapsed: a narrow curved notch sitting just above the bottom navbar
//  • Expanded: a floating card with margins on all sides, slides up over screen
//  • State lives in PaisaTrackerViewModel → survives nav changes
// ──────────────────────────────────────────────────────────────────────────────

private val COLLAPSED_HEIGHT = 44.dp
private val EXPANDED_HEIGHT = 520.dp
private val SIDE_MARGIN = 10.dp
private val BOTTOM_MARGIN_EXPANDED = 8.dp   // gap above navbar when expanded
private val CORNER_RADIUS_COLLAPSED = 20.dp
private val CORNER_RADIUS_EXPANDED = 28.dp

@Composable
fun QuickAccessFlap(
    viewModel: PaisaTrackerViewModel,
    bottomNavHeight: Dp = 104.dp  // bottom nav pill height + its padding (~72 + 24 + 8)
) {
    val isExpanded by viewModel.isFlapExpanded.collectAsState()
    val selectedTab by viewModel.flapSelectedTab.collectAsState()

    // Intercept back press when expanded
    BackHandler(enabled = isExpanded) {
        viewModel.isFlapExpanded.value = false
    }

    // Animated values
    val flapHeight by animateDpAsState(
        targetValue = if (isExpanded) EXPANDED_HEIGHT else COLLAPSED_HEIGHT,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "flapHeight"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) CORNER_RADIUS_EXPANDED else CORNER_RADIUS_COLLAPSED,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "cornerRadius"
    )

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 0.35f else 0f,
        animationSpec = tween(300),
        label = "scrimAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // ── Scrim (dims background when expanded) ──────────────────────────
        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.isFlapExpanded.value = false
                    }
            )
        }

        // ── Collapsed: small centered pill button ──────────────────────────
        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = bottomNavHeight + 6.dp),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(32.dp)
                        .coloredShadow(
                            color = primaryColor.copy(alpha = 0.18f),
                            borderRadius = 50.dp,
                            blurRadius = 10.dp
                        )
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { viewModel.isFlapExpanded.value = true }
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Active tab — primary colored
                        Text(
                            text = if (selectedTab == 0) "Calculator" else "Notes",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        // Divider dot
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                        )
                        // Other tab — muted
                        Text(
                            text = if (selectedTab == 0) "Notes" else "Calculator",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }

        // ── Expanded: full-width panel ──────────────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(180, 40)),
            exit = fadeOut(tween(120))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = SIDE_MARGIN,
                        end = SIDE_MARGIN,
                        bottom = bottomNavHeight + BOTTOM_MARGIN_EXPANDED
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                val surfaceColor = MaterialTheme.colorScheme.surface
                val primaryColor = MaterialTheme.colorScheme.primary

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(flapHeight)
                        .coloredShadow(
                            color = primaryColor.copy(alpha = 0.25f),
                            borderRadius = cornerRadius,
                            blurRadius = 28.dp,
                            offsetY = (-4).dp
                        )
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(surfaceColor)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount > 40f) viewModel.isFlapExpanded.value = false
                            }
                        }
                ) {
                    ExpandedFlapContent(
                        viewModel = viewModel,
                        selectedTab = selectedTab
                    )
                }
            }
        }
    }
}


// ── Expanded: drag handle + tab selector + content ────────────────────────────
@Composable
private fun ExpandedFlapContent(
    viewModel: PaisaTrackerViewModel,
    selectedTab: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // ── Drag handle ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 0.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { viewModel.isFlapExpanded.value = false },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Tab row ────────────────────────────────────────────────────────
        FlapTabRow(
            selectedTab = selectedTab,
            onTabSelected = { viewModel.flapSelectedTab.value = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Tab content (scrollable) ────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(100))
                },
                label = "tabContent"
            ) { tab ->
                if (tab == 0) {
                    CalculatorTab(viewModel = viewModel)
                } else {
                    NotesTab(viewModel = viewModel)
                }
            }
        }
    }
}

// ── Tab row ────────────────────────────────────────────────────────────────────
@Composable
private fun FlapTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Calculator", "Notes")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val isSelected = selectedTab == index

            val tabBg by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "tabBg_$index"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                animationSpec = tween(200),
                label = "tabText_$index"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tabBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

// ── Soft shadow helper ─────────────────────────────────────────────────────────
private fun Modifier.coloredShadow(
    color: Color,
    borderRadius: Dp,
    blurRadius: Dp,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp
) = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                this.color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    blurRadius.toPx(),
                    offsetX.toPx(),
                    offsetY.toPx(),
                    color.toArgb()
                )
            }
        }
        canvas.drawRoundRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height,
            radiusX = borderRadius.toPx(),
            radiusY = borderRadius.toPx(),
            paint = paint
        )
    }
}