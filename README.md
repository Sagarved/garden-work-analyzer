# Garden Work Analyzer

An Android app that analyzes garden images using ML and provides actionable garden work suggestions like pruning, watering, weeding, planting, fertilizing, and pest control.

Built with Kotlin, Jetpack Compose, Hilt, Retrofit, and Coil.

## Setup

### Prerequisites
- Android Studio (Hedgehog or later)
- JDK 17
- Android SDK (API 35)

### Configuration

1. Clone the repo:
   ```bash
   git clone git@github.com:Sagarved/garden-work-analyzer.git
   cd garden-work-analyzer
   ```

2. Create `gradle.properties` from the template:
   ```bash
   cp gradle.properties.example gradle.properties
   ```

3. Add your HuggingFace API token to `gradle.properties`:
   ```properties
   HF_API_TOKEN=hf_your_token_here
   ```
   Get a token at https://huggingface.co/settings/tokens

### Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk

# Run tests
./gradlew test
```

## Architecture

MVVM with clean architecture (UI → Domain → Data):

- **UI**: Jetpack Compose + Material 3
- **Domain**: Use cases for image collection management and sequencing
- **Data**: Repositories, HuggingFace Inference API, image compression

## Features

- Select images from gallery or capture with camera
- Manage image collection (add, remove, reorder)
- Image compression before upload
- ML-powered garden analysis via HuggingFace
- Suggestion cards sorted by confidence with detailed guidance
- Runtime permission handling

## Testing

79 tests covering all layers:
- 13 property-based tests (Kotest) validating correctness properties
- Unit tests for use cases, repositories, and ViewModels
- MockK for mocking, Turbine for Flow testing

```bash
./gradlew test
```

## Tech Stack

| Component | Library |
|-----------|---------|
| UI | Jetpack Compose, Material 3 |
| DI | Hilt |
| Network | Retrofit, OkHttp |
| Images | Coil |
| Testing | Kotest, MockK, Turbine, Robolectric |
| ML | HuggingFace Inference API |
