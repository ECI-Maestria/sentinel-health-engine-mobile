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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
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
import android.widget.Toast
import androidx.compose.ui.window.Dialog
import com.example.tesisv3.data.AppDatabase
import com.example.tesisv3.data.AppointmentEntity
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.json.JSONObject

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
private val CalendarSurface = Color(0xFFF2F6F3)
private val CalendarBorder = Color(0xFFE4EAE6)

@Composable
private fun CalendarScreen(onBack: () -> Unit) {
    var selectedNav by remember { mutableIntStateOf(2) }
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).appointmentDao() }
    val scope = rememberCoroutineScope()
    val appointments by dao.observeAll().collectAsState(initial = emptyList())
    var apiAppointments by remember { mutableStateOf<List<ApiAppointment>>(emptyList()) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var displayedMonthMillis by remember { mutableStateOf(startOfMonth(System.currentTimeMillis())) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }
    var deletingAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }
    var selectedFilter by remember { mutableStateOf(CalendarFilter.DAY) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackSuccess by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        apiAppointments = withContext(Dispatchers.IO) { fetchAppointments(PatientSession.patientId) }
    }

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
                    appointments = apiAppointments,
                    onDateSelected = { selectedDateMillis = it },
                    onMonthChange = { displayedMonthMillis = it },
                    onAddAppointment = { showAddDialog = true }
                )
            }

            item {
                FilterRow(
                    selected = selectedFilter,
                    onSelected = { selectedFilter = it }
                )
            }

            val filteredAppointments = filterAppointments(
                appointments = apiAppointments,
                selectedDateMillis = selectedDateMillis,
                displayedMonthMillis = displayedMonthMillis,
                filter = selectedFilter
            )

            item {
                Text(
                    text = "Citas",
                    color = CalendarText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (filteredAppointments.isEmpty()) {
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
                            Text("No hay citas en este periodo", color = CalendarMuted)
                        }
                    }
                }
            } else {
                items(filteredAppointments.size) { index ->
                    val item = filteredAppointments[index]
                    AppointmentCard(
                        title = item.title,
                        detail = formatDateTime(item.scheduledAtMillis),
                        onClick = { editingAppointment = item.toEntity() },
                        onDelete = { deletingAppointment = item.toEntity() }
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
            onSave = { title, location, notes, hour, minute ->
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
                    val scheduledAt = toUtcIsoString(cal.timeInMillis)
                    val result = withContext(Dispatchers.IO) {
                        sendAppointmentRequest(
                            patientId = PatientSession.patientId,
                            title = title,
                            scheduledAt = scheduledAt,
                            location = location,
                            notes = notes
                        )
                    }
                    if (result.success) {
                        apiAppointments = withContext(Dispatchers.IO) { fetchAppointments(PatientSession.patientId) }
                        feedbackMessage = "Cita creada correctamente"
                        feedbackSuccess = true
                    } else {
                        feedbackMessage = result.message
                        feedbackSuccess = false
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

    feedbackMessage?.let { message ->
        Toast
            .makeText(context, message, if (feedbackSuccess) Toast.LENGTH_SHORT else Toast.LENGTH_LONG)
            .show()
        feedbackMessage = null
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
    appointments: List<ApiAppointment>,
    onDateSelected: (Long) -> Unit,
    onMonthChange: (Long) -> Unit,
    onAddAppointment: () -> Unit
) {
    val displayCal = Calendar.getInstance().apply { timeInMillis = displayedMonthMillis }
    val displayYear = displayCal.get(Calendar.YEAR)
    val displayMonth = displayCal.get(Calendar.MONTH)
    val days = remember(displayedMonthMillis) { buildMonthGrid(displayYear, displayMonth) }
    val appointmentsInMonth = appointments.filter {
        val cal = Calendar.getInstance().apply { timeInMillis = it.scheduledAtMillis }
        cal.get(Calendar.YEAR) == displayYear && cal.get(Calendar.MONTH) == displayMonth
    }.mapNotNull {
        Calendar.getInstance().apply { timeInMillis = it.scheduledAtMillis }.get(Calendar.DAY_OF_MONTH)
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = displayedMonthMillis }
                            cal.add(Calendar.MONTH, -1)
                            onMonthChange(startOfMonth(cal.timeInMillis))
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CalendarSurface)
                    ) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Prev", tint = CalendarMuted)
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = displayedMonthMillis }
                            cal.add(Calendar.MONTH, 1)
                            onMonthChange(startOfMonth(cal.timeInMillis))
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CalendarSurface)
                    ) {
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next", tint = CalendarMuted)
                    }
                }

                if (PatientSession.currentUser?.role?.equals("DOCTOR", ignoreCase = true) == true) {
                    Button(
                        onClick = onAddAppointment,
                        colors = ButtonDefaults.buttonColors(containerColor = CalendarChip),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Add Appointment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
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
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CalendarBorder)
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
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CalendarChipAlt),
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE8E8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFCC6A63))
                }
            }
        }
    }
}

private enum class CalendarFilter { DAY, WEEK, MONTH, YEAR }

