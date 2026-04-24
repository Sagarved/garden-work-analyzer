package com.gardenworkanalyzer.data.model

import com.gardenworkanalyzer.data.network.HuggingFaceClassification
import com.gardenworkanalyzer.domain.model.GardenWorkSuggestion
import com.gardenworkanalyzer.domain.model.GardenWorkType

/**
 * Maps HuggingFace image classification labels to garden work suggestions.
 *
 * The VIT model returns general image labels. We map plant/garden-related
 * labels to actionable garden work suggestions. Non-garden labels are filtered out.
 */
object GardenWorkMapper {

    private val labelToWorkType = mapOf(
        // Flowers and ornamental plants
        "daisy" to GardenWorkType.WATERING,
        "rose" to GardenWorkType.PRUNING,
        "sunflower" to GardenWorkType.WATERING,
        "tulip" to GardenWorkType.PLANTING,
        "dandelion" to GardenWorkType.WEEDING,
        "pot" to GardenWorkType.PLANTING,
        "flowerpot" to GardenWorkType.PLANTING,
        "vase" to GardenWorkType.WATERING,

        // Trees and shrubs
        "tree" to GardenWorkType.PRUNING,
        "shrub" to GardenWorkType.PRUNING,
        "hedge" to GardenWorkType.PRUNING,
        "bonsai" to GardenWorkType.PRUNING,

        // Garden and lawn
        "lawn" to GardenWorkType.WATERING,
        "grass" to GardenWorkType.WATERING,
        "garden" to GardenWorkType.GENERAL_MAINTENANCE,
        "greenhouse" to GardenWorkType.GENERAL_MAINTENANCE,
        "picket fence" to GardenWorkType.GENERAL_MAINTENANCE,

        // Soil and ground
        "soil" to GardenWorkType.FERTILIZING,
        "mud" to GardenWorkType.MULCHING,
        "compost" to GardenWorkType.FERTILIZING,

        // Pests and issues
        "slug" to GardenWorkType.PEST_CONTROL,
        "snail" to GardenWorkType.PEST_CONTROL,
        "bee" to GardenWorkType.GENERAL_MAINTENANCE,
        "butterfly" to GardenWorkType.GENERAL_MAINTENANCE,
        "ladybug" to GardenWorkType.PEST_CONTROL,
        "ant" to GardenWorkType.PEST_CONTROL,
        "spider" to GardenWorkType.PEST_CONTROL,

        // Vegetables and fruits
        "cucumber" to GardenWorkType.WATERING,
        "zucchini" to GardenWorkType.WATERING,
        "bell pepper" to GardenWorkType.WATERING,
        "mushroom" to GardenWorkType.GENERAL_MAINTENANCE,
        "strawberry" to GardenWorkType.WATERING,
        "lemon" to GardenWorkType.FERTILIZING,
        "orange" to GardenWorkType.FERTILIZING,
        "acorn" to GardenWorkType.PLANTING,

        // Tools and equipment
        "lawn mower" to GardenWorkType.GENERAL_MAINTENANCE,
        "hoe" to GardenWorkType.WEEDING,
        "rake" to GardenWorkType.MULCHING,
        "shovel" to GardenWorkType.PLANTING,
        "wheelbarrow" to GardenWorkType.GENERAL_MAINTENANCE
    )

    private val guidanceMap = mapOf(
        GardenWorkType.PRUNING to "Remove dead or overgrown branches to promote healthy growth. Use clean, sharp tools and cut at a 45-degree angle just above a bud.",
        GardenWorkType.WATERING to "Water deeply and less frequently to encourage deep root growth. Early morning is the best time to water. Avoid wetting the foliage.",
        GardenWorkType.WEEDING to "Remove weeds by pulling from the root. Apply mulch to prevent new weed growth. Consider using a hoe for larger areas.",
        GardenWorkType.PLANTING to "Choose plants suited to your climate and soil. Dig a hole twice the width of the root ball. Water thoroughly after planting.",
        GardenWorkType.FERTILIZING to "Apply balanced fertilizer during the growing season. Follow package instructions for dosage. Avoid over-fertilizing.",
        GardenWorkType.PEST_CONTROL to "Inspect plants regularly for signs of pests. Use organic pest control methods when possible. Remove affected leaves promptly.",
        GardenWorkType.MULCHING to "Apply 2-3 inches of organic mulch around plants. Keep mulch away from plant stems. Replenish as it decomposes.",
        GardenWorkType.GENERAL_MAINTENANCE to "Regularly inspect your garden for issues. Keep paths clear, tools clean, and maintain good air circulation between plants."
    )

    fun mapClassificationsToSuggestions(
        classifications: List<HuggingFaceClassification>
    ): List<GardenWorkSuggestion> {
        val suggestions = mutableMapOf<GardenWorkType, GardenWorkSuggestion>()

        for (classification in classifications) {
            val label = classification.label.lowercase()
            // Check if any keyword in our map matches the label
            val workType = labelToWorkType.entries.firstOrNull { (key, _) ->
                label.contains(key)
            }?.value ?: continue

            // Keep the highest confidence per work type
            val existing = suggestions[workType]
            if (existing == null || classification.score > existing.confidence) {
                suggestions[workType] = GardenWorkSuggestion(
                    type = workType,
                    description = "Based on detected: ${classification.label}",
                    confidence = classification.score.coerceIn(0.0, 1.0),
                    detailedGuidance = guidanceMap[workType] ?: guidanceMap[GardenWorkType.GENERAL_MAINTENANCE]!!
                )
            }
        }

        return suggestions.values.toList()
    }

    fun isGardenRelated(classifications: List<HuggingFaceClassification>): Boolean {
        return classifications.any { classification ->
            val label = classification.label.lowercase()
            labelToWorkType.keys.any { keyword -> label.contains(keyword) }
        }
    }
}
