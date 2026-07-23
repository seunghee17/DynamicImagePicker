package io.github.seunghee17.imagepicker.presentation.gallery

import android.net.Uri
import app.cash.turbine.test
import io.github.seunghee17.imagepicker.PickedImage
import io.github.seunghee17.imagepicker.domain.model.GalleryAlbum
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import io.github.seunghee17.imagepicker.domain.model.MediaType
import io.github.seunghee17.imagepicker.domain.usecase.ClearEditCacheUseCase
import io.github.seunghee17.imagepicker.domain.usecase.GetGalleryAlbumsUseCase
import io.github.seunghee17.imagepicker.domain.usecase.GetPagedImagesUseCase
import io.github.seunghee17.imagepicker.fake.FakeGalleryRepository
import io.github.seunghee17.imagepicker.fake.FakeImageEditRepository
import io.github.seunghee17.imagepicker.util.MainDispatcherRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryScreenViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun fakeImage(id: Long): GalleryImage = GalleryImage(
        id = id,
        uri = mockk(relaxed = true),
        displayName = "image_$id.jpg",
        dateTaken = 0L,
        albumId = "album1",
        albumName = "Camera",
        width = 100,
        height = 100,
        mimeType = "image/jpeg",
        mediaType = MediaType.IMAGE,
    )

    private fun fakeAlbum(id: String, name: String): GalleryAlbum = GalleryAlbum(
        id = id,
        name = name,
        coverUri = mockk(relaxed = true),
        count = 1,
    )

    private fun createVm(
        maxSelectionCount: Int = 10,
        fakeGallery: FakeGalleryRepository = FakeGalleryRepository(),
    ): GalleryScreenViewModel = GalleryScreenViewModel(
        getAlbums = GetGalleryAlbumsUseCase(fakeGallery),
        getPagedImages = GetPagedImagesUseCase(fakeGallery),
        clearEditCache = ClearEditCacheUseCase(FakeImageEditRepository()),
        maxSelectionCount = maxSelectionCount,
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Album loading state
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `isAlbumsLoading is true in initial state`() = runTest {
        val vm = createVm()
        assertTrue(vm.state.value.isAlbumsLoading)
    }

    @Test
    fun `isAlbumsLoading becomes false after first album emission`() = runTest {
        val fakeGallery = FakeGalleryRepository()
        val vm = createVm(fakeGallery = fakeGallery)
        vm.handleIntent(GalleryContract.Intent.Initialize)

        fakeGallery.albums.value = listOf(fakeAlbum("1", "Camera"))

        assertFalse(vm.state.value.isAlbumsLoading)
    }

    @Test
    fun `first album in list becomes selectedAlbum when none was pre-selected`() = runTest {
        val fakeGallery = FakeGalleryRepository()
        val vm = createVm(fakeGallery = fakeGallery)
        val album = fakeAlbum("1", "Camera")
        vm.handleIntent(GalleryContract.Intent.Initialize)

        fakeGallery.albums.value = listOf(album)

        assertEquals(album, vm.state.value.selectedAlbum)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Album switching safety
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `rapid album switching keeps only last selection`() = runTest {
        val fakeGallery = FakeGalleryRepository()
        val vm = createVm(fakeGallery = fakeGallery)
        val album1 = fakeAlbum("1", "Camera")
        val album2 = fakeAlbum("2", "Downloads")
        val album3 = fakeAlbum("3", "Screenshots")
        vm.handleIntent(GalleryContract.Intent.Initialize)
        fakeGallery.albums.value = listOf(album1, album2, album3)

        vm.handleIntent(GalleryContract.Intent.SelectAlbum(album1))
        vm.handleIntent(GalleryContract.Intent.SelectAlbum(album2))
        vm.handleIntent(GalleryContract.Intent.SelectAlbum(album3))

        assertEquals(album3, vm.state.value.selectedAlbum)
    }

    @Test
    fun `album list refresh does not overwrite user album selection`() = runTest {
        val fakeGallery = FakeGalleryRepository()
        val vm = createVm(fakeGallery = fakeGallery)
        val album1 = fakeAlbum("1", "Camera")
        val album2 = fakeAlbum("2", "Downloads")
        vm.handleIntent(GalleryContract.Intent.Initialize)
        fakeGallery.albums.value = listOf(album1, album2)

        vm.handleIntent(GalleryContract.Intent.SelectAlbum(album2))
        assertEquals(album2, vm.state.value.selectedAlbum)

        // MediaStore 갱신으로 앨범 목록이 다시 방출되어도 사용자 선택이 유지돼야 한다
        fakeGallery.albums.value = listOf(album1, album2)

        assertEquals(album2, vm.state.value.selectedAlbum)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Selection toggle
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `toggling unselected image adds it to selectedImages`() = runTest {
        val vm = createVm()
        val image = fakeImage(1)

        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(image))

        assertEquals(listOf(image), vm.state.value.selectedImages)
    }

    @Test
    fun `toggling already selected image removes it from selectedImages`() = runTest {
        val vm = createVm()
        val image = fakeImage(1)

        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(image))
        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(image))

        assertTrue(vm.state.value.selectedImages.isEmpty())
    }

    @Test
    fun `selectionOrderMap reflects insertion order`() = runTest {
        val vm = createVm()
        val image1 = fakeImage(1)
        val image2 = fakeImage(2)

        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(image1))
        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(image2))

        assertEquals(1, vm.state.value.selectionOrderMap[1L])
        assertEquals(2, vm.state.value.selectionOrderMap[2L])
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Max selection enforcement
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `selecting beyond limit emits ShowSelectionLimitSnackbar`() = runTest {
        val vm = createVm(maxSelectionCount = 2)

        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(fakeImage(1)))
        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(fakeImage(2)))

        vm.effect.test {
            vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(fakeImage(3)))
            val item = awaitItem()
            assertIs<GalleryContract.Effect.ShowSelectionLimitSnackbar>(item)
            assertEquals(2, item.maxSelectionCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `image beyond limit is not added to selectedImages`() = runTest {
        val vm = createVm(maxSelectionCount = 2)

        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(fakeImage(1)))
        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(fakeImage(2)))
        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(fakeImage(3)))

        assertEquals(2, vm.state.value.selectedImages.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Drag range selection (anchor~current, Google Photos style)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `dragging over a row selects the whole range in grid order`() = runTest {
        val vm = createVm()
        val images = (1..7L).map { fakeImage(it) }

        vm.handleIntent(GalleryContract.Intent.BeginDragSelection(images[1])) // index 1
        vm.handleIntent(GalleryContract.Intent.UpdateDragSelectionRange(images.subList(1, 7))) // index 1..6

        assertEquals(images.subList(1, 7), vm.state.value.selectedImages)
    }

    @Test
    fun `reversing drag direction shrinks the range back to baseline`() = runTest {
        val vm = createVm()
        val images = (1..7L).map { fakeImage(it) }

        vm.handleIntent(GalleryContract.Intent.BeginDragSelection(images[1]))
        vm.handleIntent(GalleryContract.Intent.UpdateDragSelectionRange(images.subList(1, 7)))
        // 손가락을 되돌려 anchor~index3 까지만 남김
        vm.handleIntent(GalleryContract.Intent.UpdateDragSelectionRange(images.subList(1, 4)))

        assertEquals(images.subList(1, 4), vm.state.value.selectedImages)
    }

    @Test
    fun `pre-existing tap selections outside the range are preserved during select-mode drag`() = runTest {
        val vm = createVm()
        val tapped = fakeImage(100)
        val images = (1..7L).map { fakeImage(it) }
        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(tapped))

        vm.handleIntent(GalleryContract.Intent.BeginDragSelection(images[1]))
        vm.handleIntent(GalleryContract.Intent.UpdateDragSelectionRange(images.subList(1, 4)))

        assertTrue(vm.state.value.selectedImages.containsAll(images.subList(1, 4)))
        assertTrue(vm.state.value.selectedImages.contains(tapped))
    }

    @Test
    fun `long-pressing an already-selected image enters drag-deselect mode`() = runTest {
        val vm = createVm()
        val images = (1..7L).map { fakeImage(it) }
        images.forEach { vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(it)) }

        vm.handleIntent(GalleryContract.Intent.BeginDragSelection(images[1]))
        vm.handleIntent(GalleryContract.Intent.UpdateDragSelectionRange(images.subList(1, 4)))

        assertEquals(
            listOf(images[0]) + images.subList(4, 7),
            vm.state.value.selectedImages,
        )
    }

    @Test
    fun `drag-deselect removes range items even if pre-existing outside this drag`() = runTest {
        val vm = createVm()
        val images = (1..20L).map { fakeImage(it) }
        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(images[4])) // id=5
        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(images[19])) // id=20

        vm.handleIntent(GalleryContract.Intent.BeginDragSelection(images[4]))
        vm.handleIntent(
            GalleryContract.Intent.UpdateDragSelectionRange(images.subList(4, 20))
        )

        assertTrue(vm.state.value.selectedImages.isEmpty())
    }

    @Test
    fun `range selection beyond max fills up to the limit and emits snackbar once`() = runTest {
        val vm = createVm(maxSelectionCount = 10)
        val images = (1..20L).map { fakeImage(it) }
        images.subList(0, 8).forEach { vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(it)) }

        vm.handleIntent(GalleryContract.Intent.BeginDragSelection(images[8]))
        vm.effect.test {
            vm.handleIntent(GalleryContract.Intent.UpdateDragSelectionRange(images.subList(8, 11)))
            val item = awaitItem()
            assertIs<GalleryContract.Effect.ShowSelectionLimitSnackbar>(item)
            // 다음 tick에서도 계속 초과 상태라면 스낵바를 다시 보내지 않는다
            vm.handleIntent(GalleryContract.Intent.UpdateDragSelectionRange(images.subList(8, 12)))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(10, vm.state.value.selectedImages.size)
    }

    @Test
    fun `ending drag selection allows a fresh drag gesture to start its own baseline`() = runTest {
        val vm = createVm()
        val images = (1..7L).map { fakeImage(it) }

        vm.handleIntent(GalleryContract.Intent.BeginDragSelection(images[1]))
        vm.handleIntent(GalleryContract.Intent.UpdateDragSelectionRange(images.subList(1, 4)))
        vm.handleIntent(GalleryContract.Intent.EndDragSelection)

        vm.handleIntent(GalleryContract.Intent.BeginDragSelection(images[4]))
        vm.handleIntent(GalleryContract.Intent.UpdateDragSelectionRange(images.subList(4, 6)))

        assertEquals(images.subList(1, 4) + images.subList(4, 6), vm.state.value.selectedImages)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Confirm result composition
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `Confirm emits SelectionConfirmed with selected images`() = runTest {
        val vm = createVm()
        val image = fakeImage(1)
        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(image))

        vm.effect.test {
            vm.handleIntent(GalleryContract.Intent.Confirm)
            val item = awaitItem()
            assertIs<GalleryContract.Effect.SelectionConfirmed>(item)
            assertEquals(1, item.result.items.size)
            assertEquals(image.uri, item.result.items[0].originalUri)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Confirm resets selectedImages`() = runTest {
        val vm = createVm()
        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(fakeImage(1)))
        vm.handleIntent(GalleryContract.Intent.Confirm)

        assertTrue(vm.state.value.selectedImages.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cancel
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `Cancel resets selectedImages and editResults`() = runTest {
        val vm = createVm()
        val image = fakeImage(1)
        val editedUri = mockk<Uri>(relaxed = true)

        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(image))
        vm.handleIntent(
            GalleryContract.Intent.OnEditResult(
                PickedImage(originalUri = image.uri, editedUri = editedUri)
            )
        )

        vm.handleIntent(GalleryContract.Intent.Cancel)

        assertTrue(vm.state.value.selectedImages.isEmpty())
        assertTrue(vm.state.value.editResults.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edit result merge
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `edit result is merged into confirm result`() = runTest {
        val vm = createVm()
        val image = fakeImage(1)
        val editedUri = mockk<Uri>(relaxed = true)
        val pickedImage = PickedImage(
            originalUri = image.uri,
            editedUri = editedUri,
            rotationDegrees = 90,
        )

        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(image))
        vm.handleIntent(GalleryContract.Intent.OnEditResult(pickedImage))

        vm.effect.test {
            vm.handleIntent(GalleryContract.Intent.Confirm)
            val item = awaitItem() as GalleryContract.Effect.SelectionConfirmed
            val result = item.result.items[0]
            assertEquals(editedUri, result.editedUri)
            assertEquals(90, result.rotationDegrees)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `edit result for non-selected image is ignored`() = runTest {
        val vm = createVm()
        val selectedImage = fakeImage(1)
        val unrelatedImage = fakeImage(99)

        vm.handleIntent(GalleryContract.Intent.ToggleImageSelection(selectedImage))
        vm.handleIntent(
            GalleryContract.Intent.OnEditResult(
                PickedImage(originalUri = unrelatedImage.uri, rotationDegrees = 90)
            )
        )

        vm.effect.test {
            vm.handleIntent(GalleryContract.Intent.Confirm)
            val item = awaitItem() as GalleryContract.Effect.SelectionConfirmed
            assertNull(item.result.items[0].editedUri)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
