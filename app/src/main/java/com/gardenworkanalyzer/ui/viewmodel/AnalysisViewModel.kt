package com.gardenworkanalyzer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gardenworkanalyzer.data.repository.GardenAnalysisRepository
import com.gardenworkanalyzer.domain.model.AnalysisUiState
import com.gardenworkanalyzer.domain.model.MAX_RETRY_ATTEMPTS
import com.gardenworkanalyzer.domain.usecase.SequenceImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val sequenceImagesUseCase: SequenceImagesUseCase,
    private val gardenAnalysisRepository: GardenAnalysisRepository
) : ViewModel() {

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private var retryCount = 0

    fun analyze() {
        viewModelScope.launch {
            _analysisState.value = AnalysisUiState.Uploading(0f)

            try {
                val images = sequenceImagesUseCase.finalizeSequence()

                val result = gardenAnalysisRepository.analyze(images)

                result.fold(
                    onSuccess = { analysisResult ->
                        _analysisState.value = AnalysisUiState.Success(analysisResult)
                        retryCount = 0
                    },
                    onFailure = { throwable ->
                        _analysisState.value = AnalysisUiState.Error(
                            message = throwable.message ?: "Analysis failed",
                            retryCount = retryCount
                        )
                    }
                )
            } catch (e: Exception) {
                _analysisState.value = AnalysisUiState.Error(
                    message = e.message ?: "Analysis failed",
                    retryCount = retryCount
                )
            }
        }
    }

    fun retry() {
        retryCount++
        if (retryCount >= MAX_RETRY_ATTEMPTS) {
            _analysisState.value = AnalysisUiState.Error(
                message = "Check your network connection",
                retryCount = retryCount
            )
        } else {
            analyze()
        }
    }
}
