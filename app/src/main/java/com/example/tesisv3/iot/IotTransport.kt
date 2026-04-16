package com.example.tesisv3.iot

import com.example.tesisv3.*

interface IotTransport {
    fun sendSyncMessage(connectionString: String, payload: String): AzureIotClient.SyncResult
}
