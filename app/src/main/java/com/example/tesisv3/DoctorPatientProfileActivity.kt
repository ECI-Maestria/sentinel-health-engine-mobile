package com.example.tesisv3

import android.app.DatePickerDialog
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ── Activity ──────────────────────────────────────────────────────────────────

class DoctorPatientProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }
        super.onCreate(savedInstanceState)
        val patientId = intent.getStringExtra("patient_id") ?: ""
        val name      = intent.getStringExtra("patient_name") ?: ""
        val email     = intent.getStringExtra("patient_email") ?: ""
        val initials  = intent.getStringExtra("patient_initials") ?: ""
        val status    = intent.getStringExtra("patient_status") ?: ""
        setContent {
            MaterialTheme {
                DoctorPatientProfileScreen(
                    patientId = patientId,
                    name      = name,
                    email     = email,
                    initials  = initials,
                    status    = status,
                    onBack    = { finish() }
                )
            }
        }
    }
}

// ── Colors ────────────────────────────────────────────────────────────────────

private val ProfileBg          = Color(0xFFF3F7F4)
private val ProfileText        = Color(0xFF2E3F35)
private val ProfileMuted       = Color(0xFF7B8C81)
private val ProfileAccent      = Color(0xFF2F8A5B)
private val ProfileAccentSoft  = Color(0xFFD8F0E2)
private val ProfileWarning     = Color(0xFFD64545)
private val ProfileChipBg      = Color(0xFFE8F4EC)
private val ProfileVitals      = Color(0xFF2F8A5B)
private val ProfileVitalsDark  = Color(0xFF2A7C52)
private val ProfileDivider     = Color(0xFFE6ECE7)

// ── Data classes ──────────────────────────────────────────────────────────────

private data class ProfileLatestVitals(
    val heartRate: Int,
    val spO2: Double,
    val measuredAt: String
)

private data class ProfileMedication(
    val id: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val scheduledTimes: List<String>,
    val startDate: String,
    val endDate: String,
    val notes: String,
    val active: Boolean
)

private data class ProfileAppointment(
    val id: String,
    val date: String,
    val time: String,
    val doctorName: String,
    val type: String,
    val status: String
)

private data class ProfileCaretaker(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String
)

private data class ProfileAlertStats(
    val total: Int,
    val critical: Int,
    val high: Int,
    val medium: Int,
    val low: Int
)

// ── Frequency helpers ─────────────────────────────────────────────────────────

private val freqOptions = listOf(
    "ONCE_DAILY"        to "1 vez al día",
    "TWICE_DAILY"       to "2 veces al día",
    "THREE_TIMES_DAILY" to "3 veces al día",
    "EVERY_8_HOURS"     to "Cada 8 horas",
    "EVERY_6_HOURS"     to "Cada 6 horas",
    "ONCE_WEEKLY"       to "Una vez a la semana",
    "AS_NEEDED"         to "Según necesidad"
)

private fun freqLabel(value: String) =
    freqOptions.firstOrNull { it.first == value }?.second
        ?: value.replace("_", " ").lowercase(Locale.US).replaceFirstChar { it.uppercase() }

// ── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoctorPatientProfileScreen(
    patientId: String,
    name: String,
    email: String,
    initials: String,
    status: String,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // ── Latest vitals state
    var latestVitals       by remember { mutableStateOf<ProfileLatestVitals?>(null) }
    var vitalsLoading      by remember { mutableStateOf(false) }

    LaunchedEffect(patientId) {
        if (patientId.isBlank()) return@LaunchedEffect
        vitalsLoading = true
        latestVitals  = withContext(Dispatchers.IO) { fetchProfileLatestVitals(patientId) }
        vitalsLoading = false
    }

    // ── Medications state
    var medications        by remember { mutableStateOf<List<ProfileMedication>>(emptyList()) }
    var medLoading         by remember { mutableStateOf(false) }
    var showAddMed         by remember { mutableStateOf(false) }

    // ── Appointments state
    var appointments       by remember { mutableStateOf<List<ProfileAppointment>>(emptyList()) }
    var apptLoading        by remember { mutableStateOf(false) }

    // ── Caretakers state
    var caretakers         by remember { mutableStateOf<List<ProfileCaretaker>>(emptyList()) }
    var crtLoading         by remember { mutableStateOf(false) }

    // ── Alerts state
    var alertStats         by remember { mutableStateOf<ProfileAlertStats?>(null) }
    var alertLoading       by remember { mutableStateOf(false) }

    // Load data when tab or patientId changes
    LaunchedEffect(selectedTab, patientId) {
        if (patientId.isBlank()) return@LaunchedEffect
        when (selectedTab) {
            0 -> {
                medLoading  = true
                medications = withContext(Dispatchers.IO) { fetchProfileMedications(patientId) }
                medLoading  = false
            }
            1 -> {
                apptLoading   = true
                appointments  = withContext(Dispatchers.IO) { fetchProfileAppointments(patientId) }
                apptLoading   = false
            }
            2 -> {
                crtLoading  = true
                caretakers  = withContext(Dispatchers.IO) { fetchProfileCaretakers(patientId) }
                crtLoading  = false
            }
            3 -> {
                alertLoading = true
                alertStats   = withContext(Dispatchers.IO) { fetchProfileAlerts(patientId) }
                alertLoading = false
            }
        }
    }

    Scaffold(containerColor = ProfileBg) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ProfileBg)
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Top bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
                    ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ProfileText) }
                    Text("Perfil del Paciente", color = ProfileText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
                    ) { Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = ProfileText) }
                }
            }

            // ── Avatar + name
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(ProfileAccentSoft),
                        contentAlignment = Alignment.Center
                    ) { Text(initials, color = ProfileAccent, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                    Text(name, color = ProfileText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(email, color = ProfileMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileStatusBadge(
                            label = if (status == "Alerta") "Alerta activa" else status,
                            textColor = ProfileWarning,
                            background = Color(0xFFFFE9E9)
                        )
                        ProfileStatusBadge(label = "Activo", textColor = ProfileAccent, background = ProfileChipBg)
                    }
                }
            }

            // ── Vitals card (real data)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ProfileVitals),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ÚLTIMOS SIGNOS VITALES", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            if (vitalsLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else if (latestVitals != null) {
                                Text(
                                    latestVitals!!.measuredAt.take(10),
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProfileVitalTile(
                                "Frec. Cardíaca",
                                if (vitalsLoading) "—" else (latestVitals?.heartRate?.toString() ?: "—"),
                                "bpm",
                                modifier = Modifier.weight(1f)
                            )
                            ProfileVitalTile(
                                "SpO2",
                                if (vitalsLoading) "—" else latestVitals?.spO2?.let {
                                    if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.US, "%.1f", it)
                                } ?: "—",
                                "%",
                                modifier = Modifier.weight(1f),
                                highlight = latestVitals != null && latestVitals!!.spO2 < 95.0
                            )
                        }
                    }
                }
            }

            // ── Tab selector
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileTab("Medicamentos", selectedTab == 0) { selectedTab = 0 }
                    ProfileTab("Citas",        selectedTab == 1) { selectedTab = 1 }
                    ProfileTab("Cuidadores",   selectedTab == 2) { selectedTab = 2 }
                    ProfileTab("Alertas",      selectedTab == 3) { selectedTab = 3 }
                }
            }

            // ── Tab content ──────────────────────────────────────────────────

            when (selectedTab) {

                // ── MEDICAMENTOS ─────────────────────────────────────────────
                0 -> {
                    if (medLoading) {
                        item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(color = ProfileAccent) } }
                    } else if (medications.isEmpty()) {
                        item { ProfileEmptyState("Sin medicamentos registrados", Icons.Outlined.MedicalServices) }
                    } else {
                        items(medications) { med ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(0.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(
                                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(ProfileAccentSoft),
                                            contentAlignment = Alignment.Center
                                        ) { Icon(Icons.Outlined.MedicalServices, contentDescription = null, tint = ProfileAccent) }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(med.name, color = ProfileText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text("${med.dosage} · ${freqLabel(med.frequency)}", color = ProfileMuted, fontSize = 12.sp)
                                            if (med.scheduledTimes.isNotEmpty()) {
                                                Text(med.scheduledTimes.joinToString(", "), color = ProfileMuted, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                            .background(if (med.active) ProfileChipBg else Color(0xFFF1F1F1))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            if (med.active) "Activo" else "Inactivo",
                                            color = if (med.active) ProfileAccent else ProfileMuted,
                                            fontSize = 11.sp, fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = { showAddMed = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ProfileAccent),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("+ Agregar Medicamento", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }

                // ── CITAS ────────────────────────────────────────────────────
                1 -> {
                    if (apptLoading) {
                        item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(color = ProfileAccent) } }
                    } else if (appointments.isEmpty()) {
                        item { ProfileEmptyState("Sin citas esta semana", Icons.Outlined.CalendarMonth) }
                    } else {
                        items(appointments) { appt ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(0.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(
                                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE8F1FF)),
                                            contentAlignment = Alignment.Center
                                        ) { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Color(0xFF2E7BD8)) }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(appt.type.ifBlank { "Consulta" }, color = ProfileText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text("${appt.date}  ${appt.time}", color = ProfileMuted, fontSize = 12.sp)
                                            if (appt.doctorName.isNotBlank()) {
                                                Text(appt.doctorName, color = ProfileMuted, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFE8F1FF))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) { Text(appt.status, color = Color(0xFF2E7BD8), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }

                // ── CUIDADORES ───────────────────────────────────────────────
                2 -> {
                    if (crtLoading) {
                        item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(color = ProfileAccent) } }
                    } else if (caretakers.isEmpty()) {
                        item { ProfileEmptyState("Sin cuidadores asignados", Icons.Outlined.Person) }
                    } else {
                        items(caretakers) { crt ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(0.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(42.dp).clip(CircleShape).background(ProfileAccentSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            crt.fullName.take(1).uppercase(),
                                            color = ProfileAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(crt.fullName, color = ProfileText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(crt.email, color = ProfileMuted, fontSize = 12.sp)
                                        if (crt.phone.isNotBlank()) {
                                            Text(crt.phone, color = ProfileMuted, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── ALERTAS ──────────────────────────────────────────────────
                3 -> {
                    if (alertLoading) {
                        item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(color = ProfileAccent) } }
                    } else if (alertStats == null) {
                        item { ProfileEmptyState("Sin datos de alertas", Icons.Outlined.Warning) }
                    } else {
                        val stats = alertStats!!
                        item {
                            Text(
                                "Últimos 30 días",
                                color = ProfileMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(0.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text("Resumen de Alertas", color = ProfileText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    AlertStatRow("Total de alertas", stats.total.toString(), ProfileMuted, Color(0xFFF1F1F1))
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(ProfileDivider))
                                    AlertStatRow("Críticas",  stats.critical.toString(), Color(0xFFD64545), Color(0xFFFFE9E9))
                                    AlertStatRow("Altas",     stats.high.toString(),     Color(0xFFE07A1A), Color(0xFFFFF0E0))
                                    AlertStatRow("Medias",    stats.medium.toString(),   Color(0xFFCC7A00), Color(0xFFFFF4D9))
                                    AlertStatRow("Bajas",     stats.low.toString(),      ProfileAccent,    ProfileChipBg)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Add medication bottom sheet
    if (showAddMed) {
        AddProfileMedSheet(
            patientId = patientId,
            onDismiss = { showAddMed = false },
            onSaved = {
                showAddMed = false
                scope.launch {
                    medLoading  = true
                    medications = withContext(Dispatchers.IO) { fetchProfileMedications(patientId) }
                    medLoading  = false
                }
            }
        )
    }
}

// ── Add Medication Bottom Sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProfileMedSheet(
    patientId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var name             by remember { mutableStateOf("") }
    var dosage           by remember { mutableStateOf("") }
    var frequency        by remember { mutableStateOf("TWICE_DAILY") }
    var startDate        by remember { mutableStateOf("") }
    var endDate          by remember { mutableStateOf("") }
    var notes            by remember { mutableStateOf("") }
    var freqExpanded     by remember { mutableStateOf(false) }
    var showTimePicker   by remember { mutableStateOf(false) }
    var saving           by remember { mutableStateOf(false) }
    var error            by remember { mutableStateOf("") }
    val scheduledTimes   = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    val timePickerState  = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = true)

    fun openDate(current: String, onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        val parts = current.split("-")
        if (parts.size == 3) {
            parts[0].toIntOrNull()?.let { cal.set(Calendar.YEAR, it) }
            parts[1].toIntOrNull()?.let { cal.set(Calendar.MONTH, it - 1) }
            parts[2].toIntOrNull()?.let { cal.set(Calendar.DAY_OF_MONTH, it) }
        }
        DatePickerDialog(context, { _, y, m, d ->
            onPicked(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Nuevo Medicamento", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            OutlinedTextField(
                value = name, onValueChange = { name = it; error = "" },
                label = { Text("Nombre del medicamento") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dosage, onValueChange = { dosage = it; error = "" },
                label = { Text("Dosis (ej. 500mg)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Frequency dropdown
            Box {
                OutlinedTextField(
                    value = freqLabel(frequency), onValueChange = {},
                    label = { Text("Frecuencia") }, readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { freqExpanded = true }) {
                            Icon(Icons.Outlined.Settings, contentDescription = null, tint = ProfileMuted)
                        }
                    }
                )
                DropdownMenu(expanded = freqExpanded, onDismissRequest = { freqExpanded = false }) {
                    freqOptions.forEach { (key, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { frequency = key; freqExpanded = false })
                    }
                }
            }

            // Scheduled times
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Horas programadas", color = ProfileText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Button(
                    onClick = { showTimePicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ProfileAccent),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) { Text("+ Agregar hora", fontSize = 12.sp) }
            }
            if (scheduledTimes.isEmpty()) {
                Text("Sin horarios aún", color = ProfileMuted, fontSize = 12.sp)
            } else {
                Text(scheduledTimes.joinToString(", "), color = ProfileAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            // Dates
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = startDate, onValueChange = {},
                    label = { Text("Inicio") }, readOnly = true, singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { openDate(startDate) { startDate = it } },
                    colors = ButtonDefaults.buttonColors(containerColor = ProfileAccentSoft),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) { Text("📅", fontSize = 14.sp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = endDate, onValueChange = {},
                    label = { Text("Fin") }, readOnly = true, singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { openDate(endDate) { endDate = it } },
                    colors = ButtonDefaults.buttonColors(containerColor = ProfileAccentSoft),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) { Text("📅", fontSize = 14.sp) }
            }

            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notas") }, singleLine = false, maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            if (error.isNotBlank()) {
                Text(error, color = ProfileWarning, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Button(
                    onClick = {
                        if (name.isBlank() || dosage.isBlank()) { error = "Nombre y dosis son requeridos"; return@Button }
                        if (scheduledTimes.isEmpty()) { error = "Agrega al menos una hora"; return@Button }
                        scope.launch {
                            saving = true
                            val ok = withContext(Dispatchers.IO) {
                                createProfileMedication(patientId, ProfileMedication(
                                    id = "", name = name.trim(), dosage = dosage.trim(),
                                    frequency = frequency, scheduledTimes = scheduledTimes.toList(),
                                    startDate = startDate, endDate = endDate,
                                    notes = notes.trim(), active = true
                                ))
                            }
                            saving = false
                            if (ok) onSaved() else error = "Error al guardar, intenta de nuevo"
                        }
                    },
                    enabled = !saving,
                    colors = ButtonDefaults.buttonColors(containerColor = ProfileAccent)
                ) { Text(if (saving) "Guardando…" else "Guardar") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(20.dp), color = Color.White
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Selecciona la hora", fontWeight = FontWeight.Bold)
                    TimePicker(state = timePickerState)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
                        Button(
                            onClick = {
                                val formatted = String.format(Locale.US, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                                if (!scheduledTimes.contains(formatted)) scheduledTimes.add(formatted)
                                showTimePicker = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ProfileAccent)
                        ) { Text("Confirmar") }
                    }
                }
            }
        }
    }
}

// ── Reusable composables ──────────────────────────────────────────────────────

@Composable
private fun ProfileStatusBadge(label: String, textColor: Color, background: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(background)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) { Text(label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ProfileVitalTile(
    title: String, value: String, suffix: String,
    modifier: Modifier = Modifier, highlight: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (highlight) ProfileVitalsDark else ProfileVitals),
        elevation = CardDefaults.cardElevation(0.dp), modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text(suffix, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = if (selected) ProfileAccent else Color.White),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (selected) Color.White else ProfileMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileEmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(ProfileChipBg),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = ProfileAccent, modifier = Modifier.size(32.dp)) }
        Text(message, color = ProfileMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AlertStatRow(label: String, value: String, textColor: Color, badgeBg: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ProfileText, fontSize = 14.sp)
        Box(
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(badgeBg).padding(horizontal = 12.dp, vertical = 4.dp)
        ) { Text(value, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }
}

// ── API functions ─────────────────────────────────────────────────────────────

private fun fetchProfileMedications(patientId: String): List<ProfileMedication> {
    return try {
        val url = URL("https://calendar-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/medications?active=false")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000; readTimeout = 10000
            PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        val body = profileReadStream(if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        val arr = JSONObject(body).optJSONArray("medications") ?: JSONArray()
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val times = o.optJSONArray("scheduledTimes") ?: JSONArray()
            ProfileMedication(
                id = o.optString("id"), name = o.optString("name"),
                dosage = o.optString("dosage"), frequency = o.optString("frequency"),
                scheduledTimes = (0 until times.length()).map { times.optString(it) },
                startDate = o.optString("startDate"), endDate = o.optString("endDate"),
                notes = o.optString("notes"), active = o.optBoolean("active", true)
            )
        }
    } catch (_: Exception) { emptyList() }
}

private fun createProfileMedication(patientId: String, med: ProfileMedication): Boolean {
    return try {
        val times = med.scheduledTimes.joinToString(",") { "\"$it\"" }
        val payload = """{"name":"${med.name}","dosage":"${med.dosage}","frequency":"${med.frequency}","scheduledTimes":[$times],"startDate":"${med.startDate}","endDate":"${med.endDate}","notes":"${med.notes}"}"""
        val url = URL("https://calendar-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/medications")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true
            connectTimeout = 10000; readTimeout = 10000
            setRequestProperty("Content-Type", "application/json")
            PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        }
        val code = conn.responseCode
        conn.disconnect()
        code in 200..299
    } catch (_: Exception) { false }
}

private fun fetchProfileAppointments(patientId: String): List<ProfileAppointment> {
    return try {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
        val url = URL("https://calendar-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/appointments?period=week&date=$today")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000; readTimeout = 10000
            PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        val body = profileReadStream(if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        val raw = JSONObject(body)
        val arr = raw.optJSONArray("appointments") ?: raw.optJSONArray("data") ?: JSONArray()
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            ProfileAppointment(
                id         = o.optString("id"),
                date       = o.optString("date").ifBlank { o.optString("appointmentDate") },
                time       = o.optString("time").ifBlank { o.optString("startTime") },
                doctorName = o.optString("doctorName").ifBlank { o.optString("providerName") },
                type       = o.optString("type").ifBlank { o.optString("appointmentType") },
                status     = o.optString("status").ifBlank { "Programada" }
            )
        }
    } catch (_: Exception) { emptyList() }
}

private fun fetchProfileCaretakers(patientId: String): List<ProfileCaretaker> {
    return try {
        val url = URL("https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/caretakers")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000; readTimeout = 10000
            PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        val body = profileReadStream(if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        val raw = JSONObject(body)
        val arr = raw.optJSONArray("caretakers") ?: raw.optJSONArray("data") ?: JSONArray()
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            ProfileCaretaker(
                id       = o.optString("id"),
                fullName = o.optString("fullName").ifBlank { "${o.optString("firstName")} ${o.optString("lastName")}".trim() },
                email    = o.optString("email"),
                phone    = o.optString("phone").ifBlank { o.optString("phoneNumber") }
            )
        }
    } catch (_: Exception) { emptyList() }
}

private fun fetchProfileAlerts(patientId: String): ProfileAlertStats? {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val to   = fmt.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val from = fmt.format(cal.time)
        val url = URL("https://analytics-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/alerts/stats?from=$from&to=$to")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000; readTimeout = 10000
            PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        val body = profileReadStream(if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        val o = JSONObject(body)
        ProfileAlertStats(
            total    = o.optInt("total",    o.optInt("totalAlerts")),
            critical = o.optInt("critical", o.optInt("criticalAlerts")),
            high     = o.optInt("high",     o.optInt("highAlerts")),
            medium   = o.optInt("medium",   o.optInt("mediumAlerts")),
            low      = o.optInt("low",      o.optInt("lowAlerts"))
        )
    } catch (_: Exception) { null }
}

private fun profileReadStream(stream: java.io.InputStream?): String {
    if (stream == null) return "{}"
    return BufferedReader(InputStreamReader(stream)).use { it.readText() }
}

private fun fetchProfileLatestVitals(patientId: String): ProfileLatestVitals? {
    return try {
        val url = URL("https://analytics-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/vitals/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000; readTimeout = 10000
            PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        val body = profileReadStream(if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        val o = JSONObject(body)
        val spo2Raw = o.opt("spO2")
        val spo2 = when (spo2Raw) {
            is Double -> spo2Raw
            is Int    -> spo2Raw.toDouble()
            is Float  -> spo2Raw.toDouble()
            else      -> 0.0
        }
        ProfileLatestVitals(
            heartRate  = o.optInt("heartRate"),
            spO2       = spo2,
            measuredAt = o.optString("measuredAt")
        )
    } catch (_: Exception) { null }
}
