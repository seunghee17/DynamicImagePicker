package com.universe.imagepicker.presentation.picker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.universe.imagepicker.domain.model.PermissionStatus

internal fun requestedPermissionsForPicker(): Array<String> = when {
    Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 ->
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
    else ->
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
}

internal fun resolvePermissionStatus(
    context: Context,
    hasRequestedPermission: Boolean
): PermissionStatus {
    val activity = context.findActivity()
    val fullPermission = fullAccessPermission()

    val hasFullAccess = ContextCompat.checkSelfPermission(
        context, fullPermission
    ) == PackageManager.PERMISSION_GRANTED
    if (hasFullAccess) return PermissionStatus.GRANTED

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val hasSelectedPhotoAccess = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        ) == PackageManager.PERMISSION_GRANTED
        if (hasSelectedPhotoAccess) return PermissionStatus.PARTIALLY_GRANTED
    }

    val shouldShowRationale = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, fullPermission)
    } ?: false

    return if (hasRequestedPermission && !shouldShowRationale) {
        PermissionStatus.PERMANENTLY_DENIED
    } else {
        PermissionStatus.DENIED
    }
}

internal fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    context.startActivity(intent)
}

private fun fullAccessPermission(): String =
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
        Manifest.permission.READ_EXTERNAL_STORAGE
    else
        Manifest.permission.READ_MEDIA_IMAGES

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
