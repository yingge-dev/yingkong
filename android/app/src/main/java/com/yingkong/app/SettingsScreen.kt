package com.yingkong.app

import android.app.Activity

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF1D1D1F)
private val Muted = Color(0xFF86868B)
private val Tertiary = Color(0xFFA9A9AE)
private val Orange = Color(0xFFFF6900)
private val Green = Color(0xFF34C759)
private val Red = Color(0xFFFF3B30)

@Composable
fun SettingsScreen(deviceName: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(ModuleState()) }
    var checked by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmInstall by remember { mutableStateOf(false) }
    var confirmUninstall by remember { mutableStateOf(false) }
    var shizukuAvailable by remember { mutableStateOf(false) }
    var shizukuGranted by remember { mutableStateOf(false) }
    var shizukuMessage by remember { mutableStateOf<String?>(null) }

    val refresh = {
        scope.launch {
            state = withContext(Dispatchers.IO) { ModuleManager.state() }
            checked = true
        }
    }
    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(Unit) {
        while (true) {
            shizukuAvailable = ShizukuAccess.available()
            shizukuGranted = ShizukuAccess.granted()
            delay(2000)
        }
    }

    val statusText = when {
        state.installed && state.running -> "已安装 · 守护运行中"
        state.installed -> "已安装 · 守护未运行"
        state.pending -> "已写入 · 待重启生效"
        checked -> "未安装"
        else -> "检查中…"
    }
    val statusColor = when {
        state.installed && state.running -> Green
        state.installed || state.pending -> Orange
        else -> Tertiary
    }

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
        Text("设置", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink, letterSpacing = (-0.6).sp)
        Spacer(Modifier.height(4.dp))
        Text("模块、采集与设备", fontSize = 8.5.sp, color = Tertiary)
        Spacer(Modifier.height(13.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.94f))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(7.dp))
                Text("采集模块", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.weight(1f))
                Text(
                    if (state.version != null) "v${state.version}" else "",
                    fontSize = 8.sp,
                    color = Tertiary
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(statusText, fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Medium)
            if (state.running && state.cpu != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "采集：CPU %.0f%% · %.1f°C · 电量 %.0f%%".format(state.cpu, state.temp ?: 0f, state.battery ?: 0f),
                    fontSize = 8.5.sp,
                    color = Muted
                )
                val top = state.threads.firstOrNull()
                if (top != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "最忙线程：${top.name} %.0f%%".format(top.cpu),
                        fontSize = 8.sp,
                        color = Tertiary
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(
                "可选模块：开机启动原生采集守护进程，每秒写入 /data/adb/yingkong 状态与样本；不修改系统分区，可随时卸载。",
                fontSize = 8.5.sp,
                color = Muted,
                lineHeight = 13.sp
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "需要 root；安装后建议重启。不会自动重启手机。",
                fontSize = 8.sp,
                color = Tertiary
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        when {
                            busy -> Color(0xFFD8D8DC)
                            state.installed || state.pending -> Ink
                            else -> Orange
                        }
                    )
                    .clickable(enabled = !busy) {
                        if (state.installed || state.pending) confirmUninstall = true else confirmInstall = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when {
                        busy -> "处理中…"
                        state.installed || state.pending -> "卸载模块"
                        else -> "安装模块"
                    },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (!checked || !state.root) {
                Spacer(Modifier.height(7.dp))
                Text(
                    if (checked) "没有 root 权限，无法安装模块。" else "正在检查 root…",
                    fontSize = 8.sp,
                    color = Red
                )
            }
            message?.let {
                Spacer(Modifier.height(7.dp))
                Text(it, fontSize = 8.sp, color = Muted)
            }
        }

        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(14.dp)
        ) {
            Text("免 root 通道", fontSize = 10.5.sp, color = Muted)
            Spacer(Modifier.height(5.dp))
            Text(
                when {
                    !shizukuAvailable -> "未检测到 Shizuku 服务"
                    shizukuGranted -> "Shizuku 已连接 · 已授权"
                    else -> "Shizuku 已连接 · 未授权"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = when {
                    shizukuGranted -> Green
                    shizukuAvailable -> Orange
                    else -> Tertiary
                }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "没有 root 时，控制台、记录、进程列表会走 Shizuku shell；模块安装仍需 root。",
                fontSize = 8.5.sp,
                color = Tertiary
            )
            Spacer(Modifier.height(8.dp))
            when {
                !shizukuAvailable -> Text("请先安装并启动 Shizuku。", fontSize = 8.sp, color = Tertiary)
                !shizukuGranted -> Text(
                    "授权 Shizuku",
                    fontSize = 10.sp,
                    color = Orange,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { ShizukuAccess.request(context as? Activity) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                else -> Text(
                    "测试 Shizuku",
                    fontSize = 10.sp,
                    color = Orange,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            scope.launch {
                                val result = withContext(Dispatchers.IO) { RootShell.runShizuku("id") }
                                shizukuMessage = result?.trim() ?: "Shizuku 无响应"
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            shizukuMessage?.let {
                Spacer(Modifier.height(3.dp))
                Text(it, fontSize = 8.sp, color = Muted)
            }
        }

        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(14.dp)
        ) {
            Text("模块现在做什么", fontSize = 10.5.sp, color = Muted)
            Spacer(Modifier.height(6.dp))
            Text(
                "目前运行原生守护进程（arm64），每秒采集 CPU/核心负载、内存、温度、电池、频率、GPU，并写入状态与样本文件。",
                fontSize = 8.5.sp,
                color = Tertiary,
                lineHeight = 13.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "FrameTimeline、调频/线程绑核会在下一阶段接入；不会拿未验证的 sysfs 直接写。",
                fontSize = 8.5.sp,
                color = Tertiary,
                lineHeight = 13.sp
            )
        }

    }

    if (confirmInstall) {
        AlertDialog(
            onDismissRequest = { confirmInstall = false },
            title = { Text("安装采集模块") },
            text = {
                Text("模块会写入 /data/adb/modules/yingkong，并在开机时启动守护脚本。它不改系统分区；卸载会删除模块和状态文件。安装后建议重启。")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmInstall = false
                    busy = true
                    message = null
                    scope.launch {
                        val output = withContext(Dispatchers.IO) { ModuleManager.install(context) }
                        state = withContext(Dispatchers.IO) { ModuleManager.state() }
                        message = output.lineSequence().lastOrNull { it.isNotBlank() } ?: "完成"
                        busy = false
                    }
                }) { Text("安装") }
            },
            dismissButton = {
                TextButton(onClick = { confirmInstall = false }) { Text("取消") }
            }
        )
    }

    if (confirmUninstall) {
        AlertDialog(
            onDismissRequest = { confirmUninstall = false },
            title = { Text("卸载采集模块") },
            text = { Text("会停止守护进程，并删除 /data/adb/modules/yingkong 与 /data/adb/yingkong。不会影响系统其他模块。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmUninstall = false
                    busy = true
                    message = null
                    scope.launch {
                        val output = withContext(Dispatchers.IO) { ModuleManager.uninstall() }
                        state = withContext(Dispatchers.IO) { ModuleManager.state() }
                        message = output.lineSequence().lastOrNull { it.isNotBlank() } ?: "完成"
                        busy = false
                    }
                }) { Text("卸载") }
            },
            dismissButton = {
                TextButton(onClick = { confirmUninstall = false }) { Text("取消") }
            }
        )
    }
}





