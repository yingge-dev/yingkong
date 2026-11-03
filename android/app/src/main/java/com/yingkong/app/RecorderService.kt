package com.yingkong.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecorderService : Service() {

    companion object {
        const val ACTION_START = "com.yingkong.app.action.START_RECORD"
        const val ACTION_STOP = "com.yingkong.app.action.STOP_RECORD"
        const val ACTION_SHOW_OVERLAY = "com.yingkong.app.action.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.yingkong.app.action.HIDE_OVERLAY"
        const val ACTION_STOP_SERVICE = "com.yingkong.app.action.STOP_SERVICE"
        const val PREFS = "yingkong"
        const val KEY_OVERLAY = "overlay_enabled"
        const val EXTRA_TOKEN = "token"
        private const val CHANNEL = "recording"
        private const val NOTIF_ID = 1001

        fun controlIntent(context: android.content.Context, action: String): Intent =
            Intent(context, RecorderService::class.java)
                .setAction(action)
                .putExtra(EXTRA_TOKEN, ControlToken.get(context))
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var job: Job? = null
    private var idleJob: Job? = null
    private var homePackage: String? = null

    private var overlayEnabled = false
    private lateinit var controlToken: String
    private var windowManager: WindowManager? = null
    private var overlayView: TextView? = null
    private var overlayParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        controlToken = ControlToken.get(this)
        homePackage = resolveHome()
        overlayEnabled = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_OVERLAY, false)
        scope.launch(Dispatchers.IO) {
            RootShell.runRoot(
                "mkdir -p /data/adb/yingkong; echo '$controlToken' > /data/adb/yingkong/token; chmod 600 /data/adb/yingkong/token"
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val suppliedToken = intent?.getStringExtra(EXTRA_TOKEN)
        if (intent?.action != null && suppliedToken != controlToken) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_START -> {
                if (RecorderStore.recording.value) updateOverlay() else startRecording()
            }
            ACTION_STOP -> stopRecording()
            ACTION_SHOW_OVERLAY -> enableOverlay()
            ACTION_HIDE_OVERLAY -> disableOverlay()
            ACTION_STOP_SERVICE -> {
                stopRecording()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (overlayEnabled) {
                    startInForeground("悬浮窗已隐藏 · 通知栏可显示")
                    showOverlay()
                    updateNotification()
                } else if (!RecorderStore.recording.value) {
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }
        return if (overlayEnabled || RecorderStore.recording.value) START_STICKY else START_NOT_STICKY
    }

    private fun enableOverlay() {
        overlayEnabled = true
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_OVERLAY, true).apply()
        if (!RecorderStore.recording.value) {
            startInForeground("悬浮按钮已开启 · 点按开始记录")
            startIdleMonitoring()
        }
        setAutostart(true)
        showOverlay()
        updateNotification()
    }

    private fun disableOverlay() {
        overlayEnabled = false
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_OVERLAY, false).apply()
        setAutostart(false)
        hideOverlay()
        idleJob?.cancel()
        idleJob = null
        if (!RecorderStore.recording.value) {
            startInForeground("悬浮窗已隐藏 · 通知栏可显示")
        }
        updateNotification()
    }

    private fun startRecording() {
        if (RecorderStore.recording.value) return
        RecorderStore.begin()
        idleJob?.cancel()
        idleJob = null
        RootShell.enableFrameStats()
        startInForeground("等待前台应用…")
        showOverlay()
        job?.cancel()
        job = scope.launch {
            while (isActive && RecorderStore.recording.value) {
                val snapshot = withContext(Dispatchers.IO) { RootShell.snapshot() }
                if (snapshot != null) {
                    RecorderStore.latest.value = snapshot
                    val pkg = snapshot.fgPackage
                    if (pkg != null && isTargetable(pkg)) {
                        if (RecorderStore.targetPkg.value == null) {
                            RecorderStore.setTarget(pkg, resolveLabel(pkg))
                            RootShell.runRoot("echo $pkg > /data/adb/yingkong/target")
                        }
                        if (pkg == RecorderStore.targetPkg.value) {
                            RecorderStore.add(snapshot)
                            updateNotification()
                            updateOverlay()
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopRecording() {
        if (!RecorderStore.recording.value) return
        job?.cancel()
        job = null
        val pkg = RecorderStore.targetPkg.value
        val frameStats = if (pkg != null) RootShell.captureFrameStats(pkg) else null
        RecorderStore.finish(this, frameStats)
        updateOverlay()
        if (overlayEnabled) {
            startInForeground("悬浮按钮已开启 · 点按开始记录")
            startIdleMonitoring()
            updateNotification()
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun setAutostart(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            val path = "/data/adb/yingkong/autostart"
            RootShell.runRoot(if (enabled) "touch $path" else "rm -f $path")
        }
    }

    private fun startIdleMonitoring() {
        if (!overlayEnabled || RecorderStore.recording.value) return
        if (idleJob?.isActive == true) return
        idleJob = scope.launch {
            while (isActive && overlayEnabled && !RecorderStore.recording.value) {
                val snapshot = withContext(Dispatchers.IO) { RootShell.snapshot() }
                if (snapshot != null) {
                    RecorderStore.latest.value = snapshot
                    updateNotification()
                    updateOverlay()
                }
                delay(2000)
            }
        }
    }

    private fun startInForeground(text: String) {
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun updateNotification() {
        val text = metricsLine()
        try {
            NotificationManagerCompat.from(this).notify(NOTIF_ID, buildNotification(text))
        } catch (_: SecurityException) {
        }
    }

    private fun metricsLine(): String {
        val snapshot = RecorderStore.latest.value ?: return "正在读取系统数据…"
        return "FPS %.0f · CPU %.0f%% · %.1f°C · %.2fW".format(
            snapshot.fps, snapshot.cpu, snapshot.thermal, snapshot.power
        )
    }

    private fun bigText(): String {
        val builder = StringBuilder()
        val recording = RecorderStore.recording.value
        if (recording) {
            val name = RecorderStore.targetName.value ?: "前台应用"
            val duration = formatDuration(System.currentTimeMillis() - RecorderStore.startedAt)
            builder.append(name).append(" · ").append(duration).append('\n')
        }
        val snapshot = RecorderStore.latest.value
        if (snapshot != null) {
            builder.append("FPS %.1f · Jank %.1f%%".format(snapshot.fps, snapshot.jankPct)).append('\n')
            builder.append("CPU %.0f%% · GPU %.0f%%".format(snapshot.cpu, snapshot.gpu)).append('\n')
            builder.append("温度 %.1f°C · 功耗 %.2fW".format(snapshot.thermal, snapshot.power)).append('\n')
            builder.append("电池 %.0f%% · %.2fV · %.0fmA".format(
                snapshot.batt, snapshot.voltage, snapshot.currentMa
            ))
        } else {
            builder.append("正在读取系统数据…")
        }
        if (!recording) {
            builder.append('\n').append(
                if (overlayEnabled) "点悬浮按钮开始记录" else "悬浮窗已隐藏，点通知栏可重新显示"
            )
        }
        return builder.toString()
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_record)
            .setContentTitle(
                when {
                    RecorderStore.recording.value -> "颖控正在记录"
                    overlayEnabled -> "颖控悬浮按钮"
                    else -> "颖控已隐藏"
                }
            )
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText()))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (RecorderStore.recording.value) {
            val stop = PendingIntent.getService(
                this,
                2,
                Intent(this, RecorderService::class.java).setAction(ACTION_STOP).putExtra(EXTRA_TOKEN, controlToken),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "停止", stop)
        }
        if (overlayEnabled) {
            val hide = PendingIntent.getService(
                this,
                3,
                Intent(this, RecorderService::class.java).setAction(ACTION_HIDE_OVERLAY).putExtra(EXTRA_TOKEN, controlToken),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "隐藏悬浮", hide)
        } else {
            val show = PendingIntent.getService(
                this,
                4,
                Intent(this, RecorderService::class.java).setAction(ACTION_SHOW_OVERLAY).putExtra(EXTRA_TOKEN, controlToken),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "显示悬浮", show)
        }
        if (!RecorderStore.recording.value) {
            val close = PendingIntent.getService(
                this,
                5,
                Intent(this, RecorderService::class.java).setAction(ACTION_STOP_SERVICE).putExtra(EXTRA_TOKEN, controlToken),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "关闭", close)
        }
        return builder.build()
    }

    private fun showOverlay() {
        if (!overlayEnabled) return
        mainHandler.post {
            if (overlayView != null) {
                updateOverlay()
                return@post
            }
            if (!Settings.canDrawOverlays(this)) return@post
            val manager = getSystemService(WINDOW_SERVICE) as WindowManager
            val width = dp(56)
            val height = dp(56)
            val params = WindowManager.LayoutParams(
                width,
                height,
                if (Build.VERSION.SDK_INT >= 26) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = getSharedPreferences(PREFS, MODE_PRIVATE).getInt("overlay2_x", dp(8))
                y = getSharedPreferences(PREFS, MODE_PRIVATE).getInt("overlay2_y", dp(220))
            }
            val view = TextView(this).apply {
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 10f
                setPadding(dp(8), dp(4), dp(8), dp(4))
                elevation = dp(6).toFloat()
                background = overlayBackground(false)
                isClickable = true
                setOnClickListener {
                    if (RecorderStore.recording.value) stopRecording() else startRecording()
                }
                setOnTouchListener(makeOverlayTouch())
            }
            manager.addView(view, params)
            windowManager = manager
            overlayView = view
            overlayParams = params
            updateOverlay()
        }
    }

    private fun hideOverlay() {
        mainHandler.post {
            val view = overlayView ?: return@post
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {
            }
            overlayView = null
            overlayParams = null
        }
    }

    private fun updateOverlay() {
        mainHandler.post {
            val view = overlayView ?: return@post
            val recording = RecorderStore.recording.value
            val snapshot = RecorderStore.latest.value
            val fps = snapshot?.fps ?: 0f
            view.text = "%.0f\nFPS".format(fps)
            view.background = overlayBackground(recording)
        }
    }

    private fun overlayBackground(recording: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (recording) 0xE6FF6900.toInt() else 0xCC1D1D1F.toInt())
            setStroke(dp(2), if (recording) Color.WHITE else 0xFFFF6900.toInt())
        }
    }

    private fun makeOverlayTouch(): View.OnTouchListener {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        return View.OnTouchListener { view, event ->
            val params = overlayParams ?: return@OnTouchListener false
            val manager = windowManager ?: return@OnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d("OverlayTouch", "down ${event.rawX},${event.rawY}")
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true
                    params.x = startX - dx.toInt()
                    params.y = startY + dy.toInt()
                    try {
                        manager.updateViewLayout(view, params)
                    } catch (_: Exception) {
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    Log.d("OverlayTouch", "up moved=$moved")
                    if (moved) {
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putInt("overlay2_x", params.x)
                            .putInt("overlay2_y", params.y)
                            .apply()
                    } else {
                        view.performClick()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    Log.d("OverlayTouch", "cancel")
                    true
                }
                else -> false
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL,
                "性能记录",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "记录前台应用的帧率、功耗与温度"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun isTargetable(pkg: String): Boolean {
        if (pkg == packageName || pkg == homePackage) return false
        if (pkg == "com.android.permissioncontroller" || pkg == "com.android.systemui") return false
        return try {
            val info = packageManager.getApplicationInfo(pkg, 0)
            val system = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val updated = (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            !system || updated
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveHome(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName
    }

    private fun resolveLabel(pkg: String): String = try {
        val info = packageManager.getApplicationInfo(pkg, 0)
        packageManager.getApplicationLabel(info).toString()
    } catch (_: Exception) {
        pkg
    }

    private fun formatDuration(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        return "%02d:%02d".format(total / 60, total % 60)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (overlayEnabled) {
            mainHandler.post {
                showOverlay()
                updateNotification()
            }
        } else if (!RecorderStore.recording.value) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        job?.cancel()
        idleJob?.cancel()
        hideOverlay()
        scope.cancel()
        super.onDestroy()
    }
}






