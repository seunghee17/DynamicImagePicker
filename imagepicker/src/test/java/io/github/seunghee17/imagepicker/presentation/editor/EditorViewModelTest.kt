package io.github.seunghee17.imagepicker.presentation.editor

import android.net.Uri
import app.cash.turbine.test
import io.github.seunghee17.imagepicker.CropRect
import io.github.seunghee17.imagepicker.domain.usecase.CropImageUseCase
import io.github.seunghee17.imagepicker.domain.usecase.RotateImageUseCase
import io.github.seunghee17.imagepicker.fake.FakeImageEditRepository
import io.github.seunghee17.imagepicker.util.MainDispatcherRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val originalUri: Uri = mockk(relaxed = true)

    private fun createVm(repo: FakeImageEditRepository = FakeImageEditRepository()): EditorViewModel =
        EditorViewModel(
            originalUri = originalUri,
            rotateImage = RotateImageUseCase(repo),
            cropImage = CropImageUseCase(repo),
        )

    // ─────────────────────────────────────────────────────────────────────────
    // Rotate state update
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `RotateClockwise updates previewUri to rotated result`() = runTest {
        val rotatedUri = mockk<Uri>(relaxed = true)
        val repo = FakeImageEditRepository().apply { rotateResult = rotatedUri }
        val vm = createVm(repo)

        vm.handleIntent(EditorContract.Intent.RotateClockwise)

        assertEquals(rotatedUri, vm.state.value.previewUri)
    }

    @Test
    fun `totalRotation wraps to 0 after four rotations`() = runTest {
        val vm = createVm()
        repeat(4) { vm.handleIntent(EditorContract.Intent.RotateClockwise) }
        assertEquals(0, vm.state.value.totalRotation)
    }

    @Test
    fun `rotate failure emits ShowError effect`() = runTest {
        val repo = FakeImageEditRepository().apply { rotateThrows = true }
        val vm = createVm(repo)

        vm.effect.test {
            vm.handleIntent(EditorContract.Intent.RotateClockwise)
            assertIs<EditorContract.Effect.ShowError>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Crop state commit
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `ApplyCrop updates committedUri and previewUri to cropped result`() = runTest {
        val croppedUri = mockk<Uri>(relaxed = true)
        val repo = FakeImageEditRepository().apply { cropResult = croppedUri }
        val vm = createVm(repo)

        vm.handleIntent(EditorContract.Intent.EnterCropMode)
        vm.handleIntent(EditorContract.Intent.ApplyCrop)

        assertEquals(croppedUri, vm.state.value.committedUri)
        assertEquals(croppedUri, vm.state.value.previewUri)
    }

    @Test
    fun `ApplyCrop resets pendingRotation to 0`() = runTest {
        val vm = createVm()
        vm.handleIntent(EditorContract.Intent.RotateClockwise) // pendingRotation = 90
        vm.handleIntent(EditorContract.Intent.EnterCropMode)
        vm.handleIntent(EditorContract.Intent.ApplyCrop)

        assertEquals(0, vm.state.value.pendingRotation)
    }

    @Test
    fun `ApplyCrop resets cropRect to FULL`() = runTest {
        val vm = createVm()
        vm.handleIntent(
            EditorContract.Intent.UpdateCropRect(CropRect(0.1f, 0.1f, 0.9f, 0.9f))
        )
        vm.handleIntent(EditorContract.Intent.EnterCropMode)
        vm.handleIntent(EditorContract.Intent.ApplyCrop)

        assertEquals(CropRect.FULL, vm.state.value.cropRect)
    }

    @Test
    fun `ExitCropMode restores cropRect to value on enter`() = runTest {
        val vm = createVm()
        val rect = CropRect(0.2f, 0.2f, 0.8f, 0.8f)
        vm.handleIntent(EditorContract.Intent.UpdateCropRect(rect))
        vm.handleIntent(EditorContract.Intent.EnterCropMode)   // saves rect as cropRectOnEnter
        vm.handleIntent(EditorContract.Intent.UpdateCropRect(CropRect(0.5f, 0.5f, 0.9f, 0.9f)))
        vm.handleIntent(EditorContract.Intent.ExitCropMode)

        assertEquals(rect, vm.state.value.cropRect)
    }

    @Test
    fun `crop failure emits ShowError effect`() = runTest {
        val repo = FakeImageEditRepository().apply { cropThrows = true }
        val vm = createVm(repo)
        vm.handleIntent(EditorContract.Intent.EnterCropMode)

        vm.effect.test {
            vm.handleIntent(EditorContract.Intent.ApplyCrop)
            assertIs<EditorContract.Effect.ShowError>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Combined scenarios: rotate ↔ crop ordering
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `rotate then crop uses rotated uri as crop source`() = runTest {
        val rotatedUri = mockk<Uri>(relaxed = true)
        val repo = FakeImageEditRepository().apply { rotateResult = rotatedUri }
        val vm = createVm(repo)

        vm.handleIntent(EditorContract.Intent.RotateClockwise)
        vm.handleIntent(EditorContract.Intent.EnterCropMode)
        vm.handleIntent(EditorContract.Intent.ApplyCrop)

        // cropImage must receive the rotated uri, not originalUri
        assertEquals(rotatedUri, repo.lastCropSourceUri)
        assertEquals(0, vm.state.value.pendingRotation)
    }

    @Test
    fun `crop then rotate uses cropped uri as rotate source`() = runTest {
        val croppedUri = mockk<Uri>(relaxed = true)
        val repo = FakeImageEditRepository().apply { cropResult = croppedUri }
        val vm = createVm(repo)

        vm.handleIntent(EditorContract.Intent.EnterCropMode)
        vm.handleIntent(EditorContract.Intent.ApplyCrop)
        vm.handleIntent(EditorContract.Intent.RotateClockwise)

        // rotateImage must receive the cropped uri, not originalUri
        assertEquals(croppedUri, repo.lastRotateSourceUri)
    }

    @Test
    fun `rotate then crop then save returns crop result as editedUri`() = runTest {
        val rotatedUri = mockk<Uri>(relaxed = true)
        val croppedUri = mockk<Uri>(relaxed = true)
        val repo = FakeImageEditRepository().apply {
            rotateResult = rotatedUri
            cropResult = croppedUri
        }
        val vm = createVm(repo)

        vm.handleIntent(EditorContract.Intent.RotateClockwise)
        vm.handleIntent(EditorContract.Intent.EnterCropMode)
        vm.handleIntent(EditorContract.Intent.ApplyCrop)

        vm.effect.test {
            vm.handleIntent(EditorContract.Intent.SaveAndReturn)
            val item = awaitItem() as EditorContract.Effect.ReturnEditedImage
            assertEquals(originalUri, item.pickedImage.originalUri)
            assertEquals(croppedUri, item.pickedImage.editedUri)
            assertEquals(90, item.pickedImage.rotationDegrees)
            assertTrue(item.pickedImage.isCropped)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save result correctness
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `SaveAndReturn with no edits returns null editedUri`() = runTest {
        val vm = createVm()

        vm.effect.test {
            vm.handleIntent(EditorContract.Intent.SaveAndReturn)
            val item = awaitItem() as EditorContract.Effect.ReturnEditedImage
            assertNull(item.pickedImage.editedUri)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SaveAndReturn after rotate returns editedUri and correct rotationDegrees`() = runTest {
        val rotatedUri = mockk<Uri>(relaxed = true)
        val repo = FakeImageEditRepository().apply { rotateResult = rotatedUri }
        val vm = createVm(repo)
        vm.handleIntent(EditorContract.Intent.RotateClockwise)

        vm.effect.test {
            vm.handleIntent(EditorContract.Intent.SaveAndReturn)
            val item = awaitItem() as EditorContract.Effect.ReturnEditedImage
            assertEquals(originalUri, item.pickedImage.originalUri)
            assertEquals(rotatedUri, item.pickedImage.editedUri)
            assertEquals(90, item.pickedImage.rotationDegrees)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SaveAndReturn after crop sets isCropped`() = runTest {
        val croppedUri = mockk<Uri>(relaxed = true)
        val repo = FakeImageEditRepository().apply { cropResult = croppedUri }
        val vm = createVm(repo)
        vm.handleIntent(EditorContract.Intent.EnterCropMode)
        vm.handleIntent(EditorContract.Intent.ApplyCrop)

        vm.effect.test {
            vm.handleIntent(EditorContract.Intent.SaveAndReturn)
            val item = awaitItem() as EditorContract.Effect.ReturnEditedImage
            assertTrue(item.pickedImage.isCropped)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
