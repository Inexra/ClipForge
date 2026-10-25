package com.clipforge.app

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** A timestamped word returned by the offline Vosk recognizer, relative to the selected clip's trim start. */
data class WordTiming(val text: String, val startMs: Long, val endMs: Long)

data class CaptionSource(val id: String, val uri: Uri, val startMs: Long, val endMs: Long)

/**
 * Offline English caption engine.
 *
 * The small English acoustic model is downloaded once (about 40 MB) and stored on-device.
 * After that first download, video audio is decoded and transcribed locally without sending it anywhere.
 */
object AutoCaptionEngine {
    private const val MODEL_NAME = "vosk-model-small-en-us-0.15"
    private const val MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
    private const val TARGET_RATE = 16_000
    private const val OUTPUT_SAMPLE_PERIOD_US = 1_000_000L / TARGET_RATE

    fun transcribeAll(
        context: Context,
        sources: List<CaptionSource>,
        onStatus: (String) -> Unit
    ): Map<String, List<WordTiming>> {
        require(sources.isNotEmpty()) { "Import a video first." }
        val modelDir = ensureModel(context, onStatus)
        onStatus("Loading offline English speech model…")
        val model = Model(modelDir.absolutePath)
        try {
            val result = linkedMapOf<String, List<WordTiming>>()
            sources.forEachIndexed { index, source ->
                onStatus("Transcribing clip ${index + 1} of ${sources.size} on this device…")
                result[source.id] = transcribeOne(context, source, model)
            }
            return result
        } finally {
            model.close()
        }
    }

