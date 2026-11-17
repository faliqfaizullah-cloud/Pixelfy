@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.pixelfy.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.media3.common.util.BitmapLoader
import androidx.media3.datasource.DataSourceBitmapLoader
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/** Loads the ORIGINAL embedded album art (full resolution) of a song for the lock screen / notification background. */
class ArtLoader(private val ctx: Context) : BitmapLoader {
    private val delegate = DataSourceBitmapLoader(ctx)
    private val exec = Executors.newSingleThreadExecutor()

    override fun supportsMimeType(mimeType: String) = true
    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = delegate.decodeBitmap(data)
    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        if (uri.scheme == "content" && uri.authority == "media")
            Futures.submit(Callable { original(uri) ?: throw IllegalStateException("No artwork") }, exec)
        else delegate.loadBitmap(uri)

    private fun original(uri: Uri): Bitmap? {
        try {
            val r = MediaMetadataRetriever()
            r.setDataSource(ctx, uri)
            val b = r.embeddedPicture; r.release()
            if (b != null) {
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(b, 0, b.size, o)
                var s = 1; while (maxOf(o.outWidth, o.outHeight) / s > 1400) s *= 2
                BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = s })?.let { return it }
            }
        } catch (_: Exception) {}
        if (Build.VERSION.SDK_INT >= 29) try { return ctx.contentResolver.loadThumbnail(uri, Size(1080, 1080), null) } catch (_: Exception) {}
        return null
    }
}
