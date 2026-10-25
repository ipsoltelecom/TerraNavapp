package com.example.northstar.dash.map

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*
import java.util.Calendar

class MapPreferences private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("terranav_map", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(runCatching {
        MapThemeMode.valueOf(prefs.getString("theme_mode", null)
            ?: if (prefs.getBoolean("night", true)) "NIGHT" else "DAY")
    }.getOrDefault(MapThemeMode.NIGHT))
    val mode = _mode.asStateFlow()
    private val _night = MutableStateFlow(isNightNow())
    val night = _night.asStateFlow()
    init {
        // Application singleton: refreshes while riding, and after time/timezone changes.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            while (isActive) {
                _night.value = isNightNow()
                delay(15_000)
            }
        }
    }
    fun isNightNow() = _mode.value.isNight(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    fun setMode(value: MapThemeMode) {
        prefs.edit().putString("theme_mode", value.name).apply()
        _mode.value = value
        _night.value = isNightNow()
    }
    companion object {
        @Volatile private var instance: MapPreferences? = null
        fun get(context: Context): MapPreferences = instance ?: synchronized(this) {
            instance ?: MapPreferences(context.applicationContext).also { instance = it }
        }
    }
}
