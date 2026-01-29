package com.example.internetspeedtest.utils

import android.content.Context
import android.content.SharedPreferences

class SpeedTestSettings(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("speedtest_prefs", Context.MODE_PRIVATE)

  companion object {
    private const val KEY_MIN_CONNECTIONS = "min_connections"
    private const val KEY_MAX_CONNECTIONS = "max_connections"
    private const val KEY_MIN_DURATION = "min_duration"
    private const val KEY_MAX_DURATION = "max_duration"
    private const val KEY_AUTO_START = "auto_start"
    private const val KEY_SHOW_ADVANCED = "show_advanced"

    const val DEFAULT_MIN_CONNECTIONS = 1
    const val DEFAULT_MAX_CONNECTIONS = 8
    const val DEFAULT_MIN_DURATION = 5000L
    const val DEFAULT_MAX_DURATION = 30000L
  }

  var minConnections: Int
    get() = prefs.getInt(KEY_MIN_CONNECTIONS, DEFAULT_MIN_CONNECTIONS)
    set(value) = prefs.edit().putInt(KEY_MIN_CONNECTIONS, value).apply()

  var maxConnections: Int
    get() = prefs.getInt(KEY_MAX_CONNECTIONS, DEFAULT_MAX_CONNECTIONS)
    set(value) = prefs.edit().putInt(KEY_MAX_CONNECTIONS, value).apply()

  var minDuration: Long
    get() = prefs.getLong(KEY_MIN_DURATION, DEFAULT_MIN_DURATION)
    set(value) = prefs.edit().putLong(KEY_MIN_DURATION, value).apply()

  var maxDuration: Long
    get() = prefs.getLong(KEY_MAX_DURATION, DEFAULT_MAX_DURATION)
    set(value) = prefs.edit().putLong(KEY_MAX_DURATION, value).apply()

  var autoStart: Boolean
    get() = prefs.getBoolean(KEY_AUTO_START, true)
    set(value) = prefs.edit().putBoolean(KEY_AUTO_START, value).apply()

  var showAdvanced: Boolean
    get() = prefs.getBoolean(KEY_SHOW_ADVANCED, false)
    set(value) = prefs.edit().putBoolean(KEY_SHOW_ADVANCED, value).apply()
}