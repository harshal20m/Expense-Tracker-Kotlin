package com.example.paisatracker.ui.details.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.ui.components.emoji.EmojiPickerSheet

// ── Add Category sheet ─────────────────────────────────────────────────────────

/**
 * "Add Category" bottom sheet.
 * Shares the same [EmojiPickerSheet] component used by projects for a consistent UX.
 */
@Composable
fun AddCategorySheetContent(
    onCancel: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit
) {
    var categoryName  by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📂") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

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
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(
                        2.dp,
                        if (showEmojiPicker) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(selectedEmoji, fontSize = 30.sp)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("New Category", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Tap icon to pick an emoji", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category Name") },
                placeholder = { Text("e.g., Groceries, Transport…") },
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
                Divider(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                EmojiPickerSheet(
                    contextHint = categoryName,
                    selectedEmoji = selectedEmoji,
                    onEmojiSelected = {
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
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onConfirm(categoryName.trim(), selectedEmoji) },
                enabled = categoryName.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(3.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Create", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Edit Category sheet ────────────────────────────────────────────────────────

@Composable
fun EditCategorySheetContent(
    currentName: String,
    currentEmoji: String,
    onCancel: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit
) {
    var editedName    by remember { mutableStateOf(currentName) }
    var selectedEmoji by remember { mutableStateOf(currentEmoji) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val hasChanges = editedName.isNotBlank() && (editedName != currentName || selectedEmoji != currentEmoji)

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
                    Text("Edit Category", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Tap icon to change emoji", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedTextField(
                value = editedName,
                onValueChange = { editedName = it },
                label = { Text("Category Name") },
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
                Divider(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                EmojiPickerSheet(
                    contextHint = editedName,
                    selectedEmoji = selectedEmoji,
                    onEmojiSelected = {
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
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onConfirm(editedName.trim(), selectedEmoji) },
                enabled = hasChanges,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(2.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Delete Category sheet ──────────────────────────────────────────────────────

@Composable
fun DeleteCategorySheetContent(
    categoryName: String,
    categoryEmoji: String,
    expenseCount: Int,
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
                Text("Delete Category?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Irreversible action", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }

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
                    Box(contentAlignment = Alignment.Center) { Text(categoryEmoji, fontSize = 22.sp) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (expenseCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        ) {
                            Text(
                                "⚠️  $expenseCount expense${if (expenseCount != 1) "s" else ""} will be deleted",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
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