package com.example.paisatracker.ui.export

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.BackupMetadata
import com.example.paisatracker.data.ProjectWithTotal
import com.example.paisatracker.util.BackupManager
import com.example.paisatracker.util.formatCurrency
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

    // Full Database Backup Launcher
    val fullBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                scope.launch {
                    isBackingUp = true
                    val metadata = backupManager.createFullBackup(uri)
                    isBackingUp = false

                    if (metadata != null) {
                        Toast.makeText(context, "Backup created successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Backup failed!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Full Database Restore Launcher
    val fullRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingRestoreUri = it
            showRestoreWarning = true
        }
    }

    // CSV Export Launcher
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                scope.launch {
                    val csvData = selectedProject?.let {
                        viewModel.getExpensesForExport(it.project.id)
                    }

                    if (csvData != null) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write("\uFEFF".toByteArray(Charsets.UTF_8))
                            outputStream.write(csvData.toByteArray(Charsets.UTF_8))
                        }
                        lastExportedUri = uri
                        Toast.makeText(context, "CSV exported successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // CSV Import Launcher
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedProject?.let { project ->
                scope.launch {
                    isImportingCsv = true
                    val success = viewModel.importFromCsv(context, it, project.project.id)
                    isImportingCsv = false

                    if (success) {
                        Toast.makeText(context, "CSV imported successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Import failed. Check file format.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Loading Overlays
    if (isBackingUp) {
        LoadingOverlay(message = "Creating backup...")
    }

    if (isRestoring) {
        LoadingOverlay(message = "Restoring data...")
    }

    if (isImportingCsv) {
        LoadingOverlay(message = "Importing CSV...")
    }

    // Restore Warning Dialog
    if (showRestoreWarning) {
        RestoreWarningDialog(
            onDismiss = {
                showRestoreWarning = false
                pendingRestoreUri = null
            },
            onConfirm = {
                showRestoreWarning = false
                pendingRestoreUri?.let { uri ->
                    scope.launch {
                        isRestoring = true
                        val success = backupManager.restoreFromBackup(uri)
                        isRestoring = false

                        if (success) {
                            Toast.makeText(context, "Restore successful! Restart app to see changes.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Restore failed!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                pendingRestoreUri = null
            }
        )
    }

    // Delete Backup Dialog
    if (showDeleteBackupDialog && backupToDelete != null) {
        DeleteBackupDialog(
            backup = backupToDelete!!,
            onDismiss = {
                showDeleteBackupDialog = false
                backupToDelete = null
            },
            onConfirm = {
                val backup = backupToDelete ?: return@DeleteBackupDialog

                showDeleteBackupDialog = false
                backupToDelete = null

                scope.launch {
                    val success = backupManager.deleteBackupFile(backup)
                    if (success) {
                        Toast.makeText(context, "Backup deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        ExportHeader()

        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Full Database Backup Section
            item {
                CleanBackupCard(
                    onBackup = {
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/zip"
                            putExtra(Intent.EXTRA_TITLE, "PaisaTracker_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.zip")
                        }
                        fullBackupLauncher.launch(intent)
                    },
                    onRestore = {
                        fullRestoreLauncher.launch("application/zip")
                    }
                )
            }

            // Recent Backups Section
            if (recentBackups.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Backups",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(recentBackups) { backup ->
                    CleanBackupItem(
                        backup = backup,
                        backupManager = backupManager,
                        onRestore = {
                            pendingRestoreUri = Uri.parse(backup.filePath)
                            showRestoreWarning = true
                        },
                        onDelete = {
                            backupToDelete = backup
                            showDeleteBackupDialog = true
                        }
                    )
                }
            }

            // CSV Export/Import Section
            item {
                Text(
                    text = "CSV Export & Import",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                CleanProjectSelector(
                    projects = projects,
                    selectedProject = selectedProject,
                    isExpanded = isProjectSelectorExpanded,
                    onExpandChange = { isProjectSelectorExpanded = it },
                    onProjectSelected = {
                        selectedProject = it
                        lastExportedUri = null
                    },
                    onNoProjectsClick = {
                        // Navigate to projects page to create a project
                        navController.navigate("projects") {
                            popUpTo("export") { inclusive = false }
                        }
                    }
                )
            }

            item {
                CleanCsvRow(
                    selectedProject = selectedProject,
                    lastExportedUri = lastExportedUri,
                    onExport = {
                        selectedProject?.let { project ->
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "text/csv"
                                putExtra(Intent.EXTRA_TITLE, "${project.project.name}_expenses.csv")
                            }
                            csvExportLauncher.launch(intent)
                        }
                    },
                    onImport = {
                        csvImportLauncher.launch("*/*")
                    },
                    onShare = { uri ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share CSV"))
                    }
                )
            }
        }
    }
}

@Composable
private fun ExportHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    ),
                    startY = 0f,
                    endY = 250f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 20.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "Backup & Export",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 26.sp
            )

            Text(
                text = "Manage your data safely",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun CleanBackupCard(
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        "Full Database Backup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Complete app data including receipts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onBackup,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Backup", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun CleanBackupItem(
    backup: BackupMetadata,
    backupManager: BackupManager,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(backup.timestamp)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(backup.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onRestore,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = "Restore",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    BackupInfoRow("Size", backupManager.formatFileSize(backup.fileSize))
                    BackupInfoRow("Projects", "${backup.projectCount}")
                    BackupInfoRow("Categories", "${backup.categoryCount}")
                    BackupInfoRow("Expenses", "${backup.expenseCount}")
                    BackupInfoRow("Total Amount", formatCurrency(backup.totalAmount))
                }
            }
        }
    }
}

@Composable
private fun BackupInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CleanProjectSelector(
    projects: List<ProjectWithTotal>,
    selectedProject: ProjectWithTotal?,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onProjectSelected: (ProjectWithTotal) -> Unit,
    onNoProjectsClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Select Project for CSV",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )

        if (projects.isEmpty()) {
            // Empty state - redirect to create project
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNoProjectsClick() },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "No Projects Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Tap here to create your first project",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            ExposedDropdownMenuBox(
                expanded = isExpanded,
                onExpandedChange = onExpandChange
            ) {
                TextField(
                    value = selectedProject?.project?.name ?: "Choose a project",
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = {
                        selectedProject?.let {
                            Text(it.project.emoji, fontSize = 20.sp)
                        }
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { onExpandChange(false) }
                ) {
                    projects.forEach { project ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(project.project.emoji, fontSize = 20.sp)
                                    Text(project.project.name)
                                }
                            },
                            onClick = {
                                onProjectSelected(project)
                                onExpandChange(false)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanCsvRow(
    selectedProject: ProjectWithTotal?,
    lastExportedUri: Uri?,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onShare: (Uri) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Export Card
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    "Export CSV",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )

                Button(
                    onClick = onExport,
                    enabled = selectedProject != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Export", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                if (lastExportedUri != null) {
                    OutlinedButton(
                        onClick = { onShare(lastExportedUri) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 13.sp)
                    }
                }
            }
        }

        // Import Card
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )

                Text(
                    "Import CSV",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )

                Button(
                    onClick = onImport,
                    enabled = selectedProject != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Import", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}