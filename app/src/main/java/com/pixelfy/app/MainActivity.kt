package com.pixelfy.app

import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.media.MediaMetadataRetriever
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.speech.RecognizerIntent
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import android.view.TextureView
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import android.widget.Toast
import androidx.compose.ui.graphics.drawscope.rotate
import android.util.Log
import java.io.File
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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.Crossfade
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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

data class AccentOpt(val name: String, val color: Color)
val AccentPalette = listOf(
    AccentOpt("White", Color.White), AccentOpt("Green", Color(0xFF1ED760)), AccentOpt("Blue", Color(0xFF4DA3FF)),
    AccentOpt("Purple", Color(0xFFB06BFF)), AccentOpt("Orange", Color(0xFFFF9A3D)), AccentOpt("Pink", Color(0xFFFF6FB5)), AccentOpt("Red", Color(0xFFFF5A5F)))
/** The theme (accent) colour chosen in Settings. The background always stays OLED black. */
val Green: Color get() = AccentPalette[Audio.accent.coerceIn(0, AccentPalette.lastIndex)].color
val GreenDark: Color get() = lerp(Green, Color.Black, 0.45f)
val Bg = Color.Black; val TileBg = Color(0xFF2A2A2A); val Grey = Color(0xFFB3B3B3)

// ---------- Glassmorphism ----------
fun Modifier.glass(shape: Shape = RoundedCornerShape(16.dp), a: Float = 0.08f): Modifier = this
    .clip(shape)
    .background(Brush.linearGradient(listOf(Color.White.copy(a + 0.07f), Color.White.copy(a * 0.35f))))
    .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.38f), Color.White.copy(0.04f), Color.White.copy(0.16f))), shape)

/** Pure OLED black with slowly drifting, glowing colour orbs. */
@Composable fun GlassBackdrop() {
    val tr = rememberInfiniteTransition(label = "glow")
    val a by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by tr.animateFloat(1f, 0f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        drawRect(Color.Black)
        fun orb(c: Color, x: Float, y: Float, r: Float) =
            drawCircle(Brush.radialGradient(listOf(c, c.copy(alpha = c.alpha * 0.35f), Color.Transparent), Offset(x, y), r), r, Offset(x, y))
        orb(Green.copy(0.42f), w * (0.10f + 0.18f * a), h * (0.08f + 0.06f * b), w * 0.9f)
        orb(Color(0xFF7B4DFF).copy(0.34f), w * (0.95f - 0.20f * b), h * (0.36f + 0.08f * a), w * 0.8f)
        orb(Color(0xFF14B8A6).copy(0.26f), w * (0.05f + 0.15f * b), h * (0.70f - 0.07f * a), w * 0.75f)
        orb(Color(0xFFFF6A3D).copy(0.20f), w * (0.90f - 0.15f * a), h * (0.92f - 0.05f * b), w * 0.7f)
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(0.35f)), Offset(w / 2, h / 2), h * 0.9f))
    }
}

// ---------- Haptics ----------
fun View.buzz(strong: Boolean = false) {
    performHapticFeedback(
        if (Build.VERSION.SDK_INT >= 30) { if (strong) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CLOCK_TICK }
        else HapticFeedbackConstants.VIRTUAL_KEY
    )
}
@Composable fun Modifier.tap(strong: Boolean = false, onClick: () -> Unit): Modifier {
    val v = LocalView.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.93f else 1f, spring(dampingRatio = 0.4f, stiffness = 600f), label = "press")
    return graphicsLayer { scaleX = sc; scaleY = sc }.clickable(interactionSource = src, indication = LocalIndication.current) { v.buzz(strong); onClick() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { th, e ->
            try { File(filesDir, "crash.txt").writeText(Log.getStackTraceString(e)) } catch (_: Exception) {}
            old?.uncaughtException(th, e)
        }
        enableEdgeToEdge()
        Audio.init(applicationContext)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Green, background = Bg)) { Root() } }
    }
}

@Composable fun Root() {
    val ctx = LocalContext.current
    var songs by remember { mutableStateOf(emptyList<Song>()) }
    var tab by remember { mutableIntStateOf(0) }
    var full by remember { mutableStateOf(false) }
    var sortAz by remember { mutableStateOf(false) }
    var screen by remember { mutableIntStateOf(0) }
    var splash by remember { mutableStateOf(true) }
    val glass = Audio.uiStyle == 2
    val red = Audio.uiStyle == 3
    var showTools by remember { mutableStateOf(false) }
    val tabIdx = if (red) tab.coerceAtMost(2) else if (Audio.navStyle == 3 || glass) tab.coerceAtMost(2) else tab
    val crashFile = remember { File(ctx.filesDir, "crash.txt") }
    var crash by remember { mutableStateOf(if (crashFile.exists()) crashFile.readText() else null) }
    crash?.let { txt ->
        AlertDialog(onDismissRequest = { crashFile.delete(); crash = null },
            confirmButton = { TextButton(onClick = { crashFile.delete(); crash = null }) { Text("Close") } },
            title = { Text("Pixelfy crashed last time") },
            text = { Text(txt.take(1200), fontSize = 11.sp) })
    }
    var tick by remember { mutableIntStateOf(0) }
    val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(perm) }
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(granted) {
        if (granted && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notif.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }   // auto-detect new songs when app returns
    LaunchedEffect(granted, tick) {
        if (granted) {
            val all = withContext(Dispatchers.IO) { scanSongs(ctx) }
            songs = all.filter { it.kind == 0 }; Audio.extras = all.filter { it.kind == 1 }
            Audio.genres = withContext(Dispatchers.IO) { scanGenres(ctx) }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
    if (Audio.bgType != 0) UserBackground() else if (red) RedAmbient()
    val libSongs = remember(songs, sortAz) { if (sortAz) songs.sortedBy { it.title.lowercase() } else songs }
    var barsH by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val content: @Composable () -> Unit = {
        AnimatedContent(tabIdx, transitionSpec = {
            val d = if (targetState > initialState) 1 else -1
            (slideInHorizontally(spring(stiffness = 300f)) { it / 6 * d } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally(spring(stiffness = 300f)) { -it / 6 * d } + fadeOut(tween(160)))
        }, label = "tab") { t ->
            Box(Modifier.fillMaxSize().padding(bottom = if (glass && t != 0) barsH else 0.dp)) {
                if (red) when (t) {
                    0 -> HomeGlass(songs, barsH) { screen = 1 }
                    1 -> LibraryRed(songs, barsH, { screen = 1 }) { full = true }
                    else -> PlaylistsRoot(songs, barsH, null)
                } else when (t) {
                    0 -> when (Audio.uiStyle) {
                        1 -> HomeDiscovery(songs) { screen = 1 }
                        4 -> HomeReco(songs) { screen = 1 }
                        2 -> HomeGlass(songs, barsH) { screen = 1 }
                        else -> Home(songs) { screen = 1 }
                    }
                    1 -> SearchScreen(songs)
                    2 -> LibraryScreen(libSongs, sortAz) { sortAz = !sortAz }
                    else -> CreateScreen(songs.size) { tick++ }
                }
            }
        }
    }
    val bars: @Composable () -> Unit = {
        MiniPlayer { full = true }
        if (red) NavBarRed(tabIdx, { tab = it }, {
            if (songs.isNotEmpty()) { val mix = Audio.smartMix(songs); Audio.play(mix, 0); Toast.makeText(ctx, "Smart mix: ${mix.size} songs", Toast.LENGTH_SHORT).show() }
        }, { showTools = true })
        else NavBar(tabIdx, { tab = it }) { i ->
            when (i) {
                0, 3 -> tick++
                1 -> if (songs.isNotEmpty()) Audio.play(songs.shuffled(), 0)
                else -> sortAz = !sortAz
            }
        }
    }
    if (glass || red) Box(Modifier.fillMaxSize()) {
        content()
        Column(Modifier.align(Alignment.BottomCenter).onSizeChanged { barsH = with(density) { it.height.toDp() } }) { bars() }
    } else Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { content() }
        bars()
    }
    AnimatedVisibility(full && Audio.current != null, enter = slideInVertically { it }, exit = slideOutVertically { it }) { NowPlaying { full = false } }
    AnimatedVisibility(screen >= 1, enter = slideInVertically { it }, exit = slideOutVertically { it }) { ProfileScreen(songs.size, { screen = 2 }, { screen = 3 }, { screen = 4 }) { screen = 0 } }
    AnimatedVisibility(screen == 2, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) { SettingsScreen({ tick++ }) { screen = 1 } }
    AnimatedVisibility(screen == 3, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) {
        Box(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()) { PlaylistsRoot(songs, 0.dp) { screen = 1 } }
    }
    AnimatedVisibility(screen == 4, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) { StatsScreen(songs) { screen = 1 } }
    AnimatedVisibility(showTools, enter = slideInVertically { it }, exit = slideOutVertically { it }) { ToolsSheet { showTools = false } }
    if (splash) Splash { splash = false }
    Audio.pickSong?.let { PlaylistPicker(it) { Audio.pickSong = null } }
    }
}

// ---------- Components ----------
@Composable fun Art(s: Song, mod: Modifier, shape: Shape = RoundedCornerShape(22.dp)) {
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
    Row(modifier.height(60.dp).clip(RoundedCornerShape(22.dp)).background(TileBg).tap { onClick() }, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) { lead() }
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
        IconBtn(Icons.Rounded.PlaylistAdd, tint = Grey) { Audio.pickSong = s }
        IconBtn(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, tint = if (liked) Green else Grey) { Audio.toggleLike(s) }
    }
}

// ---------- Screens ----------
@Composable fun Home(songs: List<Song>, onProfile: () -> Unit) {
    var chip by remember { mutableIntStateOf(0) }
    val shown = if (chip == 2) emptyList() else songs
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(TileBg).tap { onProfile() }, Alignment.Center) { AvatarContent(Green, 16.sp) }
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

