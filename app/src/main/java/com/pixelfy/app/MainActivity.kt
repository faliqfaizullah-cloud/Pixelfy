package com.pixelfy.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
    val tabIdx = if (Audio.navStyle == 3 || glass) tab.coerceAtMost(2) else tab
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
    LaunchedEffect(granted, tick) { if (granted) songs = withContext(Dispatchers.IO) { scanSongs(ctx) } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
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
                when (t) {
                    0 -> when (Audio.uiStyle) {
                        1 -> HomeDiscovery(songs) { screen = 1 }
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
        NavBar(tabIdx, { tab = it }) { i ->
            when (i) {
                0, 3 -> tick++
                1 -> if (songs.isNotEmpty()) Audio.play(songs.shuffled(), 0)
                else -> sortAz = !sortAz
            }
        }
    }
    if (glass) Box(Modifier.fillMaxSize()) {
        content()
        Column(Modifier.align(Alignment.BottomCenter).onSizeChanged { barsH = with(density) { it.height.toDp() } }) { bars() }
    } else Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { content() }
        bars()
    }
    AnimatedVisibility(full && Audio.current != null, enter = slideInVertically { it }, exit = slideOutVertically { it }) { NowPlaying { full = false } }
    AnimatedVisibility(screen >= 1, enter = slideInVertically { it }, exit = slideOutVertically { it }) { ProfileScreen(songs.size, { screen = 2 }) { screen = 0 } }
    AnimatedVisibility(screen == 2, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) { SettingsScreen({ tick++ }) { screen = 1 } }
    if (splash) Splash { splash = false }
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
                Box(Modifier.size(34.dp).clip(CircleShape).background(TileBg).tap { onProfile() }, Alignment.Center) { Text(Audio.userName.trim().take(1).uppercase().ifEmpty { "J" }, color = Green, fontWeight = FontWeight.Bold) }
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
    val glass = Audio.uiStyle == 2
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
    var showLyrics by remember { mutableStateOf(false) }
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
                Text(label, Modifier.padding(top = 2.dp), color = tint, fontSize = 12.sp)
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
                Text(label, color = if (on) Color.White else Grey, fontSize = 11.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
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
@Composable fun ProfileScreen(songCount: Int, onSettings: () -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    var name by remember { mutableStateOf(Audio.userName) }
    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.Rounded.KeyboardArrowDown) { onClose() }
            Text("Profile", Modifier.weight(1f), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.size(42.dp))
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.size(110.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Green, GreenDark))), Alignment.Center) {
            Text(name.trim().take(1).uppercase().ifEmpty { "J" }, color = Color.Black, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
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
        Text("App UI", Modifier.padding(top = 18.dp, bottom = 8.dp), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        listOf("Default" to "The original Pixelfy look", "Discovery" to "Mood orbs, daily playlists and big rounded tiles",
            "Glass" to "Frosted glass, glowing orbs and translucent bars").forEachIndexed { i, (name, desc) ->
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
                Text(label, Modifier.padding(top = 6.dp), color = tint, fontSize = 12.sp, fontWeight = FontWeight.Normal)
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
