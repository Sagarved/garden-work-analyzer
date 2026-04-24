package com.gardenworkanalyzer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gardenworkanalyzer.domain.model.MAX_IMAGE_COUNT
import com.gardenworkanalyzer.domain.model.MIN_IMAGE_COUNT

/**
 * A Material 3 button that triggers garden image analysis.
 *
 * Enabled only when [imageCount] is in [MIN_IMAGE_COUNT]..[MAX_IMAGE_COUNT] (1..10).
 * When disabled (0 images), shows a helper text prompting the user to add images.
 *
 * @param imageCount Current number of images in the collection.
 * @param onAnalyze Callback invoked when the user taps the button.
 * @param modifier Modifier for the column wrapper.
 */
@Composable
fun AnalyzeButton(
    imageCount: Int,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnabled = imageCount in MIN_IMAGE_COUNT..MAX_IMAGE_COUNT

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onAnalyze,
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Analyze Garden")
        }

        if (imageCount < MIN_IMAGE_COUNT) {
            Text(
                text = "Add images to analyze",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
