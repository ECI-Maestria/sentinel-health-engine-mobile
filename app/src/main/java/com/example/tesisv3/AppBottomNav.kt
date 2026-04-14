package com.example.tesisv3

import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable

fun isWearableConnected(context: android.content.Context): Boolean = try {
    Tasks.await(Wearable.getNodeClient(context).connectedNodes).isNotEmpty()
} catch (_: Exception) { false }

@Composable
fun WatchStatusIcon(wearableConnected: Boolean?, modifier: Modifier = Modifier) {
    val tint = when (wearableConnected) {
        true  -> Color(0xFF4CAF50)
        false -> Color(0xFFD64545)
        null  -> Color(0xFFB0B8B2)
    }
    val desc = when (wearableConnected) {
        true  -> "Reloj conectado"
        false -> "Reloj desconectado"
        null  -> "Verificando reloj"
    }
    Icon(
        imageVector = Icons.Filled.Watch,
        contentDescription = desc,
        tint = tint,
        modifier = modifier
    )
}


enum class BottomNavDestination {
    DASHBOARD,
    GROUPS,
    CALENDAR,
    CARE,
    REPORTS
}

@Composable
fun AppBottomNav(
    current: BottomNavDestination,
    modifier: Modifier = Modifier,
    indicatorColor: Color,
    selectedColor: Color,
    unselectedColor: Color
) {
    val context = LocalContext.current
    val role = PatientSession.currentUser?.role?.uppercase()

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 4.dp,
        modifier = modifier
    ) {
        val isDoctor = role == "DOCTOR"
        val isCaretaker = role == "CARETAKER"
        val items = if (isDoctor) {
            listOf(
                BottomNavDestination.DASHBOARD to Icons.Filled.Home,
                BottomNavDestination.GROUPS to Icons.Filled.Groups,
                BottomNavDestination.CALENDAR to Icons.Filled.DateRange,
                BottomNavDestination.REPORTS to Icons.Outlined.Description
            )
        } else {
            listOf(
                BottomNavDestination.DASHBOARD to Icons.Filled.Home,
                BottomNavDestination.GROUPS to Icons.Filled.Groups,
                BottomNavDestination.CALENDAR to Icons.Filled.DateRange,
                BottomNavDestination.CARE to Icons.Filled.LocalHospital
            )
        }

        items.forEach { (dest, icon) ->
            val isSelected = current == dest
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (isSelected) return@NavigationBarItem
                    val clickRole = PatientSession.currentUser?.role?.trim()?.uppercase()
                    val doctorClick = clickRole == "DOCTOR"
                    val caretakerClick = clickRole == "CARETAKER"
                    if (doctorClick) {
                        when (dest) {
                            BottomNavDestination.DASHBOARD,
                            BottomNavDestination.CARE -> {
                                context.startActivity(Intent(context, DoctorPatientsActivity::class.java))
                            }
                            BottomNavDestination.GROUPS -> {
                                context.startActivity(Intent(context, DoctorPatientsListActivity::class.java))
                            }
                            BottomNavDestination.CALENDAR -> {
                                context.startActivity(Intent(context, CalendarActivity::class.java))
                            }
                            BottomNavDestination.REPORTS -> {
                                context.startActivity(Intent(context, ReportsActivity::class.java))
                            }
                        }
                    } else if (caretakerClick) {
                        when (dest) {
                            BottomNavDestination.DASHBOARD -> {
                                context.startActivity(Intent(context, DoctorPatientsActivity::class.java))
                            }
                            BottomNavDestination.GROUPS -> {
                                context.startActivity(Intent(context, DoctorPatientsListActivity::class.java))
                            }
                            BottomNavDestination.CALENDAR -> {
                                context.startActivity(Intent(context, CalendarActivity::class.java))
                            }
                            BottomNavDestination.CARE -> {
                                context.startActivity(Intent(context, CareActivity::class.java))
                            }
                            BottomNavDestination.REPORTS -> { /* caretaker never sees this tab */ }
                        }
                    } else {
                        when (dest) {
                            BottomNavDestination.DASHBOARD -> {
                                context.startActivity(Intent(context, DashboardActivity::class.java))
                            }
                            BottomNavDestination.GROUPS -> {
                                context.startActivity(Intent(context, GroupsActivity::class.java))
                            }
                            BottomNavDestination.CALENDAR -> {
                                context.startActivity(Intent(context, CalendarActivity::class.java))
                            }
                            BottomNavDestination.CARE -> {
                                context.startActivity(Intent(context, CareActivity::class.java))
                            }
                            BottomNavDestination.REPORTS -> { /* patients never see this tab */ }
                        }
                    }
                },
                icon = { Icon(icon, contentDescription = null) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedColor,
                    unselectedIconColor = unselectedColor,
                    indicatorColor = indicatorColor
                )
            )
        }
    }
}
