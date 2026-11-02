package com.yingkong.app

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject

data class RecSample(
    val t: Long,
    val fps: Float,
    val jank: Float,
    val p99: Float,
    val power: Float,
    val temp: Float
)

data class RecSession(
    val id: Long,
    val pkg: String,
    val appName: String,
    val start: Long,
    val end: Long,
    val durationMs: Long,
    val fpsAvg: Float,
    val fpsMin: Float,
    val fpsLow: Float,
    val fpsMax: Float,
    val jankAvg: Float,
    val powerAvg: Float,
    val tempMax: Float,
    val samples: Int,
    val points: List<RecSample> = emptyList()
)

object RecorderStore {
    val recording = mutableStateOf(false)
    val targetPkg = mutableStateOf<String?>(null)
    val targetName = mutableStateOf<String?>(null)
    val samples = mutableStateListOf<RecSample>()
    val sessions = mutableStateListOf<RecSession>()

    var startedAt: Long = 0L
        private set

    private const val PREFS = "yingkong"
    private const val KEY = "sessions"
    private const val MAX_SESSIONS = 20
    private const val MAX_POINTS = 1200

    fun begin() {
        recording.value = true
        startedAt = System.currentTimeMillis()
        targetPkg.value = null
        targetName.value = null
        samples.clear()
    }

    fun setTarget(pkg: String, name: String) {
        targetPkg.value = pkg
        targetName.value = name
    }

    fun add(snapshot: Snapshot) {
        if (!recording.value) return
        if (snapshot.fps < 1f) return
        samples.add(
            RecSample(
                t = System.currentTimeMillis(),
                fps = snapshot.fps,
                jank = snapshot.jankPct,
                p99 = snapshot.p99Ms,
                power = snapshot.power,
                temp = snapshot.thermal
            )
        )
        if (samples.size > 7200) samples.removeAt(0)
    }

    fun finish(context: Context) {
        if (!recording.value) return
        val pkg = targetPkg.value
        if (pkg != null && samples.isNotEmpty()) {
            val raw = samples.toList()
            val warmupEnd = startedAt + 3000
            val summary = raw.filter { it.t >= warmupEnd }.ifEmpty { raw }
            val fpsList = summary.map { it.fps }
            val lowCount = ((fpsList.size * 0.01f).toInt()).coerceAtLeast(1)
            val end = System.currentTimeMillis()
            val session = RecSession(
                id = startedAt,
                pkg = pkg,
                appName = targetName.value ?: pkg,
                start = startedAt,
                end = end,
                durationMs = (end - startedAt).coerceAtLeast(0L),
                fpsAvg = fpsList.average().toFloat(),
                fpsMin = fpsList.minOrNull() ?: 0f,
                fpsLow = fpsList.sorted().take(lowCount).average().toFloat(),
                fpsMax = fpsList.maxOrNull() ?: 0f,
                jankAvg = summary.map { it.jank }.average().toFloat(),
                powerAvg = summary.map { it.power }.average().toFloat(),
                tempMax = summary.maxOfOrNull { it.temp } ?: 0f,
                samples = fpsList.size,
                points = raw.takeLast(MAX_POINTS)
            )
            sessions.add(0, session)
            while (sessions.size > MAX_SESSIONS) sessions.removeAt(sessions.lastIndex)
            persist(context)
        }
        recording.value = false
        targetPkg.value = null
        targetName.value = null
        startedAt = 0L
        samples.clear()
    }

    fun load(context: Context) {
        if (sessions.isNotEmpty()) return
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val points = mutableListOf<RecSample>()
                val pointsArray = o.optJSONArray("points")
                if (pointsArray != null) {
                    for (j in 0 until pointsArray.length()) {
                        val p = pointsArray.getJSONObject(j)
                        points.add(
                            RecSample(
                                t = p.optLong("t"),
                                fps = p.optDouble("fps").toFloat(),
                                jank = p.optDouble("jank").toFloat(),
                                p99 = p.optDouble("p99").toFloat(),
                                power = p.optDouble("power").toFloat(),
                                temp = p.optDouble("temp").toFloat()
                            )
                        )
                    }
                }
                sessions.add(
                    RecSession(
                        id = o.optLong("id"),
                        pkg = o.optString("pkg"),
                        appName = o.optString("appName"),
                        start = o.optLong("start"),
                        end = o.optLong("end"),
                        durationMs = o.optLong("durationMs"),
                        fpsAvg = o.optDouble("fpsAvg").toFloat(),
                        fpsMin = o.optDouble("fpsMin").toFloat(),
                        fpsLow = o.optDouble("fpsLow", o.optDouble("fpsMin")).toFloat(),
                        fpsMax = o.optDouble("fpsMax").toFloat(),
                        jankAvg = o.optDouble("jankAvg").toFloat(),
                        powerAvg = o.optDouble("powerAvg").toFloat(),
                        tempMax = o.optDouble("tempMax").toFloat(),
                        samples = o.optInt("samples"),
                        points = points
                    )
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun persist(context: Context) {
        val array = JSONArray()
        sessions.forEach { s ->
            array.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("pkg", s.pkg)
                    put("appName", s.appName)
                    put("start", s.start)
                    put("end", s.end)
                    put("durationMs", s.durationMs)
                    put("fpsAvg", s.fpsAvg.toDouble())
                    put("fpsMin", s.fpsMin.toDouble())
                    put("fpsLow", s.fpsLow.toDouble())
                    put("fpsMax", s.fpsMax.toDouble())
                    put("jankAvg", s.jankAvg.toDouble())
                    put("powerAvg", s.powerAvg.toDouble())
                    put("tempMax", s.tempMax.toDouble())
                    put("samples", s.samples)
                    val points = JSONArray()
                    s.points.forEach { p ->
                        points.put(
                            JSONObject().apply {
                                put("t", p.t)
                                put("fps", p.fps.toDouble())
                                put("jank", p.jank.toDouble())
                                put("p99", p.p99.toDouble())
                                put("power", p.power.toDouble())
                                put("temp", p.temp.toDouble())
                            }
                        )
                    }
                    put("points", points)
                }
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, array.toString())
            .apply()
    }
}
