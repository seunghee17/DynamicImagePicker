package com.universe.imagepicker.domain.usecase

import com.universe.imagepicker.domain.model.PermissionStatus

/**
 * 현재 갤러리 접근 권한 상태를 반환한다.
 * 실제 권한 요청 다이얼로그는 Compose PermissionState가 담당하며,
 * 이 UseCase는 상태 조회만 수행하여 ViewModel을 Android API로부터 격리시킨다.
 */
fun interface CheckPermissionUseCase {
    operator fun invoke(): PermissionStatus
}
