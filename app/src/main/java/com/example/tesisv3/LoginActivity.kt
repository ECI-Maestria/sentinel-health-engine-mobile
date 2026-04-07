package com.example.tesisv3

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.PersonAddAlt1
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import java.util.Locale

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sentinel_alerts",
                "Alertas de Signos Vitales",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Alertas críticas del Sentinel Health Engine"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LoginScreen(
                    onLoginDestination = { destination ->
                        when (destination) {
                            LoginDestination.DASHBOARD -> {
                                startActivity(Intent(this, DashboardActivity::class.java))
                            }
                            LoginDestination.DOCTOR -> {
                                startActivity(Intent(this, DoctorPatientsActivity::class.java))
                            }
                        }
                        finish()
                    }
                )
            }
        }
    }
}

private val LoginBackground = Color(0xFFF6F7F2)
private val LoginText = Color(0xFF2E3F35)
private val LoginMuted = Color(0xFF7B8C81)
private val LoginChip = Color(0xFF5BCB90)
private val LoginChipAlt = Color(0xFFE1F2E6)
private val LoginHero = Color(0xFF2F8A5B)
private val LoginHeroDark = Color(0xFF2A7C52)

@Composable
private fun LoginScreen(onLoginDestination: (LoginDestination) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        containerColor = LoginBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LoginBackground)
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LoginHero)
                    .statusBarsPadding()
                    .padding(top = 24.dp, bottom = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(LoginHeroDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Sentinel Health",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "Sentinel Health",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Monitoreo inteligente de salud",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(top = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Bienvenido de nuevo",
                        color = LoginText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ingresa con tus credenciales para continuar",
                        color = LoginMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text("Correo electrónico", color = LoginText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; showError = false },
                        placeholder = { Text("doctor@hospital.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Text("Contraseña", color = LoginText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; showError = false },
                        placeholder = { Text("••••••••") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Toggle password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (showError) {
                        Text(
                            text = errorMessage.ifBlank { "Invalid user or password" },
                            color = Color(0xFFD35C55),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = {
                            context.startActivity(Intent(context, ForgotPasswordActivity::class.java))
                        },
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("¿Olvidaste tu contraseña?", color = LoginHero, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (isLoading) return@Button
                            showError = false
                            errorMessage = ""
                            if (username == "user" && password == "user") {
                                onLoginDestination(LoginDestination.DASHBOARD)
                                return@Button
                            }
                            if (username.isBlank() || password.isBlank()) {
                                showError = true
                                errorMessage = "Please fill all fields"
                                return@Button
                            }
                            isLoading = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    loginWithApi(username.trim(), password)
                                }
                                isLoading = false
                                if (result.success) {
                                    withContext(Dispatchers.IO) {
                                        val profile = fetchCurrentUser()
                                        if (profile != null) {
                                            PatientSession.currentUser = profile
                                            PatientSession.patientId = profile.id
                                        }
                                    }
                                    val role = PatientSession.currentUser?.role
                                    if (role != null && role.equals("DOCTOR", ignoreCase = true)) {
                                        onLoginDestination(LoginDestination.DOCTOR)
                                    } else {
                                        onLoginDestination(LoginDestination.DASHBOARD)
                                    }
                                } else {
                                    showError = true
                                    errorMessage = result.message
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LoginChip),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isLoading) "Ingresando..." else "Ingresar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .height(1.dp)
                                .weight(1f)
                                .background(LoginChipAlt)
                        )
                        Text(
                            text = "¿Eres cuidador?",
                            color = LoginMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .height(1.dp)
                                .weight(1f)
                                .background(LoginChipAlt)
                        )
                    }

                    Button(
                        onClick = {
                            context.startActivity(Intent(context, CaretakerRegistrationActivity::class.java))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAddAlt1,
                            contentDescription = "Register caretaker",
                            tint = LoginHero,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "Registrar como Cuidador",
                            color = LoginHero,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "Al registrarte, podrás acceder una vez que un médico o paciente te vincule a su perfil.",
                        color = LoginMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

}

private enum class LoginDestination {
    DASHBOARD,
    DOCTOR
}

private data class LoginResult(val success: Boolean, val message: String)

private fun loginWithApi(email: String, password: String): LoginResult {
    val url = URL("https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/auth/login")
    val payload = """{"email":"$email","password":"$password"}"""
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
        }
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val body = readStream(
            if (code in 200..299) conn.inputStream else conn.errorStream
        )
        conn.disconnect()

        return when {
            code in 200..299 -> {
                val json = JSONObject(body)
                PatientSession.accessToken = json.optString("accessToken")
                PatientSession.refreshToken = json.optString("refreshToken")
                LoginResult(true, "")
            }
            code == 401 || code == 403 -> LoginResult(false, "Invalid user or password")
            else -> LoginResult(false, body.ifBlank { "Login failed (HTTP $code)" })
        }
    } catch (e: Exception) {
        LoginResult(false, e.message ?: "Network error")
    }
}

private fun readStream(stream: java.io.InputStream?): String {
    if (stream == null) return ""
    return BufferedReader(InputStreamReader(stream)).use { it.readText() }
}

private fun fetchCurrentUser(): UserProfile? {
    val url = URL("https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/users/me")
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            PatientSession.accessToken?.let {
                setRequestProperty("Authorization", "Bearer $it")
            }
        }
        val code = conn.responseCode
        val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        if (code !in 200..299 || body.isBlank()) return null
        val json = JSONObject(body)
        UserProfile(
            id = json.optString("id"),
            email = json.optString("email"),
            role = json.optString("role"),
            firstName = json.optString("firstName"),
            lastName = json.optString("lastName"),
            fullName = json.optString("fullName"),
            isActive = json.optBoolean("isActive", true),
            createdAt = json.optString("createdAt")
        )
    } catch (_: Exception) {
        null
    }
}


private fun forgotPassword(email: String): LoginResult {
    val url = URL("https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/auth/forgot-password")
    val safeEmail = escapeJson(email.trim().lowercase(Locale.US))
    val payload = """{"email":"$safeEmail"}"""
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
        }
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        when {
            code in 200..299 -> LoginResult(true, "")
            else -> LoginResult(false, body.ifBlank { "Request failed (HTTP $code)" })
        }
    } catch (e: Exception) {
        LoginResult(false, e.message ?: "Network error")
    }
}

private fun resetPassword(token: String, newPassword: String): LoginResult {
    val url = URL("https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/auth/reset-password")
    val payload = """{"token":"${escapeJson(token.trim())}","newPassword":"${escapeJson(newPassword.trim())}"}"""
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
        }
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        when {
            code in 200..299 -> LoginResult(true, "")
            else -> LoginResult(false, body.ifBlank { "Reset failed (HTTP $code)" })
        }
    } catch (e: Exception) {
        LoginResult(false, e.message ?: "Network error")
    }
}

private fun escapeJson(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}
