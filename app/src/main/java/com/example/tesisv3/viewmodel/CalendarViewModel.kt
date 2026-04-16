package com.example.tesisv3.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisv3.PatientSession
import com.example.tesisv3.UserProfile
import com.example.tesisv3.data.AppointmentEntity
import com.example.tesisv3.network.ApiConstants
import com.example.tesisv3.network.buildJsonBody
import com.example.tesisv3.network.readStream
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.UUID

// ── Data models ───────────────────────────────────────────────────────────────

data class ApiAppointment(
    val id: String,
    val title: String,
    val scheduledAtMillis: Long,
    val location: String,
    val notes: String
) {
    fun toEntity(): AppointmentEntity = AppointmentEntity(
        id = id.ifBlank { UUID.randomUUID().toString() },
        title = title,
        startAt = scheduledAtMillis
    )
}

data class AppointmentOpResult(val success: Boolean, val message: String)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class CalendarViewModel(app: Application) : AndroidViewModel(app) {

    var apiAppointments by mutableStateOf<List<ApiAppointment>>(emptyList())
        private set
    var patients by mutableStateOf<List<UserProfile>>(emptyList())
        private set
    var selectedPatient by mutableStateOf<UserProfile?>(null)
    var wearableConnected by mutableStateOf<Boolean?>(null)
        private set
    var feedbackMessage by mutableStateOf<String?>(null)
        private set
    var feedbackSuccess by mutableStateOf(true)
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val isDoctor = PatientSession.currentUser?.role?.equals("DOCTOR", ignoreCase = true) == true
            val isCaretaker = PatientSession.currentUser?.role?.equals("CARETAKER", ignoreCase = true) == true
            if (isDoctor || isCaretaker) {
                val result = withContext(Dispatchers.IO) { fetchPatientsList() }
                patients = result
                if (selectedPatient == null) selectedPatient = result.firstOrNull()
                selectedPatient?.let {
                    apiAppointments = withContext(Dispatchers.IO) { fetchAppointments(it.id) }
                }
            } else {
                apiAppointments = withContext(Dispatchers.IO) { fetchAppointments(PatientSession.patientId) }
            }
        }
    }

    fun selectPatient(patient: UserProfile) {
        selectedPatient = patient
        viewModelScope.launch {
            apiAppointments = withContext(Dispatchers.IO) { fetchAppointments(patient.id) }
        }
    }

    fun refreshAppointments() {
        val patientId = selectedPatient?.id ?: PatientSession.patientId
        viewModelScope.launch {
            apiAppointments = withContext(Dispatchers.IO) { fetchAppointments(patientId) }
        }
    }

    fun checkWearableConnection() {
        viewModelScope.launch {
            wearableConnected = withContext(Dispatchers.IO) {
                try {
                    Tasks.await(Wearable.getNodeClient(getApplication()).connectedNodes).isNotEmpty()
                } catch (_: Exception) { false }
            }
        }
    }

    fun sendAppointment(
        patientId: String,
        title: String,
        scheduledAt: String,
        location: String,
        notes: String
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                sendAppointmentRequest(patientId, title, scheduledAt, location, notes)
            }
            feedbackSuccess = result.success
            feedbackMessage = result.message
            if (result.success) refreshAppointments()
        }
    }

    fun clearFeedback() {
        feedbackMessage = null
    }

    // ── Network ───────────────────────────────────────────────────────────────

    private fun fetchAppointments(patientId: String): List<ApiAppointment> {
        if (patientId.isBlank()) return emptyList()
        val url = URL("${ApiConstants.CALENDAR_SERVICE}/v1/patients/$patientId/appointments")
        return try {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Content-Type", "application/json")
                PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
                connectTimeout = 10000
                readTimeout = 10000
            }
            val code = conn.responseCode
            val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
            conn.disconnect()
            if (code !in 200..299 || body.isBlank()) return emptyList()
            val json = JSONObject(body)
            val arr = json.optJSONArray("appointments") ?: return emptyList()
            val list = mutableListOf<ApiAppointment>()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                list.add(
                    ApiAppointment(
                        id = item.optString("id"),
                        title = item.optString("title"),
                        scheduledAtMillis = parseUtcMillis(item.optString("scheduledAt")),
                        location = item.optString("location"),
                        notes = item.optString("notes")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e("CalendarViewModel", "fetchAppointments failed", e)
            emptyList()
        }
    }

    private fun sendAppointmentRequest(
        patientId: String,
        title: String,
        scheduledAt: String,
        location: String,
        notes: String
    ): AppointmentOpResult {
        if (patientId.isBlank()) return AppointmentOpResult(false, "patientId vacío")
        val url = URL("${ApiConstants.CALENDAR_SERVICE}/v1/patients/$patientId/appointments")
        val payload = buildJsonBody("title" to title, "scheduledAt" to scheduledAt,
            "location" to location, "notes" to notes)
        return try {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
            }
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
            conn.disconnect()
            if (code in 200..299) AppointmentOpResult(true, "Cita creada correctamente")
            else AppointmentOpResult(false, body.ifBlank { "Error creando cita (HTTP $code)" })
        } catch (e: Exception) {
            AppointmentOpResult(false, e.message ?: "Error de red")
        }
    }

    private fun fetchPatientsList(): List<UserProfile> {
        val token = PatientSession.accessToken
        if (token.isNullOrBlank()) return emptyList()
        val isCaretaker = PatientSession.currentUser?.role?.equals("CARETAKER", ignoreCase = true) == true
        val urlStr = if (isCaretaker) {
            "${ApiConstants.USER_SERVICE}/v1/caretakers/me/patients"
        } else {
            "${ApiConstants.USER_SERVICE}/v1/patients"
        }
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Authorization", "Bearer $token")
            }
            val code = conn.responseCode
            val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
            conn.disconnect()
            if (code !in 200..299 || body.isBlank()) return emptyList()
            val list = mutableListOf<UserProfile>()
            if (isCaretaker) {
                val arr = when {
                    body.trimStart().startsWith("[") -> JSONArray(body)
                    else -> JSONObject(body).let { o ->
                        o.optJSONArray("patients") ?: o.optJSONArray("data") ?: JSONArray()
                    }
                }
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val p = item.optJSONObject("patient") ?: item
                    val id = item.optString("patientId").ifBlank { p.optString("id") }
                    if (id.isBlank()) continue
                    list.add(UserProfile(
                        id = id, email = p.optString("email"), role = p.optString("role"),
                        firstName = p.optString("firstName"), lastName = p.optString("lastName"),
                        fullName = p.optString("fullName").ifBlank { null },
                        isActive = p.optBoolean("isActive", true), createdAt = p.optString("createdAt")
                    ))
                }
            } else {
                val array = JSONObject(body).optJSONArray("patients") ?: return emptyList()
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    list.add(UserProfile(
                        id = item.optString("id"), email = item.optString("email"),
                        role = item.optString("role"), firstName = item.optString("firstName"),
                        lastName = item.optString("lastName"),
                        fullName = item.optString("fullName"),
                        isActive = item.optBoolean("isActive", true),
                        createdAt = item.optString("createdAt")
                    ))
                }
            }
            list
        } catch (e: Exception) {
            Log.e("CalendarViewModel", "fetchPatientsList failed", e)
            emptyList()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    companion object {
        fun parseUtcMillis(value: String): Long {
            return try {
                Instant.parse(value).toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
