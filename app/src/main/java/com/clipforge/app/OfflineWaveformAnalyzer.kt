package com.clipforge.app

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

/** Compact peak envelope suitable for drawing an audio waveform without retaining decoded audio. */
object OfflineWaveformAnalyzer {
    fun analyze(context: Context, uri: Uri, bins: Int = 96): List<Float> {
        val count = bins.coerceIn(16, 180)
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        val peaks = FloatArray(count)
        var durationUs = 0L
        var framesSeen = 0L
        try {
            extractor.setDataSource(context, uri, null)
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
            if (audioTrack < 0 || audioFormat == null) return emptyList()
            if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                durationUs = audioFormat.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(0L)
            }
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: return emptyList()
            extractor.selectTrack(audioTrack)
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            while (!outputEnded) {
                if (!inputEnded) {
                    val inputIndex = decoder.dequeueInputBuffer(10_000L)
                    if (inputIndex >= 0) {
                        val input = decoder.getInputBuffer(inputIndex)
                            ?: throw IllegalStateException("Audio decoder input buffer unavailable.")
                        input.clear()
                        val pts = extractor.sampleTime
                        val size = if (pts < 0L || (durationUs > 0L && pts > durationUs)) -1
                            else extractor.readSampleData(input, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, max(0L, pts), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, size, pts, 0)
                            extractor.advance()
                        }
                    }
                }
                when (val outputIndex = decoder.dequeueOutputBuffer(info, 10_000L)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER, MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                    else -> if (outputIndex >= 0) {
                        if (info.size > 0) {
                            val format = decoder.outputFormat
                            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8_000)
                            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 8)
                            val encoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING))
                                format.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                            val bytesPerSample = when (encoding) {
                                AudioFormat.ENCODING_PCM_FLOAT -> 4
                                AudioFormat.ENCODING_PCM_8BIT -> 1
                                else -> 2
                            }
                            val frameBytes = channels * bytesPerSample
                            val buffer = decoder.getOutputBuffer(outputIndex)
                            if (buffer != null && frameBytes > 0) {
                                val view = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                                view.position(info.offset)
                                view.limit(info.offset + info.size)
                                val frameCount = view.remaining() / frameBytes
                                val chunkDurationUs = frameCount.toLong() * 1_000_000L / sampleRate
                                if (durationUs <= 0L) durationUs = max(durationUs, info.presentationTimeUs + chunkDurationUs)
                                for (frame in 0 until frameCount) {
                                    val frameUs = info.presentationTimeUs + frame.toLong() * 1_000_000L / sampleRate
                                    val bucket = ((frameUs.coerceAtLeast(0L) * count) / durationUs.coerceAtLeast(1L))
                                        .toInt().coerceIn(0, count - 1)
                                    var sum = 0f
                                    val base = info.offset + frame * frameBytes
                                    for (channel in 0 until channels) {
                                        val sampleOffset = base + channel * bytesPerSample
                                        val value = when (encoding) {
                                            AudioFormat.ENCODING_PCM_FLOAT -> view.getFloat(sampleOffset).coerceIn(-1f, 1f)
                                            AudioFormat.ENCODING_PCM_8BIT -> ((view.get(sampleOffset).toInt() and 0xFF) - 128) / 128f
                                            else -> view.getShort(sampleOffset) / 32768f
                                        }
                                        sum += abs(value)
                                    }
                                    peaks[bucket] = max(peaks[bucket], sum / channels)
                                    framesSeen++
                                }
                                durationUs = max(durationUs, info.presentationTimeUs + chunkDurationUs)
                            }
                        }
                        val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(outputIndex, false)
                        if (eos) outputEnded = true
                    }
                }
            }
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { extractor.release() }
        }
        if (framesSeen == 0L) return emptyList()
        val maximum = peaks.maxOrNull()?.coerceAtLeast(0.001f) ?: 0.001f
        return peaks.map { (it / maximum).coerceIn(0f, 1f) }
    }
}