@Composable fun LibraryScreen(songs: List<Song>, sortAz: Boolean, onSort: () -> Unit) {
    Column(Modifier.statusBarsPadding().padding(top = 12.dp)) {
        Text("Your Library", Modifier.padding(horizontal = 16.dp), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text("${songs.size} songs on this device", Modifier.padding(16.dp, 4.dp, 16.dp, 8.dp), color = Grey)
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { Chip(if (sortAz) "A–Z" else "Recently added", true) { onSort() } }
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

@Composable fun MiniPlayer(onOpen: () -> Unit) {
    val s = Audio.current ?: return
    val view = LocalView.current
    val liked = s.id in Audio.liked
    val shape = RoundedCornerShape(50)
    val glass = Audio.uiStyle >= 2
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    LaunchedEffect(s.id) { while (true) { pos = Audio.player.currentPosition; dur = Audio.player.duration.coerceAtLeast(0L); delay(500) } }
    // Glow: while music plays the blurred cover slowly flows and a coloured light drifts and pulses inside (and around) the bar.
    val art = rememberArtColor(s)
    val glowColor = lerp(art, Color.White, 0.2f)
    val live by animateFloatAsState(if (Audio.playing) 1f else 0f, tween(900), label = "live")
    val tr = rememberInfiniteTransition(label = "glow")
    val p1 by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(11000, easing = LinearEasing)), label = "p1")
    val p2 by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(17000, easing = LinearEasing)), label = "p2")
    val pulse by tr.animateFloat(0.55f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val tau = (2 * Math.PI).toFloat()
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).height(if (glass) 60.dp else 52.dp)) {
        if (Build.VERSION.SDK_INT >= 31) {
            Box(Modifier.matchParentSize().blur(22.dp, BlurredEdgeTreatment.Unbounded).drawBehind {
                val cx = size.width * (0.2f + 0.6f * (0.5f + 0.5f * sin(tau * p1)))
                drawRoundRect(Brush.radialGradient(listOf(glowColor.copy(alpha = 0.55f * pulse * live), Color.Transparent), Offset(cx, size.height / 2), size.width * 0.5f),
                    Offset(-8.dp.toPx(), -6.dp.toPx()), Size(size.width + 16.dp.toPx(), size.height + 12.dp.toPx()), CornerRadius(size.height))
            })
        }
        Box(Modifier.matchParentSize().clip(shape).background(Color(0xFF1B1B1B))
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.5f), Color.White.copy(0.08f), Color.White.copy(0.25f))), shape)
            .pointerInput(Unit) {
                var acc = 0f
                detectHorizontalDragGestures(
                    onDragStart = { acc = 0f },
                    onDragEnd = { if (acc < -90f) { Audio.next(); view.buzz(true) } else if (acc > 90f) { Audio.prev(); view.buzz(true) } }
                ) { _, d -> acc += d }
            }) {
            Crossfade(targetState = s, modifier = Modifier.matchParentSize(), animationSpec = tween(500), label = "mp") { song ->
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(model = song.art, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            val k = 1.7f + 0.2f * sin(tau * p1) * live
                            scaleX = k; scaleY = k
                            translationX = sin(tau * p1) * size.width * 0.18f * live
                            translationY = cos(tau * p2) * size.height * 0.5f * live
                            rotationZ = 14f * sin(tau * p2) * live
                        }.blur(36.dp))
                    AsyncImage(model = song.art, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            val k = 2.2f + 0.25f * cos(tau * p2) * live
                            scaleX = k; scaleY = k
                            translationX = -sin(tau * p2) * size.width * 0.22f * live
                            translationY = sin(tau * p1) * size.height * 0.5f * live
                            rotationZ = -18f * cos(tau * p1) * live
                            alpha = 0.55f
                        }.blur(30.dp))
                }
            }
            Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color.White.copy(0.10f), Color.Black.copy(0.36f)))))
            Box(Modifier.matchParentSize().drawBehind {
                val cx = size.width * (0.8f - 0.6f * (0.5f + 0.5f * sin(tau * p2)))
                drawRect(Brush.radialGradient(listOf(glowColor.copy(alpha = 0.40f * pulse * live), Color.Transparent), Offset(cx, size.height / 2), size.width * 0.45f), blendMode = BlendMode.Plus)
            })
            Row(Modifier.fillMaxSize().padding(start = 18.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(if (glass) 44.dp else 30.dp).clip(CircleShape).then(if (glass) Modifier.background(Color.White.copy(0.18f)) else Modifier).tap(true) { Audio.toggle() }, Alignment.Center) {
                    if (Audio.playing) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(4.dp, 17.dp).clip(RoundedCornerShape(1.dp)).background(Color.White))
                        Box(Modifier.size(4.dp, 17.dp).clip(RoundedCornerShape(1.dp)).background(Color.White))
                    } else Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp).tap { onOpen() }) {
                    Text(s.title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.artist, color = Color.White.copy(0.55f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.size(32.dp).clip(CircleShape).border(1.8.dp, if (liked) Green else Color.White, CircleShape).tap { Audio.toggleLike(s) }, Alignment.Center) {
                    Icon(if (liked) Icons.Rounded.Check else Icons.Rounded.Add, null, tint = if (liked) Green else Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Box(Modifier.align(if (glass) Alignment.TopStart else Alignment.BottomStart).padding(start = 22.dp, end = 22.dp, top = if (glass) 4.dp else 0.dp, bottom = if (glass) 0.dp else 4.dp).fillMaxWidth().height(2.dp)
                .clip(RoundedCornerShape(1.dp)).background(Color.White.copy(0.18f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f).background(Color.White))
            }
        }
    }
}

// ---------- Full-screen player (Wrapped-style themes) ----------
data class NpTheme(val bg: Color, val burst: Color, val diamond: Color, val fg: Color)
val NpThemes = listOf(
    NpTheme(Color(0xFF16D957), Color(0xFFE7FF3C), Color(0xFFF56EC6), Color(0xFF0B0B0B)),
    NpTheme(Color(0xFFF56EC6), Color(0xFFE7FF3C), Color(0xFFFF8A1E), Color(0xFF0B0B0B)),
    NpTheme(Color(0xFF6600B8), Color(0xFFE7FF3C), Color(0xFFFF8A1E), Color.White),
    NpTheme(Color(0xFFFF8A1E), Color(0xFFE7FF3C), Color(0xFF16D957), Color(0xFF0B0B0B)),
    NpTheme(Color.Black, Color(0xFF6600B8), Color(0xFFFF8A1E), Color.White)
)

@Composable fun Spikes(modifier: Modifier, n: Int, inner: Float, color: Color) {
    Canvas(modifier) {
        val p = Path(); val r = size.minDimension / 2
        for (i in 0 until n * 2) {
            val rad = if (i % 2 == 0) r else r * inner; val a = Math.PI * i / n
            val x = center.x + rad * cos(a).toFloat(); val y = center.y + rad * sin(a).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close(); drawPath(p, color)
    }
}

class LyricState(val res: LyricsResult?, val done: Boolean)

@Composable fun rememberLyrics(s: Song): LyricState {
    val ctx = LocalContext.current
    var res by remember(s.id) { mutableStateOf<LyricsResult?>(null) }
    var done by remember(s.id) { mutableStateOf(false) }
    LaunchedEffect(s.id) { res = Lyrics.load(ctx, s, Audio.player.duration.coerceAtLeast(0L)); done = true }
    return LyricState(res, done)
}

/** Mid-tone colour taken from the album cover, used as the lyrics background (like Spotify). */
@Composable fun rememberArtColor(s: Song): Color {
    val ctx = LocalContext.current
    val c by produceState(Color(0xFF3A5F6B), s.id) {
        try {
            val res = ctx.imageLoader.execute(ImageRequest.Builder(ctx).data(s.art).size(32).allowHardware(false).build())
            val bmp = (res as? SuccessResult)?.drawable?.toBitmap()
            if (bmp != null) {
                var r = 0L; var g = 0L; var b = 0L; var n = 0
                for (x in 0 until bmp.width step 2) for (y in 0 until bmp.height step 2) {
                    val px = bmp.getPixel(x, y)
                    r += android.graphics.Color.red(px); g += android.graphics.Color.green(px); b += android.graphics.Color.blue(px); n++
                }
                if (n > 0) {
                    val hsv = FloatArray(3)
                    android.graphics.Color.RGBToHSV((r / n).toInt(), (g / n).toInt(), (b / n).toInt(), hsv)
                    hsv[1] = (hsv[1] * 1.15f).coerceIn(0.25f, 0.85f); hsv[2] = hsv[2].coerceIn(0.38f, 0.60f)
                    value = Color(android.graphics.Color.HSVToColor(hsv))
                }
            }
        } catch (_: Exception) {}
    }
    return c
}

/** Bold lyric lines: sung lines are white, upcoming lines are dark. Tap a line to jump to it. */
@Composable fun LyricLines(lyr: LyricState, pos: Long, big: Boolean, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val r = lyr.res
    if (r == null) {
        Box(modifier.fillMaxSize(), Alignment.Center) {
            if (!lyr.done) LoadingBlob(Color.White)
            else Text("No lyrics found.\nConnect to the internet and try again.", color = Color.White.copy(0.8f), fontSize = 16.sp, textAlign = TextAlign.Center)
        }
        return
    }
    val idx = if (r.synced) r.lines.indexOfLast { it.ms <= pos } else -1
    val st = rememberLazyListState()
    LaunchedEffect(idx) { if (idx >= 0) st.animateScrollToItem(idx, -st.layoutInfo.viewportSize.height / 5) }
    LazyColumn(modifier, st, contentPadding = PaddingValues(vertical = if (big) 60.dp else 12.dp)) {
        items(r.lines.size) { i ->
            val l = r.lines[i]
            val target = when { !r.synced -> Color.White; i == idx -> Color.White; i < idx -> Color.White.copy(0.75f); else -> Color.Black.copy(0.38f) }
            val col by animateColorAsState(target, label = "lc")
            Text(l.text.ifBlank { if (r.synced) "♪" else " " },
                Modifier.fillMaxWidth().then(if (r.synced) Modifier.tap { onSeek(l.ms) } else Modifier).padding(vertical = if (big) 10.dp else 6.dp),
                color = col, fontSize = if (big) 30.sp else 22.sp, lineHeight = if (big) 36.sp else 28.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** Compact lyrics card shown inside the player (tap the expand icon for full screen). */
@Composable fun LyricsCard(s: Song, lyr: LyricState, pos: Long, onExpand: () -> Unit, onSeek: (Long) -> Unit, modifier: Modifier) {
    val bg by animateColorAsState(rememberArtColor(s), tween(600), label = "lbg")
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(bg)) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("LYRICS", Modifier.weight(1f), color = Color.White.copy(0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            IconBtn(Icons.Rounded.Fullscreen, tint = Color.White) { onExpand() }
        }
        LyricLines(lyr, pos, false, onSeek, Modifier.weight(1f).padding(horizontal = 18.dp))
    }
}

/** Full-screen lyrics: song header with close button, big lyrics, seek bar and a large play/pause button. */
@Composable fun LyricsFull(s: Song, lyr: LyricState, pos: Long, dur: Long, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val bg by animateColorAsState(rememberArtColor(s), tween(600), label = "fbg")
    val view = LocalView.current
    var drag by remember { mutableStateOf<Float?>(null) }
    Column(Modifier.fillMaxSize().background(bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Art(s, Modifier.size(44.dp), RoundedCornerShape(6.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(s.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(s.artist, color = Color.White.copy(0.75f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(0.35f)).tap { onClose() }, Alignment.Center) {
                Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        LyricLines(lyr, pos, true, { Audio.seekTo(it) }, Modifier.weight(1f).padding(horizontal = 22.dp))
        Column(Modifier.padding(horizontal = 20.dp)) {
            WavySeek(drag ?: (if (dur > 0) pos.toFloat() / dur else 0f), Audio.playing, Color.White, { drag = it },
                { drag?.let { Audio.seekTo((it * dur).toLong()) }; drag = null; view.buzz() })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmt(pos), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(fmt(dur), color = Color.White.copy(0.8f), fontSize = 12.sp)
            }
            Box(Modifier.align(Alignment.CenterHorizontally).padding(vertical = 14.dp).size(68.dp).clip(CircleShape).background(Color.White).tap(true) { Audio.toggle() }, Alignment.Center) {
                Icon(if (Audio.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = bg, modifier = Modifier.size(36.dp))
            }
        }
    }
}

fun fmt(ms: Long) = "%d:%02d".format(ms / 60000, (ms / 1000) % 60)

@Composable fun WideBtn(icon: ImageVector, modifier: Modifier, onClick: () -> Unit) =
    Box(modifier.fillMaxHeight().clip(RoundedCornerShape(26.dp)).background(Color.White.copy(0.16f)).tap { onClick() }, Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
    }

/** Full-screen player: full-bleed album art fading into a blurred, tinted background. */
@Composable fun NowPlaying(onClose: () -> Unit) {
    val s = Audio.current ?: return
    val view = LocalView.current
    BackHandler(onBack = onClose)
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var drag by remember { mutableStateOf<Float?>(null) }
    var showLyrics by remember { mutableStateOf(Audio.wantLyrics) }
    LaunchedEffect(Unit) { Audio.wantLyrics = false }
    var lyricsFull by remember { mutableStateOf(false) }
    val lyr = rememberLyrics(s)
    LaunchedEffect(s.id) { while (true) { pos = Audio.player.currentPosition; dur = Audio.player.duration.coerceAtLeast(0L); delay(200) } }
    val liked = s.id in Audio.liked
    val white = Color.White
    val lt = NpTheme(Color.Transparent, white, white, white)
    Box(Modifier.fillMaxSize().background(Color.Black)
        .pointerInput(Unit) {
            var total = 0f
            detectVerticalDragGestures(onDragStart = { total = 0f }, onDragEnd = { if (total > 180f) onClose() }) { _, d -> total += d }
        }) {
        // Animated full-screen cover: slow zoom + drift (Ken Burns) while playing, eases still when paused
        val tr = rememberInfiniteTransition(label = "cover")
        val zoom by tr.animateFloat(1.08f, 1.24f, infiniteRepeatable(tween(16000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "z")
        val dx by tr.animateFloat(-0.03f, 0.03f, infiniteRepeatable(tween(21000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dx")
        val dy by tr.animateFloat(-0.02f, 0.02f, infiniteRepeatable(tween(27000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dy")
        val live by animateFloatAsState(if (Audio.playing) 1f else 0f, tween(900), label = "live")
        Crossfade(targetState = s, animationSpec = tween(700), label = "art") { song ->
            val intro = remember { Animatable(1.18f) }
            LaunchedEffect(Unit) { intro.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3A3A3A), Color.Black)))) {
                AsyncImage(model = song.art, contentDescription = null,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        val k = (1.08f + (zoom - 1.08f) * live) * intro.value
                        scaleX = k; scaleY = k
                        translationX = dx * size.width * live; translationY = dy * size.height * live
                    }, contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    0f to Color.Black.copy(0.35f), 0.2f to Color.Transparent, 0.5f to Color.Transparent, 1f to Color.Black.copy(0.92f))))
            }
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Music", Modifier.weight(1f).padding(start = 4.dp), color = white, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                IconBtn(Icons.Rounded.PlaylistAdd, tint = white) { Audio.pickSong = s }
                IconBtn(Icons.Rounded.KeyboardArrowDown, tint = white) { onClose() }
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp)) {
                if (showLyrics) LyricsCard(s, lyr, pos, { lyricsFull = true }, { Audio.seekTo(it) }, Modifier.fillMaxSize())
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBtn(Icons.Rounded.Subject, tint = if (showLyrics) Green else white) { showLyrics = !showLyrics }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.title, color = white, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.artist, color = white.copy(0.65f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconBtn(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, tint = if (liked) Green else white) { Audio.toggleLike(s) }
            }
            WavySeek(drag ?: (if (dur > 0) pos.toFloat() / dur else 0f), Audio.playing, white, { drag = it },
                { drag?.let { Audio.seekTo((it * dur).toLong()) }; drag = null; view.buzz() })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(fmt(pos), color = white, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row {
                    IconBtn(Icons.Rounded.Shuffle, tint = if (Audio.shuffle) Green else white.copy(0.6f)) { Audio.toggleShuffle() }
                    IconBtn(if (Audio.repeat == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        tint = if (Audio.repeat != androidx.media3.common.Player.REPEAT_MODE_OFF) Green else white.copy(0.6f)) { Audio.cycleRepeat() }
                }
                Text(fmt(dur), color = white.copy(0.7f), fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp).height(84.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WideBtn(Icons.Rounded.SkipPrevious, Modifier.weight(1f)) { Audio.prev() }
                val corner by animateDpAsState(if (Audio.playing) 28.dp else 42.dp, spring(0.5f, 300f), label = "pc")
                Box(Modifier.weight(1.15f).fillMaxHeight().clip(RoundedCornerShape(corner)).background(white.copy(0.30f)).tap(true) { Audio.toggle() }, Alignment.Center) {
                    Icon(if (Audio.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = white, modifier = Modifier.size(34.dp))
                }
                WideBtn(Icons.Rounded.SkipNext, Modifier.weight(1f)) { Audio.next() }
            }
        }
        AnimatedVisibility(lyricsFull, enter = slideInVertically { it }, exit = slideOutVertically { it }) { LyricsFull(s, lyr, pos, dur) { lyricsFull = false } }
    }
}

// ---------- "Discovery" UI: mood orbs, daily playlists and big rounded tiles ----------
class MoodDef(val label: String, val base: Color, val b1: Color, val b2: Color, val b3: Color, val streaks: Boolean)
class Playlist(val name: String, val songs: List<Song>)

@Composable fun MoodOrb(m: MoodDef, d: Dp, onClick: () -> Unit) {
    Box(Modifier.size(d).clip(CircleShape).tap { onClick() }, Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawRect(m.base)
            fun blob(c: Color, fx: Float, fy: Float, r: Float) = drawRect(Brush.radialGradient(listOf(c, Color.Transparent), Offset(w * fx, h * fy), w * r))
            blob(m.b1, 0.2f, 0.7f, 0.7f); blob(m.b2, 0.85f, 0.75f, 0.65f); blob(m.b3, 0.4f, 0.12f, 0.6f)
            if (m.streaks) for (k in 0 until 9) drawRect(Color.Black.copy(alpha = 0.10f), Offset(0f, h * k / 9f), Size(w, h * 0.045f))
        }
        Text(m.label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable fun SectionHeader(t: String) = Row(Modifier.padding(start = 20.dp, top = 26.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(t, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    Icon(Icons.Rounded.ChevronRight, null, tint = Grey, modifier = Modifier.padding(start = 4.dp).size(22.dp))
}

@Composable fun TileSq(song: Song, label: String, caption: String, size: Dp, onClick: () -> Unit) {
    Column(Modifier.width(size).tap { onClick() }) {
        Box(Modifier.size(size).clip(RoundedCornerShape(30.dp)).background(Color(0xFF1C1C1E))) {
            Icon(Icons.Rounded.MusicNote, null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
            AsyncImage(model = song.art, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF4DFF9A).copy(0.18f), Color.Transparent, Color(0xFF3D5BFF).copy(0.25f)))))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(0.6f))))
            Text(label, Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 14.dp, end = 12.dp), color = Color.White,
                fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(caption, Modifier.padding(top = 8.dp, start = 4.dp), color = Grey, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable fun HomeDiscovery(songs: List<Song>, onProfile: () -> Unit) {
    val moods = remember {
        listOf(
            MoodDef("Cheerful", Color(0xFFE8506A), Color(0xFFFF9AD5), Color(0xFFFFA52A), Color(0xFFFF3B1F), false),
            MoodDef("Training", Color(0xFFFF7A1A), Color(0xFFFFB347), Color(0xFF7A1208), Color(0xFFFF5A1F), true),
            MoodDef("Chill", Color(0xFF2A9D8F), Color(0xFF4DA3FF), Color(0xFF8AE2D0), Color(0xFF1B4DB3), false),
            MoodDef("Focus", Color(0xFF5B3FD6), Color(0xFFB06BFF), Color(0xFF2D1B8A), Color(0xFF7B5CFF), false))
    }
    val likedSet = Audio.liked
    val lists = remember(songs, likedSet) {
        buildList {
            if (songs.isNotEmpty()) {
                add(Playlist("Always ok", songs.shuffled(Random(songs.size))))
                add(Playlist("New", songs.take(20)))
                val lk = songs.filter { it.id in likedSet }
                if (lk.isNotEmpty()) add(Playlist("Liked", lk))
                add(Playlist("Mix", songs.shuffled(Random(7))))
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(Modifier.statusBarsPadding().padding(start = 20.dp, top = 12.dp).size(44.dp).clip(CircleShape).background(Color(0xFF1C1C1E)).tap { onProfile() }, Alignment.Center) {
                Icon(Icons.Rounded.NorthWest, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, top = 18.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sound for you", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                Text("For you", Modifier.padding(start = 28.dp), color = Grey.copy(0.55f), fontSize = 14.sp)
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(moods) { m -> MoodOrb(m, 156.dp) { if (songs.isNotEmpty()) Audio.play(songs.shuffled(Random(m.label.hashCode())), 0) } }
            }
        }
        if (songs.isEmpty()) item { Text("Grant storage permission and add music files to your phone", Modifier.padding(24.dp), color = Grey) }
        else {
            item { SectionHeader("Playlists of the day") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(lists) { p -> TileSq(p.songs.first(), p.name, p.songs.map { it.artist }.distinct().take(4).joinToString(", "), 156.dp) { Audio.play(p.songs, 0) } }
                }
            }
            item { SectionHeader("Recently added") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(songs.take(15)) { sg -> TileSq(sg, sg.title, sg.artist, 156.dp) { Audio.play(songs, songs.indexOf(sg)) } }
                }
            }
        }
    }
}

@Composable fun UiPreview(i: Int) {
    val edge = RoundedCornerShape(14.dp)
    Box(Modifier.fillMaxWidth().height(70.dp).clip(edge).background(Color.Black).border(1.dp, Color.White.copy(0.08f), edge), Alignment.Center) {
        if (i == 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { Box(Modifier.size(44.dp, 28.dp).clip(RoundedCornerShape(8.dp)).background(TileBg)) }
        } else if (i == 4) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp, 50.dp).clip(RoundedCornerShape(17.dp)).background(Brush.verticalGradient(listOf(Color(0xFFFFC08A), Color(0xFFD7261E), Color(0xFF140605)))))
            Box(Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF2A98C)))
            Box(Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1C1C1E)))
        } else if (i == 3) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3A0204), Color(0xFFD00010)))), Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { Box(Modifier.size(34.dp, 44.dp).glassPanel(RoundedCornerShape(10.dp))) } }
        } else if (i == 2) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFF9AD5), Color(0xFFFF4A2A)))).border(1.dp, Color.White.copy(0.5f), CircleShape).padding(5.dp)
                .clip(CircleShape).background(Color.White.copy(0.18f)))
            repeat(2) { Box(Modifier.size(40.dp).glassPanel(RoundedCornerShape(12.dp)).padding(4.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(Color(0xFF4DA3FF), Color(0xFF2D1B8A))))) }
        } else Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFF9AD5), Color(0xFFFF4A2A)))))
            repeat(2) { Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF4DA3FF), Color(0xFF2D1B8A))))) }
        }
    }
}

