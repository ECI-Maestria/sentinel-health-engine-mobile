package com.example.tesisv3.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisv3.PatientSession
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

// ── Data models ──────────────────────────────────────────────────────────────

data class DashVitals(
    val heartRate: Int?,
    val spO2: Int?,
    val bpSystolic: Int?,
    val bpDiastolic: Int?,
    val timestampLabel: String = "Ahora"
)

data class DashMedication(
    val id: String,
    val name: String,
    val dosage: String,
    val scheduledTime: String,
    val active: Boolean,
    val isDue: Boolean
)

data class DashAppointment(
    val id: String,
    val title: String,
    val scheduledAtMillis: Long,
    val location: String,
    val notes: String
)

data class DashReminder(
    val id: String,
    val title: String,
    val timeLabel: String,
    val recurrenceLabel: String
)

data class VitalHistoryPoint(
    val timestamp: Long,
    val heartRate: Int?,
    val spO2: Int?
)

data class HistoryDebugInfo(
    val requestUrl: String,
    val httpCode: Int,
    val rawResponse: String,
    val exception: String?,
    val points: List<VitalHistoryPoint>
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    var vitals by mutableStateOf<DashVitals?>(null)
        private set
    var medications by mutableStateOf<List<DashMedication>>(emptyList())
        private set
    var appointments by mutableStateOf<List<DashAppointment>>(emptyList())
        private set
    var reminders by mutableStateOf<List<DashReminder>>(emptyList())
        private set
    var vitalsHistory by mutableStateOf<List<VitalHistoryPoint>>(emptyList())
        private set
    var historyLoading by mutableStateOf(true)
        private set
    var historyDebug by mutableStateOf<HistoryDebugInfo?>(null)
        private set
    var wearableConnected by mutableStateOf<Boolean?>(null)
        private set
    var steps by mutableStateOf<Int?>(null)

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val patientId = PatientSession.patientId
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

            if (vitals == null) {
                val apiVitals = withContext(Dispatchers.IO) { fetchApiVitals(patientId) }
                if (vitals == null) vitals = apiVitals
            }

            medications = withContext(Dispatchers.IO) { fetchDashMedications(patientId) }
            appointments = withContext(Dispatchers.IO) { fetchDashAppointments(patientId, today) }
            reminders = withContext(Dispatchers.IO) { fetchDashReminders(patientId, today) }

            val histResult = withContext(Dispatchers.IO) { fetchVitalsHistory(patientId) }
            vitalsHistory = histResult.points
            historyDebug = histResult
            historyLoading = false
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

    fun onWearableMessageReceived(payload: String, path: String) {
        try {
            val parsed = JSONObject(payload)
            val hr = parsed.optInt("heartRate", parsed.optInt("hr", 0))
            val spo2 = parsed.optInt("spO2", parsed.optInt("spo2", 0))
            if (hr > 0 || spo2 > 0) {
                val current = vitals
                vitals = DashVitals(
                    heartRate = if (hr > 0) hr else current?.heartRate,
                    spO2 = if (spo2 > 0) spo2 else current?.spO2,
                    bpSystolic = current?.bpSystolic,
                    bpDiastolic = current?.bpDiastolic,
                    timestampLabel = "Ahora"
                )
            }
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Failed to parse wearable message: $path", e)
        }
    }

    fun updateSteps(newSteps: Int) {
        steps = newSteps
    }

    // ── Network ───────────────────────────────────────────────────────────────

    private fun fetchApiVitals(patientId: String): DashVitals? {
        if (patientId.isBlank()) return null
        val url = URL("${ApiConstants.ANALYTICS_SERVICE}/v1/patients/$patientId/vitals/latest")
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
            if (code !in 200..299 || body.isBlank()) return null
            val json = JSONObject(body)
            val hr = json.optInt("heartRate").takeIf { it > 0 }
                ?: json.optInt("hr").takeIf { it > 0 }
            val spo2 = json.optInt("oxygenSaturation").takeIf { it > 0 }
                ?: json.optInt("spO2").takeIf { it > 0 }
                ?: json.optInt("spo2").takeIf { it > 0 }
            val bpSys = json.optInt("bloodPressureSystolic").takeIf { it > 0 }
                ?: json.optInt("systolic").takeIf { it > 0 }
            val bpDia = json.optInt("bloodPressureDiastolic").takeIf { it > 0 }
                ?: json.optInt("diastolic").takeIf { it > 0 }
            val tsRaw = json.optString("measuredAt").ifBlank { json.optString("timestamp") }
            DashVitals(
                heartRate = hr,
                spO2 = spo2,
                bpSystolic = bpSys,
                bpDiastolic = bpDia,
                timestampLabel = formatVitalsTimestamp(tsRaw)
            )
        } catch (_: Exception) { null }
    }

    private fun fetchDashMedications(patientId: String): List<DashMedication> {
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
            val today = LocalDate.now()
            val list = mutableListOf<DashMedication>()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val startDate = item.optString("startDate")
                val endDate = item.optString("endDate")
                if (!isMedForToday(startDate, endDate, today)) continue
                val timesArr = item.optJSONArray("scheduledTimes") ?: JSONArray()
                val firstTime = if (timesArr.length() > 0) timesArr.optString(0) else ""
                list.add(
                    DashMedication(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        dosage = item.optString("dosage"),
                        scheduledTime = formatMedTime(firstTime),
                        active = item.optBoolean("active", true),
                        isDue = firstTime.isNotBlank() && isTimeDue(firstTime)
                    )
                )
            }
            list
        } catch (_: Exception) { emptyList() }
    }

    private fun fetchDashAppointments(patientId: String, date: String): List<DashAppointment> {
        if (patientId.isBlank()) return emptyList()
        val url = URL("${ApiConstants.CALENDAR_SERVICE}/v1/patients/$patientId/appointments?period=day&date=$date")
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
            val list = mutableListOf<DashAppointment>()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val scheduledAt = item.optString("scheduledAt")
                val millis = try { Instant.parse(scheduledAt).toEpochMilli() } catch (_: Exception) { 0L }
                list.add(
                    DashAppointment(
                        id = item.optString("id"),
                        title = item.optString("title"),
                        scheduledAtMillis = millis,
                        location = item.optString("location"),
                        notes = item.optString("notes")
                    )
                )
            }
            list.sortedBy { it.scheduledAtMillis }
        } catch (_: Exception) { emptyList() }
    }

    private fun fetchDashReminders(patientId: String, date: String): List<DashReminder> {
        if (patientId.isBlank()) return emptyList()
        val url = URL("${ApiConstants.CALENDAR_SERVICE}/v1/patients/$patientId/reminders?period=day&date=$date")
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
            val arr = json.optJSONArray("reminders") ?: return emptyList()
            val list = mutableListOf<DashReminder>()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val scheduledAt = item.optString("scheduledAt").ifBlank { item.optString("time") }
                val recurrence = item.optString("recurrence").ifBlank { item.optString("frequency") }
                list.add(
                    DashReminder(
                        id = item.optString("id"),
                        title = item.optString("title"),
                        timeLabel = buildReminderTimeLabel(scheduledAt),
                        recurrenceLabel = formatRecurrence(recurrence)
                    )
                )
            }
            list
        } catch (_: Exception) { emptyList() }
    }

    private fun fetchVitalsHistory(patientId: String): HistoryDebugInfo {
        val to = LocalDate.now()
        val from = to.minusDays(29)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val urlStr = "${ApiConstants.ANALYTICS_SERVICE}/v1/patients/$patientId/vitals/history" +
                "?from=${from.format(fmt)}&to=${to.format(fmt)}"
        if (patientId.isBlank()) {
            return HistoryDebugInfo(urlStr, -1, "", "patientId vacío", emptyList())
        }
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Content-Type", "application/json")
                PatientSession.accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
                connectTimeout = 12000
                readTimeout = 12000
            }
            val code = conn.responseCode
            val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
            conn.disconnect()
            if (code !in 200..299 || body.isBlank()) {
                return HistoryDebugInfo(urlStr, code, body, null, emptyList())
            }
            val json = JSONObject(body)
            val arr = json.optJSONArray("readings")
                ?: json.optJSONArray("history")
                ?: json.optJSONArray("vitals")
                ?: json.optJSONArray("data")
                ?: return HistoryDebugInfo(
                    urlStr, code, body,
                    "No array found in: readings/history/vitals/data. Keys: ${json.keys().asSequence().joinToString()}",
                    emptyList()
                )
            val list = mutableListOf<VitalHistoryPoint>()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val tsStr = item.optString("measuredAt").ifBlank {
                    item.optString("timestamp").ifBlank { item.optString("date") }
                }
                val ts = try { Instant.parse(tsStr).toEpochMilli() } catch (_: Exception) { continue }
                val hr = item.optInt("heartRate").takeIf { it > 0 }
                    ?: item.optInt("hr").takeIf { it > 0 }
                val spo2Raw = item.opt("spO2") ?: item.opt("oxygenSaturation") ?: item.opt("spo2")
                val spo2 = when (spo2Raw) {
                    is Double -> spo2Raw.toInt().takeIf { it > 0 }
                    is Int    -> spo2Raw.takeIf { it > 0 }
                    is Float  -> spo2Raw.toInt().takeIf { it > 0 }
                    else      -> null
                }
                if (hr != null || spo2 != null) list.add(VitalHistoryPoint(ts, hr, spo2))
            }
            HistoryDebugInfo(urlStr, code, body, null, list.sortedBy { it.timestamp })
        } catch (e: Exception) {
            HistoryDebugInfo(urlStr, -1, "", "${e.javaClass.simpleName}: ${e.message}", emptyList())
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    companion object {
        fun isMedForToday(startDate: String, endDate: String, today: LocalDate): Boolean {
            return try {
                val start = LocalDate.parse(startDate.take(10))
                val end = if (endDate.isBlank() || endDate == "null") null
                else LocalDate.parse(endDate.take(10))
                !today.isBefore(start) && (end == null || !today.isAfter(end))
            } catch (_: Exception) { true }
        }

        fun isTimeDue(timeStr: String): Boolean {
            if (timeStr.isBlank()) return false
            return try {
                val cal = Calendar.getInstance()
                val currentHour = cal.get(Calendar.HOUR_OF_DAY)
                val currentMinute = cal.get(Calendar.MINUTE)
                if (timeStr.contains("T")) {
                    val localTime = Instant.parse(timeStr).atZone(ZoneId.systemDefault()).toLocalTime()
                    return localTime.hour < currentHour ||
                            (localTime.hour == currentHour && localTime.minute <= currentMinute)
                }
                val parts = timeStr.trim().split(":")
                val hour = parts[0].toInt()
                val minute = parts.getOrElse(1) { "0" }.toInt()
                hour < currentHour || (hour == currentHour && minute <= currentMinute)
            } catch (_: Exception) { false }
        }

        fun formatMedTime(timeStr: String): String {
            if (timeStr.isBlank()) return ""
            return try {
                if (timeStr.contains("T")) {
                    val localTime = Instant.parse(timeStr).atZone(ZoneId.systemDefault()).toLocalTime()
                    val h12 = if (localTime.hour % 12 == 0) 12 else localTime.hour % 12
                    val amPm = if (localTime.hour < 12) "AM" else "PM"
                    return String.format(Locale.US, "%d:%02d %s", h12, localTime.minute, amPm)
                }
                val parts = timeStr.trim().split(":")
                val hour = parts[0].toInt()
                val minute = parts.getOrElse(1) { "0" }.toInt()
                val h12 = if (hour % 12 == 0) 12 else hour % 12
                val amPm = if (hour < 12) "AM" else "PM"
                String.format(Locale.US, "%d:%02d %s", h12, minute, amPm)
            } catch (_: Exception) { timeStr }
        }

        fun buildReminderTimeLabel(scheduledAt: String): String {
            if (scheduledAt.isBlank()) return "Hoy"
            return try {
                val localTime = Instant.parse(scheduledAt).atZone(ZoneId.systemDefault()).toLocalTime()
                val h12 = if (localTime.hour % 12 == 0) 12 else localTime.hour % 12
                val amPm = if (localTime.hour < 12) "AM" else "PM"
                "Hoy · ${String.format(Locale.US, "%d:%02d %s", h12, localTime.minute, amPm)}"
            } catch (_: Exception) {
                try { "Hoy · ${formatMedTime(scheduledAt)}" } catch (_: Exception) { "Hoy" }
            }
        }

        fun formatRecurrence(recurrence: String): String {
            return when (recurrence.uppercase(Locale.US)) {
                "DAILY" -> "Diario"
                "WEEKLY" -> "Semanal"
                "MONTHLY" -> "Mensual"
                "ONCE" -> "Una vez"
                "AS_NEEDED" -> "Según necesidad"
                else -> recurrence.replace("_", " ")
                    .lowercase(Locale.US)
                    .replaceFirstChar { it.uppercase() }
            }
        }

        fun formatAppointmentTimeLabel(millis: Long): String {
            if (millis == 0L) return "PENDIENTE"
            val today = LocalDate.now(ZoneId.systemDefault())
            val zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
            val appointmentDate = zdt.toLocalDate()
            val localTime = zdt.toLocalTime()
            val h12 = if (localTime.hour % 12 == 0) 12 else localTime.hour % 12
            val amPm = if (localTime.hour < 12) "AM" else "PM"
            val timeStr = String.format(Locale.US, "%d:%02d %s", h12, localTime.minute, amPm)
            return when (appointmentDate) {
                today -> "HOY · $timeStr"
                today.plusDays(1) -> "MAÑANA · $timeStr"
                else -> {
                    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale("es"))
                    "${appointmentDate.format(fmt).uppercase()} · $timeStr"
                }
            }
        }

        fun formatVitalsTimestamp(isoString: String): String {
            if (isoString.isBlank()) return "—"
            return try {
                val instant = Instant.parse(isoString)
                val minutesAgo = ChronoUnit.MINUTES.between(instant, Instant.now())
                when {
                    minutesAgo < 1 -> "Ahora"
                    minutesAgo < 60 -> "hace $minutesAgo min"
                    else -> "hace ${minutesAgo / 60}h"
                }
            } catch (_: Exception) { "—" }
        }

        fun buildWearablePayload(rawPayload: String, path: String, deviceUuid: String): String {
            val now = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
            val parsed = try { JSONObject(rawPayload) } catch (_: Exception) { null }
            if (parsed != null) {
                parsed.put("deviceId", deviceUuid)
                if (!parsed.has("timestamp")) parsed.put("timestamp", now)
                return parsed.toString()
            }
            return JSONObject().apply {
                put("deviceId", deviceUuid)
                put("timestamp", now)
                put("rawPath", path)
                put("rawPayload", rawPayload)
            }.toString()
        }
    }
}
