package com.example.tesisv3

import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject


private data class DashVitals(
    val heartRate: Int?,
    val spO2: Int?,
    val bpSystolic: Int?,
    val bpDiastolic: Int?,
    val timestampLabel: String = "Ahora"
)

private data class DashMedication(
    val id: String,
    val name: String,
    val dosage: String,
    val scheduledTime: String,
    val active: Boolean,
    val isDue: Boolean
)

private data class DashAppointment(
    val id: String,
    val title: String,
    val scheduledAtMillis: Long,
    val location: String,
    val notes: String
)

private data class DashReminder(
    val id: String,
    val title: String,
    val timeLabel: String,
    val recurrenceLabel: String
)

private data class VitalHistoryPoint(
    val timestamp: Long,
    val heartRate: Int?,
    val spO2: Int?
)

private data class HistoryDebugInfo(
    val requestUrl: String,
    val httpCode: Int,
    val rawResponse: String,
    val exception: String?,
    val points: List<VitalHistoryPoint>
)

class DashboardActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private val liveVitals = mutableStateOf<DashVitals?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DashboardScreen(
                    onBack = { finish() },
                    liveVitals = liveVitals
                )
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

        try {
            val parsed = JSONObject(payload)
            val hr = parsed.optInt("heartRate", parsed.optInt("hr", 0))
            val spo2 = parsed.optInt("spO2", parsed.optInt("spo2", 0))
            if (hr > 0 || spo2 > 0) {
                val current = liveVitals.value
                liveVitals.value = DashVitals(
                    heartRate = if (hr > 0) hr else current?.heartRate,
                    spO2 = if (spo2 > 0) spo2 else current?.spO2,
                    bpSystolic = current?.bpSystolic,
                    bpDiastolic = current?.bpDiastolic,
                    timestampLabel = "Ahora"
                )
            }
        } catch (_: Exception) {}

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


private val DashBackground = Color(0xFFF4F5F0)
private val DashGreen = Color(0xFF2D6A4F)
private val DashGreenLight = Color(0xFF5BCB90)
private val DashGreenChip = Color(0xFFDDEFE4)
private val DashText = Color(0xFF1A2E25)
private val DashMuted = Color(0xFF7B8D80)
private val DashCard = Color.White
private val DashNav = Color(0xFF58725E)
private val DashDue = Color(0xFFE07B3A)
private val DashDueBg = Color(0xFFFFF0E0)
private val DashDivider = Color(0xFFEEF0EC)


