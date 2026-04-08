package com.example.tesisv3

import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import java.nio.charset.StandardCharsets
import androidx.lifecycle.lifecycleScope
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.net.ssl.HttpsURLConnection
import org.json.JSONObject

class DashboardActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DashboardScreen(onBack = { finish() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val payload = String(messageEvent.data, StandardCharsets.UTF_8)
        val path = messageEvent.path ?: ""
        lifecycleScope.launch(Dispatchers.IO) {
            val transport: IotTransport = when (IotSettings.getTransport(this@DashboardActivity)) {
                TransportType.HTTP -> HttpTransport
                TransportType.MQTT, TransportType.MQTT_WS -> MqttTransport
            }
            val deviceUuid = DeviceRegistrationManager.getDeviceUuid(this@DashboardActivity)
            val body = buildWearablePayload(payload, path, deviceUuid)
            transport.sendSyncMessage(BuildConfig.AZURE_IOT_CONNECTION_STRING, body)
        }
    }
}

private fun buildWearablePayload(rawPayload: String, path: String, deviceUuid: String): String {
    val now = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
    val parsed = try {
        JSONObject(rawPayload)
    } catch (_: Exception) {
        null
    }
    if (parsed != null) {
        parsed.put("deviceId", deviceUuid)
        if (!parsed.has("timestamp")) {
            parsed.put("timestamp", now)
        }
        return parsed.toString()
    }
    val heartRate = parsed?.optInt("heartRate", parsed?.optInt("hr", 0) ?: 0) ?: 0
    val spo2 = parsed?.optInt("spO2", parsed?.optInt("spo2", 0) ?: 0) ?: 0
    return buildString {
        append("""{ "deviceId": """").append("\"").append(escapeJson(deviceUuid)).append("\", ")
        append(""""heartRate": """).append(heartRate).append(", ")
        append(""""spO2": """).append(spo2).append(", ")
        append(""""timestamp": """").append("\"").append(now).append("\"")
        if (parsed == null) {
            append(", \"rawPath\": \"").append(escapeJson(path)).append("\"")
            append(", \"rawPayload\": \"").append(escapeJson(rawPayload)).append("\"")
        }
        append(" }")
    }
}

private val DashboardBackground = Color(0xFFF4F5F0)
private val DashboardCard = Color(0xFFDDEFE4)
private val DashboardText = Color(0xFF33483A)
private val DashboardMuted = Color(0xFF7B8D80)
private val DashboardChip = Color(0xFF5BCB90)
private val DashboardBlue = Color(0xFF6EA9E6)
private val DashboardGreen = Color(0xFF62D3A2)
private val DashboardBorder = Color(0xFFD8DDD4)
private val DashboardNav = Color(0xFF58725E)

