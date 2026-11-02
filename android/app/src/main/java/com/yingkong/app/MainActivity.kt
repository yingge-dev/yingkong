package com.yingkong.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val Ink = Color(0xFF1D1D1F)
private val Muted = Color(0xFF86868B)
private val Tertiary = Color(0xFFA9A9AE)
private val Orange = Color(0xFFFF6900)
private val OrangeSoft = Color(0x1AFF6900)
private val Green = Color(0xFF34C759)
private val GreenInk = Color(0xFF1B8F3A)
private val Blue = Color(0xFF0A84FF)
private val Purple = Color(0xFFAF52DE)
private val Teal = Color(0xFF30B0C7)
private val Amber = Color(0xFFFFB340)
private val Hairline = Color(0x14606067)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { YingKongApp() }
    }
}

@Composable
private fun YingKongApp() {
    var fps by remember { mutableFloatStateOf(118.6f) }
    var low by remember { mutableFloatStateOf(96.2f) }
    var jank by remember { mutableFloatStateOf(1.8f) }
    var temp by remember { mutableFloatStateOf(38.6f) }
    var power by remember { mutableFloatStateOf(5.82f) }
    var cpu by remember { mutableFloatStateOf(42f) }
    var gpu by remember { mutableFloatStateOf(68f) }
    var ram by remember { mutableFloatStateOf(9.2f) }
    var batt by remember { mutableFloatStateOf(82f) }
    var gpuFreq by remember { mutableFloatStateOf(680f) }

    val fpsHistory = remember { mutableStateListOf<Float>().apply { repeat(48) { add(116f + Random.nextFloat() * 4f) } } }
    val tempHistory = remember { mutableStateListOf<Float>().apply { repeat(24) { add(38f + Random.nextFloat() * 0.6f) } } }
    val powerHistory = remember { mutableStateListOf<Float>().apply { repeat(24) { add(5.5f + Random.nextFloat() * 0.4f) } } }
    val cpuHistory = remember { mutableStateListOf<Float>().apply { repeat(24) { add(40f + Random.nextFloat() * 8f) } } }
    val gpuHistory = remember { mutableStateListOf<Float>().apply { repeat(24) { add(62f + Random.nextFloat() * 10f) } } }
    val ramHistory = remember { mutableStateListOf<Float>().apply { repeat(24) { add(9.1f + Random.nextFloat() * 0.2f) } } }
    val battHistory = remember { mutableStateListOf<Float>().apply { repeat(24) { add(82f) } } }
    val gpuFreqHistory = remember { mutableStateListOf<Float>().apply { repeat(24) { add(650f + Random.nextFloat() * 80f) } } }
    val coreLoads = remember { mutableStateListOf(52f, 48f, 44f, 40f, 38f, 32f, 30f, 28f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            fps = (fps + (Random.nextFloat() - 0.5f) * 2.4f).coerceIn(96f, 122f)
            fpsHistory.add(fps); if (fpsHistory.size > 48) fpsHistory.removeAt(0)
            low = ((fpsHistory.minOrNull() ?: 96f) - 2f).coerceAtLeast(88f)
            jank = 1.2f + Random.nextFloat() * 1.2f
            temp = (37.9f + Random.nextFloat() * 0.7f)
            tempHistory.add(temp); if (tempHistory.size > 24) tempHistory.removeAt(0)
            power = 5.55f + Random.nextFloat() * 0.5f
            powerHistory.add(power); if (powerHistory.size > 24) powerHistory.removeAt(0)
            cpu = 38f + Random.nextFloat() * 12f
            gpu = 62f + Random.nextFloat() * 12f
            gpuFreq = 620f + Random.nextFloat() * 120f
            ram = 9.1f + Random.nextFloat() * 0.3f
            batt = (batt + 0.01f).coerceAtMost(99f)
            cpuHistory.add(cpu); if (cpuHistory.size > 24) cpuHistory.removeAt(0)
            gpuHistory.add(gpu); if (gpuHistory.size > 24) gpuHistory.removeAt(0)
            gpuFreqHistory.add(gpuFreq); if (gpuFreqHistory.size > 24) gpuFreqHistory.removeAt(0)
            ramHistory.add(ram); if (ramHistory.size > 24) ramHistory.removeAt(0)
            battHistory.add(batt); if (battHistory.size > 24) battHistory.removeAt(0)
            for (i in coreLoads.indices) {
                coreLoads[i] = (coreLoads[i] + (Random.nextFloat() - 0.5f) * 14f).coerceIn(8f, 96f)
            }
        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 2.dp, bottom = 132.dp)
        ) {
            TopBar()
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("控制台", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink, letterSpacing = (-0.6).sp)
                Spacer(Modifier.width(8.dp))
                LivePill("LIVE")
            }
            Spacer(Modifier.height(4.dp))
            StatusLine()
            Spacer(Modifier.height(13.dp))

            HeroCard(fps = fps, low = low, jank = jank, history = fpsHistory)
            Spacer(Modifier.height(10.dp))

            MetricGrid(
                cpu = cpu, gpu = gpu, gpuFreq = gpuFreq, ram = ram, temp = temp, power = power, batt = batt,
                cpuHistory = cpuHistory, gpuHistory = gpuHistory, ramHistory = ramHistory,
                tempHistory = tempHistory, powerHistory = powerHistory, battHistory = battHistory
            )
            Spacer(Modifier.height(10.dp))

            ProcessCard(cpu = cpu)
            Spacer(Modifier.height(10.dp))

            CoresCard(coreLoads = coreLoads)
            Spacer(Modifier.height(10.dp))

            TimelineCard(
                fps = fps, temp = temp, power = power, gpuFreq = gpuFreq,
                fpsHistory = fpsHistory, tempHistory = tempHistory, powerHistory = powerHistory,
                gpuFreqHistory = gpuFreqHistory
            )
            Spacer(Modifier.height(10.dp))

            ActivityStrip()
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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun TopBar() {
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
            Text("K90 至尊版", fontSize = 9.5.sp, color = Color(0xFF6E6E73))
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
private fun LivePill(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(OrangeSoft)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(4.dp).clip(CircleShape).background(Orange))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 8.5.sp, color = Orange, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun StatusLine() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(4.dp).clip(CircleShape).background(Orange))
        Spacer(Modifier.width(5.dp))
        Text("M2 演示版", fontSize = 8.5.sp, color = Tertiary)
        Text("  ·  ", fontSize = 8.5.sp, color = Tertiary)
        Text("模拟数据", fontSize = 8.5.sp, color = Tertiary)
        Text("  ·  ", fontSize = 8.5.sp, color = Tertiary)
        Text("不修改系统", fontSize = 8.5.sp, color = Tertiary)
    }
}

