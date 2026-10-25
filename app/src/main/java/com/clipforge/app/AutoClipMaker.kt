package com.clipforge.app

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import java.util.Locale
import java.util.UUID

/** A suggested highlight range, relative to the source clip's current trim start. */
data class AutoClipSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val score: Int,
    val reason: String,
    val transcriptExcerpt: String,
    val wordTimings: List<WordTiming> = emptyList()
)

/**
 * Offline highlight suggestions combining bundled Vosk word timing with sampled visual scene
 * changes. This is an on-device ranking heuristic, not a hosted generative video model.
 */
object AutoClipMaker {
    private data class SceneEvent(val timeMs: Long, val strength: Float)

    fun analyze(
        context: Context,
        uri: Uri,
        sourceStartMs: Long,
        sourceEndMs: Long,
        targetDurationMs: Long,
        language: CaptionLanguage,
        onStatus: (String) -> Unit
    ): List<AutoClipSuggestion> {
        val durationMs = (sourceEndMs - sourceStartMs).coerceAtLeast(0L)
        require(durationMs >= 3_000L) { "Choose a clip at least 3 seconds long for auto highlights." }
        val targetMs = min(targetDurationMs.coerceIn(8_000L, 60_000L), durationMs)
        onStatus("Scanning scene changes locally…")
        val sceneEvents = detectSceneChanges(context, uri, sourceStartMs, durationMs, onStatus)
        onStatus("Analyzing audio peaks locally…")
        val audioPeaks = runCatching { OfflineWaveformAnalyzer.analyze(context, uri, 180) }.getOrDefault(emptyList())

        var words = emptyList<WordTiming>()
        try {
            onStatus("Running bundled offline " + language.displayName + " speech analysis…")
            words = AutoCaptionEngine.transcribeAll(
                context,
                listOf(CaptionSource("auto-clip-source", uri, sourceStartMs, sourceEndMs)),
                language,
                onStatus
            )["auto-clip-source"].orEmpty()
        } catch (error: Exception) {
            onStatus("Speech analysis unavailable; ranking clips from visual scene changes instead…")
        }

        val candidates = linkedSetOf<Long>()
        val lastStart = (durationMs - targetMs).coerceAtLeast(0L)
        val stride = max(4_000L, targetMs / 3L)
        var cursor = 0L
        while (cursor <= lastStart) {
            candidates.add(cursor)
            cursor += stride
        }
        candidates.add(lastStart)
        sceneEvents.forEach { event ->
            candidates.add((event.timeMs - targetMs / 3L).coerceIn(0L, lastStart))
            candidates.add((event.timeMs - targetMs / 2L).coerceIn(0L, lastStart))
        }
        val interesting = interestTerms(language)
        words.forEach { word ->
            if (interesting.any { term -> word.text.lowercase(Locale.ROOT).contains(term) }) {
                candidates.add((word.startMs - targetMs / 4L).coerceIn(0L, lastStart))
            }
        }

        val scored = candidates.map { start ->
            val end = (start + targetMs).coerceAtMost(durationMs)
            val segmentWords = words.filter { it.startMs < end && it.endMs > start }
            val segmentText = segmentWords.joinToString(" ") { it.text }
            val normalizedTokens = segmentWords.map { it.text.lowercase(Locale.ROOT).trim { !it.isLetterOrDigit() } }
            val keywordHits = normalizedTokens.count { token ->
                token.isNotBlank() && interesting.any { token.contains(it) }
            }
            val segmentScenes = sceneEvents.filter { it.timeMs in start..end }
            val sceneScore = segmentScenes.sumOf { (it.strength * 6.0).toDouble() }.toFloat().coerceAtMost(26f)
            val speechScore = (segmentWords.size * 0.48f).coerceAtMost(24f)
            val keywordScore = (keywordHits * 7f).coerceAtMost(28f)
            val wordVariety = (normalizedTokens.filter { it.length >= 4 }.distinct().size * 0.35f).coerceAtMost(8f)
            val boundaryScore = if (sceneEvents.any { abs(it.timeMs - start) < 1_500L }) 4f else 0f
            val segmentPeaks = if (audioPeaks.isEmpty()) emptyList() else {
                val firstPeak = ((start.toDouble() / durationMs.coerceAtLeast(1L)) * audioPeaks.size).toInt().coerceIn(0, audioPeaks.lastIndex)
                val lastPeak = ((((end.toDouble() / durationMs.coerceAtLeast(1L)) * audioPeaks.size).toInt() + 1).coerceIn(firstPeak + 1, audioPeaks.size))
                audioPeaks.subList(firstPeak, lastPeak)
            }
            val peakValue = segmentPeaks.maxOrNull() ?: 0f
            val peakScore = peakValue * 10f
            val energyAverage = if (segmentPeaks.isEmpty()) 0f else segmentPeaks.average().toFloat()
            val energyContrast = (peakValue - energyAverage).coerceAtLeast(0f) * 6f
            val speechPauses = segmentWords.zipWithNext().count { (left, right) -> right.startMs - left.endMs >= 450L }
            val pauseScore = speechPauses.coerceAtMost(3) * 1.5f
            val score = (speechScore + keywordScore + sceneScore + wordVariety + boundaryScore + peakScore + energyContrast + pauseScore).toInt()
            val reason = when {
                keywordHits > 0 && segmentScenes.isNotEmpty() -> "Key phrase + visual change"
                keywordHits > 0 && (peakScore + energyContrast) > 5f -> "Interesting speech + audio peak"
                keywordHits > 0 -> "Interesting speech detected"
                segmentScenes.size >= 3 && peakScore > 4f -> "Visual change + audio peak"
                segmentScenes.size >= 3 -> "High visual activity"
                segmentScenes.isNotEmpty() -> "Scene changes detected"
                segmentWords.size >= 8 -> "Speech-rich moment"
                peakScore > 7f -> "Audio peak detected"
                else -> "Suggested highlight window"
            }
            val relativeWords = segmentWords.mapNotNull { word ->
                val clippedStart = max(word.startMs, start)
                val clippedEnd = min(word.endMs, end)
                if (clippedEnd <= clippedStart) null
                else WordTiming(word.text, clippedStart - start, clippedEnd - start)
            }
            AutoClipSuggestion(
                startMs = start,
                endMs = end,
                score = score,
                reason = reason,
                transcriptExcerpt = segmentText.take(140),
                wordTimings = relativeWords
            )
        }.sortedWith(compareByDescending<AutoClipSuggestion> { it.score }.thenBy { it.startMs })

        // Return a handful of non-near-duplicate options, so the edit does not become repetitive.
        val selected = mutableListOf<AutoClipSuggestion>()
        val separation = (targetMs * 0.55f).toLong()
        for (candidate in scored) {
            if (selected.none { abs(it.startMs - candidate.startMs) < separation }) {
                selected.add(candidate)
            }
            if (selected.size >= 6) break
        }
        if (selected.isEmpty()) {
            selected.add(
                AutoClipSuggestion(
                    startMs = 0L,
                    endMs = targetMs,
                    score = 0,
                    reason = "First available section",
                    transcriptExcerpt = ""
                )
            )
        }
        onStatus("Offline analysis found " + selected.size + " highlight options. Review before adding them.")
        return selected.sortedBy { it.startMs }
    }

