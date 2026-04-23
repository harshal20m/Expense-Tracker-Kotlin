package com.example.paisatracker.ui.export

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.BackupMetadata
import com.example.paisatracker.data.ProjectWithTotal
import com.example.paisatracker.ui.assets.CompactHeader
import com.example.paisatracker.ui.common.ToastType
import com.example.paisatracker.util.BackupManager
import com.example.paisatracker.util.formatCurrency
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: PaisaTrackerViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context) }

    // State
    val projects by viewModel.getAllProjectsWithTotal().collectAsState(initial = emptyList())
    val recentBackups by viewModel.getRecentBackups().collectAsState(initial = emptyList())

    var selectedProject by remember { mutableStateOf<ProjectWithTotal?>(null) }
    var isProjectSelectorExpanded by remember { mutableStateOf(false) }
    var lastExportedUri by remember { mutableStateOf<Uri?>(null) }

    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var isImportingCsv by remember { mutableStateOf(false) }

    var showRestoreWarning by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showDeleteBackupDialog by remember { mutableStateOf(false) }
    var backupToDelete by remember { mutableStateOf<BackupMetadata?>(null) }

    // Launchers
    val fullBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                scope.launch {
                    isBackingUp = true
                    if (backupManager.createFullBackup(uri) != null) viewModel.showToast("Backup Created!", ToastType.SUCCESS)
                    else viewModel.showToast("Backup Failed!", ToastType.ERROR)
                    isBackingUp = false
                }
            }
        }
    }

    val fullRestoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pendingRestoreUri = it; showRestoreWarning = true }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                scope.launch {
                    val csvData = selectedProject?.let { viewModel.getExpensesForExport(it.project.id) }
                    if (csvData != null) {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write("\uFEFF".toByteArray(Charsets.UTF_8))
                            os.write(csvData.toByteArray(Charsets.UTF_8))
                        }
                        lastExportedUri = uri
                        viewModel.showToast("CSV Exported!", ToastType.SUCCESS)
                    }
                }
            }
        }
    }

    val csvImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedProject?.let { project ->
                scope.launch {
                    isImportingCsv = true
                    if (viewModel.importFromCsv(context, it, project.project.id)) viewModel.showToast("CSV Imported!", ToastType.SUCCESS)
                    else viewModel.showToast("Import Failed!", ToastType.ERROR)
                    isImportingCsv = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CompactHeader(
            title = "Data Management",
            subtitle = "Backup or export your data",
            icon = Icons.Default.CloudSync
        )

        if (isBackingUp) LoadingOverlay("Creating backup...")
        if (isRestoring) LoadingOverlay("Restoring data...")
        if (isImportingCsv) LoadingOverlay("Importing CSV...")

        if (showRestoreWarning) {
            RestoreWarningDialog(
                onDismiss = { showRestoreWarning = false; pendingRestoreUri = null },
                onConfirm = {
                    showRestoreWarning = false
                    pendingRestoreUri?.let { uri ->
                        scope.launch {
                            isRestoring = true
                            if (backupManager.restoreFromBackup(uri)) viewModel.showToast("Restore Successful!", ToastType.SUCCESS)
                            else viewModel.showToast("Restore Failed!", ToastType.ERROR)
                            isRestoring = false
                        }
                    }
                    pendingRestoreUri = null
                }
            )
        }

        if (showDeleteBackupDialog && backupToDelete != null) {
            DeleteBackupDialog(backupToDelete!!, { showDeleteBackupDialog = false }, {
                scope.launch {
                    if (backupManager.deleteBackupFile(backupToDelete!!)) viewModel.showToast("Backup deleted", ToastType.INFO)
                }
                showDeleteBackupDialog = false
            })
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SectionTitle("System Backup")
                CleanBackupCard(
                    onBackup = {
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/octet-stream"
                            putExtra(Intent.EXTRA_TITLE, "PaisaTracker_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.backup")
                        }
                        fullBackupLauncher.launch(intent)
                    },
                    onRestore = { fullRestoreLauncher.launch("*/*") }
                )
            }

            if (recentBackups.isNotEmpty()) {
                item { SectionTitle("Recent Backups") }
                items(recentBackups) { backup ->
                    CleanBackupItem(
                        backup = backup,
                        backupManager = backupManager,
                        onRestore = { pendingRestoreUri = Uri.parse(backup.filePath); showRestoreWarning = true },
                        onDelete = { backupToDelete = backup; showDeleteBackupDialog = true },
                        onShare = {
                            try {
                                val uri = Uri.parse(backup.filePath)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/octet-stream"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Backup"))
                            } catch (e: Exception) { viewModel.showToast("Share Failed", ToastType.ERROR) }
                        }
                    )
                }
            }

            item {
                SectionTitle("CSV Export & Import")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CleanProjectSelector(projects, selectedProject, isProjectSelectorExpanded, { isProjectSelectorExpanded = it }, { selectedProject = it; lastExportedUri = null }, { navController.navigate("projects") })
                    CleanCsvRow(selectedProject, lastExportedUri, {
                        selectedProject?.let { project ->
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "text/csv"
                                putExtra(Intent.EXTRA_TITLE, "${project.project.name}_expenses.csv")
                            }
                            csvExportLauncher.launch(intent)
                        }
                    }, { csvImportLauncher.launch("*/*") }, { uri ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share CSV"))
                    })
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun CleanBackupCard(onBackup: () -> Unit, onRestore: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("System Backup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Database and assets in one file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onBackup, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) { Icon(Icons.Default.Backup, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Backup", fontSize = 13.sp) }
                OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) { Icon(Icons.Default.Restore, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Restore", fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun CleanBackupItem(backup: BackupMetadata, backupManager: BackupManager, onRestore: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(backup.timestamp)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(backup.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row {
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = onRestore, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                }
            }
            if (isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    BackupInfoRow("File", backup.fileName)
                    BackupInfoRow("Size", backupManager.formatFileSize(backup.fileSize))
                    BackupInfoRow("Expenses", "${backup.expenseCount}")
                    BackupInfoRow("Total", formatCurrency(backup.totalAmount))
                }
            }
        }
    }
}

