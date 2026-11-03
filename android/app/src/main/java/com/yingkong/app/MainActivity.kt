package com.yingkong.app

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val Ink = Color(0xFF1D1D1F)
private val Muted = Color(0xFF86868B)
private val Tertiary = Color(0xFFA9A9AE)
private val Orange = Color(0xFFFF6900)
private val OrangeSoft = Color(0x1AFF6900)
private val Green = Color(0xFF34C759)
private val Blue = Color(0xFF0A84FF)
private val Purple = Color(0xFFAF52DE)
private val Teal = Color(0xFF30B0C7)
private val Amber = Color(0xFFFFB340)
private val Hairline = Color(0x14606067)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })
        setContent { YingKongApp() }
    }
}

@Composable
private fun YingKongApp() {
    var snap by remember { mutableStateOf<Snapshot?>(null) }

    val fpsHistory = remember { mutableStateListOf<Float>() }
    val tempHistory = remember { mutableStateListOf<Float>() }
    val powerHistory = remember { mutableStateListOf<Float>() }
    val cpuHistory = remember { mutableStateListOf<Float>() }
    val gpuHistory = remember { mutableStateListOf<Float>() }
    val ramHistory = remember { mutableStateListOf<Float>() }
    val battHistory = remember { mutableStateListOf<Float>() }
    val gpuFreqHistory = remember { mutableStateListOf<Float>() }
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(tab) {
        if (tab != 0) return@LaunchedEffect
        while (true) {
            val s = withContext(Dispatchers.IO) { RootShell.snapshot() }
            snap = s
            if (s != null) {
                if (s.fps > 0f) {
                    fpsHistory.add(s.fps)
                    if (fpsHistory.size > 48) fpsHistory.removeAt(0)
                }
                tempHistory.add(s.thermal); if (tempHistory.size > 24) tempHistory.removeAt(0)
                powerHistory.add(s.power); if (powerHistory.size > 24) powerHistory.removeAt(0)
                cpuHistory.add(s.cpu); if (cpuHistory.size > 24) cpuHistory.removeAt(0)
                gpuHistory.add(s.gpu); if (gpuHistory.size > 24) gpuHistory.removeAt(0)
                ramHistory.add(s.ramUsedGb); if (ramHistory.size > 24) ramHistory.removeAt(0)
                battHistory.add(s.batt); if (battHistory.size > 24) battHistory.removeAt(0)
                gpuFreqHistory.add(s.gpuFreq); if (gpuFreqHistory.size > 24) gpuFreqHistory.removeAt(0)
            }
            delay(1000)
        }
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        RecorderStore.load(context)
        val prefs = context.getSharedPreferences(RecorderService.PREFS, android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean(RecorderService.KEY_OVERLAY, false) && Settings.canDrawOverlays(context)) {
            ContextCompat.startForegroundService(
                context,
                RecorderService.controlIntent(context, RecorderService.ACTION_SHOW_OVERLAY)
            )
        }
    }

    val appLabel = remember(snap?.fgPackage) {
        val pkg = snap?.fgPackage
        if (pkg == null) null else try {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    @Suppress("DEPRECATION")
    val refreshHz = remember {
        val wm = context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
        wm.defaultDisplay.refreshRate
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF7F6F9), Color(0xFFF0F1F5))))
            .drawBehind {
                val w = size.width
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x40FFA66E), Color.Transparent),
                        center = Offset(w * 0.05f, 0f),
                        radius = w * 0.9f
                    ),
                    radius = w * 0.9f,
                    center = Offset(w * 0.05f, 0f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x2EBEAEFF), Color.Transparent),
                        center = Offset(w * 0.98f, w * 0.12f),
                        radius = w * 0.8f
                    ),
                    radius = w * 0.8f,
                    center = Offset(w * 0.98f, w * 0.12f)
                )
            }
    ) {
        if (tab == 0) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 2.dp, bottom = 132.dp)
        ) {
            TopBar(deviceName = Build.MODEL)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("控制台", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink, letterSpacing = (-0.6).sp)
                Spacer(Modifier.width(8.dp))
                LivePill(if (snap != null) "LIVE" else "OFFLINE")
            }
            Spacer(Modifier.height(4.dp))
            StatusLine(rootOk = snap != null)
            Spacer(Modifier.height(13.dp))

            HeroCard(
                appLabel = appLabel,
                fps = snap?.fps ?: 0f,
                p99 = snap?.p99Ms ?: 0f,
                jank = snap?.jankPct ?: 0f,
                refreshHz = refreshHz,
                available = snap != null,
                history = fpsHistory
            )
            Spacer(Modifier.height(10.dp))

            MetricGrid(
                snap = snap,
                cpuHistory = cpuHistory,
                gpuHistory = gpuHistory,
                ramHistory = ramHistory,
                tempHistory = tempHistory,
                powerHistory = powerHistory,
                battHistory = battHistory
            )
            Spacer(Modifier.height(10.dp))

            ProcessCard(processes = snap?.processes.orEmpty())
            Spacer(Modifier.height(10.dp))

            CoresCard(snap = snap)
            Spacer(Modifier.height(10.dp))

            TimelineCard(
                snap = snap,
                fpsHistory = fpsHistory,
                tempHistory = tempHistory,
                powerHistory = powerHistory,
                gpuFreqHistory = gpuFreqHistory
            )
        }
        } else if (tab == 1) {
            RecordScreen(deviceName = Build.MODEL)
        } else if (tab == 2) {
            ToolsScreen(deviceName = Build.MODEL)
        } else if (tab == 3) {
            AppsScreen(deviceName = Build.MODEL)
        } else if (tab == 4) {
            SettingsScreen(deviceName = Build.MODEL)
        } else {
            PlaceholderScreen(
                title = "工具",
                deviceName = Build.MODEL
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(126.dp)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.45f to Color(0xFFF2F2F7).copy(alpha = 0.94f),
                            1f to Color(0xFFF2F2F7)
                        )
                    )
                )
        )

        BottomBar(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        )
    }
}

