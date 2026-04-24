package com.gardenworkanalyzer.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gardenworkanalyzer.data.repository.ImageRepository
import com.gardenworkanalyzer.domain.model.GardenImage
import com.gardenworkanalyzer.domain.usecase.ManageImageCollectionUseCase
import com.gardenworkanalyzer.domain.usecase.SequenceImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageCollectionViewModel @Inject constructor(
    private val manageImageCollectionUseCase: ManageImageCollectionUseCase,
    private val sequenceImagesUseCase: SequenceImagesUseCase,
    imageRepository: ImageRepository
) : ViewModel() {

    val imageCollection: StateFlow<List<GardenImage>> = imageRepository.getImages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val imageCount: StateFlow<Int> = imageCollection
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addImages(uris: List<Uri>) {
        viewModelScope.launch {
            manageImageCollectionUseCase.addImages(uris)
        }
    }

    fun removeImage(index: Int) {
        viewModelScope.launch {
            manageImageCollectionUseCase.removeImage(index)
        }
    }

    fun reorderImages(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            sequenceImagesUseCase.reorder(fromIndex, toIndex)
        }
    }

    fun canAddImages(count: Int): Boolean {
        return !manageImageCollectionUseCase.isCollectionFull(imageCollection.value.size, count)
    }
}
