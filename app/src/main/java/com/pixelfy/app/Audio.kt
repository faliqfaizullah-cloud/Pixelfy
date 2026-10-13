package com.pixelfy.app

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

data class Song(val id: Long, val title: String, val artist: String, val albumId: Long, val uri: Uri) {
    val art: Uri get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
}

/** Automatically detects every music file on the device via MediaStore. */
fun scanSongs(ctx: Context): List<Song> {
    val out = mutableListOf<Song>()
    val M = MediaStore.Audio.Media
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
    var queue by mutableStateOf(emptyList<Song>())
    var current by mutableStateOf<Song?>(null)
    var playing by mutableStateOf(false)
    var liked by mutableStateOf(setOf<Long>())

    fun init(c: Context) {
        if (::exo.isInitialized) return
        ctx = c
        liked = c.getSharedPreferences("pixelfy", 0).getStringSet("liked", emptySet())!!.map { it.toLong() }.toSet()
        exo = ExoPlayer.Builder(c).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
                override fun onMediaItemTransition(m: MediaItem?, r: Int) { current = queue.getOrNull(exo.currentMediaItemIndex) }
            })
        }
    }

    fun play(list: List<Song>, index: Int) {
        if (index !in list.indices) return
        queue = list; current = list[index]
        exo.setMediaItems(list.map { MediaItem.fromUri(it.uri) })
        exo.prepare(); exo.seekTo(index, 0); exo.play()
    }
    fun playLiked(all: List<Song>) { val l = all.filter { it.id in liked }; if (l.isNotEmpty()) play(l, 0) }
    fun toggle() { if (exo.isPlaying) exo.pause() else exo.play() }
    fun next() = exo.seekToNextMediaItem()
    fun prev() = exo.seekToPreviousMediaItem()
    fun toggleLike(s: Song) {
        liked = if (s.id in liked) liked - s.id else liked + s.id
        ctx.getSharedPreferences("pixelfy", 0).edit().putStringSet("liked", liked.map { it.toString() }.toSet()).apply()
    }
}
