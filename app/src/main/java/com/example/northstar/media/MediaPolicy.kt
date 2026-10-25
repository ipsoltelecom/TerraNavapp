package com.example.northstar.media

object MediaPolicy {
    val players = linkedMapOf("com.spotify.music" to "Spotify",
        "com.google.android.youtube" to "YouTube", "com.google.android.apps.youtube.music" to "YouTube Music")
    fun choose(packages: List<String>, playing: List<Boolean>, preferred: String?): Int? {
        val eligible = packages.indices.filter { packages[it] in players && (preferred == null || packages[it] == preferred) }
        return eligible.firstOrNull { playing.getOrElse(it) { false } } ?: eligible.firstOrNull()
    }
    fun joystickAction(code: Int): String? = when(code) { 0x09 -> "next"; 0x0A -> "previous"; else -> null }
}
