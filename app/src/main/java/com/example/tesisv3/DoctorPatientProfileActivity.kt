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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DoctorPatientProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }
        super.onCreate(savedInstanceState)
        val name = intent.getStringExtra("patient_name") ?: "María García"
        val email = intent.getStringExtra("patient_email") ?: "maria.garcia@email.com"
        val initials = intent.getStringExtra("patient_initials") ?: "MG"
        val status = intent.getStringExtra("patient_status") ?: "Alerta"
        setContent {
            MaterialTheme {
                DoctorPatientProfileScreen(
                    name = name,
                    email = email,
                    initials = initials,
                    status = status,
                    onBack = { finish() }
                )
            }
        }
    }
}

private val ProfileBg = Color(0xFFF3F7F4)
private val ProfileText = Color(0xFF2E3F35)
private val ProfileMuted = Color(0xFF7B8C81)
private val ProfileAccent = Color(0xFF2F8A5B)
private val ProfileAccentSoft = Color(0xFFD8F0E2)
private val ProfileWarning = Color(0xFFD64545)
private val ProfileChipBg = Color(0xFFE8F4EC)
private val ProfileVitals = Color(0xFF2F8A5B)
private val ProfileVitalsDark = Color(0xFF2A7C52)

@Composable
private fun DoctorPatientProfileScreen(
    name: String,
    email: String,
    initials: String,
    status: String,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(containerColor = ProfileBg) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ProfileBg)
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ProfileText)
                }
                Text("Perfil del Paciente", color = ProfileText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = ProfileText)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(ProfileAccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = ProfileAccent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Text(name, color = ProfileText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(email, color = ProfileMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusBadge(
                        label = if (status == "Alerta") "Alerta activa" else status,
                        textColor = ProfileWarning,
                        background = Color(0xFFFFE9E9)
                    )
                    StatusBadge(
                        label = "Activo",
                        textColor = ProfileAccent,
                        background = ProfileChipBg
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ProfileVitals),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("ÚLTIMOS SIGNOS VITALES", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        VitalTile("Frec. Cardíaca", "82", "bpm", modifier = Modifier.weight(1f))
                        VitalTile("Presión", "160", "/95", modifier = Modifier.weight(1f), highlight = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        VitalTile("Temperatura", "37.1", "°C", modifier = Modifier.weight(1f))
                        VitalTile("SpO2", "97", "%", modifier = Modifier.weight(1f))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileTab("Medicamentos", selectedTab == 0) { selectedTab = 0 }
                ProfileTab("Citas", selectedTab == 1) { selectedTab = 1 }
                ProfileTab("Cuidadores", selectedTab == 2) { selectedTab = 2 }
                ProfileTab("Alertas", selectedTab == 3) { selectedTab = 3 }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(120.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ProfileMuted.copy(alpha = 0.35f))
                )
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MedicationRow(
                        name = "Insulina",
                        detail = "1 dosis · Daily 8:57 PM",
                        status = "Due"
                    )
                    DividerLineLight()
                    MedicationRow(
                        name = "Dolex",
                        detail = "500mg · Daily 4:24 PM",
                        status = "Due"
                    )
                }
            }

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = ProfileAccent),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+  Agregar Medicamento", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, textColor: Color, background: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VitalTile(
    title: String,
    value: String,
    suffix: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (highlight) ProfileVitalsDark else ProfileAccent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Text(suffix, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = if (selected) ProfileAccent else Color.White),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else ProfileMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MedicationRow(name: String, detail: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ProfileAccentSoft),
                contentAlignment = Alignment.Center
            ) {
                Text("✚", color = ProfileAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, color = ProfileText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(detail, color = ProfileMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF4D9))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(status, color = Color(0xFFCC7A00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DividerLineLight() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFE6ECE7))
    )
}
