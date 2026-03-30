package com.example.tesisv3

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.net.URLEncoder
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.SSLSocketFactory

object MqttTransport : IotTransport {
    @Volatile private var lastDiagnostic: String? = null

    override fun sendSyncMessage(connectionString: String, payload: String): AzureIotClient.SyncResult {
        if (connectionString.isBlank()) {
            return AzureIotClient.SyncResult(success = false, error = "Missing connection string")
        }
        return try {
            val config = parseConnectionString(connectionString)
            val uri = buildUri(config.hostName, IotSettings.getTransport(AppContextHolder.appContext))
            val options = buildOptions(config)
            val clientId = config.deviceId

            val client = MqttClient(uri, clientId, MemoryPersistence())
            client.connect(options)
            val message = MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply {
                qos = 1
            }
            client.publish("devices/${config.deviceId}/messages/events/", message)
            client.disconnect()
            client.close()
            AzureIotClient.SyncResult(success = true, code = 200, body = "MQTT publish ok")
        } catch (e: Exception) {
            val detail = buildString {
                append(e::class.java.simpleName).append(": ").append(e.message)
                val causeChain = buildCauseChain(e)
                if (causeChain.isNotBlank()) {
                    append(" | Causes: ").append(causeChain)
                }
                if (!lastDiagnostic.isNullOrBlank()) {
                    append(" | Status: ").append(lastDiagnostic)
                }
            }
            AzureIotClient.SyncResult(success = false, error = detail)
        }
    }

    private fun buildCauseChain(t: Throwable): String {
        val parts = mutableListOf<String>()
        var cur: Throwable? = t.cause
        while (cur != null && parts.size < 4) {
            parts.add("${cur.javaClass.simpleName}: ${cur.message}")
            cur = cur.cause
        }
        return parts.joinToString(" -> ")
    }

    fun getLastDiagnostic(): String? = lastDiagnostic

    fun testConnection(connectionString: String, transportType: TransportType): String {
        if (connectionString.isBlank()) {
            return "Missing connection string"
        }
        return try {
            val config = parseConnectionString(connectionString)
            val uri = buildUri(config.hostName, transportType)
            val options = buildOptions(config)
            val client = MqttClient(uri, config.deviceId, MemoryPersistence())
            client.connect(options)
            client.disconnect()
            client.close()
            "Success | connected"
        } catch (e: Exception) {
            val detail = buildString {
                append(e::class.java.simpleName).append(": ").append(e.message)
                val causeChain = buildCauseChain(e)
                if (causeChain.isNotBlank()) {
                    append(" | Causes: ").append(causeChain)
                }
            }
            detail
        }
    }

    private data class IotConfig(
        val hostName: String,
        val deviceId: String,
        val key: String
    )

    private fun parseConnectionString(connectionString: String): IotConfig {
        val parts = connectionString.split(";")
            .mapNotNull { it.split("=", limit = 2).takeIf { kv -> kv.size == 2 } }
            .associate { it[0] to it[1] }
        val hostName = parts["HostName"] ?: error("Missing HostName")
        val deviceId = parts["DeviceId"] ?: error("Missing DeviceId")
        val key = parts["SharedAccessKey"] ?: error("Missing SharedAccessKey")
        return IotConfig(hostName, deviceId, key)
    }

    private fun buildUri(hostName: String, transportType: TransportType): String {
        return when (transportType) {
            TransportType.MQTT -> "ssl://$hostName:8883"
            TransportType.MQTT_WS -> "wss://$hostName:443/\$iothub/websocket?iothub-no-client-cert=true"
            else -> "wss://$hostName:443/\$iothub/websocket?iothub-no-client-cert=true"
        }
    }

    private fun buildOptions(config: IotConfig): MqttConnectOptions {
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = true
        options.socketFactory = SSLSocketFactory.getDefault()
        options.mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
        options.userName = "${config.hostName}/${config.deviceId}/?api-version=2021-04-12"
        options.password = buildSasToken(config).toCharArray()
        return options
    }

    private fun buildSasToken(config: IotConfig): String {
        val resourceUri = "${config.hostName}/devices/${config.deviceId}"
        val expiry = (System.currentTimeMillis() / 1000L) + 3600L
        val encodedUri = URLEncoder.encode(resourceUri, "UTF-8")
        val stringToSign = "$encodedUri\n$expiry"
        val signature = sign(stringToSign, config.key)
        val encodedSig = URLEncoder.encode(signature, "UTF-8")
        return "SharedAccessSignature sr=$encodedUri&sig=$encodedSig&se=$expiry"
    }

    private fun sign(data: String, base64Key: String): String {
        val keyBytes = Base64.getDecoder().decode(base64Key)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
        val rawHmac = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(rawHmac)
    }
}