    private fun transcribeOne(context: Context, source: CaptionSource, model: Model): List<WordTiming> {
        val endMs = source.endMs.coerceAtLeast(source.startMs + 1L)
        val recognizer = Recognizer(model, TARGET_RATE.toFloat())
        recognizer.setWords(true)
        val words = mutableListOf<WordTiming>()
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, source.uri, null)
            var audioTrack = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    audioFormat = format
                    break
                }
            }
            if (audioTrack < 0 || audioFormat == null) {
                throw IllegalArgumentException("This clip has no readable audio track.")
            }

            val mime = audioFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalArgumentException("Audio codec information is missing.")
            extractor.selectTrack(audioTrack)
            extractor.seekTo(source.startMs * 1_000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            var outputFormat: MediaFormat? = null
            val startUs = source.startMs * 1_000L
            val endUs = endMs * 1_000L
            var lastStatusBytes = 0L
            var fedBytes = 0L

            while (!outputEnded) {
                if (!inputEnded) {
                    val inputIndex = decoder.dequeueInputBuffer(10_000L)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex)
                            ?: throw IllegalStateException("Audio decoder input buffer was unavailable.")
                        inputBuffer.clear()
                        val sampleTime = extractor.sampleTime
                        val sampleSize = if (sampleTime < 0L || sampleTime > endUs) {
                            -1
                        } else {
                            extractor.readSampleData(inputBuffer, 0)
                        }
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputIndex, 0, 0, max(0L, sampleTime), MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputEnded = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10_000L)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outputFormat = decoder.outputFormat
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        if (bufferInfo.size > 0) {
                            val currentFormat = outputFormat ?: decoder.outputFormat
                            val rate = currentFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8_000)
                            val channels = currentFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 8)
                            val pcm = decoder.getOutputBuffer(outputIndex)
                            if (pcm != null) {
                                val mono16k = resampleToMono16k(
                                    pcm, bufferInfo, rate, channels, currentFormat, startUs, endUs
                                )
                                if (mono16k.isNotEmpty()) {
                                    recognizer.acceptWaveForm(mono16k, mono16k.size)
                                    fedBytes += mono16k.size
                                    if (fedBytes - lastStatusBytes >= TARGET_RATE * 2L * 15L) {
                                        lastStatusBytes = fedBytes
                                    }
                                }
                            }
                        }
                        val eos = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(outputIndex, false)
                        if (eos) outputEnded = true
                    }
                }
            }

            collectWordTimings(recognizer.finalResult(), words)
            return words.sortedBy { it.startMs }
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { extractor?.release() }
            runCatching { recognizer.close() }
        }
    }

    private fun resampleToMono16k(
        input: ByteBuffer,
        info: MediaCodec.BufferInfo,
        sampleRate: Int,
        channels: Int,
        format: MediaFormat,
        clipStartUs: Long,
        clipEndUs: Long
    ): ByteArray {
        val duplicate = input.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        duplicate.position(info.offset)
        duplicate.limit(info.offset + info.size)
        val encoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            format.getInteger(MediaFormat.KEY_PCM_ENCODING)
        } else {
            AudioFormat.ENCODING_PCM_16BIT
        }
        val bytesPerSample = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
        val frameBytes = channels * bytesPerSample
        val frames = duplicate.remaining() / frameBytes
        if (frames <= 0) return ByteArray(0)

        val startUs = info.presentationTimeUs
        val chunkEndUs = startUs + frames.toLong() * 1_000_000L / sampleRate
        var timestampUs = max(startUs, clipStartUs)
        val frameOffset = ceil((timestampUs - startUs).coerceAtLeast(0L) * sampleRate / 1_000_000.0).toInt()
        timestampUs = startUs + frameOffset.toLong() * 1_000_000L / sampleRate
        val out = ByteArrayOutputStream()

        while (timestampUs < chunkEndUs && timestampUs < clipEndUs) {
            val frameIndex = ((timestampUs - startUs) * sampleRate / 1_000_000L).toInt()
            if (frameIndex !in 0 until frames) break
            val framePosition = info.offset + frameIndex * frameBytes
            duplicate.position(framePosition)
            var sum = 0.0
            for (channel in 0 until channels) {
                val value = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) {
                    (duplicate.float.toDouble().coerceIn(-1.0, 1.0) * 32767.0).toInt()
                } else {
                    duplicate.short.toInt()
                }
                sum += value
            }
            val mono = (sum / channels).toInt().coerceIn(-32768, 32767)
            out.write(mono and 0xFF)
            out.write((mono shr 8) and 0xFF)
            timestampUs += OUTPUT_SAMPLE_PERIOD_US
        }
        return out.toByteArray()
    }

    private fun collectWordTimings(jsonText: String, destination: MutableList<WordTiming>) {
        runCatching {
            val root = JSONObject(jsonText)
            val array = root.optJSONArray("result") ?: return
            for (i in 0 until array.length()) {
                val word = array.optJSONObject(i) ?: continue
                val text = word.optString("word", "").trim()
                if (text.isBlank()) continue
                val start = (word.optDouble("start", 0.0) * 1_000.0).toLong().coerceAtLeast(0L)
                val end = (word.optDouble("end", start / 1_000.0) * 1_000.0).toLong().coerceAtLeast(start + 1L)
                destination.add(WordTiming(text, start, end))
            }
        }
    }

    private fun ensureModel(context: Context, onStatus: (String) -> Unit): File {
        val destination = File(context.filesDir, MODEL_NAME)
        if (File(destination, "am/final.mdl").exists() && File(destination, "conf/mfcc.conf").exists()) {
            return destination
        }

        destination.deleteRecursively()
        destination.mkdirs()
        val archive = File(context.cacheDir, "$MODEL_NAME.zip")
        val connection = URL(MODEL_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        try {
            onStatus("Downloading offline English caption model (about 40 MB)…")
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Model download failed (HTTP ${connection.responseCode}). Check your internet and retry.")
            }
            val total = connection.contentLengthLong
            var received = 0L
            connection.inputStream.use { input ->
                FileOutputStream(archive).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var count: Int
                    var nextReport = 4L * 1024 * 1024
                    while (input.read(buffer).also { count = it } >= 0) {
                        if (count == 0) continue
                        output.write(buffer, 0, count)
                        received += count
                        if (received >= nextReport) {
                            val progress = if (total > 0) " (${received / (1024 * 1024)} / ${total / (1024 * 1024)} MB)" else ""
                            onStatus("Downloading offline English caption model$progress…")
                            nextReport += 4L * 1024 * 1024
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        try {
            val root = destination.canonicalPath + File.separator
            ZipInputStream(BufferedInputStream(FileInputStream(archive))).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    var relative = entry.name.removePrefix("$MODEL_NAME/")
                    if (relative == entry.name && entry.name == "$MODEL_NAME") relative = ""
                    if (relative.isNotBlank()) {
                        val target = File(destination, relative)
                        if (!target.canonicalPath.startsWith(root)) {
                            throw IllegalStateException("Invalid path found in caption model archive.")
                        }
                        if (entry.isDirectory) target.mkdirs() else {
                            target.parentFile?.mkdirs()
                            FileOutputStream(target).use { out -> zip.copyTo(out) }
                        }
                    }
                    zip.closeEntry()
                }
            }
        } finally {
            archive.delete()
        }
        if (!File(destination, "am/final.mdl").exists() || !File(destination, "conf/mfcc.conf").exists()) {
            destination.deleteRecursively()
            throw IllegalStateException("The caption model archive was incomplete. Try again.")
        }
        return destination
    }
}
