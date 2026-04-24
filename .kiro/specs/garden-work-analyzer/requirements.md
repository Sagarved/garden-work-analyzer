# Requirements Document

## Introduction

The Garden Work Analyzer is an Android application that enables users to capture or select garden images, sequence them together, and upload them to a machine learning model for analysis. The ML model analyzes the garden images and provides actionable garden work suggestions such as pruning, watering, weeding, planting, fertilizing, and pest control. The app aims to help gardeners identify what maintenance tasks their garden needs by leveraging image-based ML inference.

## Glossary

- **App**: The Garden Work Analyzer Android application
- **Gallery_Picker**: The component responsible for accessing the device photo gallery and allowing the user to select one or more images
- **Camera_Capture**: The component responsible for accessing the device camera and allowing the user to capture new photos
- **Image_Sequencer**: The component responsible for joining and ordering selected/captured images into a sequenced collection for analysis
- **Image_Uploader**: The component responsible for uploading the sequenced image collection to the ML inference endpoint
- **Garden_Analyzer_Model**: The machine learning model (either custom-trained or sourced from HuggingFace) that analyzes garden images and produces garden work suggestions
- **Suggestion_Display**: The component responsible for presenting garden work suggestions to the user
- **Image_Collection**: The set of images selected or captured by the user before sequencing
- **Garden_Work_Suggestion**: A recommended garden maintenance action produced by the Garden_Analyzer_Model (e.g., pruning, watering, weeding, planting, fertilizing, pest control)

## Requirements

### Requirement 1: Select Images from Device Gallery

**User Story:** As a gardener, I want to select images from my device gallery, so that I can use existing garden photos for analysis.

#### Acceptance Criteria

1. WHEN the user taps the gallery selection button, THE Gallery_Picker SHALL open the device photo gallery for image selection
2. THE Gallery_Picker SHALL allow the user to select one or more images from the device gallery
3. WHEN the user selects one or more images, THE Gallery_Picker SHALL add the selected images to the Image_Collection
4. THE Gallery_Picker SHALL only accept images in JPEG, PNG, or WebP format
5. IF the user cancels the gallery selection, THEN THE Gallery_Picker SHALL return to the main screen without modifying the Image_Collection
6. IF the user selects an image in an unsupported format, THEN THE Gallery_Picker SHALL display an error message indicating the supported formats

### Requirement 2: Capture Images Using Device Camera

**User Story:** As a gardener, I want to capture new photos of my garden using the device camera, so that I can analyze the current state of my garden.

#### Acceptance Criteria

1. WHEN the user taps the camera capture button, THE Camera_Capture SHALL open the device camera for photo capture
2. WHEN the user captures a photo, THE Camera_Capture SHALL add the captured image to the Image_Collection
3. THE Camera_Capture SHALL allow the user to capture multiple photos in succession
4. IF the device does not have a camera, THEN THE App SHALL hide the camera capture button and display only the gallery selection option
5. IF the user denies camera permission, THEN THE Camera_Capture SHALL display a message explaining that camera permission is required and provide a link to the app settings
6. IF the user cancels the camera capture, THEN THE Camera_Capture SHALL return to the main screen without modifying the Image_Collection

### Requirement 3: Manage Image Collection

**User Story:** As a gardener, I want to review and manage my selected images before analysis, so that I can ensure the right images are included.

#### Acceptance Criteria

1. THE App SHALL display all images in the Image_Collection as a scrollable thumbnail grid
2. WHEN the user taps a thumbnail, THE App SHALL display the full-size image in a preview view
3. WHEN the user removes an image from the Image_Collection, THE App SHALL update the thumbnail grid to reflect the removal
4. THE App SHALL display the total count of images in the Image_Collection
5. THE App SHALL support a minimum of 1 and a maximum of 10 images in the Image_Collection
6. IF the user attempts to add more than 10 images, THEN THE App SHALL display a message indicating the maximum image limit has been reached

### Requirement 4: Sequence Images

**User Story:** As a gardener, I want my selected images to be sequenced together, so that the ML model can analyze them as a coherent set.

#### Acceptance Criteria

