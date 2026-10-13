package com.pixelfy.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

val Green = Color(0xFF1ED760); val Bg = Color(0xFF0E0C0C); val TileBg = Color(0xFF2A2A2A); val Grey = Color(0xFFB3B3B3)

// ---------- Haptics ----------
fun View.buzz(strong: Boolean = false) {
    performHapticFeedback(
        if (Build.VERSION.SDK_INT >= 30) { if (strong) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CLOCK_TICK }
        else HapticFeedbackConstants.VIRTUAL_KEY
    )
}
@Composable fun Modifier.tap(strong: Boolean = false, onClick: () -> Unit): Modifier {
    val v = LocalView.current
    return clickable { v.buzz(strong); onClick() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Audio.init(applicationContext)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Green, background = Bg)) { Root() } }
    }
}

@Composable fun Root() {
    val ctx = LocalContext.current
    var songs by remember { mutableStateOf(emptyList<Song>()) }
    var tab by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(perm) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }   // auto-detect new songs when app returns
    LaunchedEffect(granted, tick) { if (granted) songs = withContext(Dispatchers.IO) { scanSongs(ctx) } }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1B3A22), Bg), endY = 900f))) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> Home(songs)
                1 -> SearchScreen(songs)
                2 -> LibraryScreen(songs)
                else -> CreateScreen(songs.size) { tick++ }
            }
        }
        MiniPlayer()
        NavBar(tab) { tab = it }
    }
}