@Composable
internal fun TopBar(deviceName: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFF9A52), Color(0xFFFF5A00)))),
            contentAlignment = Alignment.Center
        ) {
            Text("颖", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(8.dp))
        Text("颖控", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink, letterSpacing = 1.sp)
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.7f))
                .padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(4.dp).clip(CircleShape).background(Green))
            Spacer(Modifier.width(5.dp))
            Text(deviceName, fontSize = 9.5.sp, color = Color(0xFF6E6E73))
        }
        Spacer(Modifier.width(7.dp))
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "设置",
                tint = Color(0xFF3A3A3C),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
internal fun LivePill(text: String) {
    val transition = rememberInfiniteTransition(label = "live")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dot"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(OrangeSoft)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(4.dp).alpha(dotAlpha).clip(CircleShape).background(Orange))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 8.5.sp, color = Orange, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun StatusLine(rootOk: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(4.dp).clip(CircleShape).background(if (rootOk) Green else Orange))
        Spacer(Modifier.width(5.dp))
        Text(if (rootOk) "权限已连接" else "等待权限", fontSize = 8.5.sp, color = Tertiary)
        Text("  ·  ", fontSize = 8.5.sp, color = Tertiary)
        Text(if (rootOk) "实时数据" else "暂无数据", fontSize = 8.5.sp, color = Tertiary)
    }
}

