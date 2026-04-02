package com.example.paisatracker.ui.flap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.FlapNote
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesTab(viewModel: PaisaTrackerViewModel) {
    val notes by viewModel.flapNotes.collectAsState()

    // Compose state for the "add new note" inline input
    var isAddingNote by remember { mutableStateOf(false) }
    var newNoteText  by remember { mutableStateOf("") }

    // Which note is currently being edited (null = none)
    var editingId   by remember { mutableStateOf<String?>(null) }
    var editingText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Header row ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (notes.isEmpty()) "No notes yet" else "${notes.size} note${if (notes.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            // Add button — only show when not already adding
            if (!isAddingNote) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable {
                            editingId = null          // close any active edit
                            isAddingNote = true
                            newNoteText = ""
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add note",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Add note",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ── Inline "add note" input ───────────────────────────────────────────
        AnimatedVisibility(
            visible = isAddingNote,
            enter = slideInVertically { -it / 2 } + fadeIn(tween(180)),
            exit  = slideOutVertically { -it / 2 } + fadeOut(tween(120))
        ) {
            NoteInputCard(
                text = newNoteText,
                onTextChange = { newNoteText = it },
                onConfirm = {
                    viewModel.addFlapNote(newNoteText)
                    newNoteText = ""
                    isAddingNote = false
                },
                onCancel = {
                    newNoteText = ""
                    isAddingNote = false
                },
                placeholder = "Write a note…"
            )
        }

        // ── Note cards ────────────────────────────────────────────────────────
        if (notes.isEmpty() && !isAddingNote) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.StickyNote2,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                    )
                    Text(
                        text = "Tap \"Add note\" to get started",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        notes.forEach { note ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically { -it / 2 } + fadeIn(tween(180)),
                exit  = slideOutVertically { -it / 2 } + fadeOut(tween(100))
            ) {
                if (editingId == note.id) {
                    // Inline edit card
                    NoteInputCard(
                        text = editingText,
                        onTextChange = { editingText = it },
                        onConfirm = {
                            viewModel.editFlapNote(note.id, editingText)
                            editingId = null
                        },
                        onCancel = { editingId = null },
                        placeholder = "Edit note…"
                    )
                } else {
                    // Display card
                    NoteCard(
                        note = note,
                        onEdit = {
                            isAddingNote = false
                            editingId = note.id
                            editingText = note.text
                        },
                        onDelete = { viewModel.deleteFlapNote(note.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

// ── Compact note display card ──────────────────────────────────────────────────
@Composable
private fun NoteCard(
    note: FlapNote,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeLabel = remember(note.createdAt) {
        val cal = Calendar.getInstance()
        val noteDay = Calendar.getInstance().apply { timeInMillis = note.createdAt }
        val isToday = cal.get(Calendar.DAY_OF_YEAR) == noteDay.get(Calendar.DAY_OF_YEAR) &&
                cal.get(Calendar.YEAR) == noteDay.get(Calendar.YEAR)
        if (isToday) SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(note.createdAt))
        else SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(note.createdAt))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Note text (takes all available width)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                fontSize = 10.sp
            )
        }

        // Action icons — compact, stacked vertically
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Edit
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
            // Delete
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                )
            }
        }
    }
}

// ── Inline input card (used for both add and edit) ─────────────────────────────
@Composable
private fun NoteInputCard(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    placeholder: String
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Text field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (text.isEmpty()) {
                Text(
                    text = placeholder,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                    )
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = 4,
                decorationBox = { it() }
            )
        }

        // Confirm / Cancel row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Confirm
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (text.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    .clickable(enabled = text.isNotBlank(), onClick = onConfirm)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Save",
                        modifier = Modifier.size(13.dp),
                        tint = if (text.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        "Save",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (text.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}