// ---------- Components ----------
@Composable fun Art(s: Song, mod: Modifier, shape: Shape = RoundedCornerShape(6.dp)) {
    Box(mod.clip(shape).background(Color(0xFF333333)), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.MusicNote, null, tint = Color.Gray)
        AsyncImage(model = s.art, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable fun Burst(size: Dp) {
    Canvas(Modifier.size(size)) {
        val p = Path(); val n = 12; val r = this.size.minDimension / 2
        for (i in 0 until n * 2) {
            val rad = if (i % 2 == 0) r else r * 0.84f; val a = Math.PI * i / n
            val x = center.x + rad * cos(a).toFloat(); val y = center.y + rad * sin(a).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close(); drawPath(p, Green)
    }
}

@Composable fun Chip(t: String, sel: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(if (sel) Green else TileBg).tap { onClick() }.padding(horizontal = 14.dp, vertical = 7.dp)) {
        Text(t, color = if (sel) Color.Black else Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable fun Tile(title: String, modifier: Modifier, lead: @Composable () -> Unit, onClick: () -> Unit) {
    Row(modifier.height(56.dp).clip(RoundedCornerShape(8.dp)).background(TileBg).tap { onClick() }, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) { lead() }
        Text(title, Modifier.padding(horizontal = 8.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable fun Sec(t: String) = Text(t, Modifier.padding(start = 16.dp, top = 22.dp, bottom = 12.dp), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)

@Composable fun IconBtn(icon: ImageVector, strong: Boolean = false, tint: Color = Color.White, onClick: () -> Unit) =
    Icon(icon, null, tint = tint, modifier = Modifier.size(42.dp).clip(CircleShape).tap(strong) { onClick() }.padding(9.dp))

@Composable fun SongRow(s: Song, all: List<Song>) {
    Row(Modifier.fillMaxWidth().tap { Audio.play(all, all.indexOf(s)) }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Art(s, Modifier.size(50.dp), RoundedCornerShape(4.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(s.title, color = if (Audio.current?.id == s.id) Green else Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(s.artist, color = Grey, fontSize = 13.sp, maxLines = 1)
        }
        val liked = s.id in Audio.liked
        IconBtn(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, tint = if (liked) Green else Grey) { Audio.toggleLike(s) }
    }
}

// ---------- Screens ----------
@Composable fun Home(songs: List<Song>) {
    var chip by remember { mutableIntStateOf(0) }
    val shown = if (chip == 2) emptyList() else songs
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(TileBg), Alignment.Center) { Text("J", color = Green, fontWeight = FontWeight.Bold) }
                listOf("All", "Music", "Podcasts").forEachIndexed { i, t -> Chip(t, chip == i) { chip = i } }
            }
        }
        if (shown.isEmpty()) item { Text(if (chip == 2) "No podcasts yet" else "Grant storage permission and add music files to your phone", Modifier.padding(24.dp), color = Grey) }
        else {
            item {
                Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf<Song?>(null) + shown.take(5)).chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { s ->
                                if (s == null) Tile("Liked Songs", Modifier.weight(1f), { Burst(30.dp) }) { Audio.playLiked(shown) }
                                else Tile(s.title, Modifier.weight(1f), { Art(s, Modifier.fillMaxSize(), RoundedCornerShape(0.dp)) }) { Audio.play(shown, shown.indexOf(s)) }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            item { Sec("Recently Added") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(shown.take(15)) { s ->
                        Column(Modifier.width(150.dp).tap { Audio.play(shown, shown.indexOf(s)) }) {
                            Art(s, Modifier.size(150.dp))
                            Text(s.title, Modifier.padding(top = 8.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(s.artist, color = Grey, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            item { Sec("Jump Back In") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(shown.drop(5).ifEmpty { shown }.take(15)) { s ->
                        Column(Modifier.width(110.dp).tap { Audio.play(shown, shown.indexOf(s)) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Art(s, Modifier.size(110.dp), CircleShape)
                            Text(s.title, Modifier.padding(top = 8.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable fun SearchScreen(songs: List<Song>) {
    var q by remember { mutableStateOf("") }
    Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Search", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        TextField(q, { q = it }, Modifier.fillMaxWidth().padding(vertical = 14.dp), singleLine = true, shape = RoundedCornerShape(8.dp),
            placeholder = { Text("Songs or artists") }, leadingIcon = { Icon(Icons.Rounded.Search, null) },
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black, unfocusedTextColor = Color.Black, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
        val r = songs.filter { it.title.contains(q, true) || it.artist.contains(q, true) }
        LazyColumn { items(r) { SongRow(it, r) } }
    }
}

@Composable fun LibraryScreen(songs: List<Song>) {
    Column(Modifier.statusBarsPadding().padding(top = 12.dp)) {
        Text("Your Library", Modifier.padding(horizontal = 16.dp), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text("${songs.size} songs on this device", Modifier.padding(16.dp, 4.dp, 16.dp, 8.dp), color = Grey)
        LazyColumn { items(songs) { SongRow(it, songs) } }
    }
}

@Composable fun CreateScreen(count: Int, rescan: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Burst(72.dp)
        Text("Pixelfy", Modifier.padding(top = 12.dp), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text("$count songs detected", color = Grey, modifier = Modifier.padding(vertical = 8.dp))
        val v = LocalView.current
        Button(onClick = { v.buzz(true); rescan() }, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black)) { Text("Rescan music files", fontWeight = FontWeight.Bold) }
    }
}

@Composable fun MiniPlayer() {
    val s = Audio.current ?: return
    val liked = s.id in Audio.liked
    Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF6B4A38), Color(0xFF2A1A14)))).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Art(s, Modifier.size(40.dp), RoundedCornerShape(4.dp))
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(s.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(s.artist, color = Grey, fontSize = 12.sp, maxLines = 1)
        }
        IconBtn(Icons.Rounded.SkipPrevious) { Audio.prev() }
        IconBtn(if (liked) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline, tint = if (liked) Green else Color.White) { Audio.toggleLike(s) }
        IconBtn(if (Audio.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, strong = true) { Audio.toggle() }
        IconBtn(Icons.Rounded.SkipNext) { Audio.next() }
    }
}

@Composable fun NavBar(sel: Int, onSelect: (Int) -> Unit) {
    val items = listOf("Home" to Icons.Rounded.Home, "Search" to Icons.Rounded.Search, "Your Library" to Icons.Rounded.LibraryMusic, "Create" to Icons.Rounded.Add)
    Row(Modifier.fillMaxWidth().background(Color(0xFF1A1412)).navigationBarsPadding().padding(vertical = 8.dp)) {
        items.forEachIndexed { i, (label, icon) ->
            Column(Modifier.weight(1f).tap { onSelect(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = if (i == sel) Color.White else Grey, modifier = Modifier.size(26.dp))
                Text(label, color = if (i == sel) Color.White else Grey, fontSize = 11.sp, fontWeight = if (i == sel) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
