package com.clipforge.app

import android.content.Context
import android.media.AudioFormat
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Timeline-aligned speech cue. The timestamps are project timeline milliseconds. */
data class NarrationCue(val startMs: Long, val endMs: Long, val text: String)

private data class WavPcm(val sampleRate: Int, val monoPcm16: ShortArray)

/**
 * Converts edited caption cues to a timestamped narration WAV using Android's installed TTS engine.
 * It does not call a cloud API. Locale data must be installed for the chosen language.
 */
object CaptionTtsNarrator {
    suspend fun generate(
        context: Context,
        cues: List<NarrationCue>,
        languageId: String,
        durationMs: Long,
        onStatus: (String) -> Unit
    ): File {
        val cleanCues = cues.filter { it.endMs > it.startMs && it.text.isNotBlank() }
            .sortedBy { it.startMs }
        require(cleanCues.isNotEmpty()) { "There are no non-empty caption lines to speak." }
        require(durationMs > 0L && durationMs <= 30L * 60L * 1_000L) {
            "TTS narration is limited to projects up to 30 minutes at once."
        }

        val locale = if (languageId == "hindi") Locale("hi", "IN") else Locale.US
        val engine = createEngine(context)
        val workDir = File(context.cacheDir, "clipforge-tts-" + UUID.randomUUID().toString())
        workDir.mkdirs()
        try {
            val languageResult = engine.isLanguageAvailable(locale)
            if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                throw IllegalStateException(
                    "The device's speech engine has no " + locale.displayLanguage +
                        " voice data. Install that language in Android Text-to-Speech settings and retry."
                )
            }
            val setLanguageResult = engine.setLanguage(locale)
            if (setLanguageResult == TextToSpeech.LANG_MISSING_DATA || setLanguageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                throw IllegalStateException("The selected speech language is not available on this phone.")
            }
            engine.setSpeechRate(1.0f)
            engine.setPitch(1.0f)

            val synthesized = mutableListOf<Pair<NarrationCue, WavPcm>>()
            cleanCues.forEachIndexed { index, cue ->
                onStatus("Generating narration " + (index + 1) + " of " + cleanCues.size + "…")
                val phraseFile = File(workDir, "phrase_" + index + ".wav")
                val utteranceId = UUID.randomUUID().toString()
                val completed = CompletableDeferred<File>()
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) = Unit

                    override fun onDone(id: String?) {
                        if (id == utteranceId && !completed.isCompleted) completed.complete(phraseFile)
                    }

                    override fun onError(id: String?) {
                        if (id == utteranceId && !completed.isCompleted) {
                            completed.completeExceptionally(IllegalStateException("The speech engine could not speak a caption line."))
                        }
                    }

                    override fun onError(id: String?, errorCode: Int) {
                        if (id == utteranceId && !completed.isCompleted) {
                            completed.completeExceptionally(IllegalStateException("Speech synthesis failed (code " + errorCode + ")."))
                        }
                    }
                })

                val accepted = engine.synthesizeToFile(
                    cue.text.take(TextToSpeech.getMaxSpeechInputLength() - 1),
                    Bundle(),
                    phraseFile,
                    utteranceId
                )
                if (accepted == TextToSpeech.ERROR) {
                    throw IllegalStateException("The speech engine rejected a caption line.")
                }
                completed.await()
                val pcm = withContext(Dispatchers.IO) { readWav(phraseFile) }
                synthesized.add(cue to pcm)
            }

            onStatus("Arranging narration against the caption timestamps…")
            return withContext(Dispatchers.IO) {
                combineAtTimestamps(context, synthesized, durationMs)
            }
        } finally {
            engine.shutdown()
            workDir.deleteRecursively()
        }
    }

    private suspend fun createEngine(context: Context): TextToSpeech =
        suspendCancellableCoroutine { continuation ->
            var engine: TextToSpeech? = null
            engine = TextToSpeech(context.applicationContext) { status ->
                val instance = engine
                if (!continuation.isActive) {
                    instance?.shutdown()
                } else if (status == TextToSpeech.SUCCESS && instance != null) {
                    continuation.resume(instance)
                } else {
                    instance?.shutdown()
                    continuation.resumeWithException(IllegalStateException("Android's text-to-speech engine did not initialize."))
                }
            }
            continuation.invokeOnCancellation { engine?.shutdown() }
        }

    private fun readWav(file: File): WavPcm {
        val bytes = file.readBytes()
        if (bytes.size < 44 || ascii(bytes, 0, 4) != "RIFF" || ascii(bytes, 8, 4) != "WAVE") {
            throw IllegalStateException("The installed speech engine returned an unsupported audio file.")
        }
        var position = 12
        var audioFormat = 0
        var channels = 0
        var sampleRate = 0
        var bits = 0
        var dataOffset = -1
        var dataLength = 0
        while (position + 8 <= bytes.size) {
            val chunkName = ascii(bytes, position, 4)
            val chunkSize = unsignedInt(bytes, position + 4).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val dataStart = position + 8
            if (dataStart + chunkSize > bytes.size) break
            when (chunkName) {
                "fmt " -> {
                    if (chunkSize >= 16) {
                        audioFormat = unsignedShort(bytes, dataStart)
                        channels = unsignedShort(bytes, dataStart + 2)
                        sampleRate = unsignedInt(bytes, dataStart + 4).toInt()
                        bits = unsignedShort(bytes, dataStart + 14)
                    }
                }
                "data" -> {
                    dataOffset = dataStart
                    dataLength = chunkSize
                    break
                }
            }
            position = dataStart + chunkSize + (chunkSize and 1)
        }
        if (dataOffset < 0 || channels !in 1..2 || sampleRate !in 8_000..192_000 || bits !in listOf(8, 16, 24, 32)) {
            throw IllegalStateException("The installed speech engine returned an unsupported WAV format.")
        }
        if (audioFormat != 1 && !(audioFormat == 3 && bits == 32)) {
            throw IllegalStateException("Only PCM-based speech output is supported on this device.")
        }

        val bytesPerSample = bits / 8
        val frameBytes = bytesPerSample * channels
        val frameCount = dataLength / frameBytes
        val mono = ShortArray(frameCount)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (frame in 0 until frameCount) {
            var total = 0L
            for (channel in 0 until channels) {
                val offset = dataOffset + frame * frameBytes + channel * bytesPerSample
                val value = if (audioFormat == 3) {
                    (buffer.getFloat(offset).coerceIn(-1f, 1f) * 32767f).toInt()
                } else {
                    when (bits) {
                        8 -> ((bytes[offset].toInt() and 0xff) - 128) shl 8
                        16 -> buffer.getShort(offset).toInt()
                        24 -> {
                            var sample = (bytes[offset].toInt() and 0xff) or
                                ((bytes[offset + 1].toInt() and 0xff) shl 8) or
                                ((bytes[offset + 2].toInt() and 0xff) shl 16)
                            if (sample and 0x800000 != 0) sample = sample or -0x1000000
                            sample shr 8
                        }
                        else -> (buffer.getInt(offset) shr 16)
                    }
                }
                total += value
            }
            mono[frame] = (total / channels).coerceIn(-32768L, 32767L).toShort()
        }
        return WavPcm(sampleRate, mono)
    }

    private fun combineAtTimestamps(
        context: Context,
        synthesized: List<Pair<NarrationCue, WavPcm>>,
        durationMs: Long
    ): File {
        val outputRate = synthesized.first().second.sampleRate
        val totalSamples = ceil(durationMs * outputRate / 1_000.0).toLong()
        val dataBytesLong = totalSamples * 2L
        require(dataBytesLong <= Int.MAX_VALUE - 44L) { "Narration audio file would be too large." }
        val outputFile = File(context.filesDir, "ClipForge_narration_" + System.currentTimeMillis() + ".wav")
        RandomAccessFile(outputFile, "rw").use { output ->
            output.setLength(44L + dataBytesLong)
            output.seek(0)
            output.write(wavHeader(outputRate, dataBytesLong.toInt()))
            synthesized.forEach { (cue, source) ->
                val resampled = resample(source.monoPcm16, source.sampleRate, outputRate)
                val startSample = (cue.startMs.coerceAtLeast(0L) * outputRate / 1_000L).coerceAtMost(totalSamples)
                val count = min(resampled.size.toLong(), totalSamples - startSample).toInt()
                if (count <= 0) return@forEach
                val byteOffset = 44L + startSample * 2L
                val mixBytes = ByteArray(count * 2)
                output.seek(byteOffset)
                output.readFully(mixBytes)
                val mixing = ByteBuffer.wrap(mixBytes).order(ByteOrder.LITTLE_ENDIAN)
                for (index in 0 until count) {
                    val oldValue = mixing.getShort(index * 2).toInt()
                    val newValue = (oldValue + resampled[index].toInt()).coerceIn(-32768, 32767).toShort()
                    mixing.putShort(index * 2, newValue)
                }
                output.seek(byteOffset)
                output.write(mixBytes)
            }
        }
        return outputFile
    }

    private fun resample(input: ShortArray, sourceRate: Int, targetRate: Int): ShortArray {
        if (input.isEmpty()) return input
        if (sourceRate == targetRate) return input
        val outputCount = max(1, ceil(input.size * targetRate.toDouble() / sourceRate).toInt())
        return ShortArray(outputCount) { index ->
            val position = index.toDouble() * sourceRate / targetRate
            val left = position.toInt().coerceIn(0, input.lastIndex)
            val right = min(left + 1, input.lastIndex)
            val fraction = (position - left).toFloat()
            (input[left] * (1f - fraction) + input[right] * fraction).toInt().coerceIn(-32768, 32767).toShort()
        }
    }

    private fun wavHeader(sampleRate: Int, dataBytes: Int): ByteArray {
        val output = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        output.put("RIFF".toByteArray(Charsets.US_ASCII))
        output.putInt(36 + dataBytes)
        output.put("WAVE".toByteArray(Charsets.US_ASCII))
        output.put("fmt ".toByteArray(Charsets.US_ASCII))
        output.putInt(16)
        output.putShort(1.toShort())
        output.putShort(1.toShort())
        output.putInt(sampleRate)
        output.putInt(sampleRate * 2)
        output.putShort(2.toShort())
        output.putShort(16.toShort())
        output.put("data".toByteArray(Charsets.US_ASCII))
        output.putInt(dataBytes)
        return output.array()
    }

    private fun ascii(bytes: ByteArray, offset: Int, length: Int): String =
        String(bytes, offset, length, Charsets.US_ASCII)

    private fun unsignedShort(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)

    private fun unsignedInt(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xffL) or
            ((bytes[offset + 1].toLong() and 0xffL) shl 8) or
            ((bytes[offset + 2].toLong() and 0xffL) shl 16) or
            ((bytes[offset + 3].toLong() and 0xffL) shl 24)
}
