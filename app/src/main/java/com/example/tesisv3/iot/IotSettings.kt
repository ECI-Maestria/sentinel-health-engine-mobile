package com.example.tesisv3.iot

import com.example.tesisv3.*

import android.content.Context

enum class TransportType { HTTP, MQTT, MQTT_WS }

object IotSettings {
    private const val PREFS = "iot_settings"
    private const val KEY_TRANSPORT = "transport"
    private const val KEY_DIAGNOSTIC = "mqtt_diagnostic"
    private const val KEY_DEVICE_REGISTER_MODAL = "device_register_modal"

    fun getTransport(context: Context): TransportType {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val value = prefs.getString(KEY_TRANSPORT, TransportType.HTTP.name)
        return TransportType.valueOf(value ?: TransportType.HTTP.name)
    }

    fun setTransport(context: Context, type: TransportType) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TRANSPORT, type.name).apply()
    }

    fun isDiagnosticEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DIAGNOSTIC, false)
    }

    fun setDiagnosticEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DIAGNOSTIC, enabled).apply()
    }

    fun isDeviceRegisterModalEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DEVICE_REGISTER_MODAL, false)
    }

    fun setDeviceRegisterModalEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DEVICE_REGISTER_MODAL, enabled).apply()
    }
}
