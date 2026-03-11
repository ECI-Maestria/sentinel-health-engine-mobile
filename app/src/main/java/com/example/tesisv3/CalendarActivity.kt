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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class CalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CalendarScreen(onBack = { finish() })
            }
        }
    }
}

private val CalendarBackground = Color(0xFFF6F7F2)
private val CalendarCard = Color(0xFFFFFFFF)
private val CalendarText = Color(0xFF2E3F35)
private val CalendarMuted = Color(0xFF7B8C81)
private val CalendarChip = Color(0xFF5BCB90)
private val CalendarChipAlt = Color(0xFFE1F2E6)
private val CalendarAccent = Color(0xFF4FA6A5)
private val CalendarNav = Color(0xFF5A7A63)

@Composable
private fun CalendarScreen(onBack: () -> Unit) {
    var selectedNav by remember { mutableIntStateOf(2) }

    Scaffold(
        containerColor = CalendarBackground,
        bottomBar = {
            AppBottomNav(
                current = BottomNavDestination.CALENDAR,
                modifier = Modifier,
                indicatorColor = CalendarChipAlt,
                selectedColor = CalendarNav,
                unselectedColor = CalendarNav.copy(alpha = 0.5f)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(CalendarBackground),
            contentPadding = PaddingValues(
                start = 18.dp,
                top = innerPadding.calculateTopPadding() + 12.dp,
                end = 18.dp,
                bottom = innerPadding.calculateBottomPadding() + 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { CalendarTopBar(onBack) }

            item { CalendarCard() }

            item {
                AppointmentCard(
                    title = "Dental Cleaning",
                    detail = "Mon, Sep 8 * 2:30 PM",
                    icon = Icons.Outlined.MedicalServices
                )
            }

            item {
                AppointmentCard(
                    title = "Refill Prescription",
                    detail = "Mon, Sep 29 * 5:00 PM",
                    icon = Icons.Outlined.Edit
                )
            }
        }
    }
}

@Composable
private fun CalendarTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.Menu, contentDescription = "Back", tint = CalendarNav)
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.EventNote, contentDescription = "Brand", tint = CalendarNav)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = CalendarNav)
        }
    }
}

@Composable
private fun CalendarCard() {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = CalendarCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("January 2022", color = CalendarText, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = CalendarChip),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Add\nAppointment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Prev", tint = CalendarMuted)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next", tint = CalendarMuted)
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = CalendarChipAlt)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { day ->
                    Text(day, color = CalendarMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            val rows = listOf(
                listOf("1", "2", "3", "4", "5", "6", "7"),
                listOf("8", "9", "10", "11", "12", "13", "14"),
                listOf("15", "16", "17", "18", "19", "20", "21"),
                listOf("22", "23", "24", "25", "26", "27", "28"),
                listOf("29", "30", "31", "", "", "", "")
            )

            rows.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { day ->
                        if (day == "1") {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(CalendarAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(day, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(day, color = CalendarText, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun AppointmentCard(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CalendarChipAlt,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = CalendarNav)
            }

            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(title, color = CalendarText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(detail, color = CalendarMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