// ---------- "Glass" UI ----------
/** Everything that defines the glass look lives here. Change these numbers to restyle the whole UI. */
object GlassStyle {
    val panelTopAlpha = 0.20f; val panelBottomAlpha = 0.06f          // frosted white fill, top -> bottom
    val strokeAlpha = 0.45f; val strokeEndAlpha = 0.06f; val strokeWidth = 1.2.dp   // rim light (border)
    val innerRingAlpha = 0.35f; val innerRingInset = 9.dp            // ring inside the orbs
    val glowAlpha = 0.45f; val glowBlur = 26.dp                      // ambient glow behind orbs and cards
    val frameWidth = 8.dp; val frameCorner = 32.dp; val artCorner = 24.dp   // card frame
    val orb = 156.dp; val tile = 156.dp                              // item sizes
    val barTopCorner = 30.dp; val barDarkBase = 0.55f                // bottom bar
    val rim: Brush get() = Brush.linearGradient(listOf(Color.White.copy(strokeAlpha), Color.White.copy(strokeEndAlpha), Color.White.copy(strokeAlpha * 0.6f)))
    val fill: Brush get() = Brush.verticalGradient(listOf(Color.White.copy(panelTopAlpha), Color.White.copy(panelBottomAlpha)))
}

fun Modifier.glassPanel(shape: Shape): Modifier = clip(shape).background(GlassStyle.fill).border(GlassStyle.strokeWidth, GlassStyle.rim, shape)

@Composable fun AmbientGlow(color: Color, modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= 31) Box(modifier.blur(GlassStyle.glowBlur, BlurredEdgeTreatment.Unbounded).background(color.copy(GlassStyle.glowAlpha), CircleShape))
}

