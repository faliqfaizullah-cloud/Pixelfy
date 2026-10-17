package com.pixelfy.app

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media as M
import androidx.compose.runtime.*
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
    var navStyle by mutableIntStateOf(2)
    var userName by mutableStateOf("You")
    var shuffle by mutableStateOf(false)
    var repeat by mutableIntStateOf(Player.REPEAT_MODE_OFF)

    fun init(c: Context) {
        if (::exo.isInitialized) return
        ctx = c.applicationContext
        liked = ctx.getSharedPreferences("pixelfy", 0).getStringSet("liked", emptySet())!!.map { it.toLong() }.toSet()
        theme = ctx.getSharedPreferences("pixelfy", 0).getInt("theme", 0)
        navStyle = ctx.getSharedPreferences("pixelfy", 0).getInt("navStyle", 2)
        userName = ctx.getSharedPreferences("pixelfy", 0).getString("userName", "You") ?: "You"
        exo = ExoPlayer.Builder(ctx)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
                    override fun onMediaItemTransition(m: MediaItem?, r: Int) { current = queue.firstOrNull { it.id.toString() == m?.mediaId } }
                })
            }
    }

    fun play(list: List<Song>, index: Int) {
        if (index !in list.indices) return
        // Start the media service so playback continues in background + lock screen controls appear.
        try { ctx.startService(Intent(ctx, PlaybackService::class.java)) } catch (_: Exception) {}
        queue = list; current = list[index]
        exo.setMediaItems(list.map { it.toItem() })
        exo.prepare(); exo.seekTo(index, 0); exo.play()
    }
    fun playLiked(all: List<Song>) { val l = all.filter { it.id in liked }; if (l.isNotEmpty()) play(l, 0) }
    fun toggle() { if (exo.isPlaying) exo.pause() else exo.play() }
    fun seekTo(ms: Long) = exo.seekTo(ms)
    fun chooseNavStyle(i: Int) { navStyle = i; ctx.getSharedPreferences("pixelfy", 0).edit().putInt("navStyle", i).apply() }
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
