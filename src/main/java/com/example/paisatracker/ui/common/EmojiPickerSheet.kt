package com.example.paisatracker.ui.components.emoji

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Smart, full-featured emoji picker with:
 * - Smart suggestions based on a context hint (e.g. project name)
 * - Live search across all emoji categories
 * - Category tabs for manual browsing
 * - Manual text input fallback for unsupported emojis
 * - Smooth animations throughout
 *
 * @param contextHint  The text to generate smart suggestions from (e.g. project name typed so far)
 * @param selectedEmoji Currently selected emoji
 * @param onEmojiSelected Callback when user taps an emoji
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmojiPickerSheet(
    contextHint: String,
    selectedEmoji: String,
    onEmojiSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    var manualInput by remember { mutableStateOf("") }
    var showManualField by remember { mutableStateOf(false) }

    val suggestions by remember(contextHint) {
        derivedStateOf {
            EmojiSuggestionEngine.suggest(contextHint, maxResults = 20)
        }
    }

    val searchResults by remember(searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) emptyList()
            else EmojiSuggestionEngine.suggest(searchQuery, maxResults = 30)
                .ifEmpty {
                    // Fallback: search across all categories
                    allEmojiCategories
                        .flatMap { it.emojis }
                        .filter { it.contains(searchQuery) }
                        .take(30)
                }
        }
    }

    val isSearching = searchQuery.isNotBlank()
    val categoryTabState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // ── Search bar ────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        showManualField = false
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search emojis…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        inner()
                    }
                )
                AnimatedVisibility(searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Content area ──────────────────────────────────────────────────────
        AnimatedContent(
            targetState = isSearching,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "emoji_content"
        ) { searching ->
            if (searching) {
                // Search results
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "${searchResults.size} results",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No emojis found.\nTry typing one manually below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            searchResults.forEach { emoji ->
                                EmojiChip(
                                    emoji = emoji,
                                    isSelected = emoji == selectedEmoji,
                                    onClick = { 
                                        EmojiSuggestionEngine.recordUsage(emoji)
                                        onEmojiSelected(emoji) 
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                Column {
                    // Smart suggestions or Popular emojis
                    if (suggestions.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(if (contextHint.isBlank()) "🔥" else "✨", fontSize = 14.sp)
                                Text(
                                    if (contextHint.isBlank()) "Popular emojis" else "Smart suggestions",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(end = 8.dp)
                            ) {
                                items(suggestions) { emoji ->
                                    EmojiChip(
                                        emoji = emoji,
                                        isSelected = emoji == selectedEmoji,
                                        onClick = { 
                                            EmojiSuggestionEngine.recordUsage(emoji)
                                            onEmojiSelected(emoji) 
                                        },
                                        size = 48
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // Category tabs
                    LazyRow(
                        state = categoryTabState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allEmojiCategories.indices.toList()) { index ->
                            val cat = allEmojiCategories[index]
                            val isSelected = index == selectedCategoryIndex
                            Surface(
                                onClick = { selectedCategoryIndex = index },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected)
                                    androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cat.icon, fontSize = 15.sp)
                                    if (isSelected) {
                                        Text(
                                            cat.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Category emoji grid
                    val currentCategory = allEmojiCategories.getOrNull(selectedCategoryIndex)
                    if (currentCategory != null) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                "${currentCategory.icon} ${currentCategory.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                currentCategory.emojis.forEach { emoji ->
                                    EmojiChip(
                                        emoji = emoji,
                                        isSelected = emoji == selectedEmoji,
                                        onClick = { 
                                            EmojiSuggestionEngine.recordUsage(emoji)
                                            onEmojiSelected(emoji) 
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Manual input row ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = { showManualField = !showManualField },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Text(
                    "✍️ Type emoji",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(showManualField) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.width(120.dp)
                    ) {
                        BasicTextField(
                            value = manualInput,
                            onValueChange = { manualInput = it.take(4) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 22.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                if (manualInput.isEmpty()) {
                                    Text("😊", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
                                }
                                inner()
                            }
                        )
                    }
                        Surface(
                        onClick = {
                            if (manualInput.isNotBlank()) {
                                EmojiSuggestionEngine.recordUsage(manualInput.trim())
                                onEmojiSelected(manualInput.trim())
                                manualInput = ""
                                showManualField = false
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (manualInput.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            "Use",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (manualInput.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

/**
 * A single tappable emoji chip with selection state and spring animation.
 */
@Composable
fun EmojiChip(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    size: Int = 44
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "emoji_scale"
    )

    Box(
        modifier = Modifier
            .size(size.dp)
            .scale(scale)
            .clip(RoundedCornerShape((size * 0.27f).dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = (size * 0.52f).sp
        )
    }
}