@Composable fun GlassOrb(m: MoodDef, onClick: () -> Unit) {
    Box(Modifier.size(GlassStyle.orb).tap { onClick() }, Alignment.Center) {
        AmbientGlow(m.b2, Modifier.matchParentSize().padding(6.dp))
        Box(Modifier.matchParentSize().clip(CircleShape)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                drawRect(m.base)
                fun blob(c: Color, fx: Float, fy: Float, r: Float) = drawRect(Brush.radialGradient(listOf(c, Color.Transparent), Offset(w * fx, h * fy), w * r))
                blob(m.b1, 0.2f, 0.7f, 0.7f); blob(m.b2, 0.85f, 0.75f, 0.65f); blob(m.b3, 0.4f, 0.12f, 0.6f)
                if (m.streaks) for (k in 0 until 9) drawRect(Color.Black.copy(alpha = 0.10f), Offset(0f, h * k / 9f), Size(w, h * 0.045f))
            }
        }
        Box(Modifier.matchParentSize().border(GlassStyle.strokeWidth, GlassStyle.rim, CircleShape))
        Box(Modifier.matchParentSize().padding(GlassStyle.innerRingInset).clip(CircleShape).background(GlassStyle.fill)
            .border(1.dp, Color.White.copy(GlassStyle.innerRingAlpha), CircleShape), Alignment.Center) {
            Text(m.label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable fun GlassTile(song: Song, label: String, caption: String, onClick: () -> Unit) {
    Column(Modifier.width(GlassStyle.tile).tap { onClick() }) {
        Box(Modifier.size(GlassStyle.tile)) {
            if (Build.VERSION.SDK_INT >= 31) AsyncImage(model = song.art, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().padding(6.dp).graphicsLayer { alpha = GlassStyle.glowAlpha }.blur(GlassStyle.glowBlur, BlurredEdgeTreatment.Unbounded))
            Box(Modifier.matchParentSize().glassPanel(RoundedCornerShape(GlassStyle.frameCorner)).padding(GlassStyle.frameWidth)) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(GlassStyle.artCorner)).background(Color(0xFF1C1C1E))) {
                    Icon(Icons.Rounded.MusicNote, null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                    AsyncImage(model = song.art, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(0.6f))))
                    Text(label, Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 12.dp, end = 10.dp), color = Color.White,
                        fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Text(caption, Modifier.padding(top = 8.dp, start = 4.dp), color = Grey, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable fun HomeGlass(songs: List<Song>, bottomPad: Dp, onProfile: () -> Unit) {
    val moods = remember {
        listOf(
            MoodDef("Cheerful", Color(0xFFE8506A), Color(0xFFFF9AD5), Color(0xFFFFA52A), Color(0xFFFF3B1F), false),
            MoodDef("Training", Color(0xFFFF7A1A), Color(0xFFFFB347), Color(0xFFE0521A), Color(0xFFFF5A1F), true),
            MoodDef("Chill", Color(0xFF2A7FD6), Color(0xFF4DA3FF), Color(0xFF8AE2D0), Color(0xFF1B4DB3), false),
            MoodDef("Focus", Color(0xFF5B3FD6), Color(0xFFB06BFF), Color(0xFF2D1B8A), Color(0xFF7B5CFF), false))
    }
    val likedSet = Audio.liked
    val lists = remember(songs, likedSet) {
        buildList {
            if (songs.isNotEmpty()) {
                add(Playlist("Always ok", songs.shuffled(Random(songs.size))))
                add(Playlist("New", songs.take(20)))
                val lk = songs.filter { it.id in likedSet }
                if (lk.isNotEmpty()) add(Playlist("Liked", lk))
                add(Playlist("Mix", songs.shuffled(Random(7))))
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPad + 16.dp)) {
        item {
            Box(Modifier.statusBarsPadding().padding(start = 20.dp, top = 12.dp).size(48.dp).glassPanel(CircleShape).tap { onProfile() }, Alignment.Center) {
                Icon(Icons.Rounded.NorthWest, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sound for you", color = Color.White.copy(0.92f), fontSize = 22.sp, fontWeight = FontWeight.Medium)
                Text("For you", Modifier.padding(start = 28.dp), color = Grey.copy(0.55f), fontSize = 14.sp)
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(moods) { m -> GlassOrb(m) { if (songs.isNotEmpty()) Audio.play(songs.shuffled(Random(m.label.hashCode())), 0) } }
            }
        }
        if (songs.isEmpty()) item { Text("Grant storage permission and add music files to your phone", Modifier.padding(24.dp), color = Grey) }
        else {
            item { SectionHeader("Playlists of the day") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(lists) { p -> GlassTile(p.songs.first(), p.name, p.songs.map { it.artist }.distinct().take(4).joinToString(", ")) { Audio.play(p.songs, 0) } }
                }
            }
            item { SectionHeader("Recently added") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(songs.take(15)) { sg -> GlassTile(sg, sg.title, sg.artist) { Audio.play(songs, songs.indexOf(sg)) } }
                }
            }
        }
    }
}

/** Translucent frosted bottom bar with a soft glowing pill behind the active tab. */
@Composable fun NavBarGlass(sel: Int, onSelect: (Int) -> Unit) {
    val items = listOf("Home", "Discovery", "Collection")
    val kinds = listOf(3, 1, 2)
    val shape = RoundedCornerShape(topStart = GlassStyle.barTopCorner, topEnd = GlassStyle.barTopCorner)
    Row(Modifier.fillMaxWidth().clip(shape).background(Color.Black.copy(GlassStyle.barDarkBase)).background(GlassStyle.fill)
        .border(GlassStyle.strokeWidth, GlassStyle.rim, shape).navigationBarsPadding().padding(top = 10.dp, bottom = 8.dp)) {
        items.forEachIndexed { i, label ->
            val on = i == sel
            val tint by animateColorAsState(if (on) Color.White else Color(0xFF8A8FA3), tween(200), label = "gt")
            val pill by animateColorAsState(if (on) Color.White.copy(0.16f) else Color.Transparent, tween(200), label = "gp")
            Column(Modifier.weight(1f).tap { onSelect(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp, 34.dp), Alignment.Center) {
                    if (on && Build.VERSION.SDK_INT >= 31) Box(Modifier.matchParentSize().blur(10.dp, BlurredEdgeTreatment.Unbounded).background(Color.White.copy(0.20f), CircleShape))
                    Box(Modifier.matchParentSize().clip(CircleShape).background(pill))
                    NavGlyph(kinds[i], tint, Modifier.size(28.dp))
                }
                if (Audio.showLabels) Text(label, Modifier.padding(top = 2.dp), color = tint, fontSize = 12.sp)
            }
        }
    }
}

// ---------- "Ember" UI: red fluid glow, glass cards, smart mix and listening tools ----------
@Composable fun RedAmbient() {
    val tr = rememberInfiniteTransition(label = "red")
    val a by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "a")
    val b by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(19000, easing = LinearEasing)), label = "b")
    val c by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(23000, easing = LinearEasing)), label = "c")
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val t = (2 * Math.PI).toFloat()
        drawRect(Brush.verticalGradient(listOf(Color(0xFF120000), Color(0xFF3A0204), Color(0xFF8A0008)), 0f, h))
        fun blob(col: Color, cx: Float, cy: Float, r: Float) = drawCircle(Brush.radialGradient(listOf(col, Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
        blob(Color(0xFFFF3B0A).copy(0.75f), w * (0.30f + 0.15f * sin(t * a)), h * (0.30f + 0.06f * cos(t * b)), w * 0.85f)
        blob(Color(0xFFFF6A00).copy(0.55f), w * (0.75f + 0.12f * cos(t * b)), h * (0.50f + 0.08f * sin(t * c)), w * 0.75f)
        blob(Color(0xFFD00010).copy(0.70f), w * (0.20f + 0.15f * sin(t * c)), h * (0.68f + 0.07f * cos(t * a)), w * 0.9f)
        blob(Color(0xFFFF4A1A).copy(0.45f), w * (0.85f + 0.10f * sin(t * a)), h * (0.20f + 0.05f * cos(t * c)), w * 0.6f)
        drawRect(Brush.verticalGradient(0f to Color.Black.copy(0.55f), 0.22f to Color.Transparent, 0.85f to Color.Transparent, 1f to Color.Black.copy(0.35f)))
    }
}

@Composable fun SparkleGlyph(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val c = Offset(w / 2f, size.height / 2f)
        fun star(cx: Float, cy: Float, r: Float) {
            val p = Path(); p.moveTo(cx, cy - r); p.quadraticBezierTo(cx, cy, cx + r, cy); p.quadraticBezierTo(cx, cy, cx, cy + r)
            p.quadraticBezierTo(cx, cy, cx - r, cy); p.quadraticBezierTo(cx, cy, cx, cy - r); p.close(); drawPath(p, color)
        }
        star(c.x, c.y, w * 0.38f); star(w * 0.82f, w * 0.20f, w * 0.14f)
        rotate(-25f, c) { drawOval(color.copy(alpha = 0.7f), Offset(w * 0.04f, c.y - w * 0.14f), Size(w * 0.92f, w * 0.28f), style = Stroke(1.4.dp.toPx())) }
    }
}

data class TrackInfo(val duration: String = "", val album: String = "", val tags: List<String> = emptyList())
fun readInfo(ctx: Context, s: Song): TrackInfo = try {
    val r = MediaMetadataRetriever(); r.setDataSource(ctx, s.uri)
    fun k(i: Int) = r.extractMetadata(i)?.takeIf { it.isNotBlank() }
    val dur = k(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    val tags = mutableListOf<String>()
    k(MediaMetadataRetriever.METADATA_KEY_GENRE)?.let { tags += it.take(18) }
    k(MediaMetadataRetriever.METADATA_KEY_YEAR)?.let { tags += it.take(4) }
    k(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.let { tags += it.substringAfter('/').uppercase().replace("MPEG", "MP3").take(6) }
    k(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()?.let { tags += "${it / 1000} kbps" }
    val album = k(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: ""
    r.release()
    if (tags.size < 2) tags += "Local file"
    TrackInfo(fmt(dur), album, tags)
} catch (_: Exception) { TrackInfo("", "", listOf("Local file")) }

@Composable fun RedCircle(icon: ImageVector, tint: Color, onClick: () -> Unit) =
    Box(Modifier.size(38.dp).glassPanel(CircleShape).tap { onClick() }, Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)) }

@Composable fun RedChip(t: String, orange: Boolean) {
    Box(Modifier.clip(CircleShape).background(if (orange) Color(0xFFE0521A).copy(0.55f) else Color.White.copy(0.14f))
        .border(1.dp, Color.White.copy(0.18f), CircleShape).padding(horizontal = 16.dp, vertical = 9.dp)) {
        Text(t, color = Color.White.copy(0.9f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable fun RedCard(s: Song, list: List<Song>, onFocus: () -> Unit, onOpenPlayer: () -> Unit) {
    val view = LocalView.current
    val liked = s.id in Audio.liked
    val isCur = Audio.current?.id == s.id
    val playing = isCur && Audio.playing
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(Modifier.width(200.dp).glassPanel(RoundedCornerShape(24.dp))
            .pointerInput(s.id) { detectTapGestures(onLongPress = { view.buzz(true); menu = true }, onTap = { view.buzz(); onFocus(); Audio.play(list, list.indexOf(s)) }) }
            .padding(10.dp)) {
            Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(0.25f))) {
                Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(0.5f), modifier = Modifier.align(Alignment.Center))
                AsyncImage(model = s.art, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.align(Alignment.TopCenter).padding(top = 12.dp).size(46.dp, 24.dp).clip(CircleShape).background(Color.White.copy(0.22f)), Alignment.Center) {
                    if (playing) Icon(Icons.Rounded.GraphicEq, null, tint = Color.White, modifier = Modifier.size(16.dp)) else SparkleGlyph(Color.White, Modifier.size(16.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(s.title, Modifier.weight(1f), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    RedCircle(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (liked) Color(0xFFFF8A7A) else Color.White) { Audio.toggleLike(s) }
                    Box {
                        RedCircle(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, Color.White) { if (isCur) Audio.toggle() else { onFocus(); Audio.play(list, list.indexOf(s)) } }
                        val n = Audio.plays[s.id] ?: 0
                        if (n > 0) Box(Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(17.dp).clip(CircleShape).background(Color.White.copy(0.28f)), Alignment.Center) {
                            Text("${minOf(n, 99)}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    RedCircle(Icons.Rounded.Subject, Color.White) { onFocus(); if (!isCur) Audio.play(list, list.indexOf(s)); Audio.wantLyrics = true; onOpenPlayer() }
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Play next") }, onClick = { Audio.playNext(s); menu = false })
            DropdownMenuItem(text = { Text("Add to queue") }, onClick = { Audio.addToQueue(s); menu = false })
            DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { Audio.pickSong = s; menu = false })
            DropdownMenuItem(text = { Text(if (liked) "Remove from liked" else "Add to liked") }, onClick = { Audio.toggleLike(s); menu = false })
        }
    }
}

@Composable fun RedDetail(s: Song, list: List<Song>) {
    val ctx = LocalContext.current
    val info by produceState(TrackInfo(), s.id) { value = withContext(Dispatchers.IO) { readInfo(ctx, s) } }
    val arts = remember(s.id, list) { (listOf(s) + list.filter { it.id != s.id }).take(3) }
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Art(s, Modifier.size(52.dp), CircleShape)
            Spacer(Modifier.width(12.dp))
            Text(if (info.duration.isBlank()) s.title else "${s.title} · ${info.duration}", Modifier.weight(1f, fill = false), color = Color.White, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            Row(Modifier.glassPanel(CircleShape).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.FlashOn, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text("${Audio.plays[s.id] ?: 0}", Modifier.padding(start = 3.dp), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(s.artist + if (info.album.isNotBlank()) " · " + info.album else "", Modifier.padding(start = 64.dp, top = 6.dp), color = Color.White.copy(0.65f), fontSize = 15.sp, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Box(Modifier.padding(start = 64.dp, top = 14.dp).fillMaxWidth().height(1.dp).background(Color.White.copy(0.25f)))
        Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(150.dp)) {
                val spots = listOf(Triple(0.dp, 28.dp, -6f), Triple(52.dp, 0.dp, 20f), Triple(48.dp, 56.dp, -28f))
                arts.forEachIndexed { k, sg ->
                    val (x, y, rot) = spots[k]
                    Box(Modifier.offset(x, y).size(86.dp).graphicsLayer { rotationZ = rot }.clip(RoundedCornerShape(22.dp)).background(Color.Black)) {
                        AsyncImage(model = sg.art, contentDescription = null, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.85f }, contentScale = ContentScale.Crop)
                    }
                }
            }
            Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                info.tags.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEachIndexed { k, t -> RedChip(t, k % 2 == 1) } } }
            }
        }
    }
}

@Composable fun RedRow(s: Song, list: List<Song>) {
    val liked = s.id in Audio.liked
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp).glassPanel(RoundedCornerShape(20.dp)).tap { Audio.play(list, list.indexOf(s)) }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Art(s, Modifier.size(48.dp), RoundedCornerShape(14.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(s.title, color = if (Audio.current?.id == s.id) Color(0xFFFFB199) else Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(s.artist, color = Color.White.copy(0.6f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconBtn(Icons.Rounded.PlaylistAdd, tint = Color.White) { Audio.pickSong = s }
        IconBtn(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, tint = if (liked) Color(0xFFFF8A7A) else Color.White) { Audio.toggleLike(s) }
    }
}

@Composable fun LibraryRed(songs: List<Song>, bottomPad: Dp, onProfile: () -> Unit, onOpenPlayer: () -> Unit) {
    var tabLiked by remember { mutableStateOf(false) }
    var sortMode by remember { mutableIntStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var focusId by remember { mutableStateOf<Long?>(null) }
    val sorts = listOf("Newest", "Oldest", "A–Z", "Artist", "Most played")
    val liked = Audio.liked; val plays = Audio.plays
    val list = remember(songs, tabLiked, sortMode, query, liked, plays) {
        var l = if (tabLiked) songs.filter { it.id in liked } else songs
        if (query.isNotBlank()) l = l.filter { it.title.contains(query, true) || it.artist.contains(query, true) }
        when (sortMode) { 1 -> l.reversed(); 2 -> l.sortedBy { it.title.lowercase() }; 3 -> l.sortedBy { it.artist.lowercase() }; 4 -> l.sortedByDescending { plays[it.id] ?: 0 }; else -> l }
    }
    val focus = list.firstOrNull { it.id == focusId } ?: Audio.current?.takeIf { c -> list.any { it.id == c.id } } ?: list.firstOrNull()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPad + 16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.glassPanel(CircleShape).padding(4.dp)) {
                    listOf("Songs", "Liked").forEachIndexed { i, t ->
                        val on = (i == 1) == tabLiked
                        val bg by animateColorAsState(if (on) Color.White else Color.Transparent, label = "seg")
                        Box(Modifier.clip(CircleShape).background(bg).tap { tabLiked = i == 1 }.padding(horizontal = 22.dp, vertical = 12.dp)) {
                            Text(t, color = if (on) Color.Black else Color.White.copy(0.85f), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(52.dp).glassPanel(CircleShape).tap { onProfile() }, Alignment.Center) {
                    AvatarContent(Color.White, 20.sp)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Library", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                Text("${list.size} tracks", Modifier.padding(start = 10.dp, top = 8.dp), color = Color.White.copy(0.6f), fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Box {
                    Row(Modifier.glassPanel(CircleShape).tap { sortMenu = true }.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sorts[sortMode], color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.padding(start = 4.dp).size(18.dp))
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        sorts.forEachIndexed { i, t -> DropdownMenuItem(text = { Text(t) }, onClick = { sortMode = i; sortMenu = false }) }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(46.dp).glassPanel(CircleShape).tap { searching = !searching; if (!searching) query = "" }, Alignment.Center) {
                    Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
        if (searching) item {
            TextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).glassPanel(CircleShape), singleLine = true,
                placeholder = { Text("Search songs or artists", color = Color.White.copy(0.6f)) },
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White))
        }
        if (songs.isEmpty()) item { Text("Grant storage permission and add music files to your phone", Modifier.padding(24.dp), color = Color.White.copy(0.7f)) }
        else if (list.isEmpty()) item { Text(if (tabLiked) "No liked songs yet. Tap the heart on a song." else "No songs match your search.", Modifier.padding(24.dp), color = Color.White.copy(0.7f)) }
        else {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(list, key = { it.id }) { sg -> RedCard(sg, list, { focusId = sg.id }, onOpenPlayer) }
                }
            }
            if (focus != null) item { RedDetail(focus, list) }
            item { Text("All tracks", Modifier.padding(start = 20.dp, top = 28.dp, bottom = 8.dp), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            items(list, key = { it.id }) { sg -> RedRow(sg, list) }
        }
    }
}

@Composable fun RedNavCircle(icon: ImageVector, on: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(60.dp).glassPanel(CircleShape).then(if (on) Modifier.background(Color.White.copy(0.14f)) else Modifier).tap { onClick() }, Alignment.Center) {
        Icon(icon, null, tint = Color.White.copy(if (on) 1f else 0.75f), modifier = Modifier.size(26.dp))
    }
}

@Composable fun RowScope.NavCell(content: @Composable () -> Unit) { Box(Modifier.weight(1f), Alignment.Center) { content() } }

/** Icons only, full width, five equal cells: Home, Library, Generate (smart mix), Playlists, Tools (bell). */
@Composable fun NavBarRed(sel: Int, onSelect: (Int) -> Unit, onGenerate: () -> Unit, onTools: () -> Unit) {
    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        NavCell { RedNavCircle(Icons.Rounded.Home, sel == 0) { onSelect(0) } }
        NavCell { RedNavCircle(Icons.Rounded.Album, sel == 1) { onSelect(1) } }
        NavCell { Box(Modifier.size(66.dp).clip(CircleShape).background(Color.White).tap(true) { onGenerate() }, Alignment.Center) { SparkleGlyph(Color(0xFF1A1A1A), Modifier.size(32.dp)) } }
        NavCell { RedNavCircle(Icons.Rounded.QueueMusic, sel == 2) { onSelect(2) } }
        NavCell { RedNavCircle(Icons.Rounded.Notifications, false) { onTools() } }
    }
}

@Composable fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(CircleShape).background(if (selected) Color.White else Color.White.copy(0.12f)).tap { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(label, color = if (selected) Color.Black else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/** Listening tools: sleep timer, playback speed and equalizer presets. */
@Composable fun ToolsSheet(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val top = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.55f)).pointerInput(Unit) { detectTapGestures { onClose() } }) {
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(top).background(Color(0xFF2A0508).copy(0.97f)).border(1.dp, GlassStyle.rim, top)
            .pointerInput(Unit) { detectTapGestures { } }.navigationBarsPadding().padding(20.dp)) {
            Text("Listening tools", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            val left = Audio.sleepEnd - now
            Text("Sleep timer", Modifier.padding(top = 18.dp, bottom = 4.dp), color = Color.White.copy(0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (Audio.sleepEnd > 0 && left > 0) Text("Music fades out in ${fmt(left)}", Modifier.padding(bottom = 8.dp), color = Color(0xFFFFB199), fontSize = 14.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionChip("Off", Audio.sleepEnd == 0L) { Audio.cancelSleep() }
                listOf(5, 15, 30, 45, 60).forEach { m -> OptionChip("$m min", false) { Audio.sleepIn(m) } }
                OptionChip("End of song", Audio.sleepEnd == -1L) { Audio.sleepAtEnd() }
            }
            Text("Speed", Modifier.padding(top = 18.dp, bottom = 8.dp), color = Color.White.copy(0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { v -> OptionChip(if (v == 1f) "Normal" else "${v}x", Audio.speed == v) { Audio.chooseSpeed(v) } }
            }
            Text("Equalizer", Modifier.padding(top = 18.dp, bottom = 8.dp), color = Color.White.copy(0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Off", "Bass", "Vocal", "Treble").forEachIndexed { i, t -> OptionChip(t, Audio.eqPreset == i) { Audio.chooseEq(i) } }
            }
        }
    }
}

// ---------- Custom background (picture or video) ----------
@Composable fun UserBackground() {
    val uri = remember(Audio.bgUri) { Uri.parse(Audio.bgUri) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (Audio.bgType == 2) VideoBackground(uri)
        else AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(Audio.bgDim)))
    }
}

/** Muted, looping video behind the app. A separate player, so it never interrupts your music. */
@Composable fun VideoBackground(uri: Uri) {
    val ctx = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(uri)); repeatMode = Player.REPEAT_MODE_ALL; volume = 0f
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING; prepare(); playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { player.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { player.play() }
    AndroidView(factory = { c -> TextureView(c).also { player.setVideoTextureView(it) } }, modifier = Modifier.fillMaxSize())
}

// ---------- Playlists ----------
class PlView(val id: Long, val name: String, val songs: List<Song>, val editable: Boolean)

/** Your playlists first, then automatic ones (Liked, Most played, Recently added). */
fun resolvePlaylists(songs: List<Song>): List<PlView> {
    val byId = songs.associateBy { it.id }
    val mine = Audio.playlists.map { p -> PlView(p.id, p.name, p.songIds.mapNotNull { byId[it] }, true) }
    val auto = listOf(
        PlView(-1, "Liked songs", songs.filter { it.id in Audio.liked }, false),
        PlView(-2, "Most played", songs.filter { (Audio.plays[it.id] ?: 0) > 0 }.sortedByDescending { Audio.plays[it.id] ?: 0 }.take(50), false),
        PlView(-3, "Recently added", songs.take(30), false))
    return mine + auto
}

@Composable fun PlCover(songs: List<Song>, d: Dp, corner: Dp = 16.dp) {
    Box(Modifier.size(d).clip(RoundedCornerShape(corner)).background(Color.White.copy(0.12f)), Alignment.Center) {
        if (songs.isEmpty()) Icon(Icons.Rounded.QueueMusic, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(d / 2))
        else if (songs.size < 4) AsyncImage(model = songs[0].art, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Column(Modifier.fillMaxSize()) {
            for (rr in 0..1) Row(Modifier.weight(1f)) {
                for (cc in 0..1) AsyncImage(model = songs[rr * 2 + cc].art, contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
            }
        }
    }
}

@Composable fun NamePlaylistDialog(title: String, initial: String, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var t by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { TextField(t, { t = it.take(30) }, singleLine = true, placeholder = { Text("Playlist name") }) },
        confirmButton = { TextButton(onClick = { if (t.isNotBlank()) onOk(t.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/** Shown from the playlist button on any song: tick the playlists it should be in, or create a new one. */
@Composable fun PlaylistPicker(song: Song, onDismiss: () -> Unit) {
    var creating by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add to playlist") },
        text = {
            Column {
                TextButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, null); Text("New playlist", Modifier.padding(start = 6.dp)) }
                LazyColumn(Modifier.heightIn(max = 280.dp)) {
                    items(Audio.playlists, key = { it.id }) { p ->
                        val has = song.id in p.songIds
                        Row(Modifier.fillMaxWidth().tap { if (has) Audio.removeFromPlaylist(p.id, song.id) else Audio.addToPlaylist(p.id, song.id) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(p.name, Modifier.weight(1f), fontSize = 16.sp)
                            if (has) Icon(Icons.Rounded.Check, null)
                        }
                    }
                }
                if (Audio.playlists.isEmpty()) Text("No playlists yet", color = Grey, fontSize = 14.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
    if (creating) NamePlaylistDialog("New playlist", "", { creating = false }) { n -> Audio.addToPlaylist(Audio.createPlaylist(n), song.id); creating = false }
}

@Composable fun PlaylistsRoot(songs: List<Song>, bottomPad: Dp, onClose: (() -> Unit)?) {
    var openId by remember { mutableStateOf<Long?>(null) }
    if (onClose != null) BackHandler(onBack = onClose)
    val open = openId
    if (open != null) { BackHandler { openId = null }; PlaylistDetail(open, songs, bottomPad) { openId = null } }
    else PlaylistList(songs, bottomPad, onClose) { openId = it }
}

@Composable fun PlaylistList(songs: List<Song>, bottomPad: Dp, onClose: (() -> Unit)?, onOpen: (Long) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    val all = resolvePlaylists(songs)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPad + 16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().then(if (onClose == null) Modifier.statusBarsPadding() else Modifier).padding(start = 20.dp, end = 20.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onClose != null) IconBtn(Icons.Rounded.KeyboardArrowDown) { onClose() }
                Text("Playlists", Modifier.weight(1f), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                Row(Modifier.glassPanel(CircleShape).tap(true) { creating = true }.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text("New", Modifier.padding(start = 4.dp), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        if (Audio.playlists.isEmpty()) item { Text("Tap New to make a playlist, then use the playlist button on any song to add it.", Modifier.padding(horizontal = 20.dp, vertical = 14.dp), color = Color.White.copy(0.65f), fontSize = 14.sp) }
        items(all, key = { it.id }) { p ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).glassPanel(RoundedCornerShape(22.dp)).tap { onOpen(p.id) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PlCover(p.songs, 64.dp)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(p.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (p.editable) "${p.songs.size} songs" else "${p.songs.size} songs · automatic", color = Color.White.copy(0.6f), fontSize = 13.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(0.6f))
            }
        }
    }
    if (creating) NamePlaylistDialog("New playlist", "", { creating = false }) { n -> Audio.createPlaylist(n); creating = false }
}

@Composable fun PlaylistDetail(id: Long, songs: List<Song>, bottomPad: Dp, onBack: () -> Unit) {
    val p = resolvePlaylists(songs).firstOrNull { it.id == id }
    LaunchedEffect(p == null) { if (p == null) onBack() }
    if (p == null) return
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPad + 16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBtn(Icons.Rounded.ArrowBack) { onBack() }
                Spacer(Modifier.weight(1f))
                if (p.editable) Box {
                    IconBtn(Icons.Rounded.MoreVert) { menu = true }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; renaming = true })
                        DropdownMenuItem(text = { Text("Delete playlist") }, onClick = { menu = false; deleting = true })
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PlCover(p.songs, 180.dp, 28.dp)
                Text(p.name, Modifier.padding(top = 16.dp), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${p.songs.size} songs", color = Color.White.copy(0.65f), fontSize = 14.sp)
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.clip(CircleShape).background(Color.White).tap(true) { if (p.songs.isNotEmpty()) Audio.play(p.songs, 0) }.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp)); Text("Play", Modifier.padding(start = 4.dp), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.glassPanel(CircleShape).tap(true) { if (p.songs.isNotEmpty()) Audio.play(p.songs.shuffled(), 0) }.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Shuffle, null, tint = Color.White, modifier = Modifier.size(20.dp)); Text("Shuffle", Modifier.padding(start = 4.dp), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (p.songs.isEmpty()) item { Text(if (p.editable) "This playlist is empty. Use the playlist button on any song to add it." else "Nothing here yet.", Modifier.padding(24.dp), color = Color.White.copy(0.65f)) }
        itemsIndexed(p.songs) { idx, sg ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp).glassPanel(RoundedCornerShape(20.dp)).tap { Audio.play(p.songs, idx) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Art(sg, Modifier.size(48.dp), RoundedCornerShape(14.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(sg.title, color = if (Audio.current?.id == sg.id) Color(0xFFFFB199) else Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(sg.artist, color = Color.White.copy(0.6f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (p.editable) {
                    if (idx > 0) IconBtn(Icons.Rounded.ArrowUpward, tint = Color.White.copy(0.8f)) { Audio.moveInPlaylist(p.id, idx, idx - 1) }
                    if (idx < p.songs.lastIndex) IconBtn(Icons.Rounded.ArrowDownward, tint = Color.White.copy(0.8f)) { Audio.moveInPlaylist(p.id, idx, idx + 1) }
                    IconBtn(Icons.Rounded.RemoveCircleOutline, tint = Color.White.copy(0.8f)) { Audio.removeFromPlaylist(p.id, sg.id) }
                }
            }
        }
    }
    if (renaming) NamePlaylistDialog("Rename playlist", p.name, { renaming = false }) { n -> Audio.renamePlaylist(p.id, n); renaming = false }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Delete \"${p.name}\"?") },
        text = { Text("The songs stay on your phone.") },
        confirmButton = { TextButton(onClick = { Audio.deletePlaylist(p.id); deleting = false }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel") } })
}

// ---------- Profile picture, shapes ----------
@Composable fun AvatarContent(textColor: Color, textSize: TextUnit) {
    if (Audio.avatarUri.isNotBlank()) AsyncImage(model = Uri.parse(Audio.avatarUri), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    else Text(Audio.userName.trim().take(1).uppercase().ifEmpty { "J" }, color = textColor, fontSize = textSize, fontWeight = FontWeight.Bold)
}

/** A square with a soft waist (the organic cut-outs in the recommendation tiles). */
class WaistShape(private val waistFrac: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width; val h = size.height; val r = w * 0.22f; val k = w * waistFrac
        val p = Path()
        p.moveTo(r, 0f); p.lineTo(w - r, 0f); p.quadraticBezierTo(w, 0f, w, r)
        p.cubicTo(w, h * 0.30f, w - k, h * 0.42f, w - k, h * 0.5f)
        p.cubicTo(w - k, h * 0.58f, w, h * 0.70f, w, h - r)
        p.quadraticBezierTo(w, h, w - r, h); p.lineTo(r, h); p.quadraticBezierTo(0f, h, 0f, h - r)
        p.cubicTo(0f, h * 0.70f, k, h * 0.58f, k, h * 0.5f)
        p.cubicTo(k, h * 0.42f, 0f, h * 0.30f, 0f, r)
        p.quadraticBezierTo(0f, 0f, r, 0f); p.close()
        return Outline.Generic(p)
    }
}

/** Round top, soft flat bottom. */
class ArchShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width; val h = size.height; val br = w * 0.2f
        val p = Path(); p.moveTo(0f, w / 2f); p.arcTo(Rect(0f, 0f, w, w), 180f, 180f, false)
        p.lineTo(w, h - br); p.quadraticBezierTo(w, h, w - br, h); p.lineTo(br, h); p.quadraticBezierTo(0f, h, 0f, h - br); p.close()
        return Outline.Generic(p)
    }
}

// ---------- "Recommend" UI ----------
class RecoItem(val kind: Int, val title: String, val subtitle: String, val songs: List<Song>)

/** Builds the tiles: genre capsule, organic square, mosaic of your most played, rounded art, waist tile, arch. */
fun buildRecos(pool: List<Song>, seed: Int, genres: Map<String, List<Long>>): List<RecoItem> {
    if (pool.isEmpty()) return emptyList()
    val rnd = Random(seed)
    val byId = pool.associateBy { it.id }
    fun mix(n: Int = 30) = pool.shuffled(rnd).take(n)
    fun byGenre(vararg keys: String): Pair<String, List<Song>>? =
        genres.entries.firstOrNull { e -> keys.any { e.key.contains(it, true) } }
            ?.let { e -> e.key to e.value.mapNotNull { byId[it] } }?.takeIf { it.second.isNotEmpty() }
    val top = pool.filter { (Audio.plays[it.id] ?: 0) > 0 }.sortedByDescending { Audio.plays[it.id] ?: 0 }
    val out = mutableListOf<RecoItem>()
    val e = byGenre("electr", "dance", "house", "techno", "edm")
    val topGenre = genres.entries.filter { en -> en.value.any { it in byId } }.maxByOrNull { it.value.size }
    out += RecoItem(0, (e?.first ?: topGenre?.key ?: "Mix").take(14), "", (e?.second ?: topGenre?.value?.mapNotNull { byId[it] } ?: mix()).shuffled(rnd).take(40))
    val jazz = byGenre("jazz", "chill", "ambient", "lofi", "lo-fi")
    out += RecoItem(1, "Chill: background ${jazz?.first?.lowercase() ?: "mix"}", "Recently updated", (jazz?.second ?: pool.take(30)).take(40))
    out += RecoItem(2, "Good morning", "Often listen to it", (top + mix()).distinctBy { it.id }.take(40))
    out += RecoItem(3, "Fresh picks", "Recently added", pool.take(30))
    out += RecoItem(4, "Late night", "Calm and slow", mix())
    out += RecoItem(5, "On a long journey", "", mix(40))
    for (round in 1..3) for (k in 0..5) out += RecoItem(k, "Mix ${round * 6 + k - 5}", "Made for you", mix())
    return out
}

@Composable fun MosaicCover(songs: List<Song>, modifier: Modifier, corner: Dp) {
    Box(modifier.clip(RoundedCornerShape(corner)).background(Color(0xFF1C1C1E))) {
        val arts = songs.distinctBy { it.albumId }
        if (arts.size < 4) arts.firstOrNull()?.let { AsyncImage(model = it.art, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        else Column(Modifier.fillMaxSize()) {
            for (rr in 0..1) Row(Modifier.weight(1f)) {
                for (cc in 0..1) AsyncImage(model = arts[rr * 2 + cc].art, contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
            }
        }
    }
}

@Composable fun RecoTile(it: RecoItem, onClick: () -> Unit) {
    val art = it.songs.firstOrNull()
    Column(Modifier.tap { onClick() }) {
        when (it.kind) {
            0 -> Box(Modifier.fillMaxWidth().aspectRatio(0.675f).clip(RoundedCornerShape(50)).background(Brush.verticalGradient(
                0f to Color(0xFFFFC08A), 0.35f to Color(0xFFF26A4B), 0.55f to Color(0xFFD7261E), 0.8f to Color(0xFF4A0D08), 1f to Color(0xFF140605))), Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    for (n in 0 until 30) drawRect(Color.Black.copy(alpha = 0.07f * (n % 3)), Offset(size.width * n / 30f, 0f), Size(size.width / 60f, size.height))
                }
                Text(it.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
            1 -> Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)).background(Color(0xFFF2A98C)).padding(6.dp)) {
                if (art != null) AsyncImage(model = art.art, contentDescription = null, modifier = Modifier.fillMaxSize().clip(WaistShape(0.07f)), contentScale = ContentScale.Crop)
            }
            2 -> MosaicCover(it.songs, Modifier.fillMaxWidth().aspectRatio(1f), 22.dp)
            3 -> Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)).background(Color(0xFF1C1C1E))) {
                if (art != null) AsyncImage(model = art.art, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            4 -> Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)).background(Color(0xFF12301F)).padding(8.dp)) {
                if (art != null) AsyncImage(model = art.art, contentDescription = null, modifier = Modifier.fillMaxSize().clip(WaistShape(0.16f)), contentScale = ContentScale.Crop)
            }
            else -> Box(Modifier.fillMaxWidth().aspectRatio(0.675f).clip(ArchShape()).background(Brush.verticalGradient(
                0f to Color(0xFFB4B2C8), 0.55f to Color(0xFFC9A98F), 1f to Color(0xFFD9791E))), Alignment.BottomCenter) {
                Text(it.title, Modifier.padding(horizontal = 8.dp, vertical = 14.dp), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp, textAlign = TextAlign.Center)
            }
        }
        if (it.kind in 1..4) {
            Text(it.title, Modifier.padding(top = 8.dp), color = Color(0xFFF1F1F3), fontSize = 16.sp, lineHeight = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (it.subtitle.isNotBlank()) Text(it.subtitle, color = Color(0xFF8E8E93), fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable fun HomeReco(songs: List<Song>, onProfile: () -> Unit) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableIntStateOf(0) }       // 0 music, 1 books and podcasts, 2 kids
    var seed by remember { mutableIntStateOf(1) }
    val spin by animateFloatAsState(seed * 360f, tween(600), label = "spin")
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { query = it }
    }
    val kidsIds = Audio.playlists.firstOrNull { it.name.equals("Kids", true) }?.songIds ?: emptyList()
    val pool = when (cat) { 1 -> Audio.extras; 2 -> songs.filter { it.id in kidsIds }; else -> songs }
    val genres = Audio.genres
    val recos = remember(pool, seed, genres) { buildRecos(pool, seed, genres) }
    val rows = recos.chunked(3)
    val results = remember(query, songs, Audio.extras) { if (query.isBlank()) emptyList() else (songs + Audio.extras).filter { it.title.contains(query, true) || it.artist.contains(query, true) } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 28.dp).padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE8A33D)).tap { onProfile() }, Alignment.Center) { AvatarContent(Color.Black, 18.sp) }
                Spacer(Modifier.width(14.dp))
                Row(Modifier.weight(1f).height(44.dp).clip(CircleShape).background(Color(0xFF2A2A2E)).padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = Color(0xFF9A9AA0), modifier = Modifier.size(22.dp))
                    BasicTextField(value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 17.sp), cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                        decorationBox = { inner -> Box(contentAlignment = Alignment.CenterStart) { if (query.isEmpty()) Text("Search", color = Color(0xFF9A9AA0), fontSize = 17.sp); inner() } })
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color.Black).tap(true) {
                        try { voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)) }
                        catch (_: Exception) { Toast.makeText(ctx, "Voice search isn't available on this phone", Toast.LENGTH_SHORT).show() }
                    }, Alignment.Center) { Icon(Icons.Rounded.GraphicEq, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Music" to 193f, "Books and podcasts" to 337f, "Kids" to 193f).forEachIndexed { i, (label, wt) ->
                    val on = cat == i
                    Box(Modifier.weight(wt).height(38.dp).clip(CircleShape).background(if (on) Color(0xFFECEFF1) else Color(0xFF1C1C1E)).tap { cat = i }, Alignment.Center) {
                        Text(label, color = if (on) Color.Black else Color(0xFFB0B0B5), fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
        if (query.isNotBlank()) {
            item { Text("Results", Modifier.padding(start = 28.dp, top = 26.dp, bottom = 6.dp), color = Color(0xFFF1F1F3), fontSize = 23.sp) }
            if (results.isEmpty()) item { Text("No songs match your search.", Modifier.padding(28.dp), color = Grey) }
            items(results, key = { it.id }) { sg -> SongRow(sg, results) }
        } else {
            item {
                Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 34.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Recommended for listening", Modifier.weight(1f), color = Color(0xFFF1F1F3), fontSize = 23.sp, lineHeight = 28.sp)
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF1C1C1E)).tap(true) { seed++ }, Alignment.Center) {
                        Icon(Icons.Rounded.Sync, null, tint = Color.White, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = spin })
                    }
                }
            }
            if (pool.isEmpty()) item {
                Column(Modifier.padding(28.dp)) {
                    Text(when (cat) { 1 -> "No audiobooks or podcasts found on this phone."; 2 -> "Add songs to a playlist named Kids and they will show up here."; else -> "Grant storage permission and add music files to your phone" }, color = Grey)
                    if (cat == 2 && Audio.playlists.none { it.name.equals("Kids", true) }) Box(Modifier.padding(top = 14.dp).clip(CircleShape).background(Color(0xFFECEFF1)).tap(true) { Audio.createPlaylist("Kids") }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text("Create Kids playlist", color = Color.Black, fontWeight = FontWeight.Medium)
                    }
                }
            }
            items(rows.size) { ri ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(top = if (ri == 0) 22.dp else 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    rows[ri].forEach { r -> Box(Modifier.weight(1f)) { RecoTile(r) { if (r.songs.isNotEmpty()) Audio.play(r.songs, 0) } } }
                    repeat(3 - rows[ri].size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

// ---------- Stats ----------
fun fmtListen(sec: Long) = if (sec >= 3600) "${sec / 3600}h ${(sec % 3600) / 60}m" else "${sec / 60}m"
fun agoText(t: Long?, now: Long): String {
    if (t == null) return "never"
    val d = (now - t) / 1000
    return when { d < 60 -> "just now"; d < 3600 -> "${d / 60} min ago"; d < 86400 -> "${d / 3600} h ago"; else -> "${d / 86400} d ago" }
}

@Composable fun StatsScreen(songs: List<Song>, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val plays = Audio.plays; val listen = Audio.listen; val last = Audio.lastPlayed
    val byId = (songs + Audio.extras).associateBy { it.id }
    val ranked = plays.entries.sortedByDescending { it.value }.mapNotNull { e -> byId[e.key]?.let { it to e.value } }.take(100)
    val maxP = (ranked.firstOrNull()?.second ?: 1).coerceAtLeast(1)
    val now = System.currentTimeMillis()
    LazyColumn(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBtn(Icons.Rounded.ArrowBack) { onClose() }
                Text("Your stats", Modifier.weight(1f), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Spacer(Modifier.size(42.dp))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("${plays.values.sum()}" to "Plays", fmtListen(listen.values.sum()) to "Listened", "${plays.size}" to "Songs played").forEach { (n, l) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(TileBg).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(n, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Text(l, color = Grey, fontSize = 13.sp)
                    }
                }
            }
        }
        item { Text("How often you play each song", Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        if (ranked.isEmpty()) item { Text("Play some songs and your stats will appear here.", Modifier.padding(20.dp), color = Grey) }
        itemsIndexed(ranked) { i, pr ->
            val sg = pr.first; val n = pr.second
            val label = when { n >= 20 -> "On repeat"; n >= 10 -> "Heavy rotation"; n >= 5 -> "Often"; n >= 2 -> "Sometimes"; else -> "Just started" }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clip(RoundedCornerShape(20.dp)).background(TileBg).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", Modifier.width(28.dp), color = Grey, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Art(sg, Modifier.size(48.dp), RoundedCornerShape(12.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(sg.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$label · listened ${fmtListen(listen[sg.id] ?: 0L)} · last ${agoText(last[sg.id], now)}", color = Grey, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(5.dp).clip(CircleShape).background(Color.White.copy(0.12f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(n.toFloat() / maxP).clip(CircleShape).background(Green))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$n", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (n == 1) "play" else "plays", color = Grey, fontSize = 11.sp)
                }
            }
        }
    }
}

// ---------- Opening animation ----------
const val FLYER_ASPECT = 0.7521f

/** Intro: the flyer collage is cut into slats that swipe in from alternating sides with motion blur, breathe, then swipe out. Tap to skip. */
@Composable fun Splash(onDone: () -> Unit) {
    val view = LocalView.current
    val painter = painterResource(R.drawable.flyers)
    val n = 5
    val pIn = remember { List(n) { Animatable(0f) } }
    val pOut = remember { List(n) { Animatable(0f) } }
    val hold = remember { Animatable(0f) }
    var skip by remember { mutableStateOf(false) }
    BackHandler { skip = true }
    val easeOutExpo = remember { CubicBezierEasing(0.16f, 1f, 0.3f, 1f) }
    val easeInExpo = remember { CubicBezierEasing(0.7f, 0f, 0.84f, 0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            for (i in 0 until n) launch {
                delay(i * 90L); if (i == 0) view.buzz(true)
                pIn[i].animateTo(1f, tween(900, easing = easeOutExpo))
                if (i == n - 1) view.buzz()
            }
            launch { hold.animateTo(1f, tween(3600, easing = LinearEasing)) }
        }
    }
    LaunchedEffect(skip) {
        if (!skip) delay(3000)
        view.buzz(true)
        val dur = if (skip) 380 else 700; val gap = if (skip) 30L else 80L
        coroutineScope { for (i in 0 until n) launch { delay(i * gap); pOut[i].animateTo(1f, tween(dur, easing = easeInExpo)) } }
        onDone()
    }

    Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { detectTapGestures { skip = true } }) {
        for (i in 0 until n) {
            val d = if (i % 2 == 0) 1f else -1f
            Box(Modifier.fillMaxSize().graphicsLayer {
                val w = size.width; val a = pIn[i].value; val o = pOut[i].value
                val drift = d * 10.dp.toPx() * sin(hold.value * Math.PI.toFloat())
                translationX = d * w * 1.1f * (1f - a) - d * w * 1.1f * o + drift
                val z = 1.1f + 0.05f * hold.value; scaleX = z; scaleY = z
                if (Build.VERSION.SDK_INT >= 31) {
                    val bx = 90f * maxOf(1f - a, o)          // horizontal motion blur: strongest while moving fast
                    renderEffect = if (bx > 0.6f) BlurEffect(bx, 0.01f, TileMode.Decal) else null
                }
            }.drawWithContent {
                val imgH = size.width / FLYER_ASPECT; val y0 = (size.height - imgH) / 2f
                clipRect(top = y0 + imgH * i / n - 0.5f, bottom = y0 + imgH * (i + 1) / n + 1f) { this@drawWithContent.drawContent() }
            }) {
                Image(painter, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillWidth)
            }
        }
        val eq = rememberInfiniteTransition(label = "eq")
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp)
            .graphicsLayer { alpha = pIn[n - 1].value * (1f - pOut[0].value) },
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            listOf(520, 640, 460, 700, 580).forEachIndexed { i, ms ->
                val h by eq.animateFloat(8f, 38f, infiniteRepeatable(tween(ms, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b$i")
                Box(Modifier.width(6.dp).height(h.dp).clip(RoundedCornerShape(3.dp)).background(Green))
            }
        }
    }
}

// ---------- Sections bar styles (chosen in Settings) ----------
@Composable fun NavBar(sel: Int, onSelect: (Int) -> Unit, onAction: (Int) -> Unit) {
    if (Audio.uiStyle == 2) { NavBarGlass(sel, onSelect); return }
    when (Audio.navStyle) {
        0 -> NavBarClassic(sel, onSelect)
        1 -> NavBarFluid(sel, onSelect, onAction)
        3 -> NavBarMinimal(sel, onSelect)
        else -> NavBarCapsule(sel, onSelect)
    }
}

/** Style 1: classic bar with icon, label and a pill highlight. */
@Composable fun NavBarClassic(sel: Int, onSelect: (Int) -> Unit) {
    val items = listOf("Home" to Icons.Rounded.Home, "Search" to Icons.Rounded.Search, "Your Library" to Icons.Rounded.LibraryMusic, "Create" to Icons.Rounded.Add)
    Row(Modifier.fillMaxWidth().background(Color.Black).navigationBarsPadding().padding(vertical = 8.dp)) {
        items.forEachIndexed { i, (label, icon) ->
            val on = i == sel
            val pill by animateColorAsState(if (on) Green.copy(0.28f) else Color.Transparent, label = "pill")
            val w by animateDpAsState(if (on) 64.dp else 40.dp, spring(0.6f, 400f), label = "pw")
            Column(Modifier.weight(1f).tap { onSelect(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(w.coerceAtLeast(0.dp), 32.dp).clip(RoundedCornerShape(16.dp)).background(pill), Alignment.Center) {
                    Icon(icon, null, tint = if (on) Color.White else Grey, modifier = Modifier.size(24.dp))
                }
                if (Audio.showLabels) Text(label, color = if (on) Color.White else Grey, fontSize = 11.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

data class NavSec(val label: String, val icon: ImageVector, val tint: List<Color>, val action: ImageVector)

/** Style 2: fluid bubbles with an expanding pill, on a frosted dock. */
@Composable fun NavBarFluid(sel: Int, onSelect: (Int) -> Unit, onAction: (Int) -> Unit) {
    val secs = listOf(
        NavSec("Home", Icons.Rounded.Home, listOf(Green, GreenDark), Icons.Rounded.Refresh),
        NavSec("Search", Icons.Rounded.Search, listOf(Color(0xFF4DA3FF), Color(0xFF1B4DB3)), Icons.Rounded.Shuffle),
        NavSec("Your Library", Icons.Rounded.LibraryMusic, listOf(Color(0xFFB06BFF), Color(0xFF5A22B8)), Icons.Rounded.SortByAlpha),
        NavSec("Create", Icons.Rounded.Add, listOf(Color(0xFFFF9A3D), Color(0xFFD1491F)), Icons.Rounded.Refresh)
    )
    val cur = Audio.current
    val dock = RoundedCornerShape(34.dp)
    BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)) {
        val gap = 8.dp; val c = 52.dp; val inner = 6.dp
        val selW = maxWidth - inner * 2 - (c + gap) * (secs.size - 1)
        Box(Modifier.fillMaxWidth().clip(dock).background(Color(0xFF1C1C1E))
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.45f), Color.White.copy(0.05f), Color.White.copy(0.2f))), dock)) {
            if (cur != null) AsyncImage(model = cur.art, contentDescription = null, modifier = Modifier.matchParentSize().blur(40.dp), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.White.copy(0.16f), Color.Black.copy(0.35f)))))
            Row(Modifier.padding(inner), horizontalArrangement = Arrangement.spacedBy(gap)) {
                secs.forEachIndexed { i, sc -> NavItem(sc, i == sel, selW, c, { onSelect(i) }, { onAction(i) }) }
            }
        }
    }
}

@Composable fun NavItem(sc: NavSec, on: Boolean, selW: Dp, c: Dp, onSelect: () -> Unit, onAction: () -> Unit) {
    val w by animateDpAsState(if (on) selW else c, spring(dampingRatio = 0.78f, stiffness = 260f), label = "w")
    val ic by animateDpAsState(if (on) 32.dp else c, spring(dampingRatio = 0.78f, stiffness = 260f), label = "ic")
    val pad by animateDpAsState(if (on) 10.dp else 0.dp, spring(dampingRatio = 0.78f, stiffness = 260f), label = "pad")
    val pillRaw by animateFloatAsState(if (on) 1f else 0f, spring(stiffness = 300f), label = "pill")
    val pill = pillRaw.coerceIn(0f, 1f)
    val wv = w.coerceIn(c, maxOf(c, selW)); val icv = ic.coerceAtLeast(0.dp); val padv = pad.coerceAtLeast(0.dp)
    Box(Modifier.width(wv).height(c).clip(CircleShape)
        .background(Color.Black.copy(alpha = 0.55f * pill))
        .border(1.dp, Color.White.copy(alpha = 0.16f * pill), CircleShape)
        .then(if (!on) Modifier.tap { onSelect() } else Modifier),
        contentAlignment = Alignment.CenterStart) {
        Box(Modifier.padding(start = padv).size(icv).clip(CircleShape).background(Brush.linearGradient(sc.tint)).border(1.dp, Color.White.copy(0.28f), CircleShape), Alignment.Center) {
            Icon(sc.icon, null, tint = if (sc.tint.first().luminance() > 0.6f) Color.Black else Color.White, modifier = Modifier.size(icv * 0.5f))
        }
        Text(sc.label, Modifier.padding(start = 56.dp).graphicsLayer { alpha = pill }, color = Color.White, fontSize = 17.sp,
            fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
        Box(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(36.dp).graphicsLayer { alpha = pill }.clip(CircleShape)
            .background(Color(0xFF3A3A3C)).then(if (on) Modifier.tap { onAction() } else Modifier), Alignment.Center) {
            Icon(sc.action, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

// ---------- Profile & Settings ----------
@Composable fun ProfileScreen(songCount: Int, onSettings: () -> Unit, onPlaylists: () -> Unit, onStats: () -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    var name by remember { mutableStateOf(Audio.userName) }
    val ctx = LocalContext.current
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            Audio.chooseAvatar(uri.toString())
        }
    }
    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.Rounded.KeyboardArrowDown) { onClose() }
            Text("Profile", Modifier.weight(1f), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.size(42.dp))
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.size(110.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Green, GreenDark))).tap { avatarPicker.launch(arrayOf("image/*")) }, Alignment.Center) {
            AvatarContent(Color.Black, 48.sp)
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(28.dp).background(Color.Black.copy(0.4f)), Alignment.Center) { Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
        }
        TextField(name, { name = it.take(20); Audio.saveName(name) }, Modifier.padding(top = 12.dp), singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Green, unfocusedIndicatorColor = Color.Transparent, cursorColor = Green))
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("$songCount" to "Songs", "${Audio.liked.size}" to "Liked").forEach { (n, l) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(TileBg).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(n, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text(l, color = Grey, fontSize = 13.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(22.dp)).background(TileBg).tap { onStats() }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.BarChart, null, tint = Green)
            Text("Stats", Modifier.weight(1f).padding(start = 14.dp), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Rounded.ChevronRight, null, tint = Grey)
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(22.dp)).background(TileBg).tap { onPlaylists() }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.QueueMusic, null, tint = Green)
            Text("Playlists", Modifier.weight(1f).padding(start = 14.dp), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Rounded.ChevronRight, null, tint = Grey)
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(22.dp)).background(TileBg).tap { onSettings() }.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Settings, null, tint = Green)
            Text("Settings", Modifier.weight(1f).padding(start = 14.dp), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Rounded.ChevronRight, null, tint = Grey)
        }
    }
}

@Composable fun SettingsScreen(onRescan: () -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val ctx = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            val vid = ctx.contentResolver.getType(uri)?.startsWith("video") == true
            Audio.chooseBackground(if (vid) 2 else 1, uri.toString())
        }
    }
    val styles = listOf("Classic" to "Icons with labels and a pill highlight",
        "Fluid" to "Bubbles that stretch into a pill, on a frosted dock",
        "Capsule" to "Hold and swipe left or right on a glowing capsule",
        "Minimal" to "Thin icons with Home, Discovery and Collection")
    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.Rounded.ArrowBack) { onClose() }
            Text("Settings", Modifier.weight(1f), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.size(42.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(22.dp)).background(TileBg).tap { onRescan() }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Refresh, null, tint = Green)
            Text("Rescan music files", Modifier.weight(1f).padding(start = 14.dp), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text("Background", Modifier.padding(top = 18.dp, bottom = 8.dp), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TileBg).padding(16.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionChip("Theme", Audio.bgType == 0) { Audio.chooseBackground(0, "") }
                OptionChip(if (Audio.bgType == 0) "Choose picture or video" else "Change picture or video", Audio.bgType != 0) { picker.launch(arrayOf("image/*", "video/*")) }
            }
            if (Audio.bgType != 0) {
                Text(if (Audio.bgType == 2) "Using your video (muted, loops)" else "Using your picture", Modifier.padding(top = 12.dp), color = Grey, fontSize = 13.sp)
                Text("Darken", Modifier.padding(top = 8.dp), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Slider(value = Audio.bgDim, onValueChange = { Audio.saveDim(it) }, valueRange = 0f..0.9f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(0.25f)))
            }
        }
        Text("App UI", Modifier.padding(top = 18.dp, bottom = 8.dp), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        listOf("Default" to "The original Pixelfy look", "Discovery" to "Mood orbs, daily playlists and big rounded tiles",
            "Glass" to "Frosted glass, glowing orbs and translucent bars",
            "Ember" to "Red glow, glass cards, smart mix and listening tools",
            "Recommend" to "Search, category chips and shaped recommendation tiles").forEachIndexed { i, (name, desc) ->
            val sel = Audio.uiStyle == i
            val shape = RoundedCornerShape(22.dp)
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(shape).background(TileBg)
                .border(if (sel) 2.dp else 0.dp, if (sel) Green else Color.Transparent, shape)
                .tap { Audio.chooseUiStyle(i) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(desc, color = Grey, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    UiPreview(i)
                }
                if (sel) Icon(Icons.Rounded.CheckCircle, null, tint = Green, modifier = Modifier.padding(start = 12.dp))
            }
        }
        Text("Theme colour", Modifier.padding(top = 18.dp, bottom = 8.dp), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TileBg).padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            AccentPalette.forEachIndexed { i, opt ->
                val sel = Audio.accent == i
                Box(Modifier.size(42.dp).clip(CircleShape).tap { Audio.chooseAccent(i) }
                    .border(2.dp, if (sel) Color.White else Color.Transparent, CircleShape).padding(5.dp), Alignment.Center) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(opt.color), Alignment.Center) {
                        if (sel) Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Text("Sections bar style", Modifier.padding(top = 12.dp, bottom = 8.dp), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Show section labels", Modifier.weight(1f), color = Color.White, fontSize = 16.sp)
            Switch(checked = Audio.showLabels, onCheckedChange = { Audio.chooseLabels(it) })
        }
        styles.forEachIndexed { i, (name, desc) ->
            val sel = Audio.navStyle == i
            val shape = RoundedCornerShape(22.dp)
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(shape).background(TileBg)
                .border(if (sel) 2.dp else 0.dp, if (sel) Green else Color.Transparent, shape)
                .tap { Audio.chooseNavStyle(i) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(desc, color = Grey, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    StylePreview(i)
                }
                if (sel) Icon(Icons.Rounded.CheckCircle, null, tint = Green, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

@Composable fun StylePreview(style: Int) {
    val edge = RoundedCornerShape(14.dp)
    Box(Modifier.fillMaxWidth().height(54.dp).clip(edge).background(Color.Black).border(1.dp, Color.White.copy(0.08f), edge), Alignment.Center) {
        when (style) {
            0 -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp, 24.dp).clip(RoundedCornerShape(12.dp)).background(Green.copy(0.28f)), Alignment.Center) {
                    Icon(Icons.Rounded.Home, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                listOf(Icons.Rounded.Search, Icons.Rounded.LibraryMusic, Icons.Rounded.Add).forEach { Icon(it, null, tint = Grey, modifier = Modifier.size(16.dp)) }
            }
            1 -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF4DA3FF), Color(0xFF1B4DB3)))))
                Box(Modifier.size(96.dp, 32.dp).clip(CircleShape).background(Color(0xFF1C1C1E)).border(1.dp, Color.White.copy(0.16f), CircleShape), Alignment.CenterStart) {
                    Box(Modifier.padding(start = 4.dp).size(24.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Green, GreenDark))))
                    Box(Modifier.padding(start = 36.dp).size(36.dp, 6.dp).clip(CircleShape).background(Color.White.copy(0.7f)))
                }
                Box(Modifier.size(32.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFB06BFF), Color(0xFF5A22B8)))))
            }
            2 -> Box(Modifier.size(110.dp, 32.dp).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF2B2B3B), Color(0xFF1D1D29))))
                .border(1.5.dp, Brush.verticalGradient(listOf(Color(0xFF8E8EB8), Color(0xFF4B4B6C))), CircleShape), Alignment.Center) {
                Box(Modifier.size(26.dp, 5.dp).clip(CircleShape).background(Color(0xFF575C70)))
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("Home", color = Color.White, fontSize = 11.sp); Text("Discovery", color = Grey, fontSize = 11.sp); Text("Collection", color = Grey, fontSize = 11.sp)
            }
        }
    }
}