@Composable
private fun BackupInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CleanProjectSelector(projects: List<ProjectWithTotal>, selectedProject: ProjectWithTotal?, isExpanded: Boolean, onExpandChange: (Boolean) -> Unit, onProjectSelected: (ProjectWithTotal) -> Unit, onNoProjectsClick: () -> Unit) {
    if (projects.isEmpty()) {
        Surface(modifier = Modifier.fillMaxWidth().clickable { onNoProjectsClick() }, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                Text("No projects. Tap to create.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    } else {
        ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = onExpandChange) {
            TextField(value = selectedProject?.project?.name ?: "Select Project", onValueChange = {}, readOnly = true, leadingIcon = { selectedProject?.let { Text(it.project.emoji) } }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) }, colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(), shape = RoundedCornerShape(8.dp), textStyle = MaterialTheme.typography.bodyMedium)
            ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { onExpandChange(false) }) {
                projects.forEach { project -> DropdownMenuItem(text = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(project.project.emoji); Text(project.project.name, style = MaterialTheme.typography.bodyMedium) } }, onClick = { onProjectSelected(project); onExpandChange(false) }) }
            }
        }
    }
}

@Composable
private fun CleanCsvRow(selectedProject: ProjectWithTotal?, lastExportedUri: Uri?, onExport: () -> Unit, onImport: () -> Unit, onShare: (Uri) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.FileUpload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Text("Export CSV", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Button(onClick = onExport, enabled = selectedProject != null, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Text("Export", fontSize = 12.sp) }
                if (lastExportedUri != null) OutlinedButton(onClick = { onShare(lastExportedUri) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Icon(Icons.Default.Share, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Share", fontSize = 12.sp) }
            }
        }
        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.FileDownload, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                Text("Import CSV", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Button(onClick = onImport, enabled = selectedProject != null, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary), shape = RoundedCornerShape(8.dp)) { Text("Import", fontSize = 12.sp) }
            }
        }
    }
}