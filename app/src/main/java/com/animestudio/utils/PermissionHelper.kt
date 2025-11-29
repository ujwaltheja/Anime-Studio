package com.animestudio.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Helper class for managing runtime permissions
 */
class PermissionHelper(private val activity: ComponentActivity) {

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        onPermissionResult?.invoke(allGranted)
    }

    /**
     * Request necessary permissions for video processing (read + write)
     */
    fun requestVideoPermissions(onResult: (Boolean) -> Unit) {
        onPermissionResult = onResult

        val permissions = mutableListOf<String>()

        // Storage permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: Use scoped storage (READ_MEDIA_VIDEO covers read)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            // Note: WRITE_EXTERNAL_STORAGE is not needed on Android 13+ with scoped storage
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12: Use legacy external files directory (managed by system)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            // Write to app-specific directory doesn't need WRITE_EXTERNAL_STORAGE on Android 10+
        } else {
            // Android 9 and below: Need explicit read/write permissions
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // Check if permissions are already granted
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            onResult(true)
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    /**
     * Request write storage permissions (for saving output videos)
     */
    fun requestWriteStoragePermission(onResult: (Boolean) -> Unit) {
        onPermissionResult = onResult

        // On Android 10+, writing to app-specific external files directory is allowed
        // without WRITE_EXTERNAL_STORAGE permission (automatically granted)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            onResult(true)
            return
        }

        // Android 9 and below need explicit WRITE_EXTERNAL_STORAGE
        val hasPermission = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            onResult(true)
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    /**
     * Request camera permission for video recording
     */
    fun requestCameraPermission(onResult: (Boolean) -> Unit) {
        onPermissionResult = onResult

        val hasPermission = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            onResult(true)
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }

    /**
     * Check if video read permissions are granted
     */
    fun hasVideoPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if write storage permissions are granted
     */
    fun hasWriteStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ doesn't need explicit write permission for app-specific directory
            true
        } else {
            // Android 9 and below need explicit permission
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if both read and write permissions are granted
     */
    fun hasAllProcessingPermissions(context: Context): Boolean {
        return hasVideoPermissions(context) && hasWriteStoragePermission(context)
    }
}
