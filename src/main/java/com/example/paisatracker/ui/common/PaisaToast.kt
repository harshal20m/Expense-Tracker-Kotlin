package com.example.paisatracker.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class ToastType {
    SUCCESS, ERROR, INFO, WARNING
}

data class ToastMessage(
    val message: String,
    val type: ToastType = ToastType.SUCCESS,
    val duration: Long = 3000L,
    val id: Long = System.currentTimeMillis()
)

@Composable
fun PaisaToast(
    toast: ToastMessage?,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            if (toast != null) {
                LaunchedEffect(toast.id) {
                    delay(toast.duration)
                    onDismiss()
                }

                val (backgroundColor, contentColor, icon) = when (toast.type) {
                    ToastType.SUCCESS -> Triple(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer,
                        Icons.Default.CheckCircle
                    )
                    ToastType.ERROR -> Triple(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.onErrorContainer,
                        Icons.Default.Error
                    )
                    ToastType.WARNING -> Triple(
                        Color(0xFFFFF4E5), // Soft Warning Amber
                        Color(0xFF663C00),
                        Icons.Default.Warning
                    )
                    ToastType.INFO -> Triple(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onSecondaryContainer,
                        Icons.Default.Info
                    )
                }

                Surface(
                    modifier = Modifier
                        .padding(bottom = 120.dp)
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = backgroundColor,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = toast.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
