package com.example.tesisv3

import org.junit.Assert.*
import org.junit.Test

class UserRoleTest {

    // ── UserRole.from() ───────────────────────────────────────────────────────

    @Test
    fun `from DOCTOR uppercase returns DOCTOR`() =
        assertEquals(UserRole.DOCTOR, UserRole.from("DOCTOR"))

    @Test
    fun `from doctor lowercase returns DOCTOR`() =
        assertEquals(UserRole.DOCTOR, UserRole.from("doctor"))

    @Test
    fun `from Doctor mixed case returns DOCTOR`() =
        assertEquals(UserRole.DOCTOR, UserRole.from("Doctor"))

    @Test
    fun `from DOCTOR with leading and trailing spaces returns DOCTOR`() =
        assertEquals(UserRole.DOCTOR, UserRole.from("  DOCTOR  "))

    @Test
    fun `from PATIENT uppercase returns PATIENT`() =
        assertEquals(UserRole.PATIENT, UserRole.from("PATIENT"))

    @Test
    fun `from patient lowercase returns PATIENT`() =
        assertEquals(UserRole.PATIENT, UserRole.from("patient"))

    @Test
    fun `from CARETAKER uppercase returns CARETAKER`() =
        assertEquals(UserRole.CARETAKER, UserRole.from("CARETAKER"))

    @Test
    fun `from caretaker lowercase returns CARETAKER`() =
        assertEquals(UserRole.CARETAKER, UserRole.from("caretaker"))

    @Test
    fun `from null returns UNKNOWN`() =
        assertEquals(UserRole.UNKNOWN, UserRole.from(null))

    @Test
    fun `from empty string returns UNKNOWN`() =
        assertEquals(UserRole.UNKNOWN, UserRole.from(""))

    @Test
    fun `from blank whitespace returns UNKNOWN`() =
        assertEquals(UserRole.UNKNOWN, UserRole.from("   "))

    @Test
    fun `from unrecognised string returns UNKNOWN`() =
        assertEquals(UserRole.UNKNOWN, UserRole.from("NURSE"))

    @Test
    fun `from numeric string returns UNKNOWN`() =
        assertEquals(UserRole.UNKNOWN, UserRole.from("123"))

    // ── UserProfile extension functions ───────────────────────────────────────

    private fun profile(role: String) =
        UserProfile("1", "test@test.com", role, "Ana", "López", "Ana López", true, null)

    @Test
    fun `isDoctor returns true for DOCTOR role`() =
        assertTrue(profile("DOCTOR").isDoctor())

    @Test
    fun `isDoctor returns true for lowercase doctor role`() =
        assertTrue(profile("doctor").isDoctor())

    @Test
    fun `isDoctor returns false for PATIENT role`() =
        assertFalse(profile("PATIENT").isDoctor())

    @Test
    fun `isDoctor returns false for CARETAKER role`() =
        assertFalse(profile("CARETAKER").isDoctor())

    @Test
    fun `isPatient returns true for PATIENT role`() =
        assertTrue(profile("PATIENT").isPatient())

    @Test
    fun `isPatient returns false for DOCTOR role`() =
        assertFalse(profile("DOCTOR").isPatient())

    @Test
    fun `isCaretaker returns true for CARETAKER role`() =
        assertTrue(profile("CARETAKER").isCaretaker())

    @Test
    fun `isCaretaker returns false for PATIENT role`() =
        assertFalse(profile("PATIENT").isCaretaker())

    @Test
    fun `null UserProfile isDoctor returns false`() =
        assertFalse((null as UserProfile?).isDoctor())

    @Test
    fun `null UserProfile isPatient returns false`() =
        assertFalse((null as UserProfile?).isPatient())

    @Test
    fun `null UserProfile isCaretaker returns false`() =
        assertFalse((null as UserProfile?).isCaretaker())

    @Test
    fun `null UserProfile roleEnum returns UNKNOWN`() =
        assertEquals(UserRole.UNKNOWN, (null as UserProfile?).roleEnum)

    // ── Enum values sanity ────────────────────────────────────────────────────

    @Test
    fun `UserRole has exactly four values`() =
        assertEquals(4, UserRole.values().size)

    @Test
    fun `all standard roles are distinct`() {
        assertNotEquals(UserRole.DOCTOR, UserRole.PATIENT)
        assertNotEquals(UserRole.DOCTOR, UserRole.CARETAKER)
        assertNotEquals(UserRole.PATIENT, UserRole.CARETAKER)
    }
}
