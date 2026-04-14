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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
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
                    text = "Recuperar Contraseña",
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
                text = "¿Olvidaste tu contraseña?",
                color = ForgotText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Ingresa tu correo y te enviaremos un código para restablecer tu contraseña.",
                color = ForgotMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(18.dp))

            Text("Correo electrónico", color = ForgotText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; message = "" },
                placeholder = { Text("tu@correo.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    if (email.isBlank()) {
                        message = "Por favor ingresa tu correo"
                        return@Button
                    }
                    isSubmitting = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            requestForgotPassword(email.trim())
                        }
                        isSubmitting = false
                        if (result.success) {
                            val intent = android.content.Intent(
                                context,
                                VerifyResetCodeActivity::class.java
                            ).apply {
                                putExtra("email", email.trim())
                            }
                            context.startActivity(intent)
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
                    text = if (isSubmitting) "Enviando..." else "Enviar código",
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
            } else {
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
