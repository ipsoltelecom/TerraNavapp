package com.example.northstar

import android.app.Application

class TerraNavApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.example.northstar.settings.AppSettings.get(this)
    }
    // Firebase is auto-initialized from google-services.json via the Google Services plugin.
    // Add any other app-wide init here as phases are built out.
}
