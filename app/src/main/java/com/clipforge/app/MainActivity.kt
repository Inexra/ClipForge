@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.clipforge.app

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import androidx.media3.common.util.Size
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Color as AndroidColor
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.OverlaySettings
import androidx.media3.common.VideoCompositorSettings
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.audio.DefaultGainProvider
import androidx.media3.common.audio.GainProcessor
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Brightness
import androidx.media3.effect.Crop
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.VideoEncoderSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.ProgressHolder
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

private val Bg = Color(0xFF0B0D0F)
private val Panel = Color(0xFF15181B)
private val PanelAlt = Color(0xFF22262A)
private val Lime = Color(0xFFB7E19B)
private val Muted = Color(0xFF9BA2B1)
private const val PREFS = "clipforge_project_v2"

private data class Clip(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val sourceMs: Long,
    val startMs: Long = 0L,
    val endMs: Long = sourceMs,
    val speed: Float = 1f,
    val zoomStart: Float = 1f,
    val zoomMid: Float = 1f,
    val zoomEnd: Float = 1f,
    val rotationStart: Float = 0f,
    val rotationMid: Float = 0f,
    val rotationEnd: Float = 0f,
    val motionCurve: String = "Linear",
    val transition: String = "Cut",
    val speedCurve: String = "Constant",
    val cropZoom: Float = 1f,
    val cropX: Float = 0f,
    val cropY: Float = 0f,
    val audioVolumeStart: Float = 1f,
    val audioVolumeMid: Float = 1f,
    val audioVolumeEnd: Float = 1f,
    val audioFadeInMs: Long = 0L,
    val audioFadeOutMs: Long = 0L
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

private data class CaptionToken(val text: String, val startMs: Long, val endMs: Long)

private data class CaptionLine(
    val id: String = UUID.randomUUID().toString(),
    val clipId: String,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val tokens: List<CaptionToken> = emptyList()
)

private data class PhotoLayer(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val startMs: Long = 0L,
    val endMs: Long = 5_000L,
    val position: String = "bottom-right",
    val scale: Float = 0.30f,
    val opacity: Float = 1f,
    val rotation: Float = 0f,
    val fullFrame: Boolean = false,
    val textContent: String? = null,
    val textStyle: String = "Classic",
    val animateOpacity: Boolean = false,
    val opacityStart: Float = 1f,
    val opacityMid: Float = 1f,
    val opacityEnd: Float = 1f
)

private data class Project(
    val clips: List<Clip>,
    val music: Uri?,
    val titleText: String,
    val brightness: Float,
    val muteVideoAudio: Boolean,
    val format: String,
    val captions: List<CaptionLine>,
    val saturation: Float,
    val filterPreset: String,
    val photoLayers: List<PhotoLayer> = emptyList(),
    val voiceover: Uri? = null,
    val beatMarkers: List<Long> = emptyList(),
    val captionLanguageId: String = "english",
    val sourceVolume: Float = 1f,
    val musicVolume: Float = 1f,
    val voiceoverVolume: Float = 1f,
    val exportQuality: String = "High",
    val exportFrameRate: Int = 30,
    val captionStyle: String = "Classic",
    val captionPosition: String = "Bottom",
    val captionScale: Float = 1f,
    val musicFadeInMs: Long = 0L,
    val musicFadeOutMs: Long = 0L,
    val duckMusicUnderVoiceover: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Lime,
                onPrimary = Bg,
                background = Bg,
                surface = Panel,
                onSurface = Color.White
            )) {
                Surface(Modifier.fillMaxSize(), color = Bg) { ClipForgeApp() }
            }
        }
    }
}

