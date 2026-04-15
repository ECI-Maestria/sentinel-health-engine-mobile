package com.example.tesisv3.network

import java.net.HttpURLConnection
import java.net.URL

data class AuthResult(val success: Boolean, val message: String)

fun requestForgotPassword(email: String): AuthResult {
    val url = URL("${ApiConstants.USER_SERVICE}/v1/auth/forgot-password")
    val payload = buildJsonBody("email" to email.trim())
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
        }
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        when {
            code in 200..299 -> AuthResult(true, "")
            else -> AuthResult(false, body.ifBlank { "Request failed (HTTP $code)" })
        }
    } catch (e: Exception) {
        AuthResult(false, e.message ?: "Network error")
    }
}

fun verifyResetCode(code: String): AuthResult {
    val url = URL("${ApiConstants.USER_SERVICE}/v1/auth/verify-reset-code")
    val payload = buildJsonBody("code" to code.trim())
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
        }
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val codeResp = conn.responseCode
        val body = readStream(if (codeResp in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        when {
            codeResp in 200..299 -> AuthResult(true, "")
            else -> AuthResult(false, body.ifBlank { "Request failed (HTTP $codeResp)" })
        }
    } catch (e: Exception) {
        AuthResult(false, e.message ?: "Network error")
    }
}

fun resetPasswordWithCode(code: String, newPassword: String): AuthResult {
    val url = URL("${ApiConstants.USER_SERVICE}/v1/auth/reset-password")
    val payload = buildJsonBody("code" to code.trim(), "newPassword" to newPassword.trim())
    return try {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
        }
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val codeResp = conn.responseCode
        val body = readStream(if (codeResp in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        when {
            codeResp in 200..299 -> AuthResult(true, "")
            else -> AuthResult(false, body.ifBlank { "Reset failed (HTTP $codeResp)" })
        }
    } catch (e: Exception) {
        AuthResult(false, e.message ?: "Network error")
    }
}
