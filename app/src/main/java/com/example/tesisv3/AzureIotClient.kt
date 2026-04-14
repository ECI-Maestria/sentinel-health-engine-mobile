package com.example.tesisv3

import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object AzureIotClient {
    private const val API_VERSION = "2021-04-12"

    data class SyncResult(
        val success: Boolean,
        val code: Int? = null,
        val body: String? = null,
        val error: String? = null
    )

    fun sendSyncMessage(connectionString: String, payload: String): SyncResult {
        if (connectionString.isBlank()) {
            return SyncResult(success = false, error = "Missing connection string")
        }

        val parts = connectionString.split(";")
            .mapNotNull { it.split("=", limit = 2).takeIf { kv -> kv.size == 2 } }
            .associate { it[0] to it[1] }

        val hostName = parts["HostName"] ?: return SyncResult(success = false, error = "Missing HostName")
        val deviceId = parts["DeviceId"] ?: return SyncResult(success = false, error = "Missing DeviceId")
        val key = parts["SharedAccessKey"] ?: return SyncResult(success = false, error = "Missing SharedAccessKey")
        val keyName = parts["SharedAccessKeyName"]

        val resourceUri = "$hostName/devices/$deviceId"
        val expiry = (System.currentTimeMillis() / 1000L) + 3600L
        val encodedUri = URLEncoder.encode(resourceUri, "UTF-8")
        val stringToSign = "$encodedUri\n$expiry"
        val signature = sign(stringToSign, key)
        val sasToken = if (keyName.isNullOrBlank()) {
            "SharedAccessSignature sr=$encodedUri&sig=${URLEncoder.encode(signature, "UTF-8")}&se=$expiry"
        } else {
            "SharedAccessSignature sr=$encodedUri&sig=${URLEncoder.encode(signature, "UTF-8")}&se=$expiry&skn=$keyName"
        }

        val url = URL("https://$hostName/devices/$deviceId/messages/events?api-version=$API_VERSION")
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", sasToken)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("iothub-contenttype", "application/json")
            connection.setRequestProperty("iothub-contentencoding", "utf-8")
            connection.doOutput = true

            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val body = readStream(
                if (code in 200..299) connection.inputStream else connection.errorStream
            )
            Log.d("AzureIotClient", "IoT Hub response: $code $body")
            if (code in 200..299) {
                SyncResult(success = true, code = code, body = body)
            } else {
                SyncResult(success = false, code = code, body = body, error = "IoT Hub error")
            }
        } catch (e: Exception) {
            Log.e("AzureIotClient", "IoT Hub error", e)
            SyncResult(success = false, error = e.message)
        } finally {
            connection.disconnect()
        }
    }

    private fun sign(data: String, base64Key: String): String {
        val keyBytes = Base64.getDecoder().decode(base64Key)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
        val rawHmac = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(rawHmac)
    }

    private fun readStream(stream: InputStream?): String? {
        if (stream == null) return null
        return BufferedReader(InputStreamReader(stream)).use { reader ->
            reader.readLines().joinToString("\n")
        }
    }
}
