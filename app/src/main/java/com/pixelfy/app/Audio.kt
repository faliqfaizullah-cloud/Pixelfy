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
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ln
import kotlin.random.Random
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

data class UserPlaylist(val id: Long, val name: String, val songIds: List<Long>)

data class Song(val id: Long, val title: String, val artist: String, val albumId: Long, val uri: Uri, val kind: Int = 0) {
    val art: Uri get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
    fun toItem(): MediaItem = MediaItem.Builder().setMediaId(id.toString()).setUri(uri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setArtworkUri(uri).build()).build()
}

/** Automatically detects every music file on the device via MediaStore. */
fun scanSongs(ctx: Context): List<Song> {
    val out = mutableListOf<Song>()
    val flags = if (android.os.Build.VERSION.SDK_INT >= 29) "${M.IS_MUSIC}!=0 OR ${M.IS_PODCAST}!=0 OR ${M.IS_AUDIOBOOK}!=0" else "${M.IS_MUSIC}!=0 OR ${M.IS_PODCAST}!=0"
    val proj = arrayOf(M._ID, M.TITLE, M.ARTIST, M.ALBUM_ID, M.IS_MUSIC)
    ctx.contentResolver.query(M.EXTERNAL_CONTENT_URI, proj, "($flags) AND ${M.DURATION}>30000", null, "${M.DATE_ADDED} DESC")?.use { c ->
        while (c.moveToNext()) {
            val id = c.getLong(0)
            out += Song(id, c.getString(1) ?: "Unknown", c.getString(2)?.takeIf { it != "<unknown>" } ?: "Unknown artist",
                c.getLong(3), ContentUris.withAppendedId(M.EXTERNAL_CONTENT_URI, id), if (c.getInt(4) != 0) 0 else 1)
        }
    }
    return out
}