@Composable
private fun HeroCard(
    appLabel: String?,
    fps: Float,
    p99: Float,
    jank: Float,
    refreshHz: Float,
    available: Boolean,
    history: List<Float>
) {
    val hasFps = available && fps > 0f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFFF7F0))))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = appLabel ?: if (available) "未检测到前台应用" else "等待权限",
                fontSize = 10.5.sp,
                color = Color(0xFF6E6E73),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (hasFps) "FPS" else "NO DATA",
                fontSize = 8.5.sp,
                color = if (hasFps) Orange else Tertiary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (hasFps) "%.1f".format(fps) else "—",
                fontSize = 56.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (hasFps) Ink else Tertiary,
                letterSpacing = (-2.2).sp
            )
            Text("FPS", fontSize = 9.sp, color = Muted, modifier = Modifier.padding(start = 5.dp, bottom = 10.dp))
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 8.dp)) {
                Row {
                    MiniStat(if (hasFps) "%.1f".format(p99) else "—", "99% 帧时")
                    Spacer(Modifier.width(14.dp))
                    MiniStat(if (hasFps) "%.1f".format(jank) else "—", "Jank %")
                }
            }
        }
        Sparkline(
            points = history,
            color = Orange,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            fill = true
        )
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1A34C759))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    if (refreshHz > 0f) "%.0fHz 面板".format(refreshHz) else "刷新率 —",
                    fontSize = 8.5.sp,
                    color = Color(0xFF1B8F3A)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("采样 1s", fontSize = 8.5.sp, color = Muted)
            Spacer(Modifier.weight(1f))
            Text(
                text = if (history.isNotEmpty()) {
                    "平均 %.1f · 最高 %.1f".format(history.average(), history.max())
                } else "平均 — · 最高 —",
                fontSize = 9.sp,
                color = if (history.isNotEmpty()) Orange else Tertiary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.End) {
        Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(label, fontSize = 7.5.sp, color = Muted)
    }
}

@Composable
private fun MetricGrid(
    snap: Snapshot?,
    cpuHistory: List<Float>,
    gpuHistory: List<Float>,
    ramHistory: List<Float>,
    tempHistory: List<Float>,
    powerHistory: List<Float>,
    battHistory: List<Float>
) {
    val available = snap != null
    val cpu = snap?.cpu ?: 0f
    val gpu = snap?.gpu ?: 0f
    val gpuFreq = snap?.gpuFreq ?: 0f
    val ram = snap?.ramUsedGb ?: 0f
    val ramTotal = snap?.ramTotalGb ?: 0f
    val temp = snap?.thermal ?: 0f
    val batteryTemp = snap?.batteryTemp ?: 0f
    val voltage = snap?.voltage ?: 0f
    val currentMa = snap?.currentMa ?: 0f
    val power = snap?.power ?: 0f
    val batt = snap?.batt ?: 0f
    val coreCount = snap?.coreLoads?.size ?: 0
    val maxGhz = (snap?.coreMaxMhz?.maxOrNull() ?: 0f) / 1000f

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile(
                "CPU", "%.0f".format(cpu), "%", if (available) "$coreCount 核" else "—",
                if (available) "最高 %.1fGHz".format(maxGhz) else "等待权限",
                Purple, cpuHistory, available, Modifier.weight(1f)
            )
            MetricTile(
                "GPU", "%.0f".format(gpu), "%", "GPU",
                if (available) "%.0f MHz".format(gpuFreq) else "等待权限",
                Blue, gpuHistory, available, Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile(
                "内存", "%.1f".format(ram), "G", if (available) "%.0fG".format(ramTotal) else "—",
                if (available) "可用 %.1fG".format((ramTotal - ram).coerceAtLeast(0f)) else "等待权限",
                Teal, ramHistory, available, Modifier.weight(1f)
            )
            MetricTile(
                "温度", "%.1f".format(temp), "°C", if (available) "电池 %.1f".format(batteryTemp) else "—",
                if (available) "热点温度" else "等待权限",
                Orange, tempHistory, available, Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile(
                "功耗", "%.2f".format(power), "W", if (available) (if (currentMa > 50f) "充电中" else "放电中") else "—",
                if (available) "%.2fV · %.0fmA".format(voltage, currentMa) else "等待权限",
                Amber, powerHistory, available, Modifier.weight(1f)
            )
            MetricTile(
                "电池", "%.0f".format(batt), "%", if (available) (if (currentMa > 50f) "充电" else "放电") else "—",
                if (available) "%.2fV · %.0fmA".format(voltage, currentMa) else "等待权限",
                Green, battHistory, available, Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricTile(
    label: String, value: String, unit: String, tag: String, sub: String,
    color: Color, data: List<Float>, available: Boolean, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(78.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 8.5.sp, color = Muted)
            Spacer(Modifier.weight(1f))
            Text(tag, fontSize = 7.sp, color = Tertiary)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (available) value else "—",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (available) Ink else Tertiary,
                letterSpacing = (-0.5).sp
            )
            if (available) {
                Text(unit, fontSize = 8.5.sp, color = Muted, modifier = Modifier.padding(start = 1.dp, bottom = 2.dp))
            }
        }
        Text(sub, fontSize = 7.sp, color = Tertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        Sparkline(points = data, color = color, modifier = Modifier.fillMaxWidth().height(14.dp))
    }
}

@Composable
private fun CoresCard(snap: Snapshot?) {
    val loads = snap?.coreLoads ?: List(8) { 0f }
    val labels = snap?.coreMaxMhz?.map { "%.1fG".format(it / 1000f) } ?: List(8) { "—" }
    val hint = if (snap != null && snap.coreMaxMhz.isNotEmpty()) {
        "%d 核 · 最高 %.1fGHz".format(loads.size, (snap.coreMaxMhz.maxOrNull() ?: 0f) / 1000f)
    } else "无数据"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("CPU 核心", fontSize = 10.5.sp, color = Muted)
            Spacer(Modifier.weight(1f))
            Text(hint, fontSize = 8.sp, color = Tertiary)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            loads.forEachIndexed { i, load ->
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$i", fontSize = 6.5.sp, color = Tertiary)
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Hairline)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .fillMaxHeight((load / 100f).coerceIn(0.02f, 1f))
                                .background(Brush.verticalGradient(listOf(Color(0xFFFFB27A), Orange)))
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(labels.getOrElse(i) { "—" }, fontSize = 6.5.sp, color = Muted)
                }
            }
        }
    }
}