@Composable
private fun ClipForgeApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var clips by remember { mutableStateOf<List<Clip>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var music by remember { mutableStateOf<Uri?>(null) }
    var musicName by remember { mutableStateOf<String?>(null) }
    var sourceVolume by remember { mutableStateOf(1f) }
    var musicVolume by remember { mutableStateOf(1f) }
    var voiceoverVolume by remember { mutableStateOf(1f) }
    var exportQuality by remember { mutableStateOf("High") }
    var exportFrameRate by remember { mutableStateOf(30) }
    var captionStyle by remember { mutableStateOf("Classic") }
    var captionPosition by remember { mutableStateOf("Bottom") }
    var captionScale by remember { mutableStateOf(1f) }
    var titleText by remember { mutableStateOf("") }
    var brightness by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(0f) }
    var filterPreset by remember { mutableStateOf("None") }
    var captions by remember { mutableStateOf<List<CaptionLine>>(emptyList()) }
    var captioning by remember { mutableStateOf(false) }
    var ttsGenerating by remember { mutableStateOf(false) }
    var captionLanguageId by remember { mutableStateOf("english") }
    var photoLayers by remember { mutableStateOf<List<PhotoLayer>>(emptyList()) }
    var voiceover by remember { mutableStateOf<Uri?>(null) }
    var voiceoverName by remember { mutableStateOf<String?>(null) }
    var voiceoverRecording by remember { mutableStateOf(false) }
    var voiceRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var voiceoverFile by remember { mutableStateOf<File?>(null) }
    var beatMarkers by remember { mutableStateOf<List<Long>>(emptyList()) }
    val undoStack = remember { mutableStateListOf<Project>() }
    val redoStack = remember { mutableStateListOf<Project>() }
    var muteVideoAudio by remember { mutableStateOf(false) }
    var format by remember { mutableStateOf("Original") }
    var tab by remember { mutableStateOf(0) }
    var loaded by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }
    var transformer by remember { mutableStateOf<Transformer?>(null) }
    var position by remember { mutableStateOf(0L) }
    var trimStart by remember { mutableStateOf(0L) }
    var trimEnd by remember { mutableStateOf(0L) }
    var musicPlaying by remember { mutableStateOf(false) }
    var beatDetecting by remember { mutableStateOf(false) }
    var autoClipDurationMs by remember { mutableStateOf(30_000L) }
    var autoClipSuggestions by remember { mutableStateOf<List<AutoClipSuggestion>>(emptyList()) }
    var autoClipSource by remember { mutableStateOf<Clip?>(null) }
    var autoClipBusy by remember { mutableStateOf(false) }
    var timelineZoom by remember { mutableStateOf(1f) }
    var snapToBeats by remember { mutableStateOf(true) }
    var musicFadeInMs by remember { mutableStateOf(0L) }
    var musicFadeOutMs by remember { mutableStateOf(0L) }
    var duckMusicUnderVoiceover by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0) }

    val player = remember(context) { ExoPlayer.Builder(context).build() }
    val audioPlayer = remember(context) { ExoPlayer.Builder(context).build() }
    DisposableEffect(player, audioPlayer) {
        onDispose {
            runCatching { voiceRecorder?.stop() }
            runCatching { voiceRecorder?.release() }
            voiceRecorder = null
            player.release()
            audioPlayer.release()
        }
    }

    val selectedIndex = clips.indexOfFirst { it.id == selectedId }
    val selected = clips.getOrNull(selectedIndex)
    val clipStartOffsetsMs = clipTimelineStartOffsetsMs(clips)
    val timelineDurationMs = clipTimelineDurationMs(clips).coerceAtLeast(1_000L)
    val timelinePositionMs = (clipStartOffsetsMs.getOrNull(selectedIndex) ?: 0L) +
        if (selected != null) (position / selected.speed).toLong() else 0L

    fun currentProject() = Project(
        clips, music, titleText, brightness, muteVideoAudio, format,
        captions, saturation, filterPreset, photoLayers, voiceover, beatMarkers, captionLanguageId,
        sourceVolume, musicVolume, voiceoverVolume, exportQuality, exportFrameRate,
        captionStyle, captionPosition, captionScale, musicFadeInMs, musicFadeOutMs, duckMusicUnderVoiceover
    )
    fun recordUndo() {
        val snapshot = currentProject()
        if (undoStack.lastOrNull() != snapshot) undoStack.add(snapshot)
        while (undoStack.size > 40) undoStack.removeAt(0)
        redoStack.clear()
    }
    fun restoreProject(project: Project) {
        clips = project.clips
        selectedId = project.clips.firstOrNull()?.id
        music = project.music
        musicName = project.music?.let { nameForUri(context, it) }
        sourceVolume = project.sourceVolume
        musicVolume = project.musicVolume
        voiceoverVolume = project.voiceoverVolume
        titleText = project.titleText
        brightness = project.brightness
        saturation = project.saturation
        filterPreset = project.filterPreset
        captions = project.captions
        photoLayers = project.photoLayers
        voiceover = project.voiceover
        voiceoverName = project.voiceover?.let { nameForUri(context, it) }
        beatMarkers = project.beatMarkers
        captionLanguageId = project.captionLanguageId
        muteVideoAudio = project.muteVideoAudio
        format = project.format
        exportQuality = project.exportQuality
        exportFrameRate = project.exportFrameRate
        captionStyle = project.captionStyle
        captionPosition = project.captionPosition
        captionScale = project.captionScale
        musicFadeInMs = project.musicFadeInMs
        musicFadeOutMs = project.musicFadeOutMs
        duckMusicUnderVoiceover = project.duckMusicUnderVoiceover
    }
    fun undoProject() {
        if (undoStack.isEmpty()) return
        redoStack.add(currentProject())
        restoreProject(undoStack.removeAt(undoStack.lastIndex))
        message = "Undo complete."
    }
    fun redoProject() {
        if (redoStack.isEmpty()) return
        undoStack.add(currentProject())
        restoreProject(redoStack.removeAt(redoStack.lastIndex))
        message = "Redo complete."
    }

    LaunchedEffect(Unit) {
        val saved = withContext(Dispatchers.IO) { loadProject(context) }
        clips = saved.clips
        selectedId = saved.clips.firstOrNull()?.id
        music = saved.music
        musicName = saved.music?.let { withContext(Dispatchers.IO) { nameForUri(context, it) } }
        sourceVolume = saved.sourceVolume
        musicVolume = saved.musicVolume
        voiceoverVolume = saved.voiceoverVolume
        musicFadeInMs = saved.musicFadeInMs
        musicFadeOutMs = saved.musicFadeOutMs
        duckMusicUnderVoiceover = saved.duckMusicUnderVoiceover
        exportQuality = saved.exportQuality
        exportFrameRate = saved.exportFrameRate
        captionStyle = saved.captionStyle
        captionPosition = saved.captionPosition
        captionScale = saved.captionScale
        titleText = saved.titleText
        brightness = saved.brightness
        saturation = saved.saturation
        filterPreset = saved.filterPreset
        captions = saved.captions
        photoLayers = saved.photoLayers
        voiceover = saved.voiceover
        voiceoverName = saved.voiceover?.let { nameForUri(context, it) }
        beatMarkers = saved.beatMarkers
        captionLanguageId = saved.captionLanguageId
        muteVideoAudio = saved.muteVideoAudio
        format = saved.format
        loaded = true
    }

    LaunchedEffect(loaded, clips, music, titleText, brightness, saturation, filterPreset, captions, photoLayers, voiceover, beatMarkers, captionLanguageId, sourceVolume, musicVolume, voiceoverVolume, muteVideoAudio, format, exportQuality, exportFrameRate, captionStyle, captionPosition, captionScale, musicFadeInMs, musicFadeOutMs, duckMusicUnderVoiceover) {
        if (loaded) withContext(Dispatchers.IO) {
            saveProject(context, Project(
                clips, music, titleText, brightness, muteVideoAudio, format,
                captions, saturation, filterPreset, photoLayers, voiceover, beatMarkers, captionLanguageId,
                sourceVolume, musicVolume, voiceoverVolume, exportQuality, exportFrameRate,
                captionStyle, captionPosition, captionScale, musicFadeInMs, musicFadeOutMs, duckMusicUnderVoiceover
            ))
        }
    }

    LaunchedEffect(selected?.id, selected?.startMs, selected?.endMs) {
        player.pause()
        if (selected == null) {
            player.clearMediaItems()
            position = 0L
            trimStart = 0L
            trimEnd = 0L
        } else {
            trimStart = selected.startMs
            trimEnd = selected.endMs
            position = 0L
            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(selected.startMs)
                .setEndPositionMs(selected.endMs)
                .build()
            player.setMediaItem(MediaItem.Builder().setUri(selected.uri).setClippingConfiguration(clipping).build())
            player.prepare()
        }
    }

    LaunchedEffect(selected?.id) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0L)
            delay(200L)
        }
    }

    LaunchedEffect(selected?.speed) {
        player.setPlaybackParameters(PlaybackParameters(selected?.speed ?: 1f))
    }

    LaunchedEffect(music) {
        audioPlayer.pause()
        musicPlaying = false
        if (music == null) audioPlayer.clearMediaItems() else {
            audioPlayer.setMediaItem(MediaItem.fromUri(music!!))
            audioPlayer.prepare()
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }
            scope.launch {
                message = "Reading selected videos…"
                val imported = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        runCatching {
                            val meta = MediaMetadataRetriever()
                            val duration = try {
                                meta.setDataSource(context, uri)
                                meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                            } finally { meta.release() }
                            if (duration < 200L) null else Clip(uri = uri, name = nameForUri(context, uri), sourceMs = duration)
                        }.getOrNull()
                    }
                }
                if (imported.isEmpty()) message = "No readable video found. Try MP4 or MOV."
                else {
                    recordUndo()
                    clips = clips + imported
                    if (selectedId == null) selectedId = imported.first().id
                    tab = 0
                    message = "Imported " + imported.size + " clip(s)."
                }
            }
        }
    }

    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            scope.launch {
                music = uri
                musicName = withContext(Dispatchers.IO) { nameForUri(context, uri) }
                message = "Background music selected."
            }
        }
    }

    val srtImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: throw IllegalStateException("Could not read the subtitle file.")
                    }
                    val parsed = parseSrtText(text, clips)
                    recordUndo()
                    captions = parsed
                    message = if (parsed.isEmpty()) "No valid SRT subtitles were found." else "Imported " + parsed.size + " subtitle lines."
                } catch (error: Exception) {
                    message = "SRT import failed: " + (error.message ?: "invalid file")
                }
            }
        }
    }

    val srtExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = writeSrt(captions, clips)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
                            ?: throw IllegalStateException("Could not write the subtitle file.")
                    }
                    message = "Subtitles exported as an SRT file."
                } catch (error: Exception) {
                    message = "SRT export failed: " + (error.message ?: "unknown error")
                }
            }
        }
    }

    val projectExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    message = "Packing project settings and source media into a portable archive…"
                    val snapshot = currentProject()
                    withContext(Dispatchers.IO) {
                        saveProject(context, snapshot)
                        ProjectArchive.export(context, uri)
                    }
                    message = "Project bundle saved. It includes the media used by this edit."
                } catch (error: Exception) {
                    message = "Project bundle export failed: " + (error.message ?: "file access error")
                }
            }
        }
    }

    val projectImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    recordUndo()
                    message = "Opening project bundle and restoring its media…"
                    withContext(Dispatchers.IO) { ProjectArchive.import(context, uri) }
                    val restored = withContext(Dispatchers.IO) { loadProject(context) }
                    restoreProject(restored)
                    message = "Project opened with its packaged media and edit settings."
                } catch (error: Exception) {
                    message = "Project import failed: " + (error.message ?: "invalid project archive")
                }
            }
        }
    }

    val photoLayerPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }
            scope.launch {
                val totalDuration = clips.sumOf { (it.durationMs / it.speed).toLong() }.coerceAtLeast(1_000L)
                val additions = withContext(Dispatchers.IO) {
                    uris.map { uri ->
                        PhotoLayer(uri = uri, name = nameForUri(context, uri), startMs = 0L, endMs = totalDuration)
                    }
                }
                recordUndo()
                photoLayers = photoLayers + additions
                message = "Added " + additions.size + " image layer(s). Adjust their timing and position in Layers."
            }
        }
    }

    fun startVoiceRecordingActual() {
        if (voiceoverRecording) return
        val file = File(context.filesDir, "clipforge_voiceover_" + System.currentTimeMillis() + ".m4a")
        var active: MediaRecorder? = null
        try {
            active = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            active.setAudioSource(MediaRecorder.AudioSource.MIC)
            active.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            active.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            active.setAudioSamplingRate(44_100)
            active.setAudioEncodingBitRate(128_000)
            active.setOutputFile(file.absolutePath)
            active.prepare()
            active.start()
            voiceRecorder = active
            voiceoverFile = file
            voiceoverRecording = true
            message = "Recording voice-over from the beginning. Tap Stop recording when finished."
        } catch (error: Exception) {
            runCatching { active?.release() }
            file.delete()
            message = "Could not start voice recording: " + (error.message ?: "microphone unavailable")
        }
    }

    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceRecordingActual()
        else message = "Microphone permission is required to record a voice-over."
    }

    fun startVoiceRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecordingActual()
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopVoiceRecording() {
        val active = voiceRecorder
        val recordedFile = voiceoverFile
        voiceRecorder = null
        voiceoverRecording = false
        var succeeded = true
        try {
            active?.stop()
        } catch (_: Exception) {
            succeeded = false
        } finally {
            runCatching { active?.release() }
        }
        if (succeeded && recordedFile != null && recordedFile.exists() && recordedFile.length() > 1_024L) {
            recordUndo()
            voiceover = Uri.fromFile(recordedFile)
            voiceoverName = recordedFile.name
            message = "Voice-over saved. It will be mixed from the beginning of the video."
        } else {
            recordedFile?.delete()
            message = "Voice recording was too short or failed. Please record again."
        }
    }

    fun freezeFrameSelected() {
        val clip = selected ?: run {
            message = "Import and select a video before creating a freeze frame."
            return
        }
        val frameTimeMs = (clip.startMs + position).coerceIn(clip.startMs, (clip.endMs - 1L).coerceAtLeast(clip.startMs))
        scope.launch {
            try {
                message = "Capturing freeze frame…"
                val layer = withContext(Dispatchers.IO) {
                    val retriever = MediaMetadataRetriever()
                    var bitmap: Bitmap? = null
                    try {
                        retriever.setDataSource(context, clip.uri)
                        bitmap = retriever.getFrameAtTime(frameTimeMs * 1_000L, MediaMetadataRetriever.OPTION_CLOSEST)
                            ?: throw IllegalStateException("Couldn't decode a frame from this video.")
                        val freezeFile = File(context.filesDir, "clipforge_freeze_" + System.currentTimeMillis() + ".jpg")
                        freezeFile.outputStream().use { stream ->
                            if (!bitmap!!.compress(CompressFormat.JPEG, 94, stream)) {
                                throw IllegalStateException("Couldn't save the captured frame.")
                            }
                        }
                        val globalStart = timelinePositionMs.coerceIn(0L, (timelineDurationMs - 100L).coerceAtLeast(0L))
                        val globalEnd = (globalStart + 1_500L).coerceAtMost(timelineDurationMs).coerceAtLeast(globalStart + 100L)
                        PhotoLayer(
                            uri = Uri.fromFile(freezeFile),
                            name = "Freeze frame " + time(position),
                            startMs = globalStart,
                            endMs = globalEnd,
                            position = "center",
                            scale = 1f,
                            opacity = 1f,
                            rotation = 0f,
                            fullFrame = true
                        )
                    } finally {
                        runCatching { bitmap?.recycle() }
                        runCatching { retriever.release() }
                    }
                }
                recordUndo()
                photoLayers = photoLayers + layer
                message = "Freeze frame added to the Layers track for " + time(layer.endMs - layer.startMs) + "."
            } catch (error: Exception) {
                message = "Freeze frame failed: " + (error.message ?: "unsupported video")
            }
        }
    }

    fun exportProjectBundle() { projectExportLauncher.launch("ClipForge_Project.zip") }
    fun openProjectBundle() { projectImportLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }
    fun importVideos() { videoPicker.launch(arrayOf("video/*")) }
    fun importMusic() { musicPicker.launch(arrayOf("audio/*")) }
    fun importPhotoLayers() { photoLayerPicker.launch(arrayOf("image/*")) }

    fun addTextSticker() {
        if (clips.isEmpty()) {
            message = "Import a video before adding a text layer."
            return
        }
        val placeholderUri = textLayerPlaceholderUri(context)
        val duration = timelineDurationMs
        val start = timelinePositionMs.coerceIn(0L, (duration - 100L).coerceAtLeast(0L))
        val end = (start + 5_000L).coerceAtMost(duration).coerceAtLeast(start + 100L)
        recordUndo()
        photoLayers = photoLayers + PhotoLayer(
            uri = placeholderUri,
            name = "Text sticker",
            startMs = start,
            endMs = end,
            position = "bottom-right",
            scale = 0.5f,
            opacity = 1f,
            rotation = 0f,
            fullFrame = false,
            textContent = "Your text",
            textStyle = captionStyle
        )
        message = "Text sticker added. Edit its wording, style and timing in Layers."
    }

    fun removeImageBackground(layer: PhotoLayer) {
        if (layer.textContent != null || layer.fullFrame) {
            message = "Select a regular image/photo layer for person cutout."
            return
        }
        scope.launch {
            message = "Detecting the person and creating a transparent cutout…"
            try {
                val cutoutUri = withContext(Dispatchers.IO) { createPersonCutout(context, layer.uri) }
                recordUndo()
                photoLayers = photoLayers.map { current ->
                    if (current.id == layer.id) current.copy(uri = cutoutUri, name = current.name + " · Cutout") else current
                }
                message = "Person cutout created as a transparent PNG. Review its edges in the preview."
            } catch (error: Exception) {
                message = "Cutout failed: " + (error.message ?: "no clear person found; try a sharper portrait")
            }
        }
    }

    fun generateAutoCaptions() {
        if (captioning || clips.isEmpty()) {
            if (clips.isEmpty()) message = "Import a video before generating captions."
            return
        }
        captioning = true
        scope.launch {
            try {
                val generated = withContext(Dispatchers.IO) {
                    val sources = clips.map { CaptionSource(it.id, it.uri, it.startMs, it.endMs) }
                    val wordMap = AutoCaptionEngine.transcribeAll(
                        context,
                        sources,
                        CaptionLanguage.fromId(captionLanguageId)
                    ) { status ->
                        scope.launch { message = status }
                    }
                    clips.flatMap { clip ->
                        makeCaptionLines(clip.id, wordMap[clip.id].orEmpty(), clip.durationMs)
                    }
                }
                if (generated.isEmpty()) {
                    message = "No clear English speech was detected. You can still add/edit subtitles manually or import an SRT."
                } else {
                    recordUndo()
                    val currentClipIds = clips.map { it.id }.toSet()
                    captions = captions.filterNot { it.clipId in currentClipIds } + generated
                    message = "Generated " + generated.size + " caption lines on-device. Review names and timing before export."
                }
            } catch (error: Exception) {
                message = "Auto captions failed: " + (error.message ?: "speech recognition unavailable")
            } finally {
                captioning = false
            }
        }
    }

    fun detectMusicBeats() {
        val audioUri = music
        if (audioUri == null) {
            message = "Choose a background music file first."
            return
        }
        if (beatDetecting) return
        beatDetecting = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    BeatDetectionEngine.detect(context, audioUri) { status ->
                        scope.launch { message = status }
                    }
                }
                val markers = mutableListOf<Long>()
                val trackDuration = result.durationMs.coerceAtLeast(1L)
                var offset = 0L
                while (offset < timelineDurationMs && markers.size < 400) {
                    result.beatMarkersMs.forEach { marker ->
                        val value = offset + marker
                        if (value < timelineDurationMs && markers.size < 400) markers.add(value)
                    }
                    offset += trackDuration
                }
                recordUndo()
                beatMarkers = markers.distinct().sorted()
                message = if (beatMarkers.isEmpty()) {
                    "No prominent beats found. You can add markers manually while listening."
                } else {
                    "Detected " + result.beatMarkersMs.size + " beat peaks and aligned " + beatMarkers.size +
                        " markers across the current timeline. Review peaks before cutting."
                }
            } catch (error: Exception) {
                message = "Beat detection failed: " + (error.message ?: "unsupported audio file")
            } finally {
                beatDetecting = false
            }
        }
    }

    fun generateCaptionNarration() {
        if (ttsGenerating) return
        if (captions.isEmpty()) {
            message = "Generate or import caption lines before creating narration."
            return
        }
        ttsGenerating = true
        scope.launch {
            try {
                val projectCues = clips.flatMapIndexed { index, clip ->
                    val offset = clips.take(index).sumOf { (it.durationMs / it.speed).toLong() }
                    captions.filter { it.clipId == clip.id }.map { cue ->
                        NarrationCue(
                            startMs = offset + (cue.startMs / clip.speed).toLong(),
                            endMs = offset + (cue.endMs / clip.speed).toLong(),
                            text = cue.text
                        )
                    }
                }
                val audioFile = CaptionTtsNarrator.generate(
                    context = context,
                    cues = projectCues,
                    languageId = captionLanguageId,
                    durationMs = timelineDurationMs,
                    onStatus = { message = it }
                )
                recordUndo()
                voiceover = Uri.fromFile(audioFile)
                voiceoverName = audioFile.name
                message = "Created speech narration from " + projectCues.size +
                    " subtitle lines. It is now on your voice-over track; preview the mix and export."
            } catch (error: Exception) {
                message = "Speech narration failed: " + (error.message ?: "check installed language voice data")
            } finally {
                ttsGenerating = false
            }
        }
    }

    fun updateClip(clip: Clip) { clips = clips.map { if (it.id == clip.id) clip else it } }

    fun generateAutoClips() {
        val source = selected ?: run {
            message = "Import and select a long video before generating auto clips."
            return
        }
        if (autoClipBusy) return
        if (source.durationMs < 3_000L) {
            message = "Choose a source clip at least 3 seconds long."
            return
        }
        autoClipSource = source
        autoClipSuggestions = emptyList()
        autoClipBusy = true
        scope.launch {
            try {
                val suggestions = withContext(Dispatchers.IO) {
                    AutoClipMaker.analyze(
                        context = context,
                        uri = source.uri,
                        sourceStartMs = source.startMs,
                        sourceEndMs = source.endMs,
                        targetDurationMs = autoClipDurationMs,
                        language = CaptionLanguage.fromId(captionLanguageId)
                    ) { status ->
                        scope.launch { message = status }
                    }
                }
                autoClipSuggestions = suggestions
                message = "Auto Clip Maker found " + suggestions.size + " options. Review and add the highlights you want."
            } catch (error: Exception) {
                message = "Auto Clip Maker failed: " + (error.message ?: "unsupported source")
            } finally {
                autoClipBusy = false
            }
        }
    }

    fun makeSuggestedClip(source: Clip, suggestion: AutoClipSuggestion, ordinal: Int): Clip {
        val start = (source.startMs + suggestion.startMs).coerceIn(source.startMs, (source.endMs - 100L).coerceAtLeast(source.startMs))
        val end = (source.startMs + suggestion.endMs).coerceIn((start + 100L).coerceAtMost(source.endMs), source.endMs)
        return source.copy(
            id = UUID.randomUUID().toString(),
            name = source.name + " · Auto " + ordinal,
            startMs = start,
            endMs = end,
            transition = "Cut"
        )
    }

    fun addAutoClipSuggestion(suggestion: AutoClipSuggestion) {
        val source = autoClipSource ?: selected ?: run {
            message = "Run Auto Clip Maker on a source video first."
            return
        }
        recordUndo()
        val added = makeSuggestedClip(source, suggestion, clips.size + 1)
        clips = clips + added
        val generatedCaptions = makeCaptionLines(added.id, suggestion.wordTimings, added.durationMs)
        if (generatedCaptions.isNotEmpty()) {
            captions = captions + generatedCaptions
            captionStyle = "Karaoke"
        }
        selectedId = added.id
        autoClipSuggestions = autoClipSuggestions.filterNot { it.id == suggestion.id }
        message = "Added " + time(added.durationMs) + " highlight" +
            (if (generatedCaptions.isNotEmpty()) " with timed word captions" else "") + " to the timeline."
    }

    fun addAllAutoClipSuggestions() {
        val source = autoClipSource ?: selected ?: return
        if (autoClipSuggestions.isEmpty()) return
        recordUndo()
        val added = autoClipSuggestions.mapIndexed { index, suggestion ->
            makeSuggestedClip(source, suggestion, clips.size + index + 1)
        }
        clips = clips + added
        val generatedCaptions = added.zip(autoClipSuggestions).flatMap { (clip, suggestion) ->
            makeCaptionLines(clip.id, suggestion.wordTimings, clip.durationMs)
        }
        if (generatedCaptions.isNotEmpty()) {
            captions = captions + generatedCaptions
            captionStyle = "Karaoke"
        }
        selectedId = added.firstOrNull()?.id ?: selectedId
        autoClipSuggestions = emptyList()
        message = "Added " + added.size + " offline highlights" +
            (if (generatedCaptions.isNotEmpty()) " with timed word captions" else "") + " to the timeline."
    }

    fun commitTrim() {
        val clip = selected ?: return
        recordUndo()
        val start = trimStart.coerceIn(0L, (clip.sourceMs - 100L).coerceAtLeast(0L))
        val end = trimEnd.coerceIn(start + 100L, clip.sourceMs)
        updateClip(clip.copy(startMs = start, endMs = end))
        message = "Trim updated."
    }

    fun splitClip() {
        val clip = selected ?: return
        val cut = clip.startMs + position.coerceIn(0L, clip.durationMs)
        val minPart = max(250L, minOf(500L, clip.durationMs / 5L))
        if (cut < clip.startMs + minPart || cut > clip.endMs - minPart) {
            message = "Move the playhead nearer to the middle, then split."
            return
        }
        recordUndo()
        val left = clip.copy(id = UUID.randomUUID().toString(), endMs = cut)
        val right = clip.copy(id = UUID.randomUUID().toString(), startMs = cut)
        clips = clips.toMutableList().apply {
            removeAt(selectedIndex)
            add(selectedIndex, right)
            add(selectedIndex, left)
        }
        selectedId = right.id
        message = "Clip split."
    }

    fun deleteClip() {
        if (selectedIndex < 0) return
        recordUndo()
        val updated = clips.toMutableList()
        updated.removeAt(selectedIndex)
        clips = updated
        selectedId = updated.getOrNull(selectedIndex.coerceAtMost(updated.lastIndex))?.id
        message = "Selected clip deleted."
    }

    fun moveClip(direction: Int) {
        if (selectedIndex < 0) return
        val target = (selectedIndex + direction).coerceIn(0, clips.lastIndex)
        if (target == selectedIndex) return
        recordUndo()
        clips = clips.toMutableList().apply { add(target, removeAt(selectedIndex)) }
    }

    fun exportVideo() {
        if (clips.isEmpty() || exporting) return
        player.pause()
        val timelineMs = clipTimelineDurationMs(clips)
        val (requestedVideoBitrate, requestedAudioBitrate) = exportBitrates(format, exportQuality)
        val estimatedBytes = ((timelineMs / 1_000.0) * (requestedVideoBitrate + requestedAudioBitrate) / 8.0 * 2.1).toLong() +
            96L * 1024L * 1024L
        val availableBytes = StatFs(context.cacheDir.absolutePath).availableBytes
        if (availableBytes < estimatedBytes) {
            message = "Not enough free storage for a safe render. Estimated need: " +
                (estimatedBytes / (1024L * 1024L)) + " MB; available: " +
                (availableBytes / (1024L * 1024L)) + " MB. Free space or choose Saver quality/shorter clips."
            return
        }
        exportProgress = 0
        val folder = File(context.cacheDir, "clipforge")
        folder.mkdirs()
        val output = File(folder, "clipforge_" + System.currentTimeMillis() + ".mp4")
        try {
            val needsCrossfadeComposition = clips.dropLast(1).any { it.transition == "Crossfade" }
            val sequenceList = mutableListOf<EditedMediaItemSequence>()
            var compositorSettings: VideoCompositorSettings? = null
            if (!needsCrossfadeComposition) {
                val editItems = buildEditedItems(
                    context, clips, captions, photoLayers, titleText, brightness, saturation, filterPreset,
                    format, sourceVolume, exportFrameRate, captionStyle, captionPosition, captionScale
                )
                sequenceList.add(
                    if (muteVideoAudio) EditedMediaItemSequence.withVideoFrom(editItems)
                    else EditedMediaItemSequence.withAudioAndVideoFrom(editItems)
                )
            } else {
                val starts = clipTimelineStartOffsetsMs(clips)
                val transparentUri = transparentFillerUri(context)
                clips.forEachIndexed { index, clip ->
                    val startMs = starts[index]
                    val pad = if (startMs > 0L) {
                        val transparentItem = MediaItem.Builder()
                            .setUri(transparentUri)
                            .setImageDurationMs(startMs)
                            .build()
                        listOf(EditedMediaItem.Builder(transparentItem).build())
                    } else emptyList()
                    val clipItems = buildEditedItems(
                        context, listOf(clip), captions, photoLayers, titleText, brightness, saturation, filterPreset,
                        format, sourceVolume, exportFrameRate, captionStyle, captionPosition, captionScale,
                        projectOffsetStartMs = startMs,
                        previousTransition = clips.getOrNull(index - 1)?.transition,
                        nextTransition = clip.transition
                    )
                    sequenceList.add(EditedMediaItemSequence.withVideoFrom(pad + clipItems))
                }
                if (!muteVideoAudio) {
                    clips.forEachIndexed { index, clip ->
                        if (hasAudioTrack(context, clip.uri)) {
                            val audioItems = mutableListOf<EditedMediaItem>()
                            val startMs = starts[index]
                            if (startMs > 0L) {
                                audioItems.add(
                                    EditedMediaItem.Builder(MediaItem.fromUri(silenceWavUri(context, startMs)))
                                        .setRemoveVideo(true)
                                        .build()
                                )
                            }
                            audioItems.addAll(buildEditedAudioItems(context, clip, sourceVolume))
                            if (audioItems.isNotEmpty()) sequenceList.add(EditedMediaItemSequence.withAudioFrom(audioItems))
                        }
                    }
                }
                val trackFades = clips.indices.map { index ->
                    val previous = clips.getOrNull(index - 1)
                    val fade = if (previous?.transition == "Crossfade") {
                        crossfadeDurationMs(previous, clips[index])
                    } else 0L
                    VideoTrackFade(starts[index], fade)
                }
                compositorSettings = ClipForgeVideoCompositorSettings(trackFades)
            }
            music?.let { audioUri ->
                val projectDuration = clipTimelineDurationMs(clips)
                if (musicFadeInMs > 0L || musicFadeOutMs > 0L || (duckMusicUnderVoiceover && voiceover != null)) {
                    val musicItems = buildMusicItems(
                        context = context,
                        musicUri = audioUri,
                        projectDurationMs = projectDuration,
                        volume = musicVolume,
                        fadeInMs = musicFadeInMs,
                        fadeOutMs = musicFadeOutMs,
                        voiceoverUri = if (duckMusicUnderVoiceover) voiceover else null
                    )
                    if (musicItems.isNotEmpty()) sequenceList.add(EditedMediaItemSequence.withAudioFrom(musicItems))
                    else {
                        val gain = GainProcessor(DefaultGainProvider.Builder(musicVolume.coerceIn(0f, 1f)).build())
                        val adjusted = EditedMediaItem.Builder(MediaItem.fromUri(audioUri)).setEffects(Effects(listOf(gain), emptyList())).build()
                        sequenceList.add(EditedMediaItemSequence.withAudioFrom(listOf(adjusted)).buildUpon().setIsLooping(true).build())
                    }
                } else {
                    val audioGain = GainProcessor(DefaultGainProvider.Builder(musicVolume.coerceIn(0f, 1f)).build())
                    val adjustedAudio = EditedMediaItem.Builder(MediaItem.fromUri(audioUri))
                        .setEffects(Effects(listOf(audioGain), emptyList()))
                        .build()
                    sequenceList.add(EditedMediaItemSequence.withAudioFrom(listOf(adjustedAudio)).buildUpon().setIsLooping(true).build())
                }
            }
            voiceover?.let { voiceUri ->
                val voiceGain = GainProcessor(DefaultGainProvider.Builder(voiceoverVolume.coerceIn(0f, 1f)).build())
                val voiceItem = EditedMediaItem.Builder(MediaItem.fromUri(voiceUri))
                    .setEffects(Effects(listOf(voiceGain), emptyList()))
                    .build()
                sequenceList.add(EditedMediaItemSequence.withAudioFrom(listOf(voiceItem)))
            }
            val compositionBuilder = Composition.Builder(sequenceList)
            compositorSettings?.let { compositionBuilder.setVideoCompositorSettings(it) }
            val composition = compositionBuilder.build()
            exporting = true
            outputUri = null
            message = "Rendering MP4 on your phone. Keep the app open until it finishes."
            val (videoBitrate, audioBitrate) = exportBitrates(format, exportQuality)
            val encoderFactory = DefaultEncoderFactory.Builder(context.applicationContext)
                .setEnableFallback(true)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder().setBitrate(videoBitrate).build()
                )
                .setRequestedAudioEncoderSettings(
                    AudioEncoderSettings.Builder().setBitrate(audioBitrate).build()
                )
                .build()
            val worker = Transformer.Builder(context.applicationContext)
                .setEncoderFactory(encoderFactory)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        scope.launch {
                            try {
                                val savedUri = withContext(Dispatchers.IO) { saveToGallery(context, output) }
                                outputUri = savedUri
                                exportProgress = 100
                                message = "Export complete. Saved to Movies/ClipForge."
                            } catch (error: Exception) {
                                message = "Rendered, but Gallery save failed: " + (error.message ?: "unknown error")
                            } finally {
                                exporting = false
                                transformer = null
                                output.delete()
                            }
                        }
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        exporting = false
                        exportProgress = 0
                        transformer = null
                        message = "Export failed: " + (exportException.message ?: "unsupported media or codec")
                        output.delete()
                    }
                })
                .build()
            transformer = worker
            worker.start(composition, output.absolutePath)
            scope.launch {
                val holder = ProgressHolder()
                while (exporting && transformer === worker) {
                    val state = runCatching { worker.getProgress(holder) }
                        .getOrDefault(Transformer.PROGRESS_STATE_NOT_STARTED)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        exportProgress = holder.progress.coerceIn(0, 100)
                    }
                    delay(250L)
                }
            }
        } catch (error: Exception) {
            exporting = false
            exportProgress = 0
            transformer = null
            message = "Couldn't start export: " + (error.message ?: "check selected media")
            output.delete()
        }
    }

    fun shareVideo() {
        val uri = outputUri ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share your ClipForge video"))
    }

    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppHeader(clips.size)
            Crossfade(targetState = tab, animationSpec = tween(durationMillis = 180), label = "editor-workspace") { activeTab ->
                when (activeTab) {
                0 -> EditorPanel(
                    clips, selectedId, selectedIndex, selected, format, player, position, trimStart, trimEnd, titleText, captions, photoLayers, captionStyle, captionPosition, captionScale, message,
                    canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty(),
                    onUndo = ::undoProject, onRedo = ::redoProject,
                    onSaveProjectBundle = ::exportProjectBundle,
                    onOpenProjectBundle = ::openProjectBundle,
                    onImport = ::importVideos,
                    onFreeze = ::freezeFrameSelected,
                    onSelect = { selectedId = it },
                    onSeek = { positionMs -> player.seekTo(positionMs); position = positionMs },
                    onPlay = { if (player.isPlaying) player.pause() else player.play() },
                    onStartChange = { value ->
                        val clip = selected
                        val snapped = if (snapToBeats && clip != null) snapTrimToBeat(
                            clip, value, clipStartOffsetsMs.getOrNull(selectedIndex) ?: 0L, beatMarkers
                        ) else value
                        trimStart = snapped.coerceIn(0L, (trimEnd - 100L).coerceAtLeast(0L))
                    },
                    onEndChange = { value ->
                        val clip = selected
                        val snapped = if (snapToBeats && clip != null) snapTrimToBeat(
                            clip, value, clipStartOffsetsMs.getOrNull(selectedIndex) ?: 0L, beatMarkers
                        ) else value
                        val maxEnd = selected?.sourceMs ?: value
                        trimEnd = snapped.coerceIn((trimStart + 100L).coerceAtMost(maxEnd), maxEnd)
                    },
                    onTrim = ::commitTrim,
                    onSplit = ::splitClip,
                    onDelete = ::deleteClip,
                    onMove = ::moveClip,
                    onAdd = ::importVideos,
                    beatMarkers = beatMarkers,
                    timelineZoom = timelineZoom,
                    snapToBeats = snapToBeats,
                    onTimelineZoom = { timelineZoom = it },
                    onToggleSnap = { snapToBeats = !snapToBeats },
                    autoClipSuggestions = autoClipSuggestions,
                    autoClipSourceName = autoClipSource?.name,
                    autoClipDurationMs = autoClipDurationMs,
                    autoClipBusy = autoClipBusy,
                    onAutoClipDuration = { autoClipDurationMs = it },
                    onGenerateAutoClips = ::generateAutoClips,
                    onUseShortsFormat = { format = "9:16 FHD"; message = "Vertical Full HD output selected. Review horizontal clips for subject framing." },
                    onAddAutoClip = ::addAutoClipSuggestion,
                    onAddAllAutoClips = ::addAllAutoClipSuggestions
                )
                1 -> AudioPanel(
                    musicName, music, music != null, musicPlaying, muteVideoAudio, message,
                    voiceoverName = voiceoverName,
                    hasVoiceover = voiceover != null,
                    voiceoverRecording = voiceoverRecording,
                    sourceVolume = sourceVolume,
                    musicVolume = musicVolume,
                    voiceoverVolume = voiceoverVolume,
                    musicFadeInMs = musicFadeInMs,
                    musicFadeOutMs = musicFadeOutMs,
                    duckMusicUnderVoiceover = duckMusicUnderVoiceover,
                    beatMarkers = beatMarkers,
                    beatDetecting = beatDetecting,
                    timelinePositionMs = timelinePositionMs,
                    timelineDurationMs = timelineDurationMs,
                    onPick = ::importMusic,
                    onRemove = { recordUndo(); music = null; musicName = null; message = "Background track removed." },
                    onPreview = {
                        if (music == null) importMusic()
                        else if (musicPlaying) { audioPlayer.pause(); musicPlaying = false }
                        else { audioPlayer.play(); musicPlaying = true }
                    },
                    onMute = { recordUndo(); muteVideoAudio = it },
                    onSourceVolume = { recordUndo(); sourceVolume = it },
                    onMusicVolume = { recordUndo(); musicVolume = it },
                    onVoiceoverVolume = { recordUndo(); voiceoverVolume = it },
                    onMusicFadeIn = { recordUndo(); musicFadeInMs = it },
                    onMusicFadeOut = { recordUndo(); musicFadeOutMs = it },
                    onDuckMusic = { recordUndo(); duckMusicUnderVoiceover = it },
                    onStartVoiceover = ::startVoiceRecording,
                    onStopVoiceover = ::stopVoiceRecording,
                    onRemoveVoiceover = {
                        recordUndo()
                        voiceover?.path?.let { path -> File(path).takeIf { it.exists() }?.delete() }
                        voiceover = null
                        voiceoverName = null
                        message = "Voice-over removed."
                    },
                    onMarkBeat = {
                        recordUndo()
                        beatMarkers = (beatMarkers + timelinePositionMs.coerceIn(0L, timelineDurationMs)).distinct().sorted()
                        message = "Beat marker added at " + time(timelinePositionMs) + "."
                    },
                    onDetectBeats = ::detectMusicBeats,
                    onDeleteBeat = { marker ->
                        recordUndo()
                        beatMarkers = beatMarkers.filterNot { it == marker }
                    }
                )
                2 -> MotionPanel(
                    selected, titleText, brightness, saturation, filterPreset, message,
                    onClipChange = ::updateClip,
                    onTitle = { titleText = it },
                    onBrightness = { brightness = it },
                    onSaturation = { saturation = it },
                    onFilter = { filterPreset = it }
                )
                3 -> CaptionsPanel(
                    captions, clips, captioning, message,
                    captionLanguageId = captionLanguageId,
                    ttsGenerating = ttsGenerating,
                    captionStyle = captionStyle,
                    captionPosition = captionPosition,
                    captionScale = captionScale,
                    onCaptionStyle = { captionStyle = it },
                    onCaptionPosition = { captionPosition = it },
                    onCaptionScale = { captionScale = it },
                    onLanguageChange = { captionLanguageId = it },
                    onAutoCaption = ::generateAutoCaptions,
                    onGenerateNarration = ::generateCaptionNarration,
                    onImportSrt = { srtImportLauncher.launch(arrayOf("application/x-subrip", "text/plain", "application/octet-stream")) },
                    onExportSrt = { srtExportLauncher.launch("ClipForge-subtitles.srt") },
                    onEdit = { id, text -> captions = captions.map { if (it.id == id) it.copy(text = text) else it } },
                    onDelete = { id -> captions = captions.filterNot { it.id == id } }
                )
                4 -> LayersPanel(
                    layers = photoLayers,
                    timelineDurationMs = timelineDurationMs,
                    message = message,
                    onAdd = ::importPhotoLayers,
                    onAddText = ::addTextSticker,
                    onUpdate = { layer -> photoLayers = photoLayers.map { if (it.id == layer.id) layer else it } },
                    onDelete = { id -> photoLayers = photoLayers.filterNot { it.id == id } },
                    onRemoveBackground = ::removeImageBackground
                )
                else -> ExportPanel(
                    clips, musicName, titleText, brightness, format, exporting, outputUri != null, message,
                    onFormat = { format = it },
                    onExport = ::exportVideo,
                    onCancel = { transformer?.cancel(); message = "Cancel requested…" },
                    onShare = ::shareVideo,
                    quality = exportQuality,
                    frameRate = exportFrameRate,
                    exportProgress = exportProgress,
                    freeStorageBytes = StatFs(context.cacheDir.absolutePath).availableBytes,
                    onQuality = { exportQuality = it },
                    onFrameRate = { exportFrameRate = it }
                )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        NavigationBar(tab) { tab = it }
    }
}

