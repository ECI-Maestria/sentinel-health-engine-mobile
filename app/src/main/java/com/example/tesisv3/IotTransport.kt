package com.example.tesisv3

interface IotTransport {
    fun sendSyncMessage(connectionString: String, payload: String): AzureIotClient.SyncResult
}
