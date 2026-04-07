package com.example.paisatracker.ui.quickadd

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paisatracker.PaisaTrackerApplication
import com.example.paisatracker.R
import com.example.paisatracker.data.Category
import com.example.paisatracker.data.Project
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  QuickAddSheet  — entry point composable, drop this inside a ModalBottomSheet
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    currencySymbol: String = "₹"
) {
    val context     = LocalContext.current
    val application = context.applicationContext as PaisaTrackerApplication
    val vm: QuickAddViewModel = viewModel(
        factory = QuickAddViewModelFactory(application.repository)
    )

    val amount          by vm.amountText.collectAsState()
    val description     by vm.description.collectAsState()
    val selectedProject by vm.selectedProject.collectAsState()
    val selectedCat     by vm.selectedCategory.collectAsState()
    val paymentMethod   by vm.paymentMethod.collectAsState()
    val selectedDate    by vm.selectedDate.collectAsState()
    val isFormValid     by vm.isFormValid.collectAsState()
    val submitResult    by vm.submitResult.collectAsState()
    val recentProjects  by vm.recentProjects.collectAsState()
    val filteredCats    by vm.filteredCategories.collectAsState()
    val recentCats      by vm.recentCategories.collectAsState()
    val isCreatingCat   by vm.isCreatingCategory.collectAsState()

    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    // Focus amount on open
    val amountFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { amountFocus.requestFocus() }

    // Handle submit result side effects
    LaunchedEffect(submitResult) {
        if (submitResult is QuickAddResult.Success) {
            Toast.makeText(context, "Expense saved!", Toast.LENGTH_SHORT).show()
            vm.reset()
            onDismiss()
        } else if (submitResult is QuickAddResult.Error) {
            Toast.makeText(context, (submitResult as QuickAddResult.Error).message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Sheet header ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Add",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Amount ────────────────────────────────────────────────────────────
        SectionLabel(text = "Amount")
        AmountField(
            value = amount,
            onValueChange = { vm.amountText.value = it },
            currencySymbol = currencySymbol,
            focusRequester = amountFocus
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ── Description ───────────────────────────────────────────────────────
        SectionLabel(text = "Description")
        DescriptionField(
            value = description,
            onValueChange = { vm.description.value = it }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ── Divider ───────────────────────────────────────────────────────────
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(14.dp))

        // ── Project selection ─────────────────────────────────────────────────
        SectionLabel(text = "Project")
        ProjectSelector(
            selectedProject = selectedProject,
            recentProjects = recentProjects.take(5),
            onProjectSelected = { vm.onProjectSelected(it) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(14.dp))

        // ── Category selection ────────────────────────────────────────────────
        SectionLabel(
            text = "Category",
            trailing = if (selectedProject == null) "Select a project first" else null
        )
        CategorySelector(
            selectedProject = selectedProject,
            selectedCategory = selectedCat,
            filteredCategories = filteredCats,
            recentCategories = recentCats.take(5),
            isCreatingNew = isCreatingCat,
            newCategoryName = vm.newCategoryName.collectAsState().value,
            newCategoryEmoji = vm.newCategoryEmoji.collectAsState().value,
            onCategorySelected = { vm.onCategorySelected(it) },
            onStartCreate = { vm.startCreatingCategory() },
            onCancelCreate = { vm.cancelCreatingCategory() },
            onNewNameChange = { vm.newCategoryName.value = it },
            onNewEmojiChange = { vm.newCategoryEmoji.value = it },
            onConfirmCreate = { vm.confirmNewCategory() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ── Payment method ────────────────────────────────────────────────────
        SectionLabel(text = "Payment method")
        PaymentMethodSelector(
            selected = paymentMethod,
            onSelect = { vm.paymentMethod.value = it }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ── Date ──────────────────────────────────────────────────────────────
        SectionLabel(text = "Date")
        DateSelector(
            selectedDateMillis = selectedDate,
            onClick = { showDatePicker = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Save button ───────────────────────────────────────────────────────
        val isLoading = submitResult is QuickAddResult.Loading
        Button(
            onClick = { vm.submit(onSuccess = {}) },
            enabled = isFormValid && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "Save Expense",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    // ── Date picker dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { vm.selectedDate.value = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Modular section composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 0.dp)
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
            letterSpacing = 0.8.sp
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }
    }
}

// ── Amount ────────────────────────────────────────────────────────────────────
@Composable
private fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    currencySymbol: String,
    focusRequester: FocusRequester
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = currencySymbol,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.W300,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            BasicTextField(
                value = value,
                onValueChange = { new ->
                    // Allow only valid decimal input
                    if (new.isEmpty() || new.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        onValueChange(new)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            "0",
                            style = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                    inner()
                }
            )
        }
    }
}

// ── Description ───────────────────────────────────────────────────────────────
@Composable
private fun DescriptionField(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            maxLines = 2,
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        "What's this expense for?",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    )
                }
                inner()
            }
        )
    }
}

// ── Project selector ──────────────────────────────────────────────────────────
@Composable
private fun ProjectSelector(
    selectedProject: Project?,
    recentProjects: List<Project>,
    onProjectSelected: (Project) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // Dropdown trigger
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { dropdownExpanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedProject?.let { "${it.emoji} ${it.name}" } ?: "Select project…",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selectedProject != null) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selectedProject != null)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                recentProjects.forEach { project ->
                    DropdownMenuItem(
                        text = { Text("${project.emoji} ${project.name}") },
                        onClick = { onProjectSelected(project); dropdownExpanded = false }
                    )
                }
            }
        }

        // Recent project pills
        if (recentProjects.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(recentProjects) { project ->
                    SelectionPill(
                        label = "${project.emoji} ${project.name}",
                        isSelected = selectedProject?.id == project.id,
                        onClick = { onProjectSelected(project) }
                    )
                }
            }
        }
    }
}

