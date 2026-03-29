package com.example.tesisv3

import android.content.Intent
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LoginScreen(
                    onLoginSuccess = {
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

private val LoginBackground = Color(0xFFF6F7F2)
private val LoginText = Color(0xFF2E3F35)
private val LoginMuted = Color(0xFF7B8C81)
private val LoginChip = Color(0xFF5BCB90)
private val LoginChipAlt = Color(0xFFE1F2E6)
private val LoginNav = Color(0xFF5A7A63)

@Composable
private fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    Scaffold(containerColor = LoginBackground) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LoginBackground)
                .padding(
                    start = 22.dp,
                    top = innerPadding.calculateTopPadding() + 14.dp,
                    end = 22.dp,
                    bottom = innerPadding.calculateBottomPadding() + 22.dp
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LoginTopBar()

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Welcome back",
                color = LoginText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Please sign in to continue",
                color = LoginMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; showError = false },
                        label = { Text("User") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; showError = false },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showError) {
                        var DeviceID = FirebaseMessaging.getInstance().token
                        //DeviceID: zzw@36545    || dEaLTmBcShiD0-L7sPvz1k:APA91bGuxrv7Sk3Wn5xp71TMhGm3LwzJmkA12ckJpMbAFJLp0Zz6Dw8iXEvr6iNiRNhWy91HwCWta8_3mISnLAT3ddOTARYpr6SUn1GanjtT0DYSNVAAuEg
                        //zzW@f81c25a
                        Text(
                            text = "Invalid user or password ",
                            color = Color(0xFFD35C55),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showPassword = !showPassword },
                            colors = ButtonDefaults.buttonColors(containerColor = LoginChipAlt),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (showPassword) "Hide" else "Show",
                                color = LoginText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (username == "user" && password == "user") {
                                    onLoginSuccess()
                                } else {
                                    showError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LoginChip),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text("Login", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginTopBar() {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = {}) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = LoginNav)
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", color = LoginText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = {
            context.startActivity(Intent(context, NotificationsActivity::class.java))
        }) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = LoginNav)
        }
    }
}
