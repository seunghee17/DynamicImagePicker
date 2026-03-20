package com.universe.imagepicker.presentation.picker

import com.universe.imagepicker.domain.model.PermissionStatus

data class ImagePickerState(
    val permissionStatus: PermissionStatus = PermissionStatus.DENIED,
    val hasRequestedPermission: Boolean = false
)
