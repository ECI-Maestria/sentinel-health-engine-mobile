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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResetPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ResetPasswordScreen(onBack = { finish() })
            }
        }
    }
}

private val ResetBg = Color(0xFFF3F7F4)
private val ResetText = Color(0xFF2E3F35)
private val ResetMuted = Color(0xFF7B8C81)
private val ResetAccent = Color(0xFF43A971)
private val ResetAccentSoft = Color(0xFFE3F4EA)

@Composable
private fun ResetPasswordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val cachedCode = PatientSession.resetCode

    Scaffold(containerColor = ResetBg) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ResetBg)
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
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ResetText)
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    text = "Nueva contraseña",
                    color = ResetText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(ResetAccentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = "Lock", tint = ResetAccent, modifier = Modifier.size(34.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Crea tu nueva contraseña",
                color = ResetText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Elige una contraseña segura y diferente a las anteriores.",
                color = ResetMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(18.dp))

            Text("Nueva contraseña", color = ResetText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it; message = "" },
                placeholder = { Text("••••••••") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Toggle"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(14.dp))

            Text("Confirmar contraseña", color = ResetText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; message = "" },
                placeholder = { Text("••••••••") },
                singleLine = true,
                visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirm = !showConfirm }) {
                        Icon(
                            imageVector = if (showConfirm) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Toggle"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(12.dp))

            PasswordStrengthRow(password = newPassword)

            Spacer(Modifier.height(8.dp))

            PasswordRequirementList(password = newPassword)

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    if (newPassword.isBlank()) {
                        message = "Ingresa una nueva contraseña"
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        message = "Las contraseñas no coinciden"
                        return@Button
                    }
                    val code = cachedCode
                    if (code.isNullOrBlank()) {
                        message = "Código no encontrado, vuelve a solicitarlo"
                        return@Button
                    }
                    isSubmitting = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            resetPasswordWithCode(code, newPassword.trim())
                        }
                        isSubmitting = false
                        if (result.success) {
                            PatientSession.resetCode = null
                            val intent = android.content.Intent(
                                context,
                                LoginActivity::class.java
                            ).apply {
                                addFlags(
                                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                )
                            }
                            context.startActivity(intent)
                        } else {
                            message = result.message
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ResetAccent),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isSubmitting) "Guardando..." else "Guardar contraseña",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(message, color = Color(0xFFD35C55), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PasswordStrengthRow(password: String) {
    val (label, strength, color) = passwordStrength(password)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Seguridad de la contraseña", color = ResetMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE1E8E4))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = strength)
                    .height(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun PasswordRequirementList(password: String) {
    val rules = listOf(
        "8 o más caracteres" to (password.length >= 8),
        "Letras mayúsculas" to password.any { it.isUpperCase() },
        "Números" to password.any { it.isDigit() },
        "Caracteres especiales (!@#$)" to password.any { !it.isLetterOrDigit() }
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Requisitos:", color = ResetMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        rules.forEach { (label, ok) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = if (ok) ResetAccent else Color(0xFFBFC8C2),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    color = if (ok) ResetText else Color(0xFFBFC8C2),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun passwordStrength(password: String): Triple<String, Float, Color> {
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return when (score) {
        0, 1 -> Triple("Débil", 0.25f, Color(0xFFE57373))
        2 -> Triple("Media", 0.5f, Color(0xFFFFB74D))
        3 -> Triple("Fuerte", 0.75f, ResetAccent)
        else -> Triple("Muy fuerte", 1f, ResetAccent)
    }
}
