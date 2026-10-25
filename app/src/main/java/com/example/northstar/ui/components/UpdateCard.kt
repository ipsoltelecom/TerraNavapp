package com.example.northstar.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.northstar.ui.theme.*
import com.example.northstar.update.AppUpdater
import com.example.northstar.update.UpdateStage

@Composable
fun UpdateCard() {
    val context = LocalContext.current
    val updater = remember { AppUpdater.get(context) }
    val state by updater.state.collectAsState()
    val busy = state.stage in listOf(UpdateStage.CHECKING,UpdateStage.DOWNLOADING)
    TerraNavCard(modifier = Modifier.fillMaxWidth(),padding = 14.dp) {
        Text("Atualizações · TerraNav ${updater.installedName}",color = TextHi,fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Text(if(state.rideActive) "Encerre a navegação e desconecte do painel para atualizar." else state.message,
            color = TextMid,fontSize = 12.sp)
        if(state.stage == UpdateStage.DOWNLOADING) {
            Spacer(Modifier.height(8.dp))
            Text("${state.progress}%",color = Gold,fontSize = 15.sp)
        }
        state.release?.let { release ->
            Spacer(Modifier.height(10.dp))
            Text("Nova versão: ${release.versionName}",color = Gold,fontSize = 14.sp)
            if(release.notes.isNotBlank()) Text(release.notes,color = TextMid,fontSize = 12.sp,maxLines = 12)
            Spacer(Modifier.height(10.dp))
            TerraNavBtn(
                label = if(state.stage == UpdateStage.READY) "Instalar atualização" else "Baixar atualização · ${release.size / (1024 * 1024)} MB",
                onClick = { if(state.stage == UpdateStage.READY) updater.install() else updater.download() },
                enabled = !busy && !state.rideActive,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(10.dp))
        TerraNavBtn("Verificar atualizações",{ updater.checkForUpdates(force = true) },
            variant = BtnVariant.Secondary,enabled = !busy && !state.rideActive,modifier = Modifier.fillMaxWidth())
    }
}
