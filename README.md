# ClipForge

ClipForge is a native Android video-editor starter built with Kotlin and Jetpack Compose. The goal is a mobile-first editor inspired by modern short-form editing apps, with offline editing as the foundation and optional online AI tools later.

## Current status

This first commit is a **buildable UI starter**, not a full video editor yet. It includes a dark editor dashboard, project preview placeholder, editing-tool shortcuts, and bottom navigation. It does not yet import media, edit a real timeline, render videos, or call AI services.

## Build an APK without Android Studio

1. Open the **Actions** tab in this repository.
2. Select **Android APK**.
3. Choose **Run workflow** if no run has started automatically, then run it on the `main` branch.
4. Open the completed workflow run and download the `ClipForge-debug-apk` artifact.
5. Extract the ZIP and install `app-debug.apk` on your Android phone. You may need to allow installs from your browser or file manager.

Every push to `main` also triggers a build. No API secrets are required for this starter.

## Technical setup

- Kotlin + Jetpack Compose
- Android Gradle Plugin 8.9.2
- Kotlin 2.0.21
- Java 17
- compileSdk / targetSdk 35; minSdk 26
- GitHub Actions builds the debug APK in the cloud

## Roadmap

1. Media picker and project creation
2. Real timeline with clip trimming, splitting, and audio tracks
3. Preview playback and export/render pipeline
4. Text, captions, transitions, filters, and speed controls
5. Optional AI-assisted captions and editing
6. Export presets and device-performance tuning

AI/provider secrets must stay on a trusted backend, never in the Android app source. Keep editing and project data usable offline wherever practical.
