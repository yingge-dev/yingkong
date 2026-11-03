package com.yingkong.app

import android.content.Context
import java.util.UUID

object ControlToken {
    private const val PREFS = "yingkong"
    private const val KEY = "control_token"

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY, null)
        if (!existing.isNullOrBlank()) return existing
        val token = UUID.randomUUID().toString()
        prefs.edit().putString(KEY, token).apply()
        return token
    }
}
