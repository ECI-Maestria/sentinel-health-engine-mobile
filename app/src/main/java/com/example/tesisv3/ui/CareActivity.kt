package com.example.tesisv3.ui

import com.example.tesisv3.*

import com.example.tesisv3.network.*
import com.example.tesisv3.viewmodel.ApiMedication
import com.example.tesisv3.viewmodel.CareViewModel

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import android.content.Intent
import android.app.DatePickerDialog
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Calendar

class CareActivity : ComponentActivity() {
    private val viewModel: CareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CareScreen(viewModel = viewModel, onBack = { finish() })
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

private fun frequencyLabel(value: String): String {
    return when (value.uppercase(Locale.US)) {
        "ONCE_DAILY" -> "Once daily"
        "TWICE_DAILY" -> "Twice daily"
        "THREE_TIMES_DAILY" -> "3 times daily"
        "EVERY_4_HOURS" -> "Every 4 hours"
        "EVERY_6_HOURS" -> "Every 6 hours"
        "EVERY_8_HOURS" -> "Every 8 hours"
        else -> value.replace("_", " ").lowercase(Locale.US).replaceFirstChar { it.uppercase() }
    }
}

@Composable
private fun CareScreen(viewModel: CareViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val medications = viewModel.medications
    val feedbackMessage = viewModel.feedbackMessage
    val feedbackSuccess = viewModel.feedbackSuccess
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val isDoctor    = PatientSession.currentUser?.role?.equals("DOCTOR",    ignoreCase = true) == true
    val isCaretaker = PatientSession.currentUser?.role?.equals("CARETAKER", ignoreCase = true) == true
    val canEdit = isDoctor
    val patients = viewModel.patients
    val selectedPatient = viewModel.selectedPatient
    var patientExpanded by remember { mutableStateOf(false) }

    var showSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ApiMedication?>(null) }
    var deletingItem by remember { mutableStateOf<ApiMedication?>(null) }
    val wearableConnected = viewModel.wearableConnected

    LaunchedEffect(Unit) {
        viewModel.checkWearableConnection()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                RoleDrawerContent(
                    role = PatientSession.currentUser?.role,
                    current = DrawerDestination.MEDICATIONS,
                    onItemClick = { destination ->
                        handleDrawerNavigation(context, destination)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
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
                item { CareTopBar(wearableConnected = wearableConnected, onMenu = { scope.launch { drawerState.open() } }) }

                if (isDoctor || isCaretaker) {
                    item {
                        PatientPickerCard(
                            label = "Paciente",
                            patients = patients,
                            selected = selectedPatient,
                            expanded = patientExpanded,
                            onExpandedChange = { patientExpanded = it },
                            onSelect = { patient ->
                                viewModel.selectPatient(patient)
                                patientExpanded = false
                            }
                        )
                    }
                }

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
                    if (canEdit) {
                        Button(
                            onClick = {
                                editingItem = null
                                showSheet = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CareChip),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Add Medication", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            items(medications.size) { index ->
                val item = medications[index]
                MedicationRow(
                    name = item.name,
                    detail = "${item.dosage} · ${frequencyLabel(item.frequency)}",
                    subDetail = buildString {
                        if (item.scheduledTimes.isNotEmpty()) {
                            append(item.scheduledTimes.joinToString(", "))
                        }
                        if (item.startDate.isNotBlank() || item.endDate.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(item.startDate)
                            if (item.endDate.isNotBlank()) {
                                append(" → ").append(item.endDate)
                            }
                        }
                    },
                    badgeText = if (item.active) "Activo" else "Inactivo",
                    badgeColor = if (item.active) CareChip else CareWarn,
                    canEdit = canEdit,
                    onClick = { if (canEdit) { editingItem = item; showSheet = true } },
                    onEdit = {
                        if (canEdit) {
                            editingItem = item
                            showSheet = true
                        }
                    },
                    onDelete = { if (canEdit) { deletingItem = item } },
                    onToggle = null
                )
            }
        }

        if (showSheet) {
            AddMedicationSheet(
                onDismiss = { showSheet = false },
                existing = editingItem,
                onSave = { newItem ->
                    if (editingItem == null) viewModel.createMedication(newItem)
                    else viewModel.updateMedication(newItem)
                    showSheet = false
                    editingItem = null
                }
            )
        }

        if (deletingItem != null) {
            val item = deletingItem
            AlertDialog(
                onDismissRequest = { deletingItem = null },
                title = { Text("Delete medication") },
                text = { Text("Are you sure you want to delete ${item?.name}?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            item?.let { med -> viewModel.deleteMedication(med.id) }
                            deletingItem = null
                        }
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { deletingItem = null }) { Text("Cancel") }
                }
            )
        }
        feedbackMessage?.let { message ->
            android.widget.Toast
                .makeText(context, message, if (feedbackSuccess) android.widget.Toast.LENGTH_SHORT else android.widget.Toast.LENGTH_LONG)
                .show()
            viewModel.clearFeedback()
        }
    }
    }
}

@Composable
private fun CareTopBar(wearableConnected: Boolean?, onMenu: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onMenu) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (PatientSession.currentUser?.role?.equals("PATIENT", ignoreCase = true) == true) {
                WatchStatusIcon(wearableConnected = wearableConnected)
            }
            IconButton(onClick = {
                context.startActivity(Intent(context, NotificationsActivity::class.java))
            }) {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = CareNav)
            }
        }
    }
}

