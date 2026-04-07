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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ForgotPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ForgotPasswordScreen(onBack = { finish() })
            }
        }
    }
}

private val ForgotBg = Color(0xFFF3F7F4)
private val ForgotText = Color(0xFF2E3F35)
private val ForgotMuted = Color(0xFF7B8C81)
private val ForgotAccent = Color(0xFF43A971)
private val ForgotAccentSoft = Color(0xFFE3F4EA)

@Composable
private fun ForgotPasswordScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(1) }
    var email by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Scaffold(containerColor = ForgotBg) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ForgotBg)
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ForgotText)
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    text = if (step == 1) "Recuperar Contraseña" else "Restablecer Contraseña",
                    color = ForgotText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(26.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(ForgotAccentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Lock",
                    tint = ForgotAccent,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (step == 1) "¿Olvidaste tu contraseña?" else "Ingresa tu código",
                color = ForgotText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (step == 1)
                    "Ingresa tu correo y te enviaremos un código para restablecer tu contraseña."
                else
                    "Ingresa el token recibido y tu nueva contraseña.",
                color = ForgotMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(18.dp))

            if (step == 1) {
                Text("Correo electrónico", color = ForgotText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; message = "" },
                    placeholder = { Text("tu@correo.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            } else {
                Text("Token", color = ForgotText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it; message = "" },
                    placeholder = { Text("Código") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text("Nueva contraseña", color = ForgotText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; message = "" },
                    placeholder = { Text("••••••••") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    if (step == 1 && email.isBlank()) {
                        message = "Por favor ingresa tu correo"
                        return@Button
                    }
                    if (step == 2 && (token.isBlank() || newPassword.isBlank())) {
                        message = "Completa los campos requeridos"
                        return@Button
                    }
                    isSubmitting = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            if (step == 1) {
                                requestForgotPassword(email.trim())
                            } else {
                                requestResetPassword(token.trim(), newPassword.trim())
                            }
                        }
                        isSubmitting = false
                        if (result.success) {
                            if (step == 1) {
                                message = "El código fue enviado a tu correo."
                                step = 2
                            } else {
                                message = "Contraseña actualizada. Inicia sesión."
                                onBack()
                            }
                        } else {
                            message = result.message
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForgotAccent),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isSubmitting) "Enviando..." else if (step == 1) "Enviar código" else "Restablecer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(10.dp))
            if (message.isNotBlank()) {
                Text(
                    text = message,
                    color = if (message.startsWith("El código") || message.startsWith("Contraseña")) ForgotAccent else Color(0xFFD35C55),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (step == 1) {
                Text(
                    text = "El código expira en 1 hora. Revisa tu bandeja de entrada.",
                    color = ForgotMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private data class ForgotResult(val success: Boolean, val message: String)

private fun requestForgotPassword(email: String): ForgotResult {
    val url = URL("https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/auth/forgot-password")
    val payload = """{"email":"${escapeJson(email.trim())}"}"""
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
            code in 200..299 -> ForgotResult(true, "")
            else -> ForgotResult(false, body.ifBlank { "Request failed (HTTP $code)" })
        }
    } catch (e: Exception) {
        ForgotResult(false, e.message ?: "Network error")
    }
}

private fun requestResetPassword(token: String, newPassword: String): ForgotResult {
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
            code in 200..299 -> ForgotResult(true, "")
            else -> ForgotResult(false, body.ifBlank { "Reset failed (HTTP $code)" })
        }
    } catch (e: Exception) {
        ForgotResult(false, e.message ?: "Network error")
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
