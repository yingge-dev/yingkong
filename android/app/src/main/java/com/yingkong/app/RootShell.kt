package com.yingkong.app

import java.io.File
import java.util.concurrent.TimeUnit
import org.json.JSONObject
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
    val thermal: Float,
    val batteryTemp: Float,
    val voltage: Float,
    val currentMa: Float,
    val power: Float,
    val batt: Float,
    val processes: List<ProcRow>,
    val fgPackage: String?,
    val fps: Float,
    val jankPct: Float,
    val p99Ms: Float
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
        ps -A -o PID,RSS,NAME
        echo ===FG
        dumpsys activity activities | grep -E 'topResumedActivity|mResumedActivity' | head -1
        echo ===CPUINFO
        dumpsys cpuinfo
        echo ===THERM
        cat /sys/class/thermal/thermal_zone*/type
        echo ===THERMTEMP
        cat /sys/class/thermal/thermal_zone*/temp
        echo ===NATIVE
        cat /data/adb/yingkong/status 2>/dev/null
    """.trimIndent()

    private val prevTotal = HashMap<String, Long>()
    private val prevIdle = HashMap<String, Long>()
    private var sfPrevCounts = HashMap<String, Long>()
    private var sfPrevTime = 0L
    private var sfPkg: String? = null


    fun snapshot(): Snapshot? {
        val raw = execCmd(script) ?: return null
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
        var cpu = loadOf("cpu", totals["cpu"], idles["cpu"])
        var cores = (0 until 8).map { i -> loadOf("cpu$i", totals["cpu$i"], idles["cpu$i"]) }

        // Frequencies
        val maxMhz = nums(sections["MAX"]).map { it / 1000f }

        // GPU
        val gpuLines = sections["GPU"].orEmpty()
        var gpu = Regex("\\d+").find(gpuLines.getOrNull(0).orEmpty())?.value?.toFloatOrNull() ?: 0f
        var gpuFreq = gpuLines.getOrNull(1)?.trim()?.toFloatOrNull() ?: 0f

        // Battery
        val battValues = nums(sections["BATT"])
        var voltage = (battValues.getOrNull(0) ?: 0L) / 1_000_000f
        var currentA = (battValues.getOrNull(1) ?: 0L) / 1_000_000f
        var currentMa = currentA * 1000f
        var batteryTemp = toCelsius(battValues.getOrNull(2) ?: 0L)
        var batt = (battValues.getOrNull(3) ?: 0L).toFloat()
        var power = abs(voltage * currentA)

        // Thermal
        val types = sections["THERM"].orEmpty().map { it.trim() }
        val temps = nums(sections["THERMTEMP"])
        val thermalCandidates = types.mapIndexedNotNull { index, type ->
            val rawTemp = temps.getOrNull(index) ?: return@mapIndexedNotNull null
            val ok = type.contains("cpu", true) || type.contains("cpuss", true) ||
                type.contains("soc", true) || type.contains("gpu", true)
            if (ok) toCelsius(rawTemp) else null
        }
        var thermal = thermalCandidates.maxOrNull() ?: batteryTemp

        // Memory
        var totalKb = 0L
        var availKb = 0L
        sections["MEM"].orEmpty().forEach { line ->
            when {
                line.startsWith("MemTotal:") -> totalKb = firstLong(line) ?: 0L
                line.startsWith("MemAvailable:") -> availKb = firstLong(line) ?: 0L
            }
        }
        var ramTotal = totalKb / 1_048_576f
        var ramUsed = (totalKb - availKb).coerceAtLeast(0L) / 1_048_576f

        val nativeStatus = sections["NATIVE"].orEmpty()
            .firstOrNull { it.trim().startsWith("{") }
            ?.let {
                try {
                    JSONObject(it.trim())
                } catch (_: Exception) {
                    null
                }
            }
        if (nativeStatus != null &&
            abs(System.currentTimeMillis() - nativeStatus.optLong("ts")) < 5000
        ) {
            cpu = nativeStatus.optDouble("cpu", cpu.toDouble()).toFloat()
            nativeStatus.optJSONArray("cores")?.let { array ->
                cores = (0 until array.length()).map { index -> array.optDouble(index, 0.0).toFloat() }
            }
            gpu = nativeStatus.optDouble("gpu", gpu.toDouble()).toFloat()
            gpuFreq = nativeStatus.optDouble("gpu_mhz", gpuFreq.toDouble()).toFloat()
            ramTotal = nativeStatus.optDouble("mem_total_gb", ramTotal.toDouble()).toFloat()
            val nativeTotal = ramTotal
            val nativeAvail = nativeStatus.optDouble("mem_avail_gb", 0.0).toFloat()
            if (nativeTotal > 0f) ramUsed = (nativeTotal - nativeAvail).coerceAtLeast(0f)
            thermal = nativeStatus.optDouble("temp", thermal.toDouble()).toFloat()
            batteryTemp = nativeStatus.optDouble("bat_temp", batteryTemp.toDouble()).toFloat()
            voltage = nativeStatus.optDouble("voltage", voltage.toDouble()).toFloat()
            currentMa = nativeStatus.optDouble("current_ma", currentMa.toDouble()).toFloat()
            currentA = currentMa / 1000f
            power = nativeStatus.optDouble("power", power.toDouble()).toFloat()
            batt = nativeStatus.optDouble("battery", batt.toDouble()).toFloat()
        }

        // RSS map from ps
        val rssByPid = HashMap<Int, Float>()
        sections["PS"].orEmpty().drop(1).forEach { line ->
            val parts = line.trim().split(Regex("\\s+"), limit = 3)
            if (parts.size >= 3) {
                val pid = parts[0].toIntOrNull() ?: return@forEach
                val rss = parts[1].toLongOrNull() ?: 0L
                rssByPid[pid] = rss / 1024f
            }
        }

        // CPU usage from dumpsys cpuinfo
        val cpuProcs = sections["CPUINFO"].orEmpty().mapNotNull { line ->
            val match = Regex("""^\s*([\d.]+)%\s+(\d+)/(\S+)""").find(line) ?: return@mapNotNull null
            val pct = match.groupValues[1].toFloatOrNull() ?: 0f
            val pid = match.groupValues[2].toIntOrNull() ?: 0
            val name = match.groupValues[3].removeSuffix(":")
            val kernelThread = name.matches(Regex(".*:\\d+$")) ||
                name.startsWith("kworker") || name.startsWith("ksoftirqd") ||
                name.startsWith("rcu") || name.startsWith("migration") ||
                name.startsWith("irq/")
            if (kernelThread) null else ProcRow(name, pct, rssByPid[pid] ?: 0f)
        }.sortedByDescending { it.cpu }.take(6)

        val processes = if (cpuProcs.isNotEmpty()) cpuProcs else {
            sections["PS"].orEmpty().drop(1).mapNotNull { line ->
                val parts = line.trim().split(Regex("\\s+"), limit = 3)
                if (parts.size < 3) return@mapNotNull null
                val pid = parts[0].toIntOrNull() ?: return@mapNotNull null
                val rss = parts[1].toLongOrNull() ?: 0L
                val name = parts[2].removeSuffix(":")
                if (pid <= 0 || name.startsWith("[")) null
                else ProcRow(name, 0f, rss / 1024f)
            }.sortedByDescending { it.memMb }.take(6)
        }

        // Foreground package
        val fgLine = sections["FG"].orEmpty().firstOrNull().orEmpty()
        val fgPackage = Regex("""\bu\d+\s+([A-Za-z0-9_.]+)/""").find(fgLine)?.groupValues?.get(1)

        // FPS / jank from gfxinfo framestats
        var fps = 0f
        var jankPct = 0f
        var p99Ms = 0f
        if (fgPackage != null) {
            if (fgPackage != "com.yingkong.app") {
                val surface = surfaceFps(fgPackage)
                if (surface != null) {
                    fps = surface
                    jankPct = 0f
                    p99Ms = 0f
                }
            }
            if (fps < 1f) {
                val result = parseFps(fgPackage)
                if (result != null && result.first >= 1f) {
                    fps = result.first
                    jankPct = result.second
                    p99Ms = result.third
                }
            }
        }

        return Snapshot(
            cpu = cpu,
            coreLoads = cores,
            coreMaxMhz = maxMhz,
            gpu = gpu,
            gpuFreq = gpuFreq,
            ramUsedGb = ramUsed,
            ramTotalGb = ramTotal,
            thermal = thermal,
            batteryTemp = batteryTemp,
            voltage = voltage,
            currentMa = currentMa,
            power = power,
            batt = batt,
            processes = processes,
            fgPackage = fgPackage,
            fps = fps,
            jankPct = jankPct,
            p99Ms = p99Ms
        )
    }

    private fun surfaceFps(pkg: String): Float? {
        val out = execCmd("dumpsys SurfaceFlinger") ?: return null
        if (sfPkg != pkg) {
            sfPrevCounts.clear()
            sfPrevTime = 0L
            sfPkg = pkg
        }
        val now = System.nanoTime()
        val counts = HashMap<String, Long>()
        var layerKey: String? = null
        out.lineSequence().forEach { line ->
            val layer = Regex("""^\s+Layer \[(\d+)\] (.+)$""").find(line)
            if (layer != null) {
                layerKey = layer.groupValues[1] + "|" + layer.groupValues[2].take(120)
                return@forEach
            }
            val key = layerKey ?: return@forEach
            val frame = Regex("""\bframe=(\d+)""").find(line)
            if (frame != null) {
                counts[key] = frame.groupValues[1].toLongOrNull() ?: 0L
                layerKey = null
            }
        }
        val prev = sfPrevCounts
        val dt = (now - sfPrevTime) / 1_000_000_000f
        sfPrevCounts = counts
        sfPrevTime = now
        if (prev.isEmpty() || dt < 0.3f) return null
        var bestSurface = 0L
        var bestPkg = 0L
        var bestVideo = 0L
        counts.forEach { (key, value) ->
            val name = key.substringAfter('|')
            if (name.startsWith("VRI-") || name.contains("Background for") ||
                name.contains("Bounds for") || name.contains("ActivityRecord") ||
                name.contains("InputSink") || name.contains("com.android.systemui") ||
                name.contains("StatusBar") || name.contains("NavigationBar") ||
                name.contains("ScreenDecor") || name.contains("Wallpaper") ||
                name.contains("Launcher") || name.contains("MultiTasking") ||
                name.contains("NotificationShade") || name.contains("VolumeDialog") ||
                name.contains("InputMethod")
            ) {
                return@forEach
            }
            val delta = value - (prev[key] ?: 0L)
            if (delta <= 0L) return@forEach
            if (name.contains(pkg)) {
                if (delta > bestPkg) bestPkg = delta
                if (name.contains("SurfaceView") && delta > bestSurface) bestSurface = delta
            }
            if ((name.contains("ttPlayer") || name.contains("Video") ||
                    name.contains("MediaCodec")) && delta > bestVideo
            ) {
                bestVideo = delta
            }
        }
        val best = when {
            bestSurface > 0L -> bestSurface
            bestPkg > 0L -> bestPkg
            else -> bestVideo
        }
        if (best <= 0L) return null
        val fps = best / dt
        return if (fps in 1f..220f) fps else null
    }

    private fun parseFps(pkg: String): Triple<Float, Float, Float>? {
        val out = execCmd("dumpsys gfxinfo $pkg framestats") ?: return null
        val frames = mutableListOf<Long>()
        var inData = false
        out.lineSequence().forEach { line ->
            if (line.contains("---PROFILEDATA---")) {
                inData = !inData
                return@forEach
            }
            if (!inData || line.startsWith("Flags,")) return@forEach
            val parts = line.split(",")
            if (parts.size > 4) {
                parts[3].toLongOrNull()?.takeIf { it > 0L }?.let { frames.add(it) }
            }
        }
        if (frames.size < 3) return null
        val now = System.nanoTime()
        val recent = frames.filter { it >= now - 3_000_000_000L }.sorted()
        if (recent.size < 3) return Triple(0f, 0f, 0f)
        val span = recent.last() - recent.first()
        if (span <= 0L) return null
        val fps = (recent.size - 1) * 1_000_000_000f / span
        val intervals = recent.zipWithNext { a, b -> b - a }.filter { it > 0L }
        if (intervals.isEmpty()) return null
        val sortedIntervals = intervals.sorted()
        val median = sortedIntervals[sortedIntervals.size / 2]
        val jank = intervals.count { it > median * 1.5 } * 100f / intervals.size
        val p99 = sortedIntervals[((sortedIntervals.size - 1) * 0.99f).toInt()] / 1_000_000f
        return Triple(fps, jank, p99)
    }

    private fun loadOf(name: String, total: Long?, idle: Long?): Float {        if (total == null || idle == null) return 0f
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

    private fun toCelsius(raw: Long): Float = when {
        raw > 10000 -> raw / 1000f
        raw > 1000 -> raw / 100f
        raw > 200 -> raw / 10f
        else -> raw.toFloat()
    }

    private fun nums(lines: List<String>?): List<Long> =
        lines.orEmpty().mapNotNull { it.trim().toLongOrNull() }

    private fun firstLong(line: String): Long? =
        Regex("\\d+").find(line)?.value?.toLongOrNull()

    fun runRoot(cmd: String, timeoutSeconds: Long = 8): String? = execCmd(cmd, timeoutSeconds)

    private fun execCmd(cmd: String, timeoutSeconds: Long = 8): String? {
        var process: Process? = null
        return try {
            val su = if (File("/system/bin/su").exists()) "/system/bin/su" else "su"
            val pb = ProcessBuilder(su, "-c", cmd)
            pb.redirectErrorStream(true)
            val started = pb.start()
            process = started
            val output = StringBuilder()
            val reader = Thread {
                try {
                    started.inputStream.bufferedReader().use { readerStream ->
                        val buffer = CharArray(8192)
                        while (true) {
                            val count = readerStream.read(buffer)
                            if (count < 0) break
                            output.append(buffer, 0, count)
                        }
                    }
                } catch (_: Exception) {
                }
            }.apply {
                isDaemon = true
                start()
            }
            if (!started.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                started.destroyForcibly()
                return null
            }
            reader.join(1000)
            output.toString()
        } catch (e: Exception) {
            try {
                process?.destroyForcibly()
            } catch (_: Exception) {
            }
            null
        }
    }
}