/** Genre name -> ids of the songs in it (used for genre mixes). */
fun scanGenres(ctx: Context): Map<String, List<Long>> {
    val out = mutableMapOf<String, List<Long>>()
    try {
        ctx.contentResolver.query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val gid = c.getLong(0); val name = c.getString(1)?.trim().orEmpty()
                if (name.isBlank()) continue
                val ids = mutableListOf<Long>()
                ctx.contentResolver.query(MediaStore.Audio.Genres.Members.getContentUri("external", gid), arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID), null, null, null)?.use { m ->
                    while (m.moveToNext()) ids += m.getLong(0)
                }
                if (ids.isNotEmpty()) out[name] = ids
            }
        }
    } catch (_: Exception) {}
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
        startStatsTicker()
        liked = ctx.getSharedPreferences("pixelfy", 0).getStringSet("liked", emptySet())!!.map { it.toLong() }.toSet()
        theme = ctx.getSharedPreferences("pixelfy", 0).getInt("theme", 0)
        navStyle = ctx.getSharedPreferences("pixelfy", 0).getInt("navStyle4", 3)
        accent = ctx.getSharedPreferences("pixelfy", 0).getInt("accent", 0)
        uiStyle = ctx.getSharedPreferences("pixelfy", 0).getInt("uiStyle", 0)
        userName = ctx.getSharedPreferences("pixelfy", 0).getString("userName", "You") ?: "You"
        loadPlaylists(ctx.getSharedPreferences("pixelfy", 0).getString("playlists", "[]") ?: "[]")
        bgType = ctx.getSharedPreferences("pixelfy", 0).getInt("bgType", 0)
        bgUri = ctx.getSharedPreferences("pixelfy", 0).getString("bgUri", "") ?: ""
        bgDim = ctx.getSharedPreferences("pixelfy", 0).getFloat("bgDim", 0.45f)
        showLabels = ctx.getSharedPreferences("pixelfy", 0).getBoolean("labels", false)
        listen = parseLongs(ctx.getSharedPreferences("pixelfy", 0).getString("listen", "") ?: "")
        lastPlayed = parseLongs(ctx.getSharedPreferences("pixelfy", 0).getString("last", "") ?: "")
        avatarUri = ctx.getSharedPreferences("pixelfy", 0).getString("avatar", "") ?: ""
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
    fun chooseUiStyle(i: Int) { uiStyle = i; ctx.getSharedPreferences("pixelfy", 0).edit().putInt("uiStyle", i).apply(); if (i == 1 || i == 4) chooseNavStyle(3) }
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
        lastPlayed = lastPlayed + (id to System.currentTimeMillis()); saveStats()
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
    // ---------------- playlists, background, labels ----------------
    var playlists by mutableStateOf(listOf<UserPlaylist>())
    var pickSong by mutableStateOf<Song?>(null)             // song waiting to be added to a playlist (shows the picker)
    var bgType by mutableIntStateOf(0)                      // 0 = theme, 1 = picture, 2 = video
    var bgUri by mutableStateOf("")
    var bgDim by mutableFloatStateOf(0.45f)
    var showLabels by mutableStateOf(false)
    private fun prefs() = ctx.getSharedPreferences("pixelfy", 0)
    private fun savePlaylists() {
        val arr = JSONArray()
        playlists.forEach { p -> arr.put(JSONObject().put("id", p.id).put("name", p.name).put("songs", JSONArray(p.songIds))) }
        prefs().edit().putString("playlists", arr.toString()).apply()
    }
    private fun loadPlaylists(s: String) {
        try {
            val arr = JSONArray(s)
            playlists = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i); val sa = o.getJSONArray("songs")
                UserPlaylist(o.getLong("id"), o.getString("name"), (0 until sa.length()).map { sa.getLong(it) })
            }
        } catch (_: Exception) {}
    }
    fun createPlaylist(name: String): Long { val id = System.currentTimeMillis(); playlists = playlists + UserPlaylist(id, name, emptyList()); savePlaylists(); return id }
    fun renamePlaylist(id: Long, name: String) { playlists = playlists.map { if (it.id == id) it.copy(name = name) else it }; savePlaylists() }
    fun deletePlaylist(id: Long) { playlists = playlists.filter { it.id != id }; savePlaylists() }
    fun addToPlaylist(id: Long, songId: Long) { playlists = playlists.map { if (it.id == id && songId !in it.songIds) it.copy(songIds = it.songIds + songId) else it }; savePlaylists() }
    fun removeFromPlaylist(id: Long, songId: Long) { playlists = playlists.map { if (it.id == id) it.copy(songIds = it.songIds - songId) else it }; savePlaylists() }
    fun moveInPlaylist(id: Long, from: Int, to: Int) {
        playlists = playlists.map {
            if (it.id == id && from in it.songIds.indices && to in it.songIds.indices) it.copy(songIds = it.songIds.toMutableList().apply { add(to, removeAt(from)) }) else it
        }
        savePlaylists()
    }
    fun chooseBackground(type: Int, uri: String) { bgType = type; bgUri = uri; prefs().edit().putInt("bgType", type).putString("bgUri", uri).apply() }
    fun saveDim(v: Float) { bgDim = v; prefs().edit().putFloat("bgDim", v).apply() }
    fun chooseLabels(on: Boolean) { showLabels = on; prefs().edit().putBoolean("labels", on).apply() }
    // ---------------- stats, avatar, extra libraries ----------------
    var listen by mutableStateOf(mapOf<Long, Long>())            // seconds actually listened, per song
    var lastPlayed by mutableStateOf(mapOf<Long, Long>())        // when each song was last started (epoch ms)
    var extras by mutableStateOf(listOf<Song>())                 // podcasts and audiobooks
    var genres by mutableStateOf(mapOf<String, List<Long>>())
    var avatarUri by mutableStateOf("")
    private var dirty = false
    private var statTick = 0
    private fun parseLongs(s: String): Map<Long, Long> = s.split(',').mapNotNull { p ->
        p.split(':').takeIf { it.size == 2 }?.let { (x, y) -> x.toLongOrNull()?.let { id -> y.toLongOrNull()?.let { id to it } } }
    }.toMap()
    private fun encode(m: Map<Long, Long>) = m.entries.joinToString(",") { "${it.key}:${it.value}" }
    private fun saveStats() { prefs().edit().putString("listen", encode(listen)).putString("last", encode(lastPlayed)).apply(); dirty = false }
    private fun startStatsTicker() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (exo.isPlaying) {
                    current?.let { c -> listen = listen + (c.id to ((listen[c.id] ?: 0L) + 1L)); dirty = true }
                    statTick++; if (statTick % 15 == 0 && dirty) saveStats()
                }
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }
    fun chooseAvatar(u: String) { avatarUri = u; prefs().edit().putString("avatar", u).apply() }
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
