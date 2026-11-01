package ru.tv34.launcher

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val RU = Locale("ru", "RU")
private val textShadow = Shadow(Color(0x99000000), Offset(0f, 2f), 8f)

@Composable
private fun T(
    text: String,
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    alpha: Float = 1f,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start,
) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = Color.White.copy(alpha = alpha),
            fontSize = size.sp,
            fontWeight = weight,
            textAlign = align,
            shadow = textShadow,
        ),
    )
}

@Composable
fun LauncherScreen(resumeTick: Int) {
    val ctx = LocalContext.current
    BackHandler(enabled = true) { /* лаунчер не закрывается кнопкой «Назад» */ }

    var wallpaper by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            WallpaperRepo.load(ctx)?.let { wallpaper = it }
            delay(30 * 60 * 1000L)
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0B10))) {
        Crossfade(targetState = wallpaper, animationSpec = tween(900), label = "wallpaper") { bmp ->
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color(0xFF1E3A6B), Color(0xFF0B0B10)))
                    )
                )
            }
        }
        // затемнение сверху и снизу для читаемости
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0x99000000), Color(0x11000000), Color(0x11000000), Color(0xCC000000))
                )
            )
        )

        Column(Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 40.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
                ClockWidget()
                PlayTimeWidget()
                BoostWidget()
            }
            Spacer(Modifier.weight(1f))
            AppRow(resumeTick)
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ───────────────────────── Виджеты ─────────────────────────

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .clip(shape)
            .background(Color(0x99101015))
            .border(
                if (focused) 3.dp else 1.dp,
                Color.White.copy(alpha = if (focused) 0.95f else 0.14f),
                shape,
            )
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) { content() }
}

@Composable
private fun ClockWidget() {
    val tz = remember { TimeZone.getTimeZone("Europe/Moscow") }
    val timeFmt = remember { SimpleDateFormat("HH:mm", RU).apply { timeZone = tz } }
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM", RU).apply { timeZone = tz } }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L - now % 1000L)
        }
    }
    val date = dateFmt.format(Date(now)).replaceFirstChar { it.uppercase() }
    GlassPanel {
        Column {
            T(timeFmt.format(Date(now)), 46, FontWeight.Light)
            T("Москва · $date", 15, alpha = 0.8f)
        }
    }
}

@Composable
private fun PlayTimeWidget() {
    var uptime by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            uptime = SystemClock.elapsedRealtime()
            delay(30_000)
        }
    }
    val totalMin = uptime / 60_000
    GlassPanel {
        Column {
            T("Вы играли:", 15, alpha = 0.8f)
            T("${totalMin / 60} ч ${totalMin % 60} мин", 32, FontWeight.Medium)
        }
    }
}

@Composable
private fun BoostWidget() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var focused by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var ram by remember { mutableStateOf(Booster.ramInfo(ctx)) }
    LaunchedEffect(Unit) {
        while (true) {
            if (!busy) ram = Booster.ramInfo(ctx)
            delay(5000)
        }
    }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(160), label = "boostScale")
    val used = (1f - ram.first.toFloat() / ram.second.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)

    GlassPanel(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .clickable {
                if (!busy) {
                    busy = true
                    status = "Очищаю…"
                    scope.launch {
                        val freed = Booster.boost(ctx)
                        ram = Booster.ramInfo(ctx)
                        status = if (freed > 0) "Освобождено ${freed / (1024 * 1024)} МБ" else "ТВ уже чистый"
                        busy = false
                        delay(4000)
                        status = ""
                    }
                }
            },
        focused = focused,
    ) {
        Column(Modifier.width(250.dp)) {
            T("Ускорить ТВ", 22, FontWeight.SemiBold)
            T(if (status.isNotEmpty()) status else "Очистить кэш и память", 15, alpha = 0.8f)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().height(6.dp)
                    .clip(RoundedCornerShape(3.dp)).background(Color(0x33FFFFFF))
            ) {
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(used).clip(RoundedCornerShape(3.dp))
                        .background(if (used > 0.85f) Color(0xFFFF453A) else Color(0xFF32D74B))
                )
            }
            Spacer(Modifier.height(6.dp))
            T("Свободно ${gb(ram.first)} из ${gb(ram.second)} ГБ", 13, alpha = 0.7f)
        }
    }
}

private fun gb(bytes: Long): String = String.format(RU, "%.1f", bytes / 1024.0 / 1024.0 / 1024.0)

// ───────────────────────── Плитки приложений ─────────────────────────

@Composable
private fun AppRow(resumeTick: Int) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(400)
        try { first.requestFocus() } catch (_: Exception) { }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 24.dp
        val count = Apps.list.size
        val tileW = ((maxWidth - gap * (count - 1)) / count).coerceAtMost(280.dp)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Top,
        ) {
            Apps.list.forEachIndexed { i, app ->
                AppTile(app, tileW, if (i == 0) Modifier.focusRequester(first) else Modifier, resumeTick)
            }
        }
    }
}

private class Art(val image: ImageBitmap, val banner: Boolean)

private fun loadArt(ctx: Context, pkg: String): Art? = try {
    val pm = ctx.packageManager
    val banner = pm.getApplicationInfo(pkg, 0).loadBanner(pm)
    if (banner != null) {
        Art(banner.toBitmap(640, 360).asImageBitmap(), true)
    } else {
        Art(pm.getApplicationIcon(pkg).toBitmap(256, 256).asImageBitmap(), false)
    }
} catch (e: Exception) {
    null
}

@Composable
private fun AppTile(app: TvApp, width: Dp, focusMod: Modifier, tick: Int) {
    val ctx = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.14f else 1f, tween(180), label = "tileScale")
    val labelAlpha by animateFloatAsState(if (focused) 1f else 0.7f, tween(180), label = "labelAlpha")
    val pkg = remember(tick) { AppLauncher.resolve(ctx, app) }
    val art = remember(pkg) { pkg?.let { loadArt(ctx, it) } }
    val shape = RoundedCornerShape(22.dp)

    Column(Modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            focusMod
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(if (focused) 30.dp else 8.dp, shape)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(app.top, app.bottom)))
                .onFocusChanged { focused = it.isFocused }
                .clickable { AppLauncher.open(ctx, app) }
        ) {
            when {
                art != null && art.banner -> Image(
                    bitmap = art.image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                art != null -> Image(
                    bitmap = art.image,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).fillMaxHeight(0.56f).aspectRatio(1f),
                )
                else -> {
                    T(
                        app.title.take(1).uppercase(), 54, FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    BasicText(
                        text = "Скачать",
                        style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x40FFFFFF))
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }
            }
            // «стеклянный» блик как на tvOS
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(listOf(Color(0x26FFFFFF), Color.Transparent))
                )
            )
            Box(
                Modifier.matchParentSize().border(
                    if (focused) 3.dp else 1.dp,
                    Color.White.copy(alpha = if (focused) 0.95f else 0.15f),
                    shape,
                )
            )
        }
        Spacer(Modifier.height(22.dp))
        T(app.title, 18, FontWeight.Medium, alpha = labelAlpha, align = TextAlign.Center, modifier = Modifier.alpha(1f))
    }
}
