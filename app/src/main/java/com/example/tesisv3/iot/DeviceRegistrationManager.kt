package com.example.tesisv3.iot

import com.example.tesisv3.*

import com.example.tesisv3.network.*

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

object DeviceRegistrationManager {
    private const val REGISTER_URL = "${ApiConstants.USER_SERVICE}/v1/devices/register"
    private const val PREFS = "device_secure_store"
    private const val KEY_DEVICE_UUID = "device_uuid"
    private const val KEY_LAST_RESULT = "last_register_result"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun registerIfNeeded(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val token = fetchFcmToken() ?: return@launch
            val deviceId = getOrCreateDeviceUuid(appContext)
            val result = registerDevice(appContext, token, deviceId)
            saveLastResult(appContext, result)
        }
    }

    fun consumeLastResult(context: Context): DeviceRegisterResult? {
        val prefs = encryptedPrefs(context)
        val raw = prefs.getString(KEY_LAST_RESULT, null) ?: return null
        prefs.edit().remove(KEY_LAST_RESULT).apply()
        val parts = raw.split("|", limit = 4)
        if (parts.size < 4) return null
        return DeviceRegisterResult(
            success = parts[0].toBooleanStrictOrNull() ?: false,
            code = parts[1].toIntOrNull(),
            body = parts[2].ifBlank { null },
            error = parts[3].ifBlank { null }
        )
    }

    fun getDeviceUuid(context: Context): String {
        return getOrCreateDeviceUuid(context.applicationContext)
    }

    private suspend fun fetchFcmToken(): String? {
        return suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> cont.resume(token) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    private fun registerDevice(context: Context, fcmToken: String, deviceIdentifier: String): DeviceRegisterResult {
        val name = Build.MODEL ?: "Android"
        val payload = buildJsonBody(
            "deviceIdentifier" to deviceIdentifier,
            "fcmToken" to fcmToken,
            "platform" to "ANDROID",
            "name" to name
        )

        return try {
            val token = PatientSession.accessToken
            val conn = (URL(REGISTER_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
            }
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
            conn.disconnect()
            if (code in 200..299) {
                DeviceRegisterResult(success = true, code = code, body = body)
            } else {
                DeviceRegisterResult(success = false, code = code, body = body, error = "$token + HTTP $code")
            }
        } catch (e: Exception) {
            DeviceRegisterResult(success = false, error = e.message ?: "Network error")
        }
    }

    private fun getOrCreateDeviceUuid(context: Context): String {
        val prefs = encryptedPrefs(context)
        val existing = prefs.getString(KEY_DEVICE_UUID, null)
        if (!existing.isNullOrBlank()) return existing
        val uuid = java.util.UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_UUID, uuid).apply()
        return uuid
    }

    private fun encryptedPrefs(context: Context): SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS,
        masterKey(context),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun masterKey(context: Context): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private fun saveLastResult(context: Context, result: DeviceRegisterResult) {
        val raw = listOf(
            result.success.toString(),
            result.code?.toString().orEmpty(),
            result.body.orEmpty(),
            result.error.orEmpty()
        ).joinToString("|")
        encryptedPrefs(context).edit().putString(KEY_LAST_RESULT, raw).apply()
    }
}

data class DeviceRegisterResult(
    val success: Boolean,
    val code: Int? = null,
    val body: String? = null,
    val error: String? = null
)
