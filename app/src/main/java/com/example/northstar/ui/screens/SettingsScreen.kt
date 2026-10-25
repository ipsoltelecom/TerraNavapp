package com.example.northstar.ui.screens

import com.example.northstar.settings.L10n
import com.example.northstar.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.northstar.ui.TerraNavIcons
import com.example.northstar.ui.components.*
import com.example.northstar.ui.theme.*
import com.example.northstar.viewmodel.AuthViewModel
import com.example.northstar.viewmodel.ConnectionState

@Composable
fun SettingsScreen(
    conn: ConnectionState,
    onConnChange: (ConnectionState) -> Unit,
    authViewModel: AuthViewModel,
    onSignedOut: () -> Unit,
    onBack: () -> Unit,
) {
    val auth by authViewModel.state.collectAsState()
    val email = auth.email ?: L10n.text(R.string.ui_848d80ca32cf)
    val initials = remember(auth.email, auth.displayName) {
        val src = auth.displayName?.takeIf { it.isNotBlank() } ?: auth.email ?: "?"
        src.split(" ", ".", "@").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "?" }
    }

    var autoConnect by remember { mutableStateOf(true) }
    var screenOff   by remember { mutableStateOf(true) }
    var keepAwake   by remember { mutableStateOf(true) }
    var units       by remember { mutableStateOf("Kilometres") }

    // Real voice setting, shared with RouteScreen via the VoiceManager singleton.
    val ctx = LocalContext.current
    val appSettings = remember { com.example.northstar.settings.AppSettings.get(ctx) }
    val language by appSettings.language.collectAsState()
    val quality by appSettings.quality.collectAsState()
    val mapPreferences = remember { com.example.northstar.dash.map.MapPreferences.get(ctx) }
    val mapMode by mapPreferences.mode.collectAsState()
    val voiceManager = remember { com.example.northstar.dash.nav.VoiceManager.get(ctx) }
    val voiceMode by voiceManager.mode.collectAsState()
    val voice = when (voiceMode) {
        com.example.northstar.dash.nav.VoiceMode.OFF   -> "Off"
        com.example.northstar.dash.nav.VoiceMode.CHIME -> "Chime"
        com.example.northstar.dash.nav.VoiceMode.FULL  -> "Full TTS"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
            .padding(bottom = 24.dp),
    ) {
        ScreenHeader(title = L10n.text(R.string.ui_c7f73bb54d92), onBack = onBack)

        SectionLabel(L10n.text(R.string.ui_89b86ab0e66f))
        TerraNavCard(modifier = Modifier.fillMaxWidth(),padding = 14.dp) {
            TerraNavSegmented(com.example.northstar.settings.AppLanguage.entries.map { it.label },language.label, { label ->
                appSettings.setLanguage(com.example.northstar.settings.AppLanguage.entries.first { it.label == label })
            },Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(16.dp))

        MusicCard()
        Spacer(Modifier.height(16.dp))
        UpdateCard()
        Spacer(Modifier.height(16.dp))

        // Account card
        TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 16.dp, onClick = {}) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GoldTint),
                ) {
                    Text(initials, color = Gold, fontFamily = GeistMonoFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    auth.displayName?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = TextHi, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeistFamily)
                    }
                    Text(email, color = if (auth.displayName.isNullOrBlank()) TextHi else TextMid, fontSize = if (auth.displayName.isNullOrBlank()) 15.5.sp else 12.5.sp, fontWeight = if (auth.displayName.isNullOrBlank()) FontWeight.SemiBold else FontWeight.Normal, fontFamily = GeistFamily, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        SectionLabel(L10n.text(R.string.ui_e00c87d1cf55))
        TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
            Text(L10n.text(R.string.ui_02e64b7a40b5), color = TextHi)
            Spacer(Modifier.height(10.dp))
            val mapModes = listOf("Dia", "Noite", "Automatic")
            TerraNavSegmented(mapModes, mapModes[mapMode.ordinal], {
                mapPreferences.setMode(com.example.northstar.dash.map.MapThemeMode.entries[mapModes.indexOf(it)])
            }, Modifier.fillMaxWidth())
            Text(L10n.text(R.string.map_schedule), color = TextLo, fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            Text(L10n.text(R.string.ui_c9384f83fdd4), color = TextMid, fontSize = 12.sp)
            Text(L10n.text(R.string.ui_2977e6dd5998), color = TextLo, fontSize = 11.sp)
        }

        SectionLabel(L10n.text(R.string.ui_6512ee1541e9))
        TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 6.dp) {
            SettingRow(TerraNavIcons.Bt, "Tripper Dash",
                sub = when (conn) { ConnectionState.Connected -> L10n.text(R.string.ui_c2f9b7b4897f); ConnectionState.Searching -> L10n.text(R.string.ui_fd3e79699426); ConnectionState.Offline -> L10n.text(R.string.ui_8b02f3de3906) },
                control = { TerraNavChip(if (conn == ConnectionState.Connected) L10n.text(R.string.ui_a089f600e3d2) else "Off", if (conn == ConnectionState.Connected) ChipTone.Gold else ChipTone.Off, dot = true) })
            TerraNavDivider(Modifier.padding(horizontal = 6.dp))
            SettingRow(TerraNavIcons.Sync, L10n.text(R.string.ui_b26ea400ee1e), L10n.text(R.string.ui_4252ae92ebb4),
                control = { TerraNavToggle(autoConnect) { autoConnect = it } })
        }

        SectionLabel(L10n.text(R.string.ui_68b0ab5b1850))
        TerraNavCard(modifier = Modifier.fillMaxWidth(),padding = 14.dp) {
            val labels = listOf("Economy", "Balanced", "High")
            TerraNavSegmented(labels,labels[quality.ordinal], { label ->
                appSettings.setQuality(com.example.northstar.settings.StreamQuality.entries[labels.indexOf(label)])
            },Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Text(when(quality) {
                com.example.northstar.settings.StreamQuality.ECONOMY -> L10n.text(R.string.ui_ee155a54ab07)
                com.example.northstar.settings.StreamQuality.BALANCED -> L10n.text(R.string.ui_72afc2a59cd5)
                com.example.northstar.settings.StreamQuality.HIGH -> L10n.text(R.string.ui_0dbbff281a01)
            },color = TextMid,fontSize = 12.sp)
            Text(L10n.text(R.string.ui_e6e9167a278a),color = TextLo,fontSize = 12.sp)
        }

        SectionLabel(L10n.text(R.string.ui_f43f9cbb198f))
        TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 6.dp) {
            SettingRow(TerraNavIcons.Power, L10n.text(R.string.ui_24158c113d59), L10n.text(R.string.ui_f3205d7e31c3),
                control = { TerraNavToggle(screenOff) { screenOff = it } })
            TerraNavDivider(Modifier.padding(horizontal = 6.dp))
            SettingRow(TerraNavIcons.Dash, L10n.text(R.string.ui_6ac5e6f53a72), L10n.text(R.string.ui_0bda22782d81),
                control = { TerraNavToggle(keepAwake) { keepAwake = it } }, last = true)
        }

        SectionLabel(L10n.text(R.string.ui_d0778ab42a66))
        TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
            TerraNavSegmented(listOf("Off", "Chime", "Full TTS"), voice, {
                voiceManager.setMode(when (it) {
                    "Off"      -> com.example.northstar.dash.nav.VoiceMode.OFF
                    "Full TTS" -> com.example.northstar.dash.nav.VoiceMode.FULL
                    else       -> com.example.northstar.dash.nav.VoiceMode.CHIME
                })
            }, Modifier.fillMaxWidth())
        }

        SectionLabel(L10n.text(R.string.ui_12748281e0f0))
        TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
            TerraNavSegmented(listOf("Kilometres", "Miles"), units, { units = it }, Modifier.fillMaxWidth())
        }

        SectionLabel(L10n.text(R.string.ui_905f63092ff2))
        TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 6.dp) {
            val (syncTitle, syncSub) = when {
                !auth.syncAvailable -> L10n.text(R.string.ui_dd2a30568400) to L10n.text(R.string.ui_b41d6b8adedf)
                auth.isSignedIn     -> L10n.text(R.string.ui_b2ef6af67fe0) to (auth.email ?: L10n.text(R.string.ui_1b6c524ffb71))
                else                -> L10n.text(R.string.ui_848d80ca32cf) to L10n.text(R.string.ui_c145743af88f)
            }
            SettingRow(TerraNavIcons.Sync, syncTitle, syncSub,
                control = {
                    TerraNavChip(
                        if (auth.isSignedIn) "On" else "Off",
                        if (auth.isSignedIn) ChipTone.Gold else ChipTone.Off, dot = true,
                    )
                }, last = true)
        }

        Spacer(Modifier.height(22.dp))

        if (auth.isSignedIn) {
            TerraNavBtn(
                L10n.text(R.string.ui_dc1649a16c14),
                onClick = { authViewModel.signOut(); onSignedOut() },
                icon = TerraNavIcons.Power,
                variant = BtnVariant.Danger,
                size = BtnSize.Md,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
        }

        Text(
            "TERRANAV ${com.example.northstar.update.AppUpdater.get(ctx).installedName} · ${if (!auth.syncAvailable) L10n.text(R.string.ui_c7ac183be49f) else if (auth.isSignedIn) L10n.text(R.string.ui_9200a84eb7b5) else L10n.text(R.string.ui_4bfdea6ff4dd)}",
            color = TextDis, fontSize = 11.sp, fontFamily = GeistMonoFamily,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp),
        )
    }
}

@Composable
private fun SectionLabel(label: String) {
    Eyebrow(label, Modifier.padding(top = 22.dp, bottom = 9.dp, start = 4.dp))
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    sub: String? = null,
    control: @Composable () -> Unit,
    last: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 13.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Surf2).border(1.dp, Line, RoundedCornerShape(11.dp)),
        ) {
            Icon(icon, contentDescription = null, tint = TextMid, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextHi, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeistFamily)
            if (sub != null) Text(sub, color = TextLo, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        control()
    }
}
