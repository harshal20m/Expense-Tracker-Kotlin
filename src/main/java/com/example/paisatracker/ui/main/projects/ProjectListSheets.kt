package com.example.paisatracker.ui.main.projects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Emoji palette (shared across add + edit sheets) ───────────────────────────

internal val projectEmojis = listOf(
    "📁", "💼", "🏠", "🚗", "✈️", "🎓", "💰", "🏥", "🛒", "🎯",
    "📱", "💻", "🎨", "🎬", "🎮", "📚", "☕", "🍕", "🎉", "💡",
    "🔧", "🏃", "🎵", "📷", "🌟", "🔥", "💎", "🎁", "🌈", "⚡",
    "🧾", "💳", "🏦", "📈", "📉", "💱", "🪙",
    "🍔", "🥗", "🍱", "🍻", "🧃",
    "👗", "👟", "💄", "👜", "🎒",
    "🔌", "🚿", "🧹", "🪑",
    "🚌", "🚕", "🚆", "⛽", "🛞",
    "✏️", "📝", "🧠",
    "🧴", "🧼", "💊", "🩺", "🛌",
    "🎧", "🏟️", "🎢", "🎤",
    "🛠️", "🧾", "🧯",
    "🧳", "📅", "📊",
    "🧘", "🌿", "🐾", "🎈", "👶", "🎀", "🔑"
)

// ── Emoji picker row (reused in add + edit) ───────────────────────────────────

@Composable
private fun EmojiPickerRow(
    selectedEmoji: String,
    onEmojiSelected: (String) -> Unit,
    height: Int = 120,
    size: Int = 48,
    fontSize: Int = 24
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(height.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        LazyRow(
            modifier              = Modifier.fillMaxSize().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            items(projectEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(size.dp)
                        .clip(CircleShape)
                        .background(
                            if (emoji == selectedEmoji) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = fontSize.sp)
                }
            }
        }
    }
}

// ── Add project sheet ─────────────────────────────────────────────────────────

@Composable
fun AddProjectSheetContent(onCancel: () -> Unit, onConfirm: (String, String) -> Unit) {
    var projectName   by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📁") }

    Column(
        modifier              = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(20.dp)
    ) {
        // Preview circle
        Box(
            modifier = Modifier.size(72.dp).background(
                Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), MaterialTheme.colorScheme.primaryContainer)),
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(selectedEmoji, style = MaterialTheme.typography.displaySmall, fontSize = 40.sp)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Create New Project", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Choose an emoji and name", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Select Emoji", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            EmojiPickerRow(selectedEmoji = selectedEmoji, onEmojiSelected = { selectedEmoji = it })
        }

        OutlinedTextField(
            value         = projectName,
            onValueChange = { projectName = it },
            label         = { Text("Project Name") },
            placeholder   = { Text("e.g., Home Renovation") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(14.dp),
            colors        = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)), contentPadding = PaddingValues(vertical = 14.dp)) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = { onConfirm(projectName, selectedEmoji) }, enabled = projectName.isNotBlank(), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), elevation = ButtonDefaults.buttonElevation(4.dp, 8.dp), contentPadding = PaddingValues(vertical = 14.dp)) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Create", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Edit project sheet ────────────────────────────────────────────────────────

@Composable
fun EditProjectSheetContent(currentName: String, currentEmoji: String, onCancel: () -> Unit, onConfirm: (String, String) -> Unit) {
    var editedName    by remember { mutableStateOf(currentName) }
    var selectedEmoji by remember { mutableStateOf(currentEmoji) }

    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.size(64.dp).background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), MaterialTheme.colorScheme.primaryContainer)), CircleShape), contentAlignment = Alignment.Center) {
            Text(selectedEmoji, fontSize = 32.sp)
        }
        Text("Edit Project", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Update project details", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Select Emoji", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            EmojiPickerRow(selectedEmoji = selectedEmoji, onEmojiSelected = { selectedEmoji = it }, height = 100, size = 42, fontSize = 22)
        }

        OutlinedTextField(value = editedName, onValueChange = { editedName = it }, label = { Text("Project Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = { onConfirm(editedName, selectedEmoji) }, enabled = editedName.isNotBlank() && (editedName != currentName || selectedEmoji != currentEmoji), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), elevation = ButtonDefaults.buttonElevation(2.dp, 6.dp), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Delete project sheet ──────────────────────────────────────────────────────

@Composable
fun DeleteProjectSheetContent(projectName: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.size(64.dp).background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), MaterialTheme.colorScheme.errorContainer)), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
        }
        Text("Delete Project?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Are you sure you want to delete", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)) {
                Text("'$projectName'", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
            Text("This will permanently remove all categories and expenses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
                Text("Delete", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
fun EmptyProjectsState() {
    Column(
        modifier              = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center
    ) {
        Icon(Icons.Outlined.Folder, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        Spacer(Modifier.height(16.dp))
        Text("No Projects Yet", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("Create your first project to start\ntracking expenses", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}