package com.example.northstar.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.northstar.R
import com.example.northstar.media.*
import com.example.northstar.settings.L10n
import com.example.northstar.ui.theme.*

@Composable
fun MusicCard() {
    val ctx = LocalContext.current
    val bridge = remember { MusicBridge.get(ctx) }
    val state by bridge.state.collectAsState()
    var settingsError by remember { mutableStateOf(false) }
    TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
        Text(L10n.text(R.string.music_title), color = TextHi, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        if (!state.connected) {
            Text(L10n.text(R.string.music_access), color = TextMid, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            TerraNavBtn(L10n.text(R.string.music_enable), {
                settingsError = runCatching { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }.isFailure
            }, modifier = Modifier.fillMaxWidth())
            if (settingsError) Text(L10n.text(R.string.music_settings_error), color = TextMid)
        } else {
            val choices = listOf(null) + MediaPolicy.players.keys.toList()
            choices.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { pkg ->
                        TerraNavBtn(pkg?.let { MediaPolicy.players.getValue(it) } ?: L10n.text(R.string.map_auto),
                            { bridge.select(pkg) }, modifier = Modifier.weight(1f), size = BtnSize.Sm,
                            variant = if (state.preferred == pkg) BtnVariant.Primary else BtnVariant.Secondary)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Text(state.packageName?.let { MediaPolicy.players[it] }.orEmpty(), color = Gold, fontSize = 12.sp)
            Text(state.title.ifBlank { L10n.text(R.string.music_start) }, color = TextHi, maxLines = 2)
            if (state.artist.isNotBlank()) Text(state.artist, color = TextLo, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TerraNavBtn(L10n.text(R.string.music_previous), { bridge.command("previous") }, modifier = Modifier.weight(1f), enabled = state.canPrevious, size = BtnSize.Sm)
                TerraNavBtn(L10n.text(if (state.playing) R.string.music_pause else R.string.music_play), { bridge.command("toggle") }, modifier = Modifier.weight(1f), enabled = state.canPlayPause, size = BtnSize.Sm)
                TerraNavBtn(L10n.text(R.string.music_next), { bridge.command("next") }, modifier = Modifier.weight(1f), enabled = state.canNext, size = BtnSize.Sm)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TerraNavBtn(L10n.text(R.string.music_quieter), { bridge.command("down") }, modifier = Modifier.weight(1f), enabled = state.packageName != null, size = BtnSize.Sm, variant = BtnVariant.Secondary)
                TerraNavBtn(L10n.text(R.string.music_louder), { bridge.command("up") }, modifier = Modifier.weight(1f), enabled = state.packageName != null, size = BtnSize.Sm, variant = BtnVariant.Secondary)
            }
            Spacer(Modifier.height(8.dp))
            Text(L10n.text(R.string.music_joystick), color = TextMid, fontSize = 12.sp)
            TerraNavSegmented(listOf("Map", "Music"), if (state.joystickMusic) "Music" else "Map",
                { bridge.setJoystick(it == "Music") }, Modifier.fillMaxWidth())
            Text(L10n.text(R.string.music_joystick_hint), color = TextLo, fontSize = 11.sp)
            if (state.failed) Text(L10n.text(R.string.music_failed), color = TextMid)
        }
        Spacer(Modifier.height(6.dp))
        Text(L10n.text(R.string.music_limits), color = TextLo, fontSize = 11.sp)
    }
}
