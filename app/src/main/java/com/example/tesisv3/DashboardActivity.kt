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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : ComponentActivity() {
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
                    DashboardTopBar(onMenu = { scope.launch { drawerState.open() } })
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
                                                TransportType.MQTT -> MqttTransport
                                                TransportType.HTTP -> HttpTransport
                                            }
                                            val result = transport.sendSyncMessage(
                                                BuildConfig.AZURE_IOT_CONNECTION_STRING,
                                                """{ "action": "sync" }"""
                                            )
                                            withContext(Dispatchers.Main) {
                                                isSyncing = false
                                                val details = buildString {
                                                    append("Success: ${result.success}\n")
                                                    append("HTTP: ${result.code ?: "N/A"}\n")
                                                    append("Body: ${result.body ?: "N/A"}\n")
                                                    append("Error: ${result.error ?: "N/A"}")
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
}

@Composable
private fun DashboardTopBar(onMenu: () -> Unit) {
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
            Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = DashboardNav)
        }
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
