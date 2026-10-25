package com.example.northstar.dash.map

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapPreferences private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("terranav_map", Context.MODE_PRIVATE)
    private val _night = MutableStateFlow(prefs.getBoolean("night", true))
    val night = _night.asStateFlow()
    fun setNight(value: Boolean) {
        prefs.edit().putBoolean("night", value).apply()
        _night.value = value
    }
    companion object {
        @Volatile private var instance: MapPreferences? = null
        fun get(context: Context): MapPreferences = instance ?: synchronized(this) {
            instance ?: MapPreferences(context.applicationContext).also { instance = it }
        }
    }
}
