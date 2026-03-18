package com.example.tesisv3

import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.tesisv3.data.AppDatabase
import com.example.tesisv3.data.AppointmentEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

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
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).appointmentDao() }
    val scope = rememberCoroutineScope()
    val appointments by dao.observeAll().collectAsState(initial = emptyList())
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var displayedMonthMillis by remember { mutableStateOf(startOfMonth(System.currentTimeMillis())) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }
    var deletingAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }

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

            item {
                CalendarCard(
                    selectedDateMillis = selectedDateMillis,
                    displayedMonthMillis = displayedMonthMillis,
                    appointments = appointments,
                    onDateSelected = { selectedDateMillis = it },
                    onMonthChange = { displayedMonthMillis = it },
                    onAddAppointment = { showAddDialog = true }
                )
            }

            val selectedMonth = Calendar.getInstance().apply { timeInMillis = displayedMonthMillis }
            val month = selectedMonth.get(Calendar.MONTH)
            val year = selectedMonth.get(Calendar.YEAR)
            val monthlyAppointments = appointments.filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.startAt }
                cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
            }

            item {
                Text(
                    text = "Appointments",
                    color = CalendarText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (monthlyAppointments.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = CalendarCard
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No appointments this month", color = CalendarMuted)
                        }
                    }
                }
            } else {
                items(monthlyAppointments.size) { index ->
                    val item = monthlyAppointments[index]
                    AppointmentCard(
                        title = item.title,
                        detail = formatDateTime(item.startAt),
                        onClick = { editingAppointment = item },
                        onDelete = { deletingAppointment = item }
                    )
                }
            }
        }
    }

    if (showAddDialog || editingAppointment != null) {
        val editing = editingAppointment
        AddAppointmentDialog(
            selectedDateMillis = editing?.startAt ?: selectedDateMillis,
            existing = editing,
            onDismiss = {
                showAddDialog = false
                editingAppointment = null
            },
            onSave = { title, hour, minute ->
                val baseMillis = editing?.startAt ?: selectedDateMillis
                val cal = Calendar.getInstance().apply {
                    timeInMillis = baseMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                scope.launch {
                    val entity = AppointmentEntity(
                        id = editing?.id ?: UUID.randomUUID().toString(),
                        title = title,
                        startAt = cal.timeInMillis
                    )
                    if (editing == null) {
                        dao.insert(entity)
                    } else {
                        dao.update(entity)
                    }
                }
                showAddDialog = false
                editingAppointment = null
            }
        )
    }

    if (deletingAppointment != null) {
        val item = deletingAppointment
        AlertDialog(
            onDismissRequest = { deletingAppointment = null },
            title = { Text("Delete appointment") },
            text = { Text("Delete ${item?.title}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        item?.let { scope.launch { dao.delete(it) } }
                        deletingAppointment = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingAppointment = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CalendarTopBar(onBack: () -> Unit) {
    val context = LocalContext.current
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
        IconButton(onClick = {
            context.startActivity(Intent(context, NotificationsActivity::class.java))
        }) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = CalendarNav)
        }
    }
}

@Composable
private fun CalendarCard(
    selectedDateMillis: Long,
    displayedMonthMillis: Long,
    appointments: List<AppointmentEntity>,
    onDateSelected: (Long) -> Unit,
    onMonthChange: (Long) -> Unit,
    onAddAppointment: () -> Unit
) {
    val displayCal = Calendar.getInstance().apply { timeInMillis = displayedMonthMillis }
    val displayYear = displayCal.get(Calendar.YEAR)
    val displayMonth = displayCal.get(Calendar.MONTH)
    val days = remember(displayedMonthMillis) { buildMonthGrid(displayYear, displayMonth) }
    val appointmentsInMonth = appointments.filter {
        val cal = Calendar.getInstance().apply { timeInMillis = it.startAt }
        cal.get(Calendar.YEAR) == displayYear && cal.get(Calendar.MONTH) == displayMonth
    }.mapNotNull {
        Calendar.getInstance().apply { timeInMillis = it.startAt }.get(Calendar.DAY_OF_MONTH)
    }.toSet()

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
                Text(
                    text = monthYearLabel(displayedMonthMillis),
                    color = CalendarText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = displayedMonthMillis }
                            cal.add(Calendar.MONTH, -1)
                            onMonthChange(startOfMonth(cal.timeInMillis))
                        }
                    ) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Prev", tint = CalendarMuted)
                    }
                    IconButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = displayedMonthMillis }
                            cal.add(Calendar.MONTH, 1)
                            onMonthChange(startOfMonth(cal.timeInMillis))
                        }
                    ) {
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next", tint = CalendarMuted)
                    }
                }

                Button(
                    onClick = onAddAppointment,
                    colors = ButtonDefaults.buttonColors(containerColor = CalendarChip),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Add\nAppointment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { day ->
                    Text(day, color = CalendarMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { cell ->
                        if (cell.day == null) {
                            Box(modifier = Modifier.size(30.dp))
                        } else {
                            val isSelected = isSameDay(cell.dateMillis, selectedDateMillis)
                            Column(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) CalendarAccent else Color.Transparent)
                                    .clickable {
                                        cell.dateMillis?.let { onDateSelected(it) }
                                    }
                                    .padding(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = cell.day.toString(),
                                    color = if (isSelected) Color.White else CalendarText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                if (appointmentsInMonth.contains(cell.day)) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else CalendarChip)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AppointmentCard(
    title: String,
    detail: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CalendarChipAlt,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
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
                Icon(Icons.Outlined.EventNote, contentDescription = null, tint = CalendarNav)
            }

            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(title, color = CalendarText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(detail, color = CalendarMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.weight(1f))
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = CalendarChipAlt),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFCC6A63))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAppointmentDialog(
    selectedDateMillis: Long,
    existing: AppointmentEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int) -> Unit
) {
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var error by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val timePickerState = rememberTimePickerState(
        initialHour = existing?.let { Calendar.getInstance().apply { timeInMillis = it.startAt }.get(Calendar.HOUR_OF_DAY) } ?: 9,
        initialMinute = existing?.let { Calendar.getInstance().apply { timeInMillis = it.startAt }.get(Calendar.MINUTE) } ?: 0,
        is24Hour = false
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (existing == null) "New appointment" else "Edit appointment",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text("Date: ${formatDate(selectedDateMillis)}", color = CalendarMuted)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = false },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showTimePicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CalendarChip),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Pick time", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = formatTime(timePickerState.hour, timePickerState.minute),
                        color = CalendarText,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (error) {
                    Text("Please enter a title", color = Color(0xFFD35C55))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                error = true
                                return@Button
                            }
                            onSave(title.trim(), timePickerState.hour, timePickerState.minute)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CalendarChip)
                    ) {
                        Text("Save")
                    }
                }
            }
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
                            onClick = { showTimePicker = false },
                            colors = ButtonDefaults.buttonColors(containerColor = CalendarChip)
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}

