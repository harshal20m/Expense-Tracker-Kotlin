package com.example.paisatracker.ui.applock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.paisatracker.util.BiometricHelper

@Composable
fun AppLockScreen(
    onUnlock: () -> Unit,
    correctPin: String,
    biometricEnabled: Boolean
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled && BiometricHelper.isBiometricAvailable(context)) {
            (context as? FragmentActivity)?.let { activity ->
                BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    onSuccess = onUnlock,
                    onError = { error -> errorMessage = error }
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                "Enter PIN to unlock",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < enteredPin.length)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                ).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        row.forEach { number ->
                            NumberButton(
                                text = number,
                                onClick = {
                                    if (enteredPin.length < 4) {
                                        enteredPin += number
                                        errorMessage = null

                                        if (enteredPin.length == 4) {
                                            if (enteredPin == correctPin) {
                                                onUnlock()
                                            } else {
                                                errorMessage = "Incorrect PIN"
                                                enteredPin = ""
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (biometricEnabled && BiometricHelper.isBiometricAvailable(context)) {
                        IconButton(
                            onClick = {
                                (context as? FragmentActivity)?.let { activity ->
                                    BiometricHelper.showBiometricPrompt(
                                        activity = activity,
                                        onSuccess = onUnlock,
                                        onError = { error -> errorMessage = error }
                                    )
                                }
                            },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = "Use Biometric",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(72.dp))
                    }

                    NumberButton(
                        text = "0",
                        onClick = {
                            if (enteredPin.length < 4) {
                                enteredPin += "0"
                                errorMessage = null

                                if (enteredPin.length == 4) {
                                    if (enteredPin == correctPin) {
                                        onUnlock()
                                    } else {
                                        errorMessage = "Incorrect PIN"
                                        enteredPin = ""
                                    }
                                }
                            }
                        }
                    )

                    IconButton(
                        onClick = {
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                                errorMessage = null
                            }
                        },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            Icons.Default.Backspace,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NumberButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
