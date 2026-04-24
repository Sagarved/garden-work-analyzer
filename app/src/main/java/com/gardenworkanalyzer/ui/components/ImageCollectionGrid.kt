package com.gardenworkanalyzer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gardenworkanalyzer.domain.model.GardenImage
import com.gardenworkanalyzer.domain.model.MAX_IMAGE_COUNT

/**
 * A scrollable grid displaying garden image thumbnails with management controls.
 *
 * Displays images in a 3-column [LazyVerticalGrid] with Coil-loaded thumbnails.
 * Each cell supports tap-to-preview and has a remove button overlay.
 * Reordering is provided via move-up/move-down buttons on a selected item
 * (a practical simplification of full drag-and-drop in LazyVerticalGrid).
 * An image count badge shows the current count vs. maximum.
 *
 * @param images The current list of [GardenImage] items to display.
 * @param imageCount The total number of images in the collection.
 * @param onRemove Called with the index of the image to remove.
 * @param onReorder Called with (fromIndex, toIndex) to reorder images.
 * @param onPreview Called with the index of the image to preview full-size.
 * @param modifier Modifier for the root layout.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageCollectionGrid(
    images: List<GardenImage>,
    imageCount: Int,
    onRemove: (Int) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onPreview: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Track which item is selected for reordering (-1 = none)
    var selectedIndex by remember { mutableIntStateOf(-1) }

    Column(modifier = modifier) {
        // Requirement 3.4: Image count badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Images",
                style = MaterialTheme.typography.titleMedium
            )
            Badge(
                modifier = Modifier.semantics {
                    contentDescription = "$imageCount of $MAX_IMAGE_COUNT images"
                }
            ) {
                Text(text = "$imageCount/$MAX_IMAGE_COUNT")
            }
        }

        // Requirement 3.1: Scrollable thumbnail grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(
                items = images,
                key = { _, image -> image.uri.toString() + image.addedTimestamp }
            ) { index, image ->
                ImageThumbnailCell(
                    image = image,
                    index = index,
                    isSelected = index == selectedIndex,
                    canMoveUp = index > 0,
                    canMoveDown = index < images.lastIndex,
                    onTap = { onPreview(index) },
                    onLongPress = {
                        selectedIndex = if (selectedIndex == index) -1 else index
                    },
                    onRemove = {
                        if (selectedIndex == index) selectedIndex = -1
                        onRemove(index)
                    },
                    onMoveUp = {
                        onReorder(index, index - 1)
                        selectedIndex = index - 1
                    },
                    onMoveDown = {
                        onReorder(index, index + 1)
                        selectedIndex = index + 1
                    }
                )
            }
        }
    }
}

/**
 * A single thumbnail cell in the image grid.
 *
 * Shows the image via Coil [AsyncImage], a remove (X) button overlay,
 * and optional move-up/move-down buttons when selected for reordering.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageThumbnailCell(
    image: GardenImage,
    index: Int,
    isSelected: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onTap,
                    onLongClick = onLongPress
                )
                .semantics {
                    contentDescription = "Image ${index + 1}"
                }
        ) {
            // Requirement 3.2: Thumbnail loaded via Coil
            AsyncImage(
                model = image.thumbnailUri ?: image.uri,
                contentDescription = "Garden image ${index + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Requirement 3.3: Remove button overlay
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .padding(2.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove image ${index + 1}",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }

            // Sequence index label
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // Requirement 4.2: Reorder controls (shown when selected via long-press)
            if (isSelected) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp)
                        )
                ) {
                    if (canMoveUp) {
                        IconButton(
                            onClick = onMoveUp,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move image ${index + 1} up",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (canMoveDown) {
                        IconButton(
                            onClick = onMoveDown,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move image ${index + 1} down",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
