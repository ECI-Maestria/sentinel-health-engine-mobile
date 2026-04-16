package com.example.tesisv3.ui

import com.example.tesisv3.*

import com.example.tesisv3.network.*

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale

class ReportsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ReportsScreen()
            }
        }
    }
}

private val ReportsBackground = Color(0xFFF4F5F0)
private val ReportsCard = Color(0xFFFFFFFF)
private val ReportsText = Color(0xFF2E3F35)
private val ReportsMuted = Color(0xFF7B8C81)
private val ReportsChip = Color(0xFF5BCB90)
private val ReportsChipAlt = Color(0xFFE1F2E6)
private val ReportsNav = Color(0xFF58725E)
private val ReportsBorder = Color(0xFFE1E7E2)

@Composable
private fun ReportsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var patients by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<UserProfile?>(null) }
    var patientExpanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("Resumen Clínico") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) { fetchReportsPatients() }
        patients = result.patients
        if (selectedPatient == null) {
            selectedPatient = result.patients.firstOrNull()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                RoleDrawerContent(
                    role = PatientSession.currentUser?.role,
                    current = DrawerDestination.DOCTOR_REPORTS,
                    onItemClick = { destination ->
                        handleDrawerNavigation(context, destination)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = ReportsBackground,
            bottomBar = {
                AppBottomNav(
                    current = BottomNavDestination.REPORTS,
                    modifier = Modifier,
                    indicatorColor = ReportsChipAlt,
                    selectedColor = ReportsNav,
                    unselectedColor = ReportsNav.copy(alpha = 0.5f)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ReportsBackground),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    end = 18.dp,
                    bottom = innerPadding.calculateBottomPadding() + 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ReportsTopBar(
                        onMenu = { scope.launch { drawerState.open() } },
                        onNotifications = {
                            context.startActivity(android.content.Intent(context, NotificationsActivity::class.java))
                        }
                    )
                }

                item {
                    Text("Nuevo Reporte", color = ReportsText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ReportsCard,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Paciente", color = ReportsMuted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Box {
                                OutlinedTextField(
                                    value = selectedPatient?.fullName ?: "Selecciona paciente",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .clip(RoundedCornerShape(14.dp))
                                        .padding(0.dp),
                                    trailingIcon = {
                                        IconButton(onClick = { patientExpanded = true }) {
                                            Icon(Icons.Filled.Menu, contentDescription = "Select", tint = ReportsMuted)
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = patientExpanded,
                                    onDismissRequest = { patientExpanded = false }
                                ) {
                                    patients.forEach { patient ->
                                        DropdownMenuItem(
                                            text = { Text(patient.fullName.orEmpty()) },
                                            onClick = {
                                                selectedPatient = patient
                                                patientExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Text("Tipo de Reporte", color = ReportsMuted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ReportTypeChip("Resumen Clínico", selectedType == "Resumen Clínico") {
                                    selectedType = "Resumen Clínico"
                                }
                                ReportTypeChip("Vitales", selectedType == "Vitales") {
                                    selectedType = "Vitales"
                                }
                                ReportTypeChip("Adherencia", selectedType == "Adherencia") {
                                    selectedType = "Adherencia"
                                }
                            }

                            Text("Rango de fechas", color = ReportsMuted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            DateRangeField(
                                startDate = startDate,
                                endDate = endDate,
                                onPickStart = { startDate = it },
                                onPickEnd = { endDate = it }
                            )

                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = ReportsChip),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(Icons.Outlined.Description, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Generar Reporte PDF", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Text("Reportes Recientes", color = ReportsText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                items(3) { index ->
                    ReportRow(
                        title = when (index) {
                            0 -> "Resumen Clínico — Torres"
                            1 -> "Signos Vitales — López"
                            else -> "Adherencia — García"
                        },
                        detail = when (index) {
                            0 -> "31 Mar 2026 · 2.4 MB"
                            1 -> "28 Mar 2026 · 1.8 MB"
                            else -> "15 Mar 2026 · 1.1 MB"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportsTopBar(onMenu: () -> Unit, onNotifications: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onMenu,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
        ) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = ReportsNav)
        }
        Text("Reportes", color = ReportsText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        IconButton(
            onClick = onNotifications,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
        ) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = ReportsNav)
        }
    }
}

@Composable
private fun ReportTypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = if (selected) ReportsChip else ReportsChipAlt),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (selected) Color.White else ReportsText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun DateRangeField(
    startDate: String,
    endDate: String,
    onPickStart: (String) -> Unit,
    onPickEnd: (String) -> Unit
) {
    val context = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = if (startDate.isBlank()) "Inicio" else startDate,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.weight(1f),
            trailingIcon = {
                IconButton(onClick = { showDatePicker(context, onPickStart) }) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = ReportsMuted)
                }
            }
        )
        OutlinedTextField(
            value = if (endDate.isBlank()) "Fin" else endDate,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.weight(1f),
            trailingIcon = {
                IconButton(onClick = { showDatePicker(context, onPickEnd) }) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = ReportsMuted)
                }
            }
        )
    }
}

private fun showDatePicker(context: android.content.Context, onSelected: (String) -> Unit) {
    val cal = Calendar.getInstance()
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

@Composable
private fun ReportRow(title: String, detail: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ReportsCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, ReportsBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, color = ReportsText, fontWeight = FontWeight.Bold)
                Text(detail, color = ReportsMuted, fontSize = 12.sp)
            }
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F6EF)),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Descargar", color = ReportsChip, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}


private data class ReportsPatientsResult(val patients: List<UserProfile>, val error: String?)

private fun fetchReportsPatients(): ReportsPatientsResult {
    val token = PatientSession.accessToken
    if (token.isNullOrBlank()) {
        return ReportsPatientsResult(emptyList(), "Missing access token")
    }
    val url = URL("${ApiConstants.USER_SERVICE}/v1/patients")
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
        if (code !in 200..299) {
            return ReportsPatientsResult(emptyList(), body.ifBlank { "Request failed (HTTP $code)" })
        }
        val json = JSONObject(body)
        val array = json.optJSONArray("patients")
        val list = mutableListOf<UserProfile>()
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
        ReportsPatientsResult(list, null)
    } catch (e: Exception) {
        ReportsPatientsResult(emptyList(), e.message ?: "Network error")
    }
}

