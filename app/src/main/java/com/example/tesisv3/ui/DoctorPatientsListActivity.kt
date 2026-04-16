package com.example.tesisv3.ui

import com.example.tesisv3.*

import com.example.tesisv3.network.*

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class DoctorPatientsListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DoctorPatientsListScreen(onBack = { finish() })
            }
        }
    }
}

private val ListBackground = Color(0xFFF4F5F0)
private val ListHeader = Color(0xFF2F8A5B)
private val ListCard = Color(0xFFFFFFFF)
private val ListText = Color(0xFF2E3F35)
private val ListMuted = Color(0xFF7B8C81)
private val ListChip = Color(0xFF5BCB90)
private val ListChipAlt = Color(0xFFE1F2E6)
private val ListDanger = Color(0xFFE06A61)
private val ListWarn = Color(0xFFF0B44F)
private val ListOk = Color(0xFF5BCB90)

@Composable
private fun DoctorPatientsListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var patients by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var enrichedPatients by remember { mutableStateOf<List<DoctorPatientRow>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var debouncedSearch by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Todos") }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) { fetchPatients() }
        patients = result.patients
        enrichedPatients = withContext(Dispatchers.IO) {
            buildPatientRows(result.patients)
        }
    }

    LaunchedEffect(search) {
        delay(300)
        debouncedSearch = search
    }

    val filtered = enrichedPatients.filter { row ->
        val fullName = row.profile.fullName ?: row.profile.email
        val matchesSearch = fullName.contains(debouncedSearch, ignoreCase = true) ||
            row.profile.email.contains(debouncedSearch, ignoreCase = true)
        val matchesFilter = when (filter) {
            "Críticos" -> row.status == RiskStatus.CRITICAL
            "Estables" -> row.status == RiskStatus.STABLE
            "Sin dispositivo" -> row.status == RiskStatus.NO_DEVICE
            else -> true
        }
        matchesSearch && matchesFilter
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                RoleDrawerContent(
                    role = PatientSession.currentUser?.role,
                    current = DrawerDestination.DOCTOR_PANEL,
                    onItemClick = { destination ->
                        handleDrawerNavigation(context, destination)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = ListBackground,
            bottomBar = {
                AppBottomNav(
                    current = BottomNavDestination.GROUPS,
                    modifier = Modifier,
                    indicatorColor = ListChipAlt,
                    selectedColor = ListHeader,
                    unselectedColor = ListMuted.copy(alpha = 0.5f)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ListBackground),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    end = 18.dp,
                    bottom = innerPadding.calculateBottomPadding() + 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ListHeader)
                        }
                        Text("Mis Pacientes", color = ListHeader, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                        ) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = ListHeader)
                        }
                    }
                }

                item {
                    val criticalCount = enrichedPatients.count { it.status == RiskStatus.CRITICAL }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${patients.size} pacientes", color = ListMuted, fontWeight = FontWeight.SemiBold)
                        Text("•", color = ListMuted)
                        Text("$criticalCount críticos", color = ListDanger, fontWeight = FontWeight.SemiBold)
                    }
                }

                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = ListMuted) },
                        placeholder = { Text("Buscar paciente...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip("Todos", filter == "Todos") { filter = "Todos" }
                        FilterChip("Críticos", filter == "Críticos") { filter = "Críticos" }
                        FilterChip("Estables", filter == "Estables") { filter = "Estables" }
                        FilterChip("Sin dispositivo", filter == "Sin dispositivo") { filter = "Sin dispositivo" }
                    }
                }

                items(filtered) { patient ->
                    PatientListRow(
                        patient = patient.profile,
                        statusColor = patient.statusColor,
                        statusLabel = patient.statusLabel,
                        onClick = {
                            val intent = android.content.Intent(context, DoctorPatientProfileActivity::class.java).apply {
                                putExtra("patient_id", patient.profile.id)
                                putExtra("patient_name", patient.profile.fullName ?: patient.profile.email)
                                putExtra("patient_email", patient.profile.email)
                                putExtra("patient_initials", initialsOf(patient.profile.fullName.orEmpty().ifBlank { patient.profile.email }))
                                putExtra("patient_status", patient.statusLabel)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = if (selected) ListChip else ListChipAlt),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (selected) Color.White else ListText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun PatientListRow(
    patient: UserProfile,
    statusColor: Color,
    statusLabel: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ListCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE6F3EC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initialsOf(patient.fullName.orEmpty().ifBlank { patient.email }), color = ListHeader, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(patient.fullName ?: patient.email, color = ListText, fontWeight = FontWeight.Bold)
                        Text(patient.email, color = ListMuted, fontSize = 12.sp)
                        Text(statusLabel, color = ListMuted, fontSize = 11.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
        }
    }
}

private enum class RiskStatus { CRITICAL, WARNING, STABLE, NO_DEVICE }

private data class DoctorPatientRow(
    val profile: UserProfile,
    val status: RiskStatus,
    val statusColor: Color,
    val statusLabel: String
)

private suspend fun buildPatientRows(patients: List<UserProfile>): List<DoctorPatientRow> = coroutineScope {
    val from = LocalDate.now().minusDays(30)
    val to = LocalDate.now()
    val fromLabel = from.format(DateTimeFormatter.ISO_DATE)
    val toLabel = to.format(DateTimeFormatter.ISO_DATE)

    patients.map { patient ->
        async(Dispatchers.IO) {
            val devices = fetchDeviceCount(patient.id)
            val stats = fetchAlertStats(patient.id, fromLabel, toLabel)
            val hasDevice = devices != null && devices > 0
            val status = when {
                !hasDevice -> RiskStatus.NO_DEVICE
                stats.critical > 0 -> RiskStatus.CRITICAL
                stats.warning > 0 -> RiskStatus.WARNING
                else -> RiskStatus.STABLE
            }
            val color = when (status) {
                RiskStatus.CRITICAL -> ListDanger
                RiskStatus.WARNING -> ListWarn
                RiskStatus.NO_DEVICE -> ListMuted
                RiskStatus.STABLE -> ListOk
            }
            val label = when (status) {
                RiskStatus.CRITICAL -> "Crítico"
                RiskStatus.WARNING -> "En observación"
                RiskStatus.NO_DEVICE -> "Sin dispositivo"
                RiskStatus.STABLE -> "Estable"
            }
            DoctorPatientRow(
                profile = patient,
                status = status,
                statusColor = color,
                statusLabel = label
            )
        }
    }.map { it.await() }
}

private data class DoctorListAlertStats(val critical: Int, val warning: Int, val total: Int)

private fun fetchAlertStats(patientId: String, from: String, to: String): DoctorListAlertStats {
    val token = PatientSession.accessToken
    if (token.isNullOrBlank()) return DoctorListAlertStats(0, 0, 0)
    val url = URL("${ApiConstants.ANALYTICS_SERVICE}/v1/patients/$patientId/alerts/stats?from=$from&to=$to")
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Authorization", "Bearer $token")
        }
        val code = conn.responseCode
        val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        if (code !in 200..299 || body.isBlank()) return DoctorListAlertStats(0, 0, 0)
        val json = JSONObject(body)
        val critical = json.optInt("critical", json.optInt("high", 0))
        val warning = json.optInt("warning", json.optInt("medium", 0))
        val total = json.optInt("total", json.optInt("count", critical + warning))
        DoctorListAlertStats(critical, warning, total)
    } catch (_: Exception) {
        DoctorListAlertStats(0, 0, 0)
    }
}

