package com.example.northstar.dash.nav

import com.example.northstar.settings.L10n
import com.example.northstar.R
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceMode { OFF, CHIME, FULL }

/**
 * Turn-by-turn voice guidance. We own this layer (CLAUDE.md feature 2), so it's a plain
 * per-trip setting: OFF (silent), CHIME (a beep before each turn), or FULL (spoken
 * directions). Singleton so the RouteScreen toggle and the nav loop in [DashViewModel]
 * share one engine + persisted mode.
 *
 * Announcements run through [maybeAnnounce]: as the rider approaches a maneuver it fires
 * once at a FAR range ("in 400 m, turn left onto X") and once NEAR it ("turn left now"),
 * de-duped per maneuver so it never nags.
 */
class VoiceManager private constructor(context: Context) {

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("voice", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(
        runCatching { VoiceMode.valueOf(prefs.getString(KEY_MODE, VoiceMode.CHIME.name)!!) }
            .getOrDefault(VoiceMode.CHIME)
    )
    val mode = _mode.asStateFlow()

    fun setMode(m: VoiceMode) {
        prefs.edit().putString(KEY_MODE, m.name).apply()
        _mode.value = m
        if (m == VoiceMode.OFF) tts?.stop()
        else if (m == VoiceMode.FULL) ensureTts()  // warm up so the first turn isn't silent
    }

    // ── TTS engine (lazy; created only when FULL is actually used) ──────────────
    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var ttsReady = false
    private var tone: ToneGenerator? = null

    private fun ensureTts() {
        if (tts != null) return
        tts = TextToSpeech(app) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts?.language = L10n.locale
            else Log.w(TAG, "TextToSpeech init failed: $status")
        }
    }

    private fun chime() {
        runCatching {
            if (tone == null) tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
        }.onFailure { Log.w(TAG, "chime failed: ${it.message}") }
    }

    private fun speak(text: String) {
        ensureTts()
        if (ttsReady && (tts?.setLanguage(L10n.locale) ?: TextToSpeech.LANG_NOT_SUPPORTED) >= TextToSpeech.LANG_AVAILABLE) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
        else chime() // engine not ready yet → at least beep
    }

    // ── Announcement scheduling ────────────────────────────────────────────────
    private var lastManeuverKey = -1.0   // cumulativeMeters of the maneuver we're tracking
    private var farDone = false
    private var nearDone = false
    private var arrived = false

    /**
     * Call every nav tick. [maneuver] is the upcoming turn (null = none), [distanceM] the
     * distance to it, [remainingM] distance to the destination.
     */
    fun maybeAnnounce(maneuver: Maneuver?, distanceM: Double, remainingM: Double) {
        if (_mode.value == VoiceMode.OFF) return

        // Arrival (announced once).
        if (remainingM in 0.0..ARRIVE_M && !arrived) {
            arrived = true
            if (_mode.value == VoiceMode.FULL) speak(L10n.text(R.string.ui_35bf77dde229)) else chime()
            return
        }
        if (remainingM > ARRIVE_M) arrived = false

        if (maneuver == null) return
        // New maneuver → reset the per-turn de-dupe flags.
        if (maneuver.cumulativeMeters != lastManeuverKey) {
            lastManeuverKey = maneuver.cumulativeMeters
            farDone = false; nearDone = false
        }
        when {
            distanceM in 0.0..NEAR_M && !nearDone -> {
                nearDone = true; farDone = true
                if (_mode.value == VoiceMode.FULL) speak(L10n.text(R.string.ui_16b1f2ee820f, turnPhrase(maneuver))) else chime()
            }
            distanceM in NEAR_M..FAR_M && !farDone -> {
                farDone = true
                if (_mode.value == VoiceMode.FULL) speak(L10n.text(R.string.ui_87c4ef4bb56d, roundDist(distanceM), turnPhrase(maneuver))) else chime()
            }
        }
    }

    /** Reset trip state when nav starts/stops so the next route announces cleanly. */
    fun resetTrip() { lastManeuverKey = -1.0; farDone = false; nearDone = false; arrived = false }

    fun shutdown() {
        runCatching { tts?.stop(); tts?.shutdown() }
        tts = null; ttsReady = false
        runCatching { tone?.release() }; tone = null
    }

    private fun turnPhrase(m: Maneuver): String = when (m.type) {
        ManeuverType.TURN_LEFT    -> L10n.text(R.string.ui_8da88b6de957)
        ManeuverType.TURN_RIGHT   -> L10n.text(R.string.ui_c8845877c8fe)
        ManeuverType.SLIGHT_LEFT  -> L10n.text(R.string.ui_2c5ddc7302f4)
        ManeuverType.SLIGHT_RIGHT -> L10n.text(R.string.ui_cf05b995ee0f)
        ManeuverType.SHARP_LEFT   -> L10n.text(R.string.ui_8494af38f0c6)
        ManeuverType.SHARP_RIGHT  -> L10n.text(R.string.ui_51854980827b)
        ManeuverType.UTURN        -> L10n.text(R.string.ui_b755468008e2)
        ManeuverType.ROUNDABOUT   -> L10n.text(R.string.ui_ac2d0c52fc73)
        ManeuverType.ARRIVE       -> L10n.text(R.string.ui_502a243e79f8)
        else                      -> L10n.text(R.string.ui_5c7ad5564578)
    }

    private fun roundDist(m: Double): String = when {
        m >= 1000 -> L10n.text(R.string.nav_kilometers, "%.1f".format(L10n.locale, m / 1000.0))
        m >= 500  -> L10n.text(R.string.ui_b3a5b8b6cd26)
        m >= 200  -> L10n.text(R.string.ui_c1df0ebd64a0)
        else      -> L10n.text(R.string.ui_35062d22f685, (m / 50).toInt() * 50)
    }

    companion object {
        private const val TAG = "VoiceManager"
        private const val FAR_M = 450.0
        private const val NEAR_M = 60.0
        private const val ARRIVE_M = 30.0
        private const val KEY_MODE = "mode"

        @Volatile private var instance: VoiceManager? = null
        fun get(context: Context): VoiceManager =
            instance ?: synchronized(this) {
                instance ?: VoiceManager(context).also { instance = it }
            }
    }
}
