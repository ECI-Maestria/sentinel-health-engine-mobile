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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DoctorPatientsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }

        super.onCreate(savedInstanceState)
        val refreshOnStart = intent?.getBooleanExtra("refresh_patients", false) ?: false
        setContent {
            MaterialTheme {
                DoctorPatientsScreen(refreshOnStart = refreshOnStart)
            }
        }
    }
}

private val PatientsBackground = Color(0xFFF4F5F0)
private val PatientsCard = Color(0xFFFFFFFF)
private val PatientsChip = Color(0xFF5BCB90)
private val PatientsText = Color(0xFF2E3F35)
private val PatientsMuted = Color(0xFF7B8C81)
private val PatientsNav = Color(0xFF58725E)
private val PatientsHeader = Color(0xFF3FA974)
private val PatientsAccentSoft = Color(0xFFE6F3EC)
private val PatientsWarningSoft = Color(0xFFFFF1E2)
private val PatientsDangerSoft = Color(0xFFFFECEC)
private val PatientsDanger = Color(0xFFD64545)
private val PatientsWarning = Color(0xFFF39C2D)

@Composable
private fun DoctorPatientsScreen(refreshOnStart: Boolean) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var patients by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasRefreshed by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var alertPatient by remember { mutableStateOf<UserProfile?>(null) }
    var alertExpanded by remember { mutableStateOf(false) }
    var alertStats by remember { mutableStateOf(DoctorAlertStats(0, 0, 0)) }

    fun loadPatients() {
        if (isLoading) return
        isLoading = true
        errorMessage = null
        scope.launch {
            val result = withContext(Dispatchers.IO) { fetchPatients() }
            isLoading = false
            if (result.error != null) {
                errorMessage = result.error
            } else {
                patients = result.patients
            }
        }
    }

    if ((patients.isEmpty() || (refreshOnStart && !hasRefreshed)) && errorMessage == null && !isLoading) {
        if (refreshOnStart) {
            hasRefreshed = true
        }
        loadPatients()
    }

    LaunchedEffect(patients) {
        if (alertPatient == null) {
            alertPatient = patients.firstOrNull()
        }
    }

    LaunchedEffect(alertPatient?.id) {
        val patientId = alertPatient?.id ?: return@LaunchedEffect
        val from = LocalDate.now().minusDays(30).format(DateTimeFormatter.ISO_DATE)
        val to = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        alertStats = withContext(Dispatchers.IO) {
            fetchAlertStats(patientId, from, to)
        }
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
            containerColor = PatientsBackground,
            bottomBar = {
                AppBottomNav(
                    current = BottomNavDestination.DASHBOARD,
                    modifier = Modifier,
                    indicatorColor = PatientsAccentSoft,
                    selectedColor = PatientsHeader,
                    unselectedColor = PatientsMuted.copy(alpha = 0.5f)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PatientsBackground),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    end = 18.dp,
                    bottom = innerPadding.calculateBottomPadding() + 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DoctorTopBar(
                        onMenu = { scope.launch { drawerState.open() } },
                        onNotifications = {
                            context.startActivity(android.content.Intent(context, NotificationsActivity::class.java))
                        }
                    )
                }

            item {
                DoctorSummaryCard(
                    doctorName = PatientSession.currentUser?.fullName ?: "Dr. Ana Martínez",
                    patientCount = patients.size,
                    alertsToday = 3,
                    appointmentsToday = 2
                )
            }

            item {
                SectionHeader(title = "Acciones Rápidas")
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    QuickActionCard(
                        title = "Nuevo Paciente",
                        icon = Icons.Outlined.PersonAddAlt1,
                        tint = PatientsHeader,
                        background = PatientsAccentSoft,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.startActivity(
                                android.content.Intent(context, PatientRegistrationActivity::class.java)
                            )
                        }
                    )
                    QuickActionCard(
                        title = "Agendar Cita",
                        icon = Icons.Outlined.CalendarToday,
                        tint = Color(0xFF2E7BD8),
                        background = Color(0xFFE8F1FF),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.startActivity(
                                android.content.Intent(context, CalendarActivity::class.java)
                            )
                        }
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    QuickActionCard(
                        title = "Generar Reporte",
                        icon = Icons.Outlined.Description,
                        tint = Color(0xFFD39C39),
                        background = Color(0xFFFFF4D9),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.startActivity(
                                android.content.Intent(context, ReportsActivity::class.java)
                            )
                        }
                    )
                    QuickActionCard(
                        title = "Nuevo Doctor",
                        icon = Icons.Outlined.Person,
                        tint = Color(0xFF7B5CE7),
                        background = Color(0xFFF0E9FF),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.startActivity(
                                android.content.Intent(context, DoctorRegistrationActivity::class.java)
                            )
                        }
                    )
                }
            }

            item {
                SectionHeader(title = "Alertas Activas", actionLabel = "Ver todas")
            }

            if (patients.isNotEmpty()) {
                item {
                    PatientPickerCard(
                        label = "Paciente",
                        patients = patients,
                        selected = alertPatient,
                        expanded = alertExpanded,
                        onExpandedChange = { alertExpanded = it },
                        onSelect = { patient ->
                            alertPatient = patient
                            alertExpanded = false
                        }
                    )
                }
            }

            item {
                AlertCard(
                    title = "Críticas",
                    subtitle = "${alertStats.critical} alertas críticas en 30 días",
                    time = "Últimos 30 días",
                    accent = PatientsDanger,
                    background = PatientsDangerSoft
                )
            }

            item {
                AlertCard(
                    title = "En observación",
                    subtitle = "${alertStats.warning} alertas de seguimiento",
                    time = "Últimos 30 días",
                    accent = PatientsWarning,
                    background = PatientsWarningSoft
                )
            }

            item {
                SectionHeader(
                    title = "Mis Pacientes",
                    actionLabel = "Ver todos",
                    onActionClick = {
                        context.startActivity(
                            android.content.Intent(context, DoctorPatientsListActivity::class.java)
                        )
                    }
                )
            }

                val cards = patients.map { patient ->
                    DoctorPatientUi(
                        id = patient.id,
                        name = patient.fullName.orEmpty().ifBlank { patient.email },
                        email = patient.email,
                        detail = "Paciente activo",
                        initials = initialsOf(patient.fullName.orEmpty().ifBlank { patient.email }),
                        avatarColor = Color(0xFFE6F3EC),
                        initialsColor = PatientsHeader,
                        status = if (patient.isActive) "Activo" else "Inactivo",
                        statusColor = if (patient.isActive) PatientsHeader else PatientsMuted,
                        statusBackground = if (patient.isActive) Color(0xFFE5F4EA) else Color(0xFFF1F1F1)
                    )
                }
                items(cards) { patient ->
                    DoctorPatientCard(patient = patient)
                }
            }
        }
    }

}

