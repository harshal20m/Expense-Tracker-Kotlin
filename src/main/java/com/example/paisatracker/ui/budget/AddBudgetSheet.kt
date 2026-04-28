package com.example.paisatracker.ui.budget
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.Budget
import com.example.paisatracker.data.BudgetPeriod
import com.example.paisatracker.data.EmojiSuggestionEngine
import com.example.paisatracker.ui.common.EmojiChip
import com.example.paisatracker.ui.common.EmojiPickerSheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetSheet(
    viewModel: PaisaTrackerViewModel,
    onDismiss: () -> Unit,
    currencySymbol: String = "₹",
    budgetToEdit: Budget? = null
) {
    val projects by viewModel.getAllProjects().collectAsState(initial = emptyList())
    val categories by viewModel.getAllCategories().collectAsState(initial = emptyList())
    var selectedProjectId by remember { mutableStateOf(budgetToEdit?.projectId) }
    var selectedCategoryId by remember { mutableStateOf(budgetToEdit?.categoryId) }
    var name by remember { mutableStateOf(budgetToEdit?.name ?: "") }
    var emoji by remember { mutableStateOf(budgetToEdit?.emoji ?: "💰") }
    var amountText by remember { mutableStateOf(budgetToEdit?.limitAmount?.toString() ?: "") }
    var selectedPeriod by remember { mutableStateOf(budgetToEdit?.period ?: BudgetPeriod.MONTHLY) }
    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var projectError by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var resetTrackingFromNow by remember { mutableStateOf(false) }
    val suggestions by remember(name) {
        derivedStateOf {
            EmojiSuggestionEngine.suggest(name, maxResults = 8)
        }
    }
    val emojiScale by animateFloatAsState(
        targetValue = if (showEmojiPicker) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "emoji_scale"
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditMode = budgetToEdit != null
    val title = if (isEditMode) "Edit Budget" else "New Budget"
    val buttonText = if (isEditMode) "Update Budget" else "Create Budget"
    val filteredCategories = if (selectedProjectId != null) {
        categories.filter { it.projectId == selectedProjectId }
    } else {
        emptyList()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            if (projects.isNotEmpty()) {
                Text(
                    text = "Select Project *",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                var projectExpanded by remember { mutableStateOf(false) }
                val selectedProject = projects.find { it.id == selectedProjectId }
                ExposedDropdownMenuBox(
                    expanded = projectExpanded,
                    onExpandedChange = { projectExpanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = selectedProject?.let { "${it.emoji} ${it.name}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Project") },
                        placeholder = { Text("Select a project") },
                        isError = projectError,
                        supportingText = if (projectError) {
                            { Text("Please select a project") }
                        } else null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = projectExpanded,
                        onDismissRequest = { projectExpanded = false }
                    ) {
                        projects.forEach { project ->
                            DropdownMenuItem(
                                text = { Text("${project.emoji} ${project.name}") },
                                onClick = {
                                    selectedProjectId = project.id
                                    selectedCategoryId = null
                                    projectError = false
                                    projectExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            if (selectedProjectId != null && filteredCategories.isNotEmpty()) {
                Text(
                    text = "Select Category (Optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                var categoryExpanded by remember { mutableStateOf(false) }
                val selectedCategory = filteredCategories.find { it.id == selectedCategoryId }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.let { "${it.emoji} ${it.name}" } ?: "All Categories",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Categories") },
                            onClick = {
                                selectedCategoryId = null
                                categoryExpanded = false
                            }
                        )
                        filteredCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text("${category.emoji} ${category.name}") },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            } else if (selectedProjectId != null && filteredCategories.isEmpty()) {
                Text(
                    text = "No categories available for this project. You can create budgets without specifying a category.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    onClick = { showEmojiPicker = !showEmojiPicker },
                    shape = CircleShape,
                    modifier = Modifier.size(80.dp).scale(emojiScale),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(
                        2.dp,
                        if (showEmojiPicker) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(emoji, fontSize = 38.sp)
                    }
                }
                AnimatedVisibility(
                    visible = suggestions.isNotEmpty() && name.isNotBlank(),
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
                            items(suggestions) { e ->
                                EmojiChip(
                                    emoji = e,
                                    isSelected = e == emoji,
                                    onClick = {
                                        viewModel.recordEmojiUsage(e)
                                        emoji = e
                                    },
                                    size = 42
                                )
                            }
                        }
                    }
                }
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
                    EmojiPickerSheet(
                        contextHint = name,
                        selectedEmoji = emoji,
                        viewModel = viewModel,
                        onEmojiSelected = {
                            viewModel.recordEmojiUsage(it)
                            emoji = it
                            showEmojiPicker = false
                        }
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text("Budget Name") },
                placeholder = { Text("e.g. Monthly Groceries") },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Please enter a budget name") }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    amountError = false
                },
                label = { Text("Budget Limit") },
                placeholder = { Text("0.00") },
                prefix = { Text(currencySymbol) },
                isError = amountError,
                supportingText = if (amountError) {
                    { Text("Please enter a valid amount") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            Text(
                text = "Period",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BudgetPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period.displayName, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (isEditMode && selectedPeriod == BudgetPeriod.MONTHLY) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = resetTrackingFromNow,
                                onValueChange = { resetTrackingFromNow = it }
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(
                            checked = resetTrackingFromNow,
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Reset monthly tracking from now",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Old expenses before this edit will not be counted in this monthly budget anymore.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            Button(
                onClick = {
                    projectError = selectedProjectId == null
                    nameError = name.isBlank()
                    val amount = amountText.toDoubleOrNull()
                    amountError = amount == null || amount <= 0
                    if (!projectError && !nameError && !amountError && amount != null) {
                        if (isEditMode && budgetToEdit != null) {
                            val updatedBudget = budgetToEdit.copy(
                                name = name.trim(),
                                emoji = emoji,
                                limitAmount = amount,
                                period = selectedPeriod,
                                projectId = selectedProjectId,
                                categoryId = selectedCategoryId
                            )
                            viewModel.updateBudgetWithReset(
                                budget = updatedBudget,
                                resetTrackingFromNow = resetTrackingFromNow
                            )
                        } else {
                            viewModel.recordEmojiUsage(emoji)
                            viewModel.addBudget(
                                Budget(
                                    name = name.trim(),
                                    emoji = emoji,
                                    limitAmount = amount,
                                    period = selectedPeriod,
                                    projectId = selectedProjectId,
                                    categoryId = selectedCategoryId
                                )
                            )
                        }
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}