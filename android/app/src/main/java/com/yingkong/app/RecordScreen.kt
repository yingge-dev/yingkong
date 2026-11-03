package com.yingkong.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.max

private val Ink = Color(0xFF1D1D1F)
private val Muted = Color(0xFF86868B)
private val Tertiary = Color(0xFFA9A9AE)
private val Orange = Color(0xFFFF6900)
private val Green = Color(0xFF34C759)
private val Hairline = Color(0x14606067)

@Composable
fun RecordScreen(deviceName: String) {
    val context = LocalContext.current
    var detail by remember { mutableStateOf<RecSession?>(null) }
    BackHandler(enabled = detail != null) { detail = null }
    if (detail != null) {
        SessionDetail(detail!!) { detail = null }
        return
    }
    val recording = RecorderStore.recording.value
    val targetName = RecorderStore.targetName.value
    val targetPkg = RecorderStore.targetPkg.value
    val samples = RecorderStore.samples
    val sessions = RecorderStore.sessions

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(recording) {
        while (recording) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    var overlayOn by remember {
        mutableStateOf(
            context.getSharedPreferences(RecorderService.PREFS, android.content.Context.MODE_PRIVATE)
                .getBoolean(RecorderService.KEY_OVERLAY, false)
        )
    }
    val overlayPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = Settings.canDrawOverlays(context)
        overlayOn = granted
        if (granted) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, RecorderService::class.java).setAction(RecorderService.ACTION_SHOW_OVERLAY)
            )
        }
    }
    val setOverlay: (Boolean) -> Unit = { enabled ->
        if (enabled) {
            if (!Settings.canDrawOverlays(context)) {
                overlayPermission.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            } else {
                overlayOn = true
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, RecorderService::class.java).setAction(RecorderService.ACTION_SHOW_OVERLAY)
                )
            }
        } else {
            overlayOn = false
            context.startService(
                Intent(context, RecorderService::class.java).setAction(RecorderService.ACTION_HIDE_OVERLAY)
            )
        }
    }

    val startRecording = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        ContextCompat.startForegroundService(
            context,
            Intent(context, RecorderService::class.java).setAction(RecorderService.ACTION_START)
        )
    }

    val stopRecording = {
        context.startService(
            Intent(context, RecorderService::class.java).setAction(RecorderService.ACTION_STOP)
        )
    }

    val duration = if (recording && RecorderStore.startedAt > 0) {
        formatDuration(now - RecorderStore.startedAt)
    } else "—"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 2.dp, bottom = 132.dp)
    ) {
        TopBar(deviceName)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("记录", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink, letterSpacing = (-0.6).sp)
            Spacer(Modifier.width(8.dp))
            LivePill(if (recording) "记录中" else "待机")
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "前台服务记录 · 通知栏可停止 · 不注入游戏",
            fontSize = 8.5.sp,
            color = Tertiary
        )
        Spacer(Modifier.height(13.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFFF7F0))))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(6.dp).clip(CircleShape)
                        .background(if (recording) Green else Tertiary)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    if (recording) "正在记录" else "未开始记录",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
                Spacer(Modifier.weight(1f))
                Text(duration, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = if (recording) Orange else Tertiary)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    recording && targetName != null -> targetName
                    recording -> "等待前台应用…"
                    else -> "点开始后切到游戏，通知栏停止"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (recording) Ink else Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                targetPkg ?: "只读系统帧数据，不改游戏",
                fontSize = 8.5.sp,
                color = Tertiary
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (recording) Ink else Orange)
                    .clickable { if (recording) stopRecording() else startRecording() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (recording) "结束记录" else "开始记录",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("悬浮按钮", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(
                    "在游戏上显示；点按开始/结束，可拖动",
                    fontSize = 8.sp,
                    color = Tertiary
                )
            }
            Switch(
                checked = overlayOn,
                onCheckedChange = setOverlay,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Orange,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0x33606067)
                )
            )
        }
        if (overlayOn) {
            Spacer(Modifier.height(5.dp))
            Text(
                "系统窗口，不注入游戏；部分游戏可能检测到悬浮窗。",
                fontSize = 7.5.sp,
                color = Tertiary
            )
        }

        if (recording) {
            Spacer(Modifier.height(10.dp))
            val last = samples.lastOrNull()
            val avg = if (samples.isNotEmpty()) samples.map { it.fps }.average() else 0.0
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text("本次记录", fontSize = 10.sp, color = Muted)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiveStat("样本", "${samples.size}", "个", Modifier.weight(1f))
                    LiveStat("当前", last?.let { "%.1f".format(it.fps) } ?: "—", "fps", Modifier.weight(1f))
                    LiveStat("平均", if (samples.isNotEmpty()) "%.1f".format(avg) else "—", "fps", Modifier.weight(1f))
                    LiveStat("Jank", last?.let { "%.1f".format(it.jank) } ?: "—", "%", Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("历史记录", fontSize = 10.sp, color = Muted, letterSpacing = 0.5.sp)
            Spacer(Modifier.weight(1f))
            Text("${sessions.size} 场", fontSize = 8.5.sp, color = Tertiary)
        }
        Spacer(Modifier.height(7.dp))

        if (sessions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = 14.dp, vertical = 18.dp)
            ) {
                Text("还没有记录", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.height(3.dp))
                Text("开始记录 → 切到游戏 → 通知栏停止，这里会出现真实场次。", fontSize = 8.5.sp, color = Tertiary)
            }
        } else {
            sessions.take(12).forEach { session ->
                SessionCard(session) { detail = session }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun LiveStat(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 7.5.sp, color = Tertiary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(" $unit", fontSize = 7.5.sp, color = Muted)
        }
    }
}

