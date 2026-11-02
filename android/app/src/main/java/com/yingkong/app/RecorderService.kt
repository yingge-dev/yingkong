package com.yingkong.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.util.Log
import java.io.File
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
        private const val CHANNEL = "recording"
        private const val NOTIF_ID = 1001
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var homePackage: String? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        homePackage = resolveHome()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRecording()
                return START_NOT_STICKY
            }
            ACTION_START -> startRecording()
            else -> if (!RecorderStore.recording.value) {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (RecorderStore.recording.value) return
        RecorderStore.begin()
        log("start recording")
        startInForeground("等待前台应用…")
        job?.cancel()
        job = scope.launch {
            while (isActive && RecorderStore.recording.value) {
                val snapshot = withContext(Dispatchers.IO) { RootShell.snapshot() }
                log("snap=${snapshot != null} pkg=${snapshot?.fgPackage}")
                if (snapshot != null) {
                    val pkg = snapshot.fgPackage
                    if (pkg != null && isTargetable(pkg)) {
                        if (RecorderStore.targetPkg.value == null) {
                            RecorderStore.setTarget(pkg, resolveLabel(pkg))
                            log("target=$pkg")
                        }
                        if (pkg == RecorderStore.targetPkg.value) {
                            RecorderStore.add(snapshot)
                            log("sample fps=${snapshot.fps} n=${RecorderStore.samples.size}")
                            updateNotification()
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopRecording() {
        job?.cancel()
        job = null
        log("stop target=${RecorderStore.targetPkg.value} n=${RecorderStore.samples.size}")
        RecorderStore.finish(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        val name = RecorderStore.targetName.value ?: "前台应用"
        val duration = formatDuration(System.currentTimeMillis() - RecorderStore.startedAt)
        val notification = buildNotification("$name · $duration")
        try {
            NotificationManagerCompat.from(this).notify(NOTIF_ID, notification)
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
        val stop = PendingIntent.getService(
            this,
            2,
            Intent(this, RecorderService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_record)
            .setContentTitle("颖控正在记录")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "停止", stop)
            .build()
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

    private fun log(@Suppress("UNUSED_PARAMETER") msg: String) {
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
        val minutes = total / 60
        val seconds = total % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}




