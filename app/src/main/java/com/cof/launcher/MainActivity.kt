@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cof.launcher

import android.content.Intent
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.random.Random

private val Red = Color(0xFFB3121F)
private val RedBright = Color(0xFFE02030)
private val TextSoft = Color(0xFFB8B8C4)

// ===== EDIT THESE =====
private const val CREATOR_NAME = "why01"
private const val WHATSAPP_URL = "https://wa.me/6282128556103"
private const val SAWERIA_URL = "https://saweria.co/why01"
// ======================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(color = Color.Transparent, contentColor = Color.White) {
                    Box(Modifier.fillMaxSize()) {
                        Background()
                        var showControls by remember { mutableStateOf(false) }
                        BackHandler(enabled = showControls) { showControls = false }
                        if (showControls) ControlsScreen { showControls = false }
                        else Launcher { showControls = true }
                    }
                }
            }
        }
    }
}

@Composable
fun Launcher(onOpenControls: () -> Unit) {
    val ctx = LocalContext.current
    var cfg by remember { mutableStateOf(Prefs.load(ctx)) }
    fun update(n: GameConfig) { cfg = n; Prefs.save(ctx, n) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            ctx.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            update(cfg.copy(gameUri = uri.toString()))
        }
    }

    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // ===== Panel kiri =====
        Panel(Modifier.weight(0.4f).fillMaxHeight()) {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "CRY OF FEAR",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = RedBright
                )
                Text("Android Launcher", fontSize = 13.sp, color = TextSoft)
                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = { picker.launch(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A36), contentColor = Color.White)
                ) { Text("SELECT GAME FOLDER", fontWeight = FontWeight.Bold) }

                Spacer(Modifier.height(8.dp))
                Text(
                    if (cfg.gameUri.isEmpty()) "No game folder selected" else "Game folder selected",
                    fontSize = 12.sp,
                    color = if (cfg.gameUri.isEmpty()) TextSoft else Color(0xFF6FD08C)
                )

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onOpenControls,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A36), contentColor = Color.White)
                ) { Text("TOUCH CONTROLS", fontWeight = FontWeight.Bold) }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { GameLauncher.launch(ctx, cfg) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Red,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF3A1A1E),
                        disabledContentColor = Color(0xFF8A6A6E)
                    )
                ) { Text("START GAME", fontWeight = FontWeight.Bold) }

                Spacer(Modifier.height(22.dp))
                Text("Created by $CREATOR_NAME", fontSize = 12.sp, color = TextSoft)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { openLink(ctx, WHATSAPP_URL) }) {
                        Text("WhatsApp", color = Color(0xFF6FD08C), fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { openLink(ctx, SAWERIA_URL) }) {
                        Text("Saweria", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    "Unofficial launcher. Cry of Fear belongs to Team Psykskallar.",
                    fontSize = 10.sp,
                    color = Color(0xFF7C7C8A)
                )
            }
        }

        // ===== Panel kanan (pengaturan) =====
        Column(
            Modifier.weight(0.6f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Options("Resolution", listOf("360p", "480p", "540p", "720p", "900p", "1080p"), cfg.resolution) {
                update(cfg.copy(resolution = it))
            }
            Options("FPS (Frames per Second)", listOf("20", "24", "30", "60"), cfg.fps.toString()) {
                update(cfg.copy(fps = it.toInt()))
            }
            Options(
                "Aspect Ratio", listOf("16:9", "Esticar"), cfg.aspect,
                label = { if (it == "Esticar") "Stretch" else it }
            ) { update(cfg.copy(aspect = it)) }
            Options(
                "Screen Refresh Rate", listOf("Auto", "60", "90", "120"), cfg.refreshHz,
                label = { if (it == "Auto") "Auto" else "$it Hz" }
            ) { update(cfg.copy(refreshHz = it)) }
            Toggle("FXAA", "Smooth jagged edges (anti-aliasing)", cfg.fxaa) { update(cfg.copy(fxaa = it)) }
            Toggle("FPS Counter", "Show FPS while playing", cfg.showFps) { update(cfg.copy(showFps = it)) }
        }
    }
}