@Composable
private fun AppHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Lime),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ContentCut, null, tint = Bg, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("ClipForge", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
            Text("VIDEO EDITOR", fontSize = 9.sp, color = Muted, letterSpacing = 1.7.sp, fontWeight = FontWeight.Medium)
        }
        Row(
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(PanelAlt).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(Icons.Default.Movie, null, tint = Lime, modifier = Modifier.size(14.dp))
            Text(count.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(if (count == 1) "clip" else "clips", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun EditorPanel(
    clips: List<Clip>, selectedId: String?, selectedIndex: Int, selected: Clip?, format: String, player: ExoPlayer,
    position: Long, trimStart: Long, trimEnd: Long, titleText: String, captions: List<CaptionLine>,
    photoLayers: List<PhotoLayer>, captionStyle: String, captionPosition: String, captionScale: Float, message: String?,
    canUndo: Boolean, canRedo: Boolean, onUndo: () -> Unit, onRedo: () -> Unit,
    onSaveProjectBundle: () -> Unit, onOpenProjectBundle: () -> Unit,
    onImport: () -> Unit, onFreeze: () -> Unit, onSelect: (String) -> Unit, onSeek: (Long) -> Unit, onPlay: () -> Unit,
    onStartChange: (Long) -> Unit, onEndChange: (Long) -> Unit, onTrim: () -> Unit,
    onSplit: () -> Unit, onDelete: () -> Unit, onMove: (Int) -> Unit, onAdd: () -> Unit,
    beatMarkers: List<Long>, timelineZoom: Float, snapToBeats: Boolean,
    onTimelineZoom: (Float) -> Unit, onToggleSnap: () -> Unit,
    autoClipSuggestions: List<AutoClipSuggestion>,
    autoClipSourceName: String?,
    autoClipDurationMs: Long,
    autoClipBusy: Boolean,
    onAutoClipDuration: (Long) -> Unit,
    onGenerateAutoClips: () -> Unit,
    onUseShortsFormat: () -> Unit,
    onAddAutoClip: (AutoClipSuggestion) -> Unit,
    onAddAllAutoClips: () -> Unit
) {
    val keyframeFraction = if (selected == null || selected.durationMs <= 0L) 0f else
        (position.toFloat() / selected.durationMs.toFloat()).coerceIn(0f, 1f)
    val previewAspect = when {
        format.startsWith("9:16") -> 9f / 16f
        format.startsWith("16:9") || format == "16:9 Video" -> 16f / 9f
        format == "1:1 Square" -> 1f
        else -> 1.22f
    }
    val previewZoom = selected?.let {
        interpolateThree(it.zoomStart, it.zoomMid, it.zoomEnd, keyframeFraction, it.motionCurve)
    } ?: 1f
    val previewRotation = selected?.let {
        interpolateThree(it.rotationStart, it.rotationMid, it.rotationEnd, keyframeFraction, it.motionCurve)
    } ?: 0f
    val activeCaption = selected?.let { clip ->
        captions.firstOrNull { it.clipId == clip.id && position >= it.startMs && position < it.endMs }
    }
    val previewCaptionAlignment = when (captionPosition) {
        "Top" -> Alignment.TopCenter
        "Center" -> Alignment.Center
        else -> Alignment.BottomCenter
    }
    val previewCaptionPadding = when (captionPosition) {
        "Top" -> Modifier.padding(top = 42.dp)
        "Center" -> Modifier
        else -> Modifier.padding(bottom = 14.dp)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Timeline", fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
            Text("Edit clips and build your sequence", fontSize = 12.sp, color = Muted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onUndo, enabled = canUndo, shape = RoundedCornerShape(11.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 7.dp)) {
                Text("Undo")
            }
            OutlinedButton(onClick = onRedo, enabled = canRedo, shape = RoundedCornerShape(11.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 7.dp)) {
                Text("Redo")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onSaveProjectBundle,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 9.dp)
            ) {
                Text("Export project", fontSize = 10.sp, maxLines = 1)
            }
            OutlinedButton(
                onClick = onOpenProjectBundle,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 9.dp)
            ) {
                Text("Open project", fontSize = 10.sp, maxLines = 1)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Lime, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Auto Clip Maker", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("OFFLINE HIGHLIGHT FINDER", color = Lime, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    }
                    Text("ON DEVICE", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Find short highlight moments using bundled speech recognition and local scene-change analysis. No video is uploaded.",
                    color = Muted, fontSize = 11.sp, lineHeight = 15.sp
                )
                Text(
                    if (selected == null) "Select a source video to begin."
                    else "Source: " + (autoClipSourceName ?: selected.name) + " · " + time(selected.durationMs),
                    color = if (selected == null) Muted else Color.White, fontSize = 11.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(15_000L, 30_000L, 45_000L, 60_000L).forEach { length ->
                        val active = autoClipDurationMs == length
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !autoClipBusy) { onAutoClipDuration(length) },
                            color = if (active) Lime else PanelAlt,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                (length / 1_000L).toString() + " sec",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                color = if (active) Bg else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Button(
                    onClick = onGenerateAutoClips,
                    enabled = selected != null && selected.durationMs >= 3_000L && !autoClipBusy,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg)
                ) {
                    if (autoClipBusy) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp), color = Bg, strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Analyzing on device…", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Find highlights", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = onUseShortsFormat,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Icon(Icons.Default.Movie, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Set vertical Shorts output (9:16 FHD)")
                }
                if (autoClipBusy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Lime)
                if (autoClipSuggestions.isNotEmpty()) {
                    HorizontalDivider(color = PanelAlt)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SUGGESTED HIGHLIGHTS", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.weight(1f))
                        Text(autoClipSuggestions.size.toString() + " found", color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    autoClipSuggestions.forEachIndexed { index, suggestion ->
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = PanelAlt)) {
                            Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(30.dp).clip(CircleShape).background(Bg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text((index + 1).toString(), color = Lime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(suggestion.reason, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            time(suggestion.startMs) + " – " + time(suggestion.endMs) + " · score " + suggestion.score,
                                            color = Muted, fontSize = 10.sp
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onAddAutoClip(suggestion) },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(9.dp)
                                    ) { Text("Add", fontSize = 10.sp) }
                                }
                                if (suggestion.transcriptExcerpt.isNotBlank()) {
                                    Text(
                                        "“" + suggestion.transcriptExcerpt + "”",
                                        color = Muted, fontSize = 10.sp, lineHeight = 14.sp,
                                        maxLines = 3, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    if (autoClipSuggestions.size > 1) {
                        OutlinedButton(
                            onClick = onAddAllAutoClips,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(11.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Add all " + autoClipSuggestions.size + " highlights to timeline")
                        }
                    }
                }
            }
        }
        if (selected == null) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Box(Modifier.size(78.dp).clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF36465A), Color(0xFF28341D)))),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Movie, null, tint = Lime, modifier = Modifier.size(38.dp))
                    }
                    Text("Make your first edit", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Choose one or more videos from your phone. ClipForge keeps your project on-device.", fontSize = 13.sp, color = Muted, lineHeight = 19.sp)
                    Button(onClick = onImport, modifier = Modifier.fillMaxWidth().height(53.dp), shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg)) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(7.dp))
                        Text("Import videos", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth().aspectRatio(previewAspect).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize().graphicsLayer(
                                scaleX = previewZoom * (selected?.cropZoom ?: 1f),
                                scaleY = previewZoom * (selected?.cropZoom ?: 1f),
                                translationX = (selected?.cropX ?: 0f) * 120f,
                                translationY = (selected?.cropY ?: 0f) * 120f,
                                rotationZ = previewRotation
                            ),
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    this.player = player
                                    setShutterBackgroundColor(AndroidColor.BLACK)
                                }
                            },
                            update = { if (it.player !== player) it.player = player }
                        )
                        if (titleText.isNotBlank()) {
                            Text(titleText, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                                    .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.75f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                        if (activeCaption != null && activeCaption.text.isNotBlank()) {
                            Text(
                                activeCaption.text,
                                color = captionComposeTextColor(captionStyle),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = (15f * captionScale).sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.align(previewCaptionAlignment).then(previewCaptionPadding)
                                    .padding(horizontal = 12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(captionComposeBackground(captionStyle))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        val totalBeforeSelected = clipTimelineStartOffsetsMs(clips).getOrNull(selectedIndex) ?: 0L
                        val globalPreviewPosition = totalBeforeSelected + if (selected != null) (position / selected.speed).toLong() else 0L
                        PhotoLayerPreview(photoLayers, globalPreviewPosition)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onPlay) {
                            Icon(if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play or pause", tint = Lime, modifier = Modifier.size(30.dp))
                        }
                        Text(time(position), fontSize = 11.sp)
                        Slider(value = position.toFloat().coerceIn(0f, selected.durationMs.toFloat().coerceAtLeast(1f)),
                            onValueChange = { onSeek(it.toLong().coerceIn(0L, selected.durationMs)) },
                            valueRange = 0f..selected.durationMs.toFloat().coerceAtLeast(1f), modifier = Modifier.weight(1f))
                        Text(time(selected.durationMs), fontSize = 11.sp, color = Muted)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TIMELINE", color = Muted, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.3.sp)
                    Spacer(Modifier.weight(1f))
                    Text(clips.size.toString() + " clips · " + time(clips.sumOf { it.durationMs }), color = Lime, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onToggleSnap() },
                        color = if (snapToBeats) Color(0xFF28351E) else PanelAlt,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = if (snapToBeats) Lime else Muted, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(if (snapToBeats) "Beat snap on" else "Beat snap off", color = if (snapToBeats) Lime else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Timeline zoom", color = Muted, fontSize = 10.sp)
                    Slider(
                        value = timelineZoom,
                        onValueChange = onTimelineZoom,
                        valueRange = 0.75f..1.8f,
                        modifier = Modifier.weight(1f)
                    )
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 4.dp)
                ) {
                    itemsIndexed(clips, key = { _, clip -> clip.id }) { index, clip ->
                        TimelineClipCard(
                            clip = clip,
                            index = index,
                            selected = clip.id == selectedId,
                            widthScale = timelineZoom,
                            onClick = { onSelect(clip.id) }
                        )
                    }
                    item(key = "add-clip") {
                        Column(
                            Modifier.width(84.dp).height(100.dp).clip(RoundedCornerShape(12.dp))
                                .background(Panel).clickable(onClick = onAdd).padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = Lime)
                            Text("Add clip", color = Muted, fontSize = 10.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ActionButton("Split", Icons.Default.ContentCut, onSplit, Modifier.weight(1f), selected != null)
                    ActionButton("Freeze", Icons.Default.Movie, onFreeze, Modifier.weight(1f), selected != null)
                    ActionButton("Up", Icons.Default.ArrowUpward, { onMove(-1) }, Modifier.weight(1f), selectedIndex > 0)
                    ActionButton("Down", Icons.Default.ArrowDownward, { onMove(1) }, Modifier.weight(1f), selectedIndex >= 0 && selectedIndex < clips.lastIndex)
                    ActionButton("Delete", Icons.Default.Delete, onDelete, Modifier.weight(1f), selected != null)
                }
            }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Trim clip", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(selected.name, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Start", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(time(trimStart), color = Lime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(value = trimStart.toFloat().coerceIn(0f, selected.sourceMs.toFloat()),
                        onValueChange = { onStartChange(it.toLong()) }, onValueChangeFinished = onTrim,
                        valueRange = 0f..selected.sourceMs.toFloat().coerceAtLeast(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("End", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(time(trimEnd), color = Lime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(value = trimEnd.toFloat().coerceIn(0f, selected.sourceMs.toFloat()),
                        onValueChange = { onEndChange(it.toLong()) }, onValueChangeFinished = onTrim,
                        valueRange = 0f..selected.sourceMs.toFloat().coerceAtLeast(1f))
                    Text("Selected range: " + time((trimEnd - trimStart).coerceAtLeast(0L)), color = Muted, fontSize = 11.sp)
                }
            }
        }
        if (message != null) MessageStrip(message)
    }
}

@Composable
private fun VolumeControl(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.width(130.dp))
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onChange,
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f)
        )
        Text((value * 100).toInt().toString() + "%", color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(38.dp))
    }
}

@Composable
private fun AudioPanel(
    musicName: String?, musicUri: Uri?, hasMusic: Boolean, playing: Boolean, muted: Boolean, message: String?,
    voiceoverName: String?, hasVoiceover: Boolean, voiceoverRecording: Boolean,
    sourceVolume: Float, musicVolume: Float, voiceoverVolume: Float,
    musicFadeInMs: Long, musicFadeOutMs: Long, duckMusicUnderVoiceover: Boolean,
    beatMarkers: List<Long>, beatDetecting: Boolean, timelinePositionMs: Long, timelineDurationMs: Long,
    onPick: () -> Unit, onRemove: () -> Unit, onPreview: () -> Unit, onMute: (Boolean) -> Unit,
    onSourceVolume: (Float) -> Unit, onMusicVolume: (Float) -> Unit, onVoiceoverVolume: (Float) -> Unit,
    onMusicFadeIn: (Long) -> Unit, onMusicFadeOut: (Long) -> Unit, onDuckMusic: (Boolean) -> Unit,
    onStartVoiceover: () -> Unit, onStopVoiceover: () -> Unit, onRemoveVoiceover: () -> Unit,
    onMarkBeat: () -> Unit, onDetectBeats: () -> Unit, onDeleteBeat: (Long) -> Unit
) {
    val context = LocalContext.current
    val waveform by produceState<List<Float>>(initialValue = emptyList(), key1 = musicUri) {
        value = if (musicUri == null) emptyList() else withContext(Dispatchers.IO) {
            runCatching { OfflineWaveformAnalyzer.analyze(context, musicUri, 92) }.getOrDefault(emptyList())
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Sound & music", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Add a background track and control the original clip audio.", color = Muted, fontSize = 13.sp)
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LibraryMusic, null, tint = Lime, modifier = Modifier.size(31.dp))
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Background music", fontWeight = FontWeight.Bold)
                        Text(musicName ?: "No track chosen", color = Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Button(onClick = onPick, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg)) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(7.dp))
                    Text(if (hasMusic) "Replace music" else "Choose music file", fontWeight = FontWeight.Bold)
                }
                if (hasMusic) {
                    if (waveform.isNotEmpty()) {
                        WaveformBars(waveform, Modifier.fillMaxWidth().height(48.dp))
                        Text("Decoded audio waveform · analyzed on this phone", color = Muted, fontSize = 10.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onPreview, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) {
                            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (playing) "Stop preview" else "Preview")
                        }
                        OutlinedButton(onClick = onRemove, shape = RoundedCornerShape(13.dp)) {
                            Icon(Icons.Default.Delete, null)
                            Text("Remove")
                        }
                    }
                }
                if (hasMusic) {
                    HorizontalDivider(color = PanelAlt)
                    Text("Music fade-in", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("0 sec", color = Muted, fontSize = 10.sp)
                        Spacer(Modifier.weight(1f))
                        Text(String.format(Locale.US, "%.1f sec", musicFadeInMs / 1_000f), color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(value = musicFadeInMs.toFloat(), onValueChange = { onMusicFadeIn(it.toLong()) }, valueRange = 0f..5_000f)
                    Text("Music fade-out", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("0 sec", color = Muted, fontSize = 10.sp)
                        Spacer(Modifier.weight(1f))
                        Text(String.format(Locale.US, "%.1f sec", musicFadeOutMs / 1_000f), color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(value = musicFadeOutMs.toFloat(), onValueChange = { onMusicFadeOut(it.toLong()) }, valueRange = 0f..5_000f)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Lower music under voice-over", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Ducks the music track while a narration track is present.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
                        }
                        Switch(checked = duckMusicUnderVoiceover, onCheckedChange = onDuckMusic)
                    }
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, null, tint = Lime, modifier = Modifier.size(27.dp))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Voice-over recording", fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                voiceoverRecording -> "Recording now…"
                                hasVoiceover -> voiceoverName ?: "Voice-over ready"
                                else -> "Record narration directly on your phone"
                            },
                            color = if (voiceoverRecording) Lime else Muted,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (voiceoverRecording) {
                    Button(onClick = onStopVoiceover, modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7777), contentColor = Color.Black)) {
                        Icon(Icons.Default.Pause, null)
                        Spacer(Modifier.width(7.dp))
                        Text("Stop recording", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(onClick = onStartVoiceover, modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg)) {
                        Icon(Icons.Default.Mic, null)
                        Spacer(Modifier.width(7.dp))
                        Text(if (hasVoiceover) "Record replacement voice-over" else "Start voice-over recording", fontWeight = FontWeight.Bold)
                    }
                }
                if (hasVoiceover) {
                    OutlinedButton(onClick = onRemoveVoiceover, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Remove voice-over")
                    }
                }
                Text("Voice-over aligns to the start of the timeline and mixes with background music. Use headphones to avoid speaker audio leaking into the microphone.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Beat markers", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Tap while listening to mark cuts or sync points.", color = Muted, fontSize = 11.sp)
                    }
                    Text(time(timelinePositionMs), color = Lime, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Button(onClick = onMarkBeat, modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanelAlt, contentColor = Lime)) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Mark current beat")
                }
                OutlinedButton(
                    onClick = onDetectBeats,
                    enabled = hasMusic && !beatDetecting,
                    modifier = Modifier.fillMaxWidth().height(45.dp),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (beatDetecting) "Analyzing music…" else "Auto-detect beats in music")
                }
                if (beatMarkers.isEmpty()) {
                    Text("No markers yet · total timeline " + time(timelineDurationMs), color = Muted, fontSize = 11.sp)
                } else {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        beatMarkers.forEach { marker ->
                            Surface(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onDeleteBeat(marker) },
                                color = Color(0xFF28351E), shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(time(marker) + " ×", modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mute original video audio", fontWeight = FontWeight.Bold)
                    Text("Keep it off to retain speech and source sound during export.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                }
                Switch(checked = muted, onCheckedChange = onMute)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Track mixer", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                VolumeControl("Original video", sourceVolume, onSourceVolume)
                VolumeControl("Background music", musicVolume, onMusicVolume)
                VolumeControl("Voice-over", voiceoverVolume, onVoiceoverVolume)
                Text("Volumes are rendered independently into the final audio mix. Muting original audio overrides its volume setting.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        Text("Background music loops across the timeline when needed. Make sure you have permission to use music in published videos.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        if (message != null) MessageStrip(message)
    }
}

@Composable
private fun MotionPanel(
    selected: Clip?,
    titleText: String,
    brightness: Float,
    saturation: Float,
    filterPreset: String,
    message: String?,
    onClipChange: (Clip) -> Unit,
    onTitle: (String) -> Unit,
    onBrightness: (Float) -> Unit,
    onSaturation: (Float) -> Unit,
    onFilter: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Motion & effects", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Per-clip speed, interpolated start/end keyframes, transitions and colour controls.", color = Muted, fontSize = 13.sp)

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("MOTION KEYFRAMES", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                if (selected == null) {
                    Text("Import and select a clip to edit its keyframes.", color = Muted, fontSize = 13.sp)
                } else {
                    Text(selected.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Playback speed", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        Text(String.format(Locale.US, "%.2fx", selected.speed), color = Lime, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f).forEach { speed ->
                            val active = selected.speed == speed
                            Surface(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onClipChange(selected.copy(speed = speed)) },
                                color = if (active) Lime else PanelAlt,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    String.format(Locale.US, "%.2fx", speed),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                    color = if (active) Bg else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text("Speed curve", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("Constant", "Ramp up", "Ramp down", "Ease in/out", "Pulse").forEach { curve ->
                            val active = selected.speedCurve == curve
                            Surface(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onClipChange(selected.copy(speedCurve = curve)) },
                                color = if (active) Lime else PanelAlt,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(curve, modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                    color = if (active) Bg else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("Curves are rendered from short time slices and can take longer to export.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)

                    HorizontalDivider(color = PanelAlt)
                    Text("CLIP AUDIO KEYFRAMES", color = Lime, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.1.sp)
                    listOf(
                        "Start volume" to selected.audioVolumeStart,
                        "Middle volume" to selected.audioVolumeMid,
                        "End volume" to selected.audioVolumeEnd
                    ).forEachIndexed { keyIndex, pair ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(pair.first, color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            Text((pair.second * 100).toInt().toString() + "%", color = Lime, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Slider(
                            value = pair.second,
                            onValueChange = { value ->
                                onClipChange(when (keyIndex) {
                                    0 -> selected.copy(audioVolumeStart = value)
                                    1 -> selected.copy(audioVolumeMid = value)
                                    else -> selected.copy(audioVolumeEnd = value)
                                })
                            },
                            valueRange = 0f..1f
                        )
                    }
                    listOf("Audio fade-in" to selected.audioFadeInMs, "Audio fade-out" to selected.audioFadeOutMs)
                        .forEach { pair ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(pair.first, color = Muted, fontSize = 12.sp)
                                Spacer(Modifier.weight(1f))
                                Text(String.format(Locale.US, "%.1f sec", pair.second / 1_000f), color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = pair.second.toFloat(),
                                onValueChange = { value ->
                                    if (pair.first == "Audio fade-in") onClipChange(selected.copy(audioFadeInMs = value.toLong()))
                                    else onClipChange(selected.copy(audioFadeOutMs = value.toLong()))
                                },
                                valueRange = 0f..5_000f
                            )
                        }
                    Text("Audio volume is interpolated across start/middle/end points; fades apply to the selected clip's original audio.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)

                    HorizontalDivider(color = PanelAlt)
                    Text("CROP & REFRAME", color = Lime, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.1.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Crop zoom", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(String.format(Locale.US, "%.2fx", selected.cropZoom), color = Lime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = selected.cropZoom,
                        onValueChange = { onClipChange(selected.copy(cropZoom = it)) },
                        valueRange = 1f..2.5f
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Horizontal pan", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(selected.cropX.toInt().toString(), color = Lime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = selected.cropX,
                        onValueChange = { onClipChange(selected.copy(cropX = it)) },
                        valueRange = -1f..1f
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vertical pan", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(selected.cropY.toInt().toString(), color = Lime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = selected.cropY,
                        onValueChange = { onClipChange(selected.copy(cropY = it)) },
                        valueRange = -1f..1f
                    )
                    OutlinedButton(onClick = { onClipChange(selected.copy(cropZoom = 1f, cropX = 0f, cropY = 0f)) },
                        shape = RoundedCornerShape(11.dp)) { Text("Reset crop") }

                    HorizontalDivider(color = PanelAlt)
                    Text("START KEYFRAME  •  FRAME 0", color = Lime, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Zoom", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(String.format(Locale.US, "%.2fx", selected.zoomStart), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = selected.zoomStart,
                        onValueChange = { onClipChange(selected.copy(zoomStart = it)) },
                        valueRange = 1f..1.8f
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rotation", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(selected.rotationStart.toInt().toString() + "°", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = selected.rotationStart,
                        onValueChange = { onClipChange(selected.copy(rotationStart = it)) },
                        valueRange = -180f..180f
                    )

                    HorizontalDivider(color = PanelAlt)
                    Text("MIDDLE KEYFRAME  •  50%", color = Lime, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Zoom", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(String.format(Locale.US, "%.2fx", selected.zoomMid), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = selected.zoomMid,
                        onValueChange = { onClipChange(selected.copy(zoomMid = it)) },
                        valueRange = 1f..1.8f
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rotation", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(selected.rotationMid.toInt().toString() + "°", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = selected.rotationMid,
                        onValueChange = { onClipChange(selected.copy(rotationMid = it)) },
                        valueRange = -180f..180f
                    )
                    Text("Interpolation curve", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("Linear", "Ease in", "Ease out", "Ease in/out").forEach { curve ->
                            val active = selected.motionCurve == curve
                            Surface(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onClipChange(selected.copy(motionCurve = curve)) },
                                color = if (active) Lime else PanelAlt,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(curve, modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                    color = if (active) Bg else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = PanelAlt)
                    Text("END KEYFRAME  •  LAST FRAME", color = Lime, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Zoom", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(String.format(Locale.US, "%.2fx", selected.zoomEnd), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = selected.zoomEnd,
                        onValueChange = { onClipChange(selected.copy(zoomEnd = it)) },
                        valueRange = 1f..1.8f
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rotation", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(selected.rotationEnd.toInt().toString() + "°", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = selected.rotationEnd,
                        onValueChange = { onClipChange(selected.copy(rotationEnd = it)) },
                        valueRange = -180f..180f
                    )
                    Text("ClipForge interpolates zoom and rotation from the start keyframe to the end keyframe when exporting.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TRANSITION TO NEXT CLIP", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                if (selected == null) {
                    Text("Select a clip first.", color = Muted, fontSize = 12.sp)
                } else {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("Cut", "Crossfade", "Fade", "Flash", "Zoom").forEach { effect ->
                            val active = selected.transition == effect
                            Surface(
                                modifier = Modifier.clip(RoundedCornerShape(11.dp)).clickable { onClipChange(selected.copy(transition = effect)) },
                                color = if (active) Lime else PanelAlt,
                                shape = RoundedCornerShape(11.dp)
                            ) {
                                Text(effect, modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                                    color = if (active) Bg else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("Crossfade blends the next clip over the outgoing clip on a separate video track during export. Preview remains clip-based; Fade, Flash and Zoom are edge effects.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("TITLE OVERLAY", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                OutlinedTextField(
                    value = titleText, onValueChange = onTitle, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title / watermark") }, placeholder = { Text("Type text for the top of the video…") },
                    minLines = 2, maxLines = 4, shape = RoundedCornerShape(14.dp)
                )
                Text("Colour grade", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("None", "Warm", "Cinematic", "Mono", "Invert").forEach { preset ->
                        val active = filterPreset == preset
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(11.dp)).clickable { onFilter(preset) },
                            color = if (active) Lime else PanelAlt,
                            shape = RoundedCornerShape(11.dp)
                        ) {
                            Text(preset, modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                                color = if (active) Bg else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Brightness", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text((brightness * 100).toInt().toString() + "%", color = Lime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Slider(value = brightness, onValueChange = onBrightness, valueRange = -0.35f..0.35f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Saturation", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text((saturation + 100f).toInt().toString() + "%", color = Lime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Slider(value = saturation, onValueChange = onSaturation, valueRange = -100f..100f)
                if (message != null) MessageStrip(message)
            }
        }
    }
}

@Composable
private fun CaptionsPanel(
    captions: List<CaptionLine>,
    clips: List<Clip>,
    captioning: Boolean,
    message: String?,
    captionLanguageId: String,
    ttsGenerating: Boolean,
    captionStyle: String,
    captionPosition: String,
    captionScale: Float,
    onCaptionStyle: (String) -> Unit,
    onCaptionPosition: (String) -> Unit,
    onCaptionScale: (Float) -> Unit,
    onLanguageChange: (String) -> Unit,
    onAutoCaption: () -> Unit,
    onGenerateNarration: () -> Unit,
    onImportSrt: () -> Unit,
    onExportSrt: () -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Captions studio", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Generate timestamped subtitles offline in English or Hindi, edit the lines, or import/export SRT.", color = Muted, fontSize = 13.sp)
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Lime, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("On-device auto captions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            (if (captionLanguageId == "hindi") "Hindi" else "English") +
                                " offline speech model · bundled in the APK",
                            color = Muted, fontSize = 11.sp, lineHeight = 16.sp
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("english" to "English", "hindi" to "हिन्दी / Hindi").forEach { (id, label) ->
                        val active = captionLanguageId == id
                        Surface(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).clickable(enabled = !captioning) { onLanguageChange(id) },
                            color = if (active) Lime else PanelAlt,
                            shape = RoundedCornerShape(11.dp)
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                color = if (active) Bg else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                Text("CAPTION STYLE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Classic", "Yellow Pop", "Neon", "Minimal", "Karaoke").forEach { style ->
                        val active = captionStyle == style
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onCaptionStyle(style) },
                            color = if (active) Lime else PanelAlt, shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(style, modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                color = if (active) Bg else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("Caption placement", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Top", "Center", "Bottom").forEach { option ->
                        val active = captionPosition == option
                        Surface(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { onCaptionPosition(option) },
                            color = if (active) Lime else PanelAlt, shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(option, modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                                color = if (active) Bg else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Caption size", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${(captionScale * 100).toInt()}%", color = Lime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Slider(value = captionScale, onValueChange = onCaptionScale, valueRange = 0.7f..1.5f)
                Button(
                    onClick = onAutoCaption,
                    enabled = clips.isNotEmpty() && !captioning,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg)
                ) {
                    if (captioning) {
                        LinearProgressIndicator(modifier = Modifier.width(28.dp), color = Bg)
                        Spacer(Modifier.width(8.dp))
                        Text("Generating captions…", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate captions for all clips", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = onGenerateNarration,
                    enabled = captions.isNotEmpty() && !ttsGenerating,
                    modifier = Modifier.fillMaxWidth().height(49.dp),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Icon(Icons.Default.Mic, null)
                    Spacer(Modifier.width(7.dp))
                    Text(if (ttsGenerating) "Creating spoken narration…" else "Generate speech from captions")
                }
                Text(
                    "Uses Android's installed speech engine for the selected caption language. If the device has no voice data for that language, install it in Text-to-Speech settings first.",
                    color = Muted, fontSize = 11.sp, lineHeight = 16.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onImportSrt, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Import SRT")
                    }
                    OutlinedButton(onClick = onExportSrt, enabled = captions.isNotEmpty(), modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) {
                        Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Export SRT")
                    }
                }
                Text(
                    "The first run downloads the selected speech model (about 40–45 MB). Speech is decoded and transcribed locally; no video is uploaded to a caption server. Model availability and recognition accuracy vary by language and audio quality.",
                    color = Muted, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SUBTITLE LINES", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
            Spacer(Modifier.weight(1f))
            Text(captions.size.toString() + " lines", color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (captions.isEmpty()) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("No subtitle lines yet", fontWeight = FontWeight.Bold)
                    Text("Tap Generate captions or import an .srt file. You can edit each line before export.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        } else {
            captions.forEachIndexed { index, line ->
                val clipName = clips.firstOrNull { it.id == line.clipId }?.name ?: "Removed clip"
                Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text((index + 1).toString().padStart(2, '0'), color = Lime, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(time(line.startMs) + " – " + time(line.endMs), color = Muted, fontSize = 11.sp)
                            Spacer(Modifier.weight(1f))
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete caption",
                                tint = Color(0xFFFF8D8D),
                                modifier = Modifier.size(19.dp).clickable { onDelete(line.id) }
                            )
                        }
                        OutlinedTextField(
                            value = line.text,
                            onValueChange = { onEdit(line.id, it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(clipName) },
                            minLines = 1,
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }
        if (message != null) MessageStrip(message)
    }
}

private fun textLayerPlaceholderUri(context: Context): Uri {
    val file = File(context.filesDir, "clipforge_text_layer.png")
    if (!file.exists()) {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { output ->
                if (!bitmap.compress(CompressFormat.PNG, 100, output)) {
                    throw IllegalStateException("Could not initialize the text-layer placeholder.")
                }
            }
        } finally {
            bitmap.recycle()
        }
    }
    return Uri.fromFile(file)
}

private fun styledCaptionText(text: String, style: String): SpannableString {
    return SpannableString(text.trim()).apply {
        if (length > 0) {
            val foreground = when (style) {
                "Yellow Pop" -> AndroidColor.BLACK
                "Neon" -> AndroidColor.rgb(80, 240, 255)
                else -> AndroidColor.WHITE
            }
            setSpan(ForegroundColorSpan(foreground), 0, length, 0)
            setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
            when (style) {
                "Yellow Pop" -> setSpan(BackgroundColorSpan(AndroidColor.rgb(255, 221, 65)), 0, length, 0)
                "Neon" -> setSpan(BackgroundColorSpan(AndroidColor.argb(225, 8, 22, 48)), 0, length, 0)
                "Classic" -> setSpan(BackgroundColorSpan(AndroidColor.argb(210, 0, 0, 0)), 0, length, 0)
                "Minimal" -> Unit
            }
        }
    }
}


private fun captionAnnotatedString(caption: CaptionLine, style: String, timeMs: Long): AnnotatedString {
    return buildAnnotatedString {
        append(caption.text)
        if (style == "Karaoke") {
            var searchFrom = 0
            caption.tokens.forEach { token ->
                val wordStart = caption.text.indexOf(token.text, searchFrom)
                if (wordStart >= 0) {
                    val wordEnd = (wordStart + token.text.length).coerceAtMost(caption.text.length)
                    val active = timeMs >= token.startMs && timeMs < token.endMs
                    addStyle(
                        SpanStyle(
                            color = if (active) Color.Black else Color.White,
                            background = if (active) Color(0xFFFFDD41) else Color.Transparent,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        wordStart,
                        wordEnd
                    )
                    searchFrom = wordEnd
                }
            }
        }
    }
}

private fun styledCaptionTextAtTime(caption: CaptionLine, style: String, timeMs: Long): SpannableString {
    val output = styledCaptionText(caption.text, if (style == "Karaoke") "Classic" else style)
    if (style != "Karaoke" || output.isEmpty()) return output
    var searchFrom = 0
    caption.tokens.forEach { token ->
        val wordStart = output.toString().indexOf(token.text, searchFrom)
        if (wordStart >= 0) {
            val wordEnd = (wordStart + token.text.length).coerceAtMost(output.length)
            if (timeMs >= token.startMs && timeMs < token.endMs) {
                output.setSpan(ForegroundColorSpan(AndroidColor.BLACK), wordStart, wordEnd, 0)
                output.setSpan(BackgroundColorSpan(AndroidColor.rgb(255, 221, 65)), wordStart, wordEnd, 0)
            }
            searchFrom = wordEnd
        }
    }
    return output
}

private fun captionAnchorY(position: String): Float = when (position) {
    "Top" -> 0.78f
    "Center" -> 0f
    else -> -0.78f
}

private fun captionComposeTextColor(style: String): Color = when (style) {
    "Yellow Pop" -> Color.Black
    "Neon" -> Color(0xFF50F0FF)
    else -> Color.White
}

private fun captionComposeBackground(style: String): Color = when (style) {
    "Yellow Pop" -> Color(0xFFFFDD41)
    "Neon" -> Color(0xE1081630)
    "Classic", "Karaoke" -> Color.Black.copy(alpha = 0.82f)
    else -> Color.Transparent
}

private fun exportBitrates(format: String, quality: String): Pair<Int, Int> {
    val scale = when {
        format.contains("4K") -> 2.1f
        format.contains("FHD") || format == "16:9 Video" -> 1f
        format == "1:1 Square" -> 0.8f
        else -> 0.65f
    }
    val videoBase = when (quality) {
        "Saver" -> 3_000_000
        "Standard" -> 6_000_000
        "Ultra" -> 20_000_000
        else -> 12_000_000
    }
    val audio = when (quality) {
        "Saver" -> 96_000
        "Standard" -> 128_000
        "Ultra" -> 256_000
        else -> 192_000
    }
    return (videoBase * scale).toInt() to audio
}

private fun qualityDescription(format: String, quality: String): String {
    val (video, audio) = exportBitrates(format, quality)
    return "Requested video bitrate ${video / 1_000_000f} Mbps · audio ${audio / 1_000} kbps. Encoders may adjust these values for compatibility."
}

@Composable
private fun ExportPanel(
    clips: List<Clip>, musicName: String?, titleText: String, brightness: Float, format: String,
    exporting: Boolean, hasOutput: Boolean, message: String?,
    onFormat: (String) -> Unit, onExport: () -> Unit, onCancel: () -> Unit, onShare: () -> Unit,
    quality: String, frameRate: Int, exportProgress: Int, freeStorageBytes: Long,
    onQuality: (String) -> Unit, onFrameRate: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Export video", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Render your edited timeline to an MP4 on this phone.", color = Muted, fontSize = 13.sp)
        Text("Free storage: " + (freeStorageBytes / (1024L * 1024L)) + " MB · keep extra room for the temporary render and Gallery copy.", color = Muted, fontSize = 11.sp)
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("PROJECT SUMMARY", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Summary("Clips", clips.size.toString())
                Summary("Duration", time(clips.sumOf { it.durationMs }))
                Summary("Music", musicName ?: "Original audio only")
                Summary("Text", if (titleText.isBlank()) "None" else titleText.take(35))
                Summary("Brightness", (brightness * 100).toInt().toString() + "%")
                HorizontalDivider(color = PanelAlt)
                Text("Canvas format", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Original", "9:16 Shorts", "9:16 FHD", "9:16 4K", "16:9 Video", "16:9 FHD", "16:9 4K", "1:1 Square").forEach { option ->
                        val selected = format == option
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(11.dp)).clickable { onFormat(option) },
                            color = if (selected) Lime else PanelAlt, shape = RoundedCornerShape(11.dp)
                        ) {
                            Text(option, modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                color = if (selected) Bg else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                HorizontalDivider(color = PanelAlt)
                Text("Quality profile", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Saver", "Standard", "High", "Ultra").forEach { option ->
                        val active = quality == option
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(11.dp)).clickable { onQuality(option) },
                            color = if (active) Lime else PanelAlt, shape = RoundedCornerShape(11.dp)
                        ) {
                            Text(option, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                color = if (active) Bg else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    qualityDescription(format, quality),
                    color = Muted, fontSize = 11.sp, lineHeight = 16.sp
                )
                Text("Output frame-rate cap", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(24, 30, 60).forEach { fps ->
                        val active = frameRate == fps
                        Surface(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).clickable { onFrameRate(fps) },
                            color = if (active) Lime else PanelAlt, shape = RoundedCornerShape(11.dp)
                        ) {
                            Text("$fps FPS", modifier = Modifier.padding(horizontal = 7.dp, vertical = 10.dp),
                                color = if (active) Bg else Color.White, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
                Text("Higher resolution/quality can make exports much larger and slower. The phone's hardware encoder may lower unsupported output settings. FPS caps drop frames; they do not create optical-flow/interpolated frames.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                Text("MP4 · H.264 video · AAC audio", color = Muted, fontSize = 12.sp)
                if (exporting) {
                    LinearProgressIndicator(progress = exportProgress.coerceIn(0, 100) / 100f, modifier = Modifier.fillMaxWidth(), color = Lime)
                    Text("Rendering on device · " + exportProgress.coerceIn(0, 100) + "%. Large videos can take several minutes.", color = Muted, fontSize = 12.sp)
                    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel export") }
                } else {
                    Button(
                        onClick = onExport, enabled = clips.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg)
                    ) {
                        Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Render MP4", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (hasOutput) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Lime)
                        Spacer(Modifier.width(8.dp))
                        Text("Saved to Gallery", color = Lime, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share video")
                    }
                }
            }
        }
        if (message != null) MessageStrip(message)
        Text("Exports use the device's supported hardware media codecs. For unsupported files, try a standard H.264 MP4.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun Summary(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.width(100.dp))
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private fun snapTrimToBeat(clip: Clip, editedSourceMs: Long, clipOffsetMs: Long, beatMarkers: List<Long>): Long {
    if (beatMarkers.isEmpty()) return editedSourceMs
    val targetTimelineMs = clipOffsetMs + ((editedSourceMs - clip.startMs) / clip.speed).toLong()
    val nearest = beatMarkers.minByOrNull { kotlin.math.abs(it - targetTimelineMs) } ?: return editedSourceMs
    if (kotlin.math.abs(nearest - targetTimelineMs) > 320L) return editedSourceMs
    return (clip.startMs + ((nearest - clipOffsetMs) * clip.speed).toLong())
        .coerceIn(0L, clip.sourceMs)
}

@Composable
private fun WaveformBars(peaks: List<Float>, modifier: Modifier = Modifier) {
    if (peaks.isEmpty()) return
    Canvas(modifier) {
        val slot = size.width / peaks.size.toFloat()
        val stroke = (slot * 0.62f).coerceAtLeast(1.2f)
        peaks.forEachIndexed { index, peak ->
            val amplitude = (peak.coerceIn(0f, 1f) * size.height * 0.9f).coerceAtLeast(2f)
            val x = slot * index + slot / 2f
            drawLine(
                color = Lime,
                start = Offset(x, size.height / 2f - amplitude / 2f),
                end = Offset(x, size.height / 2f + amplitude / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun effectiveLayerOpacity(layer: PhotoLayer, timelinePositionMs: Long): Float {
    if (!layer.animateOpacity) return layer.opacity.coerceIn(0f, 1f)
    val duration = (layer.endMs - layer.startMs).coerceAtLeast(1L)
    val fraction = ((timelinePositionMs - layer.startMs).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    return (layer.opacity * interpolateThree(layer.opacityStart, layer.opacityMid, layer.opacityEnd, fraction, "Linear"))
        .coerceIn(0f, 1f)
}

private fun layerAnchors(position: String): Pair<Float, Float> = when (position) {
    "top-left" -> -0.62f to 0.62f
    "top-right" -> 0.62f to 0.62f
    "center" -> 0f to 0f
    "bottom-left" -> -0.62f to -0.62f
    else -> 0.62f to -0.62f
}

@Composable
private fun LayersPanel(
    layers: List<PhotoLayer>,
    timelineDurationMs: Long,
    message: String?,
    onAdd: () -> Unit,
    onAddText: () -> Unit,
    onUpdate: (PhotoLayer) -> Unit,
    onDelete: (String) -> Unit,
    onRemoveBackground: (PhotoLayer) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Layers & overlays", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Add image, logo, sticker, or watermark layers on top of your video timeline. Person cutout runs on-device for photo layers.", color = Muted, fontSize = 13.sp)
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Add images / logos", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onAddText,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Edit, null)
            Spacer(Modifier.width(8.dp))
            Text("Add text sticker")
        }
        Text("Timeline length: " + time(timelineDurationMs), color = Muted, fontSize = 11.sp)
        if (layers.isEmpty()) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Your overlay track is empty", fontWeight = FontWeight.Bold)
                    Text("Choose a transparent PNG logo, a sticker, or a picture. Each layer has its own in/out time, position, scale, opacity, and rotation.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        } else {
            layers.forEachIndexed { index, layer ->
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, null, tint = Lime, modifier = Modifier.size(25.dp))
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    (if (layer.fullFrame) "FREEZE FRAME " else if (layer.textContent != null) "TEXT STICKER " else "OVERLAY ") + (index + 1),
                                    color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
                                )
                                Text(layer.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove overlay",
                                tint = Color(0xFFFF8D8D),
                                modifier = Modifier.size(20.dp).clickable { onDelete(layer.id) }
                            )
                        }
                        if (layer.textContent != null) {
                            OutlinedTextField(
                                value = layer.textContent,
                                onValueChange = { onUpdate(layer.copy(textContent = it)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Text sticker wording") },
                                placeholder = { Text("Type overlay text…") },
                                minLines = 1,
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Text("TEXT STYLE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Classic", "Yellow Pop", "Neon", "Minimal").forEach { style ->
                                    val active = layer.textStyle == style
                                    Surface(
                                        modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable { onUpdate(layer.copy(textStyle = style)) },
                                        color = if (active) Lime else PanelAlt,
                                        shape = RoundedCornerShape(9.dp)
                                    ) {
                                        Text(style, modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                                            color = if (active) Bg else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        if (layer.textContent == null && !layer.fullFrame) {
                            OutlinedButton(
                                onClick = { onRemoveBackground(layer) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(11.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(7.dp))
                                Text("AI person cutout")
                            }
                            Text("Best for portraits and people. This creates a transparent PNG; it does not remove backgrounds from moving video.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("IN", color = Muted, fontSize = 11.sp)
                            Spacer(Modifier.weight(1f))
                            Text(time(layer.startMs), color = Lime, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Slider(
                            value = layer.startMs.toFloat().coerceIn(0f, timelineDurationMs.toFloat()),
                            onValueChange = { value ->
                                onUpdate(layer.copy(startMs = value.toLong().coerceIn(0L, (layer.endMs - 100L).coerceAtLeast(0L))))
                            },
                            valueRange = 0f..timelineDurationMs.toFloat().coerceAtLeast(1_000f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("OUT", color = Muted, fontSize = 11.sp)
                            Spacer(Modifier.weight(1f))
                            Text(time(layer.endMs), color = Lime, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Slider(
                            value = layer.endMs.toFloat().coerceIn(0f, timelineDurationMs.toFloat()),
                            onValueChange = { value ->
                                onUpdate(layer.copy(endMs = value.toLong().coerceIn((layer.startMs + 100L).coerceAtMost(timelineDurationMs), timelineDurationMs)))
                            },
                            valueRange = 0f..timelineDurationMs.toFloat().coerceAtLeast(1_000f)
                        )
                        Text("POSITION", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("top-left", "top-right", "center", "bottom-left", "bottom-right").forEach { preset ->
                                val active = layer.position == preset
                                Surface(
                                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable { onUpdate(layer.copy(position = preset)) },
                                    color = if (active) Lime else PanelAlt,
                                    shape = RoundedCornerShape(9.dp)
                                ) {
                                    Text(preset.replace("-", " "), modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                                        color = if (active) Bg else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Size", color = Muted, fontSize = 11.sp)
                            Spacer(Modifier.weight(1f))
                            Text((layer.scale * 100).toInt().toString() + "%", color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(value = layer.scale, onValueChange = { onUpdate(layer.copy(scale = it)) }, valueRange = 0.12f..0.70f)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Opacity", color = Muted, fontSize = 11.sp)
                            Spacer(Modifier.weight(1f))
                            Text((layer.opacity * 100).toInt().toString() + "%", color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(value = layer.opacity, onValueChange = { onUpdate(layer.copy(opacity = it)) }, valueRange = 0f..1f)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Animate opacity", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Fade this layer through three keyframes.", color = Muted, fontSize = 10.sp)
                            }
                            Switch(checked = layer.animateOpacity, onCheckedChange = { onUpdate(layer.copy(animateOpacity = it)) })
                        }
                        if (layer.animateOpacity) {
                            listOf(
                                "Opacity · start" to layer.opacityStart,
                                "Opacity · middle" to layer.opacityMid,
                                "Opacity · end" to layer.opacityEnd
                            ).forEach { (label, value) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(label, color = Muted, fontSize = 11.sp)
                                    Spacer(Modifier.weight(1f))
                                    Text((value * 100).toInt().toString() + "%", color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = value,
                                    onValueChange = { next ->
                                        onUpdate(when (label) {
                                            "Opacity · start" -> layer.copy(opacityStart = next)
                                            "Opacity · middle" -> layer.copy(opacityMid = next)
                                            else -> layer.copy(opacityEnd = next)
                                        })
                                    },
                                    valueRange = 0f..1f
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Rotation", color = Muted, fontSize = 11.sp)
                            Spacer(Modifier.weight(1f))
                            Text(layer.rotation.toInt().toString() + "°", color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(value = layer.rotation, onValueChange = { onUpdate(layer.copy(rotation = it)) }, valueRange = -180f..180f)
                    }
                }
            }
        }
        if (message != null) MessageStrip(message)
    }
}

@Composable
private fun TimelineThumbnail(clip: Clip) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = clip.uri, key2 = clip.startMs) {
        value = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, clip.uri)
                retriever.getFrameAtTime(clip.startMs.coerceAtLeast(0L) * 1_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (_: Exception) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        }
    }
    DisposableEffect(bitmap) {
        val frame = bitmap
        onDispose { runCatching { frame?.recycle() } }
    }
    Box(
        Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(8.dp)).background(PanelAlt),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Video thumbnail for ${clip.name}",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Default.Movie, null, tint = Muted, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun TimelineClipCard(clip: Clip, index: Int, selected: Boolean, widthScale: Float = 1f, onClick: () -> Unit) {
    val cardBackground by animateColorAsState(
        targetValue = if (selected) Color(0xFF263223) else Panel,
        animationSpec = tween(160),
        label = "timeline-card-background"
    )
    val lineColor by animateColorAsState(
        targetValue = if (selected) Lime else PanelAlt,
        animationSpec = tween(160),
        label = "timeline-card-selection"
    )
    Column(
        Modifier.width((122f * widthScale.coerceIn(0.75f, 1.8f)).dp).clip(RoundedCornerShape(12.dp)).background(cardBackground)
            .clickable(onClick = onClick).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("CLIP " + (index + 1), color = if (selected) Lime else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (selected) {
                Icon(Icons.Default.CheckCircle, null, tint = Lime, modifier = Modifier.size(13.dp))
            }
        }
        TimelineThumbnail(clip)
        Text(clip.name, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
        Text(time(clip.durationMs), color = Muted, fontSize = 10.sp)
        Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(lineColor))
    }
}

@Composable
private fun PhotoLayerPreview(layers: List<PhotoLayer>, timelinePositionMs: Long) {
    val active = layers.filter { timelinePositionMs >= it.startMs && timelinePositionMs < it.endMs }
    Box(Modifier.fillMaxSize()) {
        active.forEach { layer ->
            val animatedOpacity = effectiveLayerOpacity(layer, timelinePositionMs)
            val alignment = when (layer.position) {
                "top-left" -> Alignment.TopStart
                "top-right" -> Alignment.TopEnd
                "center" -> Alignment.Center
                "bottom-left" -> Alignment.BottomStart
                else -> Alignment.BottomEnd
            }
            if (!layer.textContent.isNullOrBlank()) {
                Text(
                    text = layer.textContent,
                    color = captionComposeTextColor(layer.textStyle),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (34f * layer.scale).sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(alignment).padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(captionComposeBackground(layer.textStyle))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .graphicsLayer(rotationZ = layer.rotation, alpha = animatedOpacity)
                )
            } else {
                val context = LocalContext.current
                val bitmap by produceState<Bitmap?>(initialValue = null, key1 = layer.uri) {
                    value = withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openInputStream(layer.uri)?.use { BitmapFactory.decodeStream(it) }
                        }.getOrNull()
                    }
                }
                if (bitmap != null) {
                    if (layer.fullFrame) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = layer.name,
                            alpha = animatedOpacity,
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().graphicsLayer(rotationZ = layer.rotation)
                        )
                    } else {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = layer.name,
                            alpha = animatedOpacity,
                            modifier = Modifier.align(alignment).padding(8.dp)
                                .size((230f * layer.scale).dp)
                                .graphicsLayer(rotationZ = layer.rotation)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationBar(tab: Int, select: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(15.dp)).background(Panel).padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem("Edit", Icons.Default.Movie, tab == 0, Modifier.weight(1f)) { select(0) }
        NavItem("Audio", Icons.Default.LibraryMusic, tab == 1, Modifier.weight(1f)) { select(1) }
        NavItem("Motion", Icons.Default.Tune, tab == 2, Modifier.weight(1f)) { select(2) }
        NavItem("Captions", Icons.Default.Subtitles, tab == 3, Modifier.weight(1f)) { select(3) }
        NavItem("Layers", Icons.Default.Layers, tab == 4, Modifier.weight(1f)) { select(4) }
        NavItem("Export", Icons.Default.Download, tab == 5, Modifier.weight(1f)) { select(5) }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val itemBackground by animateColorAsState(
        targetValue = if (active) PanelAlt else Color.Transparent,
        animationSpec = tween(180),
        label = "nav-background"
    )
    val iconColor by animateColorAsState(
        targetValue = if (active) Lime else Muted,
        animationSpec = tween(180),
        label = "nav-icon"
    )
    val textColor by animateColorAsState(
        targetValue = if (active) Color.White else Muted,
        animationSpec = tween(180),
        label = "nav-text"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (active) 1.04f else 1f,
        animationSpec = tween(180),
        label = "nav-scale"
    )
    Column(
        modifier.clip(RoundedCornerShape(11.dp)).background(itemBackground)
            .clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(19.dp).graphicsLayer(scaleX = iconScale, scaleY = iconScale)
        )
        Text(
            label,
            color = textColor,
            fontSize = 9.sp,
            maxLines = 1,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ActionButton(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit,
    modifier: Modifier, enabled: Boolean
) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = modifier.height(43.dp), shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun MessageStrip(message: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PanelAlt).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = Lime, modifier = Modifier.size(16.dp))
        Text(message, color = Color(0xFFE0E4EA), fontSize = 11.sp, lineHeight = 16.sp)
    }
}

private fun interpolateThree(start: Float, middle: Float, end: Float, fraction: Float, curve: String): Float {
    val clamped = fraction.coerceIn(0f, 1f)
    val firstHalf = clamped < 0.5f
    val from = if (firstHalf) start else middle
    val to = if (firstHalf) middle else end
    val raw = if (firstHalf) clamped * 2f else (clamped - 0.5f) * 2f
    val eased = when (curve) {
        "Ease in" -> raw * raw
        "Ease out" -> 1f - (1f - raw) * (1f - raw)
        "Ease in/out" -> raw * raw * (3f - 2f * raw)
        else -> raw
    }
    return from + (to - from) * eased
}

private fun time(ms: Long): String {
    val sec = ms.coerceAtLeast(0L) / 1000L
    return String.format(Locale.US, "%02d:%02d", sec / 60L, sec % 60L)
}

private fun nameForUri(context: Context, uri: Uri): String {
    val name = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }.getOrNull()
    return name?.takeIf { it.isNotBlank() } ?: (uri.lastPathSegment ?: "Media file")
}

private fun makeCaptionLines(clipId: String, words: List<WordTiming>, durationMs: Long): List<CaptionLine> {
    val ordered = words.sortedBy { it.startMs }
    val output = mutableListOf<CaptionLine>()
    val group = mutableListOf<WordTiming>()
    fun flush() {
        if (group.isEmpty()) return
        val start = group.first().startMs.coerceIn(0L, durationMs.coerceAtLeast(1L) - 1L)
        val end = group.last().endMs.coerceIn(start + 1L, durationMs.coerceAtLeast(start + 1L))
        val text = group.joinToString(" ") { it.text }.trim()
        val tokens = group.mapNotNull { word ->
            val tokenStart = word.startMs.coerceIn(start, (end - 1L).coerceAtLeast(start))
            val tokenEnd = word.endMs.coerceIn(tokenStart + 1L, end)
            if (tokenEnd > tokenStart) CaptionToken(word.text, tokenStart, tokenEnd) else null
        }
        if (text.isNotBlank()) output.add(CaptionLine(clipId = clipId, startMs = start, endMs = end, text = text, tokens = tokens))
        group.clear()
    }
    ordered.forEach { word ->
        val punctuationEnd = group.lastOrNull()?.text?.endsWith(".") == true ||
            group.lastOrNull()?.text?.endsWith("?") == true ||
            group.lastOrNull()?.text?.endsWith("!") == true
        if (group.isNotEmpty() && (group.size >= 6 || word.endMs - group.first().startMs >= 2_600L || punctuationEnd)) flush()
        group.add(word)
        if (word.text.endsWith(".") || word.text.endsWith("?") || word.text.endsWith("!")) flush()
    }
    flush()
    return output
}

private fun buildEditedItems(
    context: Context,
    clips: List<Clip>,
    captions: List<CaptionLine>,
    photoLayers: List<PhotoLayer>,
    titleText: String,
    brightness: Float,
    saturation: Float,
    filterPreset: String,
    format: String,
    sourceVolume: Float,
    outputFrameRate: Int,
    captionStyle: String,
    captionPosition: String,
    captionScale: Float,
    projectOffsetStartMs: Long = 0L,
    previousTransition: String? = null,
    nextTransition: String? = null
): List<EditedMediaItem> {
    val result = mutableListOf<EditedMediaItem>()
    clips.forEachIndexed { clipIndex, clip ->
        val duration = clip.durationMs.coerceAtLeast(1L)
        val projectOffsetMs = projectOffsetStartMs + clips.take(clipIndex).sumOf { renderedDurationEstimateMs(it) }
        val projectClipEndMs = projectOffsetMs + (duration / clip.speed).toLong()
        val activeCaptions = captions.filter { it.clipId == clip.id && it.endMs > 0L && it.startMs < duration }
        val activePhotoLayers = photoLayers.filter { it.endMs > projectOffsetMs && it.startMs < projectClipEndMs }
        val transitionIn = previousTransition ?: clips.getOrNull(clipIndex - 1)?.transition ?: "Cut"
        val transitionOut = nextTransition ?: if (clipIndex < clips.lastIndex) clip.transition else "Cut"
        val transitionLength = minOf(400L, (duration / 3L).coerceAtLeast(0L))
        val boundaries = sortedSetOf(0L, duration)

        // These points create visible intermediate keyframes during export, rather than a UI-only animation.
        for (step in 1..12) boundaries.add(duration * step / 13L)
        activeCaptions.forEach { caption ->
            boundaries.add(caption.startMs.coerceIn(0L, duration))
            boundaries.add(caption.endMs.coerceIn(0L, duration))
            caption.tokens.forEach { token ->
                boundaries.add(token.startMs.coerceIn(0L, duration))
                boundaries.add(token.endMs.coerceIn(0L, duration))
            }
        }
        boundaries.add((clip.audioFadeInMs * clip.speed).toLong().coerceIn(0L, duration))
        boundaries.add((duration - (clip.audioFadeOutMs * clip.speed).toLong()).coerceIn(0L, duration))
        boundaries.add(duration / 3L)
        boundaries.add(duration * 2L / 3L)
        activePhotoLayers.forEach { layer ->
            val layerStartInSource = ((layer.startMs - projectOffsetMs).coerceAtLeast(0L) * clip.speed).toLong()
            val layerEndInSource = ((layer.endMs - projectOffsetMs).coerceAtLeast(0L) * clip.speed).toLong()
            boundaries.add(layerStartInSource.coerceIn(0L, duration))
            boundaries.add(layerEndInSource.coerceIn(0L, duration))
            val layerDuration = (layer.endMs - layer.startMs).coerceAtLeast(1L)
            val opacityKeyOne = layer.startMs + layerDuration / 3L
            val opacityKeyTwo = layer.startMs + layerDuration * 2L / 3L
            boundaries.add(((opacityKeyOne - projectOffsetMs).coerceAtLeast(0L) * clip.speed).toLong().coerceIn(0L, duration))
            boundaries.add(((opacityKeyTwo - projectOffsetMs).coerceAtLeast(0L) * clip.speed).toLong().coerceIn(0L, duration))
        }
        if (transitionLength > 0L) {
            for (step in 1..5) {
                val inPoint = transitionLength * step / 5L
                val outPoint = duration - transitionLength + transitionLength * step / 5L
                if (transitionIn != "Cut") boundaries.add(inPoint.coerceIn(0L, duration))
                if (transitionOut != "Cut") boundaries.add(outPoint.coerceIn(0L, duration))
            }
        }
        val points = boundaries.toList()
        for (pointIndex in 0 until points.lastIndex) {
            val relativeStart = points[pointIndex]
            val relativeEnd = points[pointIndex + 1]
            if (relativeEnd <= relativeStart) continue
            val mid = (relativeStart + relativeEnd) / 2f
            val fraction = (mid / duration.toFloat()).coerceIn(0f, 1f)
            val speedFactor = when (clip.speedCurve) {
                "Ramp up" -> 0.5f + 1.5f * fraction
                "Ramp down" -> 2.0f - 1.5f * fraction
                "Ease in/out" -> 0.5f + 1.5f * kotlin.math.sin(Math.PI.toFloat() * fraction)
                "Pulse" -> (1.0f + 0.65f * kotlin.math.sin(fraction * (Math.PI.toFloat() * 4f))).coerceAtLeast(0.25f)
                else -> 1f
            }
            val segmentSpeed = (clip.speed * speedFactor).coerceIn(0.25f, 3f)
            val speedProvider = object : SpeedProvider {
                override fun getSpeed(timeUs: Long): Float = segmentSpeed
                override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
            }
            val keyZoom = interpolateThree(clip.zoomStart, clip.zoomMid, clip.zoomEnd, fraction, clip.motionCurve)
            val keyRotation = interpolateThree(clip.rotationStart, clip.rotationMid, clip.rotationEnd, fraction, clip.motionCurve)
            val inRatio = if (transitionLength > 0L && transitionIn != "Cut" && mid < transitionLength) {
                (1f - mid / transitionLength.toFloat()).coerceIn(0f, 1f)
            } else 0f
            val outRatio = if (transitionLength > 0L && transitionOut != "Cut" && mid > duration - transitionLength) {
                (1f - (duration - mid) / transitionLength.toFloat()).coerceIn(0f, 1f)
            } else 0f

            var transitionBrightness = 0f
            var transitionScale = 1f
            fun applyTransition(name: String, ratio: Float) {
                when (name) {
                    "Fade" -> transitionBrightness += -0.95f * ratio
                    "Flash" -> transitionBrightness += 0.85f * ratio
                    "Zoom" -> transitionScale *= 1f + 0.16f * ratio
                }
            }
            applyTransition(transitionIn, inRatio)
            applyTransition(transitionOut, outRatio)

            val effects = mutableListOf<androidx.media3.common.Effect>()
            val cropHalf = 1f / clip.cropZoom.coerceIn(1f, 2.5f)
            val cropCenterX = clip.cropX.coerceIn(-1f, 1f) * (1f - cropHalf)
            val cropCenterY = clip.cropY.coerceIn(-1f, 1f) * (1f - cropHalf)
            if (clip.cropZoom > 1f || clip.cropX != 0f || clip.cropY != 0f) {
                effects.add(Crop(cropCenterX - cropHalf, cropCenterX + cropHalf, cropCenterY - cropHalf, cropCenterY + cropHalf))
            }
            val adjustedBrightness = (brightness + transitionBrightness).coerceIn(-1f, 1f)
            if (adjustedBrightness != 0f) effects.add(Brightness(adjustedBrightness))
            val scale = (keyZoom * transitionScale).coerceIn(0.75f, 2.2f)
            if (scale != 1f || keyRotation != 0f) {
                effects.add(ScaleAndRotateTransformation.Builder().setScale(scale, scale).setRotationDegrees(keyRotation).build())
            }
            if (saturation != 0f || filterPreset == "Warm" || filterPreset == "Cinematic") {
                val saturationAdjustment = (saturation + if (filterPreset == "Warm") 12f else if (filterPreset == "Cinematic") -8f else 0f).coerceIn(-100f, 100f)
                val hue = if (filterPreset == "Warm") 12f else if (filterPreset == "Cinematic") -4f else 0f
                val lightness = if (filterPreset == "Warm") 2f else if (filterPreset == "Cinematic") -3f else 0f
                effects.add(HslAdjustment.Builder()
                    .adjustHue(hue)
                    .adjustLightness(lightness)
                    .adjustSaturation(saturationAdjustment)
                    .build())
            }
            when (filterPreset) {
                "Mono" -> effects.add(RgbFilter.createGrayscaleFilter())
                "Invert" -> effects.add(RgbFilter.createInvertedFilter())
            }

            val overlays = mutableListOf<androidx.media3.effect.TextureOverlay>()
            val projectMidpointMs = projectOffsetMs + (mid / clip.speed).toLong()
            activePhotoLayers.filter { projectMidpointMs >= it.startMs && projectMidpointMs < it.endMs }.forEach { layer ->
                val (anchorX, anchorY) = layerAnchors(layer.position)
                val layerSettings = StaticOverlaySettings.Builder()
                    .setBackgroundFrameAnchor(anchorX, anchorY)
                    .setOverlayFrameAnchor(0f, 0f)
                    .setScale(if (layer.fullFrame) 1f else layer.scale.coerceIn(0.12f, 0.7f), if (layer.fullFrame) 1f else layer.scale.coerceIn(0.12f, 0.7f))
                    .setAlphaScale(effectiveLayerOpacity(layer, projectMidpointMs))
                    .setRotationDegrees(layer.rotation.coerceIn(-180f, 180f))
                    .build()
                if (!layer.textContent.isNullOrBlank()) {
                    overlays.add(TextOverlay.createStaticTextOverlay(styledCaptionText(layer.textContent, layer.textStyle), layerSettings))
                } else {
                    overlays.add(BitmapOverlay.createStaticBitmapOverlay(context, layer.uri, layerSettings))
                }
            }
            if (titleText.isNotBlank()) {
                val styledTitle = SpannableString(titleText.trim()).apply {
                    setSpan(ForegroundColorSpan(AndroidColor.WHITE), 0, length, 0)
                    setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
                    setSpan(BackgroundColorSpan(AndroidColor.argb(190, 0, 0, 0)), 0, length, 0)
                }
                val titleSettings = StaticOverlaySettings.Builder()
                    .setBackgroundFrameAnchor(0f, 0.78f)
                    .setOverlayFrameAnchor(0f, 0f)
                    .build()
                overlays.add(TextOverlay.createStaticTextOverlay(styledTitle, titleSettings))
            }
            val activeCaption = activeCaptions.firstOrNull { mid >= it.startMs && mid < it.endMs }
            if (activeCaption != null && activeCaption.text.isNotBlank()) {
                val styledCaption = styledCaptionTextAtTime(activeCaption, captionStyle, mid.toLong())
                val captionSettings = StaticOverlaySettings.Builder()
                    .setBackgroundFrameAnchor(0f, captionAnchorY(captionPosition))
                    .setOverlayFrameAnchor(0f, 0f)
                    .setScale(captionScale.coerceIn(0.7f, 1.5f), captionScale.coerceIn(0.7f, 1.5f))
                    .build()
                overlays.add(TextOverlay.createStaticTextOverlay(styledCaption, captionSettings))
            }
            if (overlays.isNotEmpty()) effects.add(OverlayEffect(overlays))

            when (format) {
                "9:16 Shorts" -> effects.add(Presentation.createForWidthAndHeight(720, 1280, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
                "9:16 FHD" -> effects.add(Presentation.createForWidthAndHeight(1080, 1920, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
                "9:16 4K" -> effects.add(Presentation.createForWidthAndHeight(2160, 3840, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
                "16:9 Video" -> effects.add(Presentation.createForWidthAndHeight(1280, 720, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
                "16:9 FHD" -> effects.add(Presentation.createForWidthAndHeight(1920, 1080, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
                "16:9 4K" -> effects.add(Presentation.createForWidthAndHeight(3840, 2160, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
                "1:1 Square" -> effects.add(Presentation.createForWidthAndHeight(720, 720, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
            }

            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(clip.startMs + relativeStart)
                .setEndPositionMs(clip.startMs + relativeEnd)
                .build()
            val mediaItem = MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(clipping).build()
            val sourceGain = GainProcessor(DefaultGainProvider.Builder(clipAudioGain(clip, sourceVolume, fraction, mid, duration)).build())
            result.add(
                EditedMediaItem.Builder(mediaItem)
                    .setEffects(Effects(listOf(sourceGain), effects))
                    .setSpeed(speedProvider)
                    .setFrameRate(outputFrameRate.coerceIn(24, 60))
                    .build()
            )
        }
    }
    return result
}



private fun createPersonCutout(context: Context, uri: Uri): Uri {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: throw IllegalStateException("Could not read the selected image.")
    if (bounds.outWidth < 1 || bounds.outHeight < 1) throw IllegalStateException("The selected file is not a readable image.")

    var sampleSize = 1
    while (max(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > 1800) sampleSize *= 2
    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val decoded = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, decodeOptions)
    } ?: throw IllegalStateException("Could not decode the selected image.")
    val source = if (decoded.config != Bitmap.Config.ARGB_8888) {
        decoded.copy(Bitmap.Config.ARGB_8888, false).also { decoded.recycle() }
    } else decoded

    val segmenter = Segmentation.getClient(
        SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()
    )
    try {
        val input = InputImage.fromBitmap(source, 0)
        val mask = Tasks.await(segmenter.process(input), 90L, TimeUnit.SECONDS)
        val maskWidth = mask.width
        val maskHeight = mask.height
        if (maskWidth < 1 || maskHeight < 1) throw IllegalStateException("The model returned an empty mask.")
        val maskBuffer = mask.buffer.duplicate().order(ByteOrder.nativeOrder())
        maskBuffer.rewind()
        val confidences = FloatArray(maskWidth * maskHeight)
        for (i in confidences.indices) confidences[i] = maskBuffer.float.coerceIn(0f, 1f)

        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        var foregroundPixels = 0
        for (y in 0 until height) {
            val maskY = (y.toLong() * maskHeight / height).toInt().coerceIn(0, maskHeight - 1)
            for (x in 0 until width) {
                val maskX = (x.toLong() * maskWidth / width).toInt().coerceIn(0, maskWidth - 1)
                val idx = y * width + x
                val confidence = confidences[maskY * maskWidth + maskX]
                // Smoothstep keeps uncertain hair/edge pixels softer than a hard binary cut.
                val t = ((confidence - 0.12f) / 0.72f).coerceIn(0f, 1f)
                val alpha = (t * t * (3f - 2f * t) * 255f).toInt().coerceIn(0, 255)
                if (alpha > 48) foregroundPixels++
                pixels[idx] = (pixels[idx] and 0x00FFFFFF) or (alpha shl 24)
            }
        }
        if (foregroundPixels < width * height / 200) {
            throw IllegalStateException("No clear person was detected. Try a sharper photo with a visible person.")
        }
        val cutout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            cutout.setPixels(pixels, 0, width, 0, 0, width, height)
            val outFile = File(context.filesDir, "clipforge_person_cutout_" + System.currentTimeMillis() + ".png")
            outFile.outputStream().use { output ->
                if (!cutout.compress(CompressFormat.PNG, 100, output)) {
                    throw IllegalStateException("Could not write the transparent PNG.")
                }
            }
            return Uri.fromFile(outFile)
        } finally {
            cutout.recycle()
        }
    } finally {
        runCatching { segmenter.close() }
        source.recycle()
    }
}

private data class VideoTrackFade(val startMs: Long, val fadeInMs: Long)

private class ClipForgeVideoCompositorSettings(
    private val tracks: List<VideoTrackFade>
) : VideoCompositorSettings {
    override fun getOutputSize(inputSizes: MutableList<Size>): Size {
        return inputSizes.firstOrNull() ?: Size(720, 1280)
    }

    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings {
        val track = tracks.getOrNull(inputId) ?: VideoTrackFade(0L, 0L)
        val timeMs = presentationTimeUs / 1_000L
        val alpha = when {
            timeMs < track.startMs -> 0f
            track.fadeInMs > 0L && timeMs < track.startMs + track.fadeInMs ->
                ((timeMs - track.startMs).toFloat() / track.fadeInMs.toFloat()).coerceIn(0f, 1f)
            else -> 1f
        }
        return StaticOverlaySettings.Builder()
            .setAlphaScale(alpha)
            .setScale(1f, 1f)
            .setBackgroundFrameAnchor(0f, 0f)
            .setOverlayFrameAnchor(0f, 0f)
            .build()
    }
}

private fun renderedDurationEstimateMs(clip: Clip): Long {
    val duration = clip.durationMs.coerceAtLeast(1L)
    var result = 0.0
    for (step in 0 until 13) {
        val start = duration * step / 13L
        val end = duration * (step + 1) / 13L
        if (end <= start) continue
        val mid = (start + end) / 2f
        val fraction = (mid / duration.toFloat()).coerceIn(0f, 1f)
        val curve = when (clip.speedCurve) {
            "Ramp up" -> 0.5f + 1.5f * fraction
            "Ramp down" -> 2.0f - 1.5f * fraction
            "Ease in/out" -> 0.5f + 1.5f * kotlin.math.sin(Math.PI.toFloat() * fraction)
            "Pulse" -> (1.0f + 0.65f * kotlin.math.sin(fraction * (Math.PI.toFloat() * 4f))).coerceAtLeast(0.25f)
            else -> 1f
        }
        val speed = (clip.speed * curve).coerceIn(0.25f, 3f)
        result += (end - start).toDouble() / speed
    }
    return result.toLong().coerceAtLeast(1L)
}

private fun crossfadeDurationMs(outgoing: Clip, incoming: Clip): Long {
    val outDuration = renderedDurationEstimateMs(outgoing)
    val inDuration = renderedDurationEstimateMs(incoming)
    return minOf(450L, outDuration / 3L, inDuration / 3L).coerceAtLeast(0L)
}

private fun clipTimelineStartOffsetsMs(clips: List<Clip>): List<Long> {
    val starts = ArrayList<Long>(clips.size)
    var offset = 0L
    clips.forEachIndexed { index, clip ->
        starts.add(offset)
        offset += renderedDurationEstimateMs(clip)
        if (index < clips.lastIndex && clip.transition == "Crossfade") {
            offset -= crossfadeDurationMs(clip, clips[index + 1])
        }
    }
    return starts
}

private fun clipTimelineDurationMs(clips: List<Clip>): Long {
    if (clips.isEmpty()) return 1_000L
    return clipTimelineStartOffsetsMs(clips).last() + renderedDurationEstimateMs(clips.last())
}

private fun transparentFillerUri(context: Context): Uri {
    val file = File(context.cacheDir, "clipforge_transparent_filler.png")
    if (!file.exists() || file.length() < 20L) {
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(AndroidColor.TRANSPARENT)
            file.outputStream().use { stream ->
                if (!bitmap.compress(CompressFormat.PNG, 100, stream)) {
                    throw IllegalStateException("Could not prepare the crossfade filler image.")
                }
            }
        } finally {
            bitmap.recycle()
        }
    }
    return Uri.fromFile(file)
}

private fun hasAudioTrack(context: Context, uri: Uri): Boolean {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            ?.equals("yes", ignoreCase = true) == true
    } catch (_: Exception) {
        false
    } finally {
        runCatching { retriever.release() }
    }
}

private fun silenceWavUri(context: Context, durationMs: Long): Uri {
    val boundedMs = durationMs.coerceAtLeast(1L)
    val file = File(context.cacheDir, "clipforge_silence_" + boundedMs + "ms.wav")
    if (!file.exists() || file.length() < 44L) {
        val sampleRate = 8_000
        val channels = 1
        val bits = 16
        val sampleCount = (sampleRate.toLong() * boundedMs / 1_000L).coerceAtLeast(1L)
        val dataLengthLong = sampleCount * channels * bits / 8
        require(dataLengthLong <= Int.MAX_VALUE - 36L) { "Timeline too long to prepare silent audio padding." }
        val dataLength = dataLengthLong.toInt()
        file.outputStream().buffered().use { out ->
            fun writeAscii(value: String) { out.write(value.toByteArray(Charsets.US_ASCII)) }
            fun writeIntLE(value: Int) {
                out.write(value and 0xFF)
                out.write((value ushr 8) and 0xFF)
                out.write((value ushr 16) and 0xFF)
                out.write((value ushr 24) and 0xFF)
            }
            fun writeShortLE(value: Int) {
                out.write(value and 0xFF)
                out.write((value ushr 8) and 0xFF)
            }
            writeAscii("RIFF")
            writeIntLE(36 + dataLength)
            writeAscii("WAVE")
            writeIntLE(16)
            writeShortLE(1)
            writeShortLE(channels)
            writeIntLE(sampleRate)
            writeIntLE(sampleRate * channels * bits / 8)
            writeShortLE(channels * bits / 8)
            writeShortLE(bits)
            writeAscii("data")
            writeIntLE(dataLength)
            val zeros = ByteArray(16 * 1024)
            var remaining = dataLength
            while (remaining > 0) {
                val size = minOf(zeros.size, remaining)
                out.write(zeros, 0, size)
                remaining -= size
            }
        }
    }
    return Uri.fromFile(file)
}

private fun buildEditedAudioItems(
    context: Context,
    clip: Clip,
    sourceVolume: Float
): List<EditedMediaItem> {
    val result = mutableListOf<EditedMediaItem>()
    val duration = clip.durationMs.coerceAtLeast(1L)
    val boundaries = sortedSetOf(0L, duration)
    for (step in 1..12) boundaries.add(duration * step / 13L)
    boundaries.add((clip.audioFadeInMs * clip.speed).toLong().coerceIn(0L, duration))
    boundaries.add((duration - (clip.audioFadeOutMs * clip.speed).toLong()).coerceIn(0L, duration))
    boundaries.add(duration / 3L)
    boundaries.add(duration * 2L / 3L)
    val points = boundaries.toList()
    for (pointIndex in 0 until points.lastIndex) {
        val relativeStart = points[pointIndex]
        val relativeEnd = points[pointIndex + 1]
        if (relativeEnd <= relativeStart) continue
        val mid = (relativeStart + relativeEnd) / 2f
        val fraction = (mid / duration.toFloat()).coerceIn(0f, 1f)
        val speedFactor = when (clip.speedCurve) {
            "Ramp up" -> 0.5f + 1.5f * fraction
            "Ramp down" -> 2.0f - 1.5f * fraction
            "Ease in/out" -> 0.5f + 1.5f * kotlin.math.sin(Math.PI.toFloat() * fraction)
            "Pulse" -> (1.0f + 0.65f * kotlin.math.sin(fraction * (Math.PI.toFloat() * 4f))).coerceAtLeast(0.25f)
            else -> 1f
        }
        val segmentSpeed = (clip.speed * speedFactor).coerceIn(0.25f, 3f)
        val speedProvider = object : SpeedProvider {
            override fun getSpeed(timeUs: Long): Float = segmentSpeed
            override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
        }
        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(clip.startMs + relativeStart)
            .setEndPositionMs(clip.startMs + relativeEnd)
            .build()
        val item = MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(clipping).build()
        val gain = GainProcessor(DefaultGainProvider.Builder(clipAudioGain(clip, sourceVolume, fraction, mid, duration)).build())
        result.add(
            EditedMediaItem.Builder(item)
                .setRemoveVideo(true)
                .setEffects(Effects(listOf(gain), emptyList()))
                .setSpeed(speedProvider)
                .build()
        )
    }
    return result
}


private fun clipAudioGain(clip: Clip, baseVolume: Float, fraction: Float, sourceMidMs: Float, sourceDurationMs: Long): Float {
    val keyVolume = interpolateThree(
        clip.audioVolumeStart, clip.audioVolumeMid, clip.audioVolumeEnd, fraction, clip.motionCurve
    ).coerceIn(0f, 1f)
    val timelineMidMs = (sourceMidMs / clip.speed.coerceAtLeast(0.25f)).toLong().coerceAtLeast(0L)
    val renderedDuration = renderedDurationEstimateMs(clip).coerceAtLeast(1L)
    val fadeIn = clip.audioFadeInMs.coerceIn(0L, renderedDuration)
    val fadeOut = clip.audioFadeOutMs.coerceIn(0L, renderedDuration)
    val fadeInGain = if (fadeIn > 0L) (timelineMidMs.toFloat() / fadeIn.toFloat()).coerceIn(0f, 1f) else 1f
    val fadeOutGain = if (fadeOut > 0L) ((renderedDuration - timelineMidMs).toFloat() / fadeOut.toFloat()).coerceIn(0f, 1f) else 1f
    return (baseVolume.coerceIn(0f, 1f) * keyVolume * fadeInGain * fadeOutGain).coerceIn(0f, 1f)
}

private fun mediaDurationMs(context: Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
    } catch (_: Exception) {
        0L
    } finally {
        runCatching { retriever.release() }
    }
}

private fun buildMusicItems(
    context: Context,
    musicUri: Uri,
    projectDurationMs: Long,
    volume: Float,
    fadeInMs: Long,
    fadeOutMs: Long,
    voiceoverUri: Uri?
): List<EditedMediaItem> {
    val musicDurationMs = mediaDurationMs(context, musicUri)
    if (musicDurationMs < 100L || projectDurationMs < 100L) return emptyList()
    val projectDuration = projectDurationMs.coerceAtLeast(100L)
    val fadeIn = fadeInMs.coerceIn(0L, projectDuration / 2L)
    val fadeOut = fadeOutMs.coerceIn(0L, projectDuration / 2L)
    val voiceDurationMs = voiceoverUri?.let { mediaDurationMs(context, it) } ?: 0L
    val boundaries = sortedSetOf(0L, projectDuration)
    var loopBoundary = musicDurationMs
    while (loopBoundary < projectDuration) {
        boundaries.add(loopBoundary)
        loopBoundary += musicDurationMs
    }
    if (fadeIn > 0L) {
        var point = 100L
        while (point < fadeIn) {
            boundaries.add(point)
            point += 100L
        }
        boundaries.add(fadeIn)
    }
    val fadeOutStart = (projectDuration - fadeOut).coerceAtLeast(0L)
    if (fadeOut > 0L) {
        boundaries.add(fadeOutStart)
        var point = fadeOutStart + 100L
        while (point < projectDuration) {
            boundaries.add(point)
            point += 100L
        }
    }
    if (voiceDurationMs in 1 until projectDuration) boundaries.add(voiceDurationMs)

    val points = boundaries.toList()
    val items = mutableListOf<EditedMediaItem>()
    for (index in 0 until points.lastIndex) {
        val globalStart = points[index]
        val globalEnd = points[index + 1]
        if (globalEnd <= globalStart) continue
        val sourceStart = globalStart % musicDurationMs
        val length = (globalEnd - globalStart).coerceAtMost(musicDurationMs - sourceStart)
        val sourceEnd = sourceStart + length
        if (length < 1L || sourceEnd <= sourceStart) continue
        val midpoint = (globalStart + globalEnd) / 2f
        val fadeInGain = if (fadeIn > 0L) (midpoint / fadeIn.toFloat()).coerceIn(0f, 1f) else 1f
        val fadeOutGain = if (fadeOut > 0L) ((projectDuration - midpoint) / fadeOut.toFloat()).coerceIn(0f, 1f) else 1f
        val duck = if (voiceoverUri != null && voiceDurationMs > 0L && midpoint < voiceDurationMs) 0.38f else 1f
        val finalGain = (volume.coerceIn(0f, 1f) * fadeInGain * fadeOutGain * duck).coerceIn(0f, 1f)
        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(sourceStart)
            .setEndPositionMs(sourceEnd)
            .build()
        val item = MediaItem.Builder().setUri(musicUri).setClippingConfiguration(clipping).build()
        val gain = GainProcessor(DefaultGainProvider.Builder(finalGain).build())
        items.add(
            EditedMediaItem.Builder(item)
                .setRemoveVideo(true)
                .setEffects(Effects(listOf(gain), emptyList()))
                .build()
        )
    }
    return items
}

private fun toSrtTimestamp(milliseconds: Long): String {
    val total = milliseconds.coerceAtLeast(0L)
    val hours = total / 3_600_000L
    val minutes = (total % 3_600_000L) / 60_000L
    val seconds = (total % 60_000L) / 1_000L
    val millis = total % 1_000L
    return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
}

private fun parseSrtTimestamp(value: String): Long? {
    val match = Regex("""(\d{2}):(\d{2}):(\d{2})[,.](\d{1,3})""").find(value.trim()) ?: return null
    val hours = match.groupValues[1].toLongOrNull() ?: return null
    val minutes = match.groupValues[2].toLongOrNull() ?: return null
    val seconds = match.groupValues[3].toLongOrNull() ?: return null
    val millis = match.groupValues[4].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
    return hours * 3_600_000L + minutes * 60_000L + seconds * 1_000L + millis
}

private fun writeSrt(captions: List<CaptionLine>, clips: List<Clip>): String {
    val offsets = mutableMapOf<String, Long>()
    var running = 0L
    clips.forEach { clip ->
        offsets[clip.id] = running
        running += (clip.durationMs / clip.speed).toLong()
    }
    val lines = captions.mapNotNull { caption ->
        val clip = clips.firstOrNull { it.id == caption.clipId } ?: return@mapNotNull null
        val offset = offsets[clip.id] ?: 0L
        Triple(
            offset + (caption.startMs / clip.speed).toLong(),
            offset + (caption.endMs / clip.speed).toLong(),
            caption.text.trim()
        )
    }.filter { it.second > it.first && it.third.isNotBlank() }.sortedBy { it.first }
    return lines.mapIndexed { index, caption ->
        (index + 1).toString() + "\n" +
            toSrtTimestamp(caption.first) + " --> " + toSrtTimestamp(caption.second) + "\n" +
            caption.third
    }.joinToString("\n\n") + if (lines.isEmpty()) "" else "\n"
}

private fun parseSrtText(text: String, clips: List<Clip>): List<CaptionLine> {
    if (clips.isEmpty()) return emptyList()
    val blocks = text.trim().split(Regex("""\r?\n\s*\r?\n"""))
    val output = mutableListOf<CaptionLine>()
    for (block in blocks) {
        val lines = block.lines()
        val timeIndex = lines.indexOfFirst { it.contains("-->") }
        if (timeIndex < 0) continue
        val parts = lines[timeIndex].split("-->")
        if (parts.size != 2) continue
        val start = parseSrtTimestamp(parts[0]) ?: continue
        val end = parseSrtTimestamp(parts[1]) ?: continue
        if (end <= start) continue
        val captionText = lines.drop(timeIndex + 1).joinToString(" ").trim()
        if (captionText.isBlank()) continue
        var clipOffset = 0L
        clips.forEach { clip ->
            val clipDurationTimeline = (clip.durationMs / clip.speed).toLong()
            val overlapStart = maxOf(start, clipOffset)
            val overlapEnd = minOf(end, clipOffset + clipDurationTimeline)
            if (overlapEnd > overlapStart && clipDurationTimeline > 0L) {
                val localStart = ((overlapStart - clipOffset) * clip.speed).toLong().coerceAtLeast(0L)
                val localEnd = ((overlapEnd - clipOffset) * clip.speed).toLong().coerceIn(localStart + 1L, clip.durationMs)
                output.add(CaptionLine(
                    clipId = clip.id,
                    startMs = localStart,
                    endMs = localEnd,
                    text = captionText
                ))
            }
            clipOffset += clipDurationTimeline
        }
    }
    return output
}

private fun loadProject(context: Context): Project {
    val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val clips = mutableListOf<Clip>()
    runCatching {
        val array = JSONArray(p.getString("clips", "[]") ?: "[]")
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val source = o.optLong("source", 0L)
            val start = o.optLong("start", 0L)
            val end = o.optLong("end", source)
            if (source > 0L && start >= 0L && end > start && end <= source) {
                clips.add(Clip(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    uri = Uri.parse(o.getString("uri")),
                    name = o.optString("name", "Video clip"),
                    sourceMs = source,
                    startMs = start,
                    endMs = end,
                    speed = o.optDouble("speed", 1.0).toFloat().coerceIn(0.25f, 3f),
                    zoomStart = o.optDouble("zoomStart", 1.0).toFloat().coerceIn(1f, 1.8f),
                    zoomMid = o.optDouble("zoomMid", 1.0).toFloat().coerceIn(1f, 1.8f),
                    zoomEnd = o.optDouble("zoomEnd", 1.0).toFloat().coerceIn(1f, 1.8f),
                    rotationStart = o.optDouble("rotationStart", 0.0).toFloat().coerceIn(-180f, 180f),
                    rotationMid = o.optDouble("rotationMid", 0.0).toFloat().coerceIn(-180f, 180f),
                    rotationEnd = o.optDouble("rotationEnd", 0.0).toFloat().coerceIn(-180f, 180f),
                    motionCurve = o.optString("motionCurve", "Linear").takeIf { it in listOf("Linear", "Ease in", "Ease out", "Ease in/out") } ?: "Linear",
                    transition = o.optString("transition", "Cut").takeIf { it in listOf("Cut", "Crossfade", "Fade", "Flash", "Zoom") } ?: "Cut",
                    speedCurve = o.optString("speedCurve", "Constant").takeIf { it in listOf("Constant", "Ramp up", "Ramp down", "Ease in/out", "Pulse") } ?: "Constant",
                    cropZoom = o.optDouble("cropZoom", 1.0).toFloat().coerceIn(1f, 2.5f),
                    cropX = o.optDouble("cropX", 0.0).toFloat().coerceIn(-1f, 1f),
                    cropY = o.optDouble("cropY", 0.0).toFloat().coerceIn(-1f, 1f),
                    audioVolumeStart = o.optDouble("audioVolumeStart", 1.0).toFloat().coerceIn(0f, 1f),
                    audioVolumeMid = o.optDouble("audioVolumeMid", 1.0).toFloat().coerceIn(0f, 1f),
                    audioVolumeEnd = o.optDouble("audioVolumeEnd", 1.0).toFloat().coerceIn(0f, 1f),
                    audioFadeInMs = o.optLong("audioFadeInMs", 0L).coerceIn(0L, 10_000L),
                    audioFadeOutMs = o.optLong("audioFadeOutMs", 0L).coerceIn(0L, 10_000L)
                ))
            }
        }
    }
    val captionList = mutableListOf<CaptionLine>()
    runCatching {
        val array = JSONArray(p.getString("captions", "[]") ?: "[]")
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val start = item.optLong("start", 0L)
            val end = item.optLong("end", 0L)
            val text = item.optString("text", "")
            val tokens = mutableListOf<CaptionToken>()
            item.optJSONArray("tokens")?.let { tokenArray ->
                for (tokenIndex in 0 until tokenArray.length()) {
                    val token = tokenArray.optJSONObject(tokenIndex) ?: continue
                    val tokenText = token.optString("text", "")
                    val tokenStart = token.optLong("start", 0L)
                    val tokenEnd = token.optLong("end", 0L)
                    if (tokenText.isNotBlank() && tokenEnd > tokenStart) {
                        tokens.add(CaptionToken(tokenText, tokenStart, tokenEnd))
                    }
                }
            }
            if (end > start && text.isNotBlank() && clips.any { it.id == item.optString("clipId") }) {
                captionList.add(CaptionLine(
                    id = item.optString("id", UUID.randomUUID().toString()),
                    clipId = item.optString("clipId"),
                    startMs = start,
                    endMs = end,
                    text = text,
                    tokens = tokens
                ))
            }
        }
    }
    val layerList = mutableListOf<PhotoLayer>()
    runCatching {
        val array = JSONArray(p.getString("photoLayers", "[]") ?: "[]")
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val start = item.optLong("start", 0L)
            val end = item.optLong("end", 5_000L)
            if (end > start) layerList.add(PhotoLayer(
                id = item.optString("id", UUID.randomUUID().toString()),
                uri = Uri.parse(item.getString("uri")),
                name = item.optString("name", "Image layer"),
                startMs = start,
                endMs = end,
                position = item.optString("position", "bottom-right").takeIf { it in listOf("top-left", "top-right", "center", "bottom-left", "bottom-right") } ?: "bottom-right",
                scale = item.optDouble("scale", 0.30).toFloat().coerceIn(0.12f, 1f),
                opacity = item.optDouble("opacity", 1.0).toFloat().coerceIn(0f, 1f),
                rotation = item.optDouble("rotation", 0.0).toFloat().coerceIn(-180f, 180f),
                fullFrame = item.optBoolean("fullFrame", false),
                textContent = item.optString("textContent", "").takeIf { it.isNotBlank() },
                textStyle = item.optString("textStyle", "Classic").takeIf { it in listOf("Classic", "Yellow Pop", "Neon", "Minimal") } ?: "Classic",
                animateOpacity = item.optBoolean("animateOpacity", false),
                opacityStart = item.optDouble("opacityStart", 1.0).toFloat().coerceIn(0f, 1f),
                opacityMid = item.optDouble("opacityMid", 1.0).toFloat().coerceIn(0f, 1f),
                opacityEnd = item.optDouble("opacityEnd", 1.0).toFloat().coerceIn(0f, 1f)
            ))
        }
    }
    val markers = mutableListOf<Long>()
    runCatching {
        val markerArray = JSONArray(p.getString("beatMarkers", "[]") ?: "[]")
        for (i in 0 until markerArray.length()) markers.add(markerArray.optLong(i).coerceAtLeast(0L))
    }
    return Project(
        clips = clips,
        music = p.getString("music", null)?.let { Uri.parse(it) },
        titleText = p.getString("title", "") ?: "",
        brightness = p.getFloat("brightness", 0f).coerceIn(-0.35f, 0.35f),
        muteVideoAudio = p.getBoolean("mute", false),
        format = p.getString("format", "Original") ?: "Original",
        captions = captionList,
        saturation = p.getFloat("saturation", 0f).coerceIn(-100f, 100f),
        filterPreset = p.getString("filterPreset", "None") ?: "None",
        photoLayers = layerList,
        voiceover = p.getString("voiceover", null)?.let { Uri.parse(it) },
        beatMarkers = markers.sorted(),
        captionLanguageId = p.getString("captionLanguage", "english") ?: "english",
        sourceVolume = p.getFloat("sourceVolume", 1f).coerceIn(0f, 1f),
        musicVolume = p.getFloat("musicVolume", 1f).coerceIn(0f, 1f),
        voiceoverVolume = p.getFloat("voiceoverVolume", 1f).coerceIn(0f, 1f),
        exportQuality = p.getString("exportQuality", "High")?.takeIf { it in listOf("Saver", "Standard", "High", "Ultra") } ?: "High",
        exportFrameRate = p.getInt("exportFrameRate", 30).coerceIn(24, 60).let { if (it in listOf(24, 30, 60)) it else 30 },
        captionStyle = p.getString("captionStyle", "Classic")?.takeIf { it in listOf("Classic", "Yellow Pop", "Neon", "Minimal", "Karaoke") } ?: "Classic",
        captionPosition = p.getString("captionPosition", "Bottom")?.takeIf { it in listOf("Top", "Center", "Bottom") } ?: "Bottom",
        captionScale = p.getFloat("captionScale", 1f).coerceIn(0.7f, 1.5f),
        musicFadeInMs = p.getLong("musicFadeInMs", 0L).coerceIn(0L, 5_000L),
        musicFadeOutMs = p.getLong("musicFadeOutMs", 0L).coerceIn(0L, 5_000L),
        duckMusicUnderVoiceover = p.getBoolean("duckMusicUnderVoiceover", false)
    )
}

private fun saveProject(context: Context, project: Project) {
    val array = JSONArray()
    project.clips.forEach { clip ->
        array.put(JSONObject().apply {
            put("id", clip.id)
            put("uri", clip.uri.toString())
            put("name", clip.name)
            put("source", clip.sourceMs)
            put("start", clip.startMs)
            put("end", clip.endMs)
            put("speed", clip.speed)
            put("zoomStart", clip.zoomStart)
            put("zoomMid", clip.zoomMid)
            put("zoomEnd", clip.zoomEnd)
            put("rotationStart", clip.rotationStart)
            put("rotationMid", clip.rotationMid)
            put("rotationEnd", clip.rotationEnd)
            put("motionCurve", clip.motionCurve)
            put("transition", clip.transition)
            put("speedCurve", clip.speedCurve)
            put("cropZoom", clip.cropZoom)
            put("cropX", clip.cropX)
            put("cropY", clip.cropY)
            put("audioVolumeStart", clip.audioVolumeStart)
            put("audioVolumeMid", clip.audioVolumeMid)
            put("audioVolumeEnd", clip.audioVolumeEnd)
            put("audioFadeInMs", clip.audioFadeInMs)
            put("audioFadeOutMs", clip.audioFadeOutMs)
        })
    }
    val subtitleArray = JSONArray()
    project.captions.forEach { caption ->
        subtitleArray.put(JSONObject().apply {
            put("id", caption.id)
            put("clipId", caption.clipId)
            put("start", caption.startMs)
            put("end", caption.endMs)
            put("text", caption.text)
            put("tokens", JSONArray().apply {
                caption.tokens.forEach { token ->
                    put(JSONObject().apply {
                        put("text", token.text)
                        put("start", token.startMs)
                        put("end", token.endMs)
                    })
                }
            })
        })
    }
    val layerArray = JSONArray()
    project.photoLayers.forEach { layer ->
        layerArray.put(JSONObject().apply {
            put("id", layer.id)
            put("uri", layer.uri.toString())
            put("name", layer.name)
            put("start", layer.startMs)
            put("end", layer.endMs)
            put("position", layer.position)
            put("scale", layer.scale)
            put("opacity", layer.opacity)
            put("rotation", layer.rotation)
            put("fullFrame", layer.fullFrame)
            put("textContent", layer.textContent)
            put("textStyle", layer.textStyle)
            put("animateOpacity", layer.animateOpacity)
            put("opacityStart", layer.opacityStart)
            put("opacityMid", layer.opacityMid)
            put("opacityEnd", layer.opacityEnd)
        })
    }
    val markerArray = JSONArray()
    project.beatMarkers.forEach { markerArray.put(it) }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString("clips", array.toString())
        .putString("captions", subtitleArray.toString())
        .putString("photoLayers", layerArray.toString())
        .putString("voiceover", project.voiceover?.toString())
        .putString("beatMarkers", markerArray.toString())
        .putString("captionLanguage", project.captionLanguageId)
        .putFloat("sourceVolume", project.sourceVolume)
        .putFloat("musicVolume", project.musicVolume)
        .putFloat("voiceoverVolume", project.voiceoverVolume)
        .putString("exportQuality", project.exportQuality)
        .putInt("exportFrameRate", project.exportFrameRate)
        .putString("captionStyle", project.captionStyle)
        .putString("captionPosition", project.captionPosition)
        .putFloat("captionScale", project.captionScale)
        .putLong("musicFadeInMs", project.musicFadeInMs)
        .putLong("musicFadeOutMs", project.musicFadeOutMs)
        .putBoolean("duckMusicUnderVoiceover", project.duckMusicUnderVoiceover)
        .putString("music", project.music?.toString())
        .putString("title", project.titleText)
        .putFloat("brightness", project.brightness)
        .putFloat("saturation", project.saturation)
        .putString("filterPreset", project.filterPreset)
        .putBoolean("mute", project.muteVideoAudio)
        .putString("format", project.format)
        .apply()
}

private fun saveToGallery(context: Context, file: File): Uri {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ClipForge")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }
    val resolver = context.contentResolver
    val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val uri = resolver.insert(collection, values) ?: throw IllegalStateException("Could not create Gallery item")
    try {
        val target = resolver.openOutputStream(uri) ?: throw IllegalStateException("Could not open Gallery file")
        target.use { out -> file.inputStream().use { input -> input.copyTo(out) } }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        }
        return uri
    } catch (error: Exception) {
        resolver.delete(uri, null, null)
        throw error
    }
}