1. THE Image_Sequencer SHALL arrange images in the Image_Collection in the order they were added
2. WHEN the user reorders images via drag-and-drop, THE Image_Sequencer SHALL update the sequence to reflect the new order
3. WHEN the user taps the analyze button, THE Image_Sequencer SHALL produce a finalized ordered sequence of all images in the Image_Collection
4. THE Image_Sequencer SHALL assign a sequential index (starting from 1) to each image in the sequence
5. IF the Image_Collection contains fewer than 1 image, THEN THE Image_Sequencer SHALL disable the analyze button and display a message prompting the user to add images

### Requirement 5: Upload Images to ML Model

**User Story:** As a gardener, I want my sequenced images to be uploaded to the ML model, so that the model can analyze my garden.

#### Acceptance Criteria

1. WHEN the user taps the analyze button and the Image_Sequencer has produced a finalized sequence, THE Image_Uploader SHALL upload the sequenced images to the Garden_Analyzer_Model endpoint
2. WHILE the Image_Uploader is uploading images, THE App SHALL display a progress indicator showing the upload status
3. THE Image_Uploader SHALL compress images to a maximum of 2 MB per image before uploading
4. IF the upload fails due to a network error, THEN THE Image_Uploader SHALL display an error message and offer a retry option
5. IF the upload fails after 3 retry attempts, THEN THE Image_Uploader SHALL display a message suggesting the user check the network connection
6. THE Image_Uploader SHALL transmit images over HTTPS to the Garden_Analyzer_Model endpoint
7. IF the Garden_Analyzer_Model endpoint is unreachable, THEN THE Image_Uploader SHALL display a message indicating the service is temporarily unavailable

### Requirement 6: ML Model Garden Analysis

**User Story:** As a gardener, I want the ML model to analyze my garden images, so that I can receive actionable garden work suggestions.

#### Acceptance Criteria

1. WHEN the Garden_Analyzer_Model receives a sequenced set of images, THE Garden_Analyzer_Model SHALL analyze the images and produce one or more Garden_Work_Suggestions
2. THE Garden_Analyzer_Model SHALL categorize each Garden_Work_Suggestion into one of the following types: pruning, watering, weeding, planting, fertilizing, pest control, mulching, or general maintenance
3. THE Garden_Analyzer_Model SHALL assign a confidence score between 0.0 and 1.0 to each Garden_Work_Suggestion
4. THE Garden_Analyzer_Model SHALL return results within 30 seconds of receiving the image set
5. IF the Garden_Analyzer_Model cannot identify any garden content in the images, THEN THE Garden_Analyzer_Model SHALL return a response indicating no garden content was detected
6. IF a suitable pre-trained model is available on HuggingFace for garden work suggestion, THE App SHALL integrate the HuggingFace model as the Garden_Analyzer_Model
7. WHERE no suitable HuggingFace model is available, THE App SHALL support integration with a custom-trained Garden_Analyzer_Model

### Requirement 7: Display Garden Work Suggestions

**User Story:** As a gardener, I want to see the analysis results clearly, so that I can understand what garden work is recommended.

#### Acceptance Criteria

1. WHEN the Garden_Analyzer_Model returns results, THE Suggestion_Display SHALL present each Garden_Work_Suggestion as a card with the suggestion type, description, and confidence score
2. THE Suggestion_Display SHALL sort Garden_Work_Suggestions by confidence score in descending order
3. THE Suggestion_Display SHALL display only Garden_Work_Suggestions with a confidence score of 0.5 or higher
4. WHEN the user taps a suggestion card, THE Suggestion_Display SHALL expand the card to show detailed guidance for the suggested garden work
5. IF the Garden_Analyzer_Model returns no suggestions, THEN THE Suggestion_Display SHALL display a message indicating no garden work suggestions were identified
6. THE Suggestion_Display SHALL display the analyzed images alongside the suggestions for reference

### Requirement 8: Permission Handling

**User Story:** As a gardener, I want the app to handle device permissions properly, so that I can grant access to the camera and gallery securely.

#### Acceptance Criteria

1. WHEN the App is launched for the first time, THE App SHALL request storage read permission before accessing the device gallery
2. WHEN the user taps the camera capture button for the first time, THE App SHALL request camera permission
3. IF the user denies storage read permission, THEN THE App SHALL display a message explaining that storage permission is required to select images and provide a link to app settings
4. THE App SHALL follow Android runtime permission guidelines and request permissions at the point of use
5. IF the user revokes a previously granted permission, THEN THE App SHALL re-request the permission when the corresponding feature is next accessed
