package com.example.paisatracker.ui.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.ui.common.ToastType
import com.example.paisatracker.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween

private val nastyMessages = listOf(
    "bro really took 3 days to name a variable 'x' 💀",
    "404: social life not found. last seen before this app.",
    "this guy googles 'how to center a div' every single day.",
    "writes TODO comments and forgets them for 6 months 🤡",
    "Stack Overflow's #1 anonymous user. trust me.",
    "debugs by adding Log.d everywhere and forgets to remove them.",
    "thinks 'it works on my machine' is valid documentation 😭",
    "his git commit messages are literally just 'fix' and 'fix 2'",
    "copy-pasted 200 lines and still doesn't know what half of it does.",
    "closes 47 Stack Overflow tabs after fixing one bug.",
)

private val secretFooterTexts = listOf(
    "Made with love in India",
    "Made with caffeine + anxiety in India",
    "Made with ctrl+z × 47 in India",
    "Made with 3am energy drinks in India",
    "Made with Stack Overflow copy-paste in India",
    "Made with bugs turned into features in India",
    "Made with questionable life choices in India",
    "ok stop tapping. go track your expenses.",
)

private val versionSecrets = listOf(
    "harshal said if you find this he owes you a chai ☕",
    "you tapped 5 times. QA tester or extremely bored. both valid.",
    "secret unlocked: infinite motivation (offer not real)",
    "there's nothing more here. go touch grass.",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutBottomSheet(
    viewModel: PaisaTrackerViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var avatarToastText by remember { mutableStateOf("") }
    var showAvatarToast by remember { mutableStateOf(false) }
    var versionTapCount by remember { mutableIntStateOf(0) }
    var showVersionSecret by remember { mutableStateOf(false) }
    var versionSecretText by remember { mutableStateOf("") }
    var footerIndex by remember { mutableIntStateOf(0) }
    var showHiddenSocialToast by remember { mutableStateOf(false) }

    val shakeAnim = remember { Animatable(0f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Developer hero card ───────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Easter egg 1: long-press for roast
                            Image(
                                painter = painterResource(id = R.drawable.harshalmali),
                                contentDescription = "Developer",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .graphicsLayer { rotationZ = shakeAnim.value }
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                val msg = nastyMessages.random()
                                                avatarToastText = msg
                                                showAvatarToast = true
                                                scope.launch {
                                                    repeat(4) {
                                                        shakeAnim.animateTo(
                                                            if (it % 2 == 0) 8f else -8f,
                                                            animationSpec = tween(80)
                                                        )
                                                    }
                                                    shakeAnim.animateTo(0f, tween(80))
                                                    delay(3500)
                                                    showAvatarToast = false
                                                }
                                            }
                                        )
                                    },
                                contentScale = ContentScale.Crop
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Harshal Mali",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Text(
                                    "Developed with love",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "@harshal20m",
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 3.dp
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Close, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Roast toast
                    AnimatedVisibility(
                        visible = showAvatarToast,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.inverseSurface
                        ) {
                            Text(
                                "\"$avatarToastText\"",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // App identity row with tappable version (Easter egg 2)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_project_icon_header),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = androidx.compose.ui.graphics.Color.Unspecified
                        )
                        Column {
                            Text(
                                "PaisaTracker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = when {
                                    versionTapCount in 1..4 -> "${5 - versionTapCount} more taps..."
                                    else -> "Version 1.0.0"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable {
                                    versionTapCount++
                                    if (versionTapCount >= 5) {
                                        versionSecretText = versionSecrets.random()
                                        showVersionSecret = true
                                        versionTapCount = 0
                                        scope.launch {
                                            delay(5000)
                                            showVersionSecret = false
                                        }
                                    }
                                }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showVersionSecret,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                versionSecretText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── Social links card — one row, 5 icons + ? Easter egg ───────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Stay in touch",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Single row: Instagram, LinkedIn, GitHub, Gmail, ? (easter egg)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SocialChip(
                            iconRes = R.drawable.ic_instagram,
                            label = "Instagram",
                            modifier = Modifier.weight(1f)
                        ) { openInstagram(context, viewModel, "20harshal") }

                        SocialChip(
                            iconRes = R.drawable.ic_linkedin,
                            label = "LinkedIn",
                            modifier = Modifier.weight(1f)
                        ) { openUrl(context, viewModel, "https://www.linkedin.com/in/harshal-mali-b40b61244/") }

                        SocialChip(
                            iconRes = R.drawable.ic_github,
                            label = "GitHub",
                            modifier = Modifier.weight(1f)
                        ) { openUrl(context, viewModel, "https://github.com/harshal20m") }

                        SocialChip(
                            iconRes = R.drawable.ic_gmail,
                            label = "Email",
                            modifier = Modifier.weight(1f)
                        ) { openEmail(context, viewModel, "20harshalmali@gmail.com") }

                        // Easter egg 3: hidden ? button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable {
                                    showHiddenSocialToast = !showHiddenSocialToast
                                    if (showHiddenSocialToast) {
                                        scope.launch {
                                            delay(4000)
                                            showHiddenSocialToast = false
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "?",
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                                Text(
                                    "???",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }

                    // Easter egg 3 reveal toast
                    AnimatedVisibility(
                        visible = showHiddenSocialToast,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                "you found the hidden button. the dev hid his Spotify here. currently on talwinders `khayal` track at 2am fixing bugs.",
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // ── Community card ────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Community",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CommunityTile(
                            iconRes = R.drawable.ic_discord,
                            label = "Discord",
                            subtitle = "Join server",
                            modifier = Modifier.weight(1f)
                        ) { openUrl(context, viewModel, "https://discord.gg/kmaqH8CSFD") }

                        CommunityTile(
                            iconRes = R.drawable.ic_telegram,
                            label = "Telegram",
                            subtitle = "Join channel",
                            modifier = Modifier.weight(1f)
                        ) { openTelegram(context, viewModel, "paisatrackercommunity") }
                    }
                }
            }

            // ── Features 2-col ────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Key features",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FeatureRow("Track by projects")
                            FeatureRow("Export CSV / PDF")
                            FeatureRow("Attach receipts")
                        }
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FeatureRow("Daily reminders")
                            FeatureRow("Offline-first")
                            FeatureRow("Biometric lock")
                        }
                    }
                }
            }

            // ── Tech stack pill cloud ─────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Built with",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Kotlin", "Jetpack Compose", "Room Database",
                            "Material 3", "MVVM", "Coroutines", "Flow"
                        ).forEach { tech ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    tech,
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 5.dp
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // ── Easter egg 4: cycling footer text ─────────────────────────────
            Text(
                text = secretFooterTexts[footerIndex % secretFooterTexts.size],
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { footerIndex++ },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Close",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun SocialChip(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CommunityTile(
    iconRes: Int,
    label: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit
            )
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

// ── URL helpers ───────────────────────────────────────────────────────────────

private fun openUrl(context: Context, viewModel: PaisaTrackerViewModel, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    } catch (_: Exception) {
        viewModel.showToast("Unable to open link", ToastType.ERROR)
    }
}

private fun openInstagram(context: Context, viewModel: PaisaTrackerViewModel, username: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, "http://instagram.com/_u/$username".toUri())
        intent.setPackage("com.instagram.android")
        context.startActivity(intent)
    } catch (_: Exception) {
        openUrl(context, viewModel, "https://instagram.com/$username")
    }
}

private fun openEmail(context: Context, viewModel: PaisaTrackerViewModel, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:$email".toUri()
            putExtra(Intent.EXTRA_SUBJECT, "Hello from PaisaTracker")
        }
        context.startActivity(Intent.createChooser(intent, "Send Email"))
    } catch (_: Exception) {
        viewModel.showToast("No email app found", ToastType.ERROR)
    }
}

private fun openTelegram(context: Context, viewModel: PaisaTrackerViewModel, username: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, "tg://resolve?domain=$username".toUri())
        context.startActivity(intent)
    } catch (_: Exception) {
        openUrl(context, viewModel, "https://t.me/$username")
    }
}
