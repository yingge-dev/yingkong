package com.yingkong.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_USER_UNLOCKED
        ) return
        val prefs = context.getSharedPreferences(RecorderService.PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(RecorderService.KEY_OVERLAY, false)) return
        if (!Settings.canDrawOverlays(context)) return
        try {
            ContextCompat.startForegroundService(
                context,
                RecorderService.controlIntent(context, RecorderService.ACTION_SHOW_OVERLAY)
            )
        } catch (_: Exception) {
        }
    }
}