// ── Category selector ─────────────────────────────────────────────────────────
@Composable
private fun CategorySelector(
    selectedProject: Project?,
    selectedCategory: Category?,
    filteredCategories: List<Category>,
    recentCategories: List<Category>,
    isCreatingNew: Boolean,
    newCategoryName: String,
    newCategoryEmoji: String,
    onCategorySelected: (Category) -> Unit,
    onStartCreate: () -> Unit,
    onCancelCreate: () -> Unit,
    onNewNameChange: (String) -> Unit,
    onNewEmojiChange: (String) -> Unit,
    onConfirmCreate: () -> Unit
) {
    val isEnabled = selectedProject != null
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {

        // Inline new-category creation card
        AnimatedVisibility(
            visible = isCreatingNew,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            NewCategoryInlineCard(
                name = newCategoryName,
                emoji = newCategoryEmoji,
                onNameChange = onNewNameChange,
                onEmojiChange = onNewEmojiChange,
                onConfirm = onConfirmCreate,
                onCancel = onCancelCreate
            )
        }

        // Dropdown trigger (hidden when creating)
        if (!isCreatingNew) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    )
                    .clickable(enabled = isEnabled) { dropdownExpanded = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedCategory?.let { "${it.emoji} ${it.name}" }
                            ?: if (isEnabled) "Select category…" else "—",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedCategory != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            selectedCategory != null -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isEnabled) 0.4f else 0.15f)
                    )
                }

                if (filteredCategories.isNotEmpty()) {
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        filteredCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.emoji} ${cat.name}") },
                                onClick = { onCategorySelected(cat); dropdownExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        // Pills row: filtered cats + recent + "New" pill
        if (isEnabled && !isCreatingNew) {
            Spacer(modifier = Modifier.height(8.dp))
            val pillsToShow = filteredCategories.take(4)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(pillsToShow) { cat ->
                    SelectionPill(
                        label = "${cat.emoji} ${cat.name}",
                        isSelected = selectedCategory?.id == cat.id,
                        onClick = { onCategorySelected(cat) }
                    )
                }
                item {
                    // "+" new category pill
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onStartCreate() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "+ New",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Inline new-category card ───────────────────────────────────────────────────
@Composable
private fun NewCategoryInlineCard(
    name: String,
    emoji: String,
    onNameChange: (String) -> Unit,
    onEmojiChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val commonEmojis = listOf("📂","🛒","🍔","🚗","💊","📚","🎮","☕","✈️","👗","🏋️","💡","🎬","🔧","🎵","📱")
    var showEmojiPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Emoji + name row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Emoji picker trigger
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { showEmojiPicker = !showEmojiPicker },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 20.sp)
            }

            // Name field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        if (name.isEmpty()) Text(
                            "Category name",
                            style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        )
                        inner()
                    }
                )
            }

            // Cancel
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }

            // Confirm
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (name.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    .clickable(enabled = name.isNotBlank(), onClick = onConfirm),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp),
                    tint = if (name.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
            }
        }

        // Emoji quick-pick row
        AnimatedVisibility(visible = showEmojiPicker) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(commonEmojis) { e ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (emoji == e) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onEmojiChange(e); showEmojiPicker = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(e, fontSize = 18.sp)
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
@Composable
fun getPaymentMethodIconRes(method: String): Int? {
    return when (method) {
        "UPI" -> R.drawable.ic_upi_payment_icon
        "PhonePe" -> R.drawable.ic_phonepe_icon
        "GPay" -> R.drawable.ic_google_pay_icon
        "Paytm" -> R.drawable.ic_paytm_icon
        "Cash" -> R.drawable.ic_cash_icon
        "Card" -> R.drawable.ic_card_icon
        else -> null
    }
}
// ── Payment method pills ──────────────────────────────────────────────────────
@Composable
fun PaymentMethodSelector(selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(PAYMENT_METHODS) { method ->
            val isSelected = selected == method
            val iconRes = getPaymentMethodIconRes(method)

            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(method) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (iconRes != null) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = method,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        text = method,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ── Date selector ─────────────────────────────────────────────────────────────
@Composable
private fun DateSelector(selectedDateMillis: Long, onClick: () -> Unit) {
    val label = remember(selectedDateMillis) {
        val today = Calendar.getInstance()
        val selected = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        when {
            today.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR)
                    && today.get(Calendar.YEAR) == selected.get(Calendar.YEAR) -> "Today"
            else -> SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
                .format(Date(selectedDateMillis))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            Icons.Default.DateRange,
            contentDescription = "Pick date",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
    }
}

// ── Reusable pill composable ──────────────────────────────────────────────────
@Composable
private fun SelectionPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}