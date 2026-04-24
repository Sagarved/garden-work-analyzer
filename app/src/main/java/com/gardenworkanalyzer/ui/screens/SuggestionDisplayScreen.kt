package com.gardenworkanalyzer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gardenworkanalyzer.domain.model.CONFIDENCE_THRESHOLD
import com.gardenworkanalyzer.domain.model.GardenAnalysisResult
import com.gardenworkanalyzer.domain.model.GardenImage
import com.gardenworkanalyzer.domain.model.GardenWorkSuggestion

/**
 * Displays garden analysis results: an image strip of analyzed images and
 * suggestion cards filtered to confidence ≥ [CONFIDENCE_THRESHOLD] and sorted
 * by confidence descending. Cards expand on tap to reveal detailed guidance.
 *
 * Handles two empty states:
 * - No garden content detected in images
 * - Garden content detected but no suggestions above threshold
 *
 * @param result The [GardenAnalysisResult] from the ML analysis.
 * @param analyzedImages The list of [GardenImage] that were analyzed.
 * @param modifier Modifier for the root layout.
 */
@Composable
fun SuggestionDisplayScreen(
    result: GardenAnalysisResult,
    analyzedImages: List<GardenImage>,
    modifier: Modifier = Modifier
) {
    // Requirement 7.3: Filter to confidence >= CONFIDENCE_THRESHOLD
    // Requirement 7.2: Sort by confidence descending
    val displayedSuggestions = remember(result.suggestions) {
        result.suggestions
            .filter { it.confidence >= CONFIDENCE_THRESHOLD }
            .sortedByDescending { it.confidence }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Requirement 7.6: Analyzed images horizontal strip
        if (analyzedImages.isNotEmpty()) {
            Text(
                text = "Analyzed Images",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            AnalyzedImageStrip(images = analyzedImages)
        }

        // Requirement 6.5 / 7.5: Empty states
        if (!result.gardenContentDetected) {
            EmptyStateMessage(
                message = "No garden content detected in the provided images"
            )
        } else if (displayedSuggestions.isEmpty()) {
            EmptyStateMessage(
                message = "No garden work suggestions identified"
            )
        } else {
            // Requirement 7.1: Suggestion cards
            Text(
                text = "Suggestions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            SuggestionCardList(suggestions = displayedSuggestions)
        }
    }
}

/**
 * Horizontal strip of analyzed images using Coil [AsyncImage].
 */
@Composable
private fun AnalyzedImageStrip(
    images: List<GardenImage>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(
            items = images,
            key = { it.uri.toString() + it.addedTimestamp }
        ) { image ->
            AsyncImage(
                model = image.thumbnailUri ?: image.uri,
                contentDescription = "Analyzed garden image",
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Scrollable list of expandable suggestion cards.
 */
@Composable
private fun SuggestionCardList(
    suggestions: List<GardenWorkSuggestion>,
    modifier: Modifier = Modifier
) {
    // Track expanded state per suggestion index
    val expandedStates = remember { mutableStateMapOf<Int, Boolean>() }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(
            count = suggestions.size,
            key = { it }
        ) { index ->
            val suggestion = suggestions[index]
            val isExpanded = expandedStates[index] == true

            SuggestionCard(
                suggestion = suggestion,
                isExpanded = isExpanded,
                onToggleExpand = { expandedStates[index] = !isExpanded }
            )
        }
    }
}

/**
 * An expandable Material 3 card displaying a single [GardenWorkSuggestion].
 *
 * Shows type name, description, and confidence percentage.
 * Expands on tap to reveal [GardenWorkSuggestion.detailedGuidance].
 */
@Composable
private fun SuggestionCard(
    suggestion: GardenWorkSuggestion,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val confidencePercent = (suggestion.confidence * 100).toInt()

    Card(
        onClick = onToggleExpand,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "${suggestion.type.name} suggestion, $confidencePercent percent confidence"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Requirement 7.1: Type name
            Text(
                text = suggestion.type.name.replace('_', ' '),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Requirement 7.1: Description
            Text(
                text = suggestion.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Requirement 7.1: Confidence score as percentage
            Text(
                text = "Confidence: $confidencePercent%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Requirement 7.4: Expand on tap to show detailed guidance
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Text(
                    text = suggestion.detailedGuidance,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Centered empty-state message.
 */
@Composable
private fun EmptyStateMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
