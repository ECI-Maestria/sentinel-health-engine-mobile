package com.example.tesisv3.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisv3.PatientSession
import com.example.tesisv3.UserProfile
import com.example.tesisv3.network.ApiConstants
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

// ── Data models ───────────────────────────────────────────────────────────────

data class ApiMedication(
    val id: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val scheduledTimes: List<String>,
    val startDate: String,
    val endDate: String,
    val notes: String,
    val active: Boolean
)

data class MedicationOpResult(val success: Boolean, val message: String)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class CareViewModel(app: Application) : AndroidViewModel(app) {

    var medications by mutableStateOf<List<ApiMedication>>(emptyList())
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
            val isDoctor    = PatientSession.currentUser?.role?.equals("DOCTOR",    ignoreCase = true) == true
            val isCaretaker = PatientSession.currentUser?.role?.equals("CARETAKER", ignoreCase = true) == true
            if (isDoctor || isCaretaker) {
                val result = withContext(Dispatchers.IO) { fetchPatientsList() }
                patients = result
                if (selectedPatient == null) selectedPatient = result.firstOrNull()
                selectedPatient?.let {
                    medications = withContext(Dispatchers.IO) { fetchMedications(it.id) }
                }
            } else {
                medications = withContext(Dispatchers.IO) { fetchMedications(PatientSession.patientId) }
            }
        }
    }

    fun selectPatient(patient: UserProfile) {
        selectedPatient = patient
        viewModelScope.launch {
            medications = withContext(Dispatchers.IO) { fetchMedications(patient.id) }
        }
    }

    fun refreshMedications() {
        val patientId = selectedPatient?.id ?: PatientSession.patientId
        viewModelScope.launch {
            medications = withContext(Dispatchers.IO) { fetchMedications(patientId) }
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

    fun createMedication(medication: ApiMedication) {
        val patientId = selectedPatient?.id ?: PatientSession.patientId
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { doCreateMedication(patientId, medication) }
            feedbackSuccess = result.success
            feedbackMessage = if (result.success) "Medicamento creado" else result.message
            if (result.success) refreshMedications()
        }
    }

    fun updateMedication(medication: ApiMedication) {
        val patientId = selectedPatient?.id ?: PatientSession.patientId
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { doUpdateMedication(patientId, medication) }
            feedbackSuccess = result.success
            feedbackMessage = if (result.success) "Medicamento actualizado" else result.message
            if (result.success) refreshMedications()
        }
    }

    fun deleteMedication(medicationId: String) {
        val patientId = selectedPatient?.id ?: PatientSession.patientId
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { doDeleteMedication(patientId, medicationId) }
            feedbackSuccess = result.success
            feedbackMessage = if (result.success) "Medicamento eliminado" else result.message
            if (result.success) refreshMedications()
        }
    }

    fun clearFeedback() {
        feedbackMessage = null
    }

    // ── Network ───────────────────────────────────────────────────────────────

    private fun fetchMedications(patientId: String): List<ApiMedication> {
        if (patientId.isBlank()) return emptyList()
        val url = URL("${ApiConstants.CALENDAR_SERVICE}/v1/patients/$patientId/medications?active=true")
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
            val arr = json.optJSONArray("medications") ?: JSONArray()
            val list = mutableListOf<ApiMedication>()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val timesArr = item.optJSONArray("scheduledTimes") ?: JSONArray()
                val times = (0 until timesArr.length()).map { timesArr.optString(it) }
                list.add(ApiMedication(
                    id = item.optString("id"),
                    name = item.optString("name"),
                    dosage = item.optString("dosage"),
                    frequency = item.optString("frequency"),
                    scheduledTimes = times,
                    startDate = item.optString("startDate"),
                    endDate = item.optString("endDate"),
                    notes = item.optString("notes"),
                    active = item.optBoolean("active", true)
                ))
            }
            list
        } catch (e: Exception) {
            Log.e("CareViewModel", "fetchMedications failed", e)
            emptyList()
        }
    }

    private fun doCreateMedication(patientId: String, medication: ApiMedication): MedicationOpResult {
        if (patientId.isBlank()) return MedicationOpResult(false, "patientId vacío")
        val url = URL("${ApiConstants.CALENDAR_SERVICE}/v1/patients/$patientId/medications")
        return sendMedicationRequest(url, "POST", buildMedicationPayload(medication))
    }

    private fun doUpdateMedication(patientId: String, medication: ApiMedication): MedicationOpResult {
        if (patientId.isBlank()) return MedicationOpResult(false, "patientId vacío")
        if (medication.id.isBlank()) return MedicationOpResult(false, "medicationId vacío")
        val url = URL("${ApiConstants.CALENDAR_SERVICE}/v1/patients/$patientId/medications/${medication.id}")
        return sendMedicationRequest(url, "PUT", buildMedicationPayload(medication))
    }

    private fun doDeleteMedication(patientId: String, medicationId: String): MedicationOpResult {
        if (patientId.isBlank()) return MedicationOpResult(false, "patientId vacío")
        if (medicationId.isBlank()) return MedicationOpResult(false, "medicationId vacío")
        val url = URL("${ApiConstants.CALENDAR_SERVICE}/v1/patients/$patientId/medications/$medicationId")
        return sendMedicationRequest(url, "DELETE", null)
    }

    private fun sendMedicationRequest(url: URL, method: String, payload: String?): MedicationOpResult {
        return try {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                setRequestProperty("Content-Type", "application/json")
                PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
                connectTimeout = 10000
                readTimeout = 10000
                if (payload != null) {
                    doOutput = true
                    outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = conn.responseCode
            val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
            conn.disconnect()
            if (code in 200..299) MedicationOpResult(true, "")
            else MedicationOpResult(false, body.ifBlank { "Error (HTTP $code)" })
        } catch (e: Exception) {
            MedicationOpResult(false, e.message ?: "Error de red")
        }
    }

    private fun buildMedicationPayload(medication: ApiMedication): String {
        val obj = JSONObject()
        obj.put("name", medication.name)
        obj.put("dosage", medication.dosage)
        obj.put("frequency", medication.frequency)
        obj.put("scheduledTimes", JSONArray(medication.scheduledTimes))
        obj.put("startDate", medication.startDate)
        obj.put("endDate", medication.endDate)
        obj.put("notes", medication.notes)
        return obj.toString()
    }

    private fun fetchPatientsList(): List<UserProfile> {
        val token = PatientSession.accessToken
        if (token.isNullOrBlank()) return emptyList()
        val isCaretaker = PatientSession.currentUser?.role?.equals("CARETAKER", ignoreCase = true) == true
        val urlStr = if (isCaretaker) "${ApiConstants.USER_SERVICE}/v1/caretakers/me/patients"
                     else "${ApiConstants.USER_SERVICE}/v1/patients"
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
                        lastName = item.optString("lastName"), fullName = item.optString("fullName"),
                        isActive = item.optBoolean("isActive", true), createdAt = item.optString("createdAt")
                    ))
                }
            }
            list
        } catch (e: Exception) {
            Log.e("CareViewModel", "fetchPatientsList failed", e)
            emptyList()
        }
    }
}
