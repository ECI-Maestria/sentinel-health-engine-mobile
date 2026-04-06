package com.example.tesisv3

import android.content.Context
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import org.json.JSONObject
import org.json.JSONTokener

object DeviceRegistrationManager {
    private const val PREFS = "device_registration"
    private const val KEY_REGISTERED = "registered"
    private const val REGISTER_URL = "https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/devices/register"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var registeredInMemory = false

    fun isRegistered(context: Context): Boolean {
        if (registeredInMemory) return true
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getBoolean(KEY_REGISTERED, false)
        if (stored) registeredInMemory = true
        return stored
    }

    fun registerIfNeeded(context: Context) {
        if (isRegistered(context)) return
        val appContext = context.applicationContext
        scope.launch {
            val token = fetchFcmToken() ?: return@launch
            val alreadyRegistered = isDeviceAlreadyRegistered(appContext, token)
            if (alreadyRegistered) {
                markRegistered(appContext)
                return@launch
            }
            val success = registerDevice(appContext, token, token)
            if (success) {
                markRegistered(appContext)
            }
        }
    }

    private suspend fun fetchFcmToken(): String? {
        return suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> cont.resume(token) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    private fun registerDevice(context: Context, fcmToken: String, deviceIdentifier: String): Boolean {
        val name = Build.MODEL ?: "Android"
        val payload = """{
            "deviceIdentifier":"${escapeJson(deviceIdentifier)}",
            "fcmToken":"${escapeJson(fcmToken)}",
            "platform":"ANDROID",
            "name":"${escapeJson(name)}"
        }""".trimIndent()

        return try {
            val conn = (URL(REGISTER_URL).openConnection() as HttpURLConnection).apply {
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
            readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    private fun isDeviceAlreadyRegistered(context: Context, deviceIdentifier: String): Boolean {
        val patientId = PatientSession.patientId
        if (patientId.isEmpty()) return false
        val url = URL("https://user-service.yellowmeadow-4dfba13a.centralus.azurecontainerapps.io/v1/patients/$patientId/profile/complete")
        return try {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                PatientSession.accessToken?.let {
                    setRequestProperty("Authorization", "Bearer $it")
                }
                connectTimeout = 10000
                readTimeout = 10000
            }
            val code = conn.responseCode
            val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
            conn.disconnect()
            if (code !in 200..299 || body.isEmpty()) return false
            val json = JSONObject(JSONTokener(body))
            val devices = json.optJSONArray("devices") ?: return false
            for (i in 0 until devices.length()) {
                val device = devices.optJSONObject(i) ?: continue
                if (deviceIdentifier == device.optString("deviceIdentifier")) {
                    return true
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun markRegistered(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_REGISTERED, true).apply()
        registeredInMemory = true
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }

    private fun readStream(stream: java.io.InputStream?): String {
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream)).use { it.readText() }
    }
}
