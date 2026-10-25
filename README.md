# ClipForge

ClipForge is a native Android video editor built with Kotlin, Jetpack Compose, ExoPlayer, and Media3 Transformer. Editing runs on the phone; the project workspace autosaves locally, and APKs are built by GitHub Actions without Android Studio.

## Editor capabilities

### Timeline and project workflow
- Import multiple video clips; preview and scrub; trim, split, reorder, and delete clips.
- Undo/redo history for common edits.
- Portable project bundle export/import ZIPs with source media and edit settings.
- Image/logo/sticker overlay layers with independent in/out timing, position, size, opacity, and rotation.
- On-device AI person cutout for still-image layers. The bundled segmentation model processes selected images locally; it does not remove backgrounds from moving video.
- Freeze-frame captures added as timed full-frame layers.

### Motion and visual effects
- Per-clip speed presets (0.25×–3×) and speed curves (constant, ramp up/down, ease-in/out, pulse). Slow motion is conventional frame-rate retiming, not optical-flow frame interpolation.
- Three-point start/middle/end zoom and rotation keyframes with selectable interpolation curves.
- Crop/reframe zoom with horizontal and vertical positioning.
- Cut, true overlapping Crossfade (up to 450 ms during export), Fade, Flash, and Zoom transition presets. Crossfades use separate video tracks during export; the timeline preview still previews the selected clip rather than a live composite.
- Brightness, saturation, and colour presets (Warm, Cinematic, Mono, Invert).
- Title overlay and timed subtitle captions with selectable visual styles (Classic, Yellow Pop, Neon, Minimal), placement, and size.

### Audio and captions
- Background music, recorded voice-over, independently adjustable source/music/voice-over levels, and original-audio mute.
- Manual beat markers and automatic audio-energy peak detection.
- Automatic on-device speech captions in English and Hindi, editable timestamped lines, plus SRT import/export.
- Text-to-speech narration generated from caption lines using the Android speech engine installed on the phone.

### Export
- MP4 H.264/AAC export.
- Canvas presets: Original, vertical 720p, vertical Full HD, vertical 4K, landscape 720p, landscape Full HD, landscape 4K, and square.
- Export quality profiles (Saver, Standard, High, Ultra) with requested video/audio encoder bitrates.
- Output frame-rate cap: 24, 30, or 60 FPS.
- Save renders into Gallery/Movies/ClipForge and share them.

## Captions and privacy

The first automatic-caption run for each language needs an internet connection to download its small speech model (roughly 40–45 MB). Speech recognition then runs locally, and source audio/video is not uploaded to a caption service. The person-cutout model is bundled in the APK and can run offline after installation. The included models are English and Hindi only. Speech-to-text quality varies with accent, noise, source codecs, and audio quality; review all generated subtitles.

Text-to-speech uses the Android speech engine installed on the phone, not a guaranteed cloud voice. Install the appropriate language voice data in Android Text-to-Speech settings when needed.

## Build and install without Android Studio

1. Open the **Actions** tab in this repository.
2. Open the latest **Android APK** workflow run.
3. Download the **ClipForge-debug-apk** artifact after the run succeeds.
4. Extract the ZIP and install `app-debug.apk` on your Android phone. You may need to allow installations from your browser or file manager.

Every push to `main` triggers a cloud build. No paid backend or provider API key is needed for the listed editing features.

## Stack

- Kotlin 2.2.0, Jetpack Compose, Android Gradle Plugin 8.10.1
- JDK 17, Android SDK 36, minimum SDK 26
- Jetpack Media3 1.11.1 for playback and video composition
- Vosk Android for offline speech recognition after initial model downloads

## Limitations versus CapCut/VN

ClipForge is a substantially expanded editing MVP, **not yet a complete feature-for-feature CapCut/VN replacement**. The current engine does not yet have full arbitrary video-on-video timeline compositing; multi-point Bézier keyframe graphs for every property; optical-flow/frame-interpolated slow motion; reverse-playback export; chroma key or AI subject/background removal for moving video (still-image person cutout is supported); a large animated text/sticker/effect/template library; silence/filler-word removal; AI image/video generation; cloud collaboration/sync; or direct social-platform publishing. Auto beat detection is a signal-energy heuristic, not a guaranteed musical beat grid. Exported 4K output may be downscaled if the phone's hardware encoder doesn't support the requested resolution; 60 FPS is a cap and does not manufacture interpolated frames. Rendering speed and supported formats depend on hardware codecs, temperature, and free storage. A successful cloud build does not replace testing on a physical phone.
