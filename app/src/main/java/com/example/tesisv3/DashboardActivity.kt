package com.example.tesisv3

import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.graphics.Color
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
import org.json.JSONArray
import org.json.JSONObject

// ── Data models ───────────────────────────────────────────────────────────────

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
    var wearableConnected by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        wearableConnected = withContext(Dispatchers.IO) { isWearableConnected(context) }
    }

    LaunchedEffect(Unit) {
        val patientId = PatientSession.patientId
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Vitals: wearable data takes priority; fallback to API if no live data
        if (vitals == null) {
            val apiVitals = withContext(Dispatchers.IO) { fetchApiVitals(patientId) }
            if (vitals == null) vitals = apiVitals
        }

        medications = withContext(Dispatchers.IO) { fetchDashMedications(patientId) }
        appointments = withContext(Dispatchers.IO) { fetchDashAppointments(patientId, today) }
        reminders = withContext(Dispatchers.IO) { fetchDashReminders(patientId, today) }
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
                item { VitalsCard(vitals = vitals) }
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
        IconButton(onClick = {
            context.startActivity(Intent(context, NotificationsActivity::class.java))
        }) {
            Box {
                Icon(
                    Icons.Outlined.NotificationsNone,
                    contentDescription = "Notificaciones",
                    tint = DashNav
                )
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
private fun VitalsCard(vitals: DashVitals?) {
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
                    label = "📌 PA",
                    value = vitals?.bpSystolic?.toString() ?: "—",
                    unit = vitals?.bpDiastolic?.let { "/$it" } ?: "",
                    highlight = bpHigh
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
            Switch(
                checked = med.active,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = DashGreenLight,
                    uncheckedTrackColor = Color(0xFFCCCCCC)
                )
            )
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

// ── API functions ─────────────────────────────────────────────────────────────

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

private fun isWearableConnected(context: android.content.Context): Boolean {
    return try {
        val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
        nodes.isNotEmpty()
    } catch (_: Exception) {
        false
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
