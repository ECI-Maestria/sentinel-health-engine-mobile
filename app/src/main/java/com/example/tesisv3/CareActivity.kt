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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class CareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CareScreen(onBack = { finish() })
            }
        }
    }
}

private val CareBackground = Color(0xFFF6F7F2)
private val CareCard = Color(0xFFFFFFFF)
private val CareText = Color(0xFF2E3F35)
private val CareMuted = Color(0xFF7B8C81)
private val CareChip = Color(0xFF5BCB90)
private val CareChipAlt = Color(0xFFE1F2E6)
private val CareAccent = Color(0xFF4FA6A5)
private val CareNav = Color(0xFF5A7A63)
private val CareWarn = Color(0xFFE0A04B)
private val CareError = Color(0xFFE06A61)

@Composable
private fun CareScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = CareBackground,
        bottomBar = {
            AppBottomNav(
                current = BottomNavDestination.CARE,
                modifier = Modifier,
                indicatorColor = CareChipAlt,
                selectedColor = CareNav,
                unselectedColor = CareNav.copy(alpha = 0.5f)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(CareBackground),
            contentPadding = PaddingValues(
                start = 18.dp,
                top = innerPadding.calculateTopPadding() + 12.dp,
                end = 18.dp,
                bottom = innerPadding.calculateBottomPadding() + 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { CareTopBar(onBack) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Medications",
                        color = CareText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = CareChip),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Add Medication", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            item {
                MedicationRow(
                    name = "Insulin",
                    detail = "10mg * Daily at 9:00 PM",
                    badgeText = "Taken",
                    badgeColor = CareChip,
                    isOn = true
                )
            }

            item {
                MedicationRow(
                    name = "Antibiotic 2",
                    detail = "500mg * Every 8h",
                    badgeText = "Due",
                    badgeColor = CareWarn,
                    isOn = true
                )
            }

            item {
                MedicationRow(
                    name = "Antibiotic",
                    detail = "500mg * Every 8h",
                    badgeText = "Missed",
                    badgeColor = CareError,
                    isOn = false
                )
            }
        }
    }
}

@Composable
private fun CareTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.Menu, contentDescription = "Back", tint = CareNav)
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.MedicalServices, contentDescription = "Brand", tint = CareNav)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = CareNav)
        }
    }
}

@Composable
private fun MedicationRow(
    name: String,
    detail: String,
    badgeText: String,
    badgeColor: Color,
    isOn: Boolean
) {
    val checked = remember { mutableStateOf(isOn) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CareCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CareChipAlt),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.MedicalServices, contentDescription = null, tint = CareNav)
            }

            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(name, color = CareText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(detail, color = CareMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = badgeColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.size(10.dp))

            Switch(
                checked = checked.value,
                onCheckedChange = { checked.value = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CareChip,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = CareChipAlt
                )
            )
        }
    }
}
