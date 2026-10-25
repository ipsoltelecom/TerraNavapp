package com.example.northstar.settings

import com.example.northstar.media.MediaPolicy
import org.junit.Assert.*
import org.junit.Test

class MediaPolicyTest {
    private val spotify = "com.spotify.music"
    private val youtube = "com.google.android.youtube"
    @Test fun automaticSelectionPrefersPlayingSupportedSession() {
        assertEquals(2, MediaPolicy.choose(listOf("other.app",spotify,youtube),listOf(true,false,true),null))
        assertNull(MediaPolicy.choose(listOf("other.app"),listOf(true),null))
        assertNull(MediaPolicy.choose(emptyList(), emptyList(), null))
    }
    @Test fun explicitPlayerNeverControlsAnotherApp() {
        assertEquals(0, MediaPolicy.choose(listOf(spotify,youtube),listOf(false,true),spotify))
        assertNull(MediaPolicy.choose(listOf(youtube),listOf(true),spotify))
        assertEquals(0, MediaPolicy.choose(listOf("com.google.android.apps.youtube.music"),listOf(true),null))
    }
    @Test fun unmappedJoystickCodesNeverBecomeMediaCommands() {
        assertEquals("next", MediaPolicy.joystickAction(0x09))
        assertEquals("previous", MediaPolicy.joystickAction(0x0A))
        for (code in 0..255) if (code !in listOf(0x09,0x0A)) assertNull(MediaPolicy.joystickAction(code))
    }
}
