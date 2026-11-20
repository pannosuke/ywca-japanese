package dev.ericallerton.yoruben.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dev.ericallerton.yoruben.core.ContentChecks
import dev.ericallerton.yoruben.core.Manifest
import dev.ericallerton.yoruben.core.ProgressFile
import dev.ericallerton.yoruben.core.StudyContent
import dev.ericallerton.yoruben.core.StudyJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ProgressStore(context: Context) {
    private val file = File(context.filesDir, "progress.json")

    fun load(profileId: String): ProgressFile {
        if (!file.exists()) return ProgressFile(profileId = profileId)
        return try {
            ProgressFile.decode(file.readText()).let { saved ->
                if (saved.profileId.isBlank()) saved.copy(profileId = profileId) else saved
            }
        } catch (_: Exception) {
            ProgressFile(profileId = profileId)
        }
    }

    fun save(progress: ProgressFile) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "progress.json.tmp")
        tmp.writeText(progress.encode())
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }
}

class ContentRepository(private val context: Context) {
    fun load(): StudyContent {
        val bundled = readBundled()
        val cached = File(context.filesDir, "content-cache")
        val remote = if (File(cached, "manifest.json").exists()) {
            try {
                StudyContent.load(cached)
            } catch (_: Exception) {
                null
            }
        } else null
        return if (remote != null && remote.manifest.version >= bundled.manifest.version) remote else bundled
    }

    suspend fun refresh(): StudyContent? = withContext(Dispatchers.IO) {
        try {
            val current = load()
            val manifestText = http("$RAW/manifest.json") ?: return@withContext null
            val manifest = StudyJson.decodeFromString<Manifest>(manifestText)
            if (manifest.schema != 1 || manifest.version < current.manifest.version) return@withContext null
            val tmp = File(context.filesDir, "content-incoming")
            tmp.deleteRecursively()
            tmp.mkdirs()
            File(tmp, "manifest.json").writeText(manifestText)
            manifest.files.forEach { name ->
                val body = http("$RAW/$name") ?: return@withContext null
                val out = File(tmp, name)
                out.parentFile?.mkdirs()
                out.writeText(body)
            }
            val loaded = StudyContent.load(tmp)
            if (ContentChecks.displayViolations(tmp, loaded.knownKanji).isNotEmpty()) return@withContext null
            val dest = File(context.filesDir, "content-cache")
            dest.deleteRecursively()
            if (!tmp.renameTo(dest)) return@withContext null
            StudyContent.load(dest)
        } catch (_: Exception) {
            null
        }
    }

    private fun readBundled(): StudyContent {
        val dest = File(context.cacheDir, "bundled-content")
        val marker = File(dest, "manifest.json")
        val manifestText = context.assets.open("manifest.json").bufferedReader().use { it.readText() }
        if (!marker.exists() || marker.readText() != manifestText) {
            dest.deleteRecursively()
            dest.mkdirs()
            marker.writeText(manifestText)
            val files = StudyJson.decodeFromString<Manifest>(manifestText).files
            files.forEach { name ->
                val out = File(dest, name)
                out.parentFile?.mkdirs()
                context.assets.open(name).use { input -> out.outputStream().use { input.copyTo(it) } }
            }
        }
        return StudyContent.load(dest)
    }

    private fun http(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
        }
        return try {
            if (conn.responseCode !in 200..299) null else conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val RAW = "https://raw.githubusercontent.com/pannosuke/ywca-japanese/main/content"
    }
}

fun shareProgress(context: Context, json: String) {
    val file = File(context.cacheDir, "yoruben-progress.json")
    file.writeText(json)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "よるべん")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share progress").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