private fun fetchDeviceCount(patientId: String): Int? {
    val token = PatientSession.accessToken
    if (token.isNullOrBlank()) return null
    val url = URL("${ApiConstants.USER_SERVICE}/v1/patients/$patientId/profile/complete")
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Authorization", "Bearer $token")
        }
        val code = conn.responseCode
        val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        if (code !in 200..299 || body.isBlank()) return null
        val json = JSONObject(body)
        val devices = json.optJSONArray("devices")
        devices?.length() ?: 0
    } catch (_: Exception) {
        null
    }
}

private data class DoctorListPatientsResult(val patients: List<UserProfile>, val error: String?)

private fun initialsOf(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) return parts.first().take(2).uppercase()
    return (parts.first().take(1) + parts.last().take(1)).uppercase()
}

private fun fetchPatients(): DoctorListPatientsResult {
    val token = PatientSession.accessToken
    if (token.isNullOrBlank()) {
        return DoctorListPatientsResult(emptyList(), "Missing access token")
    }
    val isCaretaker = PatientSession.currentUser?.role?.equals("CARETAKER", ignoreCase = true) == true
    val urlStr = if (isCaretaker) {
        "${ApiConstants.USER_SERVICE}/v1/caretakers/me/patients"
    } else {
        "${ApiConstants.USER_SERVICE}/v1/patients"
    }
    return try {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Authorization", "Bearer $token")
        }
        val code = conn.responseCode
        val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        if (code !in 200..299) {
            return DoctorListPatientsResult(emptyList(), body.ifBlank { "Request failed (HTTP $code)" })
        }
        val list = mutableListOf<UserProfile>()
        if (isCaretaker) {
            // Response: JSON array or object with "patients"/"data" key
            val arr = when {
                body.trimStart().startsWith("[") -> JSONArray(body)
                else -> JSONObject(body).let { o ->
                    o.optJSONArray("patients") ?: o.optJSONArray("data") ?: JSONArray()
                }
            }
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val p = item.optJSONObject("patient") ?: item
                val id = item.optString("patientId").ifBlank { p.optString("id") }
                if (id.isBlank()) continue
                list.add(
                    UserProfile(
                        id = id,
                        email = p.optString("email"),
                        role = p.optString("role"),
                        firstName = p.optString("firstName"),
                        lastName = p.optString("lastName"),
                        fullName = p.optString("fullName").ifBlank { null },
                        isActive = p.optBoolean("isActive", true),
                        createdAt = p.optString("createdAt")
                    )
                )
            }
        } else {
            val array = JSONObject(body).optJSONArray("patients")
            if (array != null) {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    list.add(
                        UserProfile(
                            id = item.optString("id"),
                            email = item.optString("email"),
                            role = item.optString("role"),
                            firstName = item.optString("firstName"),
                            lastName = item.optString("lastName"),
                            fullName = item.optString("fullName"),
                            isActive = item.optBoolean("isActive", true),
                            createdAt = item.optString("createdAt")
                        )
                    )
                }
            }
        }
        DoctorListPatientsResult(list, null)
    } catch (e: Exception) {
        DoctorListPatientsResult(emptyList(), e.message ?: "Network error")
    }
}

