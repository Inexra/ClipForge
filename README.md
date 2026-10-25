# ClipForge

ClipForge is a native Android video editor built with Kotlin, Jetpack Compose, ExoPlayer, and Media3 Transformer. It is designed for phone-first editing, with local project saving and on-device rendering.

## Editing features

- Import multiple video clips with Android's document picker.
- Preview and scrub clips; trim start and end points; split, delete, and reorder clips.
- Per-clip playback speed (0.5×–3×).
- Per-clip start/end motion keyframes for zoom and rotation, interpolated in the exported render.
- Transition presets (Cut, Fade, Flash, Zoom). Fade/flash/zoom are cut-safe edge effects; they are **not** overlapping cross-dissolves.
- Background music selection and preview; looping background track; option to mute original clip sound.
- Title overlay, brightness, saturation, and colour presets (Warm, Cinematic, Mono, Invert).
- Timestamped subtitle lines shown during the matching intervals and burned into the exported video.
- Automatic English speech captions using Vosk speech recognition on-device.
- Import and export SRT subtitle files; edit and delete individual caption lines.
- MP4 export (H.264/AAC) with Original, 9:16 Shorts, 16:9 Video, and 1:1 Square canvas presets.
- Save rendered videos to Gallery/Movies/ClipForge and share them.
- Project settings and timeline autosave locally.

## Automatic captions and privacy

The first auto-caption run needs an internet connection to download the small English Vosk speech model (roughly 40 MB). After that, recognition runs locally on the phone; the video/audio is not sent to a cloud transcription service. The current included model is **English-only**, and transcription accuracy depends on speech clarity, noise, accent, and available audio tracks. Always review captions before publishing.

## Build or install without Android Studio

1. Open the **Actions** tab in this repository.
2. Open the latest **Android APK** workflow run.
3. After it succeeds, download the **ClipForge-debug-apk** artifact.
4. Extract the ZIP and install `app-debug.apk` on your Android phone. Android may ask you to allow installs from your browser or file manager.

Every push to `main` triggers a cloud build. No API keys or paid backend are required for the listed editing features.

## Build stack

- Kotlin 2.2.0 / Jetpack Compose
- Android Gradle Plugin 8.10.1
- JDK 17, Android SDK 36, minSdk 26
- Jetpack Media3 1.11.1
- Vosk Android speech recognition model (downloaded on first use)

## Current limitations

ClipForge is a more capable editing MVP, but it is **not yet a feature-for-feature replacement for CapCut or VN**. Current limitations include no true overlapping cross-dissolve, no multiple simultaneous independent audio tracks or adjustable per-track volume automation, no multi-point Bézier keyframe curves, no background-removal/AI video generation, no automatic multilingual cloud captioning, no template marketplace, and no direct social publishing. Rendering speed and supported input formats depend on the phone's supported media codecs, thermal state, and free storage. The cloud CI build is not a substitute for real-device testing.
