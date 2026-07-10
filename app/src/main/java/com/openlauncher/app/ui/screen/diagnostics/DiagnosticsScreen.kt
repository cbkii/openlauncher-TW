package com.openlauncher.app.ui.screen.diagnostics

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.viewmodel.LauncherViewModel
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun DiagnosticsScreen(
    vm: LauncherViewModel,
    onBack: () -> Unit,
    onExport: (String) -> Unit
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val settings by vm.settings.collectAsState()
    val detectedProfile by vm.detectedHeadUnitProfile.collectAsState()
    val effectiveProfile by vm.effectiveHeadUnitProfile.collectAsState()
    val confidence by vm.headUnitDetectionConfidence.collectAsState()
    val evidence by vm.headUnitEvidence.collectAsState()
    val targets by vm.launchTargetStatuses.collectAsState()
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()

    val defaultHomePackage = remember {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager.resolveActivity(homeIntent, 0)?.activityInfo?.packageName
    }
    val notificationAccess = remember {
        Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )?.contains(context.packageName) == true
    }
    val overlayAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
        Settings.canDrawOverlays(context)

    val jsonString = JSONObject().apply {
        put("detected_profile", detectedProfile.name)
        put("effective_profile", effectiveProfile.name)
        put("confidence", confidence.name)
        put("profile_override", settings.headUnitProfileOverride?.name ?: JSONObject.NULL)
        put("respect_safe_area", settings.respectSafeArea)
        put("default_home_package", defaultHomePackage ?: JSONObject.NULL)
        put("is_default_home", defaultHomePackage == context.packageName)
        put("notification_listener_enabled", notificationAccess)
        put("overlay_allowed", overlayAllowed)
        put("safe_insets_dp", JSONObject().apply {
            put("left", safePadding.calculateLeftPadding(layoutDirection).value)
            put("top", safePadding.calculateTopPadding().value)
            put("right", safePadding.calculateRightPadding(layoutDirection).value)
            put("bottom", safePadding.calculateBottomPadding().value)
        })
        put("evidence", JSONArray().apply {
            evidence.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("present", item.isPresent)
                    put("details", item.details ?: JSONObject.NULL)
                })
            }
        })
        put("launch_targets", JSONArray().apply {
            targets.forEach { status ->
                put(JSONObject().apply {
                    put("id", status.target.id)
                    put("label", status.target.label)
                    put("package", status.target.packageName ?: JSONObject.NULL)
                    put("component", status.target.className ?: JSONObject.NULL)
                    put("uri", status.target.uri ?: JSONObject.NULL)
                    put("available", status.isAvailable)
                })
            }
        })
    }.toString(2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Head Unit Diagnostics", color = Color.White, fontSize = 24.sp)
        Text(
            "Detected: ${detectedProfile.name} (${confidence.name})\n" +
                "Effective: ${effectiveProfile.name}\n" +
                "Default HOME: ${defaultHomePackage ?: "none"}",
            color = Color.White
        )

        Text("Resolved launch targets", color = Color.White, fontSize = 18.sp)
        targets.forEach { status ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${status.target.label}: ${if (status.isAvailable) "available" else "unavailable"}",
                    color = if (status.isAvailable) Color.Green else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    enabled = status.isAvailable,
                    onClick = { vm.launchHeadUnitTarget(status.target.id) }
                ) {
                    Text("Open")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(jsonString, color = Color.Green, fontSize = 11.sp)

        Row {
            Button(onClick = { onExport(jsonString) }) {
                Text("Share JSON")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onBack) {
                Text("Back")
            }
        }
    }
}