@Composable
private fun HeroCard(fps: Float, low: Float, jank: Float, history: List<Float>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFFF7F0))))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("原神 · 极高画质", fontSize = 10.5.sp, color = Color(0xFF6E6E73), fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text("LIVE", fontSize = 8.5.sp, color = Orange, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "%.1f".format(fps),
                fontSize = 56.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                letterSpacing = (-2.2).sp
            )
            Text("FPS", fontSize = 9.sp, color = Muted, modifier = Modifier.padding(start = 5.dp, bottom = 10.dp))
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 8.dp)) {
                Row {
                    MiniStat("%.1f".format(low), "1% LOW")
                    Spacer(Modifier.width(14.dp))
                    MiniStat("%.1f".format(jank), "抖动 ms")
                }
            }
        }
        Sparkline(
            points = history,
            color = Orange,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            minValue = 102f,
            maxValue = 124f,
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
                Text("165Hz 已核验", fontSize = 8.5.sp, color = GreenInk)
            }
            Spacer(Modifier.width(8.dp))
            Text("本局 18:42", fontSize = 8.5.sp, color = Muted)
            Spacer(Modifier.weight(1f))
            Text("平均 117.4 · 最高 121.8", fontSize = 9.sp, color = Orange, fontWeight = FontWeight.SemiBold)
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
    cpu: Float, gpu: Float, gpuFreq: Float, ram: Float, temp: Float, power: Float, batt: Float,
    cpuHistory: List<Float>, gpuHistory: List<Float>, ramHistory: List<Float>,
    tempHistory: List<Float>, powerHistory: List<Float>, battHistory: List<Float>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("CPU", "%.0f".format(cpu), "%", "8 核", "3.5 GHz · 大核 4.3", Purple, cpuHistory, Modifier.weight(1f))
            MetricTile("GPU", "%.0f".format(gpu), "%", "Adreno 830", "%.0f MHz · 负载 %.0f%%".format(gpuFreq, gpu), Blue, gpuHistory, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("内存", "%.1f".format(ram), "G", "ZRAM 1.2G", "/ 16G · 缓存 3.1G", Teal, ramHistory, Modifier.weight(1f))
            MetricTile("温度", "%.1f".format(temp), "°C", "机身", "CPU 41.2 · 电池 34.1", Orange, tempHistory, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("功耗", "%.2f".format(power), "W", "整机", "电池 4.1 · 屏幕 1.7", Amber, powerHistory, Modifier.weight(1f))
            MetricTile("电池", "%.0f".format(batt), "%", "充电 34W", "12.4V · 2.7A · 28min→80%", Green, battHistory, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricTile(
    label: String, value: String, unit: String, tag: String, sub: String,
    color: Color, data: List<Float>, modifier: Modifier = Modifier
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
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink, letterSpacing = (-0.5).sp)
            Text(unit, fontSize = 8.5.sp, color = Muted, modifier = Modifier.padding(start = 1.dp, bottom = 2.dp))
        }
        Text(sub, fontSize = 7.sp, color = Tertiary, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Sparkline(points = data, color = color, modifier = Modifier.fillMaxWidth().height(14.dp))
    }
}

@Composable
private fun CoresCard(coreLoads: List<Float>) {
    val freqs = listOf("4.3G", "4.3G", "3.5G", "3.5G", "3.5G", "3.5G", "3.5G", "3.5G")
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
            Text("8 核 · 2 + 6 · 最高 4.3GHz", fontSize = 8.sp, color = Tertiary)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            coreLoads.forEachIndexed { i, load ->
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
                                .fillMaxHeight((load / 100f).coerceIn(0.08f, 1f))
                                .background(Brush.verticalGradient(listOf(Color(0xFFFFB27A), Orange)))
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(freqs[i], fontSize = 6.5.sp, color = Muted)
                }
            }
        }
    }
}