/** Minimal text sections bar: thin line icons with labels; the active section is white, the rest grey. */
@Composable fun NavBarMinimal(sel: Int, onSelect: (Int) -> Unit) {
    val items = listOf("Home", "Discovery", "Collection")
    Row(Modifier.fillMaxWidth().background(Color.Black).navigationBarsPadding().padding(top = 6.dp, bottom = 8.dp)) {
        items.forEachIndexed { i, label ->
            val tint by animateColorAsState(if (i == sel) Color.White else Color(0xFF6E6E6E), tween(200), label = "nt")
            Column(Modifier.weight(1f).tap { onSelect(i) }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                NavGlyph(i, tint, Modifier.size(30.dp))
                if (Audio.showLabels) Text(label, Modifier.padding(top = 6.dp), color = tint, fontSize = 12.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}

@Composable fun NavGlyph(kind: Int, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height; val sw = 1.8.dp.toPx()
        val st = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            0 -> {   // rounded square with two arcs
                drawRoundRect(color, Offset(sw, sw), Size(w - 2 * sw, h - 2 * sw), CornerRadius(w * 0.36f), style = st)
                val c = Offset(w * 0.16f, h * 0.84f)
                for (r in listOf(w * 0.30f, w * 0.52f)) drawArc(color, -90f, 90f, false, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), style = st)
            }
            1 -> {   // magnifier
                drawCircle(color, w * 0.30f, Offset(w * 0.44f, h * 0.42f), style = st)
                drawLine(color, Offset(w * 0.65f, h * 0.64f), Offset(w * 0.82f, h * 0.82f), sw, StrokeCap.Round)
            }
            3 -> {   // house
                val p = Path(); p.moveTo(w * 0.14f, h * 0.86f); p.lineTo(w * 0.14f, h * 0.45f); p.lineTo(w * 0.5f, h * 0.14f)
                p.lineTo(w * 0.86f, h * 0.45f); p.lineTo(w * 0.86f, h * 0.86f); p.close()
                drawPath(p, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.cornerPathEffect(w * 0.14f)))
            }
            else -> {   // collection: three slanted lines and one leaning the other way
                for (x in listOf(0.20f, 0.38f, 0.56f)) drawLine(color, Offset((x + 0.06f) * w, h * 0.14f), Offset((x - 0.06f) * w, h * 0.86f), sw, StrokeCap.Round)
                drawLine(color, Offset(0.74f * w, 0.14f * h), Offset(0.86f * w, 0.86f * h), sw, StrokeCap.Round)
            }
        }
    }
}

