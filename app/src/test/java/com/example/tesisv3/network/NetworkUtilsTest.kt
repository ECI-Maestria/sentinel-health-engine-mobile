package com.example.tesisv3.network

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class NetworkUtilsTest {

    // ── buildJsonBody ─────────────────────────────────────────────────────────

    @Test
    fun `buildJsonBody single pair produces valid JSON`() {
        val json = buildJsonBody("name" to "Alice")
        val obj = JSONObject(json)
        assertEquals("Alice", obj.getString("name"))
    }

    @Test
    fun `buildJsonBody multiple pairs all keys present`() {
        val json = buildJsonBody(
            "firstName" to "Ana",
            "lastName"  to "López",
            "email"     to "ana@example.com"
        )
        val obj = JSONObject(json)
        assertEquals("Ana",              obj.getString("firstName"))
        assertEquals("López",            obj.getString("lastName"))
        assertEquals("ana@example.com",  obj.getString("email"))
        assertEquals(3,                  obj.length())
    }

    @Test
    fun `buildJsonBody null value serialises as JSON null`() {
        val json = buildJsonBody("middleName" to null)
        val obj = JSONObject(json)
        assertTrue("Expected JSON null", obj.isNull("middleName"))
    }

    @Test
    fun `buildJsonBody mixed null and non-null values`() {
        val json = buildJsonBody("name" to "Bob", "phone" to null)
        val obj = JSONObject(json)
        assertEquals("Bob", obj.getString("name"))
        assertTrue(obj.isNull("phone"))
    }

    @Test
    fun `buildJsonBody empty string value is preserved`() {
        val json = buildJsonBody("code" to "")
        val obj = JSONObject(json)
        assertEquals("", obj.getString("code"))
    }

    @Test
    fun `buildJsonBody no pairs returns empty JSON object`() {
        val json = buildJsonBody()
        val obj = JSONObject(json)
        assertEquals(0, obj.length())
    }

    @Test
    fun `buildJsonBody prevents JSON injection from malicious value`() {
        // A naive string-concat approach would break JSON structure here
        val malicious = """}, "admin": true, "x": {"""
        val json = buildJsonBody("note" to malicious)
        val obj = JSONObject(json)          // must not throw
        assertEquals(malicious, obj.getString("note"))
        assertEquals(1, obj.length())       // only one key
    }

    @Test
    fun `buildJsonBody handles special characters correctly`() {
        val json = buildJsonBody("msg" to "Héllo \"wörld\" & <test>")
        val obj = JSONObject(json)
        assertEquals("Héllo \"wörld\" & <test>", obj.getString("msg"))
    }

    @Test
    fun `buildJsonBody handles newlines and tabs in values`() {
        val json = buildJsonBody("text" to "line1\nline2\ttab")
        val obj = JSONObject(json)
        assertEquals("line1\nline2\ttab", obj.getString("text"))
    }

    @Test
    fun `buildJsonBody result is parseable JSON`() {
        val json = buildJsonBody("a" to "1", "b" to "2")
        // JSONObject constructor throws if not valid JSON
        assertNotNull(JSONObject(json))
    }

    // ── readStream ────────────────────────────────────────────────────────────

    @Test
    fun `readStream with null returns empty string`() {
        assertEquals("", readStream(null))
    }

    @Test
    fun `readStream with valid content returns full text`() {
        val input = "Hello, World!".byteInputStream()
        assertEquals("Hello, World!", readStream(input))
    }

    @Test
    fun `readStream with multiline content preserves newlines`() {
        val text = "first\nsecond\nthird"
        assertEquals(text, readStream(text.byteInputStream()))
    }

    @Test
    fun `readStream with empty stream returns empty string`() {
        val input = ByteArrayInputStream(ByteArray(0))
        assertEquals("", readStream(input))
    }

    @Test
    fun `readStream with UTF-8 content preserves unicode`() {
        val text = "Paciente: José Ángel Núñez"
        assertEquals(text, readStream(text.byteInputStream(Charsets.UTF_8)))
    }

    @Test
    fun `readStream with JSON content returns raw JSON string`() {
        val json = """{"id":"abc","value":42}"""
        assertEquals(json, readStream(json.byteInputStream()))
    }

    @Test
    fun `readStream with large content returns full text`() {
        val large = "x".repeat(100_000)
        assertEquals(large, readStream(large.byteInputStream()))
    }

    @Test
    fun `readStream with broken stream returns empty string`() {
        val brokenStream = object : InputStream() {
            override fun read(): Int = throw IOException("Simulated read failure")
        }
        // Should not throw — must catch and return ""
        assertEquals("", readStream(brokenStream))
    }
}
