package com.pixelfy.app

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media as M
import android.media.audiofx.Equalizer
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import kotlin.math.abs
import kotlin.math.ln
import kotlin.random.Random
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

data class Song(val id: Long, val title: String, val artist: String, val albumId: Long, val uri: Uri) {
    val art: Uri get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
    fun toItem(): MediaItem = MediaItem.Builder().setMediaId(id.toString()).setUri(uri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setArtworkUri(uri).build()).build()
}

/** Automatically detects every music file on the device via MediaStore. */
fun scanSongs(ctx: Context): List<Song> {
    val out = mutableListOf<Song>()
    val proj = arrayOf(M._ID, M.TITLE, M.ARTIST, M.ALBUM_ID)
    ctx.contentResolver.query(M.EXTERNAL_CONTENT_URI, proj,
        "${M.IS_MUSIC}!=0 AND ${M.DURATION}>30000", null, "${M.DATE_ADDED} DESC")?.use { c ->
        while (c.moveToNext()) {
            val id = c.getLong(0)
            out += Song(id, c.getString(1) ?: "Unknown", c.getString(2)?.takeIf { it != "<unknown>" } ?: "Unknown artist",
                c.getLong(3), ContentUris.withAppendedId(M.EXTERNAL_CONTENT_URI, id))
        }
    }
    return out
}

object Audio {
    private lateinit var exo: ExoPlayer
    private lateinit var ctx: Context
    val player: ExoPlayer get() = exo
    var queue by mutableStateOf(emptyList<Song>())
    var current by mutableStateOf<Song?>(null)
    var playing by mutableStateOf(false)
    var liked by mutableStateOf(setOf<Long>())
    var theme by mutableIntStateOf(0)
    var navStyle by mutableIntStateOf(3)
    var accent by mutableIntStateOf(0)
    var uiStyle by mutableIntStateOf(0)
    var userName by mutableStateOf("You")
    var shuffle by mutableStateOf(false)
    var repeat by mutableIntStateOf(Player.REPEAT_MODE_OFF)

