package com.example.tesisv3

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import android.content.Intent
import android.app.DatePickerDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.Calendar
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class CareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CareScreen(onBack = { finish() })
            }
        }
    }
}

private val CareBackground = Color(0xFFF6F7F2)
private val CareCard = Color(0xFFFFFFFF)
private val CareText = Color(0xFF2E3F35)
private val CareMuted = Color(0xFF7B8C81)
private val CareChip = Color(0xFF5BCB90)
private val CareChipAlt = Color(0xFFE1F2E6)
private val CareNav = Color(0xFF5A7A63)
private val CareWarn = Color(0xFFE0A04B)
private val CareError = Color(0xFFE06A61)

private fun frequencyLabel(value: String): String {
    return when (value.uppercase(Locale.US)) {
        "ONCE_DAILY" -> "Once daily"
        "TWICE_DAILY" -> "Twice daily"
        "THREE_TIMES_DAILY" -> "3 times daily"
        "EVERY_4_HOURS" -> "Every 4 hours"
        "EVERY_6_HOURS" -> "Every 6 hours"
        "EVERY_8_HOURS" -> "Every 8 hours"
        else -> value.replace("_", " ").lowercase(Locale.US).replaceFirstChar { it.uppercase() }
    }
}

@Composable
private fun CareScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var medications by remember { mutableStateOf<List<ApiMedication>>(emptyList()) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackSuccess by remember { mutableStateOf(true) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val canEdit = PatientSession.currentUser?.role?.equals("PATIENT", ignoreCase = true) != true
    val isDoctor = PatientSession.currentUser?.role?.equals("DOCTOR", ignoreCase = true) == true
    var patients by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<UserProfile?>(null) }
    var patientExpanded by remember { mutableStateOf(false) }

    var showSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ApiMedication?>(null) }
    var deletingItem by remember { mutableStateOf<ApiMedication?>(null) }
    var wearableConnected by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        wearableConnected = withContext(Dispatchers.IO) { isWearableConnected(context) }
    }
    LaunchedEffect(Unit) {
        if (isDoctor) {
            val result = withContext(Dispatchers.IO) { fetchPatientsList() }
            patients = result
            selectedPatient = result.firstOrNull()
            selectedPatient?.let {
                medications = withContext(Dispatchers.IO) {
                    fetchMedications(it.id)
                }
            }
        } else {
            medications = withContext(Dispatchers.IO) {
                fetchMedications(PatientSession.patientId)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                RoleDrawerContent(
                    role = PatientSession.currentUser?.role,
                    current = DrawerDestination.MEDICATIONS,
                    onItemClick = { destination ->
                        handleDrawerNavigation(context, destination)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = CareBackground,
            bottomBar = {
                AppBottomNav(
                    current = BottomNavDestination.CARE,
                    modifier = Modifier,
                    indicatorColor = CareChipAlt,
                    selectedColor = CareNav,
                    unselectedColor = CareNav.copy(alpha = 0.5f)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CareBackground),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    end = 18.dp,
                    bottom = innerPadding.calculateBottomPadding() + 18.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { CareTopBar(wearableConnected = wearableConnected, onMenu = { scope.launch { drawerState.open() } }) }

                if (isDoctor) {
                    item {
                        PatientPickerCard(
                            label = "Paciente",
                            patients = patients,
                            selected = selectedPatient,
                            expanded = patientExpanded,
                            onExpandedChange = { patientExpanded = it },
                            onSelect = { patient ->
                                selectedPatient = patient
                                patientExpanded = false
                                scope.launch {
                                    medications = withContext(Dispatchers.IO) {
                                        fetchMedications(patient.id)
                                    }
                                }
                            }
                        )
                    }
                }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Medications",
                        color = CareText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (canEdit) {
                        Button(
                            onClick = {
                                editingItem = null
                                showSheet = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CareChip),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Add Medication", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            items(medications.size) { index ->
                val item = medications[index]
                MedicationRow(
                    name = item.name,
                    detail = "${item.dosage} · ${frequencyLabel(item.frequency)}",
                    subDetail = buildString {
                        if (item.scheduledTimes.isNotEmpty()) {
                            append(item.scheduledTimes.joinToString(", "))
                        }
                        if (item.startDate.isNotBlank() || item.endDate.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(item.startDate)
                            if (item.endDate.isNotBlank()) {
                                append(" → ").append(item.endDate)
                            }
                        }
                    },
                    badgeText = if (item.active) "Activo" else "Inactivo",
                    badgeColor = if (item.active) CareChip else CareWarn,
                    canEdit = canEdit,
                    onClick = { if (canEdit) { editingItem = item; showSheet = true } },
                    onEdit = {
                        if (canEdit) {
                            editingItem = item
                            showSheet = true
                        }
                    },
                    onDelete = { if (canEdit) { deletingItem = item } },
                    onToggle = null
                )
            }
        }

        if (showSheet) {
            AddMedicationSheet(
                onDismiss = { showSheet = false },
                existing = editingItem,
                onSave = { newItem ->
                    scope.launch {
                        val targetPatientId = if (isDoctor) {
                            selectedPatient?.id ?: PatientSession.patientId
                        } else {
                            PatientSession.patientId
                        }
                        val result = withContext(Dispatchers.IO) {
                            if (editingItem == null) {
                                createMedication(targetPatientId, newItem)
                            } else {
                                updateMedication(targetPatientId, newItem)
                            }
                        }
                        if (result.success) {
                            medications = withContext(Dispatchers.IO) {
                                fetchMedications(targetPatientId)
                            }
                            feedbackMessage = if (editingItem == null) "Medicamento creado" else "Medicamento actualizado"
                            feedbackSuccess = true
                        } else {
                            feedbackMessage = result.message
                            feedbackSuccess = false
                        }
                    }
                    showSheet = false
                    editingItem = null
                }
            )
        }

        if (deletingItem != null) {
            val item = deletingItem
            AlertDialog(
                onDismissRequest = { deletingItem = null },
                title = { Text("Delete medication") },
                text = { Text("Are you sure you want to delete ${item?.name}?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            item?.let { med ->
                                scope.launch {
                                    val targetPatientId = if (isDoctor) {
                                        selectedPatient?.id ?: PatientSession.patientId
                                    } else {
                                        PatientSession.patientId
                                    }
                                    val result = withContext(Dispatchers.IO) {
                                        deleteMedication(targetPatientId, med.id)
                                    }
                                    if (result.success) {
                                        medications = withContext(Dispatchers.IO) {
                                            fetchMedications(targetPatientId)
                                        }
                                        feedbackMessage = "Medicamento eliminado"
                                        feedbackSuccess = true
                                    } else {
                                        feedbackMessage = result.message
                                        feedbackSuccess = false
                                    }
                                }
                            }
                            deletingItem = null
                        }
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { deletingItem = null }) { Text("Cancel") }
                }
            )
        }
        feedbackMessage?.let { message ->
            android.widget.Toast
                .makeText(context, message, if (feedbackSuccess) android.widget.Toast.LENGTH_SHORT else android.widget.Toast.LENGTH_LONG)
                .show()
            feedbackMessage = null
        }
    }
    }
}

@Composable
private fun CareTopBar(wearableConnected: Boolean?, onMenu: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Filled.Menu, contentDescription = "Back", tint = CareNav)
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.MedicalServices, contentDescription = "Brand", tint = CareNav)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            WatchStatusIcon(wearableConnected = wearableConnected)
            IconButton(onClick = {
                context.startActivity(Intent(context, NotificationsActivity::class.java))
            }) {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = CareNav)
            }
        }
    }
}