@Composable
fun Options(
    title: String,
    items: List<String>,
    selected: String,
    label: (String) -> String = { it },
    onSelect: (String) -> Unit
) {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach {
                    FilterChip(
                        selected = it == selected,
                        onClick = { onSelect(it) },
                        label = { Text(label(it)) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color(0xFFDDDDE6),
                            selectedContainerColor = Red,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun Toggle(title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, fontSize = 12.sp, color = TextSoft)
            }
            Switch(
                checked = value,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Red, checkedThumbColor = Color.White)
            )
        }
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xCC0D0D14),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color(0x55E02030)),
        content = content
    )
}

/** Background: hujan animasi selalu ada; kalau ada res/raw/bg_video.mp4, video diputar di atasnya (full layar). */
@Composable
fun Background() {
    val ctx = LocalContext.current
    val videoId = remember { ctx.resources.getIdentifier("bg_video", "raw", ctx.packageName) }
    Box(Modifier.fillMaxSize()) {
        RainBackground()
        if (videoId != 0) VideoBackground(videoId)
        Box(Modifier.fillMaxSize().background(Color(0x55000000)))
    }
}

/** Video diputar center-crop supaya memenuhi seluruh layar tanpa pinggiran kosong. */
@Composable
fun VideoBackground(resId: Int) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { c ->
            val tv = TextureView(c)
            var mp: MediaPlayer? = null
            var vw = 0
            var vh = 0

            fun crop() {
                val w = tv.width.toFloat()
                val h = tv.height.toFloat()
                if (vw == 0 || vh == 0 || w == 0f || h == 0f) return
                val scale = maxOf(w / vw, h / vh)
                val m = Matrix()
                m.setScale(vw * scale / w, vh * scale / h, w / 2f, h / 2f)
                tv.setTransform(m)
            }

            tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                    try {
                        val p = MediaPlayer()
                        p.setDataSource(c, Uri.parse("android.resource://" + c.packageName + "/" + resId))
                        p.setSurface(Surface(st))
                        p.isLooping = true
                        p.setVolume(0f, 0f)
                        p.setOnVideoSizeChangedListener { _, w, h -> vw = w; vh = h; crop() }
                        p.setOnPreparedListener { it.start() }
                        p.prepareAsync()
                        mp = p
                    } catch (e: Exception) {
                    }
                }

                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) { crop() }

                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                    mp?.release()
                    mp = null
                    return true
                }

                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
            }
            tv
        }
    )
}

@Composable
fun RainBackground() {
    val infinite = rememberInfiniteTransition(label = "bg")
    val t by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "rain"
    )
    val pulse by infinite.animateFloat(
        0.08f, 0.28f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    // x, y0, speed (integer so the loop is seamless)
    val drops = remember { List(110) { Triple(Random.nextFloat(), Random.nextFloat(), 1 + Random.nextInt(3)) } }
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF07070C), Color(0xFF14060A))))
        drawRect(
            Brush.radialGradient(
                listOf(Color(0xFF8B0000).copy(alpha = pulse), Color.Transparent),
                center = Offset(size.width * 0.3f, size.height * 0.85f),
                radius = size.width * 0.7f
            )
        )
        drops.forEach { (x, y0, sp) ->
            val y = ((y0 + t * sp) % 1f) * size.height
            val px = x * size.width + y * 0.08f
            drawLine(
                Color(0x33AEB8CC),
                Offset(px, y),
                Offset(px - 5f * sp, y + 16f * sp),
                strokeWidth = 1.5f + sp * 0.5f
            )
        }
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, Color(0xCC000000)),
                center = center,
                radius = size.maxDimension * 0.75f
            )
        )
    }
}


// ===================== In-game touch controls (FPS layout example) =====================

