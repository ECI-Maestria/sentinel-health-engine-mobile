package com.example.tesisv3.network

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.InputStream

private const val TAG = "NetworkUtils"

/**
 * Reads an HTTP response stream to a String.
 * Shared by all network functions — single implementation for the whole app.
 */
fun readStream(stream: InputStream?): String {
    if (stream == null) return ""
    return try {
        BufferedReader(InputStreamReader(stream)).use { it.readText() }
    } catch (e: Exception) {
        Log.e(TAG, "readStream failed", e)
        ""
    }
}

/**
 * Builds a JSON string safely using JSONObject instead of manual string
 * concatenation, preventing JSON-injection from field values.
 *
 * Usage:
 *   buildJsonBody("firstName" to firstName, "email" to email)
 */
fun buildJsonBody(vararg pairs: Pair<String, String?>): String {
    val obj = JSONObject()
    for ((key, value) in pairs) {
        obj.put(key, value ?: JSONObject.NULL)
    }
    return obj.toString()
}
