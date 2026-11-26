package com.linguacoach.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.linguacoach.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** A newer release found on GitHub. */
data class UpdateInfo(
    val versionCode: Int,
    val name: String,
    val notes: String,
    val apiAssetUrl: String,   // api.github.com asset url (works for private repos with token)
    val browserUrl: String,    // public download url
)

/**
 * Over-the-air updates from GitHub Releases. The release tag must be "v<versionCode>"
 * (e.g. v3) and carry the app-debug.apk asset. Works with a public repo (no token) or
 * a private repo (optional read-only token).
 */
class UpdateManager {

    // Change these if the repository moves.
    private val owner = "robertobissoli-star"
    private val repo = "linguacoach"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Returns an [UpdateInfo] if the latest release is newer than the installed build. */
    suspend fun check(token: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .apply { if (token.isNotBlank()) header("Authorization", "Bearer $token") }
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val obj = JSONObject(resp.body?.string().orEmpty())
            val tag = obj.optString("tag_name").removePrefix("v")
            val latest = tag.toIntOrNull() ?: return@withContext null
            if (latest <= BuildConfig.VERSION_CODE) return@withContext null
            val assets = obj.optJSONArray("assets") ?: return@withContext null
            for (i in 0 until assets.length()) {
                val a = assets.optJSONObject(i) ?: continue
                if (a.optString("name").endsWith(".apk")) {
                    return@withContext UpdateInfo(
                        versionCode = latest,
                        name = obj.optString("name").ifBlank { "Versione v$latest" },
                        notes = obj.optString("body").take(500),
                        apiAssetUrl = a.optString("url"),
                        browserUrl = a.optString("browser_download_url"),
                    )
                }
            }
            null
        }
    }

    /** Download the APK to cache and launch the system installer. */
    suspend fun downloadAndInstall(context: Context, info: UpdateInfo, token: String) {
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val out = File(dir, "linguacoach-v${info.versionCode}.apk")
            // Private repos require the API asset url + octet-stream + token; public repos use browserUrl.
            val url = if (token.isNotBlank()) info.apiAssetUrl else info.browserUrl
            val req = Request.Builder().url(url)
                .apply {
                    if (token.isNotBlank()) {
                        header("Authorization", "Bearer $token")
                        header("Accept", "application/octet-stream")
                    }
                }
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw RuntimeException("Download fallito (HTTP ${resp.code}).")
                resp.body?.byteStream()?.use { input -> out.outputStream().use { input.copyTo(it) } }
                    ?: throw RuntimeException("Risposta vuota.")
            }
            out
        }
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun currentVersionName(): String = BuildConfig.VERSION_NAME
    fun currentVersionCode(): Int = BuildConfig.VERSION_CODE
}
