package com.yingkong.app

import android.app.Activity
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuAccess {
    const val REQUEST_CODE = 0x5A17

    fun available(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun granted(): Boolean = try {
        available() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    fun request(activity: Activity?) {
        if (activity == null) return
        try {
            if (!granted()) Shizuku.requestPermission(REQUEST_CODE)
        } catch (_: Throwable) {
        }
    }
}
