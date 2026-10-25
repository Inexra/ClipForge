package com.clipforge.app

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.TreeMap
import kotlin.math.max
import kotlin.math.sqrt

data class BeatDetectionResult(val durationMs: Long, val beatMarkersMs: List<Long>)

private data class EnergyWindow(var sumSquares: Double = 0.0, var samples: Long = 0L) {
    fun add(value: Double) {
        sumSquares += value * value
        samples++
    }
    fun rms(): Float = if (samples == 0L) 0f else sqrt(sumSquares / samples).toFloat()
}

/**
 * Detects likely music transients from decoded audio amplitude energy. This is a local signal
 * heuristic—not a machine-learning genre/beat tracker—so results should be reviewed in the editor.
 */
object BeatDetectionEngine {
    private const val WINDOW_MS = 80L
    private const val MIN_SPACING_MS = 240L
    private const val MAX_MARKERS = 160

    fun detect(context: Context, uri: Uri, onStatus: (String) -> Unit): BeatDetectionResult {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        val windows = TreeMap<Long, EnergyWindow>()
        var durationUs = 0L
        try {
            extractor.setDataSource(context, uri, null)
            var audioTrack = -1
            var audioFormat: MediaFormat? = null
            for (track in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(track)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrack = track
                    audioFormat = format
                    break
                }
            }
            if (audioTrack < 0 || audioFormat == null) {
                throw IllegalArgumentException("The selected music file has no readable audio track.")
            }
            if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                durationUs = audioFormat.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(0L)
            }
            val mime = audioFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalStateException("Audio codec information is missing.")
            extractor.selectTrack(audioTrack)
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()

            onStatus("Decoding music locally to detect beat peaks…")
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            var decodedFrames = 0L
            var lastProgressSecond = -1L

            while (!outputEnded) {
                if (!inputEnded) {
                    val inputIndex = decoder.dequeueInputBuffer(10_000L)
                    if (inputIndex >= 0) {
                        val input = decoder.getInputBuffer(inputIndex)
                            ?: throw IllegalStateException("Audio decoder input buffer was unavailable.")
                        input.clear()
                        val sampleTimeUs = extractor.sampleTime
                        val sampleSize = if (sampleTimeUs < 0L || (durationUs > 0L && sampleTimeUs > durationUs)) {
                            -1
                        } else {
                            extractor.readSampleData(input, 0)
                        }
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, max(sampleTimeUs, 0L), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, sampleSize, sampleTimeUs, 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(info, 10_000L)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        if (info.size > 0) {
                            val decodedFormat = decoder.outputFormat
                            val sampleRate = decodedFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8_000)
                            val channelCount = decodedFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 8)
                            val pcmEncoding = if (decodedFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                                decodedFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                            } else {
                                AudioFormat.ENCODING_PCM_16BIT
                            }
                            val bytesPerSample = when (pcmEncoding) {
                                AudioFormat.ENCODING_PCM_FLOAT -> 4
                                AudioFormat.ENCODING_PCM_8BIT -> 1
                                else -> 2
                            }
                            val frameBytes = bytesPerSample * channelCount
                            val buffer = decoder.getOutputBuffer(outputIndex)
                            if (buffer != null && frameBytes > 0) {
                                val view = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                                view.position(info.offset)
                                view.limit(info.offset + info.size)
                                val frameCount = view.remaining() / frameBytes
                                for (frame in 0 until frameCount) {
                                    val frameOffset = info.offset + frame * frameBytes
                                    var channelSum = 0.0
                                    for (channel in 0 until channelCount) {
                                        val offset = frameOffset + channel * bytesPerSample
                                        val value = when (pcmEncoding) {
                                            AudioFormat.ENCODING_PCM_FLOAT -> view.getFloat(offset).toDouble().coerceIn(-1.0, 1.0)
                                            AudioFormat.ENCODING_PCM_8BIT -> ((view.get(offset).toInt() and 0xff) - 128) / 128.0
                                            else -> view.getShort(offset).toDouble() / 32768.0
                                        }
                                        channelSum += value
                                    }
                                    val mono = channelSum / channelCount
                                    val timeUs = info.presentationTimeUs + frame.toLong() * 1_000_000L / sampleRate
                                    val windowIndex = (timeUs.coerceAtLeast(0L) / 1_000L) / WINDOW_MS
                                    windows.getOrPut(windowIndex) { EnergyWindow() }.add(mono)
                                    decodedFrames++
                                }
                                val second = info.presentationTimeUs / 1_000_000L
                                if (second > lastProgressSecond) {
                                    lastProgressSecond = second
                                    if (second > 0L && second % 5L == 0L) {
                                        onStatus("Analyzed " + second + " seconds of music…")
                                    }
                                }
                                val decodedEnd = info.presentationTimeUs + frameCount.toLong() * 1_000_000L / sampleRate
                                durationUs = max(durationUs, decodedEnd)
                            }
                        }
                        val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(outputIndex, false)
                        if (eos) outputEnded = true
                    }
                }
            }

            if (windows.size < 4 || decodedFrames == 0L) {
                throw IllegalStateException("The music file was too short or couldn't be decoded.")
            }

            val energies = windows.values.map { it.rms() }
            val smoothed = FloatArray(energies.size) { index ->
                val from = max(0, index - 1)
                val to = minOf(energies.lastIndex, index + 1)
                energies.subList(from, to + 1).average().toFloat()
            }
            val averageEnergy = smoothed.average().toFloat()
            val threshold = max(0.035f, averageEnergy * 1.20f)
            val detected = mutableListOf<Long>()
            var lastMarkerMs = -MIN_SPACING_MS

            for (index in 1 until smoothed.lastIndex) {
                val level = smoothed[index]
                val isPeak = level >= threshold &&
                    level >= smoothed[index - 1] &&
                    level > smoothed[index + 1] &&
                    level >= averageEnergy * 1.08f
                if (!isPeak) continue
                val timestampMs = windows.keys.elementAt(index) * WINDOW_MS
                if (timestampMs - lastMarkerMs < MIN_SPACING_MS) {
                    if (detected.isNotEmpty() && level > smoothed.getOrElse(
                            ((lastMarkerMs / WINDOW_MS).toInt()).coerceIn(0, smoothed.lastIndex)
                        ) + 0.03f) {
                        detected[detected.lastIndex] = timestampMs
                        lastMarkerMs = timestampMs
                    }
                    continue
                }
                detected.add(timestampMs)
                lastMarkerMs = timestampMs
                if (detected.size >= MAX_MARKERS) break
            }

            onStatus("Found " + detected.size + " likely beat peaks. Review markers before editing.")
            return BeatDetectionResult(durationUs / 1_000L, detected)
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { extractor.release() }
        }
    }
}
