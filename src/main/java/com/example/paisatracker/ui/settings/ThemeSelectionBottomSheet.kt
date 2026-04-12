package com.example.paisatracker.ui.settings

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.data.AppTheme
import com.example.paisatracker.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionBottomSheet(
    currentTheme: AppTheme,
    onDismiss: () -> Unit,
    onThemeSelected: (AppTheme) -> Unit
) {
    val showDynamic   = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scope         = rememberCoroutineScope()
    val sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lazyRowState  = rememberLazyListState()

    val availableThemes = remember(showDynamic) {
        AppTheme.values().filter { it != AppTheme.WALLPAPER_ORIENTED || showDynamic }
    }

    var hoveredTheme by remember { mutableStateOf(currentTheme) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle       = { BottomSheetDefaults.DragHandle() },
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Column(
                modifier            = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Choose your style", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Tap a theme to preview, tap again to apply", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }

            Spacer(Modifier.height(20.dp))

            // ── Large animated preview card ───────────────────────────────────
            AnimatedContent(
                targetState  = hoveredTheme,
                transitionSpec = {
                    (scaleIn(spring(Spring.DampingRatioMediumBouncy), 0.92f) + fadeIn(tween(180))) togetherWith
                            fadeOut(tween(100))
                },
                label = "theme_preview"
            ) { theme ->
                val previewColors = getThemePreviewColors(theme)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(100.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(previewColors.getOrElse(3) { MaterialTheme.colorScheme.surface })
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(22.dp)
                        )
                ) {
                    Row(modifier = Modifier.fillMaxSize().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Colour blobs preview
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                previewColors.take(2).forEach { c ->
                                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(c))
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                previewColors.drop(2).take(2).forEach { c ->
                                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(c))
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                theme.themeName,
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = previewColors.firstOrNull()?.let { pickTextColor(it) } ?: MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                getThemeDescription(theme),
                                style    = MaterialTheme.typography.bodySmall,
                                color    = (previewColors.firstOrNull()?.let { pickTextColor(it) } ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.6f)
                            )
                            if (theme == currentTheme) {
                                Surface(shape = RoundedCornerShape(20.dp), color = previewColors.getOrElse(0) { MaterialTheme.colorScheme.primary }.copy(alpha = 0.2f)) {
                                    Text("Current", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = previewColors.getOrElse(0) { MaterialTheme.colorScheme.primary }, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Horizontal scrolling swatch row ───────────────────────────────
            LazyRow(
                state               = lazyRowState,
                contentPadding      = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(availableThemes, key = { it.name }) { theme ->
                    ThemeSwatch(
                        theme      = theme,
                        isSelected = theme == currentTheme,
                        isHovered  = theme == hoveredTheme,
                        onHover    = { hoveredTheme = theme },
                        onSelect   = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onThemeSelected(theme) }
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Confirm button ────────────────────────────────────────────────
            Button(
                onClick  = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onThemeSelected(hoveredTheme) }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 24.dp),
                shape    = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Apply ${hoveredTheme.themeName}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Single theme swatch (compact circle + name) ─────────────────────────────

@Composable
private fun ThemeSwatch(
    theme: AppTheme,
    isSelected: Boolean,
    isHovered: Boolean,
    onHover: () -> Unit,
    onSelect: () -> Unit
) {
    val previewColors = getThemePreviewColors(theme)
    val scale by animateFloatAsState(
        targetValue    = if (isHovered) 1.08f else 1f,
        animationSpec  = spring(Spring.DampingRatioMediumBouncy),
        label          = "swatch_scale"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label         = "swatch_border"
    )

    Column(
        modifier            = Modifier
            .width(64.dp)
            .scale(scale)
            .clickable {
                if (isHovered) onSelect() else onHover()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Swatch circle with 4-quadrant colour split
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .border(2.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(previewColors.getOrElse(0) { Color.Gray }))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(previewColors.getOrElse(2) { Color.LightGray }))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(previewColors.getOrElse(1) { Color.DarkGray }))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(previewColors.getOrElse(3) { Color.White }))
                }
            }
            if (isSelected) {
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        Text(
            text      = theme.themeName.split(" ").first(), // first word to keep short
            style     = MaterialTheme.typography.labelSmall,
            fontWeight= if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color     = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis
        )
    }
}

// ─── Helpers (kept from original) ────────────────────────────────────────────

/** Pick readable text colour for a given background */
private fun pickTextColor(bg: Color): Color {
    val luminance = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
    return if (luminance > 0.5f) Color(0xFF1A1A1A) else Color(0xFFF5F5F5)
}

@Composable
fun getThemePreviewColors(theme: AppTheme): List<Color> {
    val isDark = isSystemInDarkTheme()
    return when (theme) {
        AppTheme.LIGHT            -> listOf(Purple40, PurpleGrey40, Pink40, Color(0xFFF3EDF7))
        AppTheme.DARK             -> listOf(Purple80, PurpleGrey80, Pink80, Color(0xFF1D1B20))
        AppTheme.MIDNIGHT         -> listOf(MidnightPrimary, MidnightSecondary, MidnightTertiary, MidnightBackground)
        AppTheme.SOFT_LIGHT       -> listOf(SoftLightPrimary, SoftLightSecondary, SoftLightTertiary, SoftLightSurface)
        AppTheme.OCEAN            -> listOf(OceanPrimary, OceanSecondary, OceanTertiary, OceanSurface)
        AppTheme.SUNSET           -> listOf(SunsetPrimary, SunsetSecondary, SunsetTertiary, SunsetSurface)
        AppTheme.FOREST           -> listOf(ForestPrimary, ForestSecondary, ForestTertiary, ForestSurface)
        AppTheme.ROSE             -> listOf(RosePrimary, RoseSecondary, RoseTertiary, RoseSurface)
        AppTheme.LAVENDER         -> listOf(LavenderPrimary, LavenderSecondary, LavenderTertiary, LavenderSurface)
        AppTheme.DEEP_BLUE        -> listOf(DeepBluePrimary, DeepBlueSecondary, DeepBlueTertiary, DeepBlueSurface)
        AppTheme.COFFEE           -> listOf(CoffeePrimary, CoffeeSecondary, CoffeeTertiary, CoffeeSurface)
        AppTheme.SLATE            -> listOf(SlatePrimary, SlateSecondary, SlateTertiary, SlateSurface)
        AppTheme.SOFT_PINK        -> listOf(SoftPinkPrimary, SoftPinkSecondary, SoftPinkTertiary, SoftPinkSurface)
        AppTheme.HOT_PINK         -> listOf(HotPinkPrimary, HotPinkSecondary, HotPinkTertiary, HotPinkSurface)
        AppTheme.ROSE_GOLD        -> listOf(RoseGoldPrimary, RoseGoldSecondary, RoseGoldTertiary, RoseGoldSurface)
        AppTheme.SYSTEM_DEFAULT   -> listOf(Color(0xFF6750A4), Color(0xFF625B71), if (isDark) Color(0xFF4A4458) else Color(0xFFFFD8E4), if (isDark) Color(0xFF1D1B20) else Color(0xFFF3EDF7))
        AppTheme.WALLPAPER_ORIENTED -> listOf(Color(0xFF9CA3AF), Color(0xFF6B7280), Color(0xFF4B5563), Color(0xFF374151))
    }
}

@Composable
fun getThemeDescription(theme: AppTheme): String = when (theme) {
    AppTheme.LIGHT             -> "Bright and clean"
    AppTheme.DARK              -> "Easy on the eyes"
    AppTheme.MIDNIGHT          -> "Deep and moody"
    AppTheme.SOFT_LIGHT        -> "Warm and gentle"
    AppTheme.OCEAN             -> "Calm coastal vibes"
    AppTheme.SUNSET            -> "Vibrant and energetic"
    AppTheme.FOREST            -> "Fresh and natural"
    AppTheme.ROSE              -> "Soft romantic tones"
    AppTheme.LAVENDER          -> "Soothing pastel"
    AppTheme.DEEP_BLUE         -> "Serious and focused"
    AppTheme.COFFEE            -> "Rich earthy browns"
    AppTheme.SLATE             -> "Cool modern slate"
    AppTheme.SOFT_PINK         -> "Gentle pastel pink"
    AppTheme.HOT_PINK          -> "Bold and vibrant"
    AppTheme.ROSE_GOLD         -> "Elegant metallic blush"
    AppTheme.SYSTEM_DEFAULT    -> "Follows system"
    AppTheme.WALLPAPER_ORIENTED-> "Matches wallpaper"
}

// Keep old ThemeCard for any other callers
@Composable
fun ThemeCard(theme: AppTheme, isSelected: Boolean, onThemeSelected: (AppTheme) -> Unit) {
    ThemeSwatch(theme = theme, isSelected = isSelected, isHovered = isSelected, onHover = { onThemeSelected(theme) }, onSelect = { onThemeSelected(theme) })
}