@Composable
private fun DashboardScreen(onBack: () -> Unit, liveVitals: MutableState<DashVitals?>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var vitals by liveVitals
    var medications by remember { mutableStateOf<List<DashMedication>>(emptyList()) }
    var appointments by remember { mutableStateOf<List<DashAppointment>>(emptyList()) }
    var reminders by remember { mutableStateOf<List<DashReminder>>(emptyList()) }
    var vitalsHistory by remember { mutableStateOf<List<VitalHistoryPoint>>(emptyList()) }
    var historyLoading by remember { mutableStateOf(true) }
    var historyDebug by remember { mutableStateOf<HistoryDebugInfo?>(null) }
    var showHistoryDebug by remember { mutableStateOf(false) }
    var wearableConnected by remember { mutableStateOf<Boolean?>(null) }
    var steps by remember { mutableStateOf<Int?>(null) }
    var initialSteps by remember { mutableStateOf<Int?>(null) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Permission granted, sensor will automatically start through DisposableEffect
            }
        }
    )

    LaunchedEffect(Unit) {
        wearableConnected = withContext(Dispatchers.IO) { isWearableConnected(context) }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        val stepListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val currentSteps = it.values[0].toInt()
                    steps = currentSteps
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (stepSensor != null) {
            sensorManager.registerListener(
                stepListener,
                stepSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        onDispose {
            sensorManager.unregisterListener(stepListener)
        }
    }

    LaunchedEffect(Unit) {
        val patientId = PatientSession.patientId
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        if (vitals == null) {
            val apiVitals = withContext(Dispatchers.IO) { fetchApiVitals(patientId) }
            if (vitals == null) vitals = apiVitals
        }

        medications = withContext(Dispatchers.IO) { fetchDashMedications(patientId) }
        appointments = withContext(Dispatchers.IO) { fetchDashAppointments(patientId, today) }
        reminders = withContext(Dispatchers.IO) { fetchDashReminders(patientId, today) }
        val histResult = withContext(Dispatchers.IO) { fetchVitalsHistory(patientId) }
        vitalsHistory = histResult.points
        historyDebug = histResult
        historyLoading = false
    }

    val firstName = PatientSession.currentUser?.firstName
        ?: PatientSession.currentUser?.fullName?.split(" ")?.firstOrNull()
        ?: "Paciente"
    val lastName = PatientSession.currentUser?.lastName
        ?: PatientSession.currentUser?.fullName?.split(" ")?.getOrNull(1)
        ?: ""
    val displayName = if (lastName.isNotBlank()) "$firstName $lastName" else firstName
    val initials = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}"
        .uppercase().ifBlank { "P" }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                RoleDrawerContent(
                    role = PatientSession.currentUser?.role,
                    current = DrawerDestination.DASHBOARD,
                    onItemClick = { destination ->
                        handleDrawerNavigation(context, destination)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = DashBackground,
            bottomBar = {
                AppBottomNav(
                    current = BottomNavDestination.DASHBOARD,
                    modifier = Modifier.navigationBarsPadding(),
                    indicatorColor = DashGreenChip,
                    selectedColor = DashNav,
                    unselectedColor = DashNav.copy(alpha = 0.55f)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DashBackground),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    end = 18.dp,
                    bottom = innerPadding.calculateBottomPadding() + 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    DashTopBar(
                        displayName = displayName,
                        initials = initials,
                        wearableConnected = wearableConnected,
                        onMenu = { scope.launch { drawerState.open() } }
                    )
                }
                item { VitalsCard(vitals = vitals, currentSteps = steps) }
                item {
                    VitalsHistoryCard(
                        points = vitalsHistory,
                        isLoading = historyLoading,
                        onDebugClick = { showHistoryDebug = true }
                    )
                }
                item {
                    MedicationsSection(
                        medications = medications.take(3),
                        onSeeAll = { context.startActivity(Intent(context, CareActivity::class.java)) }
                    )
                }
                item {
                    AppointmentsSection(
                        appointments = appointments.take(2),
                        onSeeAll = { context.startActivity(Intent(context, CalendarActivity::class.java)) }
                    )
                }
                item {
                    RemindersSection(
                        reminders = reminders.take(3),
                        onSeeAll = { context.startActivity(Intent(context, CalendarActivity::class.java)) }
                    )
                }
            }
        }
    }

    if (showHistoryDebug) {
        val info = historyDebug
        AlertDialog(
            onDismissRequest = { showHistoryDebug = false },
            title = { Text("Debug — Historial de Vitales", fontWeight = FontWeight.Bold) },
            text = {
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (info == null) {
                        Text("Aún sin datos. Espera a que termine la carga.", color = DashMuted)
                    } else {
                        Text("REQUEST", color = DashGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            info.requestUrl,
                            fontSize = 11.sp,
                            color = DashText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DashGreenChip, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                        Text("HTTP STATUS", color = DashGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val codeColor = when {
                            info.httpCode in 200..299 -> Color(0xFF2E7D32)
                            info.httpCode == -1 -> DashMuted
                            else -> Color(0xFFD32F2F)
                        }
                        Text(
                            if (info.httpCode == -1) "Excepción antes de conectar" else info.httpCode.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = codeColor
                        )
                        if (!info.exception.isNullOrBlank()) {
                            Text("EXCEPCIÓN", color = Color(0xFFD32F2F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                info.exception,
                                fontSize = 11.sp,
                                color = Color(0xFFD32F2F),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            )
                        }
                        Text("RESPONSE BODY", color = DashGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val preview = if (info.rawResponse.isBlank()) "(vacío)"
                        else info.rawResponse.take(2000)
                        Text(
                            preview,
                            fontSize = 10.sp,
                            color = DashText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                        Text("PUNTOS PARSEADOS", color = DashGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${info.points.size} puntos  (HR: ${info.points.count { it.heartRate != null }}, SpO2: ${info.points.count { it.spO2 != null }})",
                            fontSize = 12.sp,
                            color = DashText
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHistoryDebug = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DashGreenLight)
                ) { Text("Cerrar") }
            }
        )
    }
}


@Composable
private fun DashTopBar(
    displayName: String,
    initials: String,
    wearableConnected: Boolean?,
    onMenu: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = DashNav)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        ) {
            Text(text = "Hola,", color = DashMuted, fontSize = 14.sp)
            Text(
                text = "$displayName 👋",
                color = DashText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (PatientSession.currentUser?.role?.equals("PATIENT", ignoreCase = true) == true) {
            WatchStatusIcon(
                wearableConnected = wearableConnected,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 2.dp)
            )
        }
        IconButton(onClick = {
            context.startActivity(Intent(context, NotificationsActivity::class.java))
        }) {
            Icon(
                Icons.Outlined.NotificationsNone,
                contentDescription = "Notificaciones",
                tint = DashNav
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(DashGreenLight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VitalsCard(vitals: DashVitals?, currentSteps: Int?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DashGreen)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Signos Vitales — Ahora",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = vitals?.timestampLabel ?: "—",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VitalItem(
                    modifier = Modifier.weight(1f),
                    label = "❤️ FC",
                    value = vitals?.heartRate?.toString() ?: "—",
                    unit = "bpm",
                    highlight = false
                )
                val bpHigh = (vitals?.bpSystolic ?: 0) > 140
                VitalItem(
                    modifier = Modifier.weight(1f),
                    label = "📌 Pasos",
                    value = currentSteps?.toString() ?: "—",
                    unit = "",
                    highlight = false
                )
                VitalItem(
                    modifier = Modifier.weight(1f),
                    label = "🔥 O2",
                    value = vitals?.spO2?.toString() ?: "—",
                    unit = "%",
                    highlight = false
                )
            }
        }
    }
}

@Composable
private fun VitalItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    highlight: Boolean
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.18f))
            .then(
                if (highlight) Modifier.border(1.5.dp, Color(0xFFE05A5A), shape)
                else Modifier
            )
            .padding(12.dp)
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        if (unit.isNotBlank()) {
            Text(text = unit, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
        }
    }
}


