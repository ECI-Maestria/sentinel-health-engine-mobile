package com.example.tesisv3.viewmodel

import org.junit.Assert.*
import org.junit.Test

class CalendarViewModelTest {

    // ── parseUtcMillis ────────────────────────────────────────────────────────

    @Test
    fun `parseUtcMillis parses valid ISO 8601 string to correct epoch millis`() {
        // 2024-01-15T00:00:00Z = 1705276800000L
        val result = CalendarViewModel.parseUtcMillis("2024-01-15T00:00:00Z")
        assertEquals(1_705_276_800_000L, result)
    }

    @Test
    fun `parseUtcMillis epoch zero string returns 0`() {
        assertEquals(0L, CalendarViewModel.parseUtcMillis("1970-01-01T00:00:00Z"))
    }

    @Test
    fun `parseUtcMillis parses timestamp with non-zero time`() {
        // 2026-04-14T10:30:00Z = 1776162600000L
        // Verified: (20454 days to 2026-01-01) + 103 days to Apr-14 + 37800 s = 1,776,162,600 s
        val result = CalendarViewModel.parseUtcMillis("2026-04-14T10:30:00Z")
        assertEquals(1_776_162_600_000L, result)
    }

    @Test
    fun `parseUtcMillis with invalid string returns current time (approx)`() {
        val before = System.currentTimeMillis()
        val result  = CalendarViewModel.parseUtcMillis("not-a-date")
        val after   = System.currentTimeMillis()
        assertTrue("Result should be within current time window", result in before..after)
    }

    @Test
    fun `parseUtcMillis with empty string returns current time (approx)`() {
        val before = System.currentTimeMillis()
        val result  = CalendarViewModel.parseUtcMillis("")
        val after   = System.currentTimeMillis()
        assertTrue("Result should be within current time window", result in before..after)
    }

    @Test
    fun `parseUtcMillis with blank whitespace returns current time (approx)`() {
        val before = System.currentTimeMillis()
        val result  = CalendarViewModel.parseUtcMillis("   ")
        val after   = System.currentTimeMillis()
        assertTrue("Result should be within current time window", result in before..after)
    }

    @Test
    fun `parseUtcMillis with malformed date part returns current time (approx)`() {
        val before = System.currentTimeMillis()
        val result  = CalendarViewModel.parseUtcMillis("2024-99-99T25:99:99Z")
        val after   = System.currentTimeMillis()
        assertTrue(result in before..after)
    }

    @Test
    fun `parseUtcMillis with date-only string without time returns current time (approx)`() {
        // Instant.parse requires a full ISO-8601 with time zone — date-only should fail
        val before = System.currentTimeMillis()
        val result  = CalendarViewModel.parseUtcMillis("2026-04-14")
        val after   = System.currentTimeMillis()
        assertTrue(result in before..after)
    }

    @Test
    fun `parseUtcMillis two different valid timestamps produce distinct millis`() {
        val t1 = CalendarViewModel.parseUtcMillis("2026-01-01T00:00:00Z")
        val t2 = CalendarViewModel.parseUtcMillis("2026-06-01T00:00:00Z")
        assertTrue("Later timestamp must be greater", t2 > t1)
    }

    @Test
    fun `parseUtcMillis result is always positive for modern dates`() {
        val result = CalendarViewModel.parseUtcMillis("2025-08-20T15:45:00Z")
        assertTrue(result > 0L)
    }
}
