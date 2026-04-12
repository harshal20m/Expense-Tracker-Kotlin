package com.example.paisatracker.ui.pending

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paisatracker.PaisaTrackerApplication
import com.example.paisatracker.service.UpiNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Settings screen for the notification auto-capture feature.
 *
 * Route: "auto_capture_settings"
 *
 * Add to NavGraph:
 *   composable("auto_capture_settings") {
 *       AutoCaptureSettingsScreen(
 *           onBack       = { navController.navigateUp() },
 *           onGoToReview = { navController.navigate("pending_review") }
 *       )
 *   }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCaptureSettingsScreen(
    onBack: () -> Unit,
    onGoToReview: () -> Unit
) {
    val context       = LocalContext.current
    val app           = context.applicationContext as PaisaTrackerApplication
    val vm: PendingTransactionViewModel = viewModel(
        factory = PendingTransactionViewModelFactory(app.repository, context)
    )

    val isEnabled    by vm.isListenerEnabled.collectAsState()
    val reviewFirst  by vm.reviewBeforeSave.collectAsState()
    val skipDups     by vm.skipDuplicates.collectAsState()
    val fGPay        by vm.filterGPay.collectAsState()
    val fPhonePe     by vm.filterPhonePe.collectAsState()
    val fPaytm       by vm.filterPaytm.collectAsState()
    val fBhim        by vm.filterBhim.collectAsState()
    val fAmazon      by vm.filterAmazon.collectAsState()
    val fBankSms     by vm.filterBankSms.collectAsState()
    val pendingCount by vm.unreviewedCount.collectAsState()

    // Re-check permission every recomposition (user may have just granted it)
    val isPermGranted = UpiNotificationListener.isPermissionGranted(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Auto-capture", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(pad)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── What this does explanation ─────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                elevation= CardDefaults.cardElevation(0.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Text("🔔", fontSize = 20.sp, modifier = Modifier.padding(top = 2.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Auto-capture expenses", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "PaisaTracker reads UPI payment notifications from GPay, PhonePe, Paytm, and bank SMS. Each transaction is held for your review before being saved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ── Notification permission banner ────────────────────────────────
            if (!isPermGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Permission required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Tap to open Settings → Apps → Special app access → Notification access, then enable PaisaTracker.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                lineHeight = 16.sp
                            )
                        }
                        TextButton(
                            onClick = { UpiNotificationListener.openPermissionSettings(context) }
                        ) {
                            Text("Grant", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Permission granted success banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    color    = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("✅", fontSize = 14.sp)
                        Text("Notification access granted", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }

            // ── Master toggle ─────────────────────────────────────────────────
            SectionLabel("LISTENING")
            AcSettingsCard {
                AcToggleRow(
                    title    = "Enable auto-capture",
                    subtitle = if (isEnabled) "Actively listening for UPI payments" else "Turn on to start capturing transactions automatically",
                    checked  = isEnabled,
                    enabled  = isPermGranted,
                    onChecked= { vm.setListenerEnabled(it) }
                )
                if (isEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)).padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                        Text("Live · Monitoring GPay, PhonePe, Paytm, Bank SMS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }

            // ── App filters (only visible when enabled) ───────────────────────
            AnimatedVisibility(
                visible = isEnabled,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    SectionLabel("CAPTURE FROM")
                    AcSettingsCard {
                        data class AppFilter(
                            val label: String,
                            val subtitle: String,
                            val flow: Boolean,
                            val key: String,
                            val mflow: MutableStateFlow<Boolean>
                        )
                        val filters = listOf(
                            AppFilter("GPay",      "Google Pay notifications",       fGPay,    UpiNotificationListener.KEY_FILTER_GPAY,     vm.filterGPay),
                            AppFilter("PhonePe",   "PhonePe payment alerts",         fPhonePe, UpiNotificationListener.KEY_FILTER_PHONEPE,  vm.filterPhonePe),
                            AppFilter("Paytm",     "Paytm payment notifications",    fPaytm,   UpiNotificationListener.KEY_FILTER_PAYTM,    vm.filterPaytm),
                            AppFilter("BHIM",      "BHIM UPI notifications",         fBhim,    UpiNotificationListener.KEY_FILTER_BHIM,     vm.filterBhim),
                            AppFilter("Amazon Pay","Amazon Pay alerts",              fAmazon,  UpiNotificationListener.KEY_FILTER_AMAZON,   vm.filterAmazon),
                            AppFilter("Bank SMS",  "Debit/credit SMS from any bank", fBankSms, UpiNotificationListener.KEY_FILTER_BANK_SMS, vm.filterBankSms),
                        )
                        filters.forEachIndexed { i, f ->
                            AcToggleRow(
                                title    = f.label,
                                subtitle = f.subtitle,
                                checked  = f.flow,
                                onChecked= { vm.setFilter(f.key, f.mflow, it) }
                            )
                            if (i < filters.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                            }
                        }
                    }

                    SectionLabel("BEHAVIOUR")
                    AcSettingsCard {
                        AcToggleRow(
                            title    = "Review before saving",
                            subtitle = "Show each transaction for confirmation before it becomes an expense",
                            checked  = reviewFirst,
                            onChecked= { vm.setReviewBeforeSave(it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        AcToggleRow(
                            title    = "Skip duplicates",
                            subtitle = "Ignore notifications with a UTR number already recorded",
                            checked  = skipDups,
                            onChecked= { vm.setSkipDuplicates(it) }
                        )
                    }

                    // ── Pending review shortcut ───────────────────────────────
                    if (pendingCount > 0) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onGoToReview),
                            shape    = RoundedCornerShape(16.dp),
                            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            elevation= CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier  = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$pendingCount",
                                            style      = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color      = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Transactions waiting", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text("Tap to review and save as expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f))
                                    }
                                }
                                Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Private sub-components (scoped to this file) ──────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style         = MaterialTheme.typography.labelSmall,
        fontWeight    = FontWeight.SemiBold,
        color         = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun AcSettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun AcToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                title,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color      = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.5f else 0.3f),
                    lineHeight = 16.sp
                )
            }
        }
        Switch(
            checked         = checked,
            onCheckedChange = onChecked,
            enabled         = enabled,
            modifier        = Modifier.height(24.dp)
        )
    }
}