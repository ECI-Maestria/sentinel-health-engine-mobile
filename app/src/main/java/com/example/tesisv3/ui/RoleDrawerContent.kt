package com.example.tesisv3.ui

import com.example.tesisv3.*

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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Destinations ──────────────────────────────────────────────────────────────

enum class DrawerDestination {
    DASHBOARD,
    MEDICATIONS,
    REPORTS,
    GROUPS,
    PROFILE,
    SETTINGS,
    LOGOUT,
    DOCTOR_PANEL,
    DOCTOR_PATIENTS,   // → DoctorPatientsListActivity
    DOCTOR_CALENDAR,
    DOCTOR_REPORTS
}

// ── Role accent colors ────────────────────────────────────────────────────────

private val AccentDoctor      = Color(0xFF2F8A5B)   // green
private val AccentPatient     = Color(0xFF3B82F6)   // blue
private val AccentCaretaker   = Color(0xFF1361A8)   // dark blue

private val AccentDoctorSoft    = Color(0xFFE6F3EC)
private val AccentPatientSoft   = Color(0xFFEBF3FF)
private val AccentCaretakerSoft = Color(0xFFE3EDF8)

private val AccentDoctorIcon    = Color(0xFFCDEBDA)
private val AccentPatientIcon   = Color(0xFFD0E5FF)
private val AccentCaretakerIcon = Color(0xFFCCDFF3)

private val DrawerText    = Color(0xFF2E3F35)
private val DrawerSurface = Color.White

// ── RoleDrawerContent ─────────────────────────────────────────────────────────

@Composable
fun RoleDrawerContent(
    role: String?,
    current: DrawerDestination?,
    onItemClick: (DrawerDestination) -> Unit
) {
    val isDoctor    = role?.equals("DOCTOR",    ignoreCase = true) == true
    val isCaretaker = role?.equals("CARETAKER", ignoreCase = true) == true

    val name = PatientSession.currentUser?.fullName
        ?: if (isDoctor) "Doctor" else if (isCaretaker) "Cuidador" else "Paciente"

    val subtitle = when {
        isDoctor    -> "Doctor"
        isCaretaker -> "Cuidador"
        else        -> "Paciente"
    }

    val headerColor = when {
        isDoctor    -> AccentDoctor
        isCaretaker -> AccentCaretaker
        else        -> AccentPatient
    }

    val accentSoft = when {
        isDoctor    -> AccentDoctorSoft
        isCaretaker -> AccentCaretakerSoft
        else        -> AccentPatientSoft
    }

    val accentIcon = when {
        isDoctor    -> AccentDoctorIcon
        isCaretaker -> AccentCaretakerIcon
        else        -> AccentPatientIcon
    }

    val items = when {
        isDoctor -> listOf(
            DrawerItem("Mi Panel",        Icons.Filled.Home,             DrawerDestination.DOCTOR_PANEL),
            DrawerItem("Mis Pacientes",   Icons.Outlined.Group,          DrawerDestination.DOCTOR_PATIENTS),
            DrawerItem("Calendario",      Icons.Outlined.CalendarToday,  DrawerDestination.DOCTOR_CALENDAR),
            DrawerItem("Reportes",        Icons.Outlined.Description,    DrawerDestination.DOCTOR_REPORTS),
            DrawerItem("Configuración",   Icons.Outlined.Settings,       DrawerDestination.SETTINGS),
            DrawerItem("Cerrar sesión",   Icons.Outlined.Logout,         DrawerDestination.LOGOUT, danger = true)
        )
        isCaretaker -> listOf(
            DrawerItem("Mi Panel",        Icons.Filled.Home,             DrawerDestination.DOCTOR_PANEL),
            DrawerItem("Mis Pacientes",   Icons.Outlined.Group,          DrawerDestination.DOCTOR_PATIENTS),
            DrawerItem("Calendario",      Icons.Outlined.CalendarToday,  DrawerDestination.DOCTOR_CALENDAR),
            DrawerItem("Medicamentos",    Icons.Outlined.MedicalServices,DrawerDestination.MEDICATIONS),
            DrawerItem("Configuración",   Icons.Outlined.Settings,       DrawerDestination.SETTINGS),
            DrawerItem("Cerrar sesión",   Icons.Outlined.Logout,         DrawerDestination.LOGOUT, danger = true)
        )
        else -> listOf(
            DrawerItem("Signos Vitales",  Icons.Outlined.FavoriteBorder, DrawerDestination.DASHBOARD),
            DrawerItem("Medicamentos",    Icons.Outlined.MedicalServices,DrawerDestination.MEDICATIONS),
            DrawerItem("Reportes PDF",    Icons.Outlined.Description,    DrawerDestination.REPORTS),
            DrawerItem("Mis Doctores",    Icons.Outlined.Group,          DrawerDestination.GROUPS),
            DrawerItem("Mi Perfil",       Icons.Outlined.Person,         DrawerDestination.PROFILE),
            DrawerItem("Configuración",   Icons.Outlined.Settings,       DrawerDestination.SETTINGS),
            DrawerItem("Cerrar sesión",   Icons.Outlined.Logout,         DrawerDestination.LOGOUT, danger = true)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DrawerSurface)
    ) {
        DrawerHeader(
            name        = name,
            subtitle    = subtitle,
            headerColor = headerColor,
            showHospital = isDoctor
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEach { item ->
                DrawerItemRow(
                    item        = item,
                    selected    = current == item.destination,
                    accentColor = headerColor,
                    accentSoft  = accentSoft,
                    accentIcon  = accentIcon,
                    onClick     = { onItemClick(item.destination) }
                )
            }
        }
    }
}