@Composable
private fun ProcessCard(cpu: Float) {
    val rows = listOf(
        Triple("原神", cpu, "2.4G"),
        Triple("system_server", 12.3f, "1.8G"),
        Triple("surfaceflinger", 8.1f, "420M"),
        Triple("微信", 4.2f, "980M")
    )
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
            Text("按 CPU 排序", fontSize = 8.sp, color = Tertiary)
        }
        Spacer(Modifier.height(4.dp))
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.width(86.dp)) {
                    Text(row.first, fontSize = 9.sp, color = Ink, maxLines = 1)
                    Text(row.third, fontSize = 7.sp, color = Tertiary)
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
                            .fillMaxWidth((row.second / 100f).coerceIn(0.05f, 1f))
                            .fillMaxHeight()
                            .background(if (index == 0) Orange else if (row.first.startsWith("system")) Purple else Blue)
                    )
                }
                Text(
                    "%.1f%%".format(row.second),
                    fontSize = 8.sp,
                    color = Color(0xFF6E6E73),
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(42.dp)
                )
            }
        }
    }
}

@Composable
private fun TimelineCard(
    fps: Float, temp: Float, power: Float, gpuFreq: Float,
    fpsHistory: List<Float>, tempHistory: List<Float>, powerHistory: List<Float>, gpuFreqHistory: List<Float>
) {
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
            Text("FPS · 温度 · 功耗", fontSize = 8.sp, color = Tertiary)
        }
        Spacer(Modifier.height(5.dp))
        TimelineRow("FPS", "%.1f".format(fps), fpsHistory.takeLast(24), Orange)
        TimelineRow("温度", "%.1f°".format(temp), tempHistory, Color(0xFFFFB27A))
        TimelineRow("功耗", "%.2fW".format(power), powerHistory, Blue)
        TimelineRow("GPU", "%.0fM".format(gpuFreq), gpuFreqHistory, Teal)
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
private fun ActivityStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text("后台活动", fontSize = 10.sp, color = Muted)
        Spacer(Modifier.width(10.dp))
        Text("12", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(" 次唤醒", fontSize = 8.5.sp, color = Muted)
        Spacer(Modifier.weight(1f))
        Text("微信 · 淘宝 · 抖音", fontSize = 8.5.sp, color = Tertiary)
    }
}

@Composable
private fun Sparkline(
    points: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    minValue: Float? = null,
    maxValue: Float? = null,
    fill: Boolean = false
) {
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val lo = minValue ?: (points.minOrNull() ?: 0f)
        val hi = maxValue ?: (points.maxOrNull() ?: 1f)
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
private fun BottomBar(modifier: Modifier = Modifier) {
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
                val active = index == 0
                val tint = if (active) Orange else Tertiary
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            4 -> {
                drawCircle(tint, radius = w * 0.16f, center = Offset(w / 2, h / 2), style = Stroke(stroke))
                for (i in 0 until 8) {
                    val angle = i * PI.toFloat() / 4f
                    val x1 = w / 2 + cos(angle) * w * 0.27f
                    val y1 = h / 2 + sin(angle) * h * 0.27f
                    val x2 = w / 2 + cos(angle) * w * 0.38f
                    val y2 = h / 2 + sin(angle) * h * 0.38f
                    drawLine(tint, Offset(x1, y1), Offset(x2, y2), stroke * 0.9f, StrokeCap.Round)
                }
            }
        }
    }
}

