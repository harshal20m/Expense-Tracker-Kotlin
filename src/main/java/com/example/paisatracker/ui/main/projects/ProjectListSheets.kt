package com.example.paisatracker.ui.main.projects

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.ui.common.EmojiPickerSheet
import com.example.paisatracker.data.EmojiSuggestionEngine
import com.example.paisatracker.ui.common.EmojiChip

// ── Add project sheet ─────────────────────────────────────────────────────────

/**
 * Redesigned "Create Project" bottom sheet featuring:
 * - Live preview avatar that updates as user types
 * - Smart emoji suggestions powered by the project name
 * - Full emoji picker with search + categories + manual input
 * - Polished Material 3 design with gradients
 */
@Composable
fun AddProjectSheetContent(
    viewModel: PaisaTrackerViewModel,
    onCancel: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit
) {
    var projectName   by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📁") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val suggestions by remember(projectName) {
        derivedStateOf {
            EmojiSuggestionEngine.suggest(projectName, maxResults = 8)
        }
    }

    val emojiScale by animateFloatAsState(
        targetValue = if (showEmojiPicker) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "emoji_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live preview avatar
            Surface(
                onClick = { showEmojiPicker = !showEmojiPicker },
                shape = CircleShape,
                modifier = Modifier.size(88.dp).scale(emojiScale),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(
                    2.dp,
                    if (showEmojiPicker) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(selectedEmoji, fontSize = 42.sp)
                }
            }

            // Smart suggestions moved below emoji icon
            AnimatedVisibility(
                visible = suggestions.isNotEmpty() && projectName.isNotBlank(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("✨", fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Suggested Icons",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(suggestions) { emoji ->
                            EmojiChip(
                                emoji = emoji,
                                isSelected = emoji == selectedEmoji,
                                onClick = { 
                                    viewModel.recordEmojiUsage(emoji)
                                    selectedEmoji = emoji 
                                },
                                size = 42
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Create New Project",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tap the icon to choose an emoji",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Name field
            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text("Project Name") },
                placeholder = { Text("e.g., Home Renovation, Trip to Goa…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        // ── Emoji picker (collapsible) ─────────────────────────────────────
        AnimatedVisibility(
            visible = showEmojiPicker,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(12.dp))

                // Section title
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Pick an Emoji", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            "700+ emojis",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                EmojiPickerSheet(
                    contextHint = projectName,
                    selectedEmoji = selectedEmoji,
                    viewModel = viewModel,
                    onEmojiSelected = {
                        viewModel.recordEmojiUsage(it)
                        selectedEmoji = it
                        showEmojiPicker = false
                    }
                )
            }
        }

        // ── Action buttons ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onConfirm(projectName.trim(), selectedEmoji) },
                enabled = projectName.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Create Project", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Edit project sheet ────────────────────────────────────────────────────────

@Composable
fun EditProjectSheetContent(
    currentName: String,
    currentEmoji: String,
    viewModel: PaisaTrackerViewModel,
    onCancel: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit
) {
    var editedName    by remember { mutableStateOf(currentName) }
    var selectedEmoji by remember { mutableStateOf(currentEmoji) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val hasChanges = editedName.isNotBlank() && (editedName != currentName || selectedEmoji != currentEmoji)

    val suggestions by remember(editedName) {
        derivedStateOf {
            EmojiSuggestionEngine.suggest(editedName, maxResults = 8)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    onClick = { showEmojiPicker = !showEmojiPicker },
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(
                        2.dp,
                        if (showEmojiPicker) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(selectedEmoji, fontSize = 30.sp)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Edit Project", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Tap icon to change emoji", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Smart suggestions moved below emoji icon row
            AnimatedVisibility(
                visible = suggestions.isNotEmpty() && editedName.isNotBlank(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("✨", fontSize = 12.sp)
                        Text(
                            "Suggested Icons",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(suggestions) { emoji ->
                            EmojiChip(
                                emoji = emoji,
                                isSelected = emoji == selectedEmoji,
                                onClick = { 
                                    viewModel.recordEmojiUsage(emoji)
                                    selectedEmoji = emoji 
                                },
                                size = 42
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = editedName,
                onValueChange = { editedName = it },
                label = { Text("Project Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        AnimatedVisibility(
            visible = showEmojiPicker,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Pick an Emoji", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                EmojiPickerSheet(
                    contextHint = editedName,
                    selectedEmoji = selectedEmoji,
                    viewModel = viewModel,
                    onEmojiSelected = {
                        viewModel.recordEmojiUsage(it)
                        selectedEmoji = it
                        showEmojiPicker = false
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onConfirm(editedName.trim(), selectedEmoji) },
                enabled = hasChanges,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(2.dp)
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save Changes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Delete project sheet ──────────────────────────────────────────────────────

@Composable
fun DeleteProjectSheetContent(
    projectName: String,
    projectEmoji: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Danger icon strip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Delete Project?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("This cannot be undone", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }

        // Project preview card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(projectEmoji, fontSize = 22.sp) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        projectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "All categories and expenses will be permanently removed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Keep It", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Delete", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
fun EmptyProjectsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("📁", fontSize = 72.sp)
        Text("No Projects Yet", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Create your first project to start\ntracking your expenses",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}