@Composable
private fun PatientPickerCard(
    label: String,
    patients: List<UserProfile>,
    selected: UserProfile?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (UserProfile) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFE5F4EA),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(label, color = CareNav, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Box {
                OutlinedTextField(
                    value = selected?.fullName ?: "Selecciona paciente",
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD3EBDD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (selected?.fullName ?: "P").take(1),
                                color = CareNav,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { onExpandedChange(true) }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Select", tint = CareNav)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) }
                ) {
                    patients.forEach { patient ->
                        DropdownMenuItem(
                            text = { Text(patient.fullName.orEmpty()) },
                            onClick = { onSelect(patient) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicationRow(
    name: String,
    detail: String,
    subDetail: String,
    badgeText: String,
    badgeColor: Color,
    canEdit: Boolean = true,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: ((Boolean) -> Unit)? = null
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
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clickable(onClick = onClick),
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
                if (subDetail.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(subDetail, color = CareMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
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

            if (canEdit) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = CareNav)
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = CareError)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMedicationSheet(
    onDismiss: () -> Unit,
    onSave: (ApiMedication) -> Unit,
    existing: ApiMedication? = null
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var dosage by remember(existing?.id) { mutableStateOf(existing?.dosage ?: "") }
    var frequency by remember(existing?.id) { mutableStateOf(existing?.frequency ?: "TWICE_DAILY") }
    val scheduledTimes = remember(existing?.id) {
        androidx.compose.runtime.mutableStateListOf<String>().apply {
            if (existing != null) {
                addAll(existing.scheduledTimes)
            }
        }
    }
    var startDate by remember(existing?.id) { mutableStateOf(existing?.startDate ?: "") }
    var endDate by remember(existing?.id) { mutableStateOf(existing?.endDate ?: "") }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes ?: "") }
    var error by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = androidx.compose.material3.rememberTimePickerState(
        initialHour = 8,
        initialMinute = 0,
        is24Hour = true
    )
    val context = LocalContext.current

    val frequencyOptions = listOf(
        "ONCE_DAILY" to "1 vez al día",
        "TWICE_DAILY" to "2 veces al día",
        "THREE_TIMES_DAILY" to "3 veces al día",
        "EVERY_8_HOURS" to "Cada 8 horas",
        "EVERY_6_HOURS" to "Cada 6 horas",
        "ONCE_WEEKLY" to "Una vez a la semana",
        "AS_NEEDED" to "Según necesidad"
    )

    fun frequencyLabelFor(value: String): String {
        return frequencyOptions.firstOrNull { it.first == value }?.second ?: value
    }

    fun openDatePicker(current: String, onSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        val parts = current.split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val d = parts[2].toIntOrNull()
            if (y != null && m != null && d != null) {
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m - 1)
                cal.set(Calendar.DAY_OF_MONTH, d)
            }
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onSelected(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (existing == null) "New medication" else "Edit medication",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = false },
                label = { Text("Medication name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it; error = false },
                label = { Text("Dosage (e.g. 500mg)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Box {
                OutlinedTextField(
                    value = frequencyLabelFor(frequency),
                    onValueChange = {},
                    label = { Text("Frecuencia") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { frequencyExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Abrir opciones",
                                tint = CareMuted
                            )
                        }
                    }
                )
                DropdownMenu(
                    expanded = frequencyExpanded,
                    onDismissRequest = { frequencyExpanded = false }
                ) {
                    frequencyOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.second) },
                            onClick = {
                                frequency = option.first
                                frequencyExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Horas programadas", color = CareText, fontWeight = FontWeight.SemiBold)
                Button(
                    onClick = { showTimePicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CareChip),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Agregar hora", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (scheduledTimes.isEmpty()) {
                Text("Sin horarios aún", color = CareMuted, fontSize = 12.sp)
            } else {
                Text(
                    scheduledTimes.joinToString(", "),
                    color = CareMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = {},
                    label = { Text("Start date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    readOnly = true
                )
                Button(
                    onClick = { openDatePicker(startDate) { startDate = it; error = false } },
                    colors = ButtonDefaults.buttonColors(containerColor = CareChipAlt),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("📅", fontSize = 12.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = endDate,
                    onValueChange = {},
                    label = { Text("End date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    readOnly = true
                )
                Button(
                    onClick = { openDatePicker(endDate) { endDate = it; error = false } },
                    colors = ButtonDefaults.buttonColors(containerColor = CareChipAlt),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("📅", fontSize = 12.sp)
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it; error = false },
                label = { Text("Notes") },
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )

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
                        if (name.isBlank() || dosage.isBlank()) {
                            error = true
                            errorMessage = "Please fill all fields"
                            return@Button
                        }

                        if (frequency.isBlank()) {
                            error = true
                            errorMessage = "Please enter frequency"
                            return@Button
                        }

                        if (scheduledTimes.isEmpty()) {
                            error = true
                            errorMessage = "Please enter at least one time"
                            return@Button
                        }

                        error = false
                        errorMessage = ""
                        val item = ApiMedication(
                            id = existing?.id ?: "",
                            name = name.trim(),
                            dosage = dosage.trim(),
                            frequency = frequency.trim().uppercase(Locale.US),
                            scheduledTimes = scheduledTimes.toList(),
                            startDate = startDate.trim(),
                            endDate = endDate.trim(),
                            notes = notes.trim(),
                            active = true
                        )
                        onSave(item)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CareChip)
                ) {
                    Text(if (existing == null) "Add" else "Save")
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }

    if (showTimePicker) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Selecciona la hora", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    androidx.compose.material3.TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
                        Button(
                            onClick = {
                                val formatted = String.format(Locale.US, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                                if (!scheduledTimes.contains(formatted)) {
                                    scheduledTimes.add(formatted)
                                }
                                error = false
                                errorMessage = ""
                                showTimePicker = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CareChip)
                        ) {
                            Text("Confirmar")
                        }
                    }
                }
            }
        }
    }
}
