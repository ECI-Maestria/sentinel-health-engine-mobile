package com.example.tesisv3.ui

import com.example.tesisv3.*
import com.example.tesisv3.iot.*

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

private val SettingsBackground = Color(0xFFF6F7F2)
private val SettingsCard = Color(0xFFFFFFFF)
private val SettingsText = Color(0xFF2E3F35)
private val SettingsMuted = Color(0xFF7B8C81)
private val SettingsChip = Color(0xFF5BCB90)
private val SettingsChipAlt = Color(0xFFE1F2E6)
private val SettingsNav = Color(0xFF5A7A63)

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var transport by remember { mutableStateOf(IotSettings.getTransport(context)) }
    var diagnostic by remember { mutableStateOf(IotSettings.isDiagnosticEnabled(context)) }
    var showRegisterModal by remember { mutableStateOf(IotSettings.isDeviceRegisterModalEnabled(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBackground)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsTopBar(onBack)

        Text("IoT Transport", color = SettingsText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "Choose how the app connects to IoT Hub.",
            color = SettingsMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SettingsCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TransportOption(
                    title = "HTTP (default)",
                    description = "Simpler, best for occasional syncs.",
                    selected = transport == TransportType.HTTP,
                    onClick = {
                        transport = TransportType.HTTP
                        IotSettings.setTransport(context, TransportType.HTTP)
                    }
                )
                TransportOption(
                    title = "MQTT (port 8883)",
                    description = "Direct MQTT, may be blocked on some networks.",
                    selected = transport == TransportType.MQTT,
                    onClick = {
                        transport = TransportType.MQTT
                        IotSettings.setTransport(context, TransportType.MQTT)
                    }
                )
                TransportOption(
                    title = "MQTT over WebSockets",
                    description = "More compatible on mobile/firewalls.",
                    selected = transport == TransportType.MQTT_WS,
                    onClick = {
                        transport = TransportType.MQTT_WS
                        IotSettings.setTransport(context, TransportType.MQTT_WS)
                    }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SettingsCard
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("MQTT Diagnostic Mode", color = SettingsText, fontWeight = FontWeight.Bold)
                    Text(
                        "Show detailed MQTT connection errors in Sync result.",
                        color = SettingsMuted,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = diagnostic,
                    onCheckedChange = {
                        diagnostic = it
                        IotSettings.setDiagnosticEnabled(context, it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SettingsChip,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = SettingsChipAlt
                    )
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SettingsCard
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Device Registration Modal", color = SettingsText, fontWeight = FontWeight.Bold)
                    Text(
                        "Show the register-device result popup after login.",
                        color = SettingsMuted,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = showRegisterModal,
                    onCheckedChange = {
                        showRegisterModal = it
                        IotSettings.setDeviceRegisterModalEnabled(context, it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SettingsChip,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = SettingsChipAlt
                    )
                )
            }
        }
    }
}

@Composable
private fun TransportOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SettingsText, fontWeight = FontWeight.Bold)
            Text(description, color = SettingsMuted, fontSize = 12.sp)
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected) SettingsChip else SettingsChipAlt
            ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(if (selected) "Selected" else "Select", color = SettingsText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SettingsNav)
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = SettingsNav)
        }
        Spacer(Modifier.size(40.dp))
    }
}
