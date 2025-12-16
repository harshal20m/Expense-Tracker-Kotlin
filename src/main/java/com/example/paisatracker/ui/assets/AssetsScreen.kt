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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    var cameraUri = remember { mutableStateOf<Uri?>(null) }

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
                val imagesDir = File(context.filesDir, "images")
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
                Toast.makeText(context, "Camera error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Modern Header
        ModernHeader(imageCount = assets.size)

        if (assets.isEmpty()) {
            // Empty State
            EmptyAssetsState(
                onCameraClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                onGalleryClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        } else {
            // Assets Grid with Floating Action Buttons
            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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

                // Floating Action Buttons
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

    // Bottom Sheet Modal
    if (sheetType != null) {
        ModalBottomSheet(
            onDismissRequest = {
                sheetType = null
                selectedUri = null
                sheetAsset = null
            },
            sheetState = sheetState,
            dragHandle = null
        ) {
            when (sheetType) {
                SheetType.ADD -> AddImageSheetContent(
                    onDismiss = {
                        sheetType = null
                        selectedUri = null
                    },
                    onConfirm = { title, description ->
                        selectedUri?.let { uri ->
                            viewModel.addIndependentAsset(context, uri, title, description)
                        }
                        sheetType = null
                        selectedUri = null
                    }
                )

                SheetType.DELETE -> sheetAsset?.let { asset ->
                    DeleteAssetSheetContent(
                        asset = asset,
                        onDismiss = {
                            sheetType = null
                            sheetAsset = null
                        },
                        onConfirm = {
                            viewModel.deleteAsset(asset)
                            sheetType = null
                            sheetAsset = null
                        }
                    )
                }

                null -> {}
            }
        }
    }

    // Full Screen Image Viewer (keeping as dialog for zoom functionality)
    selectedImagePath?.let { imagePath ->
        ZoomableImageDialog(
            imageModel = imagePath,
            onDismiss = { selectedImagePath = null }
        )
    }
}

@Composable
private fun ModernHeader(imageCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gallery",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (imageCount > 0) "$imageCount ${if (imageCount == 1) "image" else "images"}" else "No images yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }

                if (imageCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAssetsState(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Empty illustration
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "No images yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Start building your collection",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCameraClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Take Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = onGalleryClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Choose from Gallery",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BoxScope.FloatingActionButtons(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = 130.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Gallery FAB
        FloatingActionButton(
            onClick = onGalleryClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = "Gallery",
                modifier = Modifier.size(24.dp)
            )
        }

        // Camera FAB
        FloatingActionButton(
            onClick = onCameraClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = "Camera",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ModernAssetCard(
    asset: Asset,
    onImageClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onImageClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image
            AsyncImage(
                model = asset.imagePath,
                contentDescription = asset.title.ifEmpty { "Asset image" },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay at bottom
            if (asset.title.isNotEmpty() || asset.description.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomStart)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
            }

            // Title and description
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                if (asset.title.isNotEmpty()) {
                    Text(
                        text = asset.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                }
                if (asset.description.isNotEmpty()) {
                    Text(
                        text = asset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
            }

            // Delete button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(36.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddImageSheetContent(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val configuration = LocalConfiguration.current
    val maxSheetHeight = configuration.screenHeightDp.dp * 0.75f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .heightIn(max = maxSheetHeight)
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )

        // Header with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                "Add Image Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Text fields
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
        )

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onConfirm(title, description) },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("Add", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DeleteAssetSheetContent(
    asset: Asset,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val maxSheetHeight = configuration.screenHeightDp.dp * 0.5f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .heightIn(max = maxSheetHeight)
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )

        // Header with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                "Delete Image?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Warning message
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "This action cannot be undone.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (asset.title.isNotEmpty()) {
                Text(
                    "\"${asset.title}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        }
    }
}
