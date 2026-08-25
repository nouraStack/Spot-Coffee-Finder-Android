# Spot — Coffee Finder Android App

**Spot** is an Android coffee-discovery application that helps users find cafés, explore locations on an interactive map, filter by preferences, save favorites, and manage reservations.

This repository is a **public portfolio version** of the project. Private credentials and environment-specific files have intentionally been removed.

## Highlights

- Interactive **Google Maps** café discovery
- Current-location support using Google Play Services Location
- Search and preference-based café filtering
- Recommendation logic for ranking cafés
- Café detail views and favorites
- Reservation and table-booking flows
- Customer and café-provider experiences
- Firebase Authentication and Realtime Database integration
- Responsive Android UI using Material components and View Binding

## Tech Stack

- Java
- Android SDK
- Google Maps SDK for Android
- Google Places API
- Firebase Authentication
- Firebase Realtime Database
- Android Navigation Component
- Material Design Components
- RecyclerView / CardView
- Glide
- Gradle Kotlin DSL

## Project Structure

```text
app/src/main/java/com/example/spot/
├── adapters/      # RecyclerView adapters
├── fragments/     # User-facing application screens
├── models/        # Application data models
├── provider/      # Café provider functionality
├── utils/         # Firebase helpers, seeders and recommendation logic
├── LoginActivity.java
├── RegisterActivity.java
├── MainActivity.java
└── ProviderActivity.java
```

## Local Setup

This public repository does **not** include API credentials or a Firebase configuration file.

1. Clone the repository and open it in Android Studio.
2. Create or use a Firebase project and add an Android application with package name `com.example.spot`.
3. Download your Firebase `google-services.json` and place it in:

   ```text
   app/google-services.json
   ```

4. Enable the Firebase services required by the app, including Authentication and Realtime Database.
5. Create a Google Maps API key and enable the Maps SDK for Android (and Places API if required).
6. Replace the placeholder value in:

   ```text
   app/src/main/res/values/strings.xml
   ```

   ```xml
   <string name="google_maps_key">YOUR_GOOGLE_MAPS_API_KEY</string>
   ```

7. Sync Gradle and run the application on an Android device or emulator.

## Security Notes

The following files and credentials are deliberately excluded from version control:

- `google-services.json`
- `local.properties`
- Google Maps API keys
- Android signing keys / keystores
- APK and AAB build artifacts

Never commit production API keys, Firebase private configuration, or signing credentials to a public repository.

## Portfolio Note

This repository is intended to demonstrate Android development, maps integration, Firebase-backed application flows, UI implementation, and application architecture. Environment-specific credentials must be supplied by anyone running their own local copy.
