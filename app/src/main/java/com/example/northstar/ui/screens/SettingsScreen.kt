package com.example.northstar.ui.screens

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
    val email = auth.email ?: "Not signed in"
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
    val mapPreferences = remember { com.example.northstar.dash.map.MapPreferences.get(ctx) }
    val nightMap by mapPreferences.night.collectAsState()
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
        ScreenHeader(title = "Settings", onBack = onBack)

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

        SectionLabel("Mapa e informações da via")
        TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
            Text("Modo do mapa · celular e moto", color = TextHi)
            Spacer(Modifier.height(10.dp))
            TerraNavSegmented(listOf("Dia", "Noite"), if (nightMap) "Noite" else "Dia", {
                mapPreferences.setNight(it == "Noite")
            }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text("Semáforos e radares fixos cadastrados. Limite em km/h somente quando identificado na via. Cobertura variável; sem indicação da cor do semáforo e sem acidentes ou obras em tempo real.", color = TextMid, fontSize = 12.sp)
            Text("Dados © OpenStreetMap contributors · ODbL\nConsulta da região via Overpass com internet.", color = TextLo, fontSize = 11.sp)
        }

        SectionLabel("Connection")
        TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 6.dp) {
            SettingRow(TerraNavIcons.Bt, "Tripper Dash",
                sub = when (conn) { ConnectionState.Connected -> "Connected"; ConnectionState.Searching -> "Connecting…"; ConnectionState.Offline -> "Not connected" },
                control = { TerraNavChip(if (conn == ConnectionState.Connected) "Linked" else "Off", if (conn == ConnectionState.Connected) ChipTone.Gold else ChipTone.Off, dot = true) })
            TerraNavDivider(Modifier.padding(horizontal = 6.dp))
            SettingRow(TerraNavIcons.Sync, "Auto-connect on start", "Link when the bike is near",
                control = { TerraNavToggle(autoConnect) { autoConnect = it } })
            TerraNavDivider(Modifier.padding(horizontal = 6.dp))
            SettingRow(TerraNavIcons.Zap, "Stream quality", "Balanced · saves battery",
                control = { Icon(TerraNavIcons.ChevronRight, null, tint = TextLo, modifier = Modifier.size(18.dp)) }, last = true)
        }

        SectionLabel("During a ride")
        TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 6.dp) {
            SettingRow(TerraNavIcons.Power, "Turn phone screen off", "Map keeps streaming to the dash",
                control = { TerraNavToggle(screenOff) { screenOff = it } })
            TerraNavDivider(Modifier.padding(horizontal = 6.dp))
            SettingRow(TerraNavIcons.Dash, "Keep dash awake", "Prevent Tripper sleep",
                control = { TerraNavToggle(keepAwake) { keepAwake = it } }, last = true)
        }

        SectionLabel("Voice & guidance")
        TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
            TerraNavSegmented(listOf("Off", "Chime", "Full TTS"), voice, {
                voiceManager.setMode(when (it) {
                    "Off"      -> com.example.northstar.dash.nav.VoiceMode.OFF
                    "Full TTS" -> com.example.northstar.dash.nav.VoiceMode.FULL
                    else       -> com.example.northstar.dash.nav.VoiceMode.CHIME
                })
            }, Modifier.fillMaxWidth())
        }

        SectionLabel("Units")
        TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
            TerraNavSegmented(listOf("Kilometres", "Miles"), units, { units = it }, Modifier.fillMaxWidth())
        }

        SectionLabel("Sync")
        TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 6.dp) {
            val (syncTitle, syncSub) = when {
                !auth.syncAvailable -> "Local only" to "Add your own Firebase project to sync across devices"
                auth.isSignedIn     -> "Synced" to (auth.email ?: "Signed in")
                else                -> "Not signed in" to "Sign in to sync across devices · data stays local until then"
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
                "Sign out",
                onClick = { authViewModel.signOut(); onSignedOut() },
                icon = TerraNavIcons.Power,
                variant = BtnVariant.Danger,
                size = BtnSize.Md,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
        }

        Text(
            "TERRANAV ${com.example.northstar.update.AppUpdater.get(ctx).installedName} · ${if (!auth.syncAvailable) "local only" else if (auth.isSignedIn) "sync on" else "sync off"}",
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
