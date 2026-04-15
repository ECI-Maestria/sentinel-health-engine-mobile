package com.example.tesisv3.viewmodel

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DashboardViewModelTest {

    // ── isMedForToday ─────────────────────────────────────────────────────────

    private val today = LocalDate.of(2026, 4, 14)

    @Test
    fun `isMedForToday start before today no end returns true`() =
        assertTrue(DashboardViewModel.isMedForToday("2026-01-01", "", today))

    @Test
    fun `isMedForToday start equals today no end returns true`() =
        assertTrue(DashboardViewModel.isMedForToday("2026-04-14", "", today))

    @Test
    fun `isMedForToday start after today returns false`() =
        assertFalse(DashboardViewModel.isMedForToday("2026-05-01", "", today))

    @Test
    fun `isMedForToday today within start-end range returns true`() =
        assertTrue(DashboardViewModel.isMedForToday("2026-01-01", "2026-12-31", today))

    @Test
    fun `isMedForToday today equals end date returns true`() =
        assertTrue(DashboardViewModel.isMedForToday("2026-01-01", "2026-04-14", today))

    @Test
    fun `isMedForToday end date before today returns false`() =
        assertFalse(DashboardViewModel.isMedForToday("2026-01-01", "2026-03-01", today))

    @Test
    fun `isMedForToday null literal end date treated as no end`() =
        assertTrue(DashboardViewModel.isMedForToday("2026-01-01", "null", today))

    @Test
    fun `isMedForToday ISO format start with time component uses only date part`() =
        assertTrue(DashboardViewModel.isMedForToday("2026-04-14T00:00:00Z", "", today))

    @Test
    fun `isMedForToday invalid start date returns true as graceful fallback`() =
        assertTrue(DashboardViewModel.isMedForToday("not-a-date", "", today))

    @Test
    fun `isMedForToday invalid end date returns true as graceful fallback`() =
        assertTrue(DashboardViewModel.isMedForToday("2026-01-01", "bad-end", today))

    @Test
    fun `isMedForToday both dates blank returns true as graceful fallback`() =
        assertTrue(DashboardViewModel.isMedForToday("", "", today))

    // ── isTimeDue ─────────────────────────────────────────────────────────────

    @Test
    fun `isTimeDue blank string returns false`() =
        assertFalse(DashboardViewModel.isTimeDue(""))

    @Test
    fun `isTimeDue midnight 00_00 is always due`() =
        // hour=0 min=0 → 0 < currentHour OR (0==currentHour && 0 <= currentMinute) → always true
        assertTrue(DashboardViewModel.isTimeDue("00:00"))

    @Test
    fun `isTimeDue 23_59 is not due except at end of day`() =
        // Only due when clock shows exactly 23:59 — safe to assert false in a test runner
        assertFalse(DashboardViewModel.isTimeDue("23:59"))

    @Test
    fun `isTimeDue invalid format returns false`() =
        assertFalse(DashboardViewModel.isTimeDue("not-a-time"))

    @Test
    fun `isTimeDue single digit hour-only string returns false`() =
        assertFalse(DashboardViewModel.isTimeDue("abc"))

    // ── formatMedTime ─────────────────────────────────────────────────────────

    @Test
    fun `formatMedTime blank string returns empty string`() =
        assertEquals("", DashboardViewModel.formatMedTime(""))

    @Test
    fun `formatMedTime midnight 00_00 returns 12_00 AM`() =
        assertEquals("12:00 AM", DashboardViewModel.formatMedTime("00:00"))

    @Test
    fun `formatMedTime noon 12_00 returns 12_00 PM`() =
        assertEquals("12:00 PM", DashboardViewModel.formatMedTime("12:00"))

    @Test
    fun `formatMedTime 08_30 returns 8_30 AM`() =
        assertEquals("8:30 AM", DashboardViewModel.formatMedTime("08:30"))

    @Test
    fun `formatMedTime 13_05 returns 1_05 PM`() =
        assertEquals("1:05 PM", DashboardViewModel.formatMedTime("13:05"))

    @Test
    fun `formatMedTime 14_45 returns 2_45 PM`() =
        assertEquals("2:45 PM", DashboardViewModel.formatMedTime("14:45"))

    @Test
    fun `formatMedTime 23_59 returns 11_59 PM`() =
        assertEquals("11:59 PM", DashboardViewModel.formatMedTime("23:59"))

    @Test
    fun `formatMedTime 01_00 returns 1_00 AM`() =
        assertEquals("1:00 AM", DashboardViewModel.formatMedTime("01:00"))

    @Test
    fun `formatMedTime ISO timestamp result contains minutes and AM-PM`() {
        val result = DashboardViewModel.formatMedTime("2026-04-14T14:30:00Z")
        assertTrue("Should contain ':30'", result.contains(":30"))
        assertTrue("Should end with AM or PM", result.endsWith("AM") || result.endsWith("PM"))
    }

    @Test
    fun `formatMedTime invalid string returns original unchanged`() =
        assertEquals("not-a-time", DashboardViewModel.formatMedTime("not-a-time"))

    // ── buildReminderTimeLabel ────────────────────────────────────────────────

    @Test
    fun `buildReminderTimeLabel blank string returns Hoy`() =
        assertEquals("Hoy", DashboardViewModel.buildReminderTimeLabel(""))

    @Test
    fun `buildReminderTimeLabel HH_MM format starts with Hoy`() {
        val result = DashboardViewModel.buildReminderTimeLabel("08:00")
        assertTrue("Should start with 'Hoy'", result.startsWith("Hoy"))
    }

    @Test
    fun `buildReminderTimeLabel HH_MM format contains dot separator`() {
        val result = DashboardViewModel.buildReminderTimeLabel("14:30")
        assertTrue("Should contain ' · '", result.contains(" · "))
    }

    @Test
    fun `buildReminderTimeLabel ISO timestamp starts with Hoy`() {
        val result = DashboardViewModel.buildReminderTimeLabel("2026-04-14T08:00:00Z")
        assertTrue("Should start with 'Hoy'", result.startsWith("Hoy"))
    }

    @Test
    fun `buildReminderTimeLabel unparseable non-blank string appends original text after Hoy`() {
        // Instant.parse throws → inner catch calls formatMedTime which returns the
        // original string unchanged → final result is "Hoy · <original>"
        assertEquals("Hoy · invalid-input", DashboardViewModel.buildReminderTimeLabel("invalid-input"))
    }

    // ── formatRecurrence ─────────────────────────────────────────────────────

    @Test
    fun `formatRecurrence DAILY returns Diario`() =
        assertEquals("Diario", DashboardViewModel.formatRecurrence("DAILY"))

    @Test
    fun `formatRecurrence daily lowercase returns Diario`() =
        assertEquals("Diario", DashboardViewModel.formatRecurrence("daily"))

    @Test
    fun `formatRecurrence Daily mixed case returns Diario`() =
        assertEquals("Diario", DashboardViewModel.formatRecurrence("Daily"))

    @Test
    fun `formatRecurrence WEEKLY returns Semanal`() =
        assertEquals("Semanal", DashboardViewModel.formatRecurrence("WEEKLY"))

    @Test
    fun `formatRecurrence MONTHLY returns Mensual`() =
        assertEquals("Mensual", DashboardViewModel.formatRecurrence("MONTHLY"))

    @Test
    fun `formatRecurrence ONCE returns Una vez`() =
        assertEquals("Una vez", DashboardViewModel.formatRecurrence("ONCE"))

    @Test
    fun `formatRecurrence AS_NEEDED returns Segun necesidad`() =
        assertEquals("Según necesidad", DashboardViewModel.formatRecurrence("AS_NEEDED"))

    @Test
    fun `formatRecurrence unknown value converts underscore to space and title-cases`() =
        assertEquals("Every other day", DashboardViewModel.formatRecurrence("EVERY_OTHER_DAY"))

    @Test
    fun `formatRecurrence empty string returns empty string`() =
        assertEquals("", DashboardViewModel.formatRecurrence(""))

    // ── formatAppointmentTimeLabel ────────────────────────────────────────────

    @Test
    fun `formatAppointmentTimeLabel zero millis returns PENDIENTE`() =
        assertEquals("PENDIENTE", DashboardViewModel.formatAppointmentTimeLabel(0L))

    @Test
    fun `formatAppointmentTimeLabel current time starts with HOY`() {
        val now = System.currentTimeMillis()
        val result = DashboardViewModel.formatAppointmentTimeLabel(now)
        assertTrue("Expected 'HOY · ...' but got '$result'", result.startsWith("HOY"))
    }

    @Test
    fun `formatAppointmentTimeLabel result always contains dot separator for non-zero millis`() {
        val result = DashboardViewModel.formatAppointmentTimeLabel(System.currentTimeMillis())
        assertTrue("Should contain ' · '", result.contains(" · "))
    }

    @Test
    fun `formatAppointmentTimeLabel far future date does not return HOY or MANANA`() {
        // ~2099-01-01T00:00:00Z
        val farFuture = 4_070_908_800_000L
        val result = DashboardViewModel.formatAppointmentTimeLabel(farFuture)
        assertFalse("Should not be HOY",    result.startsWith("HOY"))
        assertFalse("Should not be MAÑANA", result.startsWith("MAÑANA"))
        assertTrue("Should contain ' · '",  result.contains(" · "))
    }

    @Test
    fun `formatAppointmentTimeLabel result for non-zero millis is non-blank`() {
        val result = DashboardViewModel.formatAppointmentTimeLabel(1_000_000_000_000L)
        assertTrue(result.isNotBlank())
    }

    // ── formatVitalsTimestamp ─────────────────────────────────────────────────

    @Test
    fun `formatVitalsTimestamp blank string returns dash`() =
        assertEquals("—", DashboardViewModel.formatVitalsTimestamp(""))

    @Test
    fun `formatVitalsTimestamp invalid string returns dash`() =
        assertEquals("—", DashboardViewModel.formatVitalsTimestamp("not-a-timestamp"))

    @Test
    fun `formatVitalsTimestamp just now returns Ahora`() {
        val now = Instant.now().toString()
        assertEquals("Ahora", DashboardViewModel.formatVitalsTimestamp(now))
    }

    @Test
    fun `formatVitalsTimestamp 30 minutes ago returns hace 30 min`() {
        val thirtyMinsAgo = Instant.now().minus(30, ChronoUnit.MINUTES).toString()
        assertEquals("hace 30 min", DashboardViewModel.formatVitalsTimestamp(thirtyMinsAgo))
    }

    @Test
    fun `formatVitalsTimestamp 1 minute ago returns hace 1 min`() {
        val oneMinAgo = Instant.now().minus(1, ChronoUnit.MINUTES).toString()
        assertEquals("hace 1 min", DashboardViewModel.formatVitalsTimestamp(oneMinAgo))
    }

    @Test
    fun `formatVitalsTimestamp 2 hours ago returns hace 2h`() {
        val twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS).toString()
        assertEquals("hace 2h", DashboardViewModel.formatVitalsTimestamp(twoHoursAgo))
    }

    @Test
    fun `formatVitalsTimestamp 5 hours ago returns hace 5h`() {
        val fiveHoursAgo = Instant.now().minus(5, ChronoUnit.HOURS).toString()
        assertEquals("hace 5h", DashboardViewModel.formatVitalsTimestamp(fiveHoursAgo))
    }

    @Test
    fun `formatVitalsTimestamp 59 minutes ago returns hace 59 min`() {
        val fiftyNineAgo = Instant.now().minus(59, ChronoUnit.MINUTES).toString()
        assertEquals("hace 59 min", DashboardViewModel.formatVitalsTimestamp(fiftyNineAgo))
    }

    // ── buildWearablePayload ──────────────────────────────────────────────────

    @Test
    fun `buildWearablePayload adds deviceId to valid JSON`() {
        val result = DashboardViewModel.buildWearablePayload("""{"hr":75}""", "/vitals", "dev-001")
        val obj = JSONObject(result)
        assertEquals("dev-001", obj.getString("deviceId"))
    }

    @Test
    fun `buildWearablePayload preserves existing fields`() {
        val result = DashboardViewModel.buildWearablePayload("""{"hr":75,"spo2":98}""", "/vitals", "dev-001")
        val obj = JSONObject(result)
        assertEquals(75,  obj.getInt("hr"))
        assertEquals(98,  obj.getInt("spo2"))
    }

    @Test
    fun `buildWearablePayload adds timestamp when missing`() {
        val result = DashboardViewModel.buildWearablePayload("""{"hr":75}""", "/vitals", "dev-001")
        val obj = JSONObject(result)
        assertTrue("Should have timestamp key", obj.has("timestamp"))
        assertTrue("Timestamp should be non-blank", obj.getString("timestamp").isNotBlank())
    }

    @Test
    fun `buildWearablePayload does not overwrite existing timestamp`() {
        val fixed = "2026-01-01T00:00:00Z"
        val payload = """{"hr":75,"timestamp":"$fixed"}"""
        val result = DashboardViewModel.buildWearablePayload(payload, "/vitals", "dev-001")
        val obj = JSONObject(result)
        assertEquals(fixed, obj.getString("timestamp"))
    }

    @Test
    fun `buildWearablePayload with invalid JSON falls back to raw structure`() {
        val result = DashboardViewModel.buildWearablePayload("not-json", "/path", "dev-001")
        val obj = JSONObject(result)
        assertEquals("dev-001",   obj.getString("deviceId"))
        assertEquals("not-json",  obj.getString("rawPayload"))
        assertEquals("/path",     obj.getString("rawPath"))
        assertTrue(obj.has("timestamp"))
    }

    @Test
    fun `buildWearablePayload with empty string falls back to raw structure`() {
        val result = DashboardViewModel.buildWearablePayload("", "/test", "uuid-abc")
        val obj = JSONObject(result)
        assertEquals("uuid-abc", obj.getString("deviceId"))
        assertTrue(obj.has("timestamp"))
    }

    @Test
    fun `buildWearablePayload result is always valid JSON`() {
        listOf(
            """{"a":1}""",
            "plain text",
            "",
            """{"nested":{"x":1}}"""
        ).forEach { input ->
            val result = DashboardViewModel.buildWearablePayload(input, "/p", "d")
            assertNotNull("Result for '$input' must parse as JSON", JSONObject(result))
        }
    }
}
