package com.example.tesisv3.iot

import com.example.tesisv3.*

object HttpTransport : IotTransport {
    override fun sendSyncMessage(connectionString: String, payload: String): AzureIotClient.SyncResult {
        return AzureIotClient.sendSyncMessage(connectionString, payload)
    }
}
