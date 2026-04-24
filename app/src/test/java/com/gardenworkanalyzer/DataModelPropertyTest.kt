package com.gardenworkanalyzer

import com.gardenworkanalyzer.data.model.GardenWorkSuggestionDto
import com.gardenworkanalyzer.data.model.toDomain
import com.gardenworkanalyzer.domain.model.GardenWorkType
import com.gardenworkanalyzer.domain.model.SUPPORTED_MIME_TYPES
import com.gardenworkanalyzer.domain.model.isValidMimeType
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

// Feature: garden-work-analyzer, Property 5: Format validation accepts only supported MIME types
// Feature: garden-work-analyzer, Property 10: Suggestion DTO maps to valid domain model

class DataModelPropertyTest : FreeSpec({

    // -----------------------------------------------------------------------
    // Property 5: Format validation accepts only supported MIME types
    // Validates: Requirements 1.4, 1.6
    // -----------------------------------------------------------------------

    "Property 5 - supported MIME types are accepted" {
        // **Validates: Requirements 1.4, 1.6**
        val supportedMimeArb = Arb.element(SUPPORTED_MIME_TYPES.toList())

        checkAll(PropTestConfig(iterations = 100), supportedMimeArb) { mimeType ->
            isValidMimeType(mimeType) shouldBe true
        }
    }

    "Property 5 - unsupported MIME types are rejected" {
        // **Validates: Requirements 1.4, 1.6**
        val unsupportedMimeArb = Arb.string(minSize = 1, maxSize = 50)
            .filter { it !in SUPPORTED_MIME_TYPES }

        checkAll(PropTestConfig(iterations = 100), unsupportedMimeArb) { mimeType ->
            isValidMimeType(mimeType) shouldBe false
        }
    }

    // -----------------------------------------------------------------------
    // Property 10: Suggestion DTO maps to valid domain model
    // Validates: Requirements 6.2, 6.3
    // -----------------------------------------------------------------------

    "Property 10 - valid DTO with known type maps correctly" {
        // **Validates: Requirements 6.2, 6.3**
        val validTypeArb = Arb.element(GardenWorkType.entries.map { it.name })
        val validConfidenceArb = Arb.double(min = 0.0, max = 1.0)
        val descArb = Arb.string(minSize = 1, maxSize = 100)
        val guidanceArb = Arb.string(minSize = 1, maxSize = 200)

        checkAll(
            PropTestConfig(iterations = 100),
            validTypeArb,
            validConfidenceArb,
            descArb,
            guidanceArb
        ) { typeName, confidence, desc, guidance ->
            val dto = GardenWorkSuggestionDto(
                type = typeName,
                description = desc,
                confidence = confidence,
                detailedGuidance = guidance
            )
            val domain = dto.toDomain()

            domain.type shouldBe GardenWorkType.valueOf(typeName)
            domain.confidence shouldBe confidence
            domain.description shouldBe desc
            domain.detailedGuidance shouldBe guidance
        }
    }

    "Property 10 - DTO with unknown type maps to GENERAL_MAINTENANCE" {
        // **Validates: Requirements 6.2, 6.3**
        val knownTypes = GardenWorkType.entries.map { it.name }.toSet()
        val unknownTypeArb = Arb.string(minSize = 1, maxSize = 50)
            .filter { it.uppercase() !in knownTypes }
        val validConfidenceArb = Arb.double(min = 0.0, max = 1.0)

        checkAll(
            PropTestConfig(iterations = 100),
            unknownTypeArb,
            validConfidenceArb
        ) { typeName, confidence ->
            val dto = GardenWorkSuggestionDto(
                type = typeName,
                description = "desc",
                confidence = confidence,
                detailedGuidance = "guidance"
            )
            val domain = dto.toDomain()

            domain.type shouldBe GardenWorkType.GENERAL_MAINTENANCE
        }
    }

    "Property 10 - confidence values are clamped to 0.0 to 1.0" {
        // **Validates: Requirements 6.2, 6.3**
        val outOfRangeConfidenceArb = arbitrary {
            val raw = Arb.double(min = -1000.0, max = 1000.0).bind()
            raw
        }
        val validTypeArb = Arb.element(GardenWorkType.entries.map { it.name })

        checkAll(
            PropTestConfig(iterations = 100),
            validTypeArb,
            outOfRangeConfidenceArb
        ) { typeName, confidence ->
            val dto = GardenWorkSuggestionDto(
                type = typeName,
                description = "desc",
                confidence = confidence,
                detailedGuidance = "guidance"
            )
            val domain = dto.toDomain()

            domain.confidence shouldNotBe null
            domain.confidence shouldBe confidence.coerceIn(0.0, 1.0)
        }
    }
})
