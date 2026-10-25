package com.example.northstar.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AppSettings private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("terranav_options",Context.MODE_PRIVATE)
    private val _language = MutableStateFlow(AppLanguage.fromTag(prefs.getString("language",
        if(Locale.getDefault().language == "pt") "pt-BR" else "en")))
    private val _quality = MutableStateFlow(StreamQuality.fromStored(prefs.getString("stream_quality",null)))
    val language = _language.asStateFlow()
    val quality = _quality.asStateFlow()
    init { L10n.initialize(context.applicationContext,_language.value) }
    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString("language",language.tag).apply()
        _language.value = language
        L10n.language = language
    }
    fun setQuality(quality: StreamQuality) {
        prefs.edit().putString("stream_quality",quality.name).apply()
        _quality.value = quality
    }
    companion object {
        @Volatile private var instance: AppSettings? = null
        fun get(context: Context): AppSettings = instance ?: synchronized(this) {
            instance ?: AppSettings(context.applicationContext).also { instance = it }
        }
    }
}
