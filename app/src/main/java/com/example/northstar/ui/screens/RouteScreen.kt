package com.example.northstar.ui.screens

import com.example.northstar.settings.L10n
import com.example.northstar.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.northstar.ui.TerraNavIcons
import com.example.northstar.ui.components.*
import com.example.northstar.ui.theme.*
import com.example.northstar.viewmodel.RouteViewModel

@Composable
fun RouteScreen(
    onBack: () -> Unit,
    onSentToDash: (String) -> Unit,
    routeViewModel: RouteViewModel = viewModel(),
) {
    val routeState by routeViewModel.state.collectAsState()
    val dest       = routeState.destination
    val destName   = dest?.name?.ifBlank { L10n.text(R.string.ui_ca99e6ee41dc) } ?: L10n.text(R.string.ui_ca99e6ee41dc)
    val destSub    = when {
        dest?.lat != null && dest.lng != null ->
            "%.5f, %.5f".format(dest.lat, dest.lng)
        dest?.url != null -> L10n.text(R.string.ui_5be37a60d822)
        else              -> ""
    }

    val savedList by routeViewModel.saved.collectAsState()
    val ctx = LocalContext.current
    val voiceManager = remember { com.example.northstar.dash.nav.VoiceManager.get(ctx) }
    val voiceMode by voiceManager.mode.collectAsState()
    val voice = when (voiceMode) {
        com.example.northstar.dash.nav.VoiceMode.OFF   -> "Off"
        com.example.northstar.dash.nav.VoiceMode.CHIME -> "Chime only"
        com.example.northstar.dash.nav.VoiceMode.FULL  -> "Full TTS"
    }
    var sent by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<com.example.northstar.data.SavedLocation?>(null) }

    LaunchedEffect(sent) {
        if (sent) {
            kotlinx.coroutines.delay(650)
            onSentToDash(destName)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Map preview (top 46%)
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.46f)
                .background(MapBase),
        ) {
            // Real Google Maps preview — destination pin + route line.
            TerraNavMap(
                riderLat = null,
                riderLng = null,
                dest = dest?.let { d -> if (d.lat != null && d.lng != null) d.lat to d.lng else null },
                routePoints = routeState.route?.geometry.orEmpty(),
                hasLocationPermission = true,
                fitRoute = true,
                modifier = Modifier.fillMaxSize(),
            )

            // Top bar overlay
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xD9080C0C), Color.Transparent))
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                TerraNavIconBtn(
                    TerraNavIcons.ChevronLeft,
                    onClick = onBack,
                    size = 40.dp,
                    modifier = Modifier.background(Color(0xB30D0F11), IconBtnShape),
                )
                Spacer(Modifier.width(12.dp))
                TerraNavChip(
                    if (routeState.isResolving) L10n.text(R.string.ui_d945e60f4848) else L10n.text(R.string.ui_74435cbf92a4),
                    tone = if (routeState.isResolving) ChipTone.Gold else ChipTone.Neutral,
                    icon = TerraNavIcons.Share,
                    modifier = Modifier.background(Color(0xB30D0F11), CircleShape),
                )
            }
        }

        // Detail sheet (overlapping bottom portion)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-22).dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Bg1)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .padding(bottom = 20.dp),
        ) {
            // Drag handle
            Box(
                Modifier
                    .width(40.dp).height(4.dp)
                    .clip(CircleShape).background(Line3)
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            Eyebrow(L10n.text(R.string.ui_d42713493ca8), Modifier.padding(bottom = 6.dp))

            Row(verticalAlignment = Alignment.Top) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldTint),
                ) {
                    Icon(TerraNavIcons.LocationPin, contentDescription = null, tint = Gold, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    if (routeState.isResolving) {
                        Text(L10n.text(R.string.ui_786a04e7d3fd), color = TextLo, fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = GeistFamily, letterSpacing = (-0.38).sp)
                    } else {
                        Text(destName, color = TextHi, fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = GeistFamily, letterSpacing = (-0.38).sp)
                    }
                    if (destSub.isNotBlank()) {
                        Text(destSub, color = TextMid, fontSize = 13.sp, fontFamily = GeistMonoFamily, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Route stats (real values once routing completes)
            val routing = routeState.routing
            val statsList = listOf(
                Triple(if (routing) "…" else (routeState.distanceText ?: "—"), "", L10n.text(R.string.ui_423208095dd7)),
                Triple(if (routing) "…" else (routeState.durationText ?: "—"), "", L10n.text(R.string.ui_1370004da76f)),
                Triple(if (routing) "…" else (routeState.etaText ?: "—"), "", L10n.text(R.string.ui_342a85758a7a)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                statsList.forEach { (v, u, k) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Surf1)
                            .border(1.dp, Line, RoundedCornerShape(14.dp))
                            .padding(13.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(v, color = TextHi, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeistMonoFamily)
                            Spacer(Modifier.width(4.dp))
                            Text(u, color = TextLo, fontSize = 11.sp, fontFamily = GeistMonoFamily, modifier = Modifier.padding(bottom = 2.dp))
                        }
                        Eyebrow(k, Modifier.padding(top = 4.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
            ) {
                Eyebrow(L10n.text(R.string.ui_921d64e81936))
                Icon(
                    if (voice == "Off") TerraNavIcons.SpeakerOff else TerraNavIcons.Speaker,
                    contentDescription = null,
                    tint = if (voice == "Off") TextLo else Gold,
                    modifier = Modifier.size(18.dp),
                )
            }

            TerraNavSegmented(
                options = listOf("Off", "Chime only", "Full TTS"),
                selected = voice,
                onSelect = {
                    voiceManager.setMode(when (it) {
                        "Off"  -> com.example.northstar.dash.nav.VoiceMode.OFF
                        "Full TTS" -> com.example.northstar.dash.nav.VoiceMode.FULL
                        else   -> com.example.northstar.dash.nav.VoiceMode.CHIME
                    })
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
            )

            val canStart = dest?.lat != null && dest.lng != null && !routeState.isResolving
            TerraNavBtn(
                label = when {
                    sent                   -> L10n.text(R.string.ui_11b1929c3399)
                    routeState.isResolving -> L10n.text(R.string.ui_07dffcbf42e7)
                    routeState.routing     -> L10n.text(R.string.ui_5b44b4b8ac20)
                    else                   -> L10n.text(R.string.ui_2a561f6bf62d)
                },
                onClick = { sent = true },
                icon = if (sent) TerraNavIcons.Check else TerraNavIcons.Navi,
                variant = if (sent) BtnVariant.Secondary else BtnVariant.Primary,
                size = BtnSize.Lg,
                enabled = !sent && canStart,
                modifier = Modifier.fillMaxWidth(),
            )

            if (canStart) {
                Spacer(Modifier.height(10.dp))
                TerraNavBtn(
                    label = L10n.text(R.string.ui_b8eee3c5f910),
                    onClick = { showSave = true },
                    icon = TerraNavIcons.Pin,
                    variant = BtnVariant.Ghost,
                    size = BtnSize.Md,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Saved destinations ──
            if (savedList.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                Eyebrow(L10n.text(R.string.ui_48d9ab5f3e0a), Modifier.padding(bottom = 6.dp, start = 4.dp))
                TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 6.dp) {
                    savedList.forEachIndexed { i, loc ->
                        if (i > 0) TerraNavDivider(Modifier.padding(horizontal = 4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { routeViewModel.selectSaved(loc) }
                                .padding(horizontal = 6.dp, vertical = 12.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(GoldTint),
                            ) { Icon(TerraNavIcons.LocationPin, null, tint = Gold, modifier = Modifier.size(20.dp)) }
                            Spacer(Modifier.width(13.dp))
                            Column(Modifier.weight(1f)) {
                                Text(loc.name, color = TextHi, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeistFamily, maxLines = 1)
                                Text(
                                    if (loc.note.isNotBlank()) loc.note else "%.4f, %.4f".format(loc.lat, loc.lng),
                                    color = TextLo, fontSize = 12.sp, fontFamily = GeistMonoFamily,
                                    modifier = Modifier.padding(top = 2.dp), maxLines = 1,
                                )
                            }
                            Icon(
                                TerraNavIcons.Edit, L10n.text(R.string.ui_9ead47a82a0d), tint = TextLo,
                                modifier = Modifier.size(18.dp).clickable { editing = loc },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSave) SaveLocationDialog(
        defaultName = destName,
        onSave = { name, note -> routeViewModel.saveCurrentDestination(name, note); showSave = false },
        onDismiss = { showSave = false },
    )
    editing?.let { loc ->
        EditLocationDialog(
            loc = loc,
            onSave = { name, note -> routeViewModel.renameSaved(loc, name, note); editing = null },
            onDelete = { routeViewModel.deleteSaved(loc); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun SaveLocationDialog(defaultName: String, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(defaultName) }
    var note by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { androidx.compose.material3.TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), note.trim()) }) { Text(L10n.text(R.string.ui_efc007a393f6), color = if (name.isNotBlank()) Gold else TextLo) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(L10n.text(R.string.ui_77dfd2135f4d), color = TextMid) } },
        title = { Text(L10n.text(R.string.ui_d82bba14f503), color = TextHi) },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(name, { name = it }, label = { Text(L10n.text(R.string.ui_709a23220f2c)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                androidx.compose.material3.OutlinedTextField(note, { note = it }, label = { Text(L10n.text(R.string.ui_4e39567064b6)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        },
        containerColor = Surf1,
    )
}

@Composable
private fun EditLocationDialog(loc: com.example.northstar.data.SavedLocation, onSave: (String, String) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(loc.name) }
    var note by remember { mutableStateOf(loc.note) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { androidx.compose.material3.TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), note.trim()) }) { Text(L10n.text(R.string.ui_efc007a393f6), color = if (name.isNotBlank()) Gold else TextLo) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDelete) { Text(L10n.text(R.string.ui_f6fdbe48dc54), color = Alert) } },
        title = { Text(L10n.text(R.string.ui_67475b1d7205), color = TextHi) },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(name, { name = it }, label = { Text(L10n.text(R.string.ui_709a23220f2c)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                androidx.compose.material3.OutlinedTextField(note, { note = it }, label = { Text(L10n.text(R.string.ui_2c924e308820)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        },
        containerColor = Surf1,
    )
}

