package com.example.tesisv3

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.tesisv3.data.AppDatabase
import com.example.tesisv3.data.MedicationEntity
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class ScheduleType {
    SPECIFIC,
    EVERY_X
}

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
private val CareNav = Color(0xFF5A7A63)
private val CareWarn = Color(0xFFE0A04B)
private val CareError = Color(0xFFE06A61)

private fun MedicationEntity.displaySchedule(): String {
    return when (scheduleType) {
        ScheduleType.SPECIFIC.name -> {
            val h = hourOfDay ?: 0
            val m = minute ?: 0
            val hour12 = if (h % 12 == 0) 12 else h % 12
            val amPm = if (h < 12) "AM" else "PM"
            "Daily at %d:%02d %s".format(Locale.US, hour12, m, amPm)
        }
        else -> "Every ${intervalHours ?: 1}h"
    }
}

private fun statusColor(status: String): Color {
    return when (status) {
        "Taken" -> CareChip
        "Missed" -> CareError
        else -> CareWarn
    }
}

@Composable
private fun CareScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).medicationDao() }
    val scope = rememberCoroutineScope()
    val medications by dao.observeAll().collectAsState(initial = emptyList())

    var showSheet by remember { mutableStateOf(false) }
    var pendingEnableId by remember { mutableStateOf<String?>(null) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val id = pendingEnableId
        if (granted && id != null) {
            scope.launch {
                dao.getById(id)?.let { scheduleMedication(context, it) }
            }
        }
        pendingEnableId = null
    }

    LaunchedEffect(medications) {
        medications.filter { it.enabled }.forEach { scheduleMedication(context, it) }
    }

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
                        onClick = { showSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CareChip),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Add Medication", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            items(medications.size) { index ->
                val item = medications[index]
                MedicationRow(
                    name = item.name,
                    detail = "${item.amount} * ${item.displaySchedule()}",
                    badgeText = item.status,
                    badgeColor = statusColor(item.status),
                    isOn = item.enabled,
                    onToggle = { enabled ->
                        scope.launch { dao.update(item.copy(enabled = enabled)) }
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                pendingEnableId = item.id
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                scheduleMedication(context, item.copy(enabled = true))
                            }
                        } else {
                            cancelMedication(context, item)
                        }
                    }
                )
            }
        }

        if (showSheet) {
            AddMedicationSheet(
                onDismiss = { showSheet = false },
                onAdd = { newItem ->
                    scope.launch { dao.insert(newItem) }
                    showSheet = false
                    scheduleMedication(context, newItem)
                }
            )
        }
    }
}

private fun scheduleMedication(context: android.content.Context, item: MedicationEntity) {
    val workManager = WorkManager.getInstance(context)

    val data = workDataOf(
        "med_id" to item.id,
        "name" to item.name,
        "amount" to item.amount,
        "schedule" to item.displaySchedule(),
        "type" to item.medType
    )

    val request = if (item.scheduleType == ScheduleType.SPECIFIC.name) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, item.hourOfDay ?: 0)
            set(Calendar.MINUTE, item.minute ?: 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = target.timeInMillis - now.timeInMillis
        PeriodicWorkRequestBuilder<MedicationReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
    } else {
        val hours = (item.intervalHours ?: 1).coerceAtLeast(1)
        PeriodicWorkRequestBuilder<MedicationReminderWorker>(hours.toLong(), TimeUnit.HOURS)
            .setInputData(data)
            .build()
    }

    workManager.enqueueUniquePeriodicWork(
        "med_${item.id}",
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

private fun cancelMedication(context: android.content.Context, item: MedicationEntity) {
    WorkManager.getInstance(context).cancelUniqueWork("med_${item.id}")
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
    isOn: Boolean,
    onToggle: (Boolean) -> Unit
) {
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
                checked = isOn,
                onCheckedChange = onToggle,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMedicationSheet(
    onDismiss: () -> Unit,
    onAdd: (MedicationEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("") }
    var everyX by remember { mutableStateOf("") }
    var scheduleType by remember { mutableStateOf(ScheduleType.SPECIFIC) }
    var error by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }
    var medType by remember { mutableStateOf("Pill") }

    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().get(Calendar.MINUTE),
        is24Hour = false
    )

    fun formatTime(hourOfDay: Int, minute: Int): String {
        val hour12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
        val amPm = if (hourOfDay < 12) "AM" else "PM"
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("New medication", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = false },
                label = { Text("Medication name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; error = false },
                label = { Text("Amount (e.g. 500mg)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { scheduleType = ScheduleType.SPECIFIC },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (scheduleType == ScheduleType.SPECIFIC) CareChip else CareChipAlt
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Specific time", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { scheduleType = ScheduleType.EVERY_X },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (scheduleType == ScheduleType.EVERY_X) CareChip else CareChipAlt
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Every X hours", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { medType = "Pill" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (medType == "Pill") CareChip else CareChipAlt
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Pill", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { medType = "Injection" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (medType == "Injection") CareChip else CareChipAlt
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Injection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { medType = "Other" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (medType == "Other") CareChip else CareChipAlt
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Other", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (scheduleType == ScheduleType.SPECIFIC) {
                OutlinedTextField(
                    value = if (hour.isBlank()) "Pick a time" else hour,
                    onValueChange = {},
                    label = { Text("Time") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )

                TextButton(onClick = { showTimePicker = true }) {
                    Text("Open time picker")
                }
            } else {
                OutlinedTextField(
                    value = everyX,
                    onValueChange = { everyX = it; error = false },
                    label = { Text("Every X hours (e.g. 8)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (error) {
                Text(errorMessage, color = CareError, fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = {
                        if (name.isBlank() || amount.isBlank()) {
                            error = true
                            errorMessage = "Please fill all fields"
                            return@Button
                        }

                        if (scheduleType == ScheduleType.SPECIFIC && hour.isBlank()) {
                            error = true
                            errorMessage = "Please pick a time"
                            return@Button
                        }

                        if (scheduleType == ScheduleType.EVERY_X && everyX.isBlank()) {
                            error = true
                            errorMessage = "Please enter the interval"
                            return@Button
                        }

                        val interval = if (scheduleType == ScheduleType.EVERY_X) {
                            everyX.trim().toIntOrNull()
                        } else {
                            null
                        }
                        if (scheduleType == ScheduleType.EVERY_X && (interval == null || interval <= 0)) {
                            error = true
                            errorMessage = "Interval must be a number"
                            return@Button
                        }

                        error = false
                        errorMessage = ""
                        val item = MedicationEntity(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            amount = amount.trim(),
                            scheduleType = scheduleType.name,
                            hourOfDay = if (scheduleType == ScheduleType.SPECIFIC) timePickerState.hour else null,
                            minute = if (scheduleType == ScheduleType.SPECIFIC) timePickerState.minute else null,
                            intervalHours = interval,
                            status = "Due",
                            enabled = true,
                            medType = medType,
                            createdAt = System.currentTimeMillis()
                        )
                        onAdd(item)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CareChip)
                ) {
                    Text("Add")
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select time", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                hour = formatTime(timePickerState.hour, timePickerState.minute)
                                error = false
                                errorMessage = ""
                                showTimePicker = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CareChip)
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}