@Composable
private fun PatientPickerCard(
    label: String,
    patients: List<UserProfile>,
    selected: UserProfile?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (UserProfile) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFE5F4EA),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(label, color = CareNav, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Box {
                OutlinedTextField(
                    value = selected?.fullName ?: "Selecciona paciente",
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD3EBDD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (selected?.fullName ?: "P").take(1),
                                color = CareNav,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { onExpandedChange(true) }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Select", tint = CareNav)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) }
                ) {
                    patients.forEach { patient ->
                        DropdownMenuItem(
                            text = { Text(patient.fullName.orEmpty()) },
                            onClick = { onSelect(patient) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicationRow(
    name: String,
    detail: String,
    subDetail: String,
    badgeText: String,
    badgeColor: Color,
    canEdit: Boolean = true,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: ((Boolean) -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CareCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CareChipAlt),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.MedicalServices, contentDescription = null, tint = CareNav)
            }

            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(name, color = CareText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(detail, color = CareMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (subDetail.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(subDetail, color = CareMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = badgeColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.size(10.dp))

            if (canEdit) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = CareNav)
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = CareError)
                }
            }
        }
    }
}

private data class ApiMedication(
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

private data class MedicationResult(val success: Boolean, val message: String)

private fun fetchMedications(patientId: String): List<ApiMedication> {
    if (patientId.isBlank()) return emptyList()
    val url = URL("https://calendar-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/medications?active=true")
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Content-Type", "application/json")
            PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            connectTimeout = 10000
            readTimeout = 10000
        }
        val code = conn.responseCode
        val body = readStreamString(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        if (code !in 200..299 || body.isBlank()) return emptyList()
        val json = JSONObject(body)
        val arr = json.optJSONArray("medications") ?: JSONArray()
        val list = mutableListOf<ApiMedication>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val timesArr = item.optJSONArray("scheduledTimes") ?: JSONArray()
            val times = mutableListOf<String>()
            for (t in 0 until timesArr.length()) {
                times.add(timesArr.optString(t))
            }
            list.add(
                ApiMedication(
                    id = item.optString("id"),
                    name = item.optString("name"),
                    dosage = item.optString("dosage"),
                    frequency = item.optString("frequency"),
                    scheduledTimes = times,
                    startDate = item.optString("startDate"),
                    endDate = item.optString("endDate"),
                    notes = item.optString("notes"),
                    active = item.optBoolean("active", true)
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

private fun createMedication(patientId: String, medication: ApiMedication): MedicationResult {
    if (patientId.isBlank()) return MedicationResult(false, "patientId vacío")
    val url = URL("https://calendar-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/medications")
    val payload = buildMedicationPayload(medication)
    return sendMedicationRequest(url, "POST", payload)
}

private fun updateMedication(patientId: String, medication: ApiMedication): MedicationResult {
    if (patientId.isBlank()) return MedicationResult(false, "patientId vacío")
    if (medication.id.isBlank()) return MedicationResult(false, "medicationId vacío")
    val url = URL("https://calendar-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/medications/${medication.id}")
    val payload = buildMedicationPayload(medication)
    return sendMedicationRequest(url, "PUT", payload)
}

private fun deleteMedication(patientId: String, medicationId: String): MedicationResult {
    if (patientId.isBlank()) return MedicationResult(false, "patientId vacío")
    if (medicationId.isBlank()) return MedicationResult(false, "medicationId vacío")
    val url = URL("https://calendar-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/medications/$medicationId")
    return sendMedicationRequest(url, "DELETE", null)
}

private fun sendMedicationRequest(url: URL, method: String, payload: String?): MedicationResult {
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Content-Type", "application/json")
            PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            connectTimeout = 10000
            readTimeout = 10000
            if (payload != null) {
                doOutput = true
                outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            }
        }
        val code = conn.responseCode
        val body = readStreamString(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        if (code in 200..299) {
            MedicationResult(true, "")
        } else {
            MedicationResult(false, body.ifBlank { "Error (HTTP $code)" })
        }
    } catch (e: Exception) {
        MedicationResult(false, e.message ?: "Error de red")
    }
}

private fun buildMedicationPayload(medication: ApiMedication): String {
    val times = medication.scheduledTimes.joinToString(",") { "\"${escapeJson(it)}\"" }
    return """{
        "name":"${escapeJson(medication.name)}",
        "dosage":"${escapeJson(medication.dosage)}",
        "frequency":"${escapeJson(medication.frequency)}",
        "scheduledTimes":[ $times ],
        "startDate":"${escapeJson(medication.startDate)}",
        "endDate":"${escapeJson(medication.endDate)}",
        "notes":"${escapeJson(medication.notes)}"
    }""".trimIndent()
}

private fun fetchPatientsList(): List<UserProfile> {
    val token = PatientSession.accessToken
    if (token.isNullOrBlank()) return emptyList()
    val url = URL("https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients")
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Authorization", "Bearer $token")
        }
        val code = conn.responseCode
        val body = readStreamString(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        if (code !in 200..299) return emptyList()
        val json = JSONObject(body)
        val array = json.optJSONArray("patients") ?: return emptyList()
        val list = mutableListOf<UserProfile>()
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
        list
    } catch (_: Exception) {
        emptyList()
    }
}

private fun readStreamString(stream: java.io.InputStream?): String {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMedicationSheet(
    onDismiss: () -> Unit,
    onSave: (ApiMedication) -> Unit,
    existing: ApiMedication? = null
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var dosage by remember(existing?.id) { mutableStateOf(existing?.dosage ?: "") }
    var frequency by remember(existing?.id) { mutableStateOf(existing?.frequency ?: "TWICE_DAILY") }
    val scheduledTimes = remember(existing?.id) {
        androidx.compose.runtime.mutableStateListOf<String>().apply {
            if (existing != null) {
                addAll(existing.scheduledTimes)
            }
        }
    }
    var startDate by remember(existing?.id) { mutableStateOf(existing?.startDate ?: "") }
    var endDate by remember(existing?.id) { mutableStateOf(existing?.endDate ?: "") }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes ?: "") }
    var error by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = androidx.compose.material3.rememberTimePickerState(
        initialHour = 8,
        initialMinute = 0,
        is24Hour = true
    )
    val context = LocalContext.current

    val frequencyOptions = listOf(
        "ONCE_DAILY" to "1 vez al día",
        "TWICE_DAILY" to "2 veces al día",
        "THREE_TIMES_DAILY" to "3 veces al día",
        "EVERY_8_HOURS" to "Cada 8 horas",
        "EVERY_6_HOURS" to "Cada 6 horas",
        "ONCE_WEEKLY" to "Una vez a la semana",
        "AS_NEEDED" to "Según necesidad"
    )

    fun frequencyLabelFor(value: String): String {
        return frequencyOptions.firstOrNull { it.first == value }?.second ?: value
    }

    fun openDatePicker(current: String, onSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        val parts = current.split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val d = parts[2].toIntOrNull()
            if (y != null && m != null && d != null) {
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m - 1)
                cal.set(Calendar.DAY_OF_MONTH, d)
            }
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onSelected(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (existing == null) "New medication" else "Edit medication",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = false },
                label = { Text("Medication name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it; error = false },
                label = { Text("Dosage (e.g. 500mg)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Box {
                OutlinedTextField(
                    value = frequencyLabelFor(frequency),
                    onValueChange = {},
                    label = { Text("Frecuencia") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { frequencyExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Abrir opciones",
                                tint = CareMuted
                            )
                        }
                    }
                )
                DropdownMenu(
                    expanded = frequencyExpanded,
                    onDismissRequest = { frequencyExpanded = false }
                ) {
                    frequencyOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.second) },
                            onClick = {
                                frequency = option.first
                                frequencyExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Horas programadas", color = CareText, fontWeight = FontWeight.SemiBold)
                Button(
                    onClick = { showTimePicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CareChip),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Agregar hora", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (scheduledTimes.isEmpty()) {
                Text("Sin horarios aún", color = CareMuted, fontSize = 12.sp)
            } else {
                Text(
                    scheduledTimes.joinToString(", "),
                    color = CareMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = {},
                    label = { Text("Start date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    readOnly = true
                )
                Button(
                    onClick = { openDatePicker(startDate) { startDate = it; error = false } },
                    colors = ButtonDefaults.buttonColors(containerColor = CareChipAlt),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("📅", fontSize = 12.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = endDate,
                    onValueChange = {},
                    label = { Text("End date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    readOnly = true
                )
                Button(
                    onClick = { openDatePicker(endDate) { endDate = it; error = false } },
                    colors = ButtonDefaults.buttonColors(containerColor = CareChipAlt),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("📅", fontSize = 12.sp)
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it; error = false },
                label = { Text("Notes") },
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )

            if (error) {
                Text(errorMessage, color = CareError, fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = {
                        if (name.isBlank() || dosage.isBlank()) {
                            error = true
                            errorMessage = "Please fill all fields"
                            return@Button
                        }

                        if (frequency.isBlank()) {
                            error = true
                            errorMessage = "Please enter frequency"
                            return@Button
                        }

                        if (scheduledTimes.isEmpty()) {
                            error = true
                            errorMessage = "Please enter at least one time"
                            return@Button
                        }

                        error = false
                        errorMessage = ""
                        val item = ApiMedication(
                            id = existing?.id ?: "",
                            name = name.trim(),
                            dosage = dosage.trim(),
                            frequency = frequency.trim().uppercase(Locale.US),
                            scheduledTimes = scheduledTimes.toList(),
                            startDate = startDate.trim(),
                            endDate = endDate.trim(),
                            notes = notes.trim(),
                            active = true
                        )
                        onSave(item)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CareChip)
                ) {
                    Text(if (existing == null) "Add" else "Save")
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }

    if (showTimePicker) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Selecciona la hora", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    androidx.compose.material3.TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
                        Button(
                            onClick = {
                                val formatted = String.format(Locale.US, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                                if (!scheduledTimes.contains(formatted)) {
                                    scheduledTimes.add(formatted)
                                }
                                error = false
                                errorMessage = ""
                                showTimePicker = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CareChip)
                        ) {
                            Text("Confirmar")
                        }
                    }
                }
            }
        }
    }
}
