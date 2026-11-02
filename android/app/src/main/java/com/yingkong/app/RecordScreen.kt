package com.yingkong.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

private val Ink = Color(0xFF1D1D1F)
private val Muted = Color(0xFF86868B)
private val Tertiary = Color(0xFFA9A9AE)
private val Orange = Color(0xFFFF6900)
private val Green = Color(0xFF34C759)
private val Hairline = Color(0x14606067)

@Composable
fun RecordScreen(deviceName: String) {
    val context = LocalContext.current
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
                SessionCard(session)
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
private fun SessionCard(session: RecSession) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.92f))
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

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(total / 60, total % 60)
}

