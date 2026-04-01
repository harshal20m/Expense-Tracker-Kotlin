package com.example.paisatracker.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.Budget
import com.example.paisatracker.data.BudgetPeriod

private val budgetEmojis = listOf(
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditMode = budgetToEdit != null
    val title = if (isEditMode) "Edit Budget" else "New Budget"
    val buttonText = if (isEditMode) "Update Budget" else "Create Budget"

    // Filter categories based on selected project
    val filteredCategories = if (selectedProjectId != null)
        categories.filter { it.projectId == selectedProjectId }
    else emptyList()

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

            // Project selection (Mandatory)
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
                            .menuAnchor()
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
                                    selectedCategoryId = null // Reset category when project changes
                                    projectError = false
                                    projectExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Category selection (Optional, but shown after project)
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
                            .menuAnchor()
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
                // Show message when project has no categories
                Text(
                    text = "No categories available for this project. You can create budgets without specifying a category.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Emoji picker
            Text(
                text = "Choose Icon",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                items(budgetEmojis) { e ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (emoji == e) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (emoji == e) 2.dp else 0.dp,
                                color = if (emoji == e) MaterialTheme.colorScheme.primary
                                else androidx.compose.ui.graphics.Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = e, fontSize = 22.sp)
                    }
                }
            }

            // Budget Name
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

            // Limit Amount
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

            // Period selection
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

            // Save button
            Button(
                onClick = {
                    projectError = selectedProjectId == null
                    nameError = name.isBlank()
                    val amount = amountText.toDoubleOrNull()
                    amountError = amount == null || amount <= 0

                    if (!projectError && !nameError && !amountError && amount != null) {
                        if (isEditMode && budgetToEdit != null) {
                            // Update existing budget
                            val updatedBudget = budgetToEdit.copy(
                                name = name.trim(),
                                emoji = emoji,
                                limitAmount = amount,
                                period = selectedPeriod,
                                projectId = selectedProjectId,
                                categoryId = selectedCategoryId
                            )
                            viewModel.updateBudget(updatedBudget)
                        } else {
                            // Create new budget
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