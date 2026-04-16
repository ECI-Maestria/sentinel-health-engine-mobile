package com.example.tesisv3.ui

import com.example.tesisv3.*

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tesisv3.data.AppDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NotificationsScreen(onBack = { finish() })
            }
        }
    }
}

private val NotificationsBackground = Color(0xFFF6F7F2)
private val NotificationsCard = Color(0xFFFFFFFF)
private val NotificationsText = Color(0xFF2E3F35)
private val NotificationsMuted = Color(0xFF7B8C81)
private val NotificationsChip = Color(0xFF5BCB90)
private val NotificationsNav = Color(0xFF5A7A63)

@Composable
private fun NotificationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).notificationDao() }
    val scope = rememberCoroutineScope()
    val notifications by dao.observeAll().collectAsState(initial = emptyList())
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                RoleDrawerContent(
                    role = PatientSession.currentUser?.role,
                    current = DrawerDestination.DASHBOARD,
                    onItemClick = { destination ->
                        handleDrawerNavigation(context, destination)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = NotificationsBackground
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NotificationsBackground),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    end = 18.dp,
                    bottom = innerPadding.calculateBottomPadding() + 18.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { NotificationsTopBar(onMenu = { scope.launch { drawerState.open() } }) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Notifications",
                        color = NotificationsText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { scope.launch { dao.deleteAll() } },
                        colors = ButtonDefaults.buttonColors(containerColor = NotificationsChip),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Clear All", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            if (notifications.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = NotificationsCard
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = NotificationsMuted)
                            Spacer(Modifier.height(8.dp))
                            Text("No notifications yet", color = NotificationsMuted, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                items(notifications.size) { index ->
                    val item = notifications[index]
                    NotificationRow(
                        title = item.title,
                        body = item.body,
                        time = formatTimestamp(item.createdAt)
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun NotificationsTopBar(onMenu: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = NotificationsNav)
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = NotificationsNav)
        }
        Box(modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun NotificationRow(title: String, body: String, time: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NotificationsCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = NotificationsText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(body, color = NotificationsMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(time, color = NotificationsMuted, fontSize = 12.sp)
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val format = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
    return format.format(Date(timestamp))
}
