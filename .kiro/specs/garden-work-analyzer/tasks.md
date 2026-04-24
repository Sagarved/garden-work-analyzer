# Implementation Plan: Garden Work Analyzer

## Overview

Incrementally build the Garden Work Analyzer Android app using Kotlin + Jetpack Compose, following clean architecture (UI → Domain → Data). Each task builds on the previous, starting with data models and domain logic, then repositories and network, then UI, and finally wiring everything together.

## Tasks

- [x] 1. Set up project structure, dependencies, and core data models
  - [x] 1.1 Configure Gradle dependencies for Jetpack Compose, Hilt, Retrofit, OkHttp, Coil, Kotest, MockK, Turbine, and Robolectric
    - Add Material 3 Compose, Hilt, Retrofit + OkHttp, Coil, and test dependencies to `build.gradle.kts`
    - Enable Compose compiler and Hilt annotation processing
    - _Requirements: All (foundational setup)_

  - [x] 1.2 Create core data models and constants
    - Implement `GardenImage`, `SequencedImage`, `GardenWorkSuggestion`, `GardenWorkType`, `GardenAnalysisResult`, `GardenAnalysisResponse`, `GardenWorkSuggestionDto`, `AnalysisUiState`, and `PermissionStatus`
    - Define constants: `SUPPORTED_MIME_TYPES`, `MAX_IMAGE_COUNT`, `MIN_IMAGE_COUNT`, `MAX_IMAGE_SIZE_BYTES`, `MAX_RETRY_ATTEMPTS`, `CONFIDENCE_THRESHOLD`
    - _Requirements: 1.4, 3.5, 5.3, 6.2, 6.3, 7.3_

  - [x] 1.3 Write property tests for data model validation
    - **Property 5: Format validation accepts only supported MIME types**
    - **Validates: Requirements 1.4, 1.6**
    - **Property 10: Suggestion DTO maps to valid domain model**
    - **Validates: Requirements 6.2, 6.3**

- [x] 2. Implement domain layer — image collection use cases
  - [x] 2.1 Implement `ManageImageCollectionUseCase`
    - Implement `addImages`, `removeImage`, `validateFormat`, `isCollectionFull`
    - Enforce max 10 images, supported MIME types only
    - _Requirements: 1.3, 1.4, 1.6, 3.3, 3.5, 3.6_

  - [x] 2.2 Write property test: adding images grows the collection
    - **Property 1: Adding images grows the collection**
    - **Validates: Requirements 1.3, 2.2**

  - [x] 2.3 Write property test: removing an image shrinks the collection
    - **Property 3: Removing an image shrinks the collection**
    - **Validates: Requirements 3.3**

  - [x] 2.4 Write property test: collection size invariant
    - **Property 4: Collection size invariant**
    - **Validates: Requirements 3.5, 3.6**

  - [x] 2.5 Implement `SequenceImagesUseCase`
    - Implement `reorder` (drag-and-drop move) and `finalizeSequence` (produce 1-based indexed list)
    - Default ordering matches insertion order
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [x] 2.6 Write property test: default ordering matches insertion order
    - **Property 6: Default ordering matches insertion order**
    - **Validates: Requirements 4.1**

  - [x] 2.7 Write property test: reorder correctly moves elements
    - **Property 7: Reorder correctly moves elements**
    - **Validates: Requirements 4.2**

  - [x] 2.8 Write property test: finalize produces sequential 1-based indices
    - **Property 8: Finalize produces sequential 1-based indices**
    - **Validates: Requirements 4.3, 4.4**

- [x] 3. Checkpoint — Verify domain logic
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement data layer — repositories and network
  - [x] 4.1 Implement `ImageRepository`
    - Implement in-memory image storage with `getImages` (Flow), `addImages`, `removeImage`, `reorder`, `getSequencedImages`, `clear`
    - _Requirements: 1.3, 1.5, 2.2, 2.6, 3.3, 4.1, 4.2_

  - [x] 4.2 Write property test: cancel preserves collection state
    - **Property 2: Cancel preserves collection state**
    - **Validates: Requirements 1.5, 2.6**

  - [x] 4.3 Implement `ImageCompressor`
    - Compress images to max 2 MB using Android Bitmap APIs
    - Handle compression failure gracefully (skip problematic images, log error)
    - _Requirements: 5.3_

  - [x] 4.4 Write property test: compression output is within size limit
    - **Property 9: Compression output is within size limit**
    - **Validates: Requirements 5.3**

  - [x] 4.5 Implement `GardenAnalyzerApiService` with Retrofit
    - Define multipart POST `/analyze` endpoint
    - Configure OkHttp client with HTTPS, timeouts, and retry interceptor (exponential backoff, max 3 retries)
    - _Requirements: 5.1, 5.4, 5.5, 5.6, 5.7_

  - [x] 4.6 Implement `GardenAnalysisRepository`
    - Orchestrate compression, upload, and response mapping
    - Map `GardenWorkSuggestionDto` to `GardenWorkSuggestion` (unknown types → `GENERAL_MAINTENANCE`, clamp confidence to [0.0, 1.0])
    - Handle network errors, unreachable endpoint, no garden content detected
    - _Requirements: 5.1, 6.1, 6.2, 6.3, 6.5_

