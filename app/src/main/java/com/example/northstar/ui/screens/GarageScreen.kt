package com.example.northstar.ui.screens

import com.example.northstar.data.localizedName


import com.example.northstar.settings.L10n
import com.example.northstar.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.northstar.data.FuelFillup
import com.example.northstar.data.MaintenanceItem
import com.example.northstar.ui.TerraNavIcons
import com.example.northstar.ui.components.*
import com.example.northstar.ui.theme.*
import com.example.northstar.viewmodel.GarageUi
import com.example.northstar.viewmodel.GarageViewModel
import com.example.northstar.viewmodel.MaintRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dfRow get() = SimpleDateFormat("MMM d", L10n.locale)
private val dfBar get() = SimpleDateFormat("d/M", L10n.locale)

private fun iconFor(key: String): ImageVector = when (key) {
    "chain"  -> TerraNavIcons.Chain
    "drop"   -> TerraNavIcons.Drop
    "gauge"  -> TerraNavIcons.Gauge
    "thermo" -> TerraNavIcons.Thermo
    "fuel"   -> TerraNavIcons.Fuel
    else     -> TerraNavIcons.Wrench
}

private fun dueText(remainingKm: Int): String = when {
    remainingKm < 0  -> L10n.text(R.string.ui_71e78615aefa, -remainingKm)
    else             -> L10n.text(R.string.ui_feb8c3f85b2d, "%,d".format(remainingKm))
}

@Composable
fun GarageScreen(
    tab: String,
    onTabChange: (String) -> Unit,
    vm: GarageViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsState()
    var showFuel by remember { mutableStateOf(false) }
    var showAddService by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    var showOdo by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
            .padding(bottom = 24.dp),
    ) {
        ScreenHeader(
            eyebrow = "Himalayan 450 · ${"%,d".format(ui.odometerKm)} km",
            title = L10n.text(R.string.ui_f614ac324abb),
            trailing = {
                TerraNavBtn(L10n.text(R.string.ui_8ca2cb929002), onClick = { showOdo = true }, variant = BtnVariant.Ghost, size = BtnSize.Sm)
            },
        )

        TerraNavSegmented(
            options = listOf("Maintenance", "Fuel diary"),
            selected = tab,
            onSelect = onTabChange,
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
        )

        if (tab == "Maintenance")
            MaintenanceTab(ui, onMark = { item -> vm.markServiceDone(item, ui.odometerKm) }, onLog = { showLog = true }, onAdd = { showAddService = true })
        else
            FuelTab(ui, onAdd = { showFuel = true }, onDelete = { vm.deleteFuel(it) })
    }

    if (showFuel) AddFuelDialog(
        defaultOdo = ui.odometerKm,
        onAdd = { l, c, o, loc -> vm.addFuel(l, c, o, loc); showFuel = false },
        onDismiss = { showFuel = false },
    )
    if (showAddService) AddServiceDialog(
        onAdd = { n, ic, iv -> vm.addService(n, ic, iv); showAddService = false },
        onDismiss = { showAddService = false },
    )
    if (showLog) LogServiceDialog(
        rows = ui.maint, odo = ui.odometerKm,
        onMark = { item -> vm.markServiceDone(item, ui.odometerKm) },
        onDelete = { vm.deleteService(it) },
        onAddNew = { showLog = false; showAddService = true },
        onDismiss = { showLog = false },
    )
    if (showOdo) OdometerDialog(ui.odometerKm, onSet = { vm.setOdometer(it); showOdo = false }, onDismiss = { showOdo = false })
}