    fun init(c: Context) {
        if (::exo.isInitialized) return
        ctx = c.applicationContext
        liked = ctx.getSharedPreferences("pixelfy", 0).getStringSet("liked", emptySet())!!.map { it.toLong() }.toSet()
        theme = ctx.getSharedPreferences("pixelfy", 0).getInt("theme", 0)
        navStyle = ctx.getSharedPreferences("pixelfy", 0).getInt("navStyle4", 3)
        accent = ctx.getSharedPreferences("pixelfy", 0).getInt("accent", 0)
        uiStyle = ctx.getSharedPreferences("pixelfy", 0).getInt("uiStyle", 0)
        userName = ctx.getSharedPreferences("pixelfy", 0).getString("userName", "You") ?: "You"
        plays = parsePlays(ctx.getSharedPreferences("pixelfy", 0).getString("plays", "") ?: "")
        exo = ExoPlayer.Builder(ctx)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
                    override fun onMediaItemTransition(m: MediaItem?, r: Int) {
                        current = queue.firstOrNull { it.id.toString() == m?.mediaId }
                        if (r != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) current?.let { bump(it.id) }
                        if (sleepEnd == -1L && r == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) { exo.pause(); sleepEnd = 0L }
                    }
                })
            }
    }

    fun play(list: List<Song>, index: Int) {
        if (index !in list.indices) return
        // Start the media service so playback continues in background + lock screen controls appear.
        try { ctx.startService(Intent(ctx, PlaybackService::class.java)) } catch (_: Exception) {}
        queue = list; current = list[index]; bump(list[index].id)
        exo.setMediaItems(list.map { it.toItem() })
        exo.prepare(); exo.seekTo(index, 0); exo.play()
    }
    fun playLiked(all: List<Song>) { val l = all.filter { it.id in liked }; if (l.isNotEmpty()) play(l, 0) }
    fun toggle() { if (exo.isPlaying) exo.pause() else exo.play() }
    fun seekTo(ms: Long) = exo.seekTo(ms)
    fun chooseNavStyle(i: Int) { navStyle = i; ctx.getSharedPreferences("pixelfy", 0).edit().putInt("navStyle4", i).apply() }
    fun chooseUiStyle(i: Int) { uiStyle = i; ctx.getSharedPreferences("pixelfy", 0).edit().putInt("uiStyle", i).apply(); if (i == 1) chooseNavStyle(3) }
    fun chooseAccent(i: Int) { accent = i; ctx.getSharedPreferences("pixelfy", 0).edit().putInt("accent", i).apply() }
    // ---------------- extra music features ----------------
    var plays by mutableStateOf(mapOf<Long, Int>())          // how many times each song was played
    var sleepEnd by mutableLongStateOf(0L)                   // 0 = off, -1 = end of current song, otherwise stop time (ms)
    var speed by mutableFloatStateOf(1f)
    var eqPreset by mutableIntStateOf(0)
    var wantLyrics by mutableStateOf(false)
    private val handler = Handler(Looper.getMainLooper())
    private var sleepTask: Runnable? = null
    private var eq: Equalizer? = null

    private fun parsePlays(s: String): Map<Long, Int> = s.split(',').mapNotNull { p ->
        p.split(':').takeIf { it.size == 2 }?.let { (x, y) -> x.toLongOrNull()?.let { id -> y.toIntOrNull()?.let { id to it } } }
    }.toMap()
    private fun bump(id: Long) {
        plays = plays + (id to ((plays[id] ?: 0) + 1))
        ctx.getSharedPreferences("pixelfy", 0).edit().putString("plays", plays.entries.joinToString(",") { "${it.key}:${it.value}" }).apply()
    }

    fun sleepIn(min: Int) {
        cancelSleep(); sleepEnd = System.currentTimeMillis() + min * 60_000L
        val t = Runnable { fadeAndPause() }; sleepTask = t; handler.postDelayed(t, min * 60_000L)
    }
    fun sleepAtEnd() { cancelSleep(); sleepEnd = -1L }
    fun cancelSleep() { sleepTask?.let { handler.removeCallbacks(it) }; sleepTask = null; sleepEnd = 0L; exo.volume = 1f }
    private fun fadeAndPause() {
        var step = 0
        handler.post(object : Runnable {
            override fun run() {
                step++; exo.volume = (1f - step / 15f).coerceAtLeast(0f)
                if (step < 15) handler.postDelayed(this, 200) else { exo.pause(); exo.volume = 1f; sleepEnd = 0L; sleepTask = null }
            }
        })
    }
    fun chooseSpeed(v: Float) { speed = v; exo.setPlaybackSpeed(v) }
    fun chooseEq(i: Int) {
        eqPreset = i
        try {
            if (eq == null) eq = Equalizer(0, exo.audioSessionId).also { it.enabled = true }
            val e = eq!!; val n = e.numberOfBands.toInt(); val hi = e.bandLevelRange[1].toInt(); val lo = e.bandLevelRange[0].toInt()
            for (b in 0 until n) {
                val pos = if (n > 1) b / (n - 1f) else 0f
                val g = when (i) { 1 -> (1f - pos) * (1f - pos); 2 -> (1f - abs(pos - 0.6f) * 2.5f).coerceAtLeast(0f); 3 -> pos * pos; else -> 0f }
                e.setBandLevel(b.toShort(), (g * hi * 0.8f).toInt().coerceIn(lo, hi).toShort())
            }
        } catch (_: Exception) {}
    }
    fun playNext(s: Song) {
        if (queue.isEmpty()) { play(listOf(s), 0); return }
        val at = exo.currentMediaItemIndex + 1
        exo.addMediaItem(at, s.toItem()); queue = queue.toMutableList().apply { add(at.coerceAtMost(size), s) }
    }
    fun addToQueue(s: Song) {
        if (queue.isEmpty()) { play(listOf(s), 0); return }
        exo.addMediaItem(s.toItem()); queue = queue + s
    }
    /** "Generate": a smart mix that favours liked, often-played and recently added songs (weighted random sampling). */
    fun smartMix(all: List<Song>): List<Song> {
        if (all.isEmpty()) return all
        val rnd = Random(System.currentTimeMillis()); val maxP = (plays.values.maxOrNull() ?: 1).coerceAtLeast(1)
        return all.mapIndexed { idx, s ->
            val w = 1.0 + (plays[s.id] ?: 0).toDouble() / maxP * 2.0 + (if (s.id in liked) 1.5 else 0.0) + (1.0 - idx.toDouble() / all.size) * 0.5
            s to (-ln(rnd.nextDouble().coerceAtLeast(1e-9)) / w)
        }.sortedBy { it.second }.map { it.first }.take(50)
    }
    fun saveName(n: String) { userName = n; ctx.getSharedPreferences("pixelfy", 0).edit().putString("userName", n).apply() }
    fun chooseTheme(i: Int) { theme = i; ctx.getSharedPreferences("pixelfy", 0).edit().putInt("theme", i).apply() }
    fun toggleShuffle() { shuffle = !shuffle; exo.shuffleModeEnabled = shuffle }
    fun cycleRepeat() {
        repeat = when (repeat) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }
        exo.repeatMode = repeat
    }
    fun next() = exo.seekToNextMediaItem()
    fun prev() = exo.seekToPreviousMediaItem()
    fun toggleLike(s: Song) {
        liked = if (s.id in liked) liked - s.id else liked + s.id
        ctx.getSharedPreferences("pixelfy", 0).edit().putStringSet("liked", liked.map { it.toString() }.toSet()).apply()
    }
}