@Composable
fun ControlsScreen(onBack: () -> Unit) {
    var last by remember { mutableStateOf("Touch a button") }

    Box(Modifier.fillMaxSize()) {
        // LOOK area: drag anywhere on the right half to aim the camera
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(0.5f).align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectDragGestures { change, d ->
                        change.consume()
                        last = "LOOK  ${d.x.roundToInt()}, ${d.y.roundToInt()}"
                    }
                }
        )
        // crosshair
        Canvas(Modifier.align(Alignment.Center).size(28.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            val col = Color(0xCCFFFFFF)
            drawLine(col, Offset(c.x - 12f, c.y), Offset(c.x - 4f, c.y), strokeWidth = 3f)
            drawLine(col, Offset(c.x + 4f, c.y), Offset(c.x + 12f, c.y), strokeWidth = 3f)
            drawLine(col, Offset(c.x, c.y - 12f), Offset(c.x, c.y - 4f), strokeWidth = 3f)
            drawLine(col, Offset(c.x, c.y + 4f), Offset(c.x, c.y + 12f), strokeWidth = 3f)
        }
        // status
        Text(
            last, Modifier.align(Alignment.TopCenter).padding(top = 20.dp),
            color = TextSoft, fontSize = 13.sp
        )
        // back
        Button(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xAA2A2A36), contentColor = Color.White)
        ) { Text("< BACK", fontWeight = FontWeight.Bold) }

        // MOVE joystick (left)
        Joystick(Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 36.dp)) { v ->
            last = if (v == Offset.Zero) "MOVE  stop" else "MOVE  ${(v.x * 100).roundToInt()}, ${(-v.y * 100).roundToInt()}"
        }

        // Main action cluster (bottom right)
        GameButton("FIRE", 84.dp, Red, Modifier.align(Alignment.BottomEnd).padding(end = 36.dp, bottom = 36.dp)) { last = if (it) "FIRE" else "released" }
        GameButton("AIM", 60.dp, Color(0xFF3B82F6), Modifier.align(Alignment.BottomEnd).padding(end = 136.dp, bottom = 40.dp)) { last = if (it) "AIM" else "released" }
        GameButton("CROUCH", 56.dp, Color(0xFF8B8B99), Modifier.align(Alignment.BottomEnd).padding(end = 212.dp, bottom = 44.dp)) { last = if (it) "CROUCH" else "released" }
        GameButton("JUMP", 60.dp, Color(0xFF22C55E), Modifier.align(Alignment.BottomEnd).padding(end = 124.dp, bottom = 128.dp)) { last = if (it) "JUMP" else "released" }
        GameButton("RELOAD", 56.dp, Color(0xFFF59E0B), Modifier.align(Alignment.BottomEnd).padding(end = 36.dp, bottom = 136.dp)) { last = if (it) "RELOAD" else "released" }

        // Quick actions (top right)
        GameButton("LIGHT", 52.dp, Color(0xFFEAB308), Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 20.dp)) { last = if (it) "FLASHLIGHT" else "released" }
        GameButton("USE", 52.dp, Color(0xFF14B8A6), Modifier.align(Alignment.TopEnd).padding(end = 84.dp, top = 20.dp)) { last = if (it) "USE" else "released" }
        GameButton("KNIFE", 52.dp, Color(0xFFA855F7), Modifier.align(Alignment.TopEnd).padding(end = 144.dp, top = 20.dp)) { last = if (it) "MELEE" else "released" }
        GameButton("ITEM", 52.dp, Color(0xFFEC4899), Modifier.align(Alignment.TopEnd).padding(end = 204.dp, top = 20.dp)) { last = if (it) "NEXT ITEM" else "released" }
    }
}

@Composable
fun GameButton(label: String, size: Dp, color: Color, modifier: Modifier = Modifier, onPress: (Boolean) -> Unit) {
    var down by remember { mutableStateOf(false) }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (down) color.copy(alpha = 0.85f) else color.copy(alpha = 0.30f))
            .border(2.dp, color, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    down = true
                    onPress(true)
                    tryAwaitRelease()
                    down = false
                    onPress(false)
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = if (size >= 80.dp) 16.sp else 10.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable
fun Joystick(modifier: Modifier = Modifier, onMove: (Offset) -> Unit) {
    val radius = 60.dp
    val rPx = with(LocalDensity.current) { radius.toPx() }
    var thumb by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier
            .size(radius * 2)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .border(2.dp, Color(0x88FFFFFF), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { thumb = Offset.Zero; onMove(Offset.Zero) },
                    onDragCancel = { thumb = Offset.Zero; onMove(Offset.Zero) }
                ) { change, drag ->
                    change.consume()
                    var n = thumb + drag
                    val len = n.getDistance()
                    if (len > rPx) n = n * (rPx / len)
                    thumb = n
                    onMove(n / rPx)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .offset { IntOffset(thumb.x.roundToInt(), thumb.y.roundToInt()) }
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xCCB3121F))
        )
    }
}

private fun openLink(c: android.content.Context, url: String) {
    if (url.contains("XXXX") || url.contains("YOUR")) {
        android.widget.Toast.makeText(c, "Link not set yet", android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    try {
        c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        android.widget.Toast.makeText(c, "Cannot open link", android.widget.Toast.LENGTH_SHORT).show()
    }
}
