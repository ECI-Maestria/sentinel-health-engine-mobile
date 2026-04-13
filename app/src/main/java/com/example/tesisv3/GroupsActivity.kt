package com.example.tesisv3

import android.os.Build
import android.os.Bundle
import android.content.Intent
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DrawerValue
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import java.util.Locale

class GroupsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GroupsScreen(onBack = { finish() })
            }
        }
    }
}

private val GroupsBackground = Color(0xFFF6F7F2)
private val GroupsText = Color(0xFF2E3F35)
private val GroupsMuted = Color(0xFF7B8C81)
private val GroupsChip = Color(0xFF64CFA1)
private val GroupsChipAlt = Color(0xFFE1F2E6)
private val GroupsNav = Color(0xFF5A7A63)
private val GroupsCard = Color(0xFFFFFFFF)
private val GroupsAccent = Color(0xFF4FA6A5)
private val GroupsBorder = Color(0xFFE1E7E2)
private val GroupsDanger = Color(0xFFE06A61)

@Composable
private fun GroupsScreen(onBack: () -> Unit) {
    var selectedNav by remember { mutableIntStateOf(1) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val isDoctor = PatientSession.currentUser?.role?.equals("DOCTOR", ignoreCase = true) == true
    var patients by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<UserProfile?>(null) }
    var patientExpanded by remember { mutableStateOf(false) }
    var caretakers by remember { mutableStateOf<List<CaretakerUi>>(emptyList()) }
    var emailInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageSuccess by remember { mutableStateOf(true) }
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
                caretakers = withContext(Dispatchers.IO) { fetchCaretakers(it.id) }
            }
        } else {
            caretakers = withContext(Dispatchers.IO) {
                fetchCaretakers(PatientSession.patientId)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                RoleDrawerContent(
                    role = PatientSession.currentUser?.role,
                    current = DrawerDestination.GROUPS,
                    onItemClick = { destination ->
                        handleDrawerNavigation(context, destination)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = GroupsBackground,
            bottomBar = {
                AppBottomNav(
                    current = BottomNavDestination.GROUPS,
                    modifier = Modifier.navigationBarsPadding(),
                    indicatorColor = GroupsChipAlt,
                    selectedColor = GroupsNav,
                    unselectedColor = GroupsNav.copy(alpha = 0.5f)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GroupsBackground),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    end = 18.dp,
                    bottom = innerPadding.calculateBottomPadding() + 18.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { GroupsTopBar(wearableConnected = wearableConnected, onMenu = { scope.launch { drawerState.open() } }) }

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
                                    caretakers = withContext(Dispatchers.IO) {
                                        fetchCaretakers(patient.id)
                                    }
                                }
                            }
                        )
                    }
                }

                item {
                    InfoCard()
                }

                item {
                    LinkCaretakerCard(
                        email = emailInput,
                        onEmailChange = { emailInput = it; message = null },
                        isSubmitting = isSubmitting,
                        onLink = {
                            if (emailInput.isBlank()) {
                                message = "Ingresa un correo válido"
                                messageSuccess = false
                                return@LinkCaretakerCard
                            }
                            isSubmitting = true
                            scope.launch {
                                val targetPatientId = if (isDoctor) {
                                    selectedPatient?.id ?: PatientSession.patientId
                                } else {
                                    PatientSession.patientId
                                }
                                val result = withContext(Dispatchers.IO) {
                                    linkCaretaker(targetPatientId, emailInput.trim())
                                }
                                isSubmitting = false
                                if (result.success) {
                                    message = "Cuidador vinculado"
                                    messageSuccess = true
                                    emailInput = ""
                                    caretakers = withContext(Dispatchers.IO) {
                                        fetchCaretakers(targetPatientId)
                                    }
                                } else {
                                    message = result.message
                                    messageSuccess = false
                                }
                            }
                        }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Cuidadores vinculados",
                            color = GroupsText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(GroupsChipAlt)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(caretakers.size.toString(), color = GroupsNav, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                if (caretakers.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = GroupsCard
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No hay cuidadores vinculados", color = GroupsMuted)
                            }
                        }
                    }
                } else {
                    items(caretakers.size) { index ->
                        val caretaker = caretakers[index]
                        CaretakerRow(
                            caretaker = caretaker,
                            onUnlink = {
                                scope.launch {
                                    val targetPatientId = if (isDoctor) {
                                        selectedPatient?.id ?: PatientSession.patientId
                                    } else {
                                        PatientSession.patientId
                                    }
                                    val result = withContext(Dispatchers.IO) {
                                        unlinkCaretaker(targetPatientId, caretaker.caretakerId)
                                    }
                                    if (result.success) {
                                        message = "Cuidador desvinculado"
                                        messageSuccess = true
                                        caretakers = withContext(Dispatchers.IO) {
                                            fetchCaretakers(targetPatientId)
                                        }
                                    } else {
                                        message = result.message
                                        messageSuccess = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    message?.let { text ->
        android.widget.Toast
            .makeText(context, text, if (messageSuccess) android.widget.Toast.LENGTH_SHORT else android.widget.Toast.LENGTH_LONG)
            .show()
        message = null
    }
}

@Composable
private fun GroupsTopBar(wearableConnected: Boolean?, onMenu: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Filled.Menu, contentDescription = "Back", tint = GroupsNav)
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", color = GroupsText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            WatchStatusIcon(wearableConnected = wearableConnected)
            IconButton(onClick = {
                context.startActivity(Intent(context, NotificationsActivity::class.java))
            }) {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = GroupsNav)
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFEFF6FF),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD2E4FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("¿Para qué sirven los cuidadores?", color = GroupsNav, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("• Pueden ver tus signos vitales y recibir alertas", color = GroupsNav, fontSize = 12.sp)
            Text("• Te ayudan a gestionar tu medicación diaria", color = GroupsNav, fontSize = 12.sp)
            Text("• Reciben notificaciones de tus citas médicas", color = GroupsNav, fontSize = 12.sp)
            Text("• Pueden coordinar con tu médico en caso de emergencia", color = GroupsNav, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LinkCaretakerCard(
    email: String,
    onEmailChange: (String) -> Unit,
    isSubmitting: Boolean,
    onLink: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = GroupsCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, GroupsBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Vincular nuevo cuidador", color = GroupsText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Ingresa el correo electrónico del cuidador que deseas vincular.",
                color = GroupsMuted,
                fontSize = 12.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = GroupsMuted) },
                    placeholder = { Text("correo@ejemplo.com") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onLink,
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = GroupsChip),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(if (isSubmitting) "Vinculando" else "Vincular", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CaretakerRow(caretaker: CaretakerUi, onUnlink: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = GroupsCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, GroupsBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GroupsChipAlt),
                contentAlignment = Alignment.Center
            ) {
                Text(caretaker.initials, color = GroupsNav, fontWeight = FontWeight.Bold)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(caretaker.fullName, color = GroupsText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(caretaker.email, color = GroupsMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(caretaker.linkedAtLabel, color = GroupsMuted, fontSize = 11.sp)
            }

            Button(
                onClick = onUnlink,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFECEC)),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Outlined.LinkOff, contentDescription = "Desvincular", tint = GroupsDanger, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Desvincular", color = GroupsDanger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class CaretakerUi(
    val caretakerId: String,
    val fullName: String,
    val email: String,
    val linkedAtLabel: String,
    val initials: String
)

private data class CaretakerLinkResult(val success: Boolean, val message: String)

private fun fetchCaretakers(patientId: String): List<CaretakerUi> {
    if (patientId.isBlank()) return emptyList()
    val url = URL("https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/caretakers")
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
        val arr = json.optJSONArray("caretakers") ?: return emptyList()
        val list = mutableListOf<CaretakerUi>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val fullName = item.optString("fullName")
            list.add(
                CaretakerUi(
                    caretakerId = item.optString("caretakerId"),
                    fullName = fullName,
                    email = item.optString("email"),
                    linkedAtLabel = formatLinkedAt(item.optString("linkedAt")),
                    initials = initialsOf(fullName)
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

private fun linkCaretaker(patientId: String, email: String): CaretakerLinkResult {
    if (patientId.isBlank()) return CaretakerLinkResult(false, "patientId vacío")
    val url = URL("https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/caretakers")
    val caretakerId = java.util.UUID.randomUUID().toString()
    val payload = """{"caretakerEmail":"${escapeJson(email)}"}"""
    return sendCaretakerRequest(url, "POST", payload)
}

private fun unlinkCaretaker(patientId: String, caretakerId: String): CaretakerLinkResult {
    if (patientId.isBlank() || caretakerId.isBlank()) return CaretakerLinkResult(false, "Id vacío")
    val url = URL("https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/caretakers/$caretakerId")
    return sendCaretakerRequest(url, "DELETE", null)
}

private fun sendCaretakerRequest(url: URL, method: String, payload: String?): CaretakerLinkResult {
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
            CaretakerLinkResult(true, "")
        } else {
            CaretakerLinkResult(false, body.ifBlank { "Error (HTTP $code)" })
        }
    } catch (e: Exception) {
        CaretakerLinkResult(false, e.message ?: "Error de red")
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

private fun formatLinkedAt(value: String): String {
    if (value.isBlank()) return "Vinculado recientemente"
    return "Vinculado el ${value.substring(0, 10)}"
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) return parts.first().take(2).uppercase(Locale.US)
    return (parts.first().take(1) + parts.last().take(1)).uppercase(Locale.US)
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
        if (code !in 200..299 || body.isBlank()) return emptyList()
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
            Text(label, color = GroupsNav, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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
                                color = GroupsNav,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { onExpandedChange(true) }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Select", tint = GroupsNav)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) }
                ) {
                    patients.forEach { patient ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(patient.fullName ?: patient.email) },
                            onClick = { onSelect(patient) }
                        )
                    }
                }
            }
        }
    }
}