    private fun detectSceneChanges(
        context: Context,
        uri: Uri,
        sourceStartMs: Long,
        durationMs: Long,
        onStatus: (String) -> Unit
    ): List<SceneEvent> {
        val retriever = MediaMetadataRetriever()
        val events = mutableListOf<SceneEvent>()
        try {
            retriever.setDataSource(context, uri)
            val stepMs = max(900L, durationMs / 180L)
            var previous: IntArray? = null
            var previousTime = 0L
            var lastEventTime = -3_000L
            var sample = 0
            var timeMs = 0L
            while (timeMs < durationMs) {
                val frame = try {
                    retriever.getFrameAtTime((sourceStartMs + timeMs) * 1_000L, MediaMetadataRetriever.OPTION_CLOSEST)
                } catch (_: Exception) {
                    null
                }
                if (frame != null) {
                    val tiny = try {
                        Bitmap.createScaledBitmap(frame, 32, 18, false)
                    } catch (_: Exception) {
                        null
                    }
                    if (tiny != null) {
                        if (tiny !== frame) frame.recycle()
                        val pixels = IntArray(32 * 18)
                        try {
                            tiny.getPixels(pixels, 0, 32, 0, 0, 32, 18)
                        } finally {
                            tiny.recycle()
                        }
                        val prior = previous
                        if (prior != null) {
                            var total = 0L
                            for (i in pixels.indices) {
                                val a = pixels[i]
                                val b = prior[i]
                                total += abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF))
                                total += abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF))
                                total += abs((a and 0xFF) - (b and 0xFF))
                            }
                            val difference = total.toFloat() / (pixels.size * 3f * 255f)
                            if (difference >= 0.16f && timeMs - lastEventTime >= 1_600L) {
                                events.add(SceneEvent(timeMs, difference.coerceIn(0f, 1f)))
                                lastEventTime = timeMs
                            }
                        }
                        previous = pixels
                        previousTime = timeMs
                    } else {
                        frame.recycle()
                    }
                }
                sample++
                if (sample % 25 == 0) {
                    onStatus("Checked frame " + sample + " at " + (timeMs / 1_000L) + " seconds…")
                }
                timeMs += stepMs
            }
        } catch (_: Exception) {
            // Leave scene events empty; speech analysis can still supply suggestions.
        } finally {
            runCatching { retriever.release() }
        }
        return events
    }

    private fun interestTerms(language: CaptionLanguage): List<String> =
        if (language == CaptionLanguage.HINDI) {
            listOf(
                "क्यों", "कैसे", "लेकिन", "अचानक", "पहली", "सबसे", "सच", "रहस्य",
                "अविश्वसनीय", "खुलासा", "आखिर", "कभी", "दरअसल", "हैरान"
            )
        } else {
            listOf(
                "why", "how", "but", "suddenly", "first", "biggest", "truth", "secret",
                "unbelievable", "revealed", "finally", "never", "actually", "amazing",
                "impossible", "mystery", "discovered", "surprising", "remember", "because"
            )
        }
}
