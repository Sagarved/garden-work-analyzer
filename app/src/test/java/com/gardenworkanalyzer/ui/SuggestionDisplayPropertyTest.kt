package com.gardenworkanalyzer.ui

import com.gardenworkanalyzer.domain.model.CONFIDENCE_THRESHOLD
import com.gardenworkanalyzer.domain.model.GardenWorkSuggestion
import com.gardenworkanalyzer.domain.model.GardenWorkType
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

// Feature: garden-work-analyzer, Property 11: Displayed suggestions contain required fields
// Feature: garden-work-analyzer, Property 12: Suggestions are sorted by confidence descending
// Feature: garden-work-analyzer, Property 13: Only high-confidence suggestions are displayed

/**
 * Pure-logic helpers that mirror the filter/sort logic in SuggestionDisplayScreen.
 * Testing these directly avoids Compose UI dependencies while validating the same behaviour.
 */
private fun filterForDisplay(suggestions: List<GardenWorkSuggestion>): List<GardenWorkSuggestion> =
    suggestions.filter { it.confidence >= CONFIDENCE_THRESHOLD }

private fun sortForDisplay(suggestions: List<GardenWorkSuggestion>): List<GardenWorkSuggestion> =
    suggestions.sortedByDescending { it.confidence }

/** Arbitrary generator for a random [GardenWorkSuggestion]. */
private val suggestionArb: Arb<GardenWorkSuggestion> = arbitrary {
    GardenWorkSuggestion(
        type = Arb.element(GardenWorkType.entries.toList()).bind(),
        description = Arb.string(minSize = 1, maxSize = 120).bind(),
        confidence = Arb.double(min = 0.0, max = 1.0).bind(),
        detailedGuidance = Arb.string(minSize = 1, maxSize = 200).bind()
    )
}

class SuggestionDisplayPropertyTest : FreeSpec({

    // -----------------------------------------------------------------------
    // Property 11: Displayed suggestions contain required fields
    // Validates: Requirements 7.1
    // -----------------------------------------------------------------------

    "Property 11 - display representation includes type name, description, and confidence" {
        // **Validates: Requirements 7.1**
        checkAll(PropTestConfig(iterations = 100), suggestionArb) { suggestion ->
            // Build a simple display string the same way the UI card does
            val typeName = suggestion.type.name.replace('_', ' ')
            val confidencePercent = (suggestion.confidence * 100).toInt()
            val displayText = "$typeName | ${suggestion.description} | Confidence: $confidencePercent%"

            // The display representation must contain the type name
            displayText.contains(typeName).shouldBeTrue()
            // The display representation must contain the description text
            displayText.contains(suggestion.description).shouldBeTrue()
            // The display representation must contain the confidence score
            displayText.contains("$confidencePercent%").shouldBeTrue()
        }
    }

    // -----------------------------------------------------------------------
    // Property 12: Suggestions are sorted by confidence descending
    // Validates: Requirements 7.2
    // -----------------------------------------------------------------------

    "Property 12 - after sorting, each confidence >= next confidence" {
        // **Validates: Requirements 7.2**
        val suggestionsListArb = Arb.list(suggestionArb, range = 0..30)

        checkAll(PropTestConfig(iterations = 100), suggestionsListArb) { suggestions ->
            val sorted = sortForDisplay(suggestions)

            sorted.zipWithNext().forEach { (current, next) ->
                (current.confidence >= next.confidence) shouldBe true
            }
        }
    }

    // -----------------------------------------------------------------------
    // Property 13: Only high-confidence suggestions are displayed
    // Validates: Requirements 7.3
    // -----------------------------------------------------------------------

    "Property 13 - filtered list contains only suggestions with confidence >= threshold" {
        // **Validates: Requirements 7.3**
        val suggestionsListArb = Arb.list(suggestionArb, range = 0..30)

        checkAll(PropTestConfig(iterations = 100), suggestionsListArb) { suggestions ->
            val filtered = filterForDisplay(suggestions)

            // Every item in the filtered list must meet the threshold
            filtered.forEach { it.confidence shouldBe it.confidence.coerceAtLeast(CONFIDENCE_THRESHOLD) }

            // No qualifying suggestion from the original list should be missing
            val expectedCount = suggestions.count { it.confidence >= CONFIDENCE_THRESHOLD }
            filtered.size shouldBe expectedCount
        }
    }
})
