package com.yingkong.app

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF1D1D1F)
private val Muted = Color(0xFF86868B)
private val Tertiary = Color(0xFFA9A9AE)
private val Orange = Color(0xFFFF6900)
private val Red = Color(0xFFFF3B30)
private val Purple = Color(0xFF7C5CFF)

private data class AppEntry(
    val packageName: String,
    val label: String,
    val uid: Int,
    val enabled: Boolean
)

private fun loadApps(context: android.content.Context): List<AppEntry> {
    val manager = context.packageManager
    return manager.getInstalledApplications(0)
        .asSequence()
        .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
        .filter { it.packageName != context.packageName }
        .map {
            AppEntry(
                packageName = it.packageName,
                label = it.loadLabel(manager).toString(),
                uid = it.uid,
                enabled = it.enabled
            )
        }
        .sortedBy { it.label.lowercase() }
        .toList()
}

@Composable
fun AppsScreen(deviceName: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<AppEntry?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val refresh: () -> Unit = {
        scope.launch {
            loading = true
            apps = withContext(Dispatchers.IO) { loadApps(context) }
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }

    val runAction: (AppEntry, String) -> Unit = { app, command ->
        selected = null
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { RootShell.runShell(command, 15) }
            message = result?.trim()?.takeIf { it.isNotEmpty() } ?: "已执行"
            busy = false
            apps = withContext(Dispatchers.IO) { loadApps(context) }
        }
    }

    val visible = apps.filter {
        query.isBlank() || it.label.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            TopBar(deviceName)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("应用", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink, letterSpacing = (-0.6).sp)
                Spacer(Modifier.weight(1f))
                Text(
                    "刷新",
                    fontSize = 9.sp,
                    color = Orange,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { refresh() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                if (loading) "读取中…" else "${apps.size} 个用户应用 · 真实数据",
                fontSize = 8.5.sp,
                color = Tertiary
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) Text("搜索应用名或包名", fontSize = 9.sp, color = Tertiary)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Ink, fontSize = 10.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            message?.let {
                Spacer(Modifier.height(5.dp))
                Text(it, fontSize = 8.sp, color = Muted, maxLines = 1)
            }
            Spacer(Modifier.height(6.dp))
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 126.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(visible, key = { it.packageName }) { app ->
                AppRow(app) { selected = app }
            }
        }
    }

    selected?.let { app ->
        AlertDialog(
            onDismissRequest = { if (!busy) selected = null },
            title = { Text(app.label) },
            text = {
                Column {
                    Text(app.packageName, fontSize = 9.sp, color = Tertiary)
                    Spacer(Modifier.height(8.dp))
                    ActionRow("强制停止", "am force-stop") {
                        runAction(app, "am force-stop ${app.packageName}")
                    }
                    ActionRow("冻结（suspend）", "可恢复") {
                        runAction(app, "pm suspend --user 0 ${app.packageName}")
                    }
                    ActionRow("解冻（unsuspend）", "恢复运行") {
                        runAction(app, "pm unsuspend --user 0 ${app.packageName}")
                    }
                    ActionRow("待机（restricted）", "限制后台") {
                        runAction(app, "am set-standby-bucket --user 0 ${app.packageName} restricted")
                    }
                    ActionRow("设为活跃", "取消待机") {
                        runAction(app, "am set-standby-bucket --user 0 ${app.packageName} active")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selected = null }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun AppRow(app: AppEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (app.enabled) Purple.copy(alpha = 0.12f) else Color(0x14606067)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                app.label.firstOrNull()?.uppercase() ?: "?",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (app.enabled) Purple else Tertiary
            )
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                app.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (app.enabled) Ink else Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${app.packageName} · UID ${app.uid}",
                fontSize = 7.5.sp,
                color = Tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(if (app.enabled) "管理" else "已停用", fontSize = 8.sp, color = if (app.enabled) Orange else Tertiary)
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 10.5.sp, color = Ink)
            Text(subtitle, fontSize = 7.5.sp, color = Tertiary)
        }
        Text("›", fontSize = 16.sp, color = Tertiary)
    }
}
