package com.example.paisatracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class SortOption {
    AMOUNT_LOW_HIGH,
    AMOUNT_HIGH_LOW,
    NAME_A_Z,
    NAME_Z_A
}

@Composable
fun SortDropdown(
    current: SortOption,
    onChange: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { expanded = true },
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = "Sort",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = when (current) {
                    SortOption.AMOUNT_LOW_HIGH -> "₹ ↑"
                    SortOption.AMOUNT_HIGH_LOW -> "₹ ↓"
                    SortOption.NAME_A_Z -> "A → Z"
                    SortOption.NAME_Z_A -> "Z → A"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Amount: Low → High") },
                onClick = {
                    onChange(SortOption.AMOUNT_LOW_HIGH)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Amount: High → Low") },
                onClick = {
                    onChange(SortOption.AMOUNT_HIGH_LOW)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Name: A → Z") },
                onClick = {
                    onChange(SortOption.NAME_A_Z)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Name: Z → A") },
                onClick = {
                    onChange(SortOption.NAME_Z_A)
                    expanded = false
                }
            )
        }
    }
}
