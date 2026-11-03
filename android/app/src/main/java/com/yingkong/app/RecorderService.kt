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
        const val PREFS = "yingkong"
        const val KEY_OVERLAY = "overlay_enabled"
        private const val CHANNEL = "recording"
        private const val NOTIF_ID = 1001
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var job: Job? = null
    private var homePackage: String? = null

    private var overlayEnabled = false
    private var windowManager: WindowManager? = null
    private var overlayView: TextView? = null
    private var overlayParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        homePackage = resolveHome()
        overlayEnabled = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_OVERLAY, false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (RecorderStore.recording.value) updateOverlay() else startRecording()
            }
            ACTION_STOP -> stopRecording()
            ACTION_SHOW_OVERLAY -> enableOverlay()
            ACTION_HIDE_OVERLAY -> disableOverlay()
            else -> if (!RecorderStore.recording.value && !overlayEnabled) {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun enableOverlay() {
        overlayEnabled = true
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_OVERLAY, true).apply()
        if (!RecorderStore.recording.value) {
            startInForeground("悬浮按钮已开启 · 点按开始记录")
        }
        showOverlay()
        updateNotification()
    }

    private fun disableOverlay() {
        overlayEnabled = false
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_OVERLAY, false).apply()
        hideOverlay()
        if (!RecorderStore.recording.value) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            updateNotification()
        }
    }

    private fun startRecording() {
        if (RecorderStore.recording.value) return
        RecorderStore.begin()
        startInForeground("等待前台应用…")
        showOverlay()
        job?.cancel()
        job = scope.launch {
            while (isActive && RecorderStore.recording.value) {
                val snapshot = withContext(Dispatchers.IO) { RootShell.snapshot() }
                if (snapshot != null) {
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
        RecorderStore.finish(this)
        updateOverlay()
        if (overlayEnabled) {
            startInForeground("悬浮按钮已开启 · 点按开始记录")
            updateNotification()
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
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
        val recording = RecorderStore.recording.value
        val text = if (recording) {
            val name = RecorderStore.targetName.value ?: "前台应用"
            val duration = formatDuration(System.currentTimeMillis() - RecorderStore.startedAt)
            "$name · $duration"
        } else {
            "悬浮按钮已开启 · 点按开始记录"
        }
        try {
            NotificationManagerCompat.from(this).notify(NOTIF_ID, buildNotification(text))
        } catch (_: SecurityException) {
        }
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
            .setContentTitle(if (RecorderStore.recording.value) "颖控正在记录" else "颖控悬浮按钮")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (RecorderStore.recording.value) {
            val stop = PendingIntent.getService(
                this,
                2,
                Intent(this, RecorderService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "停止", stop)
        }
        if (overlayEnabled) {
            val hide = PendingIntent.getService(
                this,
                3,
                Intent(this, RecorderService::class.java).setAction(ACTION_HIDE_OVERLAY),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "隐藏悬浮", hide)
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
            val size = dp(64)
            val params = WindowManager.LayoutParams(
                size,
                size,
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
                gravity = Gravity.TOP or Gravity.START
                x = getSharedPreferences(PREFS, MODE_PRIVATE).getInt("overlay_x", dp(12))
                y = getSharedPreferences(PREFS, MODE_PRIVATE).getInt("overlay_y", dp(240))
            }
            val view = TextView(this).apply {
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 10f
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
            view.text = if (recording) {
                val fps = RecorderStore.samples.lastOrNull()?.fps
                "■\n${fps?.toInt() ?: "--"}"
            } else {
                "●\n记录"
            }
            view.background = overlayBackground(recording)
        }
    }

    private fun overlayBackground(recording: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (recording) 0xF2FF6900.toInt() else 0xE61D1D1F.toInt())
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
                    params.x = startX + dx.toInt()
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
                            .putInt("overlay_x", params.x)
                            .putInt("overlay_y", params.y)
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

    override fun onDestroy() {
        job?.cancel()
        hideOverlay()
        scope.cancel()
        super.onDestroy()
    }
}