/** Capsule sections bar. Hold and swipe left/right on the capsule to switch sections; tap to peek at where you are. */
@Composable fun NavBarCapsule(sel: Int, onSelect: (Int) -> Unit) {
    val secs = listOf("Home" to Icons.Rounded.Home, "Search" to Icons.Rounded.Search, "Your Library" to Icons.Rounded.LibraryMusic, "Create" to Icons.Rounded.Add)
    val view = LocalView.current
    val step = with(LocalDensity.current) { 44.dp.toPx() }
    var pressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var peek by remember { mutableStateOf(false) }
    var peekTick by remember { mutableIntStateOf(0) }
    var acc by remember { mutableFloatStateOf(0f) }
    val curSel by rememberUpdatedState(sel)
    val pick by rememberUpdatedState(onSelect)
    LaunchedEffect(peekTick) { if (peekTick > 0) { peek = true; delay(1400); peek = false } }
    val open = pressed || dragging || peek
    val w by animateDpAsState(if (open) 300.dp else 164.dp, spring(dampingRatio = 0.72f, stiffness = 320f), label = "w")
    val h by animateDpAsState(if (open) 58.dp else 50.dp, spring(dampingRatio = 0.72f, stiffness = 320f), label = "h")
    val dash by animateFloatAsState(if (open) 0f else 1f, tween(160), label = "dash")
    val items by animateFloatAsState(if (open) 1f else 0f, tween(220), label = "items")
    val glow = Color(0xFF8A8AC8)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 10.dp, bottom = 10.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(w.coerceAtLeast(0.dp), h.coerceAtLeast(0.dp))
            .shadow(16.dp, CircleShape, clip = false, ambientColor = glow, spotColor = glow)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2B2B3B), Color(0xFF1D1D29))))
            .border(1.5.dp, Brush.verticalGradient(listOf(Color(0xFF8E8EB8), Color(0xFF4B4B6C))), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = { pressed = true; view.buzz(); tryAwaitRelease(); pressed = false }, onTap = { peekTick++ })
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { acc = 0f; dragging = true },
                    onDragEnd = { dragging = false; peekTick++ },
                    onDragCancel = { dragging = false }
                ) { change, d ->
                    change.consume(); acc += d
                    if (acc > step && curSel < secs.lastIndex) { acc = 0f; pick(curSel + 1); view.buzz(true); peekTick++ }
                    else if (acc < -step && curSel > 0) { acc = 0f; pick(curSel - 1); view.buzz(true); peekTick++ }
                }
            }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(40.dp, 8.dp).graphicsLayer { alpha = dash.coerceIn(0f, 1f) }.clip(RoundedCornerShape(4.dp)).background(Color(0xFF575C70)))
            Row(Modifier.fillMaxSize().padding(horizontal = 8.dp).graphicsLayer { alpha = items.coerceIn(0f, 1f) },
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                secs.forEachIndexed { i, (label, icon) ->
                    val on = i == sel
                    val bg by animateColorAsState(if (on) Color.White.copy(0.16f) else Color.Transparent, label = "b")
                    Row(Modifier.height(40.dp).animateContentSize(spring(dampingRatio = 0.8f, stiffness = 400f)).clip(CircleShape).background(bg)
                        .padding(horizontal = if (on) 12.dp else 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, label, tint = if (on) Color.White else Color(0xFF8A8FA3), modifier = Modifier.size(22.dp))
                        if (on) Text(label, Modifier.padding(start = 6.dp), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable fun TonalBtn(icon: ImageVector, fg: Color, onClick: () -> Unit) =
    Box(Modifier.size(56.dp).clip(RoundedCornerShape(20.dp)).background(fg.copy(0.16f)).tap { onClick() }, Alignment.Center) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(28.dp))
    }

@Composable fun LoadingBlob(c: Color) {
    val tr = rememberInfiniteTransition(label = "lb")
    val rot by tr.animateFloat(0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "r")
    val sc by tr.animateFloat(0.8f, 1.1f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    Spikes(Modifier.size(56.dp).graphicsLayer { rotationZ = rot; scaleX = sc; scaleY = sc }, 9, 0.78f, c)
}

/** Expressive wavy progress bar: the played part waves while music plays; drag or tap to seek. */
@Composable fun WavySeek(value: Float, playing: Boolean, color: Color, onChange: (Float) -> Unit, onFinish: () -> Unit) {
    val tr = rememberInfiniteTransition(label = "w")
    val phase by tr.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "p")
    val amp by animateFloatAsState(if (playing) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 80f), label = "a")
    val change by rememberUpdatedState(onChange)
    val finish by rememberUpdatedState(onFinish)
    Canvas(Modifier.fillMaxWidth().height(40.dp)
        .pointerInput(Unit) { detectTapGestures { change((it.x / size.width).coerceIn(0f, 1f)); finish() } }
        .pointerInput(Unit) { detectHorizontalDragGestures(onDragEnd = { finish() }, onDragCancel = { finish() }) { c, _ -> change((c.position.x / size.width).coerceIn(0f, 1f)) } }
    ) {
        val w = size.width; val cy = size.height / 2; val stroke = 5.dp.toPx(); val x = w * value.coerceIn(0f, 1f)
        val a = 6.dp.toPx() * amp; val wl = 28.dp.toPx(); val twoPi = (2 * Math.PI).toFloat()
        drawLine(color.copy(0.25f), Offset(x, cy), Offset(w, cy), stroke, StrokeCap.Round)
        val p = Path(); p.moveTo(0f, cy + a * sin(phase))
        var px = 4f
        while (px <= x) { p.lineTo(px, cy + a * sin(px / wl * twoPi + phase)); px += 4f }
        drawPath(p, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawRoundRect(color, Offset(x - 3.dp.toPx(), cy - 14.dp.toPx()), Size(6.dp.toPx(), 28.dp.toPx()), CornerRadius(3.dp.toPx()))
    }
}
