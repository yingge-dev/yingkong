package com.yingkong.app

import android.util.Log
import java.io.File
import kotlin.math.abs

data class ProcRow(val name: String, val cpu: Float, val memMb: Float)

data class Snapshot(
    val cpu: Float,
    val coreLoads: List<Float>,
    val coreMaxMhz: List<Float>,
    val gpu: Float,
    val gpuFreq: Float,
    val ramUsedGb: Float,
    val ramTotalGb: Float,
    val temp: Float,
    val power: Float,
    val batt: Float,
    val processes: List<ProcRow>
)

object RootShell {
    private val script = """
        echo ===STAT
        cat /proc/stat
        echo ===FREQ
        cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq
        echo ===MAX
        cat /sys/devices/system/cpu/cpu*/cpufreq/cpuinfo_max_freq
        echo ===GPU
        cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage
        cat /sys/class/kgsl/kgsl-3d0/clock_mhz
        echo ===BATT
        cat /sys/class/power_supply/battery/voltage_now
        cat /sys/class/power_supply/battery/current_now
        cat /sys/class/power_supply/battery/temp
        cat /sys/class/power_supply/battery/capacity
        echo ===MEM
        cat /proc/meminfo
        echo ===PS
        ps -A -o PID,RSS,CPU,NAME
    """.trimIndent()

    private val prevTotal = HashMap<String, Long>()
    private val prevIdle = HashMap<String, Long>()

    fun snapshot(): Snapshot? {
        val raw = exec() ?: return null
        if (!raw.contains("===STAT")) return null

        val sections = HashMap<String, MutableList<String>>()
        var current = ""
        raw.lineSequence().forEach { line ->
            if (line.startsWith("===")) {
                current = line.removePrefix("===")
                sections[current] = mutableListOf()
            } else if (current.isNotEmpty()) {
                sections[current]?.add(line)
            }
        }

        // CPU
        val totals = HashMap<String, Long>()
        val idles = HashMap<String, Long>()
        sections["STAT"].orEmpty().forEach { line ->
            if (!line.startsWith("cpu")) return@forEach
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 5) return@forEach
            val nums = parts.drop(1).mapNotNull { it.toLongOrNull() }
            if (nums.size < 4) return@forEach
            totals[parts[0]] = nums.take(7).sum()
            idles[parts[0]] = nums[3] + (nums.getOrNull(4) ?: 0L)
        }
        val cpu = loadOf("cpu", totals["cpu"], idles["cpu"])
        val cores = (0 until 8).map { i ->
            loadOf("cpu$i", totals["cpu$i"], idles["cpu$i"])
        }

        // Frequencies
        val maxMhz = nums(sections["MAX"]).map { it / 1000f }

        // GPU
        val gpuLines = sections["GPU"].orEmpty()
        val gpu = Regex("\\d+").find(gpuLines.getOrNull(0).orEmpty())?.value?.toFloatOrNull() ?: 0f
        val gpuFreq = gpuLines.getOrNull(1)?.trim()?.toFloatOrNull() ?: 0f

        // Battery
        val battValues = nums(sections["BATT"])
        val voltage = (battValues.getOrNull(0) ?: 0L) / 1_000_000f
        val currentA = (battValues.getOrNull(1) ?: 0L) / 1_000_000f
        val tempRaw = battValues.getOrNull(2) ?: 0L
        val temp = when {
            tempRaw > 1000 -> tempRaw / 1000f
            tempRaw > 200 -> tempRaw / 10f
            else -> tempRaw.toFloat()
        }
        val batt = (battValues.getOrNull(3) ?: 0L).toFloat()
        val power = abs(voltage * currentA)

        // Memory
        var totalKb = 0L
        var availKb = 0L
        sections["MEM"].orEmpty().forEach { line ->
            when {
                line.startsWith("MemTotal:") -> totalKb = firstLong(line) ?: 0L
                line.startsWith("MemAvailable:") -> availKb = firstLong(line) ?: 0L
            }
        }
        val ramTotal = totalKb / 1_048_576f
        val ramUsed = (totalKb - availKb).coerceAtLeast(0L) / 1_048_576f

        // Processes
        val processes = sections["PS"].orEmpty()
            .drop(1)
            .mapNotNull { line ->
                val parts = line.trim().split(Regex("\\s+"), limit = 4)
                if (parts.size < 4) return@mapNotNull null
                val pid = parts[0].toIntOrNull() ?: return@mapNotNull null
                val rss = parts[1].toLongOrNull() ?: 0L
                val cpuPct = parts[2].toFloatOrNull() ?: 0f
                val name = parts[3]
                if (pid <= 0 || name.startsWith("[")) null
                else ProcRow(name, cpuPct, rss / 1024f)
            }
            .sortedByDescending { it.cpu }
            .take(6)

        return Snapshot(
            cpu = cpu,
            coreLoads = cores,
            coreMaxMhz = maxMhz,
            gpu = gpu,
            gpuFreq = gpuFreq,
            ramUsedGb = ramUsed,
            ramTotalGb = ramTotal,
            temp = temp,
            power = power,
            batt = batt,
            processes = processes
        )
    }

    private fun loadOf(name: String, total: Long?, idle: Long?): Float {
        if (total == null || idle == null) return 0f
        val pt = prevTotal[name]
        val pi = prevIdle[name]
        prevTotal[name] = total
        prevIdle[name] = idle
        if (pt == null || pi == null) return 0f
        val dt = total - pt
        val di = idle - pi
        if (dt <= 0L) return 0f
        return ((dt - di).toFloat() * 100f / dt).coerceIn(0f, 100f)
    }

    private fun nums(lines: List<String>?): List<Long> =
        lines.orEmpty().mapNotNull { it.trim().toLongOrNull() }

    private fun firstLong(line: String): Long? =
        Regex("\\d+").find(line)?.value?.toLongOrNull()

    private fun exec(): String? {
        return try {
            val su = if (File("/system/bin/su").exists()) "/system/bin/su" else "su"
            val pb = ProcessBuilder(su, "-c", script)
            pb.redirectErrorStream(true)
            val process = pb.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            Log.d("RootShell", "exit=${process.exitValue()} out=${output.take(200)}")
            output
        } catch (e: Exception) {
            Log.e("RootShell", "exec failed", e)
            null
        }
    }
}