- [x] 5. Checkpoint — Verify data layer
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement ViewModels
  - [x] 6.1 Implement `ImageCollectionViewModel`
    - Expose `imageCollection` and `imageCount` as `StateFlow`
    - Implement `addImages`, `removeImage`, `reorderImages`, `canAddImages`
    - Wire to `ManageImageCollectionUseCase` and `SequenceImagesUseCase`
    - _Requirements: 1.3, 2.2, 3.1, 3.3, 3.4, 3.5, 3.6, 4.1, 4.2_

  - [x] 6.2 Implement `AnalysisViewModel`
    - Expose `analysisState` as `StateFlow<AnalysisUiState>`
    - Implement `analyze` (sequence → compress → upload → map results) and `retry`
    - Handle loading/uploading progress, success, and error states
    - _Requirements: 4.3, 5.1, 5.2, 5.4, 5.5, 5.7, 6.1, 6.5_

  - [x] 6.3 Write unit tests for ViewModels
    - Test adding exactly 10 images succeeds, adding 11th is rejected
    - Test empty collection disables analyze
    - Test network failure produces error state with retry
    - Test 3 retries exhausted produces final error message
    - Test no garden content detected produces appropriate UI state
    - _Requirements: 3.5, 3.6, 4.5, 5.4, 5.5, 6.5_

- [x] 7. Implement UI layer — Compose screens and components
  - [x] 7.1 Implement `PermissionManager` and permission request flows
    - Check and request camera and storage permissions at point of use
    - Handle denied and permanently denied states with settings link
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [x] 7.2 Implement `GalleryPickerButton` and `CameraCaptureButton`
    - `GalleryPickerButton`: trigger `GetMultipleContents` with MIME filter for JPEG/PNG/WebP
    - `CameraCaptureButton`: trigger `TakePicture`, hide when no camera available
    - Handle cancel (no-op on collection), permission denied dialogs
    - _Requirements: 1.1, 1.2, 1.4, 1.5, 1.6, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [x] 7.3 Implement `ImageCollectionGrid`
    - Scrollable `LazyVerticalGrid` of thumbnails loaded via Coil
    - Drag-and-drop reordering, tap-to-preview, swipe/button-to-remove
    - Image count badge
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.2_

  - [x] 7.4 Implement `AnalyzeButton`
    - Enabled only when image count is in [1, 10]
    - Triggers sequencing and analysis flow
    - _Requirements: 4.3, 4.5_

  - [x] 7.5 Implement `SuggestionDisplayScreen`
    - Display suggestion cards sorted by confidence descending, filtered to ≥ 0.5
    - Cards expand on tap to show detailed guidance
    - Show analyzed images in horizontal strip
    - Handle no suggestions and no garden content states
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

  - [x] 7.6 Write property tests for suggestion display logic
    - **Property 11: Displayed suggestions contain required fields**
    - **Validates: Requirements 7.1**
    - **Property 12: Suggestions are sorted by confidence descending**
    - **Validates: Requirements 7.2**
    - **Property 13: Only high-confidence suggestions are displayed**
    - **Validates: Requirements 7.3**

- [x] 8. Wire everything together with Hilt and MainScreen
  - [x] 8.1 Set up Hilt modules and dependency injection
    - Create Hilt modules for repositories, use cases, API service, OkHttp client
    - Annotate ViewModels with `@HiltViewModel`
    - _Requirements: All (integration)_

  - [x] 8.2 Implement `MainScreen` composable
    - Compose root screen hosting `GalleryPickerButton`, `CameraCaptureButton`, `ImageCollectionGrid`, `AnalyzeButton`
    - Navigation to `SuggestionDisplayScreen` on analysis success
    - Error toasts and dialogs for all error scenarios
    - _Requirements: 1.1, 2.1, 3.1, 4.3, 5.2, 7.1_

- [x] 9. Final checkpoint — Full integration verification
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- All code uses Kotlin with Jetpack Compose, Hilt, Retrofit, and Kotest
