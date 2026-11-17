package com.pixelfy.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LyricLine(val ms: Long, val text: String)
class LyricsResult(val lines: List<LyricLine>, val synced: Boolean)

/** Time-synced lyrics from LRCLIB (free, no key). Cached on device so they work offline afterwards. */
object Lyrics {
    private val LRC = Regex("""\[(\d+):(\d+)(?:[.:](\d+))?]""")

    fun parse(text: String, synced: Boolean): LyricsResult? {
        val out = mutableListOf<LyricLine>()
        for (raw in text.lines()) {
            if (synced) {
                val ts = LRC.findAll(raw).toList(); if (ts.isEmpty()) continue
                val body = raw.replace(LRC, "").trim()
                for (m in ts) {
                    val frac = m.groupValues[3]
                    val f = if (frac.isEmpty()) 0L else frac.padEnd(3, '0').take(3).toLong()
                    out += LyricLine(m.groupValues[1].toLong() * 60000 + m.groupValues[2].toLong() * 1000 + f, body)
                }
            } else out += LyricLine(-1, raw.trim())
        }
        if (synced) out.sortBy { it.ms }
        return if (out.any { it.text.isNotBlank() }) LyricsResult(out, synced) else null
    }

    private fun http(url: String): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "Pixelfy/1.0"); c.connectTimeout = 8000; c.readTimeout = 8000
        if (c.responseCode == 200) c.inputStream.bufferedReader().use { it.readText() } else null
    } catch (_: Exception) { null }

    private fun JSONObject.str(k: String) = if (isNull(k)) "" else optString(k)

    suspend fun load(ctx: Context, s: Song, durMs: Long): LyricsResult? = withContext(Dispatchers.IO) {
        try {
            val dir = File(ctx.filesDir, "lyrics").apply { mkdirs() }
            val lrc = File(dir, "${s.id}.lrc"); val txt = File(dir, "${s.id}.txt")
            if (lrc.exists()) return@withContext parse(lrc.readText(), true)
            if (txt.exists()) return@withContext parse(txt.readText(), false)
            val t = URLEncoder.encode(s.title, "UTF-8"); val a = URLEncoder.encode(s.artist, "UTF-8")
            val d = if (durMs > 0) "&duration=${durMs / 1000}" else ""
            var o: JSONObject? = http("https://lrclib.net/api/get?track_name=$t&artist_name=$a$d")?.let { JSONObject(it) }
            if (o == null) {
                val arr = http("https://lrclib.net/api/search?track_name=$t&artist_name=$a")?.let { JSONArray(it) }
                if (arr != null) {
                    val all = (0 until arr.length()).map { arr.getJSONObject(it) }
                    o = all.firstOrNull { it.str("syncedLyrics").isNotBlank() } ?: all.firstOrNull { it.str("plainLyrics").isNotBlank() }
                }
            }
            if (o == null) return@withContext null
            val synced = o.str("syncedLyrics"); val plain = o.str("plainLyrics")
            when {
                synced.isNotBlank() -> { lrc.writeText(synced); parse(synced, true) }
                plain.isNotBlank() -> { txt.writeText(plain); parse(plain, false) }
                else -> null
            }
        } catch (_: Exception) { null }
    }
}
