package com.example.paisatracker.ui.flap

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.PaisaTrackerViewModel

// ──────────────────────────────────────────────────────────────────────────────
//  QuickAccessFlap - Draggable & Resizable Game Bar Style
//  • Collapsed: small circular button on the right edge (draggable)
//  • Expanded: large panel that can be resized by dragging edges
// ──────────────────────────────────────────────────────────────────────────────

private val COLLAPSED_SIZE = 48.dp
private val MIN_EXPANDED_WIDTH = 280.dp
private val MAX_EXPANDED_WIDTH = 450.dp
private val DEFAULT_EXPANDED_WIDTH = 380.dp
private val MIN_EXPANDED_HEIGHT = 400.dp
private val MAX_EXPANDED_HEIGHT = 700.dp
private val DEFAULT_EXPANDED_HEIGHT = 560.dp
private val VERTICAL_PADDING = 60.dp // Safe area padding for top/bottom

@Composable
fun QuickAccessFlap(
    viewModel: PaisaTrackerViewModel,
) {
    val isExpanded by viewModel.isFlapExpanded.collectAsState()
    val selectedTab by viewModel.flapSelectedTab.collectAsState()
    // ✅ Collect persisted offset from ViewModel
    val persistedOffset by viewModel.flapButtonOffsetY.collectAsState()

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightDp = configuration.screenHeightDp.dp

    // ✅ Dynamic bounds calculation based on screen height
    val collapsedButtonSize = COLLAPSED_SIZE
    val minOffset = VERTICAL_PADDING
    val maxOffset = screenHeightDp - collapsedButtonSize - VERTICAL_PADDING

    // ✅ Initial position: vertically centered (clamped to safe bounds)
    val initialOffset = ((screenHeightDp - collapsedButtonSize) / 2)
        .coerceIn(minOffset, maxOffset)

    // ✅ Track button position: use persisted value or fallback to calculated center
    var buttonOffsetY by remember {
        mutableStateOf(
            persistedOffset.takeIf { !it.isNaN() } ?: initialOffset.value
        )
    }

    // Track drag state for button (in pixels)
    var isDraggingButton by remember { mutableStateOf(false) }
    var dragButtonDeltaPx by remember { mutableStateOf(0f) }

    // Track panel size (when expanded)
    var panelWidth by remember { mutableStateOf(DEFAULT_EXPANDED_WIDTH) }
    var panelHeight by remember { mutableStateOf(DEFAULT_EXPANDED_HEIGHT) }
    var isResizing by remember { mutableStateOf(false) }
    var resizeStartSize by remember { mutableStateOf(0.dp) }

    BackHandler(enabled = isExpanded) {
        viewModel.isFlapExpanded.value = false
    }

    // Animate width and height for expansion
    val flapWidth by animateDpAsState(
        targetValue = if (isExpanded) panelWidth else COLLAPSED_SIZE,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "flapWidth"
    )

    val flapHeight by animateDpAsState(
        targetValue = if (isExpanded) panelHeight else COLLAPSED_SIZE,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "flapHeight"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 24.dp else COLLAPSED_SIZE / 2,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "cornerRadius"
    )

    // ✅ Current Y offset with proper px→dp conversion for dragging
    val currentYOffset = if (isExpanded) {
        0.dp
    } else {
        // Convert pixel delta to dp using density
        val dragDeltaDp = with(density) { dragButtonDeltaPx.toDp() }
        val rawOffset = buttonOffsetY.dp + dragDeltaDp
        val clampedOffset = rawOffset.coerceIn(minOffset, maxOffset)

        // ✅ Update local state + persist to ViewModel when drag ends
        if (!isDraggingButton && dragButtonDeltaPx != 0f) {
            buttonOffsetY = clampedOffset.value
            dragButtonDeltaPx = 0f
            viewModel.updateFlapButtonOffsetY(clampedOffset.value)
        }
        clampedOffset
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Flap container
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .offset(y = currentYOffset)
                    .width(flapWidth)
                    .height(flapHeight)
                    .then(
                        if (isExpanded) {
                            Modifier.coloredShadow(
                                color = Color.Black.copy(alpha = 0.15f),
                                borderRadius = cornerRadius,
                                blurRadius = 20.dp,
                                offsetX = (-8).dp
                            )
                        } else {
                            Modifier.coloredShadow(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                borderRadius = cornerRadius,
                                blurRadius = 12.dp,
                                offsetX = (-4).dp
                            )
                        }
                    )
                    .clip(RoundedCornerShape(
                        topStart = cornerRadius,
                        bottomStart = cornerRadius,
                        topEnd = 0.dp,
                        bottomEnd = 0.dp
                    ))
                    .background(MaterialTheme.colorScheme.surface)
                    .then(
                        if (!isExpanded) {
                            // ✅ Draggable button when collapsed
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { isDraggingButton = true },
                                    onDragEnd = { isDraggingButton = false },
                                    onDragCancel = {
                                        isDraggingButton = false
                                        dragButtonDeltaPx = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        // dragAmount.y is in pixels - store as-is
                                        dragButtonDeltaPx += dragAmount.y
                                    }
                                )
                            }
                        } else {
                            // ✅ Resizable panel when expanded
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        isResizing = true
                                        resizeStartSize = panelWidth
                                    },
                                    onDragEnd = { isResizing = false },
                                    onDragCancel = { isResizing = false },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        // Horizontal drag to resize width (dragAmount.x in pixels)
                                        val dragDeltaDp = with(density) { dragAmount.x.toDp() }
                                        val newWidth = (resizeStartSize - dragDeltaDp)
                                            .coerceIn(MIN_EXPANDED_WIDTH, MAX_EXPANDED_WIDTH)
                                        panelWidth = newWidth
                                        resizeStartSize = newWidth
                                    }
                                )
                            }
                        }
                    )
            ) {
                if (isExpanded) {
                    ExpandedFlapContent(
                        viewModel = viewModel,
                        selectedTab = selectedTab,
                        onResize = { deltaPx ->
                            // ✅ Vertical resize via drag handle (delta in pixels)
                            val deltaDp = with(density) { deltaPx.toDp() }
                            val newHeight = (panelHeight - deltaDp)
                                .coerceIn(MIN_EXPANDED_HEIGHT, MAX_EXPANDED_HEIGHT)
                            panelHeight = newHeight
                        },
                        onClose = { viewModel.isFlapExpanded.value = false }
                    )
                } else {
                    CollapsedFlapButton(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

// ── Custom shadow helper ──────────────────────────────────────────────────────────
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

// ── Collapsed: small circular button on right edge ────────────────────────────
@Composable
private fun CollapsedFlapButton(
    viewModel: PaisaTrackerViewModel
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { viewModel.isFlapExpanded.value = true },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Calculate,
            contentDescription = "Quick Access",
            tint = primaryColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

// ── Expanded: full content panel with resize handles ────────────────────────────────────
@Composable
private fun ExpandedFlapContent(
    viewModel: PaisaTrackerViewModel,
    selectedTab: Int,
    onResize: (Float) -> Unit, // delta in pixels
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // ── Top resize / Close handle ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        // dragAmount is in pixels
                        onResize(dragAmount)
                    }
                }
        ) {
            // Row with drag handle AND close icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drag handle on the left
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                )

                // Close icon on the right
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Resize hint text
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "↕ Drag to resize | Click ✕ to close",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Tab row (larger) ────────────────────────────────────────────────
        FlapTabRow(
            selectedTab = selectedTab,
            onTabSelected = { viewModel.flapSelectedTab.value = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

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

        // ── Bottom resize handle ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(top = 8.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        // Inverted for bottom handle (dragAmount in pixels)
                        onResize(-dragAmount)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            )
        }
    }
}

// ── Tab row (larger) ──────────────────────────────────────────────────────────
@Composable
private fun FlapTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Calculator", "Notes")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    .height(44.dp)
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor,
                    fontSize = 14.sp
                )
            }
        }
    }
}