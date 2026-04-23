package com.example.paisatracker.ui.assets

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.ui.common.ToastType
import com.example.paisatracker.data.Asset
import com.example.paisatracker.ui.common.ZoomableImageDialog
import java.io.File

private enum class DialogType { ADD, DELETE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsBottomSheet(
    viewModel: PaisaTrackerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val assets by viewModel.getAllAssets().collectAsState(initial = emptyList())

    var selectedImagePath by remember { mutableStateOf<String?>(null) }
    var dialogType by remember { mutableStateOf<DialogType?>(null) }
    var activeAsset by remember { mutableStateOf<Asset?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val cameraUri = remember { mutableStateOf<Uri?>(null) }

    // We skip partially expanded so it opens like a nice tall drawer
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Gallery picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            dialogType = DialogType.ADD
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri.value?.let { uri ->
                selectedUri = uri
                dialogType = DialogType.ADD
            }
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val imagesDir = File(context.filesDir, "expense_assets")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }

                val photoFile = File(imagesDir, "IMG_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                cameraUri.value = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                viewModel.showToast("Camera error", ToastType.ERROR)
            }
        }
    }


    // ── MAIN BOTTOM SHEET ──
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background

    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
        ) {
            CompactHeader(
                title = "Gallery",
                // ... rest of your code ...
                subtitle = if (assets.isNotEmpty()) "${assets.size} captures" else "Your visual memory",
                icon = Icons.Default.Collections
            )



            if (assets.isEmpty()) {
                EmptyAssetsState(
                    onCameraClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    onGalleryClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    item {
                        AddAssetActionCard(
                            onCameraClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            onGalleryClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }

                    items(assets, key = { it.id }) { asset ->
                        ModernAssetCard(
                            asset = asset,
                            onImageClick = { selectedImagePath = asset.imagePath },
                            onDeleteClick = {
                                activeAsset = asset
                                dialogType = DialogType.DELETE
                            }
                        )
                    }
                }
            }
        }
    }

    // ── DIALOGS (Instead of nested bottom sheets) ──
    when (dialogType) {
        DialogType.ADD -> {
            AddImageDialog(
                onDismiss = { dialogType = null; selectedUri = null },
                onConfirm = { title, description ->
                    selectedUri?.let { uri ->
                        viewModel.addIndependentAsset(context, uri, title, description)
                        viewModel.showToast("✅ Saved to Gallery", ToastType.SUCCESS)
                    }
                    dialogType = null
                    selectedUri = null
                }
            )
        }
        DialogType.DELETE -> {
            activeAsset?.let { asset ->
                DeleteAssetDialog(
                    asset = asset,
                    onDismiss = { dialogType = null; activeAsset = null },
                    onConfirm = {
                        viewModel.deleteAsset(asset)
                        viewModel.showToast("🗑️ Image removed", ToastType.INFO)
                        dialogType = null
                        activeAsset = null
                    }
                )
            }
        }
        null -> {}
    }

    // ── ZOOM DIALOG ──
    selectedImagePath?.let { imagePath ->
        ZoomableImageDialog(
            imageModel = imagePath,
            onDismiss = { selectedImagePath = null }
        )
    }
}

// -----------------------------------------------------------------
// UI COMPONENTS (Cards & Headers)
// -----------------------------------------------------------------

@Composable
fun AddAssetActionCard(onCameraClick: () -> Unit, onGalleryClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(backgroundBrush).padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Image, contentDescription = null,
                modifier = Modifier.size(80.dp).align(Alignment.BottomEnd).offset(x = 24.dp, y = 24.dp).alpha(0.06f),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("New Asset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Capture or select", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(onClick = onCameraClick, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f).height(44.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimary) }
                    }
                    Surface(onClick = onGalleryClick, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.weight(1f).height(44.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactHeader(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, letterSpacing = (-0.5).sp)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun EmptyAssetsState(onCameraClick: () -> Unit, onGalleryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Image, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(16.dp))
        Text("Gallery is empty", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Capture your receipts or memories", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Column(modifier = Modifier.fillMaxWidth(0.8f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onCameraClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Take Photo")
            }
            OutlinedButton(onClick = onGalleryClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Pick Image")
            }
        }
    }
}

@Composable
private fun ModernAssetCard(asset: Asset, onImageClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onImageClick),
        shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = asset.imagePath, contentDescription = asset.title, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
            if (asset.title.isNotEmpty() || asset.description.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomStart).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
            }
            Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(10.dp)) {
                if (asset.title.isNotEmpty()) { Text(asset.title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                if (asset.description.isNotEmpty()) { Text(asset.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(24.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = Color.White) }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// DIALOGS (Converted from Sheets)
// -----------------------------------------------------------------

@Composable
private fun AddImageDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Image Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it }, label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(title, description) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DeleteAssetDialog(asset: Asset, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Image?", fontWeight = FontWeight.Bold) },
        text = { Text("This image will be permanently removed. You cannot undo this action.") },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Delete")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}