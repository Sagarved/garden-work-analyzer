package com.gardenworkanalyzer.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.gardenworkanalyzer.ui.permission.PermissionManager
import java.io.File

/**
 * A Material 3 button that opens the device camera to capture a photo.
 *
 * Hidden entirely when the device has no camera hardware
 * (PackageManager.FEATURE_CAMERA_ANY check). Handles camera permission
 * request flow including the permanently-denied case with a settings link.
 *
 * Uses ActivityResultContracts.TakePicture to capture a single image.
 * On cancel (success=false), this is a no-op: the image collection remains unchanged.
 *
 * @param onImageCaptured Callback invoked with the URI of the captured image.
 *                        Not called when the user cancels.
 * @param permissionManager The PermissionManager used for permission checks and settings navigation.
 * @param enabled Whether the button is enabled.
 * @param modifier Modifier for the button.
 */
@Composable
fun CameraCaptureButton(
    onImageCaptured: (Uri) -> Unit,
    permissionManager: PermissionManager,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Requirement 2.4: Hide camera button when no camera is available
    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    if (!hasCamera) return

    var captureUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            captureUri?.let { uri -> onImageCaptured(uri) }
        }
        // Cancel (success=false) is a no-op per Requirement 2.6
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            val uri = createCaptureUri(context)
            captureUri = uri
            takePictureLauncher.launch(uri)
        } else {
            showPermissionDeniedDialog = true
        }
    }

    Button(
        onClick = {
            val permissionStatus = permissionManager.checkPermission(
                context, Manifest.permission.CAMERA
            )
            when (permissionStatus) {
                com.gardenworkanalyzer.domain.model.PermissionStatus.GRANTED -> {
                    val uri = createCaptureUri(context)
                    captureUri = uri
                    takePictureLauncher.launch(uri)
                }
                com.gardenworkanalyzer.domain.model.PermissionStatus.DENIED -> {
                    permissionManager.requestPermission(
                        Manifest.permission.CAMERA, permissionLauncher
                    )
                }
                com.gardenworkanalyzer.domain.model.PermissionStatus.PERMANENTLY_DENIED -> {
                    showPermissionDeniedDialog = true
                }
            }
        },
        enabled = enabled,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.AccountBox,
            contentDescription = null
        )
        Text(text = "Camera")
    }

    // Requirement 2.5: Permission denied dialog with settings link
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Camera Permission Required") },
            text = {
                Text(
                    "Camera permission is required to capture photos. " +
                        "Please grant camera permission in app settings."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDeniedDialog = false
                    permissionManager.openAppSettings(context)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Creates a temporary file-backed URI for camera capture via FileProvider.
 */
private fun createCaptureUri(context: Context): Uri {
    val imageDir = File(context.cacheDir, "camera_images").apply { mkdirs() }
    val imageFile = File(imageDir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}
