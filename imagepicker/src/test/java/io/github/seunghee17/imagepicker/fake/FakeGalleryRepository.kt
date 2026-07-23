package io.github.seunghee17.imagepicker.fake

import androidx.paging.PagingData
import io.github.seunghee17.imagepicker.domain.model.GalleryAlbum
import io.github.seunghee17.imagepicker.domain.model.GalleryImage
import io.github.seunghee17.imagepicker.domain.repository.GalleryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

internal class FakeGalleryRepository : GalleryRepository {

    val albums = MutableStateFlow<List<GalleryAlbum>>(emptyList())

    override fun getAlbums(): Flow<List<GalleryAlbum>> = albums

    override fun getPagedImages(albumId: String?): Flow<PagingData<GalleryImage>> =
        flowOf(PagingData.empty())
}
