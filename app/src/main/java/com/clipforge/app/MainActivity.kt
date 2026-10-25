@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.clipforge.app

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Brightness
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
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

private val Bg = Color(0xFF0D0F14)
private val Panel = Color(0xFF171A22)
private val PanelAlt = Color(0xFF232833)
private val Lime = Color(0xFFB8F35A)
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
    val zoomEnd: Float = 1f,
    val rotationStart: Float = 0f,
    val rotationEnd: Float = 0f,
    val transition: String = "Cut"
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

private data class CaptionLine(
    val id: String = UUID.randomUUID().toString(),
    val clipId: String,
    val startMs: Long,
    val endMs: Long,
    val text: String
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
    val rotation: Float = 0f
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
    val beatMarkers: List<Long> = emptyList()
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
    var titleText by remember { mutableStateOf("") }
    var brightness by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(0f) }
    var filterPreset by remember { mutableStateOf("None") }
    var captions by remember { mutableStateOf<List<CaptionLine>>(emptyList()) }
    var captioning by remember { mutableStateOf(false) }
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
    val timelineDurationMs = clips.sumOf { (it.durationMs / it.speed).toLong() }.coerceAtLeast(1_000L)
    val timelinePositionMs = clips.take(selectedIndex.coerceAtLeast(0)).sumOf { (it.durationMs / it.speed).toLong() } +
        if (selected != null) (position / selected.speed).toLong() else 0L

    fun currentProject() = Project(
        clips, music, titleText, brightness, muteVideoAudio, format,
        captions, saturation, filterPreset, photoLayers, voiceover, beatMarkers
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
        titleText = project.titleText
        brightness = project.brightness
        saturation = project.saturation
        filterPreset = project.filterPreset
        captions = project.captions
        photoLayers = project.photoLayers
        voiceover = project.voiceover
        voiceoverName = project.voiceover?.let { nameForUri(context, it) }
        beatMarkers = project.beatMarkers
        muteVideoAudio = project.muteVideoAudio
        format = project.format
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
        titleText = saved.titleText
        brightness = saved.brightness
        saturation = saved.saturation
        filterPreset = saved.filterPreset
        captions = saved.captions
        photoLayers = saved.photoLayers
        voiceover = saved.voiceover
        voiceoverName = saved.voiceover?.let { nameForUri(context, it) }
        beatMarkers = saved.beatMarkers
        muteVideoAudio = saved.muteVideoAudio
        format = saved.format
        loaded = true
    }

    LaunchedEffect(loaded, clips, music, titleText, brightness, saturation, filterPreset, captions, photoLayers, voiceover, beatMarkers, muteVideoAudio, format) {
        if (loaded) withContext(Dispatchers.IO) {
            saveProject(context, Project(
                clips, music, titleText, brightness, muteVideoAudio, format,
                captions, saturation, filterPreset, photoLayers, voiceover, beatMarkers
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

    fun importVideos() { videoPicker.launch(arrayOf("video/*")) }
    fun importMusic() { musicPicker.launch(arrayOf("audio/*")) }
    fun importPhotoLayers() { photoLayerPicker.launch(arrayOf("image/*")) }

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
                    val wordMap = AutoCaptionEngine.transcribeAll(context, sources) { status ->
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

    fun updateClip(clip: Clip) { clips = clips.map { if (it.id == clip.id) clip else it } }

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
        val folder = File(context.cacheDir, "clipforge")
        folder.mkdirs()
        val output = File(folder, "clipforge_" + System.currentTimeMillis() + ".mp4")
        try {
            val editItems = buildEditedItems(context, clips, captions, photoLayers, titleText, brightness, saturation, filterPreset, format)

            val mainSequence = if (muteVideoAudio) {
                EditedMediaItemSequence.withVideoFrom(editItems)
            } else {
                EditedMediaItemSequence.withAudioAndVideoFrom(editItems)
            }
            val sequenceList = mutableListOf(mainSequence)
            music?.let { audioUri ->
                val audioItem = EditedMediaItem.Builder(MediaItem.fromUri(audioUri)).build()
                sequenceList.add(EditedMediaItemSequence.withAudioFrom(listOf(audioItem)).buildUpon().setIsLooping(true).build())
            }
            voiceover?.let { voiceUri ->
                val voiceItem = EditedMediaItem.Builder(MediaItem.fromUri(voiceUri)).build()
                sequenceList.add(EditedMediaItemSequence.withAudioFrom(listOf(voiceItem)))
            }
            val composition = Composition.Builder(sequenceList).build()
            exporting = true
            outputUri = null
            message = "Rendering MP4 on your phone. Keep the app open until it finishes."
            val worker = Transformer.Builder(context.applicationContext)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        scope.launch {
                            try {
                                val savedUri = withContext(Dispatchers.IO) { saveToGallery(context, output) }
                                outputUri = savedUri
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
                        transformer = null
                        message = "Export failed: " + (exportException.message ?: "unsupported media or codec")
                        output.delete()
                    }
                })
                .build()
            transformer = worker
            worker.start(composition, output.absolutePath)
        } catch (error: Exception) {
            exporting = false
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

    Column(Modifier.fillMaxSize().background(Bg)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 17.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp)
        ) {
            AppHeader(clips.size)
            when (tab) {
                0 -> EditorPanel(
                    clips, selectedId, selectedIndex, selected, player, position, trimStart, trimEnd, titleText, captions, photoLayers, message,
                    canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty(),
                    onUndo = ::undoProject, onRedo = ::redoProject,
                    onImport = ::importVideos,
                    onSelect = { selectedId = it },
                    onSeek = { positionMs -> player.seekTo(positionMs); position = positionMs },
                    onPlay = { if (player.isPlaying) player.pause() else player.play() },
                    onStartChange = { trimStart = it.coerceIn(0L, (trimEnd - 100L).coerceAtLeast(0L)) },
                    onEndChange = { value ->
                        val maxEnd = selected?.sourceMs ?: value
                        trimEnd = value.coerceIn((trimStart + 100L).coerceAtMost(maxEnd), maxEnd)
                    },
                    onTrim = ::commitTrim,
                    onSplit = ::splitClip,
                    onDelete = ::deleteClip,
                    onMove = ::moveClip,
                    onAdd = ::importVideos
                )
                1 -> AudioPanel(
                    musicName, music != null, musicPlaying, muteVideoAudio, message,
                    voiceoverName = voiceoverName,
                    hasVoiceover = voiceover != null,
                    voiceoverRecording = voiceoverRecording,
                    beatMarkers = beatMarkers,
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
                    onAutoCaption = ::generateAutoCaptions,
                    onImportSrt = { srtImportLauncher.launch(arrayOf("application/x-subrip", "text/plain", "application/octet-stream")) },
                    onExportSrt = { srtExportLauncher.launch("ClipForge-subtitles.srt") },
                    onEdit = { id, text -> captions = captions.map { if (it.id == id) it.copy(text = text) else it } },
                    onDelete = { id -> captions = captions.filterNot { it.id == id } }
                )
                4 -> LayersPanel(
                    layers = photoLayers,
                    timelineDurationMs = clips.sumOf { (it.durationMs / it.speed).toLong() }.coerceAtLeast(1_000L),
                    message = message,
                    onAdd = ::importPhotoLayers,
                    onUpdate = { layer -> photoLayers = photoLayers.map { if (it.id == layer.id) layer else it } },
                    onDelete = { id -> photoLayers = photoLayers.filterNot { it.id == id } }
                )
                else -> ExportPanel(
                    clips, musicName, titleText, brightness, format, exporting, outputUri != null, message,
                    onFormat = { format = it },
                    onExport = ::exportVideo,
                    onCancel = { transformer?.cancel(); message = "Cancel requested…" },
                    onShare = ::shareVideo
                )
            }
                        Spacer(Modifier.height(8.dp))
        }
        NavigationBar(tab) { tab = it }
    }
}

@Composable
private fun AppHeader(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(43.dp).clip(RoundedCornerShape(14.dp)).background(Lime), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ContentCut, null, tint = Bg, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("ClipForge", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Text("MOBILE VIDEO STUDIO", fontSize = 9.sp, color = Muted, letterSpacing = 1.5.sp)
        }
        Text(count.toString() + " CLIPS", color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(CircleShape).background(Panel).padding(horizontal = 11.dp, vertical = 8.dp))
    }
}

@Composable
private fun EditorPanel(
    clips: List<Clip>, selectedId: String?, selectedIndex: Int, selected: Clip?, player: ExoPlayer,
    position: Long, trimStart: Long, trimEnd: Long, titleText: String, captions: List<CaptionLine>,
    photoLayers: List<PhotoLayer>, message: String?,
    canUndo: Boolean, canRedo: Boolean, onUndo: () -> Unit, onRedo: () -> Unit,
    onImport: () -> Unit, onSelect: (String) -> Unit, onSeek: (Long) -> Unit, onPlay: () -> Unit,
    onStartChange: (Long) -> Unit, onEndChange: (Long) -> Unit, onTrim: () -> Unit,
    onSplit: () -> Unit, onDelete: () -> Unit, onMove: (Int) -> Unit, onAdd: () -> Unit
) {
    val keyframeFraction = if (selected == null || selected.durationMs <= 0L) 0f else
        (position.toFloat() / selected.durationMs.toFloat()).coerceIn(0f, 1f)
    val previewZoom = selected?.let { it.zoomStart + (it.zoomEnd - it.zoomStart) * keyframeFraction } ?: 1f
    val previewRotation = selected?.let { it.rotationStart + (it.rotationEnd - it.rotationStart) * keyframeFraction } ?: 0f
    val activeCaption = selected?.let { clip ->
        captions.firstOrNull { it.clipId == clip.id && position >= it.startMs && position < it.endMs }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Your timeline", fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Import • trim • split • arrange • export", fontSize = 12.sp, color = Muted)
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
        if (selected == null) {
            Card(shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
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
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1.28f).clip(RoundedCornerShape(15.dp)).background(Color.Black)) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize().graphicsLayer(
                                scaleX = previewZoom,
                                scaleY = previewZoom,
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
                            Text(activeCaption.text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                                maxLines = 3, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 14.dp)
                                    .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.80f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                        val totalBeforeSelected = clips.take(selectedIndex.coerceAtLeast(0)).sumOf { (it.durationMs / it.speed).toLong() }
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
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    clips.forEachIndexed { index, clip ->
                        Column(
                            Modifier.width(122.dp).clip(RoundedCornerShape(15.dp))
                                .background(if (clip.id == selectedId) Color(0xFF2B3621) else Panel)
                                .clickable { onSelect(clip.id) }.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("CLIP " + (index + 1), color = if (clip.id == selectedId) Lime else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                if (clip.id == selectedId) Icon(Icons.Default.CheckCircle, null, tint = Lime, modifier = Modifier.size(14.dp))
                            }
                            Text(clip.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(time(clip.durationMs), color = Muted, fontSize = 10.sp)
                            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(if (clip.id == selectedId) Lime else PanelAlt))
                        }
                    }
                    Column(
                        Modifier.width(84.dp).height(100.dp).clip(RoundedCornerShape(15.dp))
                            .background(Panel).clickable(onClick = onAdd).padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = Lime)
                        Text("Add clip", color = Muted, fontSize = 10.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ActionButton("Split", Icons.Default.ContentCut, onSplit, Modifier.weight(1f), selected != null)
                    ActionButton("Up", Icons.Default.ArrowUpward, { onMove(-1) }, Modifier.weight(1f), selectedIndex > 0)
                    ActionButton("Down", Icons.Default.ArrowDownward, { onMove(1) }, Modifier.weight(1f), selectedIndex >= 0 && selectedIndex < clips.lastIndex)
                    ActionButton("Delete", Icons.Default.Delete, onDelete, Modifier.weight(1f), selected != null)
                }
            }

            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
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
private fun AudioPanel(
    musicName: String?, hasMusic: Boolean, playing: Boolean, muted: Boolean, message: String?,
    voiceoverName: String?, hasVoiceover: Boolean, voiceoverRecording: Boolean,
    beatMarkers: List<Long>, timelinePositionMs: Long, timelineDurationMs: Long,
    onPick: () -> Unit, onRemove: () -> Unit, onPreview: () -> Unit, onMute: (Boolean) -> Unit,
    onStartVoiceover: () -> Unit, onStopVoiceover: () -> Unit, onRemoveVoiceover: () -> Unit,
    onMarkBeat: () -> Unit, onDeleteBeat: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Sound & music", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Add a background track and control the original clip audio.", color = Muted, fontSize = 13.sp)
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
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
            }
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
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

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
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

        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mute original video audio", fontWeight = FontWeight.Bold)
                    Text("Keep it off to retain speech and source sound during export.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                }
                Switch(checked = muted, onCheckedChange = onMute)
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

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
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
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f).forEach { speed ->
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

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TRANSITION TO NEXT CLIP", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                if (selected == null) {
                    Text("Select a clip first.", color = Muted, fontSize = 12.sp)
                } else {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("Cut", "Fade", "Flash", "Zoom").forEach { effect ->
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
                    Text("Fade and flash use short edge effects; Zoom adds a push-in. Media3 currently doesn't support overlapping crossfade tracks, so these presets are rendered as cut-safe transitions.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
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
    onAutoCaption: () -> Unit,
    onImportSrt: () -> Unit,
    onExportSrt: () -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Captions studio", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Generate timestamped English subtitles offline, edit the lines, or import/export SRT.", color = Muted, fontSize = 13.sp)
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Lime, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("On-device auto captions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("English speech model · stored on this phone after first download", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
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
                Text("The first caption run downloads a small English speech model (around 40 MB). Speech is decoded and transcribed locally; no video is uploaded to a caption server.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
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

@Composable
private fun ExportPanel(
    clips: List<Clip>, musicName: String?, titleText: String, brightness: Float, format: String,
    exporting: Boolean, hasOutput: Boolean, message: String?,
    onFormat: (String) -> Unit, onExport: () -> Unit, onCancel: () -> Unit, onShare: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Export video", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Render your edited timeline to an MP4 on this phone.", color = Muted, fontSize = 13.sp)
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
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
                    listOf("Original", "9:16 Shorts", "16:9 Video", "1:1 Square").forEach { option ->
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
                Text("MP4 · H.264 video · AAC audio", color = Muted, fontSize = 12.sp)
                if (exporting) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = Lime)
                    Text("Rendering on device. Large videos can take several minutes.", color = Muted, fontSize = 12.sp)
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
    onUpdate: (PhotoLayer) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Layers & overlays", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Add image, logo, sticker, or watermark layers on top of your video timeline.", color = Muted, fontSize = 13.sp)
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
        Text("Timeline length: " + time(timelineDurationMs), color = Muted, fontSize = 11.sp)
        if (layers.isEmpty()) {
            Card(shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Your overlay track is empty", fontWeight = FontWeight.Bold)
                    Text("Choose a transparent PNG logo, a sticker, or a picture. Each layer has its own in/out time, position, scale, opacity, and rotation.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        } else {
            layers.forEachIndexed { index, layer ->
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, null, tint = Lime, modifier = Modifier.size(25.dp))
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("OVERLAY " + (index + 1), color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                                Text(layer.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove overlay",
                                tint = Color(0xFFFF8D8D),
                                modifier = Modifier.size(20.dp).clickable { onDelete(layer.id) }
                            )
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
private fun PhotoLayerPreview(layers: List<PhotoLayer>, timelinePositionMs: Long) {
    val active = layers.filter { timelinePositionMs >= it.startMs && timelinePositionMs < it.endMs }
    Box(Modifier.fillMaxSize()) {
        active.forEach { layer ->
            val context = LocalContext.current
            val bitmap by produceState<Bitmap?>(initialValue = null, key1 = layer.uri) {
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(layer.uri)?.use { BitmapFactory.decodeStream(it) }
                    }.getOrNull()
                }
            }
            if (bitmap != null) {
                val alignment = when (layer.position) {
                    "top-left" -> Alignment.TopStart
                    "top-right" -> Alignment.TopEnd
                    "center" -> Alignment.Center
                    "bottom-left" -> Alignment.BottomStart
                    else -> Alignment.BottomEnd
                }
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = layer.name,
                    alpha = layer.opacity.coerceIn(0f, 1f),
                    modifier = Modifier.align(alignment).padding(8.dp)
                        .size((230f * layer.scale).dp)
                        .graphicsLayer(rotationZ = layer.rotation)
                )
            }
        }
    }
}

@Composable
private fun NavigationBar(tab: Int, select: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 7.dp).clip(RoundedCornerShape(19.dp))
            .background(Panel).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem("Edit", Icons.Default.Movie, tab == 0) { select(0) }
        NavItem("Audio", Icons.Default.LibraryMusic, tab == 1) { select(1) }
        NavItem("Motion", Icons.Default.Tune, tab == 2) { select(2) }
        NavItem("Captions", Icons.Default.Subtitles, tab == 3) { select(3) }
        NavItem("Layers", Icons.Default.Layers, tab == 4) { select(4) }
        NavItem("Export", Icons.Default.Download, tab == 5) { select(5) }
    }
}

@Composable
private fun NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (active) Lime else Muted, modifier = Modifier.size(21.dp))
        Text(label, color = if (active) Lime else Muted, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
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
        Text(label, fontSize = 10.sp, maxLines = 1)
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
        if (text.isNotBlank()) output.add(CaptionLine(clipId = clipId, startMs = start, endMs = end, text = text))
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
    format: String
): List<EditedMediaItem> {
    val result = mutableListOf<EditedMediaItem>()
    clips.forEachIndexed { clipIndex, clip ->
        val duration = clip.durationMs.coerceAtLeast(1L)
        val projectOffsetMs = clips.take(clipIndex).sumOf { (it.durationMs / it.speed).toLong() }
        val projectClipEndMs = projectOffsetMs + (duration / clip.speed).toLong()
        val activeCaptions = captions.filter { it.clipId == clip.id && it.endMs > 0L && it.startMs < duration }
        val activePhotoLayers = photoLayers.filter { it.endMs > projectOffsetMs && it.startMs < projectClipEndMs }
        val transitionIn = clips.getOrNull(clipIndex - 1)?.transition ?: "Cut"
        val transitionOut = if (clipIndex < clips.lastIndex) clip.transition else "Cut"
        val transitionLength = minOf(400L, (duration / 3L).coerceAtLeast(0L))
        val boundaries = sortedSetOf(0L, duration)

        // These points create visible intermediate keyframes during export, rather than a UI-only animation.
        for (step in 1..12) boundaries.add(duration * step / 13L)
        activeCaptions.forEach { caption ->
            boundaries.add(caption.startMs.coerceIn(0L, duration))
            boundaries.add(caption.endMs.coerceIn(0L, duration))
        }
        activePhotoLayers.forEach { layer ->
            val layerStartInSource = ((layer.startMs - projectOffsetMs).coerceAtLeast(0L) * clip.speed).toLong()
            val layerEndInSource = ((layer.endMs - projectOffsetMs).coerceAtLeast(0L) * clip.speed).toLong()
            boundaries.add(layerStartInSource.coerceIn(0L, duration))
            boundaries.add(layerEndInSource.coerceIn(0L, duration))
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
        val speedProvider = object : SpeedProvider {
            override fun getSpeed(timeUs: Long): Float = clip.speed
            override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
        }

        for (pointIndex in 0 until points.lastIndex) {
            val relativeStart = points[pointIndex]
            val relativeEnd = points[pointIndex + 1]
            if (relativeEnd <= relativeStart) continue
            val mid = (relativeStart + relativeEnd) / 2f
            val fraction = (mid / duration.toFloat()).coerceIn(0f, 1f)
            val keyZoom = clip.zoomStart + (clip.zoomEnd - clip.zoomStart) * fraction
            val keyRotation = clip.rotationStart + (clip.rotationEnd - clip.rotationStart) * fraction
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
                    .setScale(layer.scale.coerceIn(0.12f, 0.7f), layer.scale.coerceIn(0.12f, 0.7f))
                    .setAlphaScale(layer.opacity.coerceIn(0f, 1f))
                    .setRotationDegrees(layer.rotation.coerceIn(-180f, 180f))
                    .build()
                overlays.add(BitmapOverlay.createStaticBitmapOverlay(context, layer.uri, layerSettings))
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
                val styledCaption = SpannableString(activeCaption.text.trim()).apply {
                    setSpan(ForegroundColorSpan(AndroidColor.WHITE), 0, length, 0)
                    setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
                    setSpan(BackgroundColorSpan(AndroidColor.argb(210, 0, 0, 0)), 0, length, 0)
                }
                val captionSettings = StaticOverlaySettings.Builder()
                    .setBackgroundFrameAnchor(0f, -0.78f)
                    .setOverlayFrameAnchor(0f, 0f)
                    .build()
                overlays.add(TextOverlay.createStaticTextOverlay(styledCaption, captionSettings))
            }
            if (overlays.isNotEmpty()) effects.add(OverlayEffect(overlays))

            when (format) {
                "9:16 Shorts" -> effects.add(Presentation.createForWidthAndHeight(720, 1280, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
                "16:9 Video" -> effects.add(Presentation.createForWidthAndHeight(1280, 720, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
                "1:1 Square" -> effects.add(Presentation.createForWidthAndHeight(720, 720, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
            }

            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(clip.startMs + relativeStart)
                .setEndPositionMs(clip.startMs + relativeEnd)
                .build()
            val mediaItem = MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(clipping).build()
            result.add(
                EditedMediaItem.Builder(mediaItem)
                    .setEffects(Effects(emptyList(), effects))
                    .setSpeed(speedProvider)
                    .build()
            )
        }
    }
    return result
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
                    zoomEnd = o.optDouble("zoomEnd", 1.0).toFloat().coerceIn(1f, 1.8f),
                    rotationStart = o.optDouble("rotationStart", 0.0).toFloat().coerceIn(-180f, 180f),
                    rotationEnd = o.optDouble("rotationEnd", 0.0).toFloat().coerceIn(-180f, 180f),
                    transition = o.optString("transition", "Cut").takeIf { it in listOf("Cut", "Fade", "Flash", "Zoom") } ?: "Cut"
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
            if (end > start && text.isNotBlank() && clips.any { it.id == item.optString("clipId") }) {
                captionList.add(CaptionLine(
                    id = item.optString("id", UUID.randomUUID().toString()),
                    clipId = item.optString("clipId"),
                    startMs = start,
                    endMs = end,
                    text = text
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
                scale = item.optDouble("scale", 0.30).toFloat().coerceIn(0.12f, 0.7f),
                opacity = item.optDouble("opacity", 1.0).toFloat().coerceIn(0f, 1f),
                rotation = item.optDouble("rotation", 0.0).toFloat().coerceIn(-180f, 180f)
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
        beatMarkers = markers.sorted()
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
            put("zoomEnd", clip.zoomEnd)
            put("rotationStart", clip.rotationStart)
            put("rotationEnd", clip.rotationEnd)
            put("transition", clip.transition)
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
