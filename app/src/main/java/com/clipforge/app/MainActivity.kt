@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.clipforge.app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
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
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Brightness
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
    val endMs: Long = sourceMs
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

private data class Project(
    val clips: List<Clip>,
    val music: Uri?,
    val titleText: String,
    val brightness: Float,
    val muteVideoAudio: Boolean,
    val format: String
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
        onDispose { player.release(); audioPlayer.release() }
    }

    val selectedIndex = clips.indexOfFirst { it.id == selectedId }
    val selected = clips.getOrNull(selectedIndex)

    LaunchedEffect(Unit) {
        val saved = withContext(Dispatchers.IO) { loadProject(context) }
        clips = saved.clips
        selectedId = saved.clips.firstOrNull()?.id
        music = saved.music
        musicName = saved.music?.let { withContext(Dispatchers.IO) { nameForUri(context, it) } }
        titleText = saved.titleText
        brightness = saved.brightness
        muteVideoAudio = saved.muteVideoAudio
        format = saved.format
        loaded = true
    }

    LaunchedEffect(loaded, clips, music, titleText, brightness, muteVideoAudio, format) {
        if (loaded) withContext(Dispatchers.IO) {
            saveProject(context, Project(clips, music, titleText, brightness, muteVideoAudio, format))
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

    fun importVideos() { videoPicker.launch(arrayOf("video/*")) }
    fun importMusic() { musicPicker.launch(arrayOf("audio/*")) }

    fun updateClip(clip: Clip) { clips = clips.map { if (it.id == clip.id) clip else it } }

    fun commitTrim() {
        val clip = selected ?: return
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
        clips = clips.toMutableList().apply { add(target, removeAt(selectedIndex)) }
    }

    fun exportVideo() {
        if (clips.isEmpty() || exporting) return
        player.pause()
        val folder = File(context.cacheDir, "clipforge")
        folder.mkdirs()
        val output = File(folder, "clipforge_" + System.currentTimeMillis() + ".mp4")
        try {
            val editItems = clips.map { clip ->
                val clipping = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs)
                    .build()
                val mediaItem = MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(clipping).build()
                val videoEffects = mutableListOf<androidx.media3.common.Effect>()
                if (brightness != 0f) videoEffects.add(Brightness(brightness))
                if (titleText.isNotBlank()) {
                    val styled = SpannableString(titleText.trim()).apply {
                        setSpan(ForegroundColorSpan(AndroidColor.WHITE), 0, length, 0)
                        setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
                        setSpan(BackgroundColorSpan(AndroidColor.argb(190, 0, 0, 0)), 0, length, 0)
                    }
                    val overlaySettings = StaticOverlaySettings.Builder()
                        .setBackgroundFrameAnchor(0f, -0.78f)
                        .setOverlayFrameAnchor(0f, 0f)
                        .build()
                    videoEffects.add(OverlayEffect(listOf(TextOverlay.createStaticTextOverlay(styled, overlaySettings))))
                }
                when (format) {
                    "9:16 Shorts" -> videoEffects.add(Presentation.createForWidthAndHeight(720, 1280, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
                    "16:9 Video" -> videoEffects.add(Presentation.createForWidthAndHeight(1280, 720, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
                    "1:1 Square" -> videoEffects.add(Presentation.createForWidthAndHeight(720, 720, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
                }
                EditedMediaItem.Builder(mediaItem).setEffects(Effects(emptyList(), videoEffects)).build()
            }

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
                    clips, selectedId, selectedIndex, selected, player, position, trimStart, trimEnd, titleText, message,
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
                    onPick = ::importMusic,
                    onRemove = { music = null; musicName = null; message = "Background track removed." },
                    onPreview = {
                        if (music == null) importMusic()
                        else if (musicPlaying) { audioPlayer.pause(); musicPlaying = false }
                        else { audioPlayer.play(); musicPlaying = true }
                    },
                    onMute = { muteVideoAudio = it }
                )
                2 -> LooksPanel(
                    titleText, brightness,
                    onTitle = { titleText = it },
                    onBrightness = { brightness = it },
                    onReset = { brightness = 0f }
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
    position: Long, trimStart: Long, trimEnd: Long, titleText: String, message: String?,
    onImport: () -> Unit, onSelect: (String) -> Unit, onSeek: (Long) -> Unit, onPlay: () -> Unit,
    onStartChange: (Long) -> Unit, onEndChange: (Long) -> Unit, onTrim: () -> Unit,
    onSplit: () -> Unit, onDelete: () -> Unit, onMove: (Int) -> Unit, onAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Your timeline", fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Import • trim • split • arrange • export", fontSize = 12.sp, color = Muted)
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
                            modifier = Modifier.fillMaxSize(),
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
                            Text(titleText, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                                maxLines = 3, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 15.dp)
                                    .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.75f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp))
                        }
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
    onPick: () -> Unit, onRemove: () -> Unit, onPreview: () -> Unit, onMute: (Boolean) -> Unit
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
private fun LooksPanel(
    titleText: String, brightness: Float, onTitle: (String) -> Unit,
    onBrightness: (Float) -> Unit, onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Text & picture", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Add a title overlay and adjust brightness in your exported video.", color = Muted, fontSize = 13.sp)
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, null, tint = Lime)
                    Spacer(Modifier.width(8.dp))
                    Text("Text overlay", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = titleText, onValueChange = onTitle, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title / caption") }, placeholder = { Text("Type your video text…") },
                    supportingText = { Text("Burned into the final video, not just a preview.") },
                    minLines = 2, maxLines = 4, shape = RoundedCornerShape(14.dp)
                )
            }
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, null, tint = Lime)
                    Spacer(Modifier.width(8.dp))
                    Text("Brightness", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text((brightness * 100).toInt().toString() + "%", color = Lime, fontWeight = FontWeight.Bold)
                }
                Slider(value = brightness, onValueChange = onBrightness, valueRange = -0.35f..0.35f)
                Row {
                    Text("Darker", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text("Original", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text("Brighter", color = Muted, fontSize = 11.sp)
                }
                OutlinedButton(onClick = onReset, shape = RoundedCornerShape(12.dp)) { Text("Reset brightness") }
            }
        }
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

@Composable
private fun NavigationBar(tab: Int, select: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp).clip(RoundedCornerShape(19.dp))
            .background(Panel).padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem("Edit", Icons.Default.Movie, tab == 0) { select(0) }
        NavItem("Audio", Icons.Default.LibraryMusic, tab == 1) { select(1) }
        NavItem("Looks", Icons.Default.AutoAwesome, tab == 2) { select(2) }
        NavItem("Export", Icons.Default.Download, tab == 3) { select(3) }
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
                    endMs = end
                ))
            }
        }
    }
    return Project(
        clips,
        p.getString("music", null)?.let { Uri.parse(it) },
        p.getString("title", "") ?: "",
        p.getFloat("brightness", 0f).coerceIn(-0.35f, 0.35f),
        p.getBoolean("mute", false),
        p.getString("format", "Original") ?: "Original"
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
        })
    }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString("clips", array.toString())
        .putString("music", project.music?.toString())
        .putString("title", project.titleText)
        .putFloat("brightness", project.brightness)
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