@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, color = DashText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            text = actionLabel,
            color = DashGreenLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onAction)
        )
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = DashCard) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(message, color = DashMuted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun MedicationsSection(medications: List<DashMedication>, onSeeAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("💊 Medicamentos de Hoy", "Ver todos", onSeeAll)
        if (medications.isEmpty()) {
            EmptyStateCard("Sin medicamentos para hoy")
        } else {
            Surface(shape = RoundedCornerShape(16.dp), color = DashCard) {
                Column {
                    medications.forEachIndexed { idx, med ->
                        MedicationRow(med)
                        if (idx < medications.lastIndex) {
                            HorizontalDivider(color = DashDivider, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicationRow(med: DashMedication) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DashGreenChip),
            contentAlignment = Alignment.Center
        ) {
            Text("💊", fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                med.name,
                color = DashText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            val subtitle = buildString {
                if (med.dosage.isNotBlank()) append(med.dosage)
                if (med.scheduledTime.isNotBlank()) {
                    if (length > 0) append(" · ")
                    append(med.scheduledTime)
                }
            }
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = DashMuted, fontSize = 13.sp)
            }
        }
        if (med.isDue) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DashDueBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Due", color = DashDue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DashGreen)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Activo", color = DashDue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AppointmentsSection(appointments: List<DashAppointment>, onSeeAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("📅 Próximas Citas", "ver todos", onSeeAll)
        if (appointments.isEmpty()) {
            EmptyStateCard("Sin citas para hoy")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                appointments.forEach { appointment ->
                    AppointmentCard(appointment)
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(appointment: DashAppointment) {
    val timeLabel = formatAppointmentTimeLabel(appointment.scheduledAtMillis)
    Surface(shape = RoundedCornerShape(14.dp), color = DashCard) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                    .background(DashGreenLight)
            )
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    text = timeLabel,
                    color = DashMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = appointment.title,
                    color = DashText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                val subtitle = listOfNotNull(
                    appointment.notes.ifBlank { null },
                    appointment.location.ifBlank { null }
                ).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = DashMuted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun RemindersSection(reminders: List<DashReminder>, onSeeAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("🔔 Recordatorios", "Ver todos", onSeeAll)
        if (reminders.isEmpty()) {
            EmptyStateCard("Sin recordatorios para hoy")
        } else {
            Surface(shape = RoundedCornerShape(16.dp), color = DashCard) {
                Column {
                    reminders.forEachIndexed { idx, reminder ->
                        ReminderRow(reminder)
                        if (idx < reminders.lastIndex) {
                            HorizontalDivider(color = DashDivider, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(reminder: DashReminder) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            reminder.title,
            color = DashText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        val subtitle = buildString {
            append(reminder.timeLabel)
            if (reminder.recurrenceLabel.isNotBlank()) {
                append(" · ")
                append(reminder.recurrenceLabel)
            }
        }
        Text(subtitle, color = DashMuted, fontSize = 13.sp)
    }
}


private val ChartHrColor = Color(0xFF5BCB90)      // green  → FC
private val ChartSpo2Color = Color(0xFF4A90D9)    // blue   → SpO2
private val ChartGridColor = Color(0xFFEAEDEA)

@Composable
private fun VitalsHistoryCard(
    points: List<VitalHistoryPoint>,
    isLoading: Boolean,
    onDebugClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DashCard
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "📈 Ritmo Cardíaco y SpO2",
                        color = DashText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Últimos 30 días", color = DashMuted, fontSize = 12.sp)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegendDot(ChartHrColor, "FC")
                    ChartLegendDot(ChartSpo2Color, "SpO2")
                    // Debug button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DashGreenChip)
                            .clickable(onClick = onDebugClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("🔍", fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            when {
                isLoading -> ChartPlaceholderBox("Cargando historial...")
                points.isEmpty() -> ChartPlaceholderBox("Sin datos disponibles")
                else -> VitalsLineChart(points)
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(label, color = DashMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ChartPlaceholderBox(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(msg, color = DashMuted, fontSize = 13.sp)
    }
}

@Composable
private fun VitalsLineChart(rawPoints: List<VitalHistoryPoint>) {
    // Group by calendar day → average per day
    val grouped = rawPoints
        .groupBy {
            Instant.ofEpochMilli(it.timestamp)
                .atZone(ZoneId.systemDefault()).toLocalDate()
        }
        .toSortedMap()

    data class DayPoint(val label: String, val hr: Int?, val spo2: Int?)

    val daily = grouped.map { (date, pts) ->
        val avgHr = pts.mapNotNull { it.heartRate }
            .let { if (it.isEmpty()) null else it.average().toInt() }
        val avgSpo2 = pts.mapNotNull { it.spO2 }
            .let { if (it.isEmpty()) null else it.average().toInt() }
        DayPoint("${date.dayOfMonth}/${date.monthValue}", avgHr, avgSpo2)
    }

    val n = daily.size.coerceAtLeast(2)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        val padL = 46f
        val padR = 46f
        val padT = 10f
        val padB = 26f
        val cW = size.width - padL - padR
        val cH = size.height - padT - padB

        val hrVals = daily.mapNotNull { it.hr }
        val hrMin = if (hrVals.isEmpty()) 40 else (hrVals.min() - 10).coerceAtLeast(30)
        val hrMax = if (hrVals.isEmpty()) 180 else (hrVals.max() + 10)
        val hrRange = (hrMax - hrMin).coerceAtLeast(1).toFloat()

        val sVals = daily.mapNotNull { it.spo2 }
        val sMin = if (sVals.isEmpty()) 85 else (sVals.min() - 2).coerceAtLeast(80)
        val sMax = if (sVals.isEmpty()) 100 else (sVals.max() + 1).coerceAtMost(102)
        val sRange = (sMax - sMin).coerceAtLeast(1).toFloat()

        fun xOf(i: Int): Float = padL + (i.toFloat() / (n - 1).coerceAtLeast(1)) * cW
        fun yOfHr(v: Int): Float = padT + cH - ((v - hrMin) / hrRange) * cH
        fun yOfS(v: Int): Float = padT + cH - ((v - sMin) / sRange) * cH

        drawRect(
            color = Color(0xFFF7FAF8),
            topLeft = Offset(padL, padT),
            size = GeoSize(cW, cH)
        )

        repeat(5) { j ->
            val y = padT + cH * j / 4f
            drawLine(
                color = ChartGridColor,
                start = Offset(padL, y),
                end = Offset(padL + cW, y),
                strokeWidth = 1f
            )
        }

        val hrFill = Path()
        var first = true
        daily.forEachIndexed { i, dp ->
            dp.hr?.let { v ->
                val x = xOf(i); val y = yOfHr(v)
                if (first) { hrFill.moveTo(x, padT + cH); hrFill.lineTo(x, y); first = false }
                else hrFill.lineTo(x, y)
            }
        }
        val lastHrIdx = daily.indexOfLast { it.hr != null }
        if (!first && lastHrIdx >= 0) {
            hrFill.lineTo(xOf(lastHrIdx), padT + cH)
            hrFill.close()
        }
        drawPath(hrFill, color = ChartHrColor.copy(alpha = 0.10f))

        val sFill = Path()
        var sFirst = true
        daily.forEachIndexed { i, dp ->
            dp.spo2?.let { v ->
                val x = xOf(i); val y = yOfS(v)
                if (sFirst) { sFill.moveTo(x, padT + cH); sFill.lineTo(x, y); sFirst = false }
                else sFill.lineTo(x, y)
            }
        }
        val lastSIdx = daily.indexOfLast { it.spo2 != null }
        if (!sFirst && lastSIdx >= 0) {
            sFill.lineTo(xOf(lastSIdx), padT + cH)
            sFill.close()
        }
        drawPath(sFill, color = ChartSpo2Color.copy(alpha = 0.10f))

        val hrLine = Path()
        var hrFirst = true
        daily.forEachIndexed { i, dp ->
            dp.hr?.let { v ->
                val x = xOf(i); val y = yOfHr(v)
                if (hrFirst) { hrLine.moveTo(x, y); hrFirst = false } else hrLine.lineTo(x, y)
            }
        }
        drawPath(
            hrLine, color = ChartHrColor,
            style = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val sLine = Path()
        var sLineFirst = true
        daily.forEachIndexed { i, dp ->
            dp.spo2?.let { v ->
                val x = xOf(i); val y = yOfS(v)
                if (sLineFirst) { sLine.moveTo(x, y); sLineFirst = false } else sLine.lineTo(x, y)
            }
        }
        drawPath(
            sLine, color = ChartSpo2Color,
            style = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        daily.forEachIndexed { i, dp ->
            dp.hr?.let {
                drawCircle(ChartHrColor, radius = 2.8f * density, center = Offset(xOf(i), yOfHr(it)))
                drawCircle(Color.White, radius = 1.4f * density, center = Offset(xOf(i), yOfHr(it)))
            }
            dp.spo2?.let {
                drawCircle(ChartSpo2Color, radius = 2.8f * density, center = Offset(xOf(i), yOfS(it)))
                drawCircle(Color.White, radius = 1.4f * density, center = Offset(xOf(i), yOfS(it)))
            }
        }

        val basePaint = android.graphics.Paint().apply {
            textSize = 23f
            isAntiAlias = true
        }

        val hrPaint = android.graphics.Paint(basePaint).apply {
            color = android.graphics.Color.parseColor("#5BCB90")
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        for (j in 0..3) {
            val v = hrMin + ((hrMax - hrMin) * (4 - j) / 4)
            val y = padT + cH * j / 4f + 8f
            drawContext.canvas.nativeCanvas.drawText(v.toString(), padL - 5f, y, hrPaint)
        }

        val sPaint = android.graphics.Paint(basePaint).apply {
            color = android.graphics.Color.parseColor("#4A90D9")
            textAlign = android.graphics.Paint.Align.LEFT
        }
        for (j in 0..3) {
            val v = sMin + ((sMax - sMin) * (4 - j) / 4)
            val y = padT + cH * j / 4f + 8f
            drawContext.canvas.nativeCanvas.drawText(v.toInt().toString(), padL + cW + 5f, y, sPaint)
        }

        val xPaint = android.graphics.Paint(basePaint).apply {
            color = android.graphics.Color.parseColor("#7B8D80")
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val labelEvery = maxOf(1, n / 5)
        daily.forEachIndexed { i, dp ->
            if (i % labelEvery == 0 || i == n - 1) {
                drawContext.canvas.nativeCanvas.drawText(
                    dp.label, xOf(i), padT + cH + padB - 4f, xPaint
                )
            }
        }
    }
}


private fun fetchApiVitals(patientId: String): DashVitals? {
    if (patientId.isBlank()) return null
    val url = URL(
        "https://analytics-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io" +
                "/v1/patients/$patientId/vitals/latest"
    )
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Content-Type", "application/json")
            PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            connectTimeout = 10000
            readTimeout = 10000
        }
        val code = conn.responseCode
        val body = readDashStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        if (code !in 200..299 || body.isBlank()) return null
        val json = JSONObject(body)
        val hr = json.optInt("heartRate").takeIf { it > 0 }
            ?: json.optInt("hr").takeIf { it > 0 }
        val spo2 = json.optInt("oxygenSaturation").takeIf { it > 0 }
            ?: json.optInt("spO2").takeIf { it > 0 }
            ?: json.optInt("spo2").takeIf { it > 0 }
        val bpSys = json.optInt("bloodPressureSystolic").takeIf { it > 0 }
            ?: json.optInt("systolic").takeIf { it > 0 }
        val bpDia = json.optInt("bloodPressureDiastolic").takeIf { it > 0 }
            ?: json.optInt("diastolic").takeIf { it > 0 }
        val tsRaw = json.optString("measuredAt").ifBlank { json.optString("timestamp") }
        DashVitals(
            heartRate = hr,
            spO2 = spo2,
            bpSystolic = bpSys,
            bpDiastolic = bpDia,
            timestampLabel = formatVitalsTimestamp(tsRaw)
        )
    } catch (_: Exception) {
        null
    }
}

private fun fetchDashMedications(patientId: String): List<DashMedication> {
    if (patientId.isBlank()) return emptyList()
    val url = URL(
        "https://calendar-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io" +
                "/v1/patients/$patientId/medications?active=true"
    )
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Content-Type", "application/json")
            PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            connectTimeout = 10000
            readTimeout = 10000
        }
        val code = conn.responseCode
        val body = readDashStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        if (code !in 200..299 || body.isBlank()) return emptyList()
        val json = JSONObject(body)
        val arr = json.optJSONArray("medications") ?: JSONArray()
        val today = LocalDate.now()
        val list = mutableListOf<DashMedication>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val startDate = item.optString("startDate")
            val endDate = item.optString("endDate")
            if (!isMedForToday(startDate, endDate, today)) continue
            val timesArr = item.optJSONArray("scheduledTimes") ?: JSONArray()
            val firstTime = if (timesArr.length() > 0) timesArr.optString(0) else ""
            list.add(
                DashMedication(
                    id = item.optString("id"),
                    name = item.optString("name"),
                    dosage = item.optString("dosage"),
                    scheduledTime = formatMedTime(firstTime),
                    active = item.optBoolean("active", true),
                    isDue = firstTime.isNotBlank() && isTimeDue(firstTime)
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

private fun fetchDashAppointments(patientId: String, date: String): List<DashAppointment> {
    if (patientId.isBlank()) return emptyList()
    val url = URL(
        "https://calendar-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io" +
                "/v1/patients/$patientId/appointments?period=day&date=$date"
    )
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Content-Type", "application/json")
            PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            connectTimeout = 10000
            readTimeout = 10000
        }
        val code = conn.responseCode
        val body = readDashStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        if (code !in 200..299 || body.isBlank()) return emptyList()
        val json = JSONObject(body)
        val arr = json.optJSONArray("appointments") ?: return emptyList()
        val list = mutableListOf<DashAppointment>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val scheduledAt = item.optString("scheduledAt")
            val millis = try { Instant.parse(scheduledAt).toEpochMilli() } catch (_: Exception) { 0L }
            list.add(
                DashAppointment(
                    id = item.optString("id"),
                    title = item.optString("title"),
                    scheduledAtMillis = millis,
                    location = item.optString("location"),
                    notes = item.optString("notes")
                )
            )
        }
        list.sortedBy { it.scheduledAtMillis }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun fetchDashReminders(patientId: String, date: String): List<DashReminder> {
    if (patientId.isBlank()) return emptyList()
    val url = URL(
        "https://calendar-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io" +
                "/v1/patients/$patientId/reminders?period=day&date=$date"
    )
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Content-Type", "application/json")
            PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            connectTimeout = 10000
            readTimeout = 10000
        }
        val code = conn.responseCode
        val body = readDashStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        if (code !in 200..299 || body.isBlank()) return emptyList()
        val json = JSONObject(body)
        val arr = json.optJSONArray("reminders") ?: return emptyList()
        val list = mutableListOf<DashReminder>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val scheduledAt = item.optString("scheduledAt").ifBlank { item.optString("time") }
            val recurrence = item.optString("recurrence").ifBlank { item.optString("frequency") }
            list.add(
                DashReminder(
                    id = item.optString("id"),
                    title = item.optString("title"),
                    timeLabel = buildReminderTimeLabel(scheduledAt),
                    recurrenceLabel = formatRecurrence(recurrence)
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

private fun fetchVitalsHistory(patientId: String): HistoryDebugInfo {
    val to = LocalDate.now()
    val from = to.minusDays(29)
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val urlStr = "https://analytics-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io" +
            "/v1/patients/$patientId/vitals/history" +
            "?from=${from.format(fmt)}&to=${to.format(fmt)}"

    if (patientId.isBlank()) {
        return HistoryDebugInfo(urlStr, -1, "", "patientId vacío", emptyList())
    }

    return try {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Content-Type", "application/json")
            PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            connectTimeout = 12000
            readTimeout = 12000
        }
        val code = conn.responseCode
        val body = readDashStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()

        if (code !in 200..299 || body.isBlank()) {
            return HistoryDebugInfo(urlStr, code, body, null, emptyList())
        }

        val json = JSONObject(body)
        val arr = json.optJSONArray("readings")
            ?: json.optJSONArray("history")
            ?: json.optJSONArray("vitals")
            ?: json.optJSONArray("data")
            ?: return HistoryDebugInfo(
                urlStr, code, body,
                "No se encontró array en claves: readings / history / vitals / data. Keys presentes: ${json.keys().asSequence().joinToString()}",
                emptyList()
            )

        val list = mutableListOf<VitalHistoryPoint>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val tsStr = item.optString("measuredAt").ifBlank {
                item.optString("timestamp").ifBlank { item.optString("date") }
            }
            val ts = try { Instant.parse(tsStr).toEpochMilli() } catch (_: Exception) { continue }
            // heartRate comes as Int
            val hr = item.optInt("heartRate").takeIf { it > 0 }
                ?: item.optInt("hr").takeIf { it > 0 }
            // spO2 can be Double (e.g. 98.5) — read as Double then round
            val spo2Raw = item.opt("spO2") ?: item.opt("oxygenSaturation") ?: item.opt("spo2")
            val spo2 = when (spo2Raw) {
                is Double -> spo2Raw.toInt().takeIf { it > 0 }
                is Int    -> spo2Raw.takeIf { it > 0 }
                is Float  -> spo2Raw.toInt().takeIf { it > 0 }
                else      -> null
            }
            if (hr != null || spo2 != null) {
                list.add(VitalHistoryPoint(ts, hr, spo2))
            }
        }
        HistoryDebugInfo(urlStr, code, body, null, list.sortedBy { it.timestamp })
    } catch (e: Exception) {
        HistoryDebugInfo(urlStr, -1, "", "${e.javaClass.simpleName}: ${e.message}", emptyList())
    }
}

private fun readDashStream(stream: java.io.InputStream?): String {
    if (stream == null) return ""
    return BufferedReader(InputStreamReader(stream)).use { it.readText() }
}

private fun isMedForToday(startDate: String, endDate: String, today: LocalDate): Boolean {
    return try {
        val start = LocalDate.parse(startDate.take(10))
        val end = if (endDate.isBlank() || endDate == "null") null
        else LocalDate.parse(endDate.take(10))
        !today.isBefore(start) && (end == null || !today.isAfter(end))
    } catch (_: Exception) {
        true
    }
}

private fun isTimeDue(timeStr: String): Boolean {
    if (timeStr.isBlank()) return false
    return try {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        if (timeStr.contains("T")) {
            val localTime = Instant.parse(timeStr).atZone(ZoneId.systemDefault()).toLocalTime()
            return localTime.hour < currentHour ||
                    (localTime.hour == currentHour && localTime.minute <= currentMinute)
        }
        val parts = timeStr.trim().split(":")
        val hour = parts[0].toInt()
        val minute = parts.getOrElse(1) { "0" }.toInt()
        hour < currentHour || (hour == currentHour && minute <= currentMinute)
    } catch (_: Exception) {
        false
    }
}

private fun formatMedTime(timeStr: String): String {
    if (timeStr.isBlank()) return ""
    return try {
        if (timeStr.contains("T")) {
            val localTime = Instant.parse(timeStr).atZone(ZoneId.systemDefault()).toLocalTime()
            val h12 = if (localTime.hour % 12 == 0) 12 else localTime.hour % 12
            val amPm = if (localTime.hour < 12) "AM" else "PM"
            return String.format(Locale.US, "%d:%02d %s", h12, localTime.minute, amPm)
        }
        val parts = timeStr.trim().split(":")
        val hour = parts[0].toInt()
        val minute = parts.getOrElse(1) { "0" }.toInt()
        val h12 = if (hour % 12 == 0) 12 else hour % 12
        val amPm = if (hour < 12) "AM" else "PM"
        String.format(Locale.US, "%d:%02d %s", h12, minute, amPm)
    } catch (_: Exception) {
        timeStr
    }
}

private fun buildReminderTimeLabel(scheduledAt: String): String {
    if (scheduledAt.isBlank()) return "Hoy"
    return try {
        val localTime = Instant.parse(scheduledAt).atZone(ZoneId.systemDefault()).toLocalTime()
        val h12 = if (localTime.hour % 12 == 0) 12 else localTime.hour % 12
        val amPm = if (localTime.hour < 12) "AM" else "PM"
        "Hoy · ${String.format(Locale.US, "%d:%02d %s", h12, localTime.minute, amPm)}"
    } catch (_: Exception) {
        try {
            "Hoy · ${formatMedTime(scheduledAt)}"
        } catch (_: Exception) {
            "Hoy"
        }
    }
}

private fun formatRecurrence(recurrence: String): String {
    return when (recurrence.uppercase(Locale.US)) {
        "DAILY" -> "Diario"
        "WEEKLY" -> "Semanal"
        "MONTHLY" -> "Mensual"
        "ONCE" -> "Una vez"
        "AS_NEEDED" -> "Según necesidad"
        else -> recurrence.replace("_", " ")
            .lowercase(Locale.US)
            .replaceFirstChar { it.uppercase() }
    }
}

private fun formatAppointmentTimeLabel(millis: Long): String {
    if (millis == 0L) return "PENDIENTE"
    val today = LocalDate.now(ZoneId.systemDefault())
    val zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val appointmentDate = zdt.toLocalDate()
    val localTime = zdt.toLocalTime()
    val h12 = if (localTime.hour % 12 == 0) 12 else localTime.hour % 12
    val amPm = if (localTime.hour < 12) "AM" else "PM"
    val timeStr = String.format(Locale.US, "%d:%02d %s", h12, localTime.minute, amPm)
    return when (appointmentDate) {
        today -> "HOY · $timeStr"
        today.plusDays(1) -> "MAÑANA · $timeStr"
        else -> {
            val fmt = DateTimeFormatter.ofPattern("d MMM", Locale("es"))
            "${appointmentDate.format(fmt).uppercase()} · $timeStr"
        }
    }
}

private fun formatVitalsTimestamp(isoString: String): String {
    if (isoString.isBlank()) return "—"
    return try {
        val instant = Instant.parse(isoString)
        val minutesAgo = ChronoUnit.MINUTES.between(instant, Instant.now())
        when {
            minutesAgo < 1 -> "Ahora"
            minutesAgo < 60 -> "hace $minutesAgo min"
            else -> "hace ${minutesAgo / 60}h"
        }
    } catch (_: Exception) {
        "—"
    }
}

private fun buildWearablePayload(rawPayload: String, path: String, deviceUuid: String): String {
    val now = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
    val parsed = try { JSONObject(rawPayload) } catch (_: Exception) { null }
    if (parsed != null) {
        parsed.put("deviceId", deviceUuid)
        if (!parsed.has("timestamp")) parsed.put("timestamp", now)
        return parsed.toString()
    }
    return buildString {
        append("""{ "deviceId": """")
        append(escapeJson(deviceUuid))
        append("""", "timestamp": """")
        append(now)
        append("""", "rawPath": """")
        append(escapeJson(path))
        append("""", "rawPayload": """")
        append(escapeJson(rawPayload))
        append("""" }""")
    }
}

private fun escapeJson(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