@Composable
private fun DoctorTopBar(onMenu: () -> Unit, onNotifications: () -> Unit) {
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
            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = PatientsNav)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SENTINEL HEALTH", color = PatientsMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("Panel del Doctor", color = PatientsText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(
            onClick = onNotifications,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
        ) {
            Box {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = PatientsNav)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE84747))
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
private fun DoctorSummaryCard(
    doctorName: String,
    patientCount: Int,
    alertsToday: Int,
    appointmentsToday: Int
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PatientsHeader),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Buenos días 👋", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(doctorName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetric(value = patientCount.toString(), label = "Pacientes")
                DividerLine()
                SummaryMetric(value = alertsToday.toString(), label = "Alertas hoy")
                DividerLine()
                SummaryMetric(value = appointmentsToday.toString(), label = "Citas hoy")
            }
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(Color.White.copy(alpha = 0.4f))
    )
}

@Composable
private fun SectionHeader(title: String, actionLabel: String? = null, onActionClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = PatientsText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (actionLabel != null) {
            Text(
                actionLabel,
                color = PatientsHeader,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = if (onActionClick != null) Modifier.clickable { onActionClick() } else Modifier
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(16.dp),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = tint)
                }
                Text(title, color = PatientsText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AlertCard(
    title: String,
    subtitle: String,
    time: String,
    accent: Color,
    background: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(title, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Text(subtitle, color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(time, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DoctorPatientCard(patient: DoctorPatientUi) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    ) {
        Button(
            onClick = {
                val intent = android.content.Intent(context, DoctorPatientProfileActivity::class.java).apply {
                    putExtra("patient_id", patient.id)
                    putExtra("patient_name", patient.name)
                    putExtra("patient_email", patient.email)
                    putExtra("patient_initials", patient.initials)
                    putExtra("patient_status", patient.status)
                }
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(patient.avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(patient.initials, color = patient.initialsColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(patient.name, color = PatientsText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(patient.detail, color = PatientsMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(patient.statusBackground)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(patient.status, color = patient.statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) return parts.first().take(2).uppercase()
    return (parts.first().take(1) + parts.last().take(1)).uppercase()
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
            Text(label, color = PatientsHeader, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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
                                color = PatientsHeader,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { onExpandedChange(true) }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Select", tint = PatientsHeader)
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
                            text = { Text(patient.fullName ?: patient.email) },
                            onClick = { onSelect(patient) }
                        )
                    }
                }
            }
        }
    }
}

private data class DoctorAlertStats(val critical: Int, val warning: Int, val total: Int)

private fun fetchAlertStats(patientId: String, from: String, to: String): DoctorAlertStats {
    val token = PatientSession.accessToken
    if (token.isNullOrBlank()) return DoctorAlertStats(0, 0, 0)
    val url = URL("https://analytics-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/alerts/stats?from=$from&to=$to")
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
        if (code !in 200..299 || body.isBlank()) return DoctorAlertStats(0, 0, 0)
        val json = JSONObject(body)
        val critical = json.optInt("critical", json.optInt("high", 0))
        val warning = json.optInt("warning", json.optInt("medium", 0))
        val total = json.optInt("total", json.optInt("count", critical + warning))
        DoctorAlertStats(critical, warning, total)
    } catch (_: Exception) {
        DoctorAlertStats(0, 0, 0)
    }
}

private data class DoctorPatientsResult(val patients: List<UserProfile>, val error: String?)

private data class CreateResult(val success: Boolean, val message: String)

private data class DoctorPatientUi(
    val id: String,
    val name: String,
    val email: String,
    val detail: String,
    val initials: String,
    val avatarColor: Color,
    val initialsColor: Color,
    val status: String,
    val statusColor: Color,
    val statusBackground: Color
)

private fun mockDoctorPatients(): List<DoctorPatientUi> {
    return listOf(
        DoctorPatientUi(
            id = "", name = "María García",
            email = "maria.garcia@email.com",
            detail = "2 dispositivos · 1 cuidador",
            initials = "MG",
            avatarColor = Color(0xFFD8F0E2),
            initialsColor = Color(0xFF2F8A5B),
            status = "Alerta",
            statusColor = PatientsDanger,
            statusBackground = Color(0xFFFFE7E7)
        ),
        DoctorPatientUi(
            id = "", name = "Carlos López",
            email = "carlos.lopez@email.com",
            detail = "1 dispositivo · 2 cuidadores",
            initials = "CL",
            avatarColor = Color(0xFFD6E9FF),
            initialsColor = Color(0xFF2464C3),
            status = "Revisión",
            statusColor = PatientsWarning,
            statusBackground = Color(0xFFFFF1D6)
        ),
        DoctorPatientUi(
            id = "", name = "Juan Pérez",
            email = "juan.perez@email.com",
            detail = "1 dispositivo · 0 cuidadores",
            initials = "JP",
            avatarColor = Color(0xFFFFF1DA),
            initialsColor = Color(0xFFC07000),
            status = "Normal",
            statusColor = PatientsHeader,
            statusBackground = Color(0xFFE5F4EA)
        )
    )
}

@Composable
private fun CreateUserDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: suspend (String, String, String) -> CreateResult
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var showMessage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it; showMessage = false },
                    label = { Text("First name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it; showMessage = false },
                    label = { Text("Last name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; showMessage = false },
                    label = { Text("Email") },
                    singleLine = true,
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
                    if (firstName.isBlank() || lastName.isBlank() || email.isBlank()) {
                        message = "Please fill all fields"
                        showMessage = true
                        return@Button
                    }
                    isSubmitting = true
                    scope.launch {
                        val result = onSubmit(firstName.trim(), lastName.trim(), email.trim())
                        isSubmitting = false
                        if (result.success) {
                            message = "Success: created"
                            showMessage = true
                            onDismiss()
                        } else {
                            message = result.message
                            showMessage = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PatientsChip)
            ) {
                Text(if (isSubmitting) "Saving..." else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun fetchPatients(): DoctorPatientsResult {
    val token = PatientSession.accessToken
    if (token.isNullOrBlank()) {
        return DoctorPatientsResult(emptyList(), "Missing access token")
    }
    val url = URL("https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients")
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
            return DoctorPatientsResult(emptyList(), body.ifBlank { "Request failed (HTTP $code)" })
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
        DoctorPatientsResult(list, null)
    } catch (e: Exception) {
        DoctorPatientsResult(emptyList(), e.message ?: "Network error")
    }
}

private fun createUser(endpoint: String, firstName: String, lastName: String, email: String): CreateResult {
    val token = PatientSession.accessToken
    if (token.isNullOrBlank()) {
        return CreateResult(false, "Missing access token")
    }
    if (endpoint.isBlank()) {
        return CreateResult(false, "Missing endpoint")
    }
    val payload = """{"firstName":"${escapeJson(firstName)}","lastName":"${escapeJson(lastName)}","email":"${escapeJson(email)}"}"""
    return try {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            doOutput = true
        }
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        when {
            code in 200..299 -> CreateResult(true, "")
            else -> CreateResult(false, body.ifBlank { "Request failed (HTTP $code)" })
        }
    } catch (e: Exception) {
        CreateResult(false, e.message ?: "Network error")
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
