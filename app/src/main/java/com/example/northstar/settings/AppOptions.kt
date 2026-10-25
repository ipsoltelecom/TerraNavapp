package com.example.northstar.settings

enum class AppLanguage(val tag: String, val label: String) {
    ENGLISH("en", "English"), PORTUGUESE_BR("pt-BR", "Português (Brasil)");
    companion object {
        fun fromTag(tag: String?) = entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}

/** Keep the panel's validated dimensions and codec profile for every quality. */
enum class StreamQuality(val bitrate: Int, val movingFps: Int, val idleFps: Int) {
    ECONOMY(600_000,12,4), BALANCED(1_200_000,24,7), HIGH(2_400_000,24,7);
    companion object {
        fun fromStored(value: String?) = entries.firstOrNull { it.name == value } ?: BALANCED
    }
}
