package io.github.seunghee17.imagepicker.presentation.picker

import app.cash.turbine.test
import io.github.seunghee17.imagepicker.domain.model.PermissionStatus
import io.github.seunghee17.imagepicker.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ImagePickerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ─────────────────────────────────────────────────────────────────────────
    // Initialize
    // ─────────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────────
    // PARTIALLY_GRANTED flow
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `PARTIALLY_GRANTED on INITIAL sends RequestPermission and marks flag`() = runTest {
        val vm = ImagePickerViewModel()
        vm.effect.test {
            vm.handleIntent(
                ImagePickerContract.Intent.OnPermissionEvaluated(
                    PermissionStatus.PARTIALLY_GRANTED,
                    ImagePickerContract.PermissionCheckSource.INITIAL,
                )
            )
            assertIs<ImagePickerContract.Effect.RequestPermission>(awaitItem())
            assertTrue(vm.state.value.hasRequestedFullAccessAfterPartial)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `PARTIALLY_GRANTED on RESUME does not send any effect`() = runTest {
        val vm = ImagePickerViewModel()
        vm.effect.test {
            vm.handleIntent(
                ImagePickerContract.Intent.OnPermissionEvaluated(
                    PermissionStatus.PARTIALLY_GRANTED,
                    ImagePickerContract.PermissionCheckSource.RESUME,
                )
            )
            expectNoEvents()
            assertEquals(PermissionStatus.PARTIALLY_GRANTED, vm.state.value.permissionStatus)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `PARTIALLY_GRANTED second time does not send RequestPermission again`() = runTest {
        val vm = ImagePickerViewModel()
        vm.effect.test {
            // First evaluation sets the flag and emits RequestPermission
            vm.handleIntent(
                ImagePickerContract.Intent.OnPermissionEvaluated(
                    PermissionStatus.PARTIALLY_GRANTED,
                    ImagePickerContract.PermissionCheckSource.INITIAL,
                )
            )
            assertIs<ImagePickerContract.Effect.RequestPermission>(awaitItem())
            assertTrue(vm.state.value.hasRequestedFullAccessAfterPartial)

            // Second evaluation: flag already set, should produce no effect
            vm.handleIntent(
                ImagePickerContract.Intent.OnPermissionEvaluated(
                    PermissionStatus.PARTIALLY_GRANTED,
                    ImagePickerContract.PermissionCheckSource.PERMISSION_RESULT,
                )
            )
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PERMANENTLY_DENIED → settings navigation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `PERMANENTLY_DENIED on PERMISSION_RESULT sends NavigateToSettings`() = runTest {
        val vm = ImagePickerViewModel()
        vm.effect.test {
            vm.handleIntent(
                ImagePickerContract.Intent.OnPermissionEvaluated(
                    PermissionStatus.PERMANENTLY_DENIED,
                    ImagePickerContract.PermissionCheckSource.PERMISSION_RESULT,
                )
            )
            assertIs<ImagePickerContract.Effect.NavigateToSettings>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `PERMANENTLY_DENIED on INITIAL does not send NavigateToSettings`() = runTest {
        val vm = ImagePickerViewModel()
        vm.effect.test {
            vm.handleIntent(
                ImagePickerContract.Intent.OnPermissionEvaluated(
                    PermissionStatus.PERMANENTLY_DENIED,
                    ImagePickerContract.PermissionCheckSource.INITIAL,
                )
            )
            expectNoEvents()
            assertEquals(PermissionStatus.PERMANENTLY_DENIED, vm.state.value.permissionStatus)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GRANTED
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GRANTED resets hasRequestedFullAccessAfterPartial flag`() = runTest {
        val vm = ImagePickerViewModel()
        vm.effect.test {
            // Set flag via partial grant
            vm.handleIntent(
                ImagePickerContract.Intent.OnPermissionEvaluated(
                    PermissionStatus.PARTIALLY_GRANTED,
                    ImagePickerContract.PermissionCheckSource.INITIAL,
                )
            )
            awaitItem() // consume RequestPermission effect
            assertTrue(vm.state.value.hasRequestedFullAccessAfterPartial)

            // Full grant should reset the flag
            vm.handleIntent(
                ImagePickerContract.Intent.OnPermissionEvaluated(
                    PermissionStatus.GRANTED,
                    ImagePickerContract.PermissionCheckSource.PERMISSION_RESULT,
                )
            )
            assertEquals(PermissionStatus.GRANTED, vm.state.value.permissionStatus)
            assertFalse(vm.state.value.hasRequestedFullAccessAfterPartial)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
