package com.example.paisatracker.ui.assets

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.Asset
import java.io.File
import com.example.paisatracker.ui.common.ZoomableImageDialog

private enum class SheetType { ADD, DELETE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(viewModel: PaisaTrackerViewModel) {
    val context = LocalContext.current
    val assets by viewModel.getAllAssets().collectAsState(initial = emptyList())

    var selectedImagePath by remember { mutableStateOf<String?>(null) }
    var sheetType by remember { mutableStateOf<SheetType?>(null) }
    var sheetAsset by remember { mutableStateOf<Asset?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val cameraUri = remember { mutableStateOf<Uri?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Gallery picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            sheetType = SheetType.ADD
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri.value?.let { uri ->
                selectedUri = uri
                sheetType = SheetType.ADD
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
                Toast.makeText(context, "Camera error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CompactHeader(
            title = "Gallery",
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
            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    items(assets, key = { it.id }) { asset ->
                        ModernAssetCard(
                            asset = asset,
                            onImageClick = { selectedImagePath = asset.imagePath },
                            onDeleteClick = {
                                sheetAsset = asset
                                sheetType = SheetType.DELETE
                            }
                        )
                    }
                }

                FloatingActionButtons(
                    onCameraClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    onGalleryClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        }
    }

    // Bottom Sheets & Dialogs
    if (sheetType != null) {
        ModalBottomSheet(
            onDismissRequest = { sheetType = null; selectedUri = null; sheetAsset = null },
            sheetState = sheetState,
            dragHandle = null
        ) {
            when (sheetType) {
                SheetType.ADD -> AddImageSheetContent(
                    onDismiss = { sheetType = null; selectedUri = null },
                    onConfirm = { title, description ->
                        selectedUri?.let { uri ->
                            viewModel.addIndependentAsset(context, uri, title, description)
                            Toast.makeText(context, "✅ Saved to Gallery", Toast.LENGTH_SHORT).show()
                        }
                        sheetType = null
                        selectedUri = null
                    }
                )
                SheetType.DELETE -> sheetAsset?.let { asset ->
                    DeleteAssetSheetContent(
                        asset = asset,
                        onDismiss = { sheetType = null; sheetAsset = null },
                        onConfirm = {
                            viewModel.deleteAsset(asset)
                            Toast.makeText(context, "🗑️ Image removed", Toast.LENGTH_SHORT).show()
                            sheetType = null
                            sheetAsset = null
                        }
                    )
                }
                else -> {}
            }
        }
    }

    selectedImagePath?.let { imagePath ->
        ZoomableImageDialog(
            imageModel = imagePath,
            onDismiss = { selectedImagePath = null }
        )
    }
}

@Composable
fun CompactHeader(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyAssetsState(onCameraClick: () -> Unit, onGalleryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
private fun BoxScope.FloatingActionButtons(onCameraClick: () -> Unit, onGalleryClick: () -> Unit) {
    Row(
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 78.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FloatingActionButton(onClick = onGalleryClick, containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer, shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(24.dp))
        }
        FloatingActionButton(onClick = onCameraClick, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ModernAssetCard(asset: Asset, onImageClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onImageClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = asset.imagePath, contentDescription = asset.title, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
            if (asset.title.isNotEmpty() || asset.description.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomStart).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
            }
            Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(10.dp)) {
                if (asset.title.isNotEmpty()) {
                    Text(asset.title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (asset.description.isNotEmpty()) {
                    Text(asset.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(24.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun AddImageSheetContent(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.align(Alignment.CenterHorizontally).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
        Text("Image Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), shape = RoundedCornerShape(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onConfirm(title, description) }, shape = RoundedCornerShape(10.dp)) { Text("Save") }
        }
    }
}

@Composable
private fun DeleteAssetSheetContent(asset: Asset, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.align(Alignment.CenterHorizontally).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
        Text("Delete Image?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("This image will be permanently removed.", style = MaterialTheme.typography.bodyMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(10.dp)) { Text("Delete") }
        }
    }
}
