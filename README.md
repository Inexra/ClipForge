# ClipForge

ClipForge is a native Android video-editing app built with Kotlin, Jetpack Compose, ExoPlayer, and Media3 Transformer. Editing and rendering run on your phone; no account or paid backend is required for the current editor.

## Features implemented

- Import multiple videos with the Android document picker.
- Preview and scrub through individual clips.
- Trim clip start/end points.
- Split a clip at the playhead.
- Rearrange clips and delete clips from the timeline.
- Choose and preview background audio.
- Loop background music across the video and optionally mute original clip audio.
- Burn in a static title/caption overlay.
- Adjust brightness.
- Export MP4 (H.264/AAC) with Original, 9:16 Shorts, 16:9 Video, or 1:1 Square aspect presets.
- Save rendered videos to Movies/ClipForge in Gallery and share them.
- Autosave project clips, media references, and settings locally.

## Get the APK without Android Studio

1. Open the **Actions** tab.
2. Open the latest **Android APK** workflow run.
3. After it succeeds, download the **ClipForge-debug-apk** artifact.
4. Extract the ZIP and install `app-debug.apk` on your Android phone. You may need to allow installs from your browser or file manager.

Pushes to `main` trigger cloud APK builds. No API keys are needed for the editing features.

## Build stack

- Kotlin 2.2.0 / Compose compiler plugin
- Android Gradle Plugin 8.10.1
- JDK 17, Android SDK 36, minSdk 26
- Jetpack Media3 1.11.1

## Not yet equivalent to CapCut/VN

This is a functional editing MVP, not a feature-for-feature clone. It does not yet have animated transitions, multi-layer independently timed captions, keyframes, advanced color grading, templates, AI auto-captions, cloud sync, or direct social publishing. Media support and export speed depend on the device's codec support and available storage.
