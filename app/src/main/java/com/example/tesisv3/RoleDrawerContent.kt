package com.example.tesisv3

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DrawerDestination {
    DASHBOARD,
    MEDICATIONS,
    REPORTS,
    GROUPS,
    PROFILE,
    SETTINGS,
    LOGOUT,
    DOCTOR_PANEL,
    DOCTOR_CALENDAR,
    DOCTOR_REPORTS
}

private val DrawerGreen = Color(0xFF2F8A5B)
private val DrawerGreenSoft = Color(0xFFE6F3EC)
private val DrawerText = Color(0xFF2E3F35)
private val DrawerMuted = Color(0xFF7B8C81)
private val DrawerSurface = Color.White

@Composable
fun RoleDrawerContent(
    role: String?,
    current: DrawerDestination?,
    onItemClick: (DrawerDestination) -> Unit
) {
    val isDoctor = role?.equals("DOCTOR", ignoreCase = true) == true
    val name = PatientSession.currentUser?.fullName
        ?: if (isDoctor) "Dra. Ana Martinez" else "Paciente"
    val subtitle = if (isDoctor) "Medico General" else "Paciente"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DrawerSurface)
    ) {
        DrawerHeader(name = name, subtitle = subtitle, showHospital = isDoctor)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val items = if (isDoctor) {
                listOf(
                    DrawerItem("Panel del Doctor", Icons.Outlined.Person, DrawerDestination.DOCTOR_PANEL),
                    DrawerItem("Pacientes", Icons.Outlined.Person, DrawerDestination.DOCTOR_PANEL),
                    DrawerItem("Calendario", Icons.Outlined.CalendarToday, DrawerDestination.DOCTOR_CALENDAR),
                    DrawerItem("Reportes", Icons.Outlined.Description, DrawerDestination.DOCTOR_REPORTS),
                    DrawerItem("Configuracion", Icons.Outlined.Settings, DrawerDestination.SETTINGS),
                    DrawerItem("Cerrar sesion", Icons.Outlined.Logout, DrawerDestination.LOGOUT, danger = true)
                )
            } else {
                listOf(
                    DrawerItem("Signos Vitales", Icons.Outlined.FavoriteBorder, DrawerDestination.DASHBOARD),
                    DrawerItem("Medicamentos", Icons.Outlined.MedicalServices, DrawerDestination.MEDICATIONS),
                    DrawerItem("Reportes PDF", Icons.Outlined.Description, DrawerDestination.REPORTS),
                    DrawerItem("Mis Doctores", Icons.Outlined.Group, DrawerDestination.GROUPS),
                    DrawerItem("Mi Perfil", Icons.Outlined.Person, DrawerDestination.PROFILE),
                    DrawerItem("Configuracion", Icons.Outlined.Settings, DrawerDestination.SETTINGS),
                    DrawerItem("Cerrar sesion", Icons.Outlined.Logout, DrawerDestination.LOGOUT, danger = true)
                )
            }

            items.forEach { item ->
                DrawerItemRow(
                    item = item,
                    selected = current == item.destination,
                    onClick = { onItemClick(item.destination) }
                )
            }
        }
    }
}

@Composable
private fun DrawerHeader(name: String, subtitle: String, showHospital: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DrawerGreen)
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialsOf(name),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        if (showHospital) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Hospital Central", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DrawerItemRow(item: DrawerItem, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) DrawerGreenSoft else Color.Transparent
    val textColor = if (selected) DrawerGreen else if (item.danger) Color(0xFFD64545) else DrawerText
    val iconBackground = if (selected) Color(0xFFCDEBDA) else Color(0xFFF1F2EE)
    val iconTint = if (item.danger) Color(0xFFD64545) else DrawerText

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = background),
        shape = RoundedCornerShape(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = item.label, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.size(12.dp))
            Text(item.label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private data class DrawerItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val destination: DrawerDestination,
    val danger: Boolean = false
)

fun handleDrawerNavigation(context: Context, destination: DrawerDestination) {
    when (destination) {
        DrawerDestination.DASHBOARD -> context.startActivity(Intent(context, DashboardActivity::class.java))
        DrawerDestination.MEDICATIONS -> context.startActivity(Intent(context, CareActivity::class.java))
        DrawerDestination.REPORTS -> Toast.makeText(context, "Reportes PDF pronto", Toast.LENGTH_SHORT).show()
        DrawerDestination.GROUPS -> context.startActivity(Intent(context, GroupsActivity::class.java))
        DrawerDestination.PROFILE -> Toast.makeText(context, "Perfil pronto", Toast.LENGTH_SHORT).show()
        DrawerDestination.SETTINGS -> context.startActivity(Intent(context, SettingsActivity::class.java))
        DrawerDestination.DOCTOR_PANEL -> context.startActivity(Intent(context, DoctorPatientsActivity::class.java))
        DrawerDestination.DOCTOR_CALENDAR -> context.startActivity(Intent(context, CalendarActivity::class.java))
        DrawerDestination.DOCTOR_REPORTS -> Toast.makeText(context, "Reportes pronto", Toast.LENGTH_SHORT).show()
        DrawerDestination.LOGOUT -> performLogout(context)
    }
}

fun performLogout(context: Context) {
    PatientSession.accessToken = null
    PatientSession.refreshToken = null
    PatientSession.currentUser = null
    PatientSession.patientId = ""
    PatientSession.resetCode = null
    val intent = Intent(context, LoginActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    context.startActivity(intent)
    if (context is ComponentActivity) {
        context.finish()
    }
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) return parts.first().take(2).uppercase()
    return (parts.first().take(1) + parts.last().take(1)).uppercase()
}