private fun monthYearLabel(timestamp: Long): String {
    val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return format.format(Date(timestamp)).replaceFirstChar { it.uppercaseChar() }
}

private fun formatDate(timestamp: Long): String {
    val format = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    return format.format(Date(timestamp))
}

private fun formatDateTime(timestamp: Long): String {
    val format = SimpleDateFormat("EEE, MMM d * h:mm a", Locale.getDefault())
    return format.format(Date(timestamp))
}

private fun formatTime(hour: Int, minute: Int): String {
    val hour12 = if (hour % 12 == 0) 12 else hour % 12
    val amPm = if (hour < 12) "AM" else "PM"
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)
}

private data class DayCell(val day: Int?, val dateMillis: Long?)

private fun buildMonthGrid(year: Int, month: Int): List<DayCell> {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = mutableListOf<DayCell>()
    repeat(firstDayOfWeek) { cells.add(DayCell(null, null)) }
    for (day in 1..daysInMonth) {
        cal.set(Calendar.DAY_OF_MONTH, day)
        cells.add(DayCell(day, cal.timeInMillis))
    }
    while (cells.size % 7 != 0) {
        cells.add(DayCell(null, null))
    }
    return cells
}

private fun isSameDay(a: Long?, b: Long?): Boolean {
    if (a == null || b == null) return false
    val calA = Calendar.getInstance().apply { timeInMillis = a }
    val calB = Calendar.getInstance().apply { timeInMillis = b }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
        calA.get(Calendar.MONTH) == calB.get(Calendar.MONTH) &&
        calA.get(Calendar.DAY_OF_MONTH) == calB.get(Calendar.DAY_OF_MONTH)
}

private fun startOfMonth(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
