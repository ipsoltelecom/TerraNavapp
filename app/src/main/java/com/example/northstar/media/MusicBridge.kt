package com.example.northstar.media

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MusicState(
    val connected: Boolean = false, val packageName: String? = null,
    val title: String = "", val artist: String = "", val playing: Boolean = false,
    val canPlayPause: Boolean = false, val canNext: Boolean = false, val canPrevious: Boolean = false,
    val preferred: String? = null, val joystickMusic: Boolean = false, val failed: Boolean = false,
)

/** Uses only supported media sessions. Notification contents are never read or retained. */
class MusicBridge private constructor(context: Context) {
    private val app = context.applicationContext
    private val manager = app.getSystemService(MediaSessionManager::class.java)
    private val audio = app.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val prefs = app.getSharedPreferences("terranav_music", Context.MODE_PRIVATE)
    private var preferred = prefs.getString("player", null)?.takeIf { it in MediaPolicy.players }
    private val _state = MutableStateFlow(MusicState(preferred = preferred))
    val state = _state.asStateFlow()
    private val component = ComponentName(app, MusicAccessService::class.java)
    private var connected = false
    private var controllers = emptyList<MediaController>()
    private val callbacks = mutableMapOf<MediaController, MediaController.Callback>()
    private var selected: MediaController? = null
    private var lastButtonAt = -1_000L
    private val listener = MediaSessionManager.OnActiveSessionsChangedListener { bind(it.orEmpty()) }

    fun connect() {
        disconnect()
        try {
            manager.addOnActiveSessionsChangedListener(listener, component, handler)
            connected = true
            refresh()
        } catch (_: SecurityException) { disconnect() }
    }
    fun disconnect() {
        runCatching { manager.removeOnActiveSessionsChangedListener(listener) }
        callbacks.forEach { (c, cb) -> runCatching { c.unregisterCallback(cb) } }; callbacks.clear()
        controllers = emptyList(); selected = null; connected = false
        _state.value = MusicState(preferred = preferred)
    }
    private fun refresh() {
        if (!connected) return
        try { bind(manager.getActiveSessions(component)) } catch (_: SecurityException) { disconnect() }
    }
    private fun bind(list: List<MediaController>) {
        callbacks.forEach { (c, cb) -> runCatching { c.unregisterCallback(cb) } }; callbacks.clear()
        controllers = list.filter { it.packageName in MediaPolicy.players }
        controllers.forEach { c ->
            val cb = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
                override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
                override fun onSessionDestroyed() { handler.post { refresh() } }
            }
            c.registerCallback(cb, handler); callbacks[c] = cb
        }
        publish()
    }
    private fun publish() {
        val index = MediaPolicy.choose(controllers.map { it.packageName },
            controllers.map { it.playbackState?.state == PlaybackState.STATE_PLAYING }, preferred)
        selected = index?.let { controllers[it] }
        val c = selected
        val playing = c?.playbackState?.state == PlaybackState.STATE_PLAYING
        val actions = c?.playbackState?.actions ?: 0L
        fun has(action: Long) = actions and action != 0L
        _state.value = _state.value.copy(connected = connected, packageName = c?.packageName,
            title = (c?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "").take(240),
            artist = (c?.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "").take(240),
            playing = playing,
            canPlayPause = has(if (playing) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY) || has(PlaybackState.ACTION_PLAY_PAUSE),
            canNext = has(PlaybackState.ACTION_SKIP_TO_NEXT), canPrevious = has(PlaybackState.ACTION_SKIP_TO_PREVIOUS),
            preferred = preferred, failed = false)
    }
    fun select(packageName: String?) {
        preferred = packageName?.takeIf { it in MediaPolicy.players }
        prefs.edit().putString("player", preferred).apply(); publish()
    }
    fun setJoystick(enabled: Boolean) { _state.value = _state.value.copy(joystickMusic = enabled && connected) }
    fun command(action: String) {
        val c = selected ?: return
        val s = _state.value
        try {
            when (action) {
                "toggle" -> if (s.canPlayPause) {
                    val actions = c.playbackState?.actions ?: 0L
                    val direct = if (s.playing) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY
                    if (actions and direct != 0L) {
                        if (s.playing) c.transportControls.pause() else c.transportControls.play()
                    } else {
                        c.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                        c.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                    }
                }
                "next" -> if (s.canNext) c.transportControls.skipToNext()
                "previous" -> if (s.canPrevious) c.transportControls.skipToPrevious()
                "up", "down" -> if (c.playbackInfo?.playbackType == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE)
                    c.adjustVolume(if (action == "up") AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                else audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    if (action == "up") AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            }
        } catch (_: Exception) { _state.value = _state.value.copy(failed = true) }
    }
    /** Consume known left/right events in music mode even when no track is available. */
    fun handleJoystick(code: Int): Boolean {
        if (!_state.value.joystickMusic) return false
        val action = MediaPolicy.joystickAction(code) ?: return false
        val now = SystemClock.elapsedRealtime()
        if (now - lastButtonAt >= 500) { lastButtonAt = now; command(action) }
        return true
    }
    companion object {
        @Volatile private var instance: MusicBridge? = null
        fun get(context: Context): MusicBridge = instance ?: synchronized(this) {
            instance ?: MusicBridge(context).also { instance = it }
        }
    }
}

class MusicAccessService : NotificationListenerService() {
    override fun onListenerConnected() { MusicBridge.get(this).connect() }
    override fun onListenerDisconnected() { MusicBridge.get(this).disconnect() }
    override fun onDestroy() { MusicBridge.get(this).disconnect(); super.onDestroy() }
}