private fun formatMem(memMb: Float): String =
    if (memMb >= 1024f) "%.1fG".format(memMb / 1024f) else "%.0fM".format(memMb)

@Composable
private fun ProcessCard(processes: List<ProcRow>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Top 进程", fontSize = 10.5.sp, color = Muted)
            Spacer(Modifier.weight(1f))
            Text(if (processes.isEmpty()) "无数据" else "按 CPU 排序", fontSize = 8.sp, color = Tertiary)
        }
        Spacer(Modifier.height(4.dp))
        if (processes.isEmpty()) {
            Text(
                "打开应用或游戏后，这里会显示真实进程",
                fontSize = 8.5.sp,
                color = Tertiary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            processes.take(4).forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.width(86.dp)) {
                        Text(row.name, fontSize = 9.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatMem(row.memMb), fontSize = 7.sp, color = Tertiary)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Hairline)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((row.cpu / 100f).coerceIn(0.02f, 1f))
                                .fillMaxHeight()
                                .background(if (index == 0) Orange else if (row.name.startsWith("system")) Purple else Blue)
                        )
                    }
                    Text(
                        "%.1f%%".format(row.cpu),
                        fontSize = 8.sp,
                        color = Color(0xFF6E6E73),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(42.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineCard(
    snap: Snapshot?,
    fpsHistory: List<Float>,
    tempHistory: List<Float>,
    powerHistory: List<Float>,
    gpuFreqHistory: List<Float>
) {
    val available = snap != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("同一根时间轴", fontSize = 10.5.sp, color = Muted)
            Spacer(Modifier.weight(1f))
            Text("FPS · 温度 · 功耗 · GPU", fontSize = 8.sp, color = Tertiary)
        }
        Spacer(Modifier.height(5.dp))
        TimelineRow("FPS", if (available && (snap?.fps ?: 0f) > 0f) "%.1f".format(snap?.fps) else "—", fpsHistory, Orange)
        TimelineRow("温度", if (available) "%.1f°".format(snap?.thermal) else "—", tempHistory, Color(0xFFFFB27A))
        TimelineRow("功耗", if (available) "%.2fW".format(snap?.power) else "—", powerHistory, Blue)
        TimelineRow("GPU", if (available) "%.0fM".format(snap?.gpuFreq) else "—", gpuFreqHistory, Teal)
    }
}

