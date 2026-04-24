package com.gardenworkanalyzer.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A Material 3 button that opens the system gallery picker for selecting multiple images.
 *
 * Uses [ActivityResultContracts.GetMultipleContents] with an image MIME filter.
 * On cancel (empty list returned), this is a no-op: the image collection remains unchanged.
 *
 * @param onImagesSelected Callback invoked with the list of selected image URIs.
 *                         Not called when the user cancels (empty result).
 * @param enabled Whether the button is enabled.
 * @param modifier Modifier for the button.
 */
private const val IMAGE_MIME_FILTER = "image" + "/" + "*"

@Composable
fun GalleryPickerButton(
    onImagesSelected: (List<Uri>) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onImagesSelected(uris)
        }
    }

    Button(
        onClick = { launcher.launch(IMAGE_MIME_FILTER) },
        enabled = enabled,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null
        )
        Text(text = "Gallery")
    }
}
