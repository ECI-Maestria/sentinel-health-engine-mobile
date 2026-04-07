package com.example.tesisv3

import android.os.Build
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class CaretakerRegistrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CaretakerRegistrationScreen(onBack = { finish() })
            }
        }
    }
}

private val CareBg = Color(0xFFF3F7F4)
private val CareText = Color(0xFF2E3F35)
private val CareMuted = Color(0xFF7B8C81)
private val CareAccent = Color(0xFF43A971)
private val CareAccentSoft = Color(0xFFE3F4EA)
private val CareWarning = Color(0xFFFFB74D)
private val CareWarningBg = Color(0xFFFFF3E0)
private val CareSuccess = Color(0xFF2F8A5B)
private val CareSuccessDark = Color(0xFF2A7C52)
private val CareSuccessSoft = Color(0xFFE4F3EA)
private val CareInfoSoft = Color(0xFFE9F2FF)
private val CareInfoText = Color(0xFF2F5FBF)

@Composable
private fun CaretakerRegistrationScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var createdName by remember { mutableStateOf("") }
    var createdEmail by remember { mutableStateOf("") }

    Scaffold(containerColor = CareBg) { innerPadding ->
        if (showSuccess) {
                CaretakerSuccessScreen(
                    name = createdName,
                    email = createdEmail,
                    onBackToLogin = {
                        val intent = android.content.Intent(context, LoginActivity::class.java).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CareBg)
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.Top
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
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = CareText)
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = "Registro de Cuidador",
                        color = CareText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CareWarningBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.WarningAmber, contentDescription = "Warning", tint = CareWarning)
                        Text(
                            text = "Importante: Podrás ingresar a la app, pero hasta no estar vinculado a un paciente, no podrás ver información de salud.",
                            color = CareText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text("Nombre", color = CareText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it; errorMessage = "" },
                    placeholder = { Text("Luis") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(Modifier.height(10.dp))

                Text("Apellido", color = CareText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it; errorMessage = "" },
                    placeholder = { Text("Pérez") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(Modifier.height(10.dp))

                Text("Correo electrónico", color = CareText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = "" },
                    placeholder = { Text("luis.perez@email.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(Modifier.height(12.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CareAccentSoft),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = "Info", tint = CareAccent)
                        Text(
                            text = "Recibirás tu contraseña temporal por correo electrónico una vez que te registres.",
                            color = CareAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (errorMessage.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFD35C55),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        if (firstName.isBlank() || lastName.isBlank() || email.isBlank()) {
                            errorMessage = "Por favor completa todos los campos"
                            return@Button
                        }
                        isSubmitting = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                registerCaretaker(firstName.trim(), lastName.trim(), email.trim())
                            }
                            isSubmitting = false
                            if (result.success) {
                                createdName = "${firstName.trim()} ${lastName.trim()}".trim()
                                createdEmail = email.trim()
                                showSuccess = true
                            } else {
                                errorMessage = result.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CareAccent),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSubmitting) "Creando..." else "Crear cuenta de Cuidador",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

private data class CaretakerResult(val success: Boolean, val message: String)

private fun registerCaretaker(firstName: String, lastName: String, email: String): CaretakerResult {
    val payload =
        """{"firstName":"${escapeJson(firstName)}","lastName":"${escapeJson(lastName)}","email":"${escapeJson(email)}"}"""
    val url = URL("https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/caretakers")
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        when {
            code in 200..299 -> CaretakerResult(true, "")
            else -> CaretakerResult(false, body.ifBlank { "Request failed (HTTP $code)" })
        }
    } catch (e: Exception) {
        CaretakerResult(false, e.message ?: "Network error")
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

@Composable
private fun CaretakerSuccessScreen(
    name: String,
    email: String,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CareSuccess)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(RoundedCornerShape(42.dp))
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Success", tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("¡Cuenta creada!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Tu cuenta de cuidador ha sido creada. Espera a que un médico o paciente te vincule para acceder a la información de salud.",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(22.dp))

        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CareSuccessSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialsOf(name),
                        color = CareSuccess,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(name.ifBlank { "Nuevo cuidador" }, color = CareText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(email, color = CareMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFEDE7FF))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Cuidador", color = Color(0xFF5C4CCB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(6.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CareInfoSoft),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                        ) {
                            Text("✉", modifier = Modifier.align(Alignment.Center), color = CareInfoText, fontSize = 12.sp)
                        }
                        Text(
                            text = "Tu contraseña temporal fue enviada a tu correo electrónico. Revisa tu bandeja de entrada.",
                            color = CareInfoText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = onBackToLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = CareSuccess),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ir al inicio de sesión", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    if (parts.isEmpty()) return "C"
    val first = parts.first().take(1)
    val second = parts.getOrNull(1)?.take(1) ?: ""
    return (first + second).uppercase()
}
