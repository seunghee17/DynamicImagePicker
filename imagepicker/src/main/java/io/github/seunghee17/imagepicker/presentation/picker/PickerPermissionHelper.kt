package io.github.seunghee17.imagepicker.presentation.picker

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
import io.github.seunghee17.imagepicker.domain.model.PermissionStatus

internal fun requestedPermissionsForPicker(allowVideo: Boolean = false): Array<String> = when {
    Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 ->
        // READ_EXTERNAL_STORAGE covers both images and videos on API <= 32
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
        buildList {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            if (allowVideo) add(Manifest.permission.READ_MEDIA_VIDEO)
        }.toTypedArray()
    else ->
        buildList {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            if (allowVideo) add(Manifest.permission.READ_MEDIA_VIDEO)
        }.toTypedArray()
}

internal fun resolvePermissionStatus(
    context: Context,
    hasRequestedPermission: Boolean,
    allowVideo: Boolean = false
): PermissionStatus {
    val activity = context.findActivity()
    val fullPermissions = fullAccessPermissions(allowVideo)

    // Check if all required permissions are granted
    val hasFullAccess = fullPermissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    if (hasFullAccess) return PermissionStatus.GRANTED

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val hasSelectedPhotoAccess = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        ) == PackageManager.PERMISSION_GRANTED
        if (hasSelectedPhotoAccess) return PermissionStatus.PARTIALLY_GRANTED
    }

    val primaryPermission = fullPermissions.first()
    val shouldShowRationale = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, primaryPermission)
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

private fun fullAccessPermissions(allowVideo: Boolean = false): Array<String> = when {
    Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 ->
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
        buildList {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            if (allowVideo) add(Manifest.permission.READ_MEDIA_VIDEO)
        }.toTypedArray()
    else ->
        buildList {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            if (allowVideo) add(Manifest.permission.READ_MEDIA_VIDEO)
        }.toTypedArray()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
