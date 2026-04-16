package com.example.tesisv3.ui

import com.example.tesisv3.*

import com.example.tesisv3.network.*

import android.content.Intent
import android.os.Build
import android.util.Log
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

private val NpBackground  = Color(0xFFF0F4EF)
private val NpText        = Color(0xFF1A2E20)
private val NpMuted       = Color(0xFF7A8C7E)
private val NpGreen       = Color(0xFF2D6A4F)
private val NpGreenLight  = Color(0xFF5BCB90)
private val NpGreenChip   = Color(0xFFDDEFE4)
private val NpWarnBg      = Color(0xFFFFF8E1)
private val NpWarnIcon    = Color(0xFFFF8F00)
private val NpCard        = Color.White
private val NpNav         = Color(0xFF58725E)

class CaretakerNoPatientsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CaretakerNoPatientsScreen(
                    onLogout = {
                        PatientSession.accessToken = null
                        PatientSession.refreshToken = null
                        PatientSession.currentUser = null
                        val intent = Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    },
                    onPatientsFound = {
                        startActivity(Intent(this, DoctorPatientsActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun CaretakerNoPatientsScreen(
    onLogout: () -> Unit,
    onPatientsFound: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var checkMessage by remember { mutableStateOf<String?>(null) }

    val email = PatientSession.currentUser?.email ?: ""

    Scaffold(containerColor = NpBackground) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NpBackground),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                NpTopBar()
                Spacer(Modifier.height(48.dp))
            }

            item {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(NpWarnBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = NpWarnIcon,
                        modifier = Modifier.size(52.dp)
                    )
                }
                Spacer(Modifier.height(28.dp))
            }

            item {
                Text(
                    text = "No estás vinculado a ningún paciente",
                    color = NpText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Para ver información de salud, un médico o paciente debe vincularte a su perfil usando tu correo electrónico.",
                    color = NpMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(32.dp))
            }

            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = NpCard,
                    shadowElevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "¿Cómo vincularse?",
                            color = NpText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(14.dp))
                        HowToStep(
                            number = 1,
                            text = "Comparte tu correo con el médico o paciente"
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                        Spacer(Modifier.height(12.dp))
                        HowToStep(
                            number = 2,
                            text = "El médico te agregará como cuidador del paciente"
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                        Spacer(Modifier.height(12.dp))
                        HowToStep(
                            number = 3,
                            text = "Recibirás una notificación y podrás ver los datos"
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
            }

            item {
                Row(horizontalArrangement = Arrangement.Center) {
                    Text("Tu correo: ", color = NpMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(email, color = NpGreenLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(32.dp))
            }

            item {
                if (checkMessage != null) {
                    Text(
                        text = checkMessage ?: "",
                        color = NpMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Button(
                    onClick = {
                        if (isChecking) return@Button
                        isChecking = true
                        checkMessage = null
                        scope.launch {
                            val patients = withContext(Dispatchers.IO) {
                                fetchCaretakerPatientIds()
                            }
                            isChecking = false
                            if (patients.isNotEmpty()) {
                                PatientSession.patientId = patients.first()
                                onPatientsFound()
                            } else {
                                checkMessage = "Aún no estás vinculado a ningún paciente."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NpGreenLight),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isChecking) "Verificando..." else "Verificar vinculación",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(14.dp))
                TextButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Cerrar sesión",
                        color = NpMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun NpTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholders on both sides to keep title perfectly centered
        Box(modifier = Modifier.size(48.dp))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SENTINEL HEALTH",
                color = NpMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Mi Panel",
                color = NpText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.size(48.dp))
    }
}


@Composable
private fun HowToStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(NpGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = NpText,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

fun fetchCaretakerPatientIds(): List<String> {
    val token = PatientSession.accessToken ?: return emptyList()
    val url = URL(
        "${ApiConstants.USER_SERVICE}" +
                "/v1/caretakers/me/patients"
    )
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 10000
            readTimeout = 10000
        }
        val code = conn.responseCode
        val body = BufferedReader(InputStreamReader(
            if (code in 200..299) conn.inputStream else conn.errorStream
        )).use { it.readText() }
        conn.disconnect()
        if (code !in 200..299 || body.isBlank()) return emptyList()

        val ids = mutableListOf<String>()
        try {
            val arr = when {
                body.trimStart().startsWith("[") -> JSONArray(body)
                else -> JSONObject(body).let { obj ->
                    obj.optJSONArray("patients")
                        ?: obj.optJSONArray("data")
                        ?: JSONArray()
                }
            }
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val id = item.optString("patientId").ifBlank { item.optString("id") }
                if (id.isNotBlank()) ids.add(id)
            }
        } catch (e: Exception) {
            Log.e("CaretakerNoPatients", "Failed to parse patient array", e)
        }
        ids
    } catch (e: Exception) {
        Log.e("CaretakerNoPatients", "fetchPatientIds failed", e)
        emptyList()
    }
}
