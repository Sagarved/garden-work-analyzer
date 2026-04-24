package com.gardenworkanalyzer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.gardenworkanalyzer.domain.model.AnalysisUiState
import com.gardenworkanalyzer.domain.model.GardenImage
import com.gardenworkanalyzer.ui.components.AnalyzeButton
import com.gardenworkanalyzer.ui.components.CameraCaptureButton
import com.gardenworkanalyzer.ui.components.GalleryPickerButton
import com.gardenworkanalyzer.ui.components.ImageCollectionGrid
import com.gardenworkanalyzer.ui.permission.PermissionManager
import com.gardenworkanalyzer.ui.viewmodel.AnalysisViewModel
import com.gardenworkanalyzer.ui.viewmodel.ImageCollectionViewModel

/**
 * Root screen composable hosting the image collection workflow.
 *
 * Layout (Material 3 Scaffold):
 * - TopAppBar with app title
 * - Top row: GalleryPickerButton + CameraCaptureButton
 * - Middle: ImageCollectionGrid (weight 1f, fills available space)
 * - Bottom: AnalyzeButton
 *
 * Handles analysis state transitions:
 * - Uploading → progress overlay
 * - Success → navigates to SuggestionDisplayScreen
 * - Error → Snackbar with retry action
 *
 * Requirements: 1.1, 2.1, 3.1, 4.3, 5.2, 7.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    imageCollectionViewModel: ImageCollectionViewModel = hiltViewModel(),
    analysisViewModel: AnalysisViewModel = hiltViewModel(),
    permissionManager: PermissionManager = remember { PermissionManager() }
) {
    val imageCollection by imageCollectionViewModel.imageCollection.collectAsState()
    val imageCount by imageCollectionViewModel.imageCount.collectAsState()
    val analysisState by analysisViewModel.analysisState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Image preview dialog state
    var previewImage by remember { mutableStateOf<GardenImage?>(null) }

    // Track whether we're showing the suggestion screen
    var showSuggestions by remember { mutableStateOf(false) }

    // Navigate to suggestions on success
    LaunchedEffect(analysisState) {
        if (analysisState is AnalysisUiState.Success) {
            showSuggestions = true
        }
    }

    // Show snackbar on error
    LaunchedEffect(analysisState) {
        val state = analysisState
        if (state is AnalysisUiState.Error) {
            val result = snackbarHostState.showSnackbar(
                message = state.message,
                actionLabel = "Retry",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                analysisViewModel.retry()
            }
        }
    }

    // If analysis succeeded, show the suggestion screen
    if (showSuggestions) {
        val successState = analysisState as? AnalysisUiState.Success
        if (successState != null) {
            SuggestionResultScreen(
                result = successState.result,
                analyzedImages = imageCollection,
                onBack = { showSuggestions = false }
            )
            return
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Garden Work Analyzer") }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Top: Gallery + Camera buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GalleryPickerButton(
                        onImagesSelected = { uris -> imageCollectionViewModel.addImages(uris) },
                        enabled = imageCollectionViewModel.canAddImages(1),
                        modifier = Modifier.weight(1f)
                    )
                    CameraCaptureButton(
                        onImageCaptured = { uri -> imageCollectionViewModel.addImages(listOf(uri)) },
                        permissionManager = permissionManager,
                        enabled = imageCollectionViewModel.canAddImages(1),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Middle: Image grid (takes remaining space)
                ImageCollectionGrid(
                    images = imageCollection,
                    imageCount = imageCount,
                    onRemove = { index -> imageCollectionViewModel.removeImage(index) },
                    onReorder = { from, to -> imageCollectionViewModel.reorderImages(from, to) },
                    onPreview = { index ->
                        if (index in imageCollection.indices) {
                            previewImage = imageCollection[index]
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                // Bottom: Analyze button
                AnalyzeButton(
                    imageCount = imageCount,
                    onAnalyze = { analysisViewModel.analyze() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            // Uploading overlay with progress indicator (Req 5.2)
            if (analysisState is AnalysisUiState.Uploading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp))
                }
            }
        }
    }

    // Image preview dialog (Req 3.2)
    previewImage?.let { image ->
        ImagePreviewDialog(
            image = image,
            onDismiss = { previewImage = null }
        )
    }
}

/**
 * Full-screen dialog showing a full-size image preview.
 */
@Composable
private fun ImagePreviewDialog(
    image: GardenImage,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = image.uri,
                contentDescription = "Full-size image preview",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * Wrapper around [SuggestionDisplayScreen] with a back button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestionResultScreen(
    result: com.gardenworkanalyzer.domain.model.GardenAnalysisResult,
    analyzedImages: List<GardenImage>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis Results") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        SuggestionDisplayScreen(
            result = result,
            analyzedImages = analyzedImages,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