@Composable
private fun TimelineRow(label: String, value: String, data: List<Float>, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 8.sp, color = Muted, modifier = Modifier.width(26.dp))
        Sparkline(points = data, color = color, modifier = Modifier.weight(1f).height(20.dp))
        Text(value, fontSize = 8.sp, color = Color(0xFF6E6E73), textAlign = TextAlign.End, modifier = Modifier.width(44.dp))
    }
}

@Composable
private fun Sparkline(
    points: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    fill: Boolean = false
) {
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val lo = points.minOrNull() ?: 0f
        val hi = points.maxOrNull() ?: 1f
        val range = (hi - lo).let { if (it < 0.001f) 1f else it }
        val topPad = 2.dp.toPx()
        val bottomPad = 2.dp.toPx()
        val stepX = size.width / (points.size - 1)

        val line = Path()
        val area = Path()
        points.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - bottomPad - ((value - lo) / range) * (size.height - topPad - bottomPad)
            if (index == 0) {
                line.moveTo(x, y)
                area.moveTo(x, y)
            } else {
                line.lineTo(x, y)
                area.lineTo(x, y)
            }
        }
        if (fill) {
            area.lineTo(size.width, size.height)
            area.lineTo(0f, size.height)
            area.close()
            drawPath(
                path = area,
                brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.18f), Color.Transparent))
            )
        }
        drawPath(
            path = line,
            color = color,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val tabs = listOf("控制台" to 0, "记录" to 1, "工具" to 2, "应用" to 3, "设置" to 4)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .shadow(12.dp, RoundedCornerShape(31.dp), clip = false)
            .clip(RoundedCornerShape(31.dp))
            .background(Color(0xFFFBFAFD))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val active = index == selected
                val tint = if (active) Orange else Tertiary
                Column(modifier = Modifier.clickable { onSelect(index) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (tab.second == 4) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(19.dp)
                        )
                    } else {
                        TabIcon(kind = tab.second, tint = tint, iconSize = 19.dp)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(tab.first, fontSize = 8.sp, color = tint)
                }
            }
        }
    }
}

@Composable
private fun TabIcon(kind: Int, tint: Color, iconSize: Dp = 19.dp) {
    Canvas(Modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val stroke = 1.7.dp.toPx()
        when (kind) {
            0 -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.08f, h * 0.18f),
                    size = Size(w * 0.84f, h * 0.54f),
                    cornerRadius = CornerRadius(w * 0.1f),
                    style = Stroke(stroke)
                )
                drawLine(tint, Offset(w * 0.34f, h * 0.86f), Offset(w * 0.66f, h * 0.86f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.72f), Offset(w * 0.5f, h * 0.86f), stroke, StrokeCap.Round)
            }
            1 -> {
                val path = Path()
                path.moveTo(w * 0.08f, h * 0.7f)
                path.lineTo(w * 0.3f, h * 0.44f)
                path.lineTo(w * 0.46f, h * 0.56f)
                path.lineTo(w * 0.72f, h * 0.26f)
                path.lineTo(w * 0.92f, h * 0.4f)
                drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(tint, Offset(w * 0.08f, h * 0.86f), Offset(w * 0.92f, h * 0.86f), stroke * 0.8f, StrokeCap.Round)
            }
            2 -> {
                val ys = listOf(0.28f, 0.5f, 0.72f)
                val knobs = listOf(0.68f, 0.36f, 0.58f)
                ys.forEachIndexed { i, y ->
                    drawLine(tint, Offset(w * 0.1f, h * y), Offset(w * 0.9f, h * y), stroke * 0.85f, StrokeCap.Round)
                    drawCircle(tint, radius = w * 0.075f, center = Offset(w * knobs[i], h * y))
                }
            }
            3 -> {
                val r = w * 0.16f
                val positions = listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.28f to 0.72f, 0.72f to 0.72f)
                positions.forEach { (x, y) ->
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(w * x - r, h * y - r),
                        size = Size(r * 2, r * 2),
                        cornerRadius = CornerRadius(r * 0.45f),
                        style = Stroke(stroke)
                    )
                }
            }
        }
    }
}









