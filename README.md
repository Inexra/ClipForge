# ClipForge

ClipForge is a native Android video editor built with Kotlin, Jetpack Compose, ExoPlayer, and Media3 Transformer. Editing runs on the phone; the project workspace autosaves locally, and APKs are built by GitHub Actions without Android Studio.

## Implemented editor workflows

- Import multiple video clips; preview and scrub; trim, split, reorder, and delete clips.
- Undo/redo history for common timeline edits.
- Per-clip speed presets and speed curves (constant, ramp up/down, ease-in/out, pulse).
- Start/end zoom and rotation keyframes, crop/reframe zoom, and horizontal/vertical crop positioning.
- Cut, Fade, Flash, and Zoom transition presets. Fade/flash/zoom are cut-safe edge effects, **not overlapping cross-dissolves**.
- Image/logo overlay layers with independent in/out timing, position, size, opacity, and rotation.
- Freeze-frame capture as a timed full-frame layer.
- Background music, recorded voice-over, independently adjustable source/music/voice-over levels, and original-audio mute.
- Manual beat markers and automatic audio-energy peak detection.
- Static title overlay; brightness, saturation, and Warm/Cinematic/Mono/Invert colour presets.
- Timestamped, editable subtitles burned into the output.
- Automatic on-device English and Hindi speech captions using the corresponding Vosk model.
- Text-to-speech narration generated from caption lines using the phone's installed Android TTS voice data.
- SRT subtitle import/export.
- Portable project bundle export/import: ZIP containing project settings and copied source media.
- MP4 H.264/AAC export with Original, 9:16 Shorts, 9:16 Full HD, 16:9 Video, 16:9 Full HD, and 1:1 Square presets.
- Save renders into Gallery/Movies/ClipForge and share them.

## Captions, speech and privacy

The first automatic-caption run for each language needs an internet connection to download its small speech model (roughly 40–45 MB). Speech recognition then runs locally, and the source audio/video is not uploaded to a caption service. The included models are English and Hindi only. Speech-to-text quality varies with accent, noise, source codecs, and audio quality; review all generated subtitles.

Text-to-speech uses the Android speech engine installed on the phone, not a guaranteed cloud voice. If the selected language's voice data isn't available, install it in the phone's Text-to-Speech settings.

## Get an APK without Android Studio

1. Open the **Actions** tab in this repository.
2. Open the latest **Android APK** workflow run.
3. Download the **ClipForge-debug-apk** artifact after the run succeeds.
4. Extract the ZIP and install `app-debug.apk` on your Android phone. You may need to allow installations from your browser or file manager.

Every push to `main` triggers a cloud build. No paid backend or provider API key is needed for the current feature set.

## Stack

- Kotlin 2.2.0, Jetpack Compose, Android Gradle Plugin 8.10.1
- JDK 17, Android SDK 36, minimum SDK 26
- Jetpack Media3 1.11.1 for playback and video composition
- Vosk Android for offline speech recognition after the initial model download

## Important limitations

ClipForge is a substantially expanded editing MVP, **not yet a complete feature-for-feature CapCut/VN replacement**. It still lacks true overlapping cross-dissolves, multi-point Bézier keyframe graphs for every property, optical-flow/frame-interpolated slow motion, reverse playback export, chroma key and AI subject/background removal, animated text/sticker libraries, full multi-track video compositing with independently edited video-overlay timelines, imported LUT pipeline, silence/filler-word removal, auto-template/AutoCut library, AI image/video generation, cloud collaboration/sync, and direct social-platform account publishing. Auto beat detection is a signal-energy heuristic that should be reviewed, not a guaranteed musical beat grid. Supported codecs and render speed depend on the phone's hardware decoder/encoder, temperature, and free storage. Passing cloud CI does not replace testing actual media on the target phone.