@Composable
private fun DrawerHeader(
    name: String,
    subtitle: String,
    headerColor: Color,
    showHospital: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerColor)
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
        Text(name,     color = Color.White,                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color.White.copy(alpha = 0.85f),fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
private fun DrawerItemRow(
    item: DrawerItem,
    selected: Boolean,
    accentColor: Color,
    accentSoft: Color,
    accentIcon: Color,
    onClick: () -> Unit
) {
    val background  = if (selected) accentSoft else Color.Transparent
    val textColor   = when {
        item.danger -> Color(0xFFD64545)
        selected    -> accentColor
        else        -> DrawerText
    }
    val iconBg   = if (selected) accentIcon else Color(0xFFF1F2EE)
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
                    .background(iconBg),
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
        DrawerDestination.DASHBOARD        -> context.startActivity(Intent(context, DashboardActivity::class.java))
        DrawerDestination.MEDICATIONS      -> context.startActivity(Intent(context, CareActivity::class.java))
        DrawerDestination.REPORTS          -> Toast.makeText(context, "Reportes PDF pronto", Toast.LENGTH_SHORT).show()
        DrawerDestination.GROUPS           -> context.startActivity(Intent(context, GroupsActivity::class.java))
        DrawerDestination.PROFILE          -> Toast.makeText(context, "Perfil pronto", Toast.LENGTH_SHORT).show()
        DrawerDestination.SETTINGS         -> context.startActivity(Intent(context, SettingsActivity::class.java))
        DrawerDestination.DOCTOR_PANEL     -> context.startActivity(Intent(context, DoctorPatientsActivity::class.java))
        DrawerDestination.DOCTOR_PATIENTS  -> context.startActivity(Intent(context, DoctorPatientsListActivity::class.java))
        DrawerDestination.DOCTOR_CALENDAR  -> context.startActivity(Intent(context, CalendarActivity::class.java))
        DrawerDestination.DOCTOR_REPORTS   -> context.startActivity(Intent(context, ReportsActivity::class.java))
        DrawerDestination.LOGOUT           -> performLogout(context)
    }
}

fun performLogout(context: Context) {
    PatientSession.accessToken  = null
    PatientSession.refreshToken = null
    PatientSession.currentUser  = null
    PatientSession.patientId    = ""
    PatientSession.resetCode    = null
    val intent = Intent(context, LoginActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    context.startActivity(intent)
    if (context is ComponentActivity) context.finish()
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) return parts.first().take(2).uppercase()
    return (parts.first().take(1) + parts.last().take(1)).uppercase()
}