@Composable
private fun MaintenanceTab(ui: GarageUi, onMark: (MaintenanceItem) -> Unit, onLog: () -> Unit, onAdd: () -> Unit) {
    val toneColor = mapOf("ok" to Gold, "warn" to Warn, "alert" to Alert)
    val hero = ui.maint.minByOrNull { it.remainingKm }

    if (hero != null) {
        val ridden = (ui.odometerKm - hero.item.lastDoneOdoKm).coerceAtLeast(0)
        val frac = (ridden.toFloat() / hero.item.intervalKm.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        val tone = toneColor[hero.tone] ?: Gold
        TerraNavCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Eyebrow(L10n.text(R.string.ui_83922b9c177f))
                    Text(hero.item.localizedName, color = TextHi, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = GeistFamily, letterSpacing = (-0.36).sp, modifier = Modifier.padding(top = 5.dp))
                }
                TerraNavChip(
                    dueText(hero.remainingKm),
                    if (hero.tone == "alert") ChipTone.Alert else if (hero.tone == "warn") ChipTone.Warn else ChipTone.Gold,
                    dot = true,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%,d".format(ridden), color = TextHi, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeistMonoFamily)
                Spacer(Modifier.width(6.dp))
                Text(L10n.text(R.string.ui_7ccdbd4d8595, "%,d".format(hero.item.intervalKm)), color = TextLo, fontSize = 13.sp, fontFamily = GeistMonoFamily, modifier = Modifier.padding(bottom = 3.dp))
            }
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Surf3)) {
                Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(GoldDeep, tone))))
            }
            Spacer(Modifier.height(14.dp))
            TerraNavBtn(L10n.text(R.string.ui_471eb94ac9f9), onClick = { onMark(hero.item) }, icon = TerraNavIcons.Check, variant = BtnVariant.Primary, size = BtnSize.Sm, modifier = Modifier.fillMaxWidth())
        }
    }

    Eyebrow(L10n.text(R.string.ui_4cf8d44a4fee), Modifier.padding(bottom = 8.dp, start = 4.dp))

    TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 6.dp) {
        if (ui.maint.isEmpty()) {
            Text(L10n.text(R.string.ui_371b614b9104), color = TextLo, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
        }
        ui.maint.forEachIndexed { i, row ->
            if (i > 0) TerraNavDivider(Modifier.padding(horizontal = 4.dp))
            val color = toneColor[row.tone] ?: Gold
            val fill = (1f - (row.remainingKm.toFloat().coerceAtLeast(0f) / row.item.intervalKm.coerceAtLeast(1).toFloat())).coerceIn(0.06f, 1f)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 12.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(Surf2).border(1.dp, Line, RoundedCornerShape(11.dp))) {
                    Icon(iconFor(row.item.iconKey), null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(row.item.localizedName, color = TextHi, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeistFamily)
                    Text(L10n.text(R.string.ui_0c9ad7ca7982, "%,d".format(row.item.lastDoneOdoKm)), color = TextLo, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(dueText(row.remainingKm), color = color, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeistMonoFamily)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(56.dp).height(4.dp).clip(CircleShape).background(Surf3)) {
                        Box(Modifier.fillMaxWidth(fill).fillMaxHeight().clip(CircleShape).background(color))
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TerraNavBtn(L10n.text(R.string.ui_01ebfb7fd850), onClick = onLog, icon = TerraNavIcons.Check, variant = BtnVariant.Ghost, size = BtnSize.Md, modifier = Modifier.weight(1f))
        TerraNavBtn(L10n.text(R.string.ui_efc578c58e04), onClick = onAdd, icon = TerraNavIcons.Plus, variant = BtnVariant.Ghost, size = BtnSize.Md, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FuelTab(ui: GarageUi, onAdd: () -> Unit, onDelete: (FuelFillup) -> Unit) {
    TerraNavCard(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Column {
                Eyebrow(L10n.text(R.string.ui_b0699b5d1f83))
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 7.dp)) {
                    Text(ui.avgKmpl30?.let { "%.1f".format(it) } ?: "—", color = Gold, fontSize = 38.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeistMonoFamily, letterSpacing = (-0.76).sp)
                    Spacer(Modifier.width(6.dp))
                    Text("km / l", color = TextMid, fontSize = 14.sp, fontFamily = GeistMonoFamily, modifier = Modifier.padding(bottom = 5.dp))
                }
            }
        }
        val chart = ui.fuel.filter { it.kmpl != null }.take(6).reversed()
        if (chart.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            TerraNavBarChart(
                data = chart.map { BarEntry(dfBar.format(Date(it.fill.dateMs)), it.kmpl!!.toFloat()) },
                height = 108.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 18.dp)) {
        listOf(
            Pair("₹${"%,.0f".format(ui.spent30)}", L10n.text(R.string.ui_76854ee883db)),
            Pair("%.1f l".format(ui.litres30), L10n.text(R.string.ui_790408f90b27, ui.fills30)),
        ).forEach { (v, k) ->
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Surf1).border(1.dp, Line, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(v, color = TextHi, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeistMonoFamily)
                Eyebrow(k, Modifier.padding(top = 4.dp))
            }
        }
    }

    Eyebrow(L10n.text(R.string.ui_b52229c807cf), Modifier.padding(bottom = 8.dp, start = 4.dp))
    TerraNavCard(modifier = Modifier.fillMaxWidth(), padding = 6.dp) {
        if (ui.fuel.isEmpty()) {
            Text(L10n.text(R.string.ui_d064ebf2cec2), color = TextLo, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
        }
        ui.fuel.forEachIndexed { i, row ->
            if (i > 0) TerraNavDivider(Modifier.padding(horizontal = 4.dp))
            val f = row.fill
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onDelete(f) }.padding(horizontal = 6.dp, vertical = 12.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(Surf2).border(1.dp, Line, RoundedCornerShape(11.dp))) {
                    Icon(TerraNavIcons.Fuel, null, tint = TextMid, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("%.1f l · ₹%,.0f".format(f.litres, f.cost), color = TextHi, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeistFamily)
                    Text("${dfRow.format(Date(f.dateMs))} · ${"%,d".format(f.odometerKm)} km${if (f.location.isNotBlank()) " · ${f.location}" else ""}", color = TextLo, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp), maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(row.kmpl?.let { "%.1f".format(it) } ?: "—", color = Gold, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeistMonoFamily)
                    Eyebrow("km/l", Modifier.padding(top = 2.dp))
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    TerraNavBtn(L10n.text(R.string.ui_486f10a2685e), onClick = onAdd, icon = TerraNavIcons.Plus, variant = BtnVariant.Ghost, size = BtnSize.Md, modifier = Modifier.fillMaxWidth())
}

// ── Dialogs ──────────────────────────────────────────────────────────────

@Composable
private fun AddFuelDialog(defaultOdo: Int, onAdd: (Double, Double, Int, String) -> Unit, onDismiss: () -> Unit) {
    var litres by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var odo by remember { mutableStateOf(defaultOdo.toString()) }
    var loc by remember { mutableStateOf("") }
    val valid = litres.toDoubleOrNull() != null && cost.toDoubleOrNull() != null && odo.toIntOrNull() != null
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = valid, onClick = { onAdd(litres.toDouble(), cost.toDouble(), odo.toInt(), loc.trim()) }) { Text(L10n.text(R.string.ui_61cc55aa0453), color = if (valid) Gold else TextLo) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L10n.text(R.string.ui_77dfd2135f4d), color = TextMid) } },
        title = { Text(L10n.text(R.string.ui_486f10a2685e), color = TextHi) },
        text = {
            Column {
                NumField(litres, { litres = it }, L10n.text(R.string.ui_92517c7185b4), true)
                NumField(cost, { cost = it }, L10n.text(R.string.ui_255a32093d6b), true)
                NumField(odo, { odo = it }, L10n.text(R.string.ui_6a1f9fb704ab), false)
                OutlinedTextField(loc, { loc = it }, label = { Text(L10n.text(R.string.ui_b08ded404a8c)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        },
        containerColor = Surf1,
    )
}

@Composable
private fun AddServiceDialog(onAdd: (String, String, Int) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var interval by remember { mutableStateOf("") }
    val icons = listOf("wrench", "chain", "drop", "gauge", "thermo", "fuel")
    var icon by remember { mutableStateOf("wrench") }
    val valid = name.isNotBlank() && interval.toIntOrNull() != null && interval.toInt() > 0
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(enabled = valid, onClick = { onAdd(name.trim(), icon, interval.toInt()) }) { Text(L10n.text(R.string.ui_61cc55aa0453), color = if (valid) Gold else TextLo) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L10n.text(R.string.ui_77dfd2135f4d), color = TextMid) } },
        title = { Text(L10n.text(R.string.ui_efc578c58e04), color = TextHi) },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text(L10n.text(R.string.ui_709a23220f2c)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                NumField(interval, { interval = it }, L10n.text(R.string.ui_a9426d33f02c), false)
                Spacer(Modifier.height(10.dp))
                Eyebrow(L10n.text(R.string.ui_716f63b96e0c))
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    icons.forEach { key ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (key == icon) GoldTint else Surf2)
                                .border(1.dp, if (key == icon) Gold else Line, RoundedCornerShape(10.dp))
                                .clickable { icon = key },
                        ) { Icon(iconFor(key), null, tint = if (key == icon) Gold else TextMid, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        },
        containerColor = Surf1,
    )
}

@Composable
private fun LogServiceDialog(rows: List<MaintRow>, odo: Int, onMark: (MaintenanceItem) -> Unit, onDelete: (MaintenanceItem) -> Unit, onAddNew: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onAddNew) { Text(L10n.text(R.string.ui_efc578c58e04), color = Gold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L10n.text(R.string.ui_bbfa773e5a63), color = TextMid) } },
        title = { Text(L10n.text(R.string.ui_6baa6287639c), color = TextHi) },
        text = {
            Column {
                Text(L10n.text(R.string.ui_1cbf6a273a43, "%,d".format(odo)), color = TextLo, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                rows.forEach { row ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Icon(iconFor(row.item.iconKey), null, tint = TextMid, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(row.item.localizedName, color = TextHi, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onMark(row.item) }) { Text(L10n.text(R.string.ui_e9b450d14bc2), color = Gold, fontSize = 13.sp) }
                        Icon(TerraNavIcons.Cross, "delete", tint = TextLo, modifier = Modifier.size(16.dp).clickable { onDelete(row.item) })
                    }
                }
            }
        },
        containerColor = Surf1,
    )
}

@Composable
private fun OdometerDialog(current: Int, onSet: (Int) -> Unit, onDismiss: () -> Unit) {
    var odo by remember { mutableStateOf(current.toString()) }
    val valid = odo.toIntOrNull() != null
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(enabled = valid, onClick = { onSet(odo.toInt()) }) { Text(L10n.text(R.string.ui_efc007a393f6), color = if (valid) Gold else TextLo) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L10n.text(R.string.ui_77dfd2135f4d), color = TextMid) } },
        title = { Text(L10n.text(R.string.ui_23bfe6ba1a05), color = TextHi) },
        text = { NumField(odo, { odo = it }, L10n.text(R.string.ui_6a1f9fb704ab), false) },
        containerColor = Surf1,
    )
}

@Composable
private fun NumField(value: String, onChange: (String) -> Unit, label: String, decimal: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}
