package com.example.tesisv3

import com.microsoft.azure.sdk.iot.device.DeviceClient
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode
import com.microsoft.azure.sdk.iot.device.Message
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object MqttTransport : IotTransport {
    @Volatile private var client: DeviceClient? = null

    private fun getOrCreateClient(connectionString: String): DeviceClient {
        val existing = client
        if (existing != null) return existing
        synchronized(this) {
            val again = client
            if (again != null) return again
            val created = DeviceClient(connectionString, IotHubClientProtocol.MQTT)
            created.open()
            client = created
            return created
        }
    }

    override fun sendSyncMessage(connectionString: String, payload: String): AzureIotClient.SyncResult {
        if (connectionString.isBlank()) {
            return AzureIotClient.SyncResult(success = false, error = "Missing connection string")
        }
        return try {
            val deviceClient = getOrCreateClient(connectionString)
            val message = Message(payload)
            val latch = CountDownLatch(1)
            var status: IotHubStatusCode? = null

            val callback = IotHubEventCallback { responseStatus, _ ->
                status = responseStatus
                latch.countDown()
            }

            deviceClient.sendEventAsync(message, callback, null)
            val completed = latch.await(10, TimeUnit.SECONDS)
            if (!completed) {
                AzureIotClient.SyncResult(success = false, error = "MQTT timeout")
            } else {
                val ok = status == IotHubStatusCode.OK || status == IotHubStatusCode.OK_EMPTY
                AzureIotClient.SyncResult(
                    success = ok,
                    code = status?.ordinal,
                    body = status?.name,
                    error = if (ok) null else "MQTT failed: ${status?.name}"
                )
            }
        } catch (e: Exception) {
            AzureIotClient.SyncResult(success = false, error = e.message)
        }
    }
}
