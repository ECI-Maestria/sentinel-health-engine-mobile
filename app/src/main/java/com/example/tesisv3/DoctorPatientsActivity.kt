package com.example.tesisv3

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class DoctorPatientsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DoctorPatientsScreen()
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

@Composable
private fun DoctorPatientsScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var patients by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreatePatient by remember { mutableStateOf(false) }
    var showCreateCaretaker by remember { mutableStateOf(false) }

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

    if (patients.isEmpty() && errorMessage == null && !isLoading) {
        loadPatients()
    }

    Scaffold(containerColor = PatientsBackground) { innerPadding ->
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
                PatientsTopBar(onNotifications = {
                    context.startActivity(android.content.Intent(context, NotificationsActivity::class.java))
                })
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Patients", color = PatientsText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Assigned patients list",
                            color = PatientsMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showCreatePatient = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PatientsChip),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("New patient", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { showCreateCaretaker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PatientsChip),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("New caretaker", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { loadPatients() },
                    colors = ButtonDefaults.buttonColors(containerColor = PatientsChip),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(if (isLoading) "Loading..." else "Refresh", fontWeight = FontWeight.Bold)
                }
            }

            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage ?: "Error loading patients",
                        color = Color(0xFFD35C55),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            items(patients) { patient ->
                PatientCard(patient = patient)
            }
        }
    }

    if (showCreatePatient) {
        CreateUserDialog(
            title = "Create patient",
            onDismiss = { showCreatePatient = false },
            onSubmit = { first, last, email ->
                val result = withContext(Dispatchers.IO) {
                    createUser(
                        endpoint = "https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients",
                        firstName = first,
                        lastName = last,
                        email = email
                    )
                }
                if (result.success) {
                    loadPatients()
                }
                result
            }
        )
    }

    if (showCreateCaretaker) {
        CreateUserDialog(
            title = "Create caretaker",
            onDismiss = { showCreateCaretaker = false },
            onSubmit = { first, last, email ->
                withContext(Dispatchers.IO) {
                    createUser(
                        endpoint = "https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/caretakers",
                        firstName = first,
                        lastName = last,
                        email = email
                    )
                }
            }
        )
    }
}

@Composable
private fun PatientsTopBar(onNotifications: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = {}) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = PatientsNav)
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Dr", color = PatientsText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onNotifications) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = PatientsNav)
        }
    }
}

@Composable
private fun PatientCard(patient: UserProfile) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PatientsCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = patient.fullName ?: "${patient.firstName ?: ""} ${patient.lastName ?: ""}".trim(),
                color = PatientsText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = patient.email, color = PatientsMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusChip(label = if (patient.isActive) "Active" else "Inactive")
                StatusChip(label = patient.role.ifBlank { "PATIENT" })
            }
        }
    }
}

@Composable
private fun StatusChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PatientsChip.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = PatientsText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private data class PatientsResult(val patients: List<UserProfile>, val error: String?)

private data class CreateResult(val success: Boolean, val message: String)

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

private fun fetchPatients(): PatientsResult {
    val token = PatientSession.accessToken
    if (token.isNullOrBlank()) {
        return PatientsResult(emptyList(), "Missing access token")
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
            return PatientsResult(emptyList(), body.ifBlank { "Request failed (HTTP $code)" })
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
        PatientsResult(list, null)
    } catch (e: Exception) {
        PatientsResult(emptyList(), e.message ?: "Network error")
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
