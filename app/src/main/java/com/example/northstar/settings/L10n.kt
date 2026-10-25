package com.example.northstar.settings

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Resource-backed language selection. Snapshot reads update Compose without restarting a ride. */
object L10n {
    var language by mutableStateOf(AppLanguage.ENGLISH)
        internal set
    private lateinit var contexts: Map<AppLanguage,Context>
    private var knownStrings: Map<String,Int> = emptyMap()
    val locale: Locale get() = Locale.forLanguageTag(language.tag)
    fun initialize(app: Context, selected: AppLanguage) {
        contexts = AppLanguage.entries.associateWith { lang ->
            val config = Configuration(app.resources.configuration)
            config.setLocale(Locale.forLanguageTag(lang.tag))
            app.createConfigurationContext(config)
        }
        language = selected
        knownStrings = buildMap {
            for (ctx in contexts.values) for (id in L10nCatalog.ids) {
                val value = ctx.getString(id)
                if (!value.contains(Regex("%[0-9]+\\$[sd]"))) put(value,id)
            }
        }
    }
    fun text(@StringRes id: Int, vararg args: Any?): String = contexts.getValue(language).getString(id,*args)
    /** Only for developer-owned option keys/status messages; never for user-entered names. */
    fun known(key: String): String = knownStrings[key]?.let { text(it) } ?: key
}
