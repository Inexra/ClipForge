package com.clipforge.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * A portable ClipForge project bundle. It includes edit metadata and copies of all source
 * media referenced by the project, so the project can be transferred between devices.
 */
object ProjectArchive {
    private const val PREFS_NAME = "clipforge_project_v2"
    private const val PROJECT_ENTRY = "project.json"
    private const val PACKAGE_URI_PREFIX = "package:"

    fun export(context: Context, destination: Uri) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val projectJson = JSONObject()
        prefs.all.forEach { (key, value) ->
            if (value != null) projectJson.put(key, value)
        }

        val output = context.contentResolver.openOutputStream(destination)
            ?: throw IllegalStateException("Could not open the project bundle destination.")
        output.use { raw ->
            ZipOutputStream(raw.buffered()).use { zip ->
                val copied = mutableMapOf<String, String>()

                fun addSource(original: String?, requestedName: String): String? {
                    if (original.isNullOrBlank()) return original
                    val existing = copied[original]
                    if (existing != null) return PACKAGE_URI_PREFIX + existing
                    val uri = Uri.parse(original)
                    val name = safeMediaName(context, uri, requestedName)
                    val entryName = "media/" + UUID.randomUUID().toString() + "_" + name
                    val input = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Could not read source media: " + name)
                    zip.putNextEntry(ZipEntry(entryName))
                    input.use { it.copyTo(zip, 64 * 1024) }
                    zip.closeEntry()
                    copied[original] = entryName
                    return PACKAGE_URI_PREFIX + entryName
                }

                val clips = JSONArray(projectJson.optString("clips", "[]"))
                for (index in 0 until clips.length()) {
                    val clip = clips.getJSONObject(index)
                    val original = clip.optString("uri", "")
                    clip.put("uri", addSource(original, "clip_" + (index + 1) + ".mp4"))
                }
                projectJson.put("clips", clips.toString())

                val photoLayers = JSONArray(projectJson.optString("photoLayers", "[]"))
                for (index in 0 until photoLayers.length()) {
                    val layer = photoLayers.getJSONObject(index)
                    val original = layer.optString("uri", "")
                    layer.put("uri", addSource(original, "overlay_" + (index + 1) + ".png"))
                }
                projectJson.put("photoLayers", photoLayers.toString())

                projectJson.optString("music", "").takeIf { it.isNotBlank() && it != "null" }?.let { source ->
                    projectJson.put("music", addSource(source, "background_music.audio"))
                }
                projectJson.optString("voiceover", "").takeIf { it.isNotBlank() && it != "null" }?.let { source ->
                    projectJson.put("voiceover", addSource(source, "voiceover.audio"))
                }

                zip.putNextEntry(ZipEntry(PROJECT_ENTRY))
                zip.write(projectJson.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    fun import(context: Context, source: Uri) {
        val root = File(context.filesDir, "ClipForgeProjects/import_" + System.currentTimeMillis())
        if (root.exists()) root.deleteRecursively()
        if (!root.mkdirs()) throw IllegalStateException("Could not create the project workspace.")

        try {
            val input = context.contentResolver.openInputStream(source)
                ?: throw IllegalStateException("Could not open the selected project archive.")
            var totalBytes = 0L
            input.use { raw ->
                ZipInputStream(raw.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val target = File(root, entry.name)
                        val rootPrefix = root.canonicalPath + File.separator
                        if (!target.canonicalPath.startsWith(rootPrefix) && target.canonicalPath != root.canonicalPath) {
                            throw IllegalStateException("The project archive contains an invalid path.")
                        }
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            target.outputStream().use { out ->
                                val buffer = ByteArray(64 * 1024)
                                var size: Int
                                while (zip.read(buffer).also { size = it } >= 0) {
                                    if (size == 0) continue
                                    totalBytes += size
                                    if (totalBytes > 8L * 1024L * 1024L * 1024L) {
                                        throw IllegalStateException("The project bundle exceeds the 8 GB import limit.")
                                    }
                                    out.write(buffer, 0, size)
                                }
                            }
                        }
                        zip.closeEntry()
                    }
                }
            }

            val projectFile = File(root, PROJECT_ENTRY)
            if (!projectFile.exists()) throw IllegalArgumentException("This ZIP isn't a ClipForge project bundle.")
            val json = JSONObject(projectFile.readText(Charsets.UTF_8))

            fun restoreUri(value: String?): String? {
                if (value.isNullOrBlank() || value == "null") return null
                if (!value.startsWith(PACKAGE_URI_PREFIX)) return value
                val relative = value.removePrefix(PACKAGE_URI_PREFIX)
                val target = File(root, relative)
                val rootPrefix = root.canonicalPath + File.separator
                if (!target.canonicalPath.startsWith(rootPrefix) || !target.isFile) {
                    throw IllegalStateException("A bundled source media file is missing: " + relative)
                }
                return Uri.fromFile(target).toString()
            }

            val clips = JSONArray(json.optString("clips", "[]"))
            for (index in 0 until clips.length()) {
                val clip = clips.getJSONObject(index)
                clip.put("uri", restoreUri(clip.optString("uri", "")))
            }
            json.put("clips", clips.toString())

            val layers = JSONArray(json.optString("photoLayers", "[]"))
            for (index in 0 until layers.length()) {
                val layer = layers.getJSONObject(index)
                layer.put("uri", restoreUri(layer.optString("uri", "")))
            }
            json.put("photoLayers", layers.toString())
            json.put("music", restoreUri(json.optString("music", "")))
            json.put("voiceover", restoreUri(json.optString("voiceover", "")))

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit().clear()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                when (key) {
                    "brightness", "saturation", "sourceVolume", "musicVolume", "voiceoverVolume", "captionScale" ->
                        editor.putFloat(key, json.optDouble(key, 0.0).toFloat())
                    "exportFrameRate" -> editor.putInt(key, json.optInt(key, 30))
                    "mute" -> editor.putBoolean(key, json.optBoolean(key, false))
                    else -> {
                        val value = json.opt(key)
                        if (value is String) editor.putString(key, value)
                    }
                }
            }
            if (!editor.commit()) throw IllegalStateException("Could not save the imported project.")
        } catch (error: Exception) {
            root.deleteRecursively()
            throw error
        }
    }

    private fun safeMediaName(context: Context, uri: Uri, fallback: String): String {
        val providerName = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        val candidate = (providerName ?: uri.lastPathSegment ?: fallback)
            .substringAfterLast('/')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(90)
        return candidate.ifBlank { fallback }
    }
}