@Composable
private fun SessionCard(session: RecSession, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                session.appName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(session.start)),
                fontSize = 8.sp,
                color = Tertiary
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            "${formatDuration(session.durationMs)} · ${session.samples} 个样本 · ${session.pkg}",
            fontSize = 8.sp,
            color = Tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SessionMetric("平均", "%.1f".format(session.fpsAvg), "fps", Modifier.weight(1f))
            SessionMetric("1% Low", "%.1f".format(session.fpsLow), "fps", Modifier.weight(1f))
            SessionMetric("Jank", "%.1f".format(session.jankAvg), "%", Modifier.weight(1f))
            SessionMetric("功耗", "%.2f".format(session.powerAvg), "W", Modifier.weight(1f))
            SessionMetric("温度", "%.1f".format(session.tempMax), "°C", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SessionMetric(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 7.sp, color = Tertiary)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(unit, fontSize = 7.sp, color = Muted)
    }
}

@Composable
fun PlaceholderScreen(title: String, deviceName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        TopBar(deviceName)
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink, letterSpacing = (-0.6).sp)
        Spacer(Modifier.height(6.dp))
        Text("这个页面下一步接入。数据没接完之前不显示假数据。", fontSize = 9.sp, color = Tertiary)
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Text("开发中", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(3.dp))
            Text("先做记录页，再回来补这个 Tab。", fontSize = 8.5.sp, color = Tertiary)
        }
    }
}

