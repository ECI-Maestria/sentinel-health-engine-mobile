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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

class GroupsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GroupsScreen(onBack = { finish() })
            }
        }
    }
}

private val GroupsBackground = Color(0xFFF6F7F2)
private val GroupsText = Color(0xFF2E3F35)
private val GroupsMuted = Color(0xFF7B8C81)
private val GroupsChip = Color(0xFF64CFA1)
private val GroupsChipAlt = Color(0xFFE1F2E6)
private val GroupsNav = Color(0xFF5A7A63)
private val GroupsCard = Color(0xFFFFFFFF)

@Composable
private fun GroupsScreen(onBack: () -> Unit) {
    var selectedNav by remember { mutableIntStateOf(1) }

    Scaffold(
        containerColor = GroupsBackground,
        bottomBar = {
            AppBottomNav(
                current = BottomNavDestination.GROUPS,
                modifier = Modifier.navigationBarsPadding(),
                indicatorColor = GroupsChipAlt,
                selectedColor = GroupsNav,
                unselectedColor = GroupsNav.copy(alpha = 0.5f)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(GroupsBackground),
            contentPadding = PaddingValues(
                start = 18.dp,
                top = innerPadding.calculateTopPadding() + 12.dp,
                end = 18.dp,
                bottom = innerPadding.calculateBottomPadding() + 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { GroupsTopBar(onBack) }

            item {
                Text(
                    text = "Your Groups",
                    color = GroupsText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = GroupsChip),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("New Group", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = GroupsChipAlt),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Invite User", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GroupsText)
                    }
                }
            }

            item {
                GroupRow(
                    initials = "FS",
                    name = "Family Share",
                    detail = "3 members * Steps, Heart Rate"
                )
            }

            item {
                GroupRow(
                    initials = "TR",
                    name = "Trainer",
                    detail = "1 member * All Data"
                )
            }

            item {
                GroupRow(
                    initials = "CT",
                    name = "Care Team",
                    detail = "4 members * Meds, Appointments"
                )
            }
        }
    }
}

@Composable
private fun GroupsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.Menu, contentDescription = "Back", tint = GroupsNav)
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", color = GroupsText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = GroupsNav)
        }
    }
}

@Composable
private fun GroupRow(initials: String, name: String, detail: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = GroupsCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GroupsChipAlt),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = GroupsNav, fontWeight = FontWeight.Bold)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = GroupsText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(detail, color = GroupsMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = GroupsChipAlt),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share", tint = GroupsNav)
                }
            }

            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = GroupsChipAlt),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = GroupsNav)
                }
            }
        }
    }
}
