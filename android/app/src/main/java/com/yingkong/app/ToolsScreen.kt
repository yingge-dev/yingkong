package com.yingkong.app

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF1D1D1F)
private val Muted = Color(0xFF86868B)
private val Tertiary = Color(0xFFA9A9AE)
private val Orange = Color(0xFFFF6900)
private val Red = Color(0xFFFF3B30)

@Composable
fun ToolsScreen(deviceName: String) {
    val scope = rememberCoroutineScope()
    var processes by remember { mutableStateOf<List<ProcessItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var sortByRss by remember { mutableStateOf(true) }
    var confirm by remember { mutableStateOf<ProcessItem?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val refresh: () -> Unit = {
        scope.launch {
            loading = true
            processes = withContext(Dispatchers.IO) { ProcessManager.list() }
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }

    val visible = processes
        .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        .let { if (sortByRss) it.sortedByDescending { p -> p.rssKb } else it.sortedBy { p -> p.name } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            TopBar(deviceName)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("进程", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink, letterSpacing = (-0.6).sp)
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
                if (loading) "读取中…" else "${processes.size} 个进程 · 真实数据",
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
                if (query.isEmpty()) {
                    Text("搜索进程名", fontSize = 9.sp, color = Tertiary)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Ink, fontSize = 10.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "按内存",
                    fontSize = 8.5.sp,
                    color = if (sortByRss) Orange else Tertiary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { sortByRss = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    "按名称",
                    fontSize = 8.5.sp,
                    color = if (!sortByRss) Orange else Tertiary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { sortByRss = false }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(Modifier.weight(1f))
                message?.let {
                    Text(it, fontSize = 8.sp, color = Muted, maxLines = 1)
                }
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
            items(visible, key = { it.pid }) { item ->
                ProcessRow(item) { confirm = item }
            }
        }
    }

    confirm?.let { item ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("结束进程") },
            text = {
                Text("确定结束 ${item.name}（PID ${item.pid}）？应用可能会自动重启。系统进程不会被执行。")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { ProcessManager.kill(item) }
                        message = result
                        refresh()
                    }
                }) { Text("结束") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ProcessRow(item: ProcessItem, onKill: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "PID ${item.pid} · UID ${item.uid} · ${"%.0f".format(item.rssMb)}M",
                fontSize = 7.5.sp,
                color = Tertiary
            )
        }
        when {
            item.isKernelThread -> Text("内核", fontSize = 7.5.sp, color = Tertiary)
            !item.isKillable -> Text("系统", fontSize = 7.5.sp, color = Tertiary)
            else -> Text(
                "结束",
                fontSize = 8.5.sp,
                color = Red,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onKill() }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
    }
}


