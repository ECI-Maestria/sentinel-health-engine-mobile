package com.example.tesisv3

/**
 * Single source of truth for role identifiers and comparisons.
 *
 * Usage:
 *   if (PatientSession.currentUser.isDoctor()) { ... }
 *   val role = PatientSession.currentUser?.roleEnum ?: UserRole.UNKNOWN
 */
enum class UserRole {
    DOCTOR, PATIENT, CARETAKER, UNKNOWN;

    companion object {
        fun from(raw: String?): UserRole = when (raw?.trim()?.uppercase()) {
            "DOCTOR"    -> DOCTOR
            "PATIENT"   -> PATIENT
            "CARETAKER" -> CARETAKER
            else        -> UNKNOWN
        }
    }
}

val UserProfile?.roleEnum: UserRole get() = UserRole.from(this?.role)

fun UserProfile?.isDoctor()    = this.roleEnum == UserRole.DOCTOR
fun UserProfile?.isPatient()   = this.roleEnum == UserRole.PATIENT
fun UserProfile?.isCaretaker() = this.roleEnum == UserRole.CARETAKER
