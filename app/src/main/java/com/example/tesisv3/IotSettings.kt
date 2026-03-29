package com.example.tesisv3

import android.content.Context

enum class TransportType { HTTP, MQTT }

object IotSettings {
    private const val PREFS = "iot_settings"
    private const val KEY_TRANSPORT = "transport"

    fun getTransport(context: Context): TransportType {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val value = prefs.getString(KEY_TRANSPORT, TransportType.HTTP.name)
        return TransportType.valueOf(value ?: TransportType.HTTP.name)
    }

    fun setTransport(context: Context, type: TransportType) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TRANSPORT, type.name).apply()
    }
}