@Composable
private fun FilterRow(
    selected: CalendarFilter,
    onSelected: (CalendarFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(label = "Día", active = selected == CalendarFilter.DAY) { onSelected(CalendarFilter.DAY) }
        FilterChip(label = "Semana", active = selected == CalendarFilter.WEEK) { onSelected(CalendarFilter.WEEK) }
        FilterChip(label = "Mes", active = selected == CalendarFilter.MONTH) { onSelected(CalendarFilter.MONTH) }
        FilterChip(label = "Año", active = selected == CalendarFilter.YEAR) { onSelected(CalendarFilter.YEAR) }
    }
}

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = if (active) CalendarChip else Color.White),
        border = if (active) null else androidx.compose.foundation.BorderStroke(1.dp, CalendarBorder),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (active) Color.White else CalendarText,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ReminderCard(title: String, detail: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = CalendarText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = CalendarMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAppointmentDialog(
    selectedDateMillis: Long,
    existing: AppointmentEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, Int) -> Unit
) {
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var location by remember(existing?.id) { mutableStateOf("") }
    var notes by remember(existing?.id) { mutableStateOf("") }
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

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it; error = false },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it; error = false },
                    label = { Text("Notes") },
                    singleLine = false,
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
                            onSave(
                                title.trim(),
                                location.trim(),
                                notes.trim(),
                                timePickerState.hour,
                                timePickerState.minute
                            )
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

private fun toUtcIsoString(timestamp: Long): String {
    return DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC))
}

private data class ApiAppointment(
    val id: String,
    val title: String,
    val scheduledAtMillis: Long,
    val location: String,
    val notes: String
) {
    fun toEntity(): AppointmentEntity {
        return AppointmentEntity(
            id = id.ifBlank { UUID.randomUUID().toString() },
            title = title,
            startAt = scheduledAtMillis
        )
    }
}

private fun fetchAppointments(patientId: String): List<ApiAppointment> {
    if (patientId.isBlank()) return emptyList()
    val url = URL("https://calendar-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/appointments")
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Content-Type", "application/json")
            PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            connectTimeout = 10000
            readTimeout = 10000
        }
        val code = conn.responseCode
        val body = readStreamString(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        if (code !in 200..299 || body.isBlank()) return emptyList()
        val json = JSONObject(body)
        val arr = json.optJSONArray("appointments") ?: return emptyList()
        val list = mutableListOf<ApiAppointment>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val scheduledAt = item.optString("scheduledAt")
            list.add(
                ApiAppointment(
                    id = item.optString("id"),
                    title = item.optString("title"),
                    scheduledAtMillis = parseUtcMillis(scheduledAt),
                    location = item.optString("location"),
                    notes = item.optString("notes")
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

private fun parseUtcMillis(value: String): Long {
    return try {
        Instant.parse(value).toEpochMilli()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}

private fun filterAppointments(
    appointments: List<ApiAppointment>,
    selectedDateMillis: Long,
    displayedMonthMillis: Long,
    filter: CalendarFilter
): List<ApiAppointment> {
    val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    return when (filter) {
        CalendarFilter.DAY -> appointments.filter {
            isSameDay(it.scheduledAtMillis, selectedDateMillis)
        }
        CalendarFilter.WEEK -> {
            val start = startOfWeek(selectedDateMillis)
            val end = start + 7 * 24 * 60 * 60 * 1000L
            appointments.filter { it.scheduledAtMillis in start until end }
        }
        CalendarFilter.MONTH -> {
            val monthCal = Calendar.getInstance().apply { timeInMillis = displayedMonthMillis }
            val month = monthCal.get(Calendar.MONTH)
            val year = monthCal.get(Calendar.YEAR)
            appointments.filter {
                val apCal = Calendar.getInstance().apply { timeInMillis = it.scheduledAtMillis }
                apCal.get(Calendar.MONTH) == month && apCal.get(Calendar.YEAR) == year
            }
        }
        CalendarFilter.YEAR -> {
            val year = cal.get(Calendar.YEAR)
            appointments.filter {
                val apCal = Calendar.getInstance().apply { timeInMillis = it.scheduledAtMillis }
                apCal.get(Calendar.YEAR) == year
            }
        }
    }.sortedBy { it.scheduledAtMillis }
}

private fun sendAppointmentRequest(
    patientId: String,
    title: String,
    scheduledAt: String,
    location: String,
    notes: String
) : AppointmentResult {
    if (patientId.isBlank()) return AppointmentResult(false, "patientId vacío")
    val url = URL("https://calendar-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/appointments")
    val payload = """{
        "title":"${escapeJson(title)}",
        "scheduledAt":"${escapeJson(scheduledAt)}",
        "location":"${escapeJson(location)}",
        "notes":"${escapeJson(notes)}"
    }""".trimIndent()
    try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            PatientSession.accessToken?.let {
                setRequestProperty("Authorization", "Bearer $it")
            }
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
        }
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val body = readStreamString(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        return if (code in 200..299) {
            AppointmentResult(true, "Cita creada correctamente")
        } else {
            AppointmentResult(false, body.ifBlank { "Error creando cita (HTTP $code)" })
        }
    } catch (e: Exception) {
        return AppointmentResult(false, e.message ?: "Error de red")
    }
}

private fun escapeJson(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}

private fun readStream(stream: java.io.InputStream?) {
    if (stream == null) return
    BufferedReader(InputStreamReader(stream)).use { it.readText() }
}

private fun readStreamString(stream: java.io.InputStream?): String {
    if (stream == null) return ""
    return BufferedReader(InputStreamReader(stream)).use { it.readText() }
}

private data class AppointmentResult(val success: Boolean, val message: String)

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

private fun startOfWeek(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestamp
        firstDayOfWeek = Calendar.SUNDAY
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