@Composable
private fun DashboardScreen(onBack: () -> Unit) {
    var selectedNav by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var syncDetails by remember { mutableStateOf<String?>(null) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var transportType by remember { mutableStateOf(IotSettings.getTransport(context)) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var tlsDetails by remember { mutableStateOf<String?>(null) }
    var showTlsDialog by remember { mutableStateOf(false) }
    var mqttDetails by remember { mutableStateOf<String?>(null) }
    var showMqttDialog by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var wearableConnected by remember { mutableStateOf<Boolean?>(null) }
    var registerDetails by remember { mutableStateOf<String?>(null) }
    var showRegisterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        wearableConnected = withContext(Dispatchers.IO) {
            isWearableConnected(context)
        }
    }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) {
            DeviceRegistrationManager.consumeLastResult(context)
        }
        if (result != null) {
            registerDetails = buildString {
                append("Success: ${result.success}\n")
                append("HTTP: ${result.code ?: "N/A"}\n")
                append("Body: ${result.body ?: "N/A"}\n")
                append("Error: ${result.error ?: "N/A"}")
            }
            showRegisterDialog = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Menu", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Button(
                        onClick = {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                            scope.launch { drawerState.close() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DashboardChip),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Settings", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            showChangePassword = true
                            scope.launch { drawerState.close() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DashboardChip),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Change password", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = DashboardBackground,
            bottomBar = {
                AppBottomNav(
                    current = BottomNavDestination.DASHBOARD,
                    modifier = Modifier.navigationBarsPadding(),
                    indicatorColor = DashboardCard,
                    selectedColor = DashboardNav,
                    unselectedColor = DashboardNav.copy(alpha = 0.55f)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DashboardBackground),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    end = 18.dp,
                    bottom = innerPadding.calculateBottomPadding() + 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    DashboardTopBar(
                        onMenu = { scope.launch { drawerState.open() } },
                        wearableConnected = wearableConnected
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Sync", color = DashboardText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Send data to IoT Hub (${transportType.name})",
                                    color = DashboardMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        context.startActivity(Intent(context, SettingsActivity::class.java))
                                        transportType = IotSettings.getTransport(context)
                                    }
                                ) {
                                    Text("Settings")
                                }
                                Button(
                                    onClick = {
                                        if (isSyncing) return@Button
                                        isSyncing = true
                                        scope.launch(Dispatchers.IO) {
                                    val transport: IotTransport = when (IotSettings.getTransport(context)) {
                                        TransportType.HTTP -> HttpTransport
                                        TransportType.MQTT, TransportType.MQTT_WS -> MqttTransport
                                    }
                                            val transportType = IotSettings.getTransport(context)
                                            val payload = buildString {
                                                append("""{ "deviceId": "mobile-gateway-01", "heartRate": 145, "spO2": 88, "timestamp": """")
                                                append(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString())
                                                append("""" }""")
                                            }
                                            val result = transport.sendSyncMessage(
                                                BuildConfig.AZURE_IOT_CONNECTION_STRING,
                                                payload
                                            )
                                            withContext(Dispatchers.Main) {
                                                isSyncing = false
                                        val details = buildString {
                                            append("Success: ${result.success}\n")
                                            append("HTTP: ${result.code ?: "N/A"}\n")
                                            append("Body: ${result.body ?: "N/A"}\n")
                                            append("Error: ${result.error ?: "N/A"}")
                                            if (IotSettings.isDiagnosticEnabled(context) &&
                                                IotSettings.getTransport(context) != TransportType.HTTP
                                            ) {
                                                val diag = MqttTransport.getLastDiagnostic()
                                                if (!diag.isNullOrBlank()) {
                                                    append("\nMQTT: ").append(diag)
                                                }
                                            }
                                        }
                                                syncDetails = details
                                                showSyncDialog = true
                                                if (result.success) {
                                                    Toast.makeText(context, "Sync sent", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Sync failed: ${result.error ?: "Unknown error"}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DashboardChip),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(if (isSyncing) "Sending..." else "Sync", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("TLS Check", color = DashboardText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Verify TLS to azure-devices.net",
                                    color = DashboardMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Button(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        val result = runTlsCheck()
                                        withContext(Dispatchers.Main) {
                                            tlsDetails = result
                                            showTlsDialog = true
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DashboardChip),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Check", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("MQTT Check", color = DashboardText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Test MQTT connection only",
                                    color = DashboardMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Button(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        val result = MqttTransport.testConnection(
                                            BuildConfig.AZURE_IOT_CONNECTION_STRING,
                                            IotSettings.getTransport(context)
                                        )
                                        withContext(Dispatchers.Main) {
                                            mqttDetails = result
                                            showMqttDialog = true
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DashboardChip),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Check", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Steps Today",
                            value = "8,942",
                            detail = "Goal 10,000"
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Sleep",
                            value = "7h 15m",
                            detail = "Quality Good"
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Heart Rate",
                            value = "72 bpm",
                            detail = "Resting"
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Glucose",
                            value = "2405",
                            detail = "+33 % vs last month"
                        )
                    }
                }

                item {
                    OverviewCard()
                }
            }
        }
    }

    if (showSyncDialog && syncDetails != null) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("Sync result") },
            text = { Text(syncDetails ?: "") },
            confirmButton = {
                Button(onClick = { showSyncDialog = false }) { Text("OK") }
            }
        )
    }

    if (showTlsDialog && tlsDetails != null) {
        AlertDialog(
            onDismissRequest = { showTlsDialog = false },
            title = { Text("TLS check") },
            text = { Text(tlsDetails ?: "") },
            confirmButton = {
                Button(onClick = { showTlsDialog = false }) { Text("OK") }
            }
        )
    }

    if (showMqttDialog && mqttDetails != null) {
        AlertDialog(
            onDismissRequest = { showMqttDialog = false },
            title = { Text("MQTT check") },
            text = { Text(mqttDetails ?: "") },
            confirmButton = {
                Button(onClick = { showMqttDialog = false }) { Text("OK") }
            }
        )
    }

    if (showRegisterDialog && registerDetails != null) {
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = { Text("Device registration") },
            text = { Text(registerDetails ?: "") },
            confirmButton = {
                Button(onClick = { showRegisterDialog = false }) { Text("OK") }
            }
        )
    }

    if (showChangePassword) {
        ChangePasswordDialog(onDismiss = { showChangePassword = false })
    }
}

@Composable
private fun DashboardTopBar(onMenu: () -> Unit, wearableConnected: Boolean?) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = DashboardNav)
        }

        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", color = DashboardText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        IconButton(onClick = {
            context.startActivity(Intent(context, NotificationsActivity::class.java))
        }) {
            Box {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = DashboardNav)
                val dotColor = when (wearableConnected) {
                    true -> Color(0xFF4CAF50)
                    false -> Color(0xFFD64545)
                    null -> Color(0xFFB0B8B2)
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

private fun isWearableConnected(context: android.content.Context): Boolean {
    return try {
        val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
        nodes.isNotEmpty()
    } catch (_: Exception) {
        false
    }
}

@Composable
private fun MetricCard(modifier: Modifier = Modifier, title: String, value: String, detail: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, color = DashboardText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(value, color = DashboardText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(detail, color = DashboardMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun OverviewCard() {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, DashboardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activity Overview",
                    color = DashboardText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                FilterChip("Day", true)
                Spacer(Modifier.size(14.dp))
                Text("Week", color = DashboardText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(14.dp))
                Text("Month", color = DashboardText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(18.dp))
            Text("Weekly balance", color = DashboardText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            ChartPlaceholder()
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendDot(DashboardGreen)
                Spacer(Modifier.size(8.dp))
                Text("Steps", color = DashboardMuted, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(22.dp))
                LegendDot(DashboardBlue)
                Spacer(Modifier.size(8.dp))
                Text("Calories", color = DashboardMuted, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) DashboardChip else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else DashboardText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChartPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DashboardBorder)
            )
            Spacer(Modifier.height(34.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            ChartBar(0.38f, DashboardGreen, Modifier.weight(1f))
            ChartBar(0.48f, DashboardGreen, Modifier.weight(1f))
            ChartBar(0.58f, DashboardGreen, Modifier.weight(1f))
            ChartBar(0.68f, DashboardBlue, Modifier.weight(1f))
            ChartBar(0.82f, DashboardBlue, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ChartBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(160.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((160 * progress).dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(color.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

private fun runTlsCheck(): String {
    return try {
        val host = BuildConfig.AZURE_IOT_HOST_NAME.ifBlank { "azure-devices.net" }
        val url = URL("https://$host")
        val conn = (url.openConnection() as HttpsURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
        }
        conn.connect()
        val code = conn.responseCode
        val cipher = conn.cipherSuite ?: "N/A"
        val protocol = conn.url.protocol ?: "N/A"
        conn.disconnect()
        "Success\nHTTP: $code\nProtocol: $protocol\nCipher: $cipher"
    } catch (e: Exception) {
        val cause = e.cause?.let { " | Cause: ${it.javaClass.simpleName}: ${it.message}" } ?: ""
        "Failed: ${e.javaClass.simpleName}: ${e.message}$cause"
    }
}

@Composable
private fun ChangePasswordDialog(onDismiss: () -> Unit) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var showMessage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it; showMessage = false },
                    label = { Text("Old password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; showMessage = false },
                    label = { Text("New password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (showMessage) {
                    Text(
                        text = message,
                        color = if (message.startsWith("Success")) Color(0xFF2E7D32) else Color(0xFFD35C55),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    if (oldPassword.isBlank() || newPassword.isBlank()) {
                        message = "Please fill all fields"
                        showMessage = true
                        return@Button
                    }
                    isSubmitting = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            changePassword(oldPassword.trim(), newPassword.trim())
                        }
                        isSubmitting = false
                        if (result.success) {
                            message = "Success: password updated"
                            showMessage = true
                            onDismiss()
                        } else {
                            message = result.message
                            showMessage = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DashboardChip)
            ) {
                Text(if (isSubmitting) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private data class ChangePasswordResult(val success: Boolean, val message: String)

private fun changePassword(oldPassword: String, newPassword: String): ChangePasswordResult {
    val url = URL("https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/auth/change-password")
    val payload = """{"oldPassword":"${escapeJson(oldPassword)}","newPassword":"${escapeJson(newPassword)}"}"""
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
        }
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        when {
            code in 200..299 -> ChangePasswordResult(true, "")
            code == 401 || code == 403 -> ChangePasswordResult(false, "Unauthorized")
            else -> ChangePasswordResult(false, body.ifBlank { "Change failed (HTTP $code)" })
        }
    } catch (e: Exception) {
        ChangePasswordResult(false, e.message ?: "Network error")
    }
}

private fun readStream(stream: java.io.InputStream?): String {
    if (stream == null) return ""
    return BufferedReader(InputStreamReader(stream)).use { it.readText() }
}

private fun escapeJson(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}
