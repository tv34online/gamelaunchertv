package ru.tv34.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Обои из r/wallpapers: берём случайную горизонтальную картинку из топа и кэшируем на 6 часов. */
object WallpaperRepo {
    private const val UA = "android:ru.tv34.launcher:v1.0 (by /u/tv34launcher)"
    private const val MAX_AGE = 6 * 60 * 60 * 1000L
    private val FEEDS = listOf(
        "https://www.reddit.com/r/wallpapers/top.json?t=day&limit=50&raw_json=1",
        "https://www.reddit.com/r/wallpapers/top.json?t=week&limit=100&raw_json=1",
    )

    suspend fun load(ctx: Context): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(ctx.filesDir, "wallpaper.jpg")
        val stale = !file.exists() || System.currentTimeMillis() - file.lastModified() > MAX_AGE
        if (stale) runCatching { refresh(file) }
        runCatching { decode(file) }.getOrNull()
    }

    private fun refresh(target: File) {
        for (feed in FEEDS) {
            val urls = runCatching { candidates(feed) }.getOrDefault(emptyList())
            for (u in urls.shuffled().take(3)) {
                if (runCatching { download(u, target) }.getOrDefault(false)) return
            }
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 25_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", UA)
        }

    private fun candidates(feed: String): List<String> {
        val conn = open(feed)
        try {
            if (conn.responseCode != 200) return emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val children = JSONObject(body).getJSONObject("data").getJSONArray("children")
            val out = mutableListOf<String>()
            for (i in 0 until children.length()) {
                val d = children.getJSONObject(i).getJSONObject("data")
                if (d.optBoolean("over_18")) continue
                val url = d.optString("url")
                val low = url.lowercase()
                if (!low.contains("redd.it")) continue
                if (!(low.endsWith(".jpg") || low.endsWith(".jpeg") || low.endsWith(".png"))) continue
                val src = d.optJSONObject("preview")?.optJSONArray("images")
                    ?.optJSONObject(0)?.optJSONObject("source")
                val w = src?.optInt("width") ?: 0
                val h = src?.optInt("height") ?: 0
                if (w >= 1920 && w > h) out.add(url)
            }
            return out
        } finally {
            conn.disconnect()
        }
    }

    private fun download(url: String, target: File): Boolean {
        val tmp = File(target.parentFile, "wallpaper.tmp")
        val conn = open(url)
        try {
            if (conn.responseCode != 200) return false
            conn.inputStream.use { input -> tmp.outputStream().use { out -> input.copyTo(out) } }
        } finally {
            conn.disconnect()
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(tmp.path, bounds)
        if (bounds.outWidth <= 0) { tmp.delete(); return false }
        target.delete()
        return tmp.renameTo(target)
    }

    private fun decode(file: File): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 1920) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
