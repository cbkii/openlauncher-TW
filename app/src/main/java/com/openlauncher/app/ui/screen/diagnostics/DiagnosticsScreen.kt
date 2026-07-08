package com.openlauncher.app.ui.screen.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.viewmodel.LauncherViewModel
import org.json.JSONObject

@Composable
fun DiagnosticsScreen(
    vm: LauncherViewModel,
    onBack: () -> Unit,
    onExport: (String) -> Unit
) {
    val settings by vm.settings.collectAsState()

    val profileJson = JSONObject().apply {
        put("profile_override", settings.headUnitProfileOverride?.name ?: "null")
        put("respect_safe_area", settings.respectSafeArea)
        put("radio_package", settings.radioPackage)
    }

    val jsonString = profileJson.toString(2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Diagnostics", color = Color.White, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Profile Data:", color = Color.White)
        Text(jsonString, color = Color.Green, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = { onExport(jsonString) }) {
                Text("Export to Log")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onBack) {
                Text("Back")
            }
        }
    }
}
