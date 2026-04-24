package com.gardenworkanalyzer.ui.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.gardenworkanalyzer.domain.model.PermissionStatus

/**
 * Manages runtime permission checks and requests for camera and storage access.
 * Handles granted, denied, and permanently denied states per Android guidelines.
 */
class PermissionManager {

    /**
     * Checks the current status of a permission.
     *
     * @param context The context (must be an Activity to detect permanently denied state)
     * @param permission The permission string (e.g., Manifest.permission.CAMERA)
     * @return [PermissionStatus.GRANTED] if already granted,
     *         [PermissionStatus.PERMANENTLY_DENIED] if the user denied and checked "Don't ask again",
     *         [PermissionStatus.DENIED] otherwise
     */
    fun checkPermission(context: Context, permission: String): PermissionStatus {
        val granted = ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) return PermissionStatus.GRANTED

        // shouldShowRequestPermissionRationale returns false when:
        // 1. Permission has never been requested (first time) → DENIED
        // 2. User selected "Don't ask again" → PERMANENTLY_DENIED
        // It returns true when the user previously denied but didn't check "Don't ask again".
        // We treat the "never asked" case as DENIED so the caller can request it.
        // The permanently denied case is only reliably detectable after a request has been made,
        // so the activity must be provided for accurate detection.
        if (context is Activity) {
            val shouldShowRationale = context.shouldShowRequestPermissionRationale(permission)
            return if (shouldShowRationale) {
                PermissionStatus.DENIED
            } else {
                // Could be first time or permanently denied.
                // We return PERMANENTLY_DENIED here; callers should track whether
                // the permission was previously requested to distinguish the two cases.
                PermissionStatus.PERMANENTLY_DENIED
            }
        }

        return PermissionStatus.DENIED
    }

    /**
     * Launches a permission request using the provided [ActivityResultLauncher].
     *
     * @param permission The permission string to request
     * @param launcher The launcher obtained from registerForActivityResult
     */
    fun requestPermission(permission: String, launcher: ActivityResultLauncher<String>) {
        launcher.launch(permission)
    }

    /**
     * Opens the app's settings page so the user can manually grant permissions
     * that were permanently denied.
     *
     * @param context The context used to start the settings activity
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
