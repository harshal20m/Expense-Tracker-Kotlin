package com.example.paisatracker.ui.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.data.AppTheme
import com.example.paisatracker.ui.theme.CoffeePrimary
import com.example.paisatracker.ui.theme.CoffeeSecondary
import com.example.paisatracker.ui.theme.CoffeeSurface
import com.example.paisatracker.ui.theme.CoffeeTertiary
import com.example.paisatracker.ui.theme.DeepBluePrimary
import com.example.paisatracker.ui.theme.DeepBlueSecondary
import com.example.paisatracker.ui.theme.DeepBlueSurface
import com.example.paisatracker.ui.theme.DeepBlueTertiary
import com.example.paisatracker.ui.theme.ForestPrimary
import com.example.paisatracker.ui.theme.ForestSecondary
import com.example.paisatracker.ui.theme.ForestSurface
import com.example.paisatracker.ui.theme.ForestTertiary
import com.example.paisatracker.ui.theme.HotPinkPrimary
import com.example.paisatracker.ui.theme.HotPinkSecondary
import com.example.paisatracker.ui.theme.HotPinkSurface
import com.example.paisatracker.ui.theme.HotPinkTertiary
import com.example.paisatracker.ui.theme.LavenderPrimary
import com.example.paisatracker.ui.theme.LavenderSecondary
import com.example.paisatracker.ui.theme.LavenderSurface
import com.example.paisatracker.ui.theme.LavenderTertiary
import com.example.paisatracker.ui.theme.MidnightBackground
import com.example.paisatracker.ui.theme.MidnightPrimary
import com.example.paisatracker.ui.theme.MidnightSecondary
import com.example.paisatracker.ui.theme.MidnightTertiary
import com.example.paisatracker.ui.theme.OceanPrimary
import com.example.paisatracker.ui.theme.OceanSecondary
import com.example.paisatracker.ui.theme.OceanSurface
import com.example.paisatracker.ui.theme.OceanTertiary
import com.example.paisatracker.ui.theme.Pink40
import com.example.paisatracker.ui.theme.Pink80
import com.example.paisatracker.ui.theme.Purple40
import com.example.paisatracker.ui.theme.Purple80
import com.example.paisatracker.ui.theme.PurpleGrey40
import com.example.paisatracker.ui.theme.PurpleGrey80
import com.example.paisatracker.ui.theme.RoseGoldPrimary
import com.example.paisatracker.ui.theme.RoseGoldSecondary
import com.example.paisatracker.ui.theme.RoseGoldSurface
import com.example.paisatracker.ui.theme.RoseGoldTertiary
import com.example.paisatracker.ui.theme.RosePrimary
import com.example.paisatracker.ui.theme.RoseSecondary
import com.example.paisatracker.ui.theme.RoseSurface
import com.example.paisatracker.ui.theme.RoseTertiary
import com.example.paisatracker.ui.theme.SlatePrimary
import com.example.paisatracker.ui.theme.SlateSecondary
import com.example.paisatracker.ui.theme.SlateSurface
import com.example.paisatracker.ui.theme.SlateTertiary
import com.example.paisatracker.ui.theme.SoftLightPrimary
import com.example.paisatracker.ui.theme.SoftLightSecondary
import com.example.paisatracker.ui.theme.SoftLightSurface
import com.example.paisatracker.ui.theme.SoftLightTertiary
import com.example.paisatracker.ui.theme.SoftPinkPrimary
import com.example.paisatracker.ui.theme.SoftPinkSecondary
import com.example.paisatracker.ui.theme.SoftPinkSurface
import com.example.paisatracker.ui.theme.SoftPinkTertiary
import com.example.paisatracker.ui.theme.SunsetPrimary
import com.example.paisatracker.ui.theme.SunsetSecondary
import com.example.paisatracker.ui.theme.SunsetSurface
import com.example.paisatracker.ui.theme.SunsetTertiary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionBottomSheet(
    currentTheme: AppTheme,
    onDismiss: () -> Unit,
    onThemeSelected: (AppTheme) -> Unit
) {
    val context = LocalContext.current
    val showDynamicColorOption = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Filter themes based on Android version
    val availableThemes = remember(showDynamicColorOption) {
        AppTheme.values().filter { theme ->
            theme != AppTheme.WALLPAPER_ORIENTED || showDynamicColorOption
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Header with gradient accent ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Choose Your Style",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select a color theme that suits you",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Theme Cards Grid ───────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableThemes, key = { it.name }) { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = theme == currentTheme,
                        onThemeSelected = { selectedTheme ->
                            scope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                onThemeSelected(selectedTheme)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Current selection hint ─────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Current: ${currentTheme.themeName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    onThemeSelected: (AppTheme) -> Unit
) {
    // Get color palette for theme preview
    val previewColors = getThemePreviewColors(theme)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .clickable { onThemeSelected(theme) }
            .then(
                if (isSelected) {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                } else {
                    Modifier.shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Color palette preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                previewColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            // Theme name and selection indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = theme.themeName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Animated checkmark for selected state
                AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Theme description (subtle)
            Text(
                text = getThemeDescription(theme),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// Helper function to generate a representative color palette for each theme
@Composable
fun getThemePreviewColors(theme: AppTheme): List<Color> {
    val isDark = isSystemInDarkTheme()

    return when (theme) {
        AppTheme.LIGHT -> listOf(Purple40, PurpleGrey40, Pink40, Color(0xFFF3EDF7))
        AppTheme.DARK -> listOf(Purple80, PurpleGrey80, Pink80, Color(0xFF1D1B20))
        AppTheme.MIDNIGHT -> listOf(MidnightPrimary, MidnightSecondary, MidnightTertiary, MidnightBackground)
        AppTheme.SOFT_LIGHT -> listOf(SoftLightPrimary, SoftLightSecondary, SoftLightTertiary, SoftLightSurface)
        AppTheme.OCEAN -> listOf(OceanPrimary, OceanSecondary, OceanTertiary, OceanSurface)
        AppTheme.SUNSET -> listOf(SunsetPrimary, SunsetSecondary, SunsetTertiary, SunsetSurface)
        AppTheme.FOREST -> listOf(ForestPrimary, ForestSecondary, ForestTertiary, ForestSurface)
        AppTheme.ROSE -> listOf(RosePrimary, RoseSecondary, RoseTertiary, RoseSurface)
        AppTheme.LAVENDER -> listOf(LavenderPrimary, LavenderSecondary, LavenderTertiary, LavenderSurface)
        AppTheme.DEEP_BLUE -> listOf(DeepBluePrimary, DeepBlueSecondary, DeepBlueTertiary, DeepBlueSurface)
        AppTheme.COFFEE -> listOf(CoffeePrimary, CoffeeSecondary, CoffeeTertiary, CoffeeSurface)
        AppTheme.SLATE -> listOf(SlatePrimary, SlateSecondary, SlateTertiary, SlateSurface)
        AppTheme.SOFT_PINK -> listOf(SoftPinkPrimary, SoftPinkSecondary, SoftPinkTertiary, SoftPinkSurface)
        AppTheme.HOT_PINK -> listOf(HotPinkPrimary, HotPinkSecondary, HotPinkTertiary, HotPinkSurface)
        AppTheme.ROSE_GOLD -> listOf(RoseGoldPrimary, RoseGoldSecondary, RoseGoldTertiary, RoseGoldSurface)
        AppTheme.SYSTEM_DEFAULT -> listOf(
            Color(0xFF6750A4),
            Color(0xFF625B71),
            if (isDark) Color(0xFF4A4458) else Color(0xFFFFD8E4),
            if (isDark) Color(0xFF1D1B20) else Color(0xFFF3EDF7)
        )
        AppTheme.WALLPAPER_ORIENTED -> listOf(Color(0xFF9CA3AF), Color(0xFF6B7280), Color(0xFF4B5563), Color(0xFF374151))
    }
}

@Composable
fun getThemeDescription(theme: AppTheme): String {
    return when (theme) {
        AppTheme.LIGHT -> "Bright and clean"
        AppTheme.DARK -> "Easy on the eyes"
        AppTheme.MIDNIGHT -> "Deep and moody"
        AppTheme.SOFT_LIGHT -> "Warm and gentle"
        AppTheme.OCEAN -> "Calm coastal vibes"
        AppTheme.SUNSET -> "Vibrant and energetic"
        AppTheme.FOREST -> "Fresh and natural"
        AppTheme.ROSE -> "Soft romantic tones"
        AppTheme.LAVENDER -> "Soothing pastel"
        AppTheme.DEEP_BLUE -> "Serious and focused"
        AppTheme.COFFEE -> "Rich earthy browns"
        AppTheme.SLATE -> "Cool modern slate"
        AppTheme.SOFT_PINK -> "Gentle pastel pink"
        AppTheme.HOT_PINK -> "Bold and vibrant"
        AppTheme.ROSE_GOLD -> "Elegant metallic blush"
        AppTheme.SYSTEM_DEFAULT -> "Follows system"
        AppTheme.WALLPAPER_ORIENTED -> "Matches wallpaper"
    }
}