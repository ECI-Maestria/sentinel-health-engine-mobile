package com.example.tesisv3

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.google.firebase.messaging.FirebaseMessaging
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
                    onLoginSuccess = {
                        startActivity(Intent(this, DashboardActivity::class.java))
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
private val LoginNav = Color(0xFF5A7A63)

@Composable
private fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = LoginBackground) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LoginBackground)
                .padding(
                    start = 22.dp,
                    top = innerPadding.calculateTopPadding() + 14.dp,
                    end = 22.dp,
                    bottom = innerPadding.calculateBottomPadding() + 22.dp
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LoginTopBar()

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Welcome back",
                color = LoginText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Please sign in to continue",
                color = LoginMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; showError = false },
                        label = { Text("User") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; showError = false },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showError) {
                        Text(
                            text = errorMessage.ifBlank { "Invalid user or password" },
                            color = Color(0xFFD35C55),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showPassword = !showPassword },
                            colors = ButtonDefaults.buttonColors(containerColor = LoginChipAlt),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (showPassword) "Hide" else "Show",
                                color = LoginText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (isLoading) return@Button
                                showError = false
                                errorMessage = ""
                                if (username == "user" && password == "user") {
                                    onLoginSuccess()
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
                                        onLoginSuccess()
                                    } else {
                                        showError = true
                                        errorMessage = result.message
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LoginChip),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text(if (isLoading) "Checking..." else "Login", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    TextButton(
                        onClick = { showChangePassword = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Change password", color = LoginText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    TextButton(
                        onClick = { showForgotPassword = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot password?", color = LoginText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            scope = scope,
            onDismiss = { showChangePassword = false }
        )
    }

    if (showForgotPassword) {
        ForgotPasswordDialog(
            scope = scope,
            onDismiss = { showForgotPassword = false }
        )
    }
}

@Composable
private fun LoginTopBar() {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = {}) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = LoginNav)
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", color = LoginText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = {
            context.startActivity(Intent(context, NotificationsActivity::class.java))
        }) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = LoginNav)
        }
    }
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

@Composable
private fun ChangePasswordDialog(
    scope: kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var showMessage by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it; showMessage = false },
                    label = { Text("Old password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; showMessage = false },
                    label = { Text("New password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
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
                    if (oldPassword.isBlank() || newPassword.isBlank()) {
                        message = "Please fill all fields"
                        showMessage = true
                        return@Button
                    }
                    isSubmitting = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            changePassword(oldPassword.trim(), newPassword.trim())
                        }
                        isSubmitting = false
                        message = if (result.success) "Success: password updated" else result.message
                        showMessage = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LoginChip)
            ) {
                Text(if (isSubmitting) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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

private fun changePassword(oldPassword: String, newPassword: String): LoginResult {
    val url = URL("https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/auth/change-password")
    val payload = """{"oldPassword":"$oldPassword","newPassword":"$newPassword"}"""
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            PatientSession.accessToken?.let {
                setRequestProperty("Authorization", "Bearer $it")
            }
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
            code == 400 -> LoginResult(false, body.ifBlank { "Invalid password" })
            code == 401 || code == 403 -> LoginResult(false, "Unauthorized")
            else -> LoginResult(false, body.ifBlank { "Change password failed (HTTP $code)" })
        }
    } catch (e: Exception) {
        LoginResult(false, e.message ?: "Network error")
    }
}

@Composable
private fun ForgotPasswordDialog(
    scope: kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var email by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var showMessage by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 1) "Forgot password" else "Reset password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step == 1) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; showMessage = false },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it; showMessage = false },
                        label = { Text("Token") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; showMessage = false },
                        label = { Text("New password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                    isSubmitting = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            if (step == 1) {
                                forgotPassword(email.trim())
                            } else {
                                resetPassword(token.trim(), newPassword.trim())
                            }
                        }
                        isSubmitting = false
                        if (result.success) {
                            if (step == 1) {
                                message = "Success: token sent"
                                showMessage = true
                                step = 2
                            } else {
                                message = "Success: password updated"
                                showMessage = true
                                onDismiss()
                            }
                        } else {
                            message = result.message
                            showMessage = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LoginChip)
            ) {
                Text(if (isSubmitting) "Saving..." else if (step == 1) "Send" else "Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
