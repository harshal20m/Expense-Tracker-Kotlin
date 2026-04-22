package com.example.paisatracker.ui.flap

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.PaisaTrackerViewModel

@Composable
fun CalculatorTab(viewModel: PaisaTrackerViewModel) {
    val showHistory by viewModel.calcShowHistory.collectAsState()

    AnimatedContent(
        targetState = showHistory,
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally { it } + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut(tween(150)))
            } else {
                (slideInHorizontally { -it } + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut(tween(150)))
            }
        },
        label = "calc_screens"
    ) { inHistory ->
        if (inHistory) {
            CalculatorHistoryScreen(viewModel = viewModel)
        } else {
            CalculatorMainScreen(viewModel = viewModel)
        }
    }
}

// ── Main calculator screen (display + keypad) ────────────────────────────────
@Composable
private fun CalculatorMainScreen(viewModel: PaisaTrackerViewModel) {
    val display    by viewModel.calcDisplay.collectAsState()
    val expression by viewModel.calcExpression.collectAsState()
    val history    by viewModel.calcHistory.collectAsState()
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // ── Display (CLEAN — no backspace icon) ──────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Expression line
                Text(
                    text = expression.ifEmpty { "" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Main number
                Text(
                    text = display,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.W300,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── History button (top-left) ─────────────────────────────────────
            if (history.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewModel.calcShowHistory.value = true }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = "History",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${history.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
            // ✅ Backspace icon REMOVED from display area
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Keypad with ⌫ key ─────────────────────────────────────────────────
        val keys = listOf(
            listOf("AC", "%", "⌫", "÷"),  // ✅ ⌫ only in numpad
            listOf("7",  "8",   "9", "×"),
            listOf("4",  "5",   "6", "−"),
            listOf("1",  "2",   "3", "+"),
            listOf("0",  "00",  ".", "=")
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        CalcKey(
                            label = key,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                processKey(key, viewModel)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── History screen (unchanged) ───────────────────────────────────────────────
@Composable
private fun CalculatorHistoryScreen(viewModel: PaisaTrackerViewModel) {
    val history by viewModel.calcHistory.collectAsState()
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { viewModel.calcShowHistory.value = false }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Calculator",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Clear all
            if (history.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.calcHistory.value = emptyList() },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Clear all",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
                    )
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "No history yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                items(history) { entry ->
                    HistoryEntry(
                        entry = entry,
                        onTap = {
                            val result = entry.substringAfterLast("= ").trim()
                            viewModel.calcDisplay.value = result
                            viewModel.calcExpression.value = ""
                            viewModel.calcShowHistory.value = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun HistoryEntry(entry: String, onTap: () -> Unit) {
    val eqIdx = entry.lastIndexOf("=")
    val exprPart   = if (eqIdx >= 0) entry.substring(0, eqIdx).trim() else entry
    val resultPart = if (eqIdx >= 0) entry.substring(eqIdx + 1).trim() else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = exprPart,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "= $resultPart",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End
            )
        }
    }
}

// ── Shared key composable ──────────────────────────────────────────────────────
@Composable
private fun CalcKey(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isOperator = label in listOf("÷", "×", "−", "+", "=")
    val isFunction = label in listOf("AC", "%", "⌫")

    val bgColor = when {
        label == "=" -> MaterialTheme.colorScheme.primary
        label == "⌫" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        isOperator   -> MaterialTheme.colorScheme.primaryContainer
        isFunction   -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        else         -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val textColor = when {
        label == "=" -> MaterialTheme.colorScheme.onPrimary
        label == "⌫" -> MaterialTheme.colorScheme.error
        isOperator   -> MaterialTheme.colorScheme.primary
        isFunction   -> MaterialTheme.colorScheme.secondary
        else         -> MaterialTheme.colorScheme.onSurface
    }

    val animBg by animateColorAsState(bgColor, spring(), label = "key_bg_$label")

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(animBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (label == "⌫") "⌫" else label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (isOperator || label == "=") FontWeight.W400 else FontWeight.W500,
                fontSize = if (label.length > 2) 14.sp else 18.sp
            ),
            color = textColor
        )
    }
}

// ── Calculator logic with backspace support ──────────────────────────────────
private fun processKey(key: String, vm: PaisaTrackerViewModel) {
    val current = vm.calcDisplay.value
    val expr    = vm.calcExpression.value

    when (key) {
        "AC"  -> {
            vm.calcDisplay.value = "0"
            vm.calcExpression.value = ""
        }

        "⌫"   -> {
            when {
                current == "0" || current.isEmpty() -> return
                current.length == 1 -> vm.calcDisplay.value = "0"
                else -> {
                    val newValue = current.dropLast(1)
                    vm.calcDisplay.value = if (newValue == "-" || newValue.isEmpty()) "0" else newValue
                }
            }
        }

        "+/-" -> {
            vm.calcDisplay.value = formatResult(-(current.toDoubleOrNull() ?: return))
        }

        "%"   -> {
            vm.calcDisplay.value = formatResult((current.toDoubleOrNull() ?: return) / 100.0)
        }

        "="   -> {
            if (expr.isEmpty()) return
            val full = "$expr $current"
            evalExpression(full)?.let { result ->
                val res = formatResult(result)
                vm.calcHistory.value = (vm.calcHistory.value + "$full = $res").takeLast(50)
                vm.calcDisplay.value = res
                vm.calcExpression.value = ""
            }
        }

        "÷", "×", "−", "+" -> {
            if (expr.isNotEmpty()) {
                val full = "$expr $current"
                val res = evalExpression(full)?.let { formatResult(it) } ?: current
                vm.calcExpression.value = "$res $key"
                vm.calcDisplay.value = "0"
            } else {
                vm.calcExpression.value = "$current $key"
                vm.calcDisplay.value = "0"
            }
        }

        "."  -> {
            if (!current.contains(".")) vm.calcDisplay.value = "$current."
        }

        "00" -> {
            if (current != "0" && current.length < 12) vm.calcDisplay.value = "${current}00"
        }

        else -> {
            vm.calcDisplay.value = when {
                current == "0" -> key
                current.length >= 12 -> current
                else -> "$current$key"
            }
        }
    }
}

private fun evalExpression(expr: String): Double? = try {
    val p = expr.trim().split("\\s+".toRegex())
    if (p.size != 3) null else {
        val a = p[0].toDoubleOrNull() ?: return null
        val b = p[2].toDoubleOrNull() ?: return null
        when (p[1]) {
            "÷" -> if (b == 0.0) null else a / b
            "×" -> a * b
            "−" -> a - b
            "+" -> a + b
            else -> null
        }
    }
} catch (e: Exception) { null }

private fun formatResult(v: Double): String =
    if (v == kotlin.math.floor(v) && !v.isInfinite() && kotlin.math.abs(v) < 1e12)
        v.toLong().toString()
    else String.format(java.util.Locale.ROOT, "%.8f", v).trimEnd('0').trimEnd('.')
