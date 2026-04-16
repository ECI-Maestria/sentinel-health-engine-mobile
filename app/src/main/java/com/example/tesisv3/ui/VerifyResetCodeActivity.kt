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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifyResetCodeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }
        super.onCreate(savedInstanceState)
        val email = intent.getStringExtra("email") ?: ""
        setContent {
            MaterialTheme {
                VerifyResetCodeScreen(
                    email = email,
                    onBack = { finish() },
                    onVerified = {
                        val intent = android.content.Intent(this, ResetPasswordActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

private val VerifyBg = Color(0xFFF3F7F4)
private val VerifyText = Color(0xFF2E3F35)
private val VerifyMuted = Color(0xFF7B8C81)
private val VerifyAccent = Color(0xFF43A971)
private val VerifyAccentSoft = Color(0xFFE3F4EA)

@Composable
private fun VerifyResetCodeScreen(
    email: String,
    onBack: () -> Unit,
    onVerified: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var secondsLeft by remember { mutableStateOf(60) }
    var isResending by remember { mutableStateOf(false) }

    LaunchedEffect(secondsLeft) {
        if (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

    Scaffold(containerColor = VerifyBg) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(VerifyBg)
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
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = VerifyText)
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    text = "Verificar código",
                    color = VerifyText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(26.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(VerifyAccentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mail,
                    contentDescription = "Mail",
                    tint = VerifyAccent,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Revisa tu correo",
                color = VerifyText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Enviamos un código de 6 dígitos a\n$email",
                color = VerifyMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            CodeInputRow(code = code, onCodeChange = { value ->
                val filtered = value.filter { it.isDigit() }.take(6)
                code = filtered
                message = ""
            })

            Spacer(Modifier.height(10.dp))
            Text(
                text = "Reenviar código en 00:${secondsLeft.toString().padStart(2, '0')}",
                color = VerifyMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    if (code.length < 6) {
                        message = "Ingresa el código completo"
                        return@Button
                    }
                    isSubmitting = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { verifyResetCode(code) }
                        isSubmitting = false
                        if (result.success) {
                            PatientSession.resetCode = code
                            onVerified()
                        } else {
                            message = result.message
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VerifyAccent),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isSubmitting) "Verificando..." else "Verificar código",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("¿No recibiste el código?", color = VerifyMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                TextButton(
                    onClick = {
                        if (isResending || secondsLeft > 0) return@TextButton
                        isResending = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { requestForgotPassword(email) }
                            isResending = false
                            if (result.success) {
                                message = "Código reenviado."
                                secondsLeft = 60
                            } else {
                                message = result.message
                            }
                        }
                    },
                    contentPadding = PaddingValues(0.dp),
                    enabled = secondsLeft == 0 && !isResending
                ) {
                    Text(
                        text = if (isResending) "Reenviando..." else "Reenviar",
                        color = if (secondsLeft == 0) VerifyAccent else VerifyMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(message, color = Color(0xFFD35C55), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CodeInputRow(code: String, onCodeChange: (String) -> Unit) {
    BasicTextField(
        value = code,
        onValueChange = onCodeChange,
        modifier = Modifier.fillMaxWidth(),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(6) { index ->
                    val char = code.getOrNull(index)?.toString() ?: ""
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(char, color = VerifyText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
}