@Composable
private fun SessionDetail(session: RecSession, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 2.dp, bottom = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("‹", fontSize = 24.sp, color = Ink)
            }
            Spacer(Modifier.width(9.dp))
            Text("场次详情", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.weight(1f))
            Text(formatDuration(session.durationMs), fontSize = 9.sp, color = Tertiary)
        }
        Spacer(Modifier.height(16.dp))
        Text(session.appName, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink, letterSpacing = (-0.5).sp)
        Spacer(Modifier.height(3.dp))
        Text(
            "${session.pkg} · ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(session.start))} · ${session.samples} 个样本",
            fontSize = 8.5.sp,
            color = Tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(13.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .padding(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailStat("平均", "%.1f".format(session.fpsAvg), "fps", Modifier.weight(1f))
                DetailStat("1% Low", "%.1f".format(session.fpsLow), "fps", Modifier.weight(1f))
                DetailStat("Jank", "%.1f".format(session.jankAvg), "%", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailStat("最低", "%.1f".format(session.fpsMin), "fps", Modifier.weight(1f))
                DetailStat("最高", "%.1f".format(session.fpsMax), "fps", Modifier.weight(1f))
                DetailStat("功耗", "%.2f".format(session.powerAvg), "W", Modifier.weight(1f))
                DetailStat("温度", "%.1f".format(session.tempMax), "°C", Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("帧率曲线", fontSize = 10.5.sp, color = Muted)
                Spacer(Modifier.weight(1f))
                Text("逐秒样本", fontSize = 8.sp, color = Tertiary)
            }
            Spacer(Modifier.height(8.dp))
            if (session.points.size < 2) {
                Text(
                    "这一场没有保存逐秒样本。新记录会保存，最多 1200 个点。",
                    fontSize = 8.5.sp,
                    color = Tertiary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                FpsChart(points = session.points, modifier = Modifier.fillMaxWidth().height(170.dp))
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("0s", fontSize = 7.5.sp, color = Tertiary)
                    Spacer(Modifier.weight(1f))
                    Text("浅色 = 前 3 秒预热", fontSize = 7.5.sp, color = Tertiary)
                    Spacer(Modifier.weight(1f))
                    Text(formatDuration(session.durationMs), fontSize = 7.5.sp, color = Tertiary)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(12.dp)
            ) {
                Text("温度", fontSize = 8.sp, color = Tertiary)
                Text("%.1f°C".format(session.tempMax), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                if (session.points.size >= 2) {
                    MiniLine(
                        values = session.points.map { it.temp },
                        color = Color(0xFFFF6900),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(12.dp)
            ) {
                Text("功耗", fontSize = 8.sp, color = Tertiary)
                Text("%.2fW".format(session.powerAvg), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                if (session.points.size >= 2) {
                    MiniLine(
                        values = session.points.map { it.power },
                        color = Color(0xFF0A84FF),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Text("A/B 对比 · 下一版", fontSize = 10.sp, color = Tertiary)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "预热 3 秒不计入汇总；原始样本保留在曲线里。所有数值来自真机采样。",
            fontSize = 8.sp,
            color = Tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DetailStat(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 7.5.sp, color = Tertiary)
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(unit, fontSize = 7.sp, color = Muted)
    }
}

@Composable
private fun FpsChart(points: List<RecSample>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val t0 = points.first().t
        val span = (points.last().t - t0).coerceAtLeast(1L)
        val maxFps = max(60f, points.maxOf { it.fps })
        val w = size.width
        val h = size.height
        val grid = Color(0x14606067)
        for (i in 0..3) {
            val y = h * i / 3f
            drawLine(grid, Offset(0f, y), Offset(w, y), 1f)
        }
        val warmX = (((t0 + 3000L) - t0).toFloat() / span * w).coerceIn(0f, w)
        drawRect(Color(0x12FF6900), topLeft = Offset.Zero, size = Size(warmX, h))

        val line = Path()
        points.forEachIndexed { index, p ->
            val x = ((p.t - t0).toFloat() / span * w).coerceIn(0f, w)
            val y = h - (p.fps / maxFps).coerceIn(0f, 1f) * h
            if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        val area = Path().apply {
            addPath(line)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(Color(0x33FF6900), Color.Transparent)))
        drawPath(
            line,
            Color(0xFFFF6900),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        points.forEach { p ->
            if (p.jank > 5f) {
                val x = ((p.t - t0).toFloat() / span * w).coerceIn(0f, w)
                val y = h - (p.fps / maxFps).coerceIn(0f, 1f) * h
                drawCircle(Color(0xFFFF3B30), radius = 2.2.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}

@Composable
private fun MiniLine(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val lo = values.minOrNull() ?: 0f
        val hi = values.maxOrNull() ?: 1f
        val range = (hi - lo).takeIf { it > 0.01f } ?: 1f
        val line = Path()
        values.forEachIndexed { index, value ->
            val x = size.width * index / (values.size - 1f)
            val y = size.height - (value - lo) / range * (size.height - 2.dp.toPx()) - 1.dp.toPx()
            if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        drawPath(
            line,
            color,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(total / 60, total % 60